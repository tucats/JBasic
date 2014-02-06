
package org.fernwood.jbasic.runtime;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

/**
 * A client file object for communicating with the File System Manager
 * file system.  An instance of this class is used to communicate with 
 * the manager, open, read, write, and close files.
 * <p>
 * The FSMFile system is a streaming system similar to HDFS in features.
 * You can open a file for write, for append, or for read, and the data is
 * streamed from the file system to this client class.
 * <p>
 * Data is stored across a data topology; this specifies one or more groups
 * and one or more resources in each group.  The combination of group
 * identification and resource identification defines a specific resource
 * path, such as a node, port, and directory specification.
 * <p>
 * The FSMFile system supports distribution; a file is
 * written in segments, typically each one being 64MB in size.  The segments
 * can be distributed over a storage topology (multiple directories on one
 * system, multiple systems, multiple nodes on a rack, etc.
 * <p>
 * The FSMFile system supports replication; for a given file the owner can
 * specify the number of replications of each segment that are to be 
 * maintained.  The FSMFile system detects when a segment becomes unavailable
 * due to accidental deletion or physical failure of the storage path or
 * the hosting node, and allocates a new segment elsewhere in the topology.
 * <p>
 * The typical usage pattern is:
 * <p>
 * <li> Connect to a server by creating an FSMFile instance.</li>
 * <li>Create a new file, or open an existing file for append or input</li>
 * <li>Call the write() method to stream to the file, or read() to read from
 * the file.</li>
 * <li> Close the file when done, which commits the changes for create or
 * append </li>
 * <p>
 * Future versions will support positioning, such that you can rewind a file
 * or position to a specific byte in the file to resume reading.
 * <p><br><br>
 * @author tom
 * @version version 1.0 Aug 11, 2011
 *
 */
public class FSMFile {

	/**
	 * This indicates the size of the data chunks read from the spool file
	 * and sent over the network connection to the remote server.  The default
	 * is 64k at a time.
	 */
	private static final int SPOOL_READ_SIZE = 65536;

	/**
	 * This indicates the prefix of the temporary file created on the local
	 * system to spool data (buffering it) before it is sent to a remote data
	 * server.
	 */
	private static final String SPOOL_NAME_PREFIX = "fsmspool-";
	
	/**
	 * This indicates the suffix of the temporary file created on the local
	 * system to spool data (buffering it) before it is sent to a remote data
	 * server.
	 */
	private static final String SPOOL_NAME_SUFFIX = ".dat";

	/**
	 * Indicate that this instance of a file is open for OUTPUT.
	 */
	public static final int MODE_OUTPUT = 1;

	/**
	 * Indicate that this instance of a file is open for INPUT.
	 */
	public static final int MODE_INPUT = 2;

	/**
	 * This indicates how big the spool file is allowed to become before
	 * it is written to the remote server.  The default is 1 megabyte
	 */
	private long dataSegmentSize;

	/**
	 * The socket to the name server.  This is held open when the
	 * object is created for the duration of the object's existence
	 * or whenever the server is shut down.
	 */
	private Socket nameServerSocket;

	/**
	 * Used to read responses from the server socket
	 */
	private BufferedReader nameServerInput;

	/**
	 * Used to write commands to the server socket
	 */
	private PrintWriter nameServerOutput;

	/**
	 * Record the status of the last operation.  A status
	 * string that starts with "-" is an error.
	 */
	private String status;

	/**
	 * The file handle on the server for the open file connection.
	 * This must be used for reads, writes, and close operations
	 * to reference the correct file object on the server side.
	 */
	private int fileHandle;

	/**
	 * When a read operation is performed, it is done via a cursor
	 * object on the server, which records the position and state of 
	 * the read operation.  This integer remembers the handle number
	 * on the server session for the read cursor being used.
	 */
	private int cursorHandle;

	/**
	 * This flag is false when the object is being initialized, and is
	 * only set to true after initialization completes.  It does not
	 * indicate that the user was able to connect successfully, but
	 * does indicate that a name server connection was made.
	 */
	private boolean valid;

	/**
	 * This is the open spool file currently in use.  This is held open
	 * as long as there are active write operations against the file.
	 * When a write to this file causes it to exceed the segment size,
	 * the file is closed and the contents copied to  a data server for
	 * replication and storage, after which the spoolFile is reset to null
	 * to indicate no active spooling.
	 */
	private FileOutputStream spoolFile;

	/**
	 * This indicates how many bytes have been written to the spool file
	 * so far. This is used to track when a spool file is large enough to
	 * be created as a new data segment on a dataserver.
	 */
	private long spoolFileSize;

	/**
	 * This contains the name of the spool file. The name is generated as
	 * a host-appropriate local temporary file name.
	 */
	private String spoolName;

	Socket dataServerSocket;
	ObjectOutputStream dataServerOutput;
	ObjectInputStream dataServerInput;

	private int mode;

	private byte[] bufferedData;

	private int bufferPos;

	private int bufferMax;

	/**
	 * A flag indicating if errors result in more detailed reports and/or 
	 * tracebacks.
	 */

	public static boolean DEBUG;

	/**
	 * Version number of the client.  The server must have a compatible
	 * version
	 */
	final public String VERSION = "1.1-0";

	
	/**
	 * Create an instance of a new file connection to the File System
	 * Manager, using a URL specification for a file and an open mode.
	 * 
	 * @param sourceName the String containing the source specification
	 * @param openMode the open mode
	 * @throws IOException if a communication error occurs while connecting
	 * to the server.
	 * @throws UnknownHostException  if the host name is invalid.
	 */
	public FSMFile( String sourceName, int openMode) throws UnknownHostException, IOException {
		valid = false;
		if( sourceName == null ) {
			status = "- missing source name string";
			return;
		}

		FSMConnectionManager cnx = new FSMConnectionManager(sourceName);
		if( cnx.error != null ) {
			System.out.println(cnx.error);
			System.exit(1);
		}

		String username = cnx.username;
		String password = cnx.password;
		String hostname = cnx.host;
		int port = cnx.port;
		
		if( username.length() == 0 )
			username = System.getProperty("user.name");
		
		if( DEBUG ) {
			System.out.println("Connection string,");
			System.out.println("  username=" + username);
			System.out.println("  password=" + password);
			System.out.println("  hostname=" + hostname);
			System.out.println("  port=" + port);
			System.out.println("  path=" + cnx.getPath());
		}
		
		if( cnx.getPath() == null || cnx.getPath().length() == 0 )
			throw new IOException("-missing path designation in connection string");
		
		initialize( hostname, port, username, password);

		/*
		 * Now open the file itself using the path data.
		 */
		
		this.open(cnx.getPath(), openMode);
	}
	
	
	/**
	 * Create an instance of a new file connection to the File System
	 * Manager, using a connection string.  The string has the format:
	 * <p>
	 * <code><em>username</em>,<em>password</em>@<em>hostname</em>:<em>port</em></code>
	 * <p>
	 * 
	 * After the object is create, the status field can be consulted to
	 * determine if an error occurred while connecting to the server or
	 * authenticating.
	 * 
	 * @param sourceName the String containing the source specification
	 * @throws IOException if a communication error occurs while connecting
	 * to the server.
	 * @throws UnknownHostException  if the host name is invalid.
	 */
	public FSMFile( String sourceName ) throws UnknownHostException, IOException {
		
		valid = false;
		if( sourceName == null ) {
			status = "- missing source name string";
			return;
		}

		FSMConnectionManager cnx = new FSMConnectionManager(sourceName);
		if( cnx.error != null ) {
			System.out.println(cnx.error);
			System.exit(1);
		}

		String username = cnx.username;
		String password = cnx.password;
		String hostname = cnx.host;
		int port = cnx.port;
		
		if( username.length() == 0 )
			username = System.getProperty("user.name");
		
		if( DEBUG ) {
			System.out.println("Connection string,");
			System.out.println("  username=" + username);
			System.out.println("  password=" + password);
			System.out.println("  hostname=" + hostname);
			System.out.println("  port=" + port);
			System.out.println("  path=" + cnx.getPath());
		}
		initialize( hostname, port, username, password);

		
		if( cnx.getPath() != null && cnx.getPath().length() > 0) {
			nameServerOutput.println("cd " + cnx.getPath());
			nameServerOutput.flush();
			nameServerInput.readLine();
		}
	}

	/**
	 * Create an instance of a new file connection to the File System
	 * Manager, using discrete connection values.
	 * @param server the host name where the NameServer is running
	 * @param port the port number on the host to connect to
	 * @param username The username to use to authenticate to the server
	 * @param password The password to use to authenticate to the server
	 * @throws UnknownHostException if the host name is invalid or unknown
	 * @throws IOException if an error occurs making a connection to the
	 * server.
	 */
	public FSMFile( String server, int port, String username, String password ) throws UnknownHostException, IOException {
		initialize( server, port, username, password);
	}

	/**
	 * Do the work ot initialize an instance of a new file connection to the File System
	 * Manager, using discrete connection values.
	 * @param server the host name where the NameServer is running
	 * @param port the port number on the host to connect to
	 * @param username The username to use to authenticate to the server
	 * @param password The password to use to authenticate to the server
	 * @throws UnknownHostException if the host name is invalid or unknown
	 * @throws IOException if an error occurs making a connection to the
	 * server.
	 */
	private void initialize( String server, int port, String username, String password ) 
		throws UnknownHostException, IOException {

		nameServerSocket = new Socket(server, port);
		nameServerInput = new BufferedReader(new InputStreamReader(nameServerSocket.getInputStream()));
		nameServerOutput = new PrintWriter(nameServerSocket.getOutputStream());
		status = nameServerInput.readLine();
		if( DEBUG )
			System.out.println("debug: status " + status);
		if( username != null )
			nameServerOutput.println("login " + username + " " + password);
		else
			nameServerOutput.println("handles");
		nameServerOutput.flush();
		status = nameServerInput.readLine();
		while( !status.startsWith("-") && !status.startsWith("+"))
			status = nameServerInput.readLine();
		if( DEBUG )
			System.out.println("debug: status " + status);
		if( status.startsWith("+")) {
			nameServerOutput.println("version " + VERSION);
			nameServerOutput.flush();
			status = nameServerInput.readLine();
		}
		
		valid = true;
		dataSegmentSize = 64*1024*1024;
		cursorHandle = -1;
		if( status.startsWith("-"))
			throw new IOException(status);
	}

	/**
	 * Return the last status message from the file object.  A string
	 * starting with a "-" indicates an error.
	 * @return a String.
	 */
	public String status() {
		return status;
	}

	/**
	 * Set the size of each data segment for this file.  The default is 1MB per segment.
	 * A segment defines the unit of replication of data; i.e. each 1MB of data is written
	 * to a new physical location on the data server and is replicated in 1MB chunks. The
	 * smaller this value is, the more physical files are created on the servers.
	 * 
	 * @param newSegmentSize the new segment size.
	 * @throws IOException If the file is not currently open
	 */
	public void setSegmentSize( long newSegmentSize ) throws IOException {
		if( mode != MODE_OUTPUT)
			throw new IOException("- file not opened for output");
		dataSegmentSize = newSegmentSize;
	}

	/**
	 * Create a file specification for use with this file object.
	 * @param path a string containing the fully qualified name of the
	 * file on the server.  The file is implicitly opened for output.
	 * @return a numeric handle representing the open file on the
	 * server, or -1 if there was an error.
	 * @throws IOException if there is no connection to the server, the
	 * file path or name is invalid or does not exist, or the current
	 * user does not have permission to access the file object.
	 */
	int create( String path ) throws IOException {
		if( !valid )
			throw new IOException("- no connection to server");
		if( mode != 0 )
			throw new IOException("- file already opened for " 
					+ ( mode == MODE_INPUT ? "input" : "output"));

		fileHandle = -1;
		cursorHandle = -1;
		if( nameServerSocket == null )
			throw new IOException("- no connection to nameserver");


		nameServerOutput.println("create file " + path);
		nameServerOutput.flush();
		status = nameServerInput.readLine();
		if( status.startsWith("-"))
			throw new IOException(status);

		fileHandle = Integer.parseInt(status.substring(1).trim());
		mode = MODE_OUTPUT;
		return fileHandle;
	}

	/**
	 * Delete a file on the server.  No handle results
	 * @param path a string containing the fully qualified name of the
	 * file on the server.  The file is implicitly opened for output.
	 * 
	 * @throws IOException if there is no connection to the server, the
	 * file path or name is invalid or does not exist, or the current
	 * user does not have permission to access the file object.
	 */
	public void delete( String path ) throws IOException {
		if( !valid )
			throw new IOException("- no connection to server");
		if( mode != 0 )
			throw new IOException("- file already opened for " 
					+ ( mode == MODE_INPUT ? "input" : "output"));

		fileHandle = -1;
		cursorHandle = -1;
		if( nameServerSocket == null )
			throw new IOException("- no connection to nameserver");


		nameServerOutput.println("delete file " + path);
		nameServerOutput.flush();
		status = nameServerInput.readLine();
		if( status.startsWith("-"))
			throw new IOException(status);

		return;
	}

	/**
	 * Open a given file specification for use with this file object.
	 * @param path a string containing the fully qualified name of the
	 * file on the server.
	 * @param fileMode the mode for the file, MODE_INPUT or MODE_OUTPUT
	 * @return a numeric handle representing the open file on the
	 * server, or -1 if there was an error.
	 * @throws IOException if there is no connection to the server, the
	 * file path or name is invalid or does not exist, or the current
	 * user does not have permission to access the file object.
	 */
	int open( String path, int fileMode ) throws IOException {
		if( !valid )
			throw new IOException("- no connection to server");
		if( mode != 0 )
			throw new IOException("- file already opened for " 
					+ ( mode == MODE_INPUT ? "input" : "output"));

		fileHandle = -1;
		cursorHandle = -1;
		if( nameServerSocket == null )
			throw new IOException("- no connection to nameserver");

		if( fileMode != MODE_INPUT && fileMode != MODE_OUTPUT )
			throw new IOException("- invalid file mode " + fileMode);
		mode = fileMode;

		if( mode == MODE_OUTPUT) {
			if( DEBUG )
				System.out.println("Client: create file_always " + path);

			nameServerOutput.println("create file_always " + path);
			nameServerOutput.flush();
			status = nameServerInput.readLine();
			if( status.startsWith("-"))
				throw new IOException(status);
			fileHandle = Integer.parseInt(status.substring(1).trim());
			return fileHandle;

		}
		if( DEBUG )
			System.out.println("Client: file "  + path);

		nameServerOutput.println("file " + path);
		nameServerOutput.flush();
		status = nameServerInput.readLine();
		if( status.startsWith("-"))
			throw new IOException(status);

		fileHandle = Integer.parseInt(status.substring(1).trim());
		if( DEBUG )
			System.out.println("Client: received handle "  +fileHandle);

		return fileHandle;
	}

	/**
	 * Seek the input file to a specific location. This is very similar to
	 * the readSegment operation, but always dumps the existing buffer and
	 * reloads it from the position seeked to in the READ command to the
	 * dataserver.
	 * <p>
	 * NOTE: This routine is not yet smart enough to know if the position 
	 * being seeked to is already in the buffer, so this is expensive if
	 * the position is "near" the current data item as it dumps the
	 * entire (64K) buffer.
	 * 
	 * @param bytePos byte position to seek to.
	 * @throws IOException if the position is invalid
	 */
	public void seek( long bytePos ) throws IOException {

		if( mode != MODE_INPUT)
			throw new IOException("file not opened for INPUT");		
		if( bytePos < 0 )
			throw new IOException("invalid seek position " + bytePos);		
		if( !valid )
			throw new IOException("- no connection to server");
		if( nameServerSocket == null )
			throw new IOException("- no connection to nameserver");
		if(fileHandle < 0 )
			throw new IOException("- file not open");

		/*
		 * Empty out all existing positional data.
		 */
		bufferedData = new byte[SPOOL_READ_SIZE];
		bufferPos = -1;
		bufferMax = 0;
		dataServerSocket = null;

		/*
		 * Do we have a cursor we're using for all this?  If not, 
		 * be sure to get one.
		 */
		if( cursorHandle < 0 ) {
			nameServerOutput.println("cursor " + fileHandle);
			nameServerOutput.flush();
			status = nameServerInput.readLine();
			if( status.startsWith("-"))
				throw new IOException(status);
			StringTokenizer cmd = new StringTokenizer(status);

			/*
			 * Skip the "+" and then parse the handle number.  Then
			 * all done with this buffer; toss it away.  The rest of
			 * the buffer contains the cursor description which we 
			 * don't need.
			 */
			cmd.nextToken();
			cursorHandle = Integer.parseInt(cmd.nextToken());
			cmd = null;
		}

		int count = 0;

		while( count <= 0 ) {

			/*
			 * Is there an active connection to a data server? If not,
			 * use the cursor to find the next segment to read.
			 */
			if( dataServerSocket == null ) {
				nameServerOutput.println("seek " + cursorHandle + " " + bytePos);
				nameServerOutput.flush();
				status = nameServerInput.readLine();
				throw new IOException(status);
			}

			String resourceName = status.substring(2);

			if( DEBUG )
				System.out.println("Seek from " + resourceName);

			while( true ) {

				StringTokenizer segmentList = new StringTokenizer(status);

				/*
				 * Skip the "+"
				 */
				segmentList.nextToken();

				/*
				 * Create a connection to the data server now.  If there is a connection
				 * token for the data server, get that from the segment list now.
				 */

				String segment = segmentList.nextToken();

				String authenticationToken = "";
				if( segment.startsWith("@")) {
					authenticationToken = segment;
					segment = segmentList.nextToken();
				}

				String  host = hostName(segment);
				int port = port(segment);
				String path = resourceName(segment);
				String bytePosition = segmentList.nextToken();
				dataServerSocket = new Socket();
				dataServerSocket.setPerformancePreferences(0, 1, 2);
				dataServerSocket.setReceiveBufferSize(SPOOL_READ_SIZE);

				dataServerSocket.connect(new InetSocketAddress(host, port));

				dataServerOutput = new ObjectOutputStream( dataServerSocket.getOutputStream());
				dataServerInput = new ObjectInputStream( dataServerSocket.getInputStream());

				if( DEBUG )
					System.out.println("Debug: sending dataserver command:  read <authtoken> "  + path + " " + bytePosition);

				dataServerOutput.writeObject("read " + authenticationToken + " " + path + " " + bytePosition);
				/*
				 * Get the control byte that says if the segment opened correctly.  This will
				 * be zero for success, or non-zero indicating the rest of the stream contains
				 * an error message.
				 */
				count = this.dataServerInput.read(bufferedData, 0, 1);
				if( bufferedData[0] != 0 ) {

					/*
					 * Darn, there was an error opening the segment on the
					 * remote side.  Get the error message text.
					 */
					count = this.dataServerInput.read(bufferedData);
					String msg = new String(bufferedData);
					if( DEBUG) 
						System.out.println("Remote dataserver reports error, " + msg);
					/*
					 * Move to the next segment in the list by reporting
					 * a read failure and a need to find a different
					 * segment.
					 */

					try {
						nameServerOutput.println("read again " + cursorHandle + " " + bytePosition);
						 
						nameServerOutput.flush();
						status = nameServerInput.readLine();
						if( status.startsWith("-")) {
							throw new IOException(status);
						}
						continue; /* Try again with new segment info */

					} catch ( Exception e ) {
						/*
						 * We are out of usable segments, throw an error
						 */
						throw new IOException( "- segment seek error; all segments unusable");
					}

				}
				break;
			}
		}

		/*
		 * We have a connect to a data server, so read more data.  If we read some data, then
		 * return the size of the data to the caller and we're done.
		 */

		bufferMax = dataServerInput.read(bufferedData);
	}


	
	
	/**
	 * Read a buffer full of data from the file.
	 * @param buffer the buffer into which to read the data
	 * @return the count of bytes read, or zero if there is no more data.
	 * @throws IOException if an I/O error occurs.
	 */
	public int read( byte[] buffer) throws IOException {
		int count = 0;

		if( bufferedData == null ) {
			bufferedData = new byte[SPOOL_READ_SIZE];
			bufferPos = -1;
			bufferMax = 0;
		}
		count = 0;

		while( count < buffer.length){ 
			/*
			 * First, copy as much data in our buffer as will fit in the caller's buffer.
			 */

			if( bufferPos > -1) {
				for( ; count < buffer.length; count++ ) {
					if( bufferPos >= bufferMax) {
						bufferPos = -1;
						break;
					}
					buffer[count] = bufferedData[bufferPos++];
				}
			}

			/*
			 * Is there more room in the caller's buffer?
			 */
			if( count < buffer.length) {
				bufferMax = readSegment(bufferedData);

				/*
				 * If end-of-file, then we're done - return what we have so far.
				 */
				if( bufferMax == 0 )
					return count;
				bufferPos = 0;
			}
		}
		return count;
	}

	/**
	 * Execute a command on the nameserver and return the reply as a single
	 * large string.  This interface will be eventually be deprecated as it
	 * would permit users to disrupt the operation of the server.
	 * @param cmd The command text to execute
	 * @return a string containing the reply
	 * @throws IOException if a socket error occurs or the connection is not
	 */
	public String command( String cmd ) throws IOException {

		if( !valid )
			throw new IOException("- no connection to server");
		if( nameServerSocket == null )
			throw new IOException("- no connection to nameserver");

		nameServerOutput.println(cmd);
		nameServerOutput.flush();

		StringBuffer result = new StringBuffer();
		if( DEBUG )
			result.append("Executing command " + cmd + "\n");

		String reply = nameServerInput.readLine();
		while( reply != null && reply.length() > 0 && (reply.charAt(0) != '-' && reply.charAt(0) != '+')) {
			result.append(reply);
			result.append("\n");
			reply = nameServerInput.readLine();
		}
		if( reply != null )
			result.append(reply + "\n");
		return result.toString();
	}


	/**
	 * Get an XML string that describes a domain and it's contents.
	 * @param domain The domain to gather info on
	 * @return an XML string containing the reply
	 * @throws IOException if a socket error occurs or the connection is not
	 */
	public String describeDomain( String domain ) throws IOException {

		if( !valid )
			throw new IOException("- no connection to server");
		if( nameServerSocket == null )
			throw new IOException("- no connection to nameserver");

		nameServerOutput.println("domain " + domain);
		nameServerOutput.flush();
		String reply = nameServerInput.readLine();
		if( reply.startsWith("-")) {
			status = reply;
			return null;
		}

		
		int handle = Integer.parseInt(reply.substring(1).trim());
		nameServerOutput.println("getxml " + handle + " 1");
		nameServerOutput.flush();
		
		reply = nameServerInput.readLine();
		if( reply.startsWith("-")) {
			status = reply;
			return null;
		}

		return reply.substring(1);
	}

	/**
	 * Get an XML string that describes a domain and it's contents.
	 * @param domain The domain to gather info on
	 * @return an XML string containing the reply
	 * @throws IOException if a socket error occurs or the connection is not
	 */
	public String describeFile( String domain ) throws IOException {

		if( !valid )
			throw new IOException("- no connection to server");
		if( nameServerSocket == null )
			throw new IOException("- no connection to nameserver");

		String reply = null;
		
		nameServerOutput.println("file " + domain);
		nameServerOutput.flush();
		reply = nameServerInput.readLine();
		if( reply.startsWith("-")) {
			status = reply;
			return null;
		}

		
		int handle = Integer.parseInt(reply.substring(1).trim());
		nameServerOutput.println("getxml " + handle + " 1");
		nameServerOutput.flush();
		
		reply = nameServerInput.readLine();
		if( reply.startsWith("-")) {
			status = reply;
			return null;
		}

		return reply.substring(1);
	}

	/**
	 * Read a segment from the file.  The data is returned in chunks
	 * in a byte array.
	 * @return number of bytes actually read into the array, or zero
	 * if there is no more data to read.
	 * @throws IOException if there is no connection to the server,
	 * the file is not open, or the user does not have permission
	 * to read the data.
	 */
	private int readSegment( byte[] buffer ) throws IOException {
		if( mode != MODE_INPUT)
			throw new IOException("- file not opened for input");
		if( buffer.length < 1 )
			throw new IOException("- invalid zero-sized buffer");

		if( !valid )
			throw new IOException("- no connection to server");
		if( nameServerSocket == null )
			throw new IOException("- no connection to nameserver");
		if(fileHandle < 0 )
			throw new IOException("- file not open");

		/*
		 * Do we have a cursor we're using for all this?  If not, 
		 * be sure to get one.
		 */
		if( cursorHandle < 0 ) {
			nameServerOutput.println("cursor " + fileHandle);
			nameServerOutput.flush();
			status = nameServerInput.readLine();
			if( status.startsWith("-"))
				throw new IOException(status);
			StringTokenizer cmd = new StringTokenizer(status);

			/*
			 * Skip the "+" and then parse the handle number.  Then
			 * all done with this buffer; toss it away.  The rest of
			 * the buffer contains the cursor description which we 
			 * don't need.
			 */
			cmd.nextToken();
			cursorHandle = Integer.parseInt(cmd.nextToken());
			cmd = null;
		}

		int count = 0;

		while( count <= 0 ) {

			/*
			 * Is there an active connection to a data server? If not,
			 * use the cursor to find the next segment to read.
			 */
			if( dataServerSocket == null ) {
				nameServerOutput.println("read " + cursorHandle);
				nameServerOutput.flush();
				status = nameServerInput.readLine();
				if( status.startsWith("-")) {
					if( status.equals("- EOF"))
						return 0;

					throw new IOException(status);
				}
				String resourceName = status.substring(2);

				if( DEBUG )
					System.out.println("Read from " + resourceName);

				while( true ) {

					StringTokenizer segmentList = new StringTokenizer(status);

					/*
					 * Skip the "+"
					 */
					segmentList.nextToken();

					/*
					 * Create a connection to the data server now.  If there is a connection
					 * token for the data server, get that from the segment list now.
					 */

					String segment = segmentList.nextToken();

					String authenticationToken = "";
					if( segment.startsWith("@")) {
						authenticationToken = segment;
						segment = segmentList.nextToken();
					}

					String  host = hostName(segment);
					int port = port(segment);
					String path = resourceName(segment);
					
					dataServerSocket = new Socket();
					dataServerSocket.setPerformancePreferences(0, 1, 2);
					dataServerSocket.setReceiveBufferSize(SPOOL_READ_SIZE);
					
					dataServerSocket.connect(new InetSocketAddress(host, port));
					
					dataServerOutput = new ObjectOutputStream( dataServerSocket.getOutputStream());
					dataServerInput = new ObjectInputStream( dataServerSocket.getInputStream());

					if( DEBUG )
						System.out.println("Debug: sending dataserver command:  read <authtoken> "  + path);

					dataServerOutput.writeObject("read " + authenticationToken + " " + path);
					/*
					 * Get the control byte that says if the segment opened correctly.  This will
					 * be zero for success, or non-zero indicating the rest of the stream contains
					 * an error message.
					 */
					count = this.dataServerInput.read(buffer, 0, 1);
					if( buffer[0] != 0 ) {

						/*
						 * Darn, there was an error opening the segment on the
						 * remote side.  Get the error message text.
						 */
						count = this.dataServerInput.read(buffer);
						String msg = new String(buffer);
						if( DEBUG) 
							System.out.println("Remote dataserver reports error, " + msg);
						/*
						 * Move to the next segment in the list by reporting
						 * a read failure and a need to find a different
						 * segment.
						 */

						try {
							nameServerOutput.println("read again " + cursorHandle);
							nameServerOutput.flush();
							status = nameServerInput.readLine();
							if( status.startsWith("-")) {
								if( status.equals("- EOF"))
									return 0;

								throw new IOException(status);
							}
							continue; /* Try again with new segment info */

						} catch ( Exception e ) {
							/*
							 * We are out of usable segments, throw an error
							 */
							throw new IOException( "- segment read error; all segments unusable");
						}

					}
					break;
				}
			}

			/*
			 * We have a connect to a data server, so read more data.  If we read some data, then
			 * return the size of the data to the caller and we're done.
			 */

			count = this.dataServerInput.read(buffer);
			//if( count < 0 ) {
			//	throw new IOException("- unexpected dataserver buffer size of " + count );
			//}
			if( count > 0 )
				return count;

			/*
			 * No data was read, which means we hit end-of-file on this segment, so loop to 
			 * find the next segment. Zero out the connection to this data server segment 
			 * so we find the next one via the active cursor.
			 */
			dataServerInput.close();
			dataServerOutput.close();
			dataServerSocket.close();
			dataServerSocket = null;
		}

		return count;
	}

	/**
	 * Terminate the connection to the server.
	 */
	public void terminate() {
		if( !valid )
			return;
		if( nameServerSocket == null )
			return;
		if( fileHandle >= 0 )
			try {
				close();
			} catch (IOException e) {
				if( DEBUG )
					e.printStackTrace();
			}
		
		try {
			nameServerInput.close();
			nameServerOutput.close();
			nameServerSocket.close();
		} catch (IOException e) {
			if( DEBUG )
				e.printStackTrace();
		}
		
		valid = false;
		nameServerSocket = null;
		nameServerInput = null;
		nameServerOutput = null;
	}

	/**
	 * Close the open file object.
	 * @throws IOException if there is no connection to the server or the
	 * file is not open.
	 */
	public void close() throws IOException {
		if( !valid )
			throw new IOException("- no connection to server");
		if( nameServerSocket == null )
			throw new IOException("- no connection to nameserver");
		if(fileHandle < 0 )
			throw new IOException("- file not open");

		if( spoolFileSize > 0 ) {
			if( DEBUG )
				System.out.println("Debug: closing, flushing spool file " + spoolName);
			flush();
		}

		/*
		 * If the data server is still hanging around, close it.
		 */
		if( dataServerInput != null ) {
			dataServerInput.close();
			dataServerInput = null;
		}
		if( dataServerOutput != null ) {
			dataServerOutput.close();
			dataServerOutput = null;
		}
		if( dataServerSocket != null ) {
			dataServerSocket.close();
			dataServerSocket = null;
		}

		if( nameServerOutput == null )
			return;

		/*
		 * Tell the name server to update it's metadata
		 */
		nameServerOutput.println("commit " + fileHandle);
		nameServerOutput.flush();
		status = nameServerInput.readLine();
		if( status == null )
			return;

		if( status.startsWith("-"))
			throw new IOException(status);
		if( DEBUG)
			System.out.println("Debug: file commit reply is " + status);
		fileHandle = -1;
		cursorHandle = -1;
		mode = 0;
		return;
	}

	/**
	 * Write data to the output file.  The data to be written is presented
	 * as an array of bytes.
	 * @param bytes the data to write.
	 * @param length number of bytes to write
	 * @throws IOException if an error occurs.
	 */
	public void write(byte[] bytes, int length) throws IOException {

		if( mode != MODE_OUTPUT)
			throw new IOException("- file not opened for output");

		if( spoolFile == null ) {
			spoolName = File.createTempFile(SPOOL_NAME_PREFIX, SPOOL_NAME_SUFFIX).getAbsolutePath();
			spoolFileSize = 0L;
			spoolFile = new FileOutputStream(spoolName);
			if( DEBUG )
				System.out.println("Debug: created spool file " + spoolName);
		}

		spoolFile.write(bytes, 0, length);
		spoolFileSize += length;
		if( spoolFileSize < dataSegmentSize)
			return;

		if( DEBUG )
			System.out.println("Debug: spool file full, flushing " + spoolName);

		spoolFile.flush();
		spoolFile.close();
		flush();

	}


	/**
	 * Write data to the open file instance.  The data is written from the current
	 * temporary spool file.
	 * @throws IOException if there is no connection to the server or the 
	 * file is not open or not authorized for write access by the current
	 * user.
	 */

	
	/**
	 * Write data to the open file instance.  The data is written from the current
	 * temporary spool file.
	 * @throws IOException if there is no connection to the server or the 
	 * file is not open or not authorized for write access by the current
	 * user.
	 */
	private void flush() throws IOException {
		if( mode != MODE_OUTPUT)
			throw new IOException("- file not opened for output");
		if( !valid )
			throw new IOException("- no connection to server");
		if( nameServerSocket == null )
			throw new IOException("- no connection to nameserver");
		if(fileHandle < 0 )
			throw new IOException("- file not open");

		if( DEBUG )
			System.out.println("Debug: sending nameserver command:  alloc " + fileHandle);

		long blockSize = 0;
		
		nameServerOutput.println("alloc " + fileHandle);
		nameServerOutput.flush();
		status = nameServerInput.readLine();
		if( status.startsWith("-"))
			throw new IOException(status);

		if( DEBUG )
			System.out.println("Debug: nameserver replies with " + status);

		StringTokenizer segmentList = new StringTokenizer(status);
		String reply = null;

		/*
		 * Skip the "+"
		 */
		segmentList.nextToken();
		int segmentHandle = Integer.parseInt(segmentList.nextToken());

		/*
		 * Create a connection to the data server now.  If there is a connection
		 * token for the data server, get that from the segment list now.
		 */

		String segment = segmentList.nextToken();

		String authenticationToken = "";
		if( segment.startsWith("@")) {
			authenticationToken = segment;
			segment = segmentList.nextToken();
		}

		String  host = hostName(segment);
		int port = port(segment);
		String path = resourceName(segment);
		
		/*
		 * Set up the data socket connection we'll use to talk
		 * to the DataServer. For all sockets, we want big buffers
		 * and a priority on bandwidth because we want to shovel
		 * lots of data as fast as possible
		 */
		dataServerSocket = new Socket( /* host, port */ );
		dataServerSocket.setReceiveBufferSize(65536);
		dataServerSocket.setPerformancePreferences(0, 1, 2);
		dataServerSocket.connect(new InetSocketAddress(host, port));
		
		/*
		 * For communication exchanges, we use an ObjectDataStream that
		 * conveniently lets us send strings, integers, etc. without
		 * adding a lot of metadata to the protocol.
		 */
		dataServerOutput = new ObjectOutputStream( dataServerSocket.getOutputStream());
		dataServerInput = new ObjectInputStream( dataServerSocket.getInputStream());
		StringBuffer remainder= new StringBuffer();
		while( segmentList.hasMoreTokens()) 
			remainder.append(segmentList.nextToken() + " ");

		
		if( DEBUG ) {
			System.out.println("Debug: socket buffer size is " + dataServerSocket.getReceiveBufferSize());
			System.out.println("Debug: sending dataserver command:  write <authtoken> " 
					+ spoolFileSize + " " + path + " " + remainder);
		}

		dataServerOutput.writeObject("write " + authenticationToken + " " 
				+ spoolFileSize + " " + path + " " + remainder);
		dataServerOutput.flush();
		
		FileInputStream fis = new FileInputStream(spoolName);
		
		byte[] bytes = new byte[SPOOL_READ_SIZE];
		int pos = 0;
		
		/*
		 * The actual data is sent over a buffered stream object that doesn't
		 * do object serialization (no need, just overhead) and lets us use
		 * the much larger buffering size.
		 */
		BufferedOutputStream net = new BufferedOutputStream(dataServerSocket.getOutputStream());
		
		/*
		 * Read as the buffer gets filled.  When the buffer fills
		 * up, go ahead and write it to the dataserver.
		 */
		while( true ) {
			int count = fis.read(bytes, pos, SPOOL_READ_SIZE-pos);
			if( count <= 0 ) {
				if( pos > 0 ) {
					if( DEBUG )
						System.out.println("Writing " + pos + " bytes over network");
					// dataServerOutput.write(bytes, 0, pos+1);
					net.write(bytes, 0, pos);
				}
				break;
			}
			blockSize += count;
			pos += count;
			if( pos >= SPOOL_READ_SIZE) {
				if( DEBUG )
					System.out.println("Writing " + SPOOL_READ_SIZE + " bytes over network");
				// dataServerOutput.write(bytes, 0, SPOOL_READ_SIZE);
				net.write(bytes, 0, SPOOL_READ_SIZE);
				pos = 0;
			}
		}
		net.flush();
		dataServerOutput.flush();

		/*
		 * Done reading from the input spool file.
		 */
		fis.close();
		fis = null;
		/*
		 * Since we're all done with the spool file, so delete the physical file and reset the
		 * handle and size values.
		 */

		new File(spoolName).delete();
		spoolFile = null;
		spoolFileSize = 0L;

		try {
			reply = (String) dataServerInput.readObject();
		} catch (ClassNotFoundException e1) {
			throw new IOException("object protocol error");
		}
		if( DEBUG )
			System.out.println("Debug: Dataserver replies with " + reply);

		dataServerSocket.close();

		if( reply.startsWith("-")) {
			if( DEBUG )
				System.out.println("Debug: sending nameserver command: rollback " + fileHandle);
			nameServerOutput.println("rollback " + fileHandle);
			nameServerOutput.flush();

			String reply2 = nameServerInput.readLine();
			if( DEBUG )
				System.out.println("Debug: rollback response was " + reply2);
			
			throw new IOException(reply);
		}

		if( DEBUG )
			System.out.println("Debug: sending nameserver command:  commit " + segmentHandle);

		nameServerOutput.println("commit " + segmentHandle + " size " + blockSize);
		nameServerOutput.flush();

		reply = nameServerInput.readLine();

		if( reply.startsWith("-"))
			throw new IOException(reply);
		if( DEBUG)
			System.out.println("Debug: reply from commit is " + reply);
	}
	
	
	private int port ( String resourceName ) {
		int pos = resourceName.indexOf(':');
		if( pos < 0 )
			return 0;

		int pos2 = resourceName.indexOf('/');
		if( pos < 0 )
			return 0;
		return Integer.parseInt(resourceName.substring(pos+1,pos2));
	}


	private String hostName( String resourceName ) {
		int pos = resourceName.indexOf(':');
		if( pos < 0 )
			pos = resourceName.indexOf('/');
		if( pos < 0 )
			return null;
		return resourceName.substring(0,pos);
	}

	private String resourceName( String resourceName ) {
		int pos = resourceName.indexOf('/');
		if( pos < 0 )
			return resourceName;
		return resourceName.substring(pos);
	}

	/**
	 * Write an entire buffer (no length specified)
	 * @param bbuff the buffer to write
	 * @throws IOException if a write error occurs
	 */
	public void write(byte[] bbuff) throws IOException {
		write( bbuff, bbuff.length);

	}
}

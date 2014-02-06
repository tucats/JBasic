/*
 * THIS SOURCE FILE IS PART OF JBASIC, AN OPEN SOURCE PUBLICLY AVAILABLE
 * JAVA SOFTWARE PACKAGE HOSTED BY SOURCEFORGE.NET
 * 
 * THIS SOFTWARE IS PROVIDED VIA THE GNU PUBLIC LICENSE AND IS FREELY
 * AVAILABLE FOR ANY PURPOSE COMMERCIAL OR OTHERWISE AS LONG AS THE AUTHORSHIP
 * AND COPYRIGHT INFORMATION IS RETAINED INTACT AND APPROPRIATELY VISIBLE
 * TO THE END USER.
 * 
 * SEE THE PROJECT FILE AT HTTP://WWW.SOURCEFORGE.NET/PROJECTS/JBASIC FOR
 * MORE INFORMATION.
 * 
 * COPYRIGHT 2003-2011 BY TOM COLE, TOMCOLE@USERS.SF.NET
 *
 */
package org.fernwood.jbasic.runtime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * Subclass of JBasicFile. This handles PIPE files, which are subprocesses
 * that run host shell commands.  The input and output of the subprocess are
 * managed by using the file methods.  For example, println() will send a
 * line of text to the subprocess, while read() will read a line of output
 * from the subprocess.  Binary mode IO is not supported.
 * 
 * @author cole
 * 
 */
public class JBFSocket extends JBasicFile {


	/**
	 * The buffered reader used to handle all input.
	 */
	private BufferedReader stdin;

	/**
	 * Generic file output stream, for writing data to the socket if needed.
	 */
	BufferedWriter wout;


	/**
	 * A string buffer that may hold the next recordValue if lookahead is
	 * active. This happens when an EOF test is done, for example, which tries
	 * to read a recordValue into this buffer, and tests the result. However, to
	 * avoid file positioning challenges, subsequent reads just fetch this
	 * buffer value.
	 */
	private String buffer;

	/**
	 * A string buffer that holds pending output.  If you PRINT to the socket
	 * without a trailing new line, the data is just put in this buffer
	 * instead of actually written.  This prevents the socket message from
	 * being sent until the buffer is "complete"
	 */
	
	private StringBuffer outputBuffer;
	
	/**
	 * Flag indicating if the lookahead buffer field is valid or not. When set,
	 * the next read just uses the buffer string and clears this flag; when not
	 * set an actual read from the input file occurs.
	 */
	private boolean fReadAhead;

	/**
	 * This is the socket number (SERVER or CLIENT)
	 */

	int socketNumber;

	String hostName;

	ServerSocket serverSocket;
	Socket clientSocket;


	/**
	 * Create a new bidirectional network socket file.
	 * @param jb the JBasic session that owns this file.
	 */
	public JBFSocket(final JBasic jb) {
		super(jb);
		jbenv = jb;
		mode = JBasicFile.UNDEFINED;

	}

	/**
	 * Format the file as a string. This is overridden from the JBasicFile class
	 * to add the additional information about the shell command executed.
	 * 
	 * @return A formatted string describing the state of the
	 *         JBasicFile::JBFInput object.
	 */
	public String toString() {
		String result = "JBFSocket ";
		if( serverSocket != null )
			result = result + "ServerSocket=" + socketNumber;
		else
			result = result + "ClientSocket=" + socketNumber;
		if (fReadAhead)
			result = result + ", readahead active";
		if (lastStatus != null)
			result = result
			+ ", status="
			+ (lastStatus.success() ? "success"
					: ("failed:" + lastStatus.getCode()));
		return result;
	}

	/**
	 * Open the input file, using a provided file name. This operation must be
	 * performed before the file can be used for any I/O operation.
	 * 
	 * @param fn
	 *            The external physical file name, which really encodes the
	 *            mode and physical socket number.
	 * @throws JBasicException   if an I/O error occurs or the end of file is reached.
	 */
	public void open(final Value fn, final SymbolTable symbols) throws JBasicException {

		String command = fn.getString().toUpperCase();
		boolean client;
		hostName = null;
		
		if( command.equals(JBasic.CONSOLE_NAME)) {
			throw new JBasicException(Status.FILECONSOLE);
		}

		if( command.indexOf("CLIENT/") == 0 ) {
			client = true;
			hostName = command.substring(7);
			int i = hostName.indexOf(":");
			if( i >= 0 ) {
				socketNumber = Integer.parseInt(hostName.substring(i+1));
				if( i < 1 )
					hostName = "localhost";
				else
					hostName = hostName.substring(0,i-1);
			}
			else {
				socketNumber = Integer.parseInt(hostName);
				hostName = "localhost";
			}
		}
		else
			if( command.indexOf("SERVER/") == 0 ) {
				client = false;
				socketNumber = Integer.parseInt(command.substring(7));
			}
			else
				throw new JBasicException(Status.SOCKETMODE, command);


		fReadAhead = false;
		fname = command;
		serverSocket = null;
		clientSocket = null;
		stdin = null;
		wout = null;

		buffer = null;
		mode = JBasicFile.MODE_SOCKET;
		type = FILE;

		jbenv.checkPermission(Permissions.SHELL);
		jbenv.checkPermission(Permissions.FILE_IO);

		if( !client ) {
			try {
				
				clientSocket = JBasic.getAndAcceptServerSocket(socketNumber);

				stdin = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()));
				wout = new BufferedWriter(
						new OutputStreamWriter(
								clientSocket.getOutputStream()));

			} catch (IOException e) {
				throw new JBasicException(Status.SOCKET, e.getLocalizedMessage());
			}

		}
		else {
			try {
				clientSocket = new Socket(hostName, socketNumber);
				stdin = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()));
				wout = new BufferedWriter(
						new OutputStreamWriter(
								clientSocket.getOutputStream()));
			} catch (IOException e) {
				throw new JBasicException(Status.SOCKET, e.getLocalizedMessage());
			}

		}

		register();
		fileID.setElement(client, "CLIENT");
		if( client) {
			fileID.setElement(hostName, "HOST");
		}
		fileID.setElement(clientSocket.getLocalPort(), "LOCALPORT");
		fileID.setElement(clientSocket.getPort(), "PORT");
		//fileID.setElement(clientSocket.toString(), "SOCKET");
		lastStatus = new Status(Status.SUCCESS);

	}


	/**
	 * Write a string to the output.
	 * 
	 * @param s
	 *            The string to write to the output socket.
	 */
	public synchronized void print(final String s) {
		lastStatus = new Status(Status.SUCCESS);

		if( outputBuffer == null )
			outputBuffer = new StringBuffer();
		
		outputBuffer.append(s);
	}

	/**
	 * Write a string followed by a newline to the output file. If we are in
	 * column mode, resets the column pointers. IF indentation is occuring, then
	 * indents the next output line as well.
	 * 
	 * @param s
	 *            The string to print to the output buffer.
	 */
	public synchronized void println(final String s) {

		if( outputBuffer == null )
			outputBuffer = new StringBuffer();
		outputBuffer.append(s);
		outputBuffer.append(JBasic.newLine);
		
		lastStatus = new Status();

		
		try {
			wout.write(outputBuffer.toString());
			wout.flush();
			outputBuffer = null;
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			lastStatus.print(jbenv);
		}
	}

	/**
	 * Generate a newline in the output socket.
	 */
	public synchronized void println() {


		if( outputBuffer == null )
			outputBuffer = new StringBuffer();
		outputBuffer.append(JBasic.newLine);
		

		try {
			wout.write(outputBuffer.toString());
			wout.flush();
			outputBuffer = null;
		} catch (final IOException e) {
			final Status sts = new Status(Status.IOERROR, e.toString());
			sts.print(jbenv);
		}
	}

	/**
	 * Read a line from an input file. When the file is exhausted, the routine
	 * returns null.
	 * 
	 * @return A string containing the last recordValue read.
	 */
	public String read() {

		lastStatus = new Status(Status.SUCCESS);

		if (fReadAhead) {
			fReadAhead = false;
			return buffer;
		}

		try {
			buffer = getStdin().readLine();
		} catch (final IOException e) {
			buffer = null;
			lastStatus = new Status(Status.IOERROR, e.toString());

		}
		return buffer;
	}

	/**
	 * Read a Value item from the input stream. The value is read from the
	 * read-ahead buffer, which is filled if necessary. The next token is
	 * removed from the buffer and returned to the caller.
	 * 
	 * @return A Value object containing the token, converted to a numeric type
	 *         if appropriate. The result is null if we are at end-of-file or
	 *         the file was not open for input.
	 * @throws JBasicException   if an I/O error occurs or the end of file is reached.
	 */
	public Value readValue() throws JBasicException {

		Value result = null;
		if (mode != MODE_INPUT) {
			throw new JBasicException(Status.INFILE, fname);
		}

		if (!fReadAhead)
			try {
				fReadAhead = true;
				buffer = getStdin().readLine();
			} catch (final IOException e) {
				buffer = null;
				fReadAhead = false;
				throw new JBasicException(Status.IOERROR, e.toString());
			}
			if (buffer == null)
				throw new JBasicException(Status.EOF);

			final Tokenizer t = new Tokenizer(buffer);
			final String inputValue = t.nextToken();
			switch (t.getType()) {
			case Tokenizer.INTEGER:
				result = new Value(Integer.parseInt(inputValue));
				break;

			case Tokenizer.DOUBLE:
				try {
					result = new Value(Double.parseDouble(inputValue));
				}
				catch (NumberFormatException e ) {
					result = new Value(Double.NaN);
				}
				break;

			default:
				result = new Value(inputValue);
			}

			if (t.testNextToken(Tokenizer.END_OF_STRING)) {
				buffer = null;
				fReadAhead = false;
			} else
				buffer = t.getBuffer().trim();
			return result;
	}

	/**
	 * Test to see if this file is at end-of-file. If there is an active
	 * read-ahead buffer, then the file is not at EOF. (A read-ahead buffer is
	 * caused by a previous call to EOF or an INPUT statement that is reading
	 * multiple variables). If there is no read-ahead buffer, then the method
	 * attempts to read a line into the read-ahead buffer. IF this fails, then
	 * we are at end-of-file. If it succeeds, it marks the read-ahead buffer as
	 * valid and returns true.
	 * 
	 * @return Returns true if there is no more data to be read from the file.
	 *         Returns false if there is more data to be read.
	 */
	public boolean eof() {

		if( fReadAhead ) 
			return false;

		try {
			return !stdin.ready();
		} catch (IOException e) {
			return true;
		}
	}

	public void close() {
		lastStatus = new Status();
		if( clientSocket == null )
			return;

		try {
			if( stdin != null ) {
				stdin.close();
				stdin = null;
			}
			
			if( wout != null ) {
				wout.close();
				wout = null;
			}
						
			if( clientSocket != null ) {
				clientSocket.close();
				clientSocket = null;
			}

		} catch (final IOException e) {
			lastStatus = new Status(Status.FILE, "close error; " + e.getLocalizedMessage());
		}
		super.close();

	}


	/**
	 * Set the given string as the new read-ahead buffer.  This is done
	 * by routines that parse a portion of the buffer (such as the INPUTXML
	 * byte code) but may leave unfinished parts of the buffer to be loaded
	 * by subsequent input operations.
	 * @param newBuffer the string to be stored as the new lookahead buffer.
	 * If this is null, then any existing lookahead buffer is discarded.
	 */
	public void setReadAheadBuffer( String newBuffer ) {

		if( newBuffer == null ) {
			buffer = null;
			fReadAhead = false;
			return;
		}
		if( fReadAhead ) 
			buffer = buffer + newBuffer;
		else
			buffer = newBuffer;

		fReadAhead = true;

	}


	/**
	 * Set the standard input file for this object.
	 * @param stdin a BufferedReader to use as the new input
	 * process for this socket.
	 */
	public void setStdin(BufferedReader stdin) {
		this.stdin = stdin;
	}

	/**
	 * Get the standard input file for this object.
	 * @return a BufferedReader that can be used as the input file
	 */
	public BufferedReader getStdin() {
		return stdin;
	}
}

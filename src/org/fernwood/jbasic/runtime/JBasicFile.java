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

import java.util.Iterator;

import net.wimpi.telnetd.io.BasicTerminalIO;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.value.Value;

/**
 * JBasic files. This class handles the string-based lightweight file structure
 * used by JBasic. This handles things like source file input, and also it
 * handles interactive input from the console. You can open a file, read or
 * write it, and close it. If it is open, you can look up the file and get it's
 * handle by name.
 * <p>
 * The kind of semantic operation performed depends on the file typing being
 * accessed. Most access is either of a <em>text</em> file opened for INPUT or
 * OUTPUT, or a <em>binary</em> file opened for BINARY mode which implies both
 * input and output. The kind of operations you can perform depends on the mode
 * the file was opened in.
 * <p>
 * <br>
 * <table border="1"> <caption><em>File Open Mode Semantics</em></caption>
 * <tr>
 * <td><code><b>INPUT   <td><code>OPEN, INPUT, LINE INPUT, CLOSE</code></tr>
 * <tr><td><code><b>OUTPUT  <td><code>OPEN, PRINT, PRINT USING, CLOSE</code><tr>
 * <tr><td><code><b>BINARY  <td><code>OPEN, GET, PUT, SEEK, CLOSE</code><tr>
 * <tr><td><code><b>DATABASE<td><code>OPEN, PRINT, GET, CLOSE</code><tr>
 * <tr><td><code><b>PIPE</b><td><code>OPEN, PRINT, PRINT USING, INPUT, LINE INPUT, CLOSE</code></tr>
 * </table>
 * <p><br>
 * A special note about DATABASE files.  These are connectors to a JDBC database
 * driver.  Different operations occur with the JDBC interface based on the semantic
 * operations performed.<p><br>
 * 
 * <table border="1">
 * <caption><em>DATABASE Semantics</em></caption>
 * <tr><td><b><code>OPEN  <td>  File name is connection string, make the connection.
 * <tr><td><b><code>PRINT <td>  Text to be printed is a SQL statement which is executed.
 * <tr><td><b><code>GET   <td>  Data to be input is a result set from last statement.
 * <tr><td><b><code>CLOSE <td>  Connection is terminated.
 * </table>
 * <p>
 * No specific database is guaranteed to work with JBasic.  Testing will be done with 
 * MySQL and Firebird. The user must provide access to the JDBC driver in the load
 * path for JBasic to be able to make the connection.
 *
 * <p><br>
 * 
 * Once the sub-classed file types are all created, this will become an abstract class!
 * @author cole
 */

public abstract class JBasicFile {

	JBasic jbenv;

	/**
	 * This is a sequence number used to create a unique integer ID number for
	 * every open file. This is used to track open file handles uniquely, even
	 * when more than one file handle may reference the same physical file. Each
	 * physical file still has a unique file handle.
	 */
	private static int sequenceNumber = 1;

	/**
	 * The name of the file (typically it's path)
	 */

	protected String fname;

	/**
	 * The file mode (INPUT or OUTPUT). This determines what operations can be
	 * done on the file.
	 */
	protected int mode;

	/**
	 * Get the file mode (INPUT or OUTPUT) for the current file handle, which is
	 * used by the caller to validate whether an operation is permitted on the
	 * file.
	 * 
	 * @return The mode value, such as JBasicFile.MODE_INPUT or
	 *         JBasicFile.MODE_BINARY
	 */
	public int getMode() {

		if (mode == MODE_APPEND)
			return MODE_OUTPUT;

		return mode;
	}

	/**
	 * Set the file mode for this handle. This is only done during OPEN
	 * operations.
	 * 
	 * @param newMode
	 *            The mode value, either JBasicFile.INPUT or JBasicFile.OUTPUT.
	 */
	void setMode(final int newMode) {
		mode = newMode;
	}

	/**
	 * The file type (CONSOLE or FILE). This determines which stream objects to
	 * use to read or write data.
	 * 
	 * 
	 */
	protected int type;

	/**
	 * Return the file type. A CONSOLE file is directed to the process stdin or
	 * stdout system files, while a FILE is directed to a physical file on disk.
	 * 
	 * @return JBasicFile.CONSOLE or JBasicFile.FILE
	 */
	int getType() {
		return type;
	}

	/**
	 * Set the file type. This is only done during OPEN processing.
	 * 
	 * @param newType
	 *            JBasicFile.CONSOLE or JBasicFile.FILE
	 */
	void setType(final int newType) {
		type = newType;
	}

	/**
	 * See if the current file object is a CONSOLE or not.
	 * 
	 * @return True if the current file is of type JBasicFile.CONSOLE.
	 */
	public boolean isConsole() {
		return (type == CONSOLE);
	}

	/**
	 * The status of the last operation done on the file. This can be checked
	 * after a read returns a null pointer, for example, to determine the cause
	 * of the error (which is usually end-of-file but could be wrong file mode
	 * for operation, etc.)
	 * 
	 * 
	 */
	protected Status lastStatus;

	/**
	 * Accessor function to get the last status code for this file. This
	 * reflects whatever last operation was done on the file, even for routines
	 * that do not surface a return code directly.
	 * 
	 * @return The Status object for the open file, or null if the file has
	 *         never been operated on.
	 */
	public Status getStatus() {
		return lastStatus;
	}

	/**
	 * Flag indicating if the file is a "system" file or not. A "system" file is
	 * one that is opened by JBasic itself to manage it's operations, such as
	 * the command line console. A system file cannot be closed by a user
	 * program statement, though it can be accessed by a user program via a
	 * PRINT or INPUT operation.
	 */
	private boolean fSystem;

	/**
	 * The identifier used in the OPEN statement to hold a key used to locate
	 * this file object in the language itself. For example, a later PRINT
	 * statement can use the #identifier syntax to indicate which file to write
	 * to. This contains the string expression of this identifier. This is
	 * mostly used for support for users in debugging programs through the
	 * <code>SHOW FILES</code> command.
	 * 
	 * 
	 */
	private String ident;

	/**
	 * The unique file identification record for this file. Each file is
	 * identified by a fileID, which is a Value.RECORD type object. The members
	 * of the object describe the file name, open mode, and unique sequence
	 * number information.
	 */
	protected Value fileID;

	/**
	 * Return the unique file identification record for the open file.
	 * 
	 * @return The Value containing the file identification record
	 */
	public Value getFileID() {
		return fileID;
	}

	/**
	 * The current file can be used for read() method calls, INPUT and LINE
	 * INPUT program statements.
	 */
	static public final int MODE_INPUT = 1;

	/**
	 * The current file can be used for print(), println(), and PRINT program
	 * statements. Any existing contents of the file are overwritten by new
	 * output.
	 */
	static public final int MODE_OUTPUT = 2;

	/**
	 * The current file can be used for print(), println(), and PRINT program
	 * statements. Output will be added to the end of the file when an existing
	 * file is opened in this mode.
	 */
	static public final int MODE_APPEND = 3;

	/**
	 * The file is used for binary (non-ASCII, untyped) data storage. This mode
	 * is not currently implemented, and is reserved for future use.
	 */
	static public final int MODE_BINARY = 4;

	/**
	 * The file is used for JDBC database access. In this case, the file name is
	 * the connect string.
	 */

	static public final int MODE_DATABASE = 5;

	/**
	 * The file is a thread-safe FIFO queue
	 */
	static public final int MODE_QUEUE = 6;

	/**
	 * The current file has not been opened.
	 */
	static public final int MODE_UNDEFINED = 0;

	/**
	 * The current file is a CONSOLE file, mapped to the process stdin or
	 * stdout, and is usually associated with a command-line shell.
	 */
	static public final int CONSOLE = 1;

	/**
	 * The current file is a FILE object, and is associated with a physical file
	 * on the user's file system.
	 */
	static public final int FILE = 2;

	/**
	 * This file type is reserved for future use.
	 */
	static public final int OTHER = 3;

	/**
	 * This file is for FSM files
	 */
	static public final int FSM = 4;
	
	/**
	 * The file type is undefined.
	 */
	static public final int UNDEFINED = 0;

	/**
	 * The file represents a pipe to a spawned command
	 */
	public static final int MODE_PIPE = 7;

	/**
	 * The file represents a bidirectional socket.
	 */
	public static final int MODE_SOCKET = 8;

	/**
	 * Create a new JBasicFile object, initialized to reflect that is has not
	 * been opened or given any identifying characteristics.
	 * 
	 * @param jb
	 *            the JBasic object that contains the active session.
	 */
	public JBasicFile(final JBasic jb) {
		if (jb == null)
			System.out.println("Ouch! file context error!");
		mode = MODE_UNDEFINED;
		type = UNDEFINED;
		jbenv = jb;
		fname = null;
		lastStatus = new Status(Status.SUCCESS);
	}

	/**
	 * Abstract constructor. This should never be called, since if you know the
	 * name of the file you also know what kind of file object to create.
	 * 
	 * @param jb
	 *            The containing JBasic object
	 * @param extName
	 *            The name of the external file name.
	 */
	public JBasicFile(final JBasic jb, final String extName) {
		System.out
				.println("ERROR: Abstract instance of constructor called for JBasicFile!");

	}

	/**
	 * Method used to format a JBasicFile object into a string. This is used in
	 * debugging JBasic in Eclipse, for example.
	 */
	public String toString() {
		String result = "JBasicFile ";
		if (fileID != null)
			result = result + fileID.toString();
		if (lastStatus != null)
			result = result
					+ ", status="
					+ (lastStatus.success() ? "success"
							: ("failed:" + lastStatus.getCode()));
		return result;
	}

	/**
	 * Set the identifier string for the file. This is the name of the variable
	 * that contains the Value.RECORD describing the open file and it's
	 * characteristics. This is set during an OPEN statement to store the file
	 * identifier information.
	 * 
	 * @param s the name of the identifier variable for thsi file.
	 */
	public void setIdentifier(final String s) {
		ident = s;
	}

	/**
	 * Return the name of the symbol containing the Value.RECORD that identifies
	 * this open file. This is used to remove it from the symbol table during a
	 * CLOSE or KILL operation, and also during SHOW FILES to indicate what the
	 * identifier variable is.
	 * 
	 * @return The name of a symbol containing the identifier data.
	 */
	public String getIdentifier() {
		return ident;
	}

	/**
	 * Return a string containing the physical file name of this file object.
	 * For example, this is used by the KILL statement to kill a file located
	 * via a file identifier object.
	 * 
	 * @return String containing the physical file name.
	 */
	public String getName() {
		return fname;
	}

	/**
	 * Set the flag indicating that this file is owned by the JBasic runtime
	 * environment rather than under control of a user program.
	 */
	public void setSystemFlag() {
		fSystem = true;
		if( this.fileID != null ) {
			fileID.setElement(new Value(true), "SYSTEM");
		}
	}

	/**
	 * Get the system flag
	 * 
	 * @return True if the file is owned by the system. False if the file is
	 *         under the control of a user program.
	 */
	boolean getSystemFlag() {
		return fSystem;
	}

	/**
	 * Generate a FILE ID handle.
	 * 
	 * @return A Value object containing a record that uniquely identifies this
	 *         file.
	 */
	Value generateID() {
		return fileID = generateID(fname, mode);
	}

	/**
	 * Generate a Value.RECORD object that contains the information about the
	 * currently open file.
	 * 
	 * @param fname
	 *            The physical file name that should be represented in the
	 *            identifier.
	 * @param extMode
	 *            The physical file mode that is represented in the identiifer.
	 * @return A Value record object containing the minimal required identifier
	 *         information.
	 */
	static Value generateID(final String fname, final int extMode) {

		final String[] modes = { "UNDEFINED", "INPUT", "OUTPUT", "APPEND",
				"BINARY", "DATABASE", "QUEUE", "PIPE", "SOCKET" };

		final Value d = new Value(Value.RECORD, null);
		d.setElement(new Value(fname), "FILENAME");
		d.setElement(new Value(modes[extMode]), "MODE");
		d.setElement(new Value(JBasicFile.sequenceNumber++), "SEQNO");
		d.setElement(new Value(false), "SYSTEM");
		return d;
	}

	/**
	 * Given a file identifier value (typically retrieved by symbolic name),
	 * find the JBasicFile object that represents it, and return the object.
	 * <p>
	 * The file identifier contains a unique sequence number that is used to
	 * identify each open file. This sequence number is used to perform a lookup
	 * in a TreeMap.
	 * 
	 * @param session
	 *            the JBasic object that contains the active session.
	 * @param fileID
	 *            The Value containing the file identification record
	 * @return The JBasicFile object that the fileID represented, or null if the
	 *         fileID is invalid.
	 * @throws JBasicException if the fileID is not valid or not found.
	 */

	public static JBasicFile lookup(final JBasic session, final Value fileID) throws JBasicException {
		if (fileID.getType() != Value.RECORD) 
			throw new JBasicException( Status.INVFID);

		final Value d = fileID.getElement("SEQNO");
		if (d == null)
			throw new JBasicException( Status.INVFID);

		JBasicFile f =  session.openUserFiles.get(new Integer(d.getInteger()));
		if( f == null )
			throw new JBasicException( Status.INVFID);
		return f;
	}

	/**
	 * Close all user-mode files.  This does not (and cannot) free up
	 * all the symbols that might reference the file, because they are
	 * not necessary.
	 * @param session the JBasic session that owns the files to close
	 * @param s The active symbol table to search for the file identifiers
	 * for files that are closed.  The symbol is deleted from the table.
	 */
	public static void closeUserFiles( final JBasic session, final SymbolTable s) {
				
		Iterator i = session.openUserFiles.values().iterator();
		while( i.hasNext()) {
			JBasicFile f = (JBasicFile) i.next();
			String fileIdentifier = f.getIdentifier();
			
			if( !f.isSystem() & fileIdentifier != null) {
				s.delete(fileIdentifier);
				f.close();
			}
		}
	}
	/**
	 * Construct a file object of the specific kind requested by the external
	 * mode flag. This is one of the file types, such as MODE_INPUT or
	 * MODE_DATABASE. The factory returns a new object of the right kind.
	 * 
	 * @param jb
	 *            The containing session object.
	 * @param extMode
	 *            The mode flag, such as MODE_BINARY
	 * @return A specific JBasicFile object such as a JBFDatabase
	 */
	public static JBasicFile newInstance(final JBasic jb, final int extMode) {

		JBasicFile f = null;

		switch (extMode) {

		case MODE_INPUT:
			return new JBFInput(jb);

		case MODE_OUTPUT:
			return new JBFOutput(jb);

		case MODE_APPEND:
			f = new JBFOutput(jb);
			f.setAppend(true);
			return f;

		case MODE_DATABASE:
			return new JBFDatabase(jb);

		case MODE_BINARY:
			return new JBFBinary(jb);

		case MODE_QUEUE:
			return new JBFQueue(jb);

		case MODE_PIPE:
			return new JBFPipe(jb);
		
		case MODE_SOCKET:
			return new JBFSocket(jb);
			
		default:
			return null;
		}
	}

	/**
	 * @param b
	 */
	private void setAppend(final boolean b) {
		mode = b ? MODE_APPEND : MODE_OUTPUT;
	}

	/**
	 * Internal routine used to deregister a previously opened file in the list
	 * of files that can be access/recovered by name. This is used to hand a
	 * file handle from an OPEN statement to a PRINT statement, for example. The
	 * file name is used as a key to store the object in a TreeMap at
	 * JBasic.files. Whenever a file is successfully opened, it is registered.
	 * When it is closed, it is unregistered.
	 * 
	 */
	private void unregister() {
		if (mode == MODE_UNDEFINED)
			return;

		if (fileID == null) {
			new Status(Status.INVFID).print(jbenv);
			return;
		}

		final Value d = fileID.getElement("SEQNO");
		if (d == null) {
			new Status(Status.INVFID).print(jbenv);
			return;
		}

		jbenv.openUserFiles.remove(new Integer(d.getInteger()));

	}

	/**
	 * Internal routine used to register a successfully opened file in the list
	 * of files that can be access/recovered by name. This is used to hand a
	 * file handle from an OPEN statement to a PRINT statement, for example. The
	 * file name is used as a key to store the object in a TreeMap at
	 * JBasic.files. Whenever a file is successfully opened, it is registered.
	 * When it is closed, it is unregistered.
	 * 
	 */

	protected void register() {
		if (fileID == null)
			fileID = generateID();

		final Value d = fileID.getElement("SEQNO");
		jbenv.openUserFiles.put(new Integer(d.getInteger()), this);
	}

	/**
	 * Open a JBasicFile. This should never be called. as this is a prototype
	 * of the function that must be overridden by each file type.
	 * 
	 * @param value1 the file identifier
	 * @param symbols the symbol table to use to store the open file information.
	 * @throws JBasicException indicates an invalid direct access to the
	 * JBasic file type.
	 */
	public void open(final Value value1, final SymbolTable symbols) throws JBasicException {
		throw new JBasicException(Status.FAULT,
				"subclass of JBasicFile.open()");
	}

	/**
	 * Generic println() implementation that writes a message to the system console.
	 * @param s the string to print
	 */
	public void println(final String s) {
		System.out.println("PRINT: " + s);
	}

	/**
	 * Generic routine that throws an error, because you cannot do input on anything
	 * but a JBFInput subclass of this type.
	 * @return A string indicating an error occurred in usage
	 */
	public String read() {
		return "BAD READ ATTEMPT!";
	}

	/**
	 * Open a stream file for input or output, given the name and the mode. The
	 * mode must be one of MODE_INPUT or MODE_OUTPUT in the current version
	 * (append is not yet supported and there is no binary IO mode yet). <br>
	 * 
	 * Additionally, you can name a file beginning with an at-sign and it will
	 * be searched for as a Java resource in the resource path rather than in
	 * the file system (such as contained within the JAR file itself). Finally,
	 * the special name JBasic.CONSOLE_FILE means to use the System.in and 
	 * System.out console streams. <br>
	 * 
	 * See the documentation on the OpenStatement for additional information on
	 * how JBasicFiles are used in the language.
	 * 
	 * @param extName a file name
	 * @param extMode the file mode (input, output, etc.)
	 * @return Status block indicating an illegal direct access to the
	 * JBasicFile object type.
	 */
	public Status open(final String extName, final int extMode) {
		return lastStatus = new Status(Status.FAULT,
				"subclass of JBasicFile.open()");
	}

	/**
	 * Test to see if this file is at EOF.
	 * 
	 * @return A boolean value indicating if a read() or readValue() method call
	 *         will fail with an end-of-file error. If true, then no further
	 *         data can be read from the file. If false, then there is more data
	 *         in the file.
	 */
	public boolean eof() {
		lastStatus = new Status(Status.FAULT, "subclass of JBasicFile.eof()");
		return true;
	}

	/**
	 * Close a file for input or output. After this call, no additional read or
	 * write operations can be performed on the file until a new open() call is
	 * made. <b>This is normally called from the sub-classed close() method after
	 * the appropriate file-level close has been performed.</b>
	 */
	public void close() {

		/*
		 * Now that we've done any actual file closing work, unregister this as
		 * an open file, and mark the internal variables so we can't
		 * accidentally re-use this object.
		 */
		unregister();
		mode = MODE_UNDEFINED;
		type = UNDEFINED;
		lastStatus = new Status(Status.SUCCESS);
	}

	

	/**
	 * Determine if the current file object is owned by the JBasic system.
	 * If so, it cannot be closed directly by user action
	 * 
	 * @return True if the file is a "system file".
	 */
	public boolean isSystem() {
		return fSystem;
	}

	/**
	 * Stub for a routine that can only be used with JBFInput subclass
	 * @throws JBasicException indicating illegal Terminal I/O on a non-terminal
	 * file type.
	 */
	public void flushReadAhead() throws JBasicException {
		throw new JBasicException(Status.FAULT, "terminal I/O on non-terminal file");
		
	}

	/**
	 * Stub for a routine that can only be used with JBFInput subclass
	 * @return false because this routine isn't functional with a generic
	 * JBasicFile
	 */
	public boolean hasReadAhead() {
		return false;
	}

	/**
	 * Stub for a routine that can only be used with JBFInput subclass
	 * @param session  the parent session of the terminal file
	 * @param m_io  a BasicTerminalIO object that will manage the 
	 * telnet semantics for the connection.
	 * @throws JBasicException Attempt to open a terminal on a non-CONSOLE
	 * file type.
	 */
	public void openTerminal(JBasic session, BasicTerminalIO m_io) throws JBasicException {
		throw new JBasicException(Status.FAULT, "terminal I/O on non-terminal file");
		
	}

	/**
	 * Stub for a routine that can only be used with JBFInput subclass
	 * @param b true of echo is enabled
	 * @throws JBasicException attempted operation on a non-CONSOLE file.
	 */
	public void setEcho(boolean b) throws JBasicException {
		throw new JBasicException(Status.FAULT, "terminal I/O on non-terminal file");
	}

	/**
	 * Stub for a routine that can only be used with JBFInput subclass
	 * @param m_io The BasicTerminalIO handler that manages the terminal
	 * session semantics.
	 * @throws JBasicException attempted operation on a non-CONSOLE file.
	 */
	public void openTerminal(BasicTerminalIO m_io) throws JBasicException {
		throw new JBasicException(Status.FAULT, "terminal I/O on non-terminal file");
			
	}

	/**
	 * Stub for a routine that can only be used with JBFOutput subclass
	 * @param pdata data to be printed
	 */
	public void print(String pdata) {
		// No work.
	}

	/**
	 * Stub for a routine that can only be used with JBFOutput subclass
	 */
	public void println() {
		// No work.
		
	}

	/**
	 * Stub for a routine that can only be used with JBFOutput subclass
	 * @param i  set the number of spaces to indent output
	 */
	public void indent(int i) {
		
	}
	
	/**
	 * Stub for a routine that can only be used with JBFOutput subclass
	 * @param i set the number of columns in tabular output
	 * @param j  set the number of spaces in each column of tabular output
	 */
	public void columnOutput(int i, int j) {
		
	}
	/**
	 * Stub for a routine that can only be used with JBFOutput subclass
	 */
	public void columnOutputEnd() {
		
	}

	/**
	 * Given the name of a file identifier, format it for display.  For most identifiers,
	 * this just involves the identifier name.  For identifiers that start with the 
	 * FILEPREFIX, the name is converted to the #n format.
	 * 
	 * @param fname the file identifier to format for output
	 * @return the formatted identifier name
	 */
	public static String name(String fname) {
		
		int prefixLen = JBasic.FILEPREFIX.length();
		
		if(fname.length() > prefixLen)
			if( fname.substring(0,prefixLen).equals(JBasic.FILEPREFIX))
				return "#" + fname.substring(prefixLen);
		
		return fname;
	}

}
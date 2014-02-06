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
public class JBFPipe extends JBasicFile {

	/**
	 * The buffered reader used to handle all input. This is the buffered reader
	 * from the spawned command thread.
	 */
	private BufferedReader stdin;

	/**
	 * Generic file output stream, for writing data to the pipe if needed.
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
	 * Flag indicating if the lookahead buffer field is valid or not. When set,
	 * the next read just uses the buffer string and clears this flag; when not
	 * set an actual read from the input file occurs.
	 */
	private boolean fReadAhead;

	/**
	 * This is the string that was sent to the subprocess to execute as the
	 * piped operation.
	 */

	private String command;

	/**
	 * Create a new bi-directional stream pipe object.
	 * @param jb the controlling JBasic session that owns the file.
	 */
	public JBFPipe(final JBasic jb) {
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
		String result = "JBFPipe ";
		if (command != null)
			result = result + "Command=\"" + command + "\"";
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
	 *            The command to execute via the pipe, which is encoded as the
	 *            file name.
	 * @throws JBasicException  if an I/O error occurs or the end of file is reached.
	 */
	public void open(final Value fn, final SymbolTable symbols) throws JBasicException {

		/*
		 * Quick sanity check; you cannot open a pipe with the name of the reserved
		 * console name.
		 */
		if( fn.getString().equalsIgnoreCase(JBasic.CONSOLE_NAME)) {
			throw new JBasicException(Status.FILECONSOLE);
		}

		/*
		 * We will store the component parts of the commadn in this array
		 */
		String[] cmdArray = null;
		
		/*
		 * If the command we're given is already expressed as an array, we just copy the array
		 * members (cast to strings if needed) to the command array, and build a fake command
		 * buffer that is blank-padded to store as the file path info.
		 */
		
		if( fn.getType() == Value.ARRAY) {
			StringBuffer tempCommand = new StringBuffer();
			cmdArray = new String[fn.size()];
			for( int i = 0; i < fn.size(); i++ ) {
				cmdArray[i] = fn.getString(i+1);
				if( i > 0)
					tempCommand.append(' ');
				tempCommand.append(cmdArray[i]);
			}
			command = tempCommand.toString();
		}
		
		/*
		 * If we were just given a string to execute, that will be what we use for both the
		 * command and the path.
		 */
		else
			command = fn.getString();
		
		fReadAhead = false;
		fname = command;
		
		buffer = null;
		mode = MODE_PIPE;
		type = FILE;

		jbenv.checkPermission(Permissions.SHELL);
		jbenv.checkPermission(Permissions.FILE_IO);

		final ExecManager e = new ExecManager();
		
		Status sts = null;
		
		/*
		 * If there was a command array, use that. Otherwise, use the version with the
		 * helper function that parses apart the command as best it can using blanks as
		 * delimiters.  This is limited for cases where you want to pass a filename with
		 * embedded blanks; in that case always call using a Value containing an array
		 * of strings that are already broken down by verb and arguments.
		 */
		if( cmdArray == null )
			sts = e.spawn(command);
		else
			sts = e.spawn(cmdArray);
		
		symbols.insert("SYS$STATUS", new Value(sts));
		if( !sts.success())
			throw new JBasicException(sts);

		/*
		 * Now that we've successfully started the pipe, register this as a file handle
		 * that can be found by name and we're done.
		 */
		register();
		lastStatus = new Status(Status.SUCCESS);

	}


	/**
	 * Write a string to the output. If column mode is in effect, calculates if
	 * a newline should be generated after this column is formatted.
	 * 
	 * @param s
	 *            The string to write to the output file.
	 */
	public void print(final String s) {
		lastStatus = new Status(Status.SUCCESS);
		if( wout == null ) {
			lastStatus = new Status(Status.FILE, new Status(Status.WRONGMODE, "PIPE output"));
			lastStatus.print(jbenv);
			return;
		}
		String localBuffer = s;

		try {
			wout.write(localBuffer);
			wout.flush();
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			lastStatus.print(jbenv);
		}
	}

	/**
	 * Write a string followed by a newline to the output file. If we are in
	 * column mode, resets the column pointers. IF indentation is occuring, then
	 * indents the next output line as well.
	 * 
	 * @param s
	 *            The string to print to the output buffer.
	 */
	public void println(final String s) {

		if( wout == null ) {
			lastStatus = new Status(Status.FILE, new Status(Status.WRONGMODE, "PIPE output"));
			lastStatus.print(jbenv);
			return;
		}

		String localBuffer = s;
		lastStatus = new Status();

		try {
			wout.write(localBuffer + JBasic.newLine);
			wout.flush();
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			lastStatus.print(jbenv);
		}
	}

	/**
	 * Generate a newline in the output file. If in column mode, also resets the
	 * column output counters.
	 */
	public void println() {
		if( wout == null ) {
			lastStatus = new Status(Status.FILE, new Status(Status.WRONGMODE, "PIPE output"));
			lastStatus.print(jbenv);
			return;
		}

		try {
			wout.write(JBasic.newLine);
			wout.flush();
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			lastStatus.print(jbenv);
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
	 * @throws JBasicException  if an I/O error occurs or the end of file is reached.
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
		/*
		 * If there is a read-ahead buffer active, then we are not at eof.
		 */
		if (fReadAhead)
			return false;

		/*
		 * We'll have to read a line to see if we're at the end of file.
		 */

		read();

		/*
		 * If the buffer comes back empty, then we are at the end of the file.
		 * In this case, return 'true' and we are done.
		 */
		if (buffer == null)
			return true;

		/*
		 * Since we succeeded in reading a line, we are not at the end of the
		 * file. In this case, leave the buffer alone, but mark that it is
		 * active so the next read will not do another IO. Then tell the caller
		 * that we are not at EOF.
		 */
		fReadAhead = true;

		return false;
	}

	public void close() {
		lastStatus = new Status();
		try {
			getStdin().close();
		} catch (final IOException e) {
			lastStatus = new Status(Status.FILE, "close error " + e);
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
		else {
			buffer = newBuffer;
			fReadAhead = true;
		}
	}


	/**
	 * Set the standard input file for this object.
	 * @param newStdin a BufferedReader to be used as the 
	 * new input stream for this pipe.
	 */
	public void setStdin(BufferedReader newStdin) {
		stdin = newStdin;
	}

	/**
	 * Get the standard input file for this object.
	 * @return a BufferedReader that can be used as the input file
	 */
	public BufferedReader getStdin() {
		return stdin;
	}



	/**
	 * Object for handling execution context. Has only one subclass that does
	 * the actual work (the spawn(cmd) method).
	 * 
	 * @author tom
	 * @version version 1.0 Aug 16, 2006
	 * 
	 */
	public class ExecManager {
		

		Status spawn(final String[] realCommand) {

			try {
				final String osName = System.getProperty("os.name");
				String[] cmd = null;
				boolean onWindows = false;
				final String myOS = osName;
				String winName;
				
				try {
					winName = myOS.substring(0, 7);
				}
				catch (Exception e ) {
					winName = null;
				}
				if (winName != null && winName.equals("Windows")) { 
					onWindows = true;
					cmd = new String[realCommand.length+2];
					cmd[0] = myOS.equals("Windows 95") ? "command.com" : "cmd.exe";
					cmd[1] = "/C";
					for( int i = 0; i < realCommand.length; i++ )
						cmd[i+2] = realCommand[i];
				}

				final Runtime rt = Runtime.getRuntime();
				Process proc = null;

				try {
					if (onWindows)
						proc = rt.exec(cmd);
					else
						proc = rt.exec(realCommand);
				} catch (final Throwable t) {
					final Status sts = new Status(Status.EXEC, t.getLocalizedMessage());
					return sts;
				}

				InputStreamReader isr = new InputStreamReader(proc.getInputStream());
				setStdin(new BufferedReader(isr));
				
				OutputStreamWriter osr = new OutputStreamWriter(proc.getOutputStream());
				wout = new BufferedWriter(osr);

			} catch (final Throwable t) {
				return new Status(Status.FAULT, t.toString());
			}
			return new Status();
		}
		
		
		Status spawn(final String realCommand) {

			try {
				final String osName = System.getProperty("os.name");
				final String[] cmd = new String[3];
				boolean onWindows = false;
				final String myOS = osName;
				String winName;
				
				try {
					winName = myOS.substring(0, 7);
				}
				catch (Exception e ) {
					winName = null;
				}
				if (winName != null && winName.equals("Windows")) { 
					onWindows = true;
					cmd[0] = myOS.equals("Windows 95") ? "command.com" : "cmd.exe";
					cmd[1] = "/C";
					cmd[2] = realCommand;
				}

				final Runtime rt = Runtime.getRuntime();
				Process proc = null;

				try {
					if (onWindows)
						proc = rt.exec(cmd);
					else
						proc = rt.exec(realCommand);
				} catch (final Throwable t) {
					final Status sts = new Status(Status.EXEC, t.getLocalizedMessage());
					return sts;
				}

				InputStreamReader isr = new InputStreamReader(proc.getInputStream());
				setStdin(new BufferedReader(isr));
				
				OutputStreamWriter osr = new OutputStreamWriter(proc.getOutputStream());
				wout = new BufferedWriter(osr);

			} catch (final Throwable t) {
				return new Status(Status.FAULT, t.toString());
			}
			return new Status();
		}
	}


/**
 * Close the WRITE side of a pipe.  This lets a pipe be opened for input 
 * and output, and the output side (that which is writing to the underlying
 * pipe) can be closed, which sends an EOF to the underlying process.  The 
 * file can still be read for input, but not written to after this operation.
 * 
 * @return a Status value indicating if the PIPE write-side was closed 
 * successfully.  Failures could include a system write failure while
 * flushing the pipe, or attempting this operation on a PIPE whose
 * write side is already closed.
 */
	public Status closePipe() {

		if( wout == null ) {
			lastStatus = new Status(Status.FILE, new Status(Status.FNOPENOUTPUT));
			return lastStatus;
		}
		try {
			wout.flush();
			wout.close();
			wout = null;
		} catch (IOException e ) {
			lastStatus = new Status(Status.FILE, new Status(Status.FAULT, e.toString()));
			return lastStatus;
		}
		lastStatus = new Status();
		return lastStatus;
	}
}



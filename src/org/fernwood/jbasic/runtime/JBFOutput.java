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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Date;

import net.wimpi.telnetd.io.BasicTerminalIO;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.value.Value;


/**
 * Subclass of JBasicFile. This handles text output files only. Supported
 * methods are open(), print(), println(), and close();
 * 
 * This class supports OUTPUT file I/O, which means that data is written (not
 * read) as human-readable text to the external file, which must be accessible
 * to the user via a file name.
 * <p>
 * The operations that can be performed on an INPUT file are OPEN, CLOSE, and
 * PRINT. The print and println methods here write a single buffer of text to
 * the file, with carriage control in the println() case. The runtime bytecode
 * is responsible for creating a formatted buffer as part of the execution of
 * the PRINT command, and that buffer is sent here for output.
 * <p>
 * An additional feature of OUTPUT files is support for COLUMNS. The file can be
 * specified as having 'n' columns, each of width 'm' characters. PRINT
 * operations without carriage control are automatically formed into colums,
 * with blanks added to pad out each column. The output can also be
 * automatically indented by a given number of spaces to support automatic
 * nesting of output listings, etc. These features are controlled via extensions
 * to the OPEN statement in the language.
 * 
 * @author cole
 * 
 */
public class JBFOutput extends JBasicFile {
	/**
	 * Console output stream, used to send to System.out for files named
	 * %console.
	 */
	protected PrintStream stdout;

	/**
	 * Generic file output stream, for when the file is other than %console.
	 */
	private BufferedWriter wout;

	/**
	 * FSM File handle when used.
	 */
	private FSMFile fsmFile;
	
	/**
	 * This flag indicates if the file is configured for column output. Column
	 * output means that each PRINT operation or print() method call that that
	 * does not already contain carriage return information will be formatted
	 * into columns of equal width and number, as specified by the column
	 * configuration data. This corresponds to the COLUMNS(n,m) value in a
	 * JBasic OPEN statement.
	 */
	protected boolean fColumnOutput;

	/**
	 * The width is characters of each column of the output line, or zero if
	 * column output is not enabled for this file.
	 */
	protected int columnWidth;

	/**
	 * The current column position in the output buffer following the last PRINT
	 * statement or print() method call.
	 */
	protected int columnPosition;

	/**
	 * The maximum number of columns per line of output, or zero if column
	 * output mode is not enabled for this file.
	 */
	protected int columnMaxCount;

	/**
	 * The number of spaces to indent columnar output when the maximum number of
	 * columns have been output previous line, and a new line must be formed.
	 */
	protected int indentation;

	/**
	 * This flag tells if the last PRINT or print() method call generated a new
	 * line operation automatically by hitting the columnMaxCount value. If this
	 * is true, then no additional carriage control is necessary when resuming
	 * non-column output or closing the file. If false, then we have a
	 * "dangling" output line and a carriage return will be added to the output
	 * before disabling column operation or closing the file.
	 */
	protected boolean newLineLast;

	/**
	 * This is the channel to any telnet-based terminal that might be connected
	 * in multiuser mode.  Normally this is null, indicating normal PrintStream
	 * output.
	 */
	private BasicTerminalIO term_channel;

	private boolean logInfo;
	private boolean logError;
	private boolean logDebug;
	
	/**
	 * Get the width of each column in the current COLUMN(n,m) setting.
	 * 
	 * @return The width in characters of each column, or zero if column mode is
	 *         not active.
	 */
	public int getWidth() {
		return columnWidth;
	}

	/**
	 * Get the number of columns in each output line.
	 * 
	 * @return The number of columns per line before a new line is automatically
	 *         generated, or zero if column mode is not active.
	 */
	public int getColumns() {
		return columnMaxCount;
	}

	/**
	 * Create an output file object.
	 * 
	 * @param jb
	 *            The JBasic context that owns this file object.
	 */
	public JBFOutput(final JBasic jb) {
		super(jb);
		mode = MODE_OUTPUT;
		logError = true;
		logDebug = false;
		logInfo = false;
	}

	/**
	 * Set the "append" flag for output. The default is that the file is not in
	 * append mode. When the append flag is set, it means that an open operation
	 * will attempt to open an existing instance of the file if it exists and
	 * position at the end of the file to append text to it. If the file does
	 * not exist, it is created normally.
	 * <p>
	 * <br>
	 * NOTE: This <b><em>must</em></b> be called before open() to have an
	 * effect on the file.
	 * 
	 * @param flag
	 *            True if APPEND mode is to be enabled, or false if all open
	 *            operations are to create a new instance of the file.
	 */
	public void setAppend(final boolean flag) {
		mode = flag ? MODE_APPEND : MODE_OUTPUT;
	}

	/**
	 * Open the output file, using a provided file name. This operation must be
	 * performed before the file can be used for any I/O operation.
	 * 
	 * @param fn
	 *            The external physical file name, stored as a Value. Can also
	 *            be "%console" which is a reserved name meaning the Java system
	 *            console.
	 * @throws JBasicException   if an I/O error occurs or the file cannot be
	 * created.
	 */

	public void open(final Value fn, final SymbolTable symbols) throws JBasicException {

		String extName = fn.getString();

		if( FSMConnectionManager.isFSMURL(extName)) {
			try {
				FSMConnectionManager cnx = new FSMConnectionManager(jbenv);
				cnx.parse(extName);
				String pathName = cnx.getPath();
				cnx.setPath(null);
				extName = cnx.toString();
				//System.out.println("DEBUG: connection " + extName);
				//System.out.println("DEBUG: path       " + pathName);
				fsmFile = new FSMFile( extName );
				fsmFile.open(pathName, FSMFile.MODE_OUTPUT);
				
				fname = "fsm://" + cnx.username + "@" + cnx.host + ":" + cnx.port + pathName;
				
			} catch (Exception e) {
				throw new JBasicException(Status.FILE, new Status(Status.FAULT, e.toString()));
			}
			
			mode = MODE_OUTPUT;
			type = FSM;
			register();
			lastStatus = new Status(Status.SUCCESS);
			return;
			
		}
		
		
		if (extName.equalsIgnoreCase(JBasic.CONSOLE_NAME)) {
			fname = JBasic.CONSOLE_NAME;
			mode = MODE_OUTPUT;
			type = CONSOLE;
			stdout = System.out;
			register();
			lastStatus = new Status(Status.SUCCESS);
			return;
		}

		FileOutputStream outputfile;

		fname = extName;
		type = FILE;
		stdout = null;
		wout = null;

		try {
			/*
			 * Remap the user-specific path name to a file system path name if needed.
			 */
			String fsName = JBasic.userManager.makeFSPath(this.jbenv, extName);
			outputfile = new FileOutputStream(fsName, mode == MODE_APPEND);
			wout = new BufferedWriter(new OutputStreamWriter(outputfile));
		} catch (final FileNotFoundException e) {
			final Status sts = new Status(Status.IOERROR, e.toString());
			sts.print(jbenv);
			throw new JBasicException(sts);
		}
		register();
		return;

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

		String localBuffer = s;

		if (newLineLast & (indentation > 0))
			localBuffer = Utility.spaces(indentation) + localBuffer;

		if (fColumnOutput) {

			int w = columnWidth;

			if (columnWidth < 0)
				w = -(columnWidth);

			while (localBuffer.length() < w)
				if (columnWidth < 0)
					localBuffer = " " + localBuffer;
				else
					localBuffer = localBuffer + " ";

			columnPosition++;
			if (columnPosition >= columnMaxCount) {
				localBuffer = localBuffer + JBasic.newLine;
				newLineLast = true;
				columnPosition = 0;
			}
		}
		if( type == FSM ) {
			byte[] byteBuffer = new byte[localBuffer.length()];
			for( int ix = 0; ix < byteBuffer.length; ix++ )
				byteBuffer[ix] = (byte) localBuffer.charAt(ix);
			try {
				fsmFile.write(byteBuffer);
			} catch (IOException e) {
				lastStatus = new Status(Status.FILE, new Status(Status.FAULT, e.toString()));
			}
		} else if (type == FILE)
			try {
				wout.write(localBuffer);
				wout.flush();
			} catch (final IOException e) {
				final Status sts = new Status(Status.IOERROR, e.toString());
				sts.print(jbenv);
			}
		else if (type == CONSOLE) {
			if (term_channel == null) {
				stdout.print(localBuffer);
			}
			else
				try {
					term_channel.write(localBuffer);
				} catch (IOException e) {
					JBasic.log.info("WRITE exception caught; " + e.toString());
				} 
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

		String localBuffer = s;
		lastStatus = new Status();

		if (newLineLast & (indentation > 0))
			localBuffer = Utility.spaces(indentation) + localBuffer;

		if (fColumnOutput)
			columnPosition = 0;
		if( type == FSM ) {
			byte[] byteBuffer = new byte[localBuffer.length()+1];
			for( int ix = 0; ix < byteBuffer.length-1; ix++ )
				byteBuffer[ix] = (byte) localBuffer.charAt(ix);
			byteBuffer[byteBuffer.length-1] = '\n';
			try {
				fsmFile.write(byteBuffer);
			} catch (IOException e) {
				lastStatus = new Status(Status.FILE, new Status(Status.FAULT, e.toString()));
			}
		} else 
		if (type == CONSOLE) {
			if (term_channel == null)
				stdout.println(localBuffer);
			else
				try {
					term_channel.write(localBuffer + JBasic.newLine);
				} catch (IOException e) {
					e.printStackTrace();
				}
		} else
			try {
				wout.write(localBuffer + JBasic.newLine);
				wout.flush();
			} catch (final IOException e) {
				lastStatus = new Status(Status.IOERROR, e.toString());
				lastStatus.print(jbenv);
			}
		newLineLast = true;
	}

	/**
	 * Generate a newline in the output file. If in column mode, also resets the
	 * column output counters.
	 */
	public void println() {
		// print(s);
		columnPosition = 0;
		if( type == FSM ) {
			byte[] aByte = new byte[] { '\n' };
			try {
				fsmFile.write(aByte);
			} catch (IOException e) {
				lastStatus = new Status(Status.FILE, new Status(Status.FAULT, e.toString()));
			}
		} else if (type == CONSOLE) {
			if (term_channel == null)
				System.out.println();
			else
				try {
					term_channel.write(JBasic.newLine);
				} catch (IOException e) {
					e.printStackTrace();
				}
		} else
			try {
				wout.write(JBasic.newLine);
				wout.flush();
			} catch (final IOException e) {
				final Status sts = new Status(Status.IOERROR, e.toString());
				sts.print(jbenv);
			}
		newLineLast = true;
	}

	/**
	 * Set column output mode for this file. If column output is enabled (via a
	 * width that is non-zero), then each print() call is padded to the given
	 * width before printing. Additionally, each "count" columns will be
	 * followed by a newline operation.
	 * 
	 * So a columnOutput(25,3) will print output in three columns, each of which
	 * is 25 characters wide. Print operations of greater than 25 columns are
	 * allowed but will destroy column alignment - there is no truncation.
	 * 
	 * If you call columnOutput and it was already enabled (via a previous
	 * non-zero width) then that output is considered finished and any previous
	 * pending newline is issued.
	 * 
	 * @param width
	 *            The width of each column. A positive number means blanks are
	 *            padded on the right of the string. A negative number means
	 *            blanks are padded on the left of the string. A width of zero
	 *            disables column output.
	 * @param count
	 *            The number of columns that are output before a newline is
	 *            generated.
	 */
	public void columnOutput(final int width, final int count) {

		if (fColumnOutput)
			if (columnPosition > 0)
				println();

		if ((width == 0) || (count == 0))
			fColumnOutput = false;
		else {
			fColumnOutput = true;
			columnPosition = 0;
			columnWidth = width;
			columnMaxCount = count;
		}
	}

	/**
	 * Disable column output. This is the same as calling columnOutput(0,0), and
	 * forces any pending newline to be output, and then column output mode is
	 * turned off.
	 */
	public void columnOutputEnd() {
		columnOutput(0, 0);
	}

	/**
	 * Set the number of blanks to put in front of a print() or PRINT call for a
	 * file in column output mode.
	 * 
	 * @param count
	 *            The number of blanks to indent future print() or PRINT output.
	 */
	public void indent(final int count) {
		newLineLast = true;
		if (count < 0)
			indentation = 0;
		else
			indentation = count;
	}

	public void close() {
		if( type == FSM ) {
			try {
				fsmFile.close();
				fsmFile.terminate();
				super.close();
			} catch (IOException e) {
				lastStatus = new Status(Status.IOERROR, e.toString());
				lastStatus.print(jbenv);

			}
		} else if (type != CONSOLE) {
			try {
				wout.flush();
				wout.close();
			} catch (final Exception e) {
				lastStatus = new Status(Status.IOERROR, e.toString());
				lastStatus.print(jbenv);
			}
			wout = null;
			super.close();
		} else if (term_channel != null)
			try {
				term_channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	/**
	 * Format a text message as an error message.
	 * @param msg the text of the message.
	 * @return formatted message with date stamp, etc.
	 */
	public String errorFormat(String msg ){
		return "[" + new Date().toString() + "] " + msg;
	}
	/**
	 * Format a text message as a debugging message and display
	 * on the output file.
	 * @param msg the text of the message
	 */
	public void debug(String msg) {
		if( logDebug )
			println("DEBUG: " + errorFormat(msg));
	}

	/**
	 * Format an exception as an error message and display
	 * on the output file.
	 * @param e the Exception to format
	 */
	public void error(Exception e) {
		if( logError )
			error(e.toString());
	}

	/**
	 * Format an error message with an auxiliary exception as
	 * an error message on the output file.
	 * @param msg the text of the primary message
	 * @param e the exception containing additional information.
	 */
	public void error(String msg, Exception e) {
		if( logError )
			error(msg + ", " + e.toString());
	}

	/**
	 * Format a text message as an informational message on the
	 * current output file.
	 * @param msg the text of the message to display
	 */
	public void info(String msg) {
		if( logInfo )
			println("INFO: " + errorFormat(msg));
	}

	/**
	 * Format a text message as an error message on the current
	 * output file.
	 * @param msg  the text of the message to display
	 */
	public void error(String msg) {
		if( logError )
			println("ERROR: " + errorFormat(msg));
	}

	/**
	 * For output files that are to connect to console devices, create
	 * a new terminal session using a BasicTerminalIO object that controls 
	 * the low-level terminal communications.
	 * @param m_io the BasicTerminalIO object already created.
	 */
	public void openTerminal(BasicTerminalIO m_io) {
		term_channel = m_io;
		mode = MODE_OUTPUT;
		type = CONSOLE;
		fname = JBasic.CONSOLE_NAME;
		register();
	}
	
	/**
	 * Set the logging level for messages send through this as logging
	 * data (as opposed to program data)
	 * 0 = no logging
	 * 1 = Errors only
	 * 2 = Errors and Info messages
	 * 3 = Errors, Info, and debug messages
	 * 
	 * @param loggingLevel the logging level to output messages
	 */
	public void setLogging( int loggingLevel ) {
		logError = loggingLevel >= 1;
		logInfo = loggingLevel >= 2;
		logDebug = loggingLevel >= 3;
	}

	/**
	 * Generate a permission failure logging record.
	 * @param session The current session.
	 * @param permissionName The permission that was asked for but not
	 * available to the current session.
	 */
	public void permission(JBasic session, String permissionName) {
		if( logInfo )
			info("Permission failure, user=" + session.getUserIdentity().getName()
				+ ", permission=" + permissionName );
		
	}

	/**
	 * Determine if the given log level is set.
	 * @param i 1 for errors, 2 for info, 3 for debug
	 * @return true if the given log level is set.
	 */
	public boolean isLoggingLevel(int i) {
		
		switch(i) {
		case 1:
			return this.logError;
		case 2:
			return this.logInfo;
		case 3:
			return this.logDebug;
		}
		return false;
	}

	/**
	 * Print a message from the message file.
	 * @param string the message code
	 */
	public void printMessage(String string) {
		println(JBasic.firstSession.getMessage(string));
	}

	/**
	 * Print a message form the message file with a string argument
	 * @param code the message code
	 * @param arg the string argument
	 */
	public void printMessage(String code, String arg) {
		String msg = JBasic.firstSession.getMessage(code, arg);
		println(msg);
	}
}

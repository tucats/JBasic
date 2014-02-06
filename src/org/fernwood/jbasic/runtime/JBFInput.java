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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.wimpi.telnetd.io.BasicTerminalIO;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * Subclass of JBasicFile. This handles text input files only. Supported methods
 * are open(), read(), readValue(), and close();
 * 
 * Text input version of JBasicFile. This class supports INPUT file I/O, which
 * means that data is read (not written) as human-readable text from the
 * external file, which must be accessible to the user via a file name.
 * <p>
 * The operations that can be performed on an INPUT file are OPEN, CLOSE, INPUT,
 * and LINE INPUT. The input operation supported by this program reads a single
 * line at a time from the file, and passes this data to the runtime operations
 * like _INPUT which parse the buffer to return values to variables.
 * 
 * @author cole
 * 
 */
public class JBFInput extends JBasicFile {

	/**
	 * The buffered reader used to handle all input. This is either a buffered
	 * reader created from an external file, or from the System.in stream if the
	 * filename is JBasic.CONSOLE_FILE
	 */
	private BufferedReader stdin;

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

	private TerminalInput term;

	/**
	 * File handle for FSM file system objects.
	 */
	private FSMFile fsmFile;
	
	/**
	 * This is the tokenizer used to break up the buffer into parsable objects
	 * when inputting discrete values.  The tokenizer is created the first time
	 * it's needed, and is retained for the life of the open file.
	 */
	private Tokenizer tokenizer;

	/**
	 * Create a new sequential input file for use by JBasic
	 * @param jb the controlling JBasic session that owns the file.
	 */
	public JBFInput(final JBasic jb) {
		super(jb);
		jbenv = jb;
		mode = JBasicFile.MODE_INPUT;

	}

	/**
	 * Format the file as a string. This is overridden from the JBasicFile class
	 * to add the additional information about the state of the read-ahead
	 * buffer.
	 * 
	 * @return A formatted string describing the state of the
	 *         JBasicFile::JBFInput object.
	 */
	public String toString() {
		String result = "JBasicFile ";
		if (fileID != null)
			result = result + fileID.toString();
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
	 *            The external physical file name, stored as a string in a
	 *            Value. Can also be "%console" which is a reserved name meaning
	 *            the Java system console.
	 * @throws JBasicException if an I/O error occurs or the file does not exist
	 */
	public void open(final Value fn, final SymbolTable symbols) throws JBasicException {

		final String extName = fn.getString();
		fReadAhead = false;
		fsmFile = null;

		if (extName.equalsIgnoreCase(JBasic.CONSOLE_NAME)) {
			fname = JBasic.CONSOLE_NAME;
			buffer = null;
			mode = MODE_INPUT;
			type = CONSOLE;
			stdin = new BufferedReader(new InputStreamReader(System.in));
			register();
			lastStatus = new Status(Status.SUCCESS);
			return;
		}

		fname = extName;
		buffer = null;
		mode = MODE_INPUT;
		type = FILE;

		if(FSMConnectionManager.isFSMURL(extName)) {
			try {
				
				FSMConnectionManager cnx = new FSMConnectionManager(jbenv);
				cnx.parse(extName);
				//System.out.println("DEBUG: connection " + cnx.toString());
				fsmFile = new FSMFile(extName, FSMFile.MODE_INPUT);
				type = FSM;
				fname = "fsm://" + cnx.username + "@" + cnx.host + ":" + cnx.port + cnx.getPath();
				
			} catch (Exception e) {
				throw new JBasicException(Status.INFILE, new Status(Status.FAULT, e.toString()));
			} 
			
		}
		else if (extName.substring(0, 1).equals("@")) {

			final String fisname = extName.substring(1, extName.length());
			final InputStream fis = JBasic.class.getResourceAsStream(fisname);
			if (fis == null)
				throw new JBasicException(Status.INFILE, fname);
			stdin = new BufferedReader(new InputStreamReader(fis));
		} else

			/*
			 * Open it as a conventional file.
			 */
			try {
				/*
				 * Remap the user-specific path name to a file system path name if needed.
				 */
				String fsName = JBasic.userManager.makeFSPath(this.jbenv, extName);
				stdin = new BufferedReader(new FileReader(fsName));
			} catch (final FileNotFoundException e1) {
				mode = MODE_UNDEFINED;
				type = UNDEFINED;
				lastStatus = new Status(Status.INFILE, extName);
				throw new JBasicException(lastStatus);
			}
		register();
		lastStatus = new Status(Status.SUCCESS);

	}

	/**
	 * Read a line from an input file. When the file is exhausted, the routine
	 * returns null.
	 * 
	 * @return A string containing the last string read.
	 */
	public String read() {

		lastStatus = new Status(Status.SUCCESS);

		if (fReadAhead) {
			fReadAhead = false;
			return buffer;
		}

		if( type == FSM && fsmFile != null ) {
			buffer = null;
			byte[] aByte = new byte[1];
			StringBuffer inputBuffer = new StringBuffer();
			
			while(true) {
				try {
					int bytesRead = fsmFile.read(aByte);
					if( bytesRead == 0 ) {
						if( inputBuffer.length() == 0 ) {
							lastStatus = new Status(Status.EOF);
							return null;
						}
						return inputBuffer.toString();
					}
				} catch (IOException e) {
					if( inputBuffer.length() == 0 ) {
						lastStatus = new Status(Status.EOF);
						return null;
					}
					return inputBuffer.toString();
				}
				if( aByte[0] == '\n') {
					buffer = inputBuffer.toString();
					break;
				}
				inputBuffer.append((char)aByte[0]);
			}
			
		}
		else
			try {
				buffer = stdin.readLine();
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
	 * @throws JBasicException if an I/O error occurs or the end of file is reached.
	 */
	public Value readValue() throws JBasicException {

		Value result = null;
		if (mode != MODE_INPUT) {
			throw new JBasicException(Status.INFILE, fname);
		}

		if (!fReadAhead)
			buffer = read();

		if (buffer == null)
			throw new JBasicException(Status.EOF);

		/*
		 * If this is the first time we need to parse input values, then
		 * create the Tokenizer object.  Otherwise, just reset it with a
		 * the current buffer.
		 */
		
		if( tokenizer == null )
			tokenizer = new Tokenizer(buffer);
		else
			tokenizer.loadBuffer(buffer);
		
		/*
		 * Skip over comma or semicolon that might be in the input stream.
		 */
		tokenizer.assumeNextSpecial(new String[] {",", ";"});
		
		/*
		 * Get the next token as a value.
		 */
		String inputValue = tokenizer.nextToken();
		int sign = 1;
		if( inputValue.equals("-")) {
			sign = -1;
			inputValue = tokenizer.nextToken();
		}
		
		switch (tokenizer.getType()) {
		
		case Tokenizer.END_OF_STRING:
			if( sign == -1 ) {
				result = new Value("-");
			}
			else
				result = new Value("");
			break;
			
		case Tokenizer.SPECIAL:
			if( inputValue.equals(",") || inputValue.equals(";")) {
				inputValue = "";
				tokenizer.restoreToken();
			}
			if( sign == -1 ) {
				tokenizer.restoreToken();
				result = new Value("-");
			}
			else
				result = new Value(inputValue);
			break;
			
		case Tokenizer.INTEGER:
			result = new Value(sign * Integer.parseInt(inputValue));
			break;

		case Tokenizer.DOUBLE:
			try {
				result = new Value(sign * Double.parseDouble(inputValue));
			}
			catch (NumberFormatException e ) {
				result = new Value(Double.NaN);
			}
			break;

		default:
			result = new Value(inputValue);
		}

		if (tokenizer.testNextToken(Tokenizer.END_OF_STRING)) {
			buffer = null;
			fReadAhead = false;
		} else {
			buffer = tokenizer.getBuffer().trim();
			fReadAhead = true;
		}
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
			if( type == FSM && fsmFile != null ) {
				fsmFile.close();
				fsmFile.terminate();
			}
			else
				stdin.close();
		} catch (final IOException e) {
			lastStatus = new Status(Status.FILE, "close error " + e);
		}
		super.close();

	}

	/**
	 * Create an input file that reads from a terminal session.
	 * @param activeSession  the parent session that owns this console.
	 * @param m_io The terminal input controller for this console
	 */
	public void openTerminal(JBasic activeSession, BasicTerminalIO m_io) {
		term = new TerminalInput( m_io );
		stdin = new BufferedReader( term );
		buffer = null;
		mode = MODE_INPUT;
		type = CONSOLE;
		fname = JBasic.CONSOLE_NAME;
		fReadAhead = false;
		m_io.setAutoflushing(true);
		term.session = activeSession;
		register();
	}

	/**
	 * Enable or disable echoing of input.  This is used when entering a
	 * password, for example, to suppress the echo of the characters typed
	 * in the input terminal device.
	 * @param b true if echoing is to be enabled.
	 */
	public void setEcho(boolean b) {
		term.setEcho(b);
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
	 * Implement the SETPOS routine for the input stream.  Normally the only
	 * supported value would be zero, which rewinds the file to the beginning.
	 * A non-zero value means skip that many characters into the file.
	 * <p>
	 * This is a very expensive operation and should not be done casually!
	 * @param i the byte position in the file to seek to.
	 */
	public void setPos(int i) {

		/*
		 * If this is a TERMINAL device (the console) there is no work to do.
		 */
		if( this.term != null )
			return;

		if( fname.equals(JBasic.CONSOLE_NAME))
			return;

		if( fsmFile != null )
			try {
				fsmFile.seek(i);
			} catch (IOException e1) {
				lastStatus = new Status(Status.FAULT, e1.toString());
			}
		else {
			/*
			 * Close the stream and re-open it.
			 */

			String fsName = null;
			try {
				stdin.close();
				fsName = JBasic.userManager.makeFSPath(this.jbenv, fname);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JBasicException e) {
				e.printStackTrace();
			}
			try {
				stdin = new BufferedReader(new FileReader(fsName));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			/*
			 * If the position was greater than zero, skip that many bytes.
			 */

			if( i > 0 ) {
				try {
					stdin.skip(i);
				} catch (IOException e) {
					; /* Do nothing if we shoot past the end of the file */
				}
			}
		}
		buffer = null;
		fReadAhead = false;
		
	}

	/**
	 * Return flag indicating if there is data in the read-ahead buffer.
	 * @return true if there is data in the read-ahead buffer.
	 */
	public boolean hasReadAhead() {
		return fReadAhead;
	}

	/**
	 * If there is a read-ahead buffer active, then discard it so additional
	 * input won't be read from the pending buffer.
	 */
	public void flushReadAhead() {
		fReadAhead = false;
		buffer = null;
	}
}

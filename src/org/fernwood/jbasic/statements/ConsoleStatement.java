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
package org.fernwood.jbasic.statements;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.FileParse;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * Specify an existing file as the default console. The syntax is
 * <p>
 * <br>
 * <code>CONSOLE [FILE | #] identifier</code>
 * <p>
 * <br>
 * The keyword FILE or it's synonym "#" are optional, and are present for
 * compatibility with other dialects of BASIC. The identifier is a variable
 * containing the file identification data. This is usually the symbol that was
 * given in the OPEN statement, and contains a RECORD data type describing the
 * open file.
 * <p>
 * The CONSOLE statement will fail if the file is not already opened.
 * 
 * @author tom
 * @version version 1.0 Jun 24, 2004
 * 
 */

class ConsoleStatement extends Statement {

	/**
	 * Compile 'CLOSE' statement. Processes a token stream, and compiles it into
	 * a byte-code stream associated with the statement object. The first token
	 * in the input stream has already been removed, since it was the "verb"
	 * that told us what kind of statement object to create.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the source to
	 *            compile.
	 * @return A Status value that indicates if the compilation was successful.
	 *         Compile errors are almost always syntax errors in the input
	 *         stream. When a compile error is returned, the byte-code stream is
	 *         invalid.
	 */

	public Status compile(final Tokenizer tokens) {

		/*
		 * Create the bytecode stream for this specific statement.
		 */
		byteCode = new ByteCode(session, this);

		/*
		 * If it is a CONSOLE with nothing else, then it means to
		 * revert to default files.
		 */


		if( tokens.assumeNextToken("CLEAR") || tokens.endOfStatement()) {
			byteCode.add(ByteCode._CONSOLE, 0 );
			return new Status();
		}

		int mode = 0;

		if( tokens.assumeNextToken("INPUT"))
			mode = 1;
		else
			if( tokens.assumeNextToken("OUTPUT"))
				mode = 2;

		/*
		 * Are we resetting a specific INPUT or OUTPUT designation?
		 */

		if( tokens.assumeNextToken("CLEAR") || tokens.endOfStatement()) {
			byteCode.add(ByteCode._LOADFREF, (mode == 1) ? "CONSOLE_INPUT" : "CONSOLE_OUTPUT");
		}
		/*
		 * No, we're supposed to close a specific file by identifier or
		 * by file number designation.
		 * 
		 * Parse the FILE <id> or #<number> file identifier info. Generate the
		 * code to load it's identifier value on the stack.
		 */
		else {
			final FileParse f = new FileParse( tokens, false);
			if (f.failed())
				return f.getStatus();

			f.generate(byteCode);
		}

		if( mode == 0 ) {
			byteCode.add(ByteCode._DUP);
			byteCode.add(ByteCode._CONSOLE, 1);
			byteCode.add(ByteCode._CONSOLE, 2);
		}

		else
			byteCode.add(ByteCode._CONSOLE, mode);

		return status = new Status(Status.SUCCESS);

	}

}
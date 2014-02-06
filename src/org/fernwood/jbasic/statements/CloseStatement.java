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
 * Close a file. The file msut have been previously opened with an OPEN
 * statement. The syntax is
 * <p>
 * <br>
 * <code>CLOSE [FILE | #] identifier</code>
 * <p>
 * <br>
 * The keyword FILE or it's synonym "#" are optional, and are present for
 * compatibility with other dialects of BASIC. The identifier is a variable
 * containing the file identification data. This is usually the symbol that was
 * given in the OPEN statement, and contains a RECORD data type describing the
 * open file.
 * <p>
 * The CLOSE statement will fail if the file is not already opened.
 * 
 * @author tom
 * @version version 1.0 Jun 24, 2004
 * 
 */

class CloseStatement extends Statement {

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
		 * The default is to close  single file (mode 0).  Other
		 * choices are 1 for all files or 2 for a pipe output close.
		 */
		int closeMode = 0;
		
		/*
		 * If it is a CLOSE with nothing else, then it means to
		 * close all files.
		 */
		
		
		if( tokens.endOfStatement()) {
			byteCode.add(ByteCode._CLOSE, 1 );
			return new Status();
		}
		
		/*
		 * JBasic also allows CLOSE ALL FILES.  If "ALL" is found but not
		 * "FILES" then back up the tokenizer to the position before the "ALL".
		 */
		
		int tokenPos = tokens.getPosition();
		
		if( tokens.assumeNextToken("ALL"))
			if( tokens.assumeNextToken("FILES")) {
				/*
				 * It was CLOSE ALL FILES, generate the special _CLOSE
				 * and we're done with this statement.
				 */
				byteCode.add(ByteCode._CLOSE, 1 );
				return new Status();
			}
		
		tokens.setPosition(tokenPos);
		
		if( tokens.assumeNextToken("PIPE"))
			closeMode = 2;
		
		/*
		 * No, we're supposed to close a specific file by identifier or
		 * by file number designation.
		 * 
		 * Parse the FILE <id> or #<number> file identifier info. Generate the
		 * code to load it's identifier value on the stack.
		 */
		final FileParse f = new FileParse( tokens, false);
		if (f.failed())
			return f.getStatus();

		f.generate(byteCode);

		if( closeMode == 0 && tokens.assumeNextToken("PIPE"))
			closeMode = 2;

		/*
		 * Generate the _CLOSE, which takes it's argument from the top of the
		 * stack.
		 */
		byteCode.add(ByteCode._CLOSE, closeMode);

		return status = new Status(Status.SUCCESS);

	}

}
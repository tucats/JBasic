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
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * On statement. This establishes condition handlers, etc.
 * 
 * @author tom
 * @version 1.0 October 18, 2004
 */

class OnStatement extends Statement {

	/**
	 * Compile 'ON' statement. Processes a token stream, and compiles it into a
	 * byte-code stream associated with the statement object. The first token in
	 * the input stream has already been removed, since it was the "verb" that
	 * told us what kind of statement object to create.
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

		if (program == null)
			return new Status(Status.NOACTIVEPGM);

		String errorCode = null;

		/*
		 * ON ERROR establishes a condition handler
		 */
		if (tokens.assumeNextToken("ERROR"))
			;
		else if (tokens.testNextToken(Tokenizer.IDENTIFIER))
			errorCode = tokens.nextToken();
		else
			return status = new Status(Status.EXPMESSAGE);

		/*
		 * Optional THEN keyword
		 */

		tokens.assumeNextToken("THEN");
		
		/*
		 * There can be an optional GOTO keyword, or a GOSUB
		 * keyword.  If GOSUB is used, we mark the label with
		 * a ">" prefix that tells the signal handler in the
		 * ByteCode class to do the work for a subroutine 
		 * call rather than a simple transfer.
		 */
		
		boolean isGosub = false;
		if( !tokens.assumeNextToken("GOTO"))
			isGosub = tokens.assumeNextToken("GOSUB");

		/*
		 * Get the label we transfer to on an error condition.
		 */

		if (!tokens.testNextToken(Tokenizer.IDENTIFIER))
			return status = new Status(Status.EXPLABEL);

		byteCode = new ByteCode(session, this);
		if (errorCode != null)
			byteCode.add(ByteCode._STRING, errorCode);
		
		String label = (isGosub? ">" : "") + tokens.nextToken();
		byteCode.add(ByteCode._ERROR, (errorCode == null) ? 0 : 1, label);

		return new Status(Status.SUCCESS);

	}
}
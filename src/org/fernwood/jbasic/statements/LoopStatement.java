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
 * Created on Jan 14, 2008 by tom
 *
 */
package org.fernwood.jbasic.statements;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * @author tom
 * @version version 1.0 Jan 14, 2008
 *
 */
public class LoopStatement extends Statement {

	/**
	 * Compile 'LOOP UNTIL' and 'LOOP WHILE' statements. 
	 * 
	 * Processes a token stream, and compiles it into
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
		if (program == null)
			return new Status(Status.NOACTIVEPGM);

		/*
		 * First, see what mode we're in.  Choices are 
		 * LOOP WHILE or LOOP UNTIL.
		 */

		final int UNKNOWN = 0;
		final int UNTIL = 1;
		final int WHILE = 2;

		int mode = UNKNOWN;

		if( tokens.endOfStatement())
			mode = UNKNOWN;
		else
			if( tokens.assumeNextToken("UNTIL"))
				mode = UNTIL;
			else
				if( tokens.assumeNextToken("WHILE"))
					mode = WHILE;
				else
					return new Status(Status.SYNEXPTOK, "WHILE / UNTIL");

		/*
		 * Compile the expression that defines the loop termination condition.
		 * This is only done if a condition is required.  If the condition was
		 * UNTIL then invert the result.
		 */
		byteCode = new ByteCode(session, this);

		if( mode != UNKNOWN ) {
			final Expression exp = new Expression(session);
			exp.compile(byteCode, tokens);
			if (exp.status.failed())
				return status;

			if( mode == UNTIL )
				byteCode.add(ByteCode._NOT);
		}

		/*
		 * Generate the loop terminate code.  UNKNOWN means that this is really
		 * turned into an unconditional branch back to the top of the loop by
		 * the linker later on.
		 */
		byteCode.add(ByteCode._LOOP, mode);

		indent = -1;
		return new Status();

	}

}

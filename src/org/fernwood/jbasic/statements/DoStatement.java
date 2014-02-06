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
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * DO statement handler. This just creates a DO loop control block and puts it
 * on the stack. The exit condition isn't known until later when a WHILE
 * statement is parsed.
 * 
 * @author cole
 * 
 */

class DoStatement extends Statement {

	/**
	 * Compile 'DO' statement. Processes a token stream, and compiles it into a
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
		if ( tokens.endOfStatement() && program == null)
			return new Status(Status.NOACTIVEPGM);
		byteCode = new ByteCode(session, this);
		
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
		
		if( mode == UNKNOWN) {
			byteCode.add(ByteCode._DO, mode);
		}
		else {
			
			/* We have to handle the WHILE or UNTIL at the top of the
			 * loop now.
			 */
			final Expression exp = new Expression(session);
			exp.compile(byteCode, tokens);
			if (exp.status.failed())
				return status;

			if( mode == UNTIL )
				byteCode.add(ByteCode._NOT);
			
			/* 
			 * Note that we do not emit an integer operand here!
			 * 
			 * The linker will locate the _LOOP 0 that indicates the
			 * end of the loop, and back up to find this DO.  When it
			 * does, it will find the _STMT that starts the instruction
			 * and backpatch the _LOOP to be a branch to that.  This DO
			 * will be replaced by a _BR past the LOOP.
			 */
			byteCode.add(ByteCode._DO);
			
		}
		
		indent = 1;
		return new Status();
	}

}
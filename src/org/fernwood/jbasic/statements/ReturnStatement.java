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
 * RETURN statement handler.
 * 
 * @author cole
 * 
 */

class ReturnStatement extends Statement {

	/**
	 * Compile 'RETURN' statement. Processes a token stream, and compiles it
	 * into a byte-code stream associated with the statement object. The first
	 * token in the input stream has already been removed, since it was the
	 * "verb" that told us what kind of statement object to create.
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

		// Only valid in a running program

		if (program == null)
			return new Status(Status.NOACTIVEPGM);

		byteCode = new ByteCode(session, this);

		/*
		 * If there is more after this, then we're to RETURN a <value>
		 */
		if (!tokens.endOfStatement()) {

			final Expression exp = new Expression(session);
			int count = 0;
			exp.compile(byteCode, tokens);
			if (exp.status.failed())
				return exp.status;
			count = 1;
			while( tokens.assumeNextSpecial(",")) {
				exp.compile(byteCode, tokens);
				if (exp.status.failed())
					return exp.status;
				count++;
			}
			
			/*
			 * IF the return data is a list, then make it into an
			 * array.
			 */
			
			if( count > 1 ) {
				byteCode.add(ByteCode._ARRAY, count );
			}
			
			/*
			 * Return value on top of stack, generate code to store it.
			 */
			byteCode.add(ByteCode._RET, 1); /* 1 = has a value */

		} else
			byteCode.add(ByteCode._RET, 0); /* 0 = no value, might be GOSUB */

		return new Status(Status.RETURN);

	}
}
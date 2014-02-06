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
 * WHILE statement handler.
 * 
 * @author cole
 * 
 */

class UntilStatement extends Statement {

	/**
	 * Compile 'UNTIL' statement. Processes a token stream, and compiles it into
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
		 * WHILE (and UNTIL) require an active program or they are in error.
		 */

		if (program == null)
			return new Status(Status.NOACTIVEPGM);

		/*
		 * Create a new ByteCode stream for this statement, and initialize
		 * an expression evaluator.
		 */
		byteCode = new ByteCode(session, this);
		final Expression exp = new Expression(session);
		
		/*
		 * Compile the remainder of the statement as an expression, which
		 * generates code to leave the expression result on the top of the
		 * runtime stack.
		 */

		exp.compile(byteCode, tokens);
		if (exp.status.failed())
			return status;
		
		/*
		 * Invert the expression result with a _NOT operator, and then
		 * generate the _LOOP operator which uses the top-of-stack to
		 * determine if another loop iteration is called for, using
		 * the loop stack to locate the top of the loop.
		 */

		byteCode.add(ByteCode._NOT);
		byteCode.add(ByteCode._LOOP, 1);
		
		/*
		 * The end of the loop changes the indent level in the source code
		 * for this statement.  This indent level is used during the LIST
		 * operation to format source code.
		 */

		indent = -1;
		return new Status();
	}

}

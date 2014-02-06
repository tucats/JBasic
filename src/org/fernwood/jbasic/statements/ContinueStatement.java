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
 * Branches back to the top instruction of a loop body.
 * 
 * <p>
 * <br>
 * <code>CONTINUE [LOOP]</code>
 * <p>
 * <br>
 * The keyword <code>LOOP</code> is optional and can be used for readability when
 * the <code>LOOP</code> keyword is also present in the loop construct.  For
 * example,<p>
 * <code>
 * DO<br>
 * &nbsp;IF X > 10 THEN CONTINUE LOOP<br>
 * &nbsp;Y = XFUNC(X)<br>
 * &nbsp;X = X + 1<br>
 * LOOP UNTIL Y = 0<br>
 * </code>
 * <p>
 * In the above code, the <code>LOOP</code> keyword is optional both in the
 * <code>CONTINUE</code> statement and in the <code>UNTIL</code> terminating
 * condition.
 * 
 * @author tom
 * @version version 1.0 Jan 5, 2009
 * 
 */

class ContinueStatement extends Statement {

	/**
	 * Compile 'CONTINUE' statement. Processes a token stream, and compiles it into
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
		byteCode = new ByteCode(session, this);
		/*
		 * Optionally you can put the word LOOP here, for parallelism with
		 * the END LOOP statement, etc.
		 */
		tokens.assumeNextToken("LOOP");
		
		byteCode.add(ByteCode._BRLOOP, "B" );
		return status = new Status(Status.SUCCESS);

	}

}
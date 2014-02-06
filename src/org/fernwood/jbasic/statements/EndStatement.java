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
 * Stops execution of the current program (and all programs that it was
 * called from), closes all files, and returns control to the console or
 * shell as appropriate.  Variants are used to terminate a multi-statement
 * IF..ELSE block or as a statement for breaking out of a loop body.
 * 
 * <p>
 * <br>
 * <code>    END</code>
 * <p>
 * <br>
 * The <code>END</code> statement cannot close SYSTEM files, only user files.
 * <p>
 * Alternatively, the statement <code>END IF</code> can be used to mark the
 * end of a multi-statement IF..THEN..ELSE block.  For example,
 * <code>
 * <p>
 * IF SALARY > 10.0 THEN<br>
 * &nbsp;PRINT "LARGE"<br>
 * &nbsp;PAY = SALARY * HOURS<br>
 * ELSE<br>
 * &nbsp;PRINT "SMALL"<br>
 * &nbsp;PAY = SALARY * HOURS * 1.20<br>
 * END IF<br></code>
 * <p>
 * In this case, the two clauses of the <code>IF</code> statement are
 * expressed as multiple statements, signified by the trailing <code>THEN</code>
 * keyword before the block to be executed if the condition is true.  The 
 * <code>ELSE</code> keyword optionally identifies the start of
 * the block to execution if the condition is false.  The entire compound
 * statement is terminated with the <code>END IF</code> statement.
 * <p>
 * Finally, you can use the <code>END LOOP</code> statement to terminate
 * execution of the current loop body of a <code>DO..LOOP</code> or a
 * <code>FOR..NEXT</code> loop construct. For example,
 * <p>
 * <code>
 * DO<br>
 * &nbsp;X = X + 1<br>
 * &nbsp;IF X > 10 THEN END LOOP<br>
 * &nbsp;V = V * X<br>
 * UNTIL V > 100.0<br>
 * </code>
 * <p>
 * In this example, if the value of X becomes greater than 10, the loop
 * is terminated even if the <code>UNTIL</code> clause has not yet been satisfied.
 * 
 * @author tom
 * @version version 1.1 Dec 2008
 * 
 */

class EndStatement extends Statement {

	/**
	 * Compile 'END' statement. Processes a token stream, and compiles it into
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
		 * If this is an END LOOP statement then it is meant to break out
		 * of a DO..LOOP that is in progress.
		 */
		
		if( tokens.assumeNextToken("LOOP")) {
			if( program == null )
				return new Status(Status.NOACTIVEPGM);
			byteCode.add(ByteCode._BRLOOP, "F");
			return new Status();
		}
		/*
		 * If this is an END IF statement it ends a multi-line IF statement.
		 * In that case, emit the marker for the end of the ELSE block and
		 * be done with it.
		 */
		
		if( tokens.assumeNextToken("IF")) {
			if( program == null )
				return new Status(Status.NOACTIVEPGM);

			byteCode.add(ByteCode._IF, 3);
			indent = -1;
			return new Status();
		}
		/*
		 * Close all user files
		 */
		byteCode.add(ByteCode._CLOSE, 1 );

		/*
		 * Signal an "*END" which unwinds all call stacks.
		 */
		
		byteCode.add(ByteCode._SIGNAL, "*END");
		
		return status = new Status(Status.SUCCESS);

	}

}
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
 * CHAIN statement handler. This is a compiled statement (it has a compile()
 * method) and generates bytecode which is later executed.
 * <p>
 * The CHAIN statement transfers control to another program.  All variables
 * and status in the current program are lost, except those symbols marked
 * with the COMMON attribute (see the COMMON statement for details).
 * <p>
 * <b>SYNTAX</b>
 * <p>
 * <code>
 * 
 * 		CHAIN <em>"program"</em> [, <em>line number</em>]
 * 	
 * </code>
 * <p>
 * <b>USAGE</b>
 * <p>
 * <em>program</em> is the name of the program to run, as a string expression.
 * If a line number is given, it specifies the starting line to begin execution
 * at in the new program.
 * <p>
 *
 * @author cole
 * 
 */

class ChainStatement extends Statement {

	/**
	 * Compile 'CHAIN' statement. Processes a token stream, and compiles it into
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
		 * Let's generate some code!
		 */
		byteCode = new ByteCode(session, this);
		
		/*
		 * Get the name of the program to call.  This can be any
		 * expression.  The _CHAIN bytecode will coerce it to a
		 * string so no explicit conversion is needed.
		 */

		final Expression exp = new Expression(session);
		exp.compile(byteCode, tokens);
		if( exp.status.failed())
			return exp.status;
		
		/*
		 * See if there is an optional line number expression.  If there
		 * is one, generate code to put it on the stack, and set the flag
		 * in the instruction indicating that there is a line number value.
		 */
		
		int targetLineNumber = 0;
		if( tokens.assumeNextSpecial(",")) {
			exp.compile(byteCode, tokens);
			if( exp.status.failed())
				return exp.status;
			targetLineNumber = 1;
		}
		
		/*
		 * Generate the opcode and we're done....
		 */
		byteCode.add(ByteCode._CHAIN, targetLineNumber);
			
		return new Status(Status.SUCCESS);

	}

}
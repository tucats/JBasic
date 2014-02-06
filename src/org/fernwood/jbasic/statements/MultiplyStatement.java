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
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * MULTIPLY statement. Parses an expression and arithmetically multiplies it to the 
 * target value, and stores the value back in the target.
 * <p>
 * The syntax of the MULTIPLY statement is
 * <p>
 * <code>
 *
 *            MULTIPLY <em>variable</em> BY <em>expression</em> 
 * 
 * </code>
 * <p>
 * The variable must already exist.
 * <p><br>
 * 
 * @author tom
 * @version version 1.0 Feb 9, 2010
 * @see Value
 * @see SymbolTable
 */

class MultiplyStatement extends Statement {

	/**
	 * Compile 'MULTIPLY' statement. Processes a token stream, and compiles it into a
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

		/*
		 * Prepare the statement's generated code, and then parse assignments
		 * in a loop.
		 */
		
		byteCode = new ByteCode(session, this);
		status = new Status();
		
		Expression exp = new Expression(session);
		LValue destination = new LValue(session, strongTyping());
		destination.forceReference(true);
		
		/*
		 * Start by parsing the target value.
		 */
		status = destination.compileLValue(byteCode, tokens);
		if( status.failed())
			return status;


		/*
		 * Now parse the expression value, and writing it to the bytecode buffer.
		 */
		
		if( !tokens.assumeNextToken("BY"))
			return new Status(Status.EXPCLAUSE, "BY");
		
		status = exp.compile(byteCode, tokens);
		if( status.failed())
			return status;

		destination.compileLoad();
		byteCode.add(ByteCode._MULT);

		/*
		 * Store the result back in the value.
		 */
		
		destination.setIgnoreIncrements(true);
		status = destination.compileStore();
		return status;

	}

}

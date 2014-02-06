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
 * SUBTRACT statement. Parses an expression and arithmetically subtracts it from the 
 * target value.
 * <p>
 * The syntax of the ADD statement is
 * <p>
 * <code>
 *
 *            SUBTRACT <em>expression</em> FROM <em>variable</em> 
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

class SubtractStatement extends Statement {

	/**
	 * Compile 'SUBTRACT' statement. Processes a token stream, and compiles it into a
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
		final LValue destination = new LValue(session, strongTyping());
		destination.forceReference(true);
		
		/*
		 * Start by parsing the expression value, and writing it to the bytecode buffer.
		 */
		
		status = exp.compile(byteCode, tokens);
		if( status.failed())
			return status;

		/*
		 * Now parse the destination as if it was a source.
		 */
		
		if( !tokens.assumeNextToken("FROM"))
			return new Status(Status.EXPCLAUSE, "FROM");

		status = destination.compileLValue(byteCode, tokens);
		if( status.failed())
			return status;
		
		destination.compileLoad();
		
		
		/*
		 * Do the math... we must swap the values first so that we're subtracting
		 * the correct value.
		 */
		byteCode.add(ByteCode._SWAP);
		byteCode.add(ByteCode._SUB);
		
		destination.setIgnoreIncrements(true);
		destination.compileStore();

		return status;

	}

}

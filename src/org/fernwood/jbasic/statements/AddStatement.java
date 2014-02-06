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
 * ADD statement. Parses an expression and arithmetically adds it to the 
 * target value.
 * <p>
 * The syntax of the ADD statement is
 * <p>
 * <code>
 *
 *            ADD <em>expression</em> TO <em>variable</em> 
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

class AddStatement extends Statement {

	/**
	 * Compile 'ADD' statement. Processes a token stream, and compiles it into a
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
		
		/*
		 * Start by parsing the expression value, and writing it to the bytecode buffer.
		 */
		
		status = exp.compile(byteCode, tokens);
		if( status.failed())
			return status;

		/*
		 * Now parse the destination.  We enabel forceReference because we have
		 * to be able to read and write from the same LValue.
		 */
		
		if( !tokens.assumeNextToken("TO"))
			return new Status(Status.EXPCLAUSE, "TO");

		LValue destination = new LValue(session, strongTyping());
		destination.forceReference(true);
		destination.compileLValue(byteCode, tokens);
		
		/*
		 * Now generate the LOAD part of the operation, swap the values 
		 * (so if a TABLE is used, it's in the right addend) and do the
		 * ADD operation.  Finally, generate code to store back in the
		 * LValue.  When references are forced and we use compileLoad()
		 * and compileStore() separately like this, post increments only
		 * happen after the STORE operation completes. We must also
		 * disable incrments explicity so pre-increments are only executed
		 * once time.
		 */
		destination.compileLoad();
		
		byteCode.add(ByteCode._SWAP);
		byteCode.add(ByteCode._ADD);
		
		destination.setIgnoreIncrements(true);
		destination.compileStore();
		

		return status;

	}

}

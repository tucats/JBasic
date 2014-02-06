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
import org.fernwood.jbasic.opcodes.OpSYS;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * EVENT statement. Store a string in the event queue of the session
 * <p>
 * The syntax of the EVENT statement is
 * <p>
 * <code>
 *
 *            EVENT <em>string-expression</em> 
 * 
 * </code>
 * 
 * @author tom
 * @version version 1.0 Feb 9, 2010
 * @see Value
 * @see SymbolTable
 */

class EventStatement extends Statement {

	/**
	 * Compile 'EVENT' statement. Processes a token stream, and compiles it into a
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
		
		status = exp.compile(byteCode,  tokens);
		if( status.failed())
			return status;
		byteCode.add(ByteCode._SYS, OpSYS.SYS_ADD_EVENT);
		return status;

	}

}

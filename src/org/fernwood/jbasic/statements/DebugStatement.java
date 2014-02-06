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
import org.fernwood.jbasic.opcodes.OpDEBUG;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * DEBUG statement compiler. This accepts a statement after the DEBUG verb and
 * runs that statement under control of the debugger.
 * 
 * @author cole
 * 
 */

class DebugStatement extends Statement {

	/**
	 * Compile 'debug' statement.  This means compiling the statement that follows
	 * the DEBUG command (via the store() method which compiles the code) and then
	 * generating a _DEBUG opcode in this statement, followed by concatenating the
	 * statement's compile code into the new statement.  So for example, if you
	 * had the trivial case of
	 * 
	 * 		DEBUG LET X=3
	 * 
	 * The assignment statement would generate the code
	 * 
	 * 		_INTEGER 3
	 * 		_STOR    "X"
	 * 
	 * And this statement would generate a new byte code prefixed by the DEBUG
	 * operator, as
	 * 
	 * 		_DEBUG  1
	 * 		_INTEGER 3
	 * 		_STOR    "X"
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status compile(final Tokenizer tokens) {

		final Statement s = new Statement(session);
		
		if( tokens.endOfStatement())
			s.store("RUN");
		else
			s.store(tokens.getBuffer());
		tokens.flush();

		byteCode = new ByteCode(session, this);
		byteCode.add(ByteCode._DEBUG, OpDEBUG.STEP_INTO);
		byteCode.concat(s.byteCode);
		return new Status();

	}

}
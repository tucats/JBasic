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
 * TRACE statement handler. Accepts a command, and runs it with the diagnostic
 * statement trace enabled.
 * 
 * @author cole
 * @version 1.0 August 12, 2004
 * 
 */

class TraceStatement extends Statement {

	/**
	 * Compile 'trace' statement.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status compile(final Tokenizer tokens) {

		byteCode = new ByteCode(session, this);

		int mark = tokens.getPosition();
		if( tokens.assumeNextToken(new String [] { "ON", "OFF"})) {
			int state = tokens.getSpelling().equals("ON")? 2:0;
			if( tokens.endOfStatement()) {
				byteCode.add(ByteCode._TRACE, state );
				byteCode.add(ByteCode._NEEDP, 1 );
				return new Status();
				}
			}
		tokens.setPosition(mark);
		
		
		/*
		 * At this point there might be the request to trace at the instruction
		 * level, indicated by the BYTECODE keyword.
		 */
		
		int mode = 2;
		
		if( tokens.assumeNextSpecial("(")) {
			mode = 0;
			while( !tokens.assumeNextSpecial(")")) {
				if( tokens.endOfStatement())
					return new Status(Status.PAREN);
				if( tokens.assumeNextSpecial(","))
					continue;
				if( tokens.assumeNextToken("BYTECODE")) {
					mode = mode + 1;
					continue;
				}
				if( tokens.assumeNextToken("STATEMENT")) {
					mode = mode + 2;
					continue;
				}
				return new Status(Status.INVNAME, tokens.nextToken());
				
			}
		}

		/*
		 * If there is nothing else after the statement, then we are to run the
		 * current program in the given trace mode (by statement or bytecode)
		 */
		
		if( tokens.endOfStatement()) {

			byteCode.add(ByteCode._TRACE, mode);
			byteCode.add(ByteCode._LOAD, 20, "SYS$CURRENT_PROGRAM");
			byteCode.add(ByteCode._CALLP, -1 );
			byteCode.add(ByteCode._TRACE, 0 );
			byteCode.add(ByteCode._NEEDP, 1 );
			return new Status();
		}

		/*
		 * The TRACE statement traces statement-level execution of a given
		 * program or operation.  In order to do this, the statement that
		 * is to be generated is bracketed with code to turn trace mode
		 * on and off.
		 * 
		 * As such, the rest of the program text after the TRACE statement
		 * will be compiled as a separate step in a new (temporary) Statement
		 * object.  The resulting bytecode will be used in the current TRACE
		 * statement.
		 */
		
		final Statement s = new Statement(session);
		
		/*
		 * Store the rest of the program text of the TRACE statement in the
		 * newly created Statement object.  This results in the compilation
		 * of the statement.  This is followed by discarding the token buffer
		 * in the current statement (having been used to compile the temporary
		 * statement).  If the store-and-compile operation fails, return this
		 * as the error for the TRACE statement.
		 */
		s.store(tokens.getBuffer());
		tokens.flush();
		if( s.status.failed())
			return s.status;

		/*
		 * Otherwise, the compile of the rest of the statement went well.
		 * Create a new bytecode buffer for the TRACE statement, and put
		 * the _TRACE 1 command in, which enables trace mode.
		 */
		byteCode.add(ByteCode._TRACE, mode);
		
		/*
		 * Now concatenate the bytecode generated in the temporary statement
		 * to the TRACE statement's bytecode buffer.
		 */
		byteCode.concat(s.byteCode);
		
		/*
		 * Finally, add code that turns trace mode back off, and also generate
		 * code to request a prompt in NOPROMPTMODE.
		 */
		byteCode.add(ByteCode._TRACE, 0);
		byteCode.add(ByteCode._NEEDP, 1 );
		return new Status();

	}
}
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
 * LOCK statement handler. Creates a named lock if needed, and then attempts to
 * acquire the lock.
 * 
 * @author cole
 * @version 1.0 March, 2009
 * 
 */

class LockStatement extends Statement {

	/**
	 * Compile 'thread' statement.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 * @return A Status value indicating if the THREAD statement was compiled
	 *         successfully.
	 */

	public Status compile(final Tokenizer tokens) {

		byteCode = new ByteCode(session, this);
		if( tokens.endOfStatement())
			return new Status(Status.EXPNAME);
		int create = 0;
		
		/*
		 * See if this is LOCK CREATE <name-list>.
		 */
		if( tokens.peek(0).equals("CREATE") & !tokens.peek(1).equals("")) {
			create = 1;
			tokens.nextToken(); /* Eat the token */
		}
		
		Expression exp = new Expression(session);
		
		while( !tokens.endOfStatement()) {
			if (!tokens.testNextToken(Tokenizer.IDENTIFIER))
				return new Status(Status.EXPNAME);
			String lockName = tokens.nextToken();
			if( lockName.equals("USING") && tokens.testNextToken("(")) {
				exp.compile(byteCode, tokens);
				if( exp.status.failed())
					return exp.status;
			}
			else
				byteCode.add(ByteCode._STRING, lockName);
			
			byteCode.add(ByteCode._LOCK, create);
			
			if( !tokens.assumeNextSpecial(","))
				break;
		}
		
		return status = new Status();
		}

}
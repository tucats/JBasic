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
 * UNLOCK statement handler. Releases a named lock. If no lock name is given,
 * then releases all locks held by the current thread.
 * 
 * @author cole
 * @version 1.0 March, 2009
 * 
 */

class UnlockStatement extends Statement {

	/**
	 * Compile 'unlock' statement.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 * @return A Status value indicating if the THREAD statement was compiled
	 *         successfully.
	 */

	public Status compile(final Tokenizer tokens) {

		byteCode = new ByteCode(session, this);

		/*
		 * Peek ahead to see if this is an UNLOCK ALL LOCKS statement.
		 * If so, call the function LOCKS() to get an array of all
		 * the active lock names and pass that array to the _UNLOCK
		 * opcode.
		 */
		if( tokens.peek(0).equals("ALL") & tokens.peek(1).equals("LOCKS")) {
			byteCode.add(ByteCode._CALLF, "LOCKS");
			tokens.nextToken();
			tokens.nextToken();
			byteCode.add(ByteCode._UNLOCK);

		}
		
		/*
		 * Otherwise it's a list of explicitly given lock names,
		 * or a list of USING(expr) indirect lock names.  If there
		 * are more than one name, they are separated by commas.
		 * An _UNLOCK is generated for each one.s
		 */
		else {
			Expression exp = new Expression(session);
			int lockCount = 0;
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
				
				byteCode.add(ByteCode._UNLOCK);
				lockCount++;
				if( !tokens.assumeNextSpecial(","))
					break;
			}
			if( lockCount == 0 )
				return status = new Status(Status.EXPNAME);
		}

		return status = new Status();
	}

}
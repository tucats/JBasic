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
 * RANDOMIZE statement handler, which (re)sets the random number variable RND.
 * 
 * @author cole
 * @version 1.0 August 12, 2004
 * 
 */

class RandomizeStatement extends Statement {

	/**
	 * Compile 'randomize' statement.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the
	 *            statement to be compiled.
	 */

	public Status compile(final Tokenizer tokens) {
		
		byteCode = new ByteCode(session, this);
		
		/*
		 * If TIMER, use _RAND 1 which uses system timer to reseed RND
		 */
		if( tokens.assumeNextToken("TIMER")) {
			byteCode.add(ByteCode._RAND, 1 );
			return new Status();
		}
		
		Expression exp = new Expression( this.session);
		exp.compile(byteCode, tokens);
		if( exp.status.failed())
			return exp.status;
		
		/*
		 * IF value, use _RAND 2 which uses integer from stack to re-seed RND
		 */
		byteCode.add(ByteCode._RAND, 2 );
		
		return new Status();
	}
}
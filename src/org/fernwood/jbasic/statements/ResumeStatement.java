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
import org.fernwood.jbasic.runtime.JBasicDebugger;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * 
 * @author tom
 * @version version 1.0 Aug 16, 2006
 * 
 */
public class ResumeStatement extends Statement {

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		final JBasicDebugger dbg = findDebugger();
		if (dbg == null)
			return new Status(Status.NODBG);

		/*
		 * If you issue a RESUME RESET then you reset the break states, so each one
		 * becomes eligible to fire again.
		 */
		
		if( tokens.testNextToken(Tokenizer.IDENTIFIER)) {
			String key = tokens.nextToken();
			
			if( key.equals("RESET"))
				dbg.resetAll();
			else
				tokens.restoreToken();
		}

		return new Status("*STEP", 0);

	}
}

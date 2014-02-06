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

import java.util.Iterator;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * PROTECT statement handler, which marks a program as protected.
 * 
 * @author cole
 * @version 1.0 August 12, 2004
 * 
 */

class ProtectStatement extends Statement {

	/**
	 * Execute 'protect' statement.
	 * 
	 * @param symbols
	 *            The symbol table to use to resolve identifiers, or null if no
	 *            symbols allowed.
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		/*
		 * Make sure a prompt is printed after this statement even if in 
		 * NOPROMPTMODE.
		 */
		this.session.setNeedPrompt(true);
		

		/*
		 * Special case syntax of PROTECT ALL PROGRAMS which means issue
		 * a protect on all programs in memory.
		 */
		
		if( tokens.peek(0).equals("ALL") & tokens.peek(1).equals("PROGRAMS")) {
		
			tokens.assumeNextToken("ALL");
			tokens.assumeNextToken("PROGRAMS");
			Iterator i = session.programs.iterator();
			while( i.hasNext()) {
				Program pgm = (Program) i.next();
				pgm.setSystemObject(false);
				pgm.protect();
			}
			session.programs.setCurrent(null);

			return new Status();
		}
		
		/*
		 * Figure out what the program name is, which may include an explicit
		 * qualifier like VERB or FUNCTION
		 */
		String prefix = "";

		if (tokens.assumeNextToken("VERB"))
			prefix = JBasic.VERB;
		else if (tokens.assumeNextToken("FUNCTION"))
			prefix = JBasic.FUNCTION;
		else if (tokens.assumeNextToken("TEST"))
			prefix = JBasic.TEST;

		/*
		 * Find the program and zap it.
		 */
		
		if(!tokens.testNextToken(Tokenizer.IDENTIFIER))
			return new Status(Status.EXPPGM);
		
		final Program pgm = session.programs.find(prefix + tokens.nextToken());
		if (pgm == null)
			return new Status(Status.PROGRAM, prefix + tokens.getSpelling());

		pgm.setSystemObject(false);
		pgm.protect();

		/*
		 * IF the program we are protecting is current, then make nothing be
		 * current.
		 */
		if (session.programs.getCurrent() == pgm)
			session.programs.setCurrent(null);

		return new Status();
	}
}
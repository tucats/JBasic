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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Linker;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * The UNLINK statement is a utility statement to help test JBasic linking and
 * optimization. Given the name of a program, it removes the aggregated bytecode
 * for the statement, and then re-compiled each individual statement -
 * essentially restoring the program to the state it was in before it was
 * linked.
 * 
 * @author tom
 * @version version 1.0 Aug 16, 2006
 * 
 */
public class UnlinkStatement extends Statement {

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		/*
		 * The UNLINK statement should be followed by an explicit
		 * prompt even if we are in NOPROMPTMODE.  So set the session
		 * flag indicating we think a prompt is required after this
		 * command.
		 */
		
		session.setNeedPrompt(true);

		/*
		 * If there is no program name after the verb, we are to work
		 * on the current (default) program.
		 */
		if (tokens.endOfStatement()) {
			
			/*
			 * If there is no default program, then error
			 */
			if (session.programs.getCurrent() == null)
				return new Status(Status.NOPGM);
			
			/*
			 * Otherwise, call the unlink method on the specific
			 * program and return the result code.
			 */
			return Linker.unlink(session.programs.getCurrent());
		}

		/*
		 * There is text after the verb which is either the program
		 * name, or a prefix name like PROGRAM or FUNCTION followed
		 * by the actual name.  Start by assuming that it's a program
		 * and that the next (only) token is the name.
		 */
		String prefix = JBasic.PROGRAM;
		String pgmName = tokens.nextToken();
		
		/*
		 * See if the name is a reserved word of PROGRAM, FUNCTION,
		 * TEST, or VERB.  IF any is true, then use the token to
		 * set the program type and get another token which now must
		 * be the actual name.
		 */
		if( pgmName.equals("PROGRAM"))
			pgmName = tokens.nextToken();
		else
			if( pgmName.equals("FUNCTION")) {
				prefix = JBasic.FUNCTION;
				pgmName = tokens.nextToken();
			}
			else
				if( pgmName.equals("VERB")) {
					prefix = JBasic.VERB;
					pgmName = tokens.nextToken();
				}
				else
					if( pgmName.equals("TEST")) {
						prefix = JBasic.TEST;
						pgmName = tokens.nextToken();
					}

		/*
		 * If the last token parsed isn't an identifier then it can't
		 * be a valid program name.  Throw an error.
		 */
		if( !tokens.isIdentifier())
			return new Status(Status.EXPPGM);
				
		/*
		 * Using the prefix for the program type and the program name,
		 * locate the actual program object.
		 */
		Program pgmValue = session.programs.find(prefix + pgmName);

		/*
		 * If the program was not found (at least by this name), then
		 * generate an error.
		 */
		if( pgmValue == null ) {
			return status = new Status(Status.PROGRAM, pgmName);
		}
		
		/*
		 * Otherwise, unlink the program object and return the result.
		 */
		return status = Linker.unlink(pgmValue);

	}
}

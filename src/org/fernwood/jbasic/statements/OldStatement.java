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
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * @author cole
 * 
 */
public class OldStatement extends Statement {

	static int nextName = 0;

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		String programName = null;
		String prefix = "";

		if (tokens.assumeNextToken("PROGRAM"))
			prefix = "";
		else if (tokens.assumeNextToken("VERB"))
			prefix = JBasic.VERB;
		else if (tokens.assumeNextToken("FUNCTION"))
			prefix = JBasic.FUNCTION;
		else if (tokens.assumeNextToken("TEST"))
			prefix = JBasic.TEST;

		if (tokens.testNextToken(Tokenizer.IDENTIFIER))
			programName = prefix + tokens.nextToken();
		else
			return new Status(Status.EXPPGM );

		final Program newProgram = session.programs.find(programName);
		if (newProgram == null)
			return new Status(Status.PROGRAM, programName.toUpperCase());
		if (newProgram.isProtected())
			return new Status(Status.PROTECTED, programName);

		session.programs.setCurrent(newProgram);
		/*
		 * Make sure a prompt is printed after this statement even if in 
		 * NOPROMPTMODE.
		 */
		this.session.setNeedPrompt(true);
		
		newProgram.link(false);
		session.setCurrentProgramName(newProgram.getName());

		return status = new Status(Status.SUCCESS);
	}
}

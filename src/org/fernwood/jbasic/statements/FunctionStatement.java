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
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * FUNCTION statement handler. The FUNCTION statement declares a user-written
 * function, which is a JBasic program with some special invocation
 * characteristics so it can be executed from the expression handler. This lets
 * a user written function program be called from within any expression
 * evaluation, whether in another program or from the command line. <br>
 * <br>
 * Functions accept a varying number of arguments (up to 32) and always return a
 * single Value as a value (even though this Value could be an arrayValue). The
 * FUNCTION declaration includes an optional USING clause which identifies named
 * variables in the function program that are assigned the values of the
 * matching positional arguments in the invocation. For functions that accept
 * varying length arguments, the program can also use the ARG$LIST arrayValue
 * variable which is created with an element for each argument (and the LENGTH()
 * function can determine how many elements are in the arrayValue). <br>
 * <br>
 * In most other respects, the FUNCTION verb acts like the PROGRAM verb for
 * storing lines of program text in a Program object. See the documentation on
 * the Program class for more information.
 * 
 * @author cole
 * 
 */

class FunctionStatement extends Statement {

	public Status compile(final Tokenizer tokens) {

		byteCode = new ByteCode(session, this);
		status = new Status();
		String programName = tokens.nextToken();
		if (!tokens.isIdentifier())
			return status = new Status(Status.EXPPGM);

		programName = JBasic.FUNCTION + programName;

		byteCode.add(ByteCode._ENTRY, ByteCode.ENTRY_FUNCTION, programName);

		status = generateArgumentList(session, tokens);
		if( status.failed())
			return status;
		
		/*
		 * See what pragma operations might have been defined for this function.
		 * Use the local program object if possible else the default object if
		 * the program hasn't ever been run yet.
		 */
		Program p = program;
		if (p == null)
			p = session.programs.getCurrent();

		if (p != null)
			status = p.handlePragmas(byteCode, tokens);
		if( status.success())
			session.setCurrentProgramName(programName);

		return status;
	}



}
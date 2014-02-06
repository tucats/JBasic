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

import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * VERB statement handler.
 * 
 * @author cole
 * 
 */

class VerbStatement extends Statement {

	public Status compile(final Tokenizer tokens) {

		final String programName = tokens.nextToken();
		if (!tokens.isIdentifier())
			return status = new Status(Status.EXPPGM);
		
		byteCode = new ByteCode(session, this);
		byteCode.add(ByteCode._ENTRY, ByteCode.ENTRY_VERB, programName);
		byteCode.add(ByteCode._SETSCOPE, -1);
		status = new Status();

		/*
		 * If there is an argument list, take care of that now.
		 */
		status = generateArgumentList(session, tokens);
		if( status.failed())
			return status;
		
		/*
		 * See what pragma operations might have been defined for this verb. Use
		 * the local program object if possible else the default object if the
		 * program hasn't ever been run yet.
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
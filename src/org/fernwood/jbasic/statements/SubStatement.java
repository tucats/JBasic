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
 * SUB statement handler.
 * 
 * @author cole
 * 
 */

class SubStatement extends Statement {

	public Status compile(final Tokenizer tokens) {
		byteCode = new ByteCode(session, this);
		status = new Status();
		
		final String programName = tokens.nextToken();
		if (!tokens.isIdentifier())
			return status = new Status(Status.EXPPGM);

		/*
		 * Terminate the previous program flow as if an
		 * END statement had been encountered.
		 */
		byteCode.add(ByteCode._CLOSE, 1);
		byteCode.add(ByteCode._SIGNAL, "*END");
		
		/*
		 * And start a new entry point in this bytecode
		 */
		byteCode.add(ByteCode._ENTRY, ByteCode.ENTRY_SUB, programName);

		status = generateArgumentList(session, tokens);

		return status;
	}
}
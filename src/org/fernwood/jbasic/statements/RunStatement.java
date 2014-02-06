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
 * RUN statement handler. Accepts a label token, and attempts to set the current
 * program's execution mode to that label location.
 * <p>
 * You can specify the name of the program to run, or else the default (last)
 * program is run. When giving the name of the program, if it is not found, then
 * JBasic attempts to load a program in the default directory of the given name
 * (it tries the name as-given and with an extension of
 * <code>".jbasic"</code>).<p>
 * You can specify the starting location of where in the program to
 * begin execution by adding a <code>FROM label</CODE> clause to the
 * statement.<p>
 * 
 * @author cole
 * @version 1.2 Removed the interpreted version and use only the compiled 
 * invocation of <code>_CALLP</code>
 *
 */

class RunStatement extends Statement {

	public Status compile(final Tokenizer tokens) {

		byteCode = new ByteCode(session);
		if (tokens.assumeNextToken("DEBUG"))
			byteCode.add(ByteCode._DEBUG, 1);
		byteCode.add(ByteCode._NEEDP, 1);
		
		if (tokens.endOfStatement()) {
			byteCode.add(ByteCode._LOAD, 20, "SYS$CURRENT_PROGRAM");			
			byteCode.add(ByteCode._CALLP, -1);
			return new Status();
		}

		if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
			return new Status(Status.EXPPGM);
		
		final String name = tokens.nextToken();
		byteCode.add(ByteCode._SETPGM, name);
		byteCode.add(ByteCode._CALLP, -1, name);

		return new Status();
	}

}
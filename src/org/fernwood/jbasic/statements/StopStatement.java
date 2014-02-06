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
 * Stops execution of the current program and gives control to the debugger.
 * 
 * <p>
 * <br>
 * <code>STOP</code>
 * <p>
 * <br>
 * The <code>END</code> statement cannot close SYSTEM files, only user files.
 * 
 * @author tom
 * @version version 1.0 Jun 24, 2004
 * 
 */

class StopStatement extends Statement {

	/**
	 * Compile 'STOP' statement. Processes a token stream, and compiles it into
	 * a byte-code stream associated with the statement object. The first token
	 * in the input stream has already been removed, since it was the "verb"
	 * that told us what kind of statement object to create.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the source to
	 *            compile.
	 * @return A Status value that indicates if the compilation was successful.
	 *         Compile errors are almost always syntax errors in the input
	 *         stream. When a compile error is returned, the byte-code stream is
	 *         invalid.
	 */

	public Status compile(final Tokenizer tokens) {
		
		if (program == null)
			return new Status(Status.NOPGM);
		
		byteCode = new ByteCode(session, this);
		byteCode.add(ByteCode._LOADREF, "SYS$STOP_MESSAGE" );
		byteCode.add(ByteCode._OUTNL, 0);
		byteCode.add(ByteCode._DEBUG, 1);
		
		return status = new Status(Status.SUCCESS);

	}

}
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
 * 
 * ELSE compilation module. Not actually used in normal syntax; the IF statement
 * handles both the THEN and ELSE clauses. However, ill-formed statements can
 * cause the statement dispatcher to attempt to compile a ELSE statement.
 * 
 * @author cole
 * 
 */
public class ElseStatement extends Statement {

	/**
	 * Compile 'else' statement. There isn't a ELSE statement; this is a trap to
	 * catch ill-formed IF..THEN..ELSE clauses which have an extra ELSE
	 * statement. The code generator helpfully attempts to generate an EXEC
	 * statement for an "ELSE" verb, which of course doesn't exist... but this
	 * isn't caught until runtime. So we add a compile module for ELSE which
	 * catches it right away.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status compile(final Tokenizer tokens) {

		if( program == null )
				return new Status(Status.NOACTIVEPGM);
			
			byteCode = new ByteCode(session, this);
			byteCode.add(ByteCode._IF, 2);
			return new Status();
	}
}

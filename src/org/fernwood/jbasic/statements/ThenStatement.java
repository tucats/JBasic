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

/**
 * <code>THEN</code> compilation module. Not actually used in normal syntax; 
 * the <code>IF</code> statement handles the <code>THEN</code> (and possibly 
 * <code>ELSE</code>) clause(s). However, ill-formed statements can
 * cause the statement dispatcher to attempt to run a <code>THEN</code>
 * statement so this compilation unit just signals that error.
 * 
 * @author cole
 * 
 */
public class ThenStatement extends Statement {

	/**
	 * Execute 'then' statement. There isn't a THEN statement; this is a trap to
	 * catch ill-formed IF..THEN clauses which have an extra THEN statement. The
	 * code generator helpfully attempts to generate an EXEC statement for a
	 * "THEN" verb, which of course doesn't exist... but this isn't caught until
	 * runtime. So we add a compile module for THEN which catches it right away.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status compile(final Tokenizer tokens) {

		/*
		 * Make sure there is no bytecode for this statement and signal an
		 * error since a stand-alone THEN indicates a broken IF..THEN..ELSE
		 * syntax construct.
		 */
		byteCode = null;
		return new Status(Status.THEN);
	}
}

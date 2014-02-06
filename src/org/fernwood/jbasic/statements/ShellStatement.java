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
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * THREAD statement handler. Accepts a command, and runs it on a new thread with
 * a new instance of JBasic.
 * 
 * @author cole
 * @version 1.0 August 12, 2004
 * 
 */

class ShellStatement extends Statement {

	/**
	 * Execute 'shell' statement.
	 * 
	 * @param symbols
	 *            The symbol table to use to resolve identifiers, or null if no
	 *            symbols allowed.
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		/*
		 * Currently, there must be nothing else on the command line. If there
		 * is, signal success but do no work; the verb dispatcher will see that
		 * there was more and complain about it.
		 */
		if (!tokens.endOfStatement())
			return new Status();

		final String shell_level = "SYS$SHELL_LEVEL";
		final int level = symbols.getInteger(shell_level);
		Status sts = null;
		try {
			symbols.insert(shell_level, level + 1);
			String shellTableName = "Local to SHELL" + Integer.toString(level+1);
			SymbolTable shellTable = new SymbolTable(session, shellTableName, symbols);
			sts = session.shell(shellTable, "SHELL> ");
			symbols.insert(shell_level, level);
		} catch( JBasicException e ) {
			return e.getStatus();
		}
		session.setNeedPrompt(true);
		return sts;
	}
}
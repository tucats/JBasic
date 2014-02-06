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
import org.fernwood.jbasic.compiler.Optimizer;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.JBasicDebugger;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * Compile a statement
 * 
 * @author cole
 * @version 1.0 August 12, 2004
 * 
 */

class CompileStatement extends Statement {

	/**
	 * Compile a statement.
	 * 
	 * @param symbols
	 *            The symbol table to use to resolve identifiers, or null if no
	 *            symbols allowed.
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		/*
		 * We will temporarily turn optimization off, so get the current state
		 * and then force the global state off.
		 */
		final boolean doOptimization = session.globals().getBoolean(
				"SYS$OPTIMIZE");
		if (doOptimization)
			try {
				session.globals().insert("SYS$OPTIMIZE", false);
			} catch (JBasicException e) {
				return new Status(Status.FAULT, new Status(Status.READONLY, "SYS$OPTIMIZE"));
			}

		final boolean debug = false;

		/*
		 * Make sure a prompt is printed after this statement even if in 
		 * NOPROMPTMODE.
		 */
		this.session.setNeedPrompt(true);
		
		/*
		 * Construct a new statement, and store/compile the statement
		 */
		status = new Status(Status.SUCCESS);
		final Statement theStatement = new Statement(session);

		/*
		 * If there is nothing else, the user messed up.
		 */
		
		if( tokens.endOfStatement()) 
			return new Status(Status.SYNTAX, 
					new Status(Status.EXPCLAUSE, "statement"));
		
		/*
		 * Store the remainder of the string in the statement object we
		 * created. This will generate a compilation.  If the compile fails,
		 * then we are done here.
		 */

		if (theStatement.store(session, tokens, program).failed())
			return theStatement.status;

		/*
		 * Turn back on optimizations if they were off.
		 */
		if (doOptimization)
			try {
				session.globals().insert("SYS$OPTIMIZE", true);
			} catch (JBasicException e) {
				return new Status(Status.FAULT, new Status(Status.READONLY, "SYS$OPTIMIZE"));
			}

		if (!theStatement.hasByteCode())
			session.stdout.println("Statement cannot be compiled");
		else {

			/*
			 * Let's show the user the (unoptimized) code.
			 */
			theStatement.program = program;
			if (doOptimization)
				session.stdout.println("BEFORE OPTIMIZATION:");

			if (theStatement.hasByteCode())
				theStatement.disassemble();

			/*
			 * If optimization is wanted, let's separate out that task here and
			 * show the results (if any)
			 */
			if (doOptimization) {
				final Optimizer opt = new Optimizer();

				final int optCount = opt.optimize(theStatement.byteCode);

				if (optCount == 0)
					session.stdout.println("No optimizations possible");
				else {

					final String plural = optCount == 1 ? ": " : "S: ";

					session.stdout.println("AFTER " + optCount + " OPTIMIZATION"
							+ plural);
					theStatement.disassemble();
				}
			}

			if (theStatement.status.success()) {
				if (debug)
					theStatement.byteCode.setDebugger(new JBasicDebugger(session.stdin(), session.stdout));
				status = theStatement.byteCode.run(symbols, 0);
			}
		}

		return status;
	}
}
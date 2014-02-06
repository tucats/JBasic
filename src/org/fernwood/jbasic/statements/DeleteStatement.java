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
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * Delete lines of a program. This is interpreted and executes immediately on
 * the default program. If there is no default program, then an error is
 * generated.
 * 
 * DELETE [start]-[end]
 * 
 * If the line range is omitted, then JBASIC offers to delete the entire
 * program.
 * 
 * @author tom
 * @version version 1.0 Jan 30, 2006
 * 
 */

class DeleteStatement extends Statement {

	/**
	 * Execute 'delete' statement.
	 * 
	 * @param symbols
	 *            The symbol table to use to resolve identifiers, or null if no
	 *            symbols allowed.
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */
	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		if( program != null )
			if( program.getExecutable() != null)
				if( program.getExecutable().fRunning & program.getExecutable().debugger != null )
					return new Status(Status.INVDBGOP);

		int startLine = 0;
		int endLine = 100000;

		final Program program = session.programs.getCurrent();

		if (program == null)
			return new Status(Status.NOPGM);

		/*
		 * See if we're given a range of line(s) to delete.
		 */
		if (!tokens.endOfStatement()) {

			if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {
				startLine = session.programs.getCurrent().findLabel(tokens.nextToken());
				if (startLine < 0)
					return new Status(Status.NOSUCHLABEL, tokens.getSpelling());
				startLine = program.getStatement(startLine).lineNumber;
				endLine = startLine;
			} else if (tokens.testNextToken(Tokenizer.INTEGER)) {
				startLine = Integer.parseInt(tokens.nextToken());
				endLine = startLine;
			}
			else
				return new Status(Status.INVLINE, tokens.nextToken());

			if (tokens.assumeNextToken("-")) {

				if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {
					endLine = session.programs.getCurrent().findLabel(tokens
							.nextToken());
					if (endLine < 0)
						return new Status(Status.NOSUCHLABEL, tokens
								.getSpelling());
					endLine = program.getStatement(endLine).lineNumber;
				} 
				else if (tokens.testNextToken(Tokenizer.INTEGER))
					endLine = Integer.parseInt(tokens.nextToken());
				else if (tokens.endOfStatement()) {
					endLine = program.statementCount();
					endLine = program.getStatement(endLine-1).lineNumber;
				}
				else
					return new Status(Status.INVLINE, tokens.nextToken());
			}
		}
		else {
			session.stdout.print("Are you sure you want to delete program "
					+ session.programs.getCurrent().getName() + " (yes/no)? ");
			final String answer = session.stdin().read();
			if (answer != null)
				if (answer.toUpperCase().startsWith("Y"))
					try {
						session.programs.getCurrent().clear();
					} catch (JBasicException e) {
						return e.getStatus();
					}
			
			return status = new Status(Status.SUCCESS);

		}

		/*
		 * Make sure a prompt is printed after this statement even if in 
		 * NOPROMPTMODE.
		 */
		this.session.setNeedPrompt(true);
		
		int vectorLength = session.programs.getCurrent().statementCount();
		int deleteCount = 0;
		
		for (int i = 0; i < vectorLength; i++) {

			final Statement stmt = session.programs.getCurrent().getStatement(i);

			if (stmt.lineNumber < startLine)
				continue;
			if (stmt.lineNumber > endLine)
				break;

			try {
				session.programs.getCurrent().removeStatement(i);
			} catch (JBasicException e ) {
				return e.getStatus();
			}
			i--;
			vectorLength--;
			deleteCount++;

		}

		if( deleteCount == 0 ) 
			return status = new Status(Status.NODELLINE);
		
		return status = new Status(Status.SUCCESS);

	}
}

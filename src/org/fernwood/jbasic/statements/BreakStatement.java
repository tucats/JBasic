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

import java.util.Iterator;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.BreakPoint;
import org.fernwood.jbasic.runtime.JBasicDebugger;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * Create/manage breakpoints in the JBASIC debugger.  This statement is
 * interpreted (it has a run() method) and dynamically processes the
 * tokens in the statement input buffer at runtime to perform the
 * operation of the statement.
 * <p>
 * <b>SYNTAX</b>
 * <p>
 * <code>BREAK LIST</code><br>
 * <code>BREAK CLEAR <em>name</em> | ALL</code><br>
 * <code>BREAK WHEN <em>condition</em></code><br>
 * <code>BREAK AT <em>location</em></code><br>
 * <p>
 * BREAK is followed by a subcommand of LIST, CLEAR, WHEN, or AT.  These
 * define the specific breakpoint management operation to be performed.  The
 * <em>USAGE</em> section below outlines the use of each command.
 * <p>
 * <b>USAGE</b>
 * <p>
 * <code>BREAK LIST</code><br><br>
 * This command lists all active breakpoints and their status.  For each
 * breakpoint, the command lists the name of the breakpoint (used to reference
 * that breakpoint later in a BREAK CLEAR command), the condition or 
 * location of the breakpoint, and whether the breakpoint has been triggered
 * or not.
 * <p>
 * <code>BREAK CLEAR</code><br><br>
 * The BREAK CLEAR command is used to remove one or all breakpoints.  If the
 * keyword ALL is used after the command it removes all breakpoints.  
 * Otherwise, the command is followed by the name of a specific breakpoint,
 * usually determined by the BREAK LIST command.
 * <p>
 * <code>BREAK AT [<em>program</em>@]<em>line</em></code><br><br>
 * The BREAK AT statement defines a location at which the program will break
 * execution and return control to the debugger.  The line number is a line
 * within the current program.  If you wish the breakpoint to be in another 
 * program, put the program name and an at-sign ("@") before the line
 * number.
 * <p>
 * <code>BREAK WHEN <em>expression</em></code><br><br>
 * The BREAK WHEN statement defines a condition that will cause the program
 * to break execution and give control to the debugger.  The conditional
 * expression is evaluated, and when the result is true, the breakpoint is
 * taken.  For example, and exmpression of <code>COUNT=15</code> will break
 * when the variable COUNT becomes equal to the value 15.
 * 
 * @author cole
 * @version 1.0 August 12, 2004
 * 
 */

class BreakStatement extends Statement {

	/**
	 * Create or manage breakpoints
	 * 
	 * @param symbols
	 *            The symbol table to use to resolve identifiers, or null if no
	 *            symbols allowed.
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		/*
		 * This command is only valid during control of a debugger.  Find
		 * the instance of the debugger that is active.  If there is no
		 * debugger attached to the current execution contact, then report
		 * an error and we have no work to do.
		 */
		final JBasicDebugger theDebugger = findDebugger();
		if (theDebugger == null)
			return new Status(Status.NODBG);

		/*
		 * Check for sub-commands
		 */

		if (tokens.assumeNextToken("LIST")) {

			/*
			 * List all the breakpoints attached to the debugger.  If there
			 * are none, the iterator is empty and we report as such.
			 */
			
			final Iterator i = theDebugger.getBreakpointIterator();
			if (i == null) {
				session.stdout.println("No breakpoints");
				return new Status();
			}
			
			/*
			 * Otherwise, loop over the iterator, and print each one.  The 
			 * actual format of the output is controlled by the breakpoint o
			 * bject's toString() method.
			 */
			while (i.hasNext()) {
				final BreakPoint bp = (BreakPoint) i.next();
				session.stdout.println("   " + bp.getID() + ": " + bp.toString());
			}

			return new Status();
		}

		/*
		 * Clear one or more breakpoints.  
		 */
		if (tokens.assumeNextToken("CLEAR")) {

			/*
			 * If the CLEAR ALL command is given then all breakpoints are 
			 * removed.
			 */
			if (tokens.assumeNextToken("ALL")) {
				theDebugger.removeAllBreakpoints();
				return new Status();
			}
			
			/*
			 * Otherwise there is a list of breakpoints that are to be
			 * cleared explicitly.  These can be cleared by name or by
			 * numeric ID.
			 */
			while (true) {

				/*
				 * Determine the name based on the numeric representation
				 * or the actual name given in the command, based on the
				 * type of the next token.
				 */
				String bname;
				if (tokens.testNextToken(Tokenizer.INTEGER))
					bname = "BREAK_" + tokens.nextToken();
				else if (tokens.testNextToken(Tokenizer.IDENTIFIER))
					bname = tokens.nextToken();
				else
					break;

				/*
				 * Remove the breakpoint.  If this fails, it usually means
				 * the named breakpoint does not exist.  Report the error
				 * and quit.  Note that all breakpoints up to this one will
				 * have been removed!
				 */
				final Status sts = theDebugger.removeBreakpoint(bname);
				if (sts.failed())
					return sts;

				/*
				 * The list of breakpoints must be comma-separated; if there
				 * isn't a comma then we must have come to the end of the list.
				 */
				if (!tokens.assumeNextSpecial(","))
					break;
			}

			return new Status();
		}

		/*
		 * Must be a BREAK declaration. Create a new breakpoint object
		 * to hold the information, and then handle all the clauses.  You
		 * can use BREAK WHEN, BREAK AT, or a combination of WHEN and AT
		 * to define a condition and location.
		 */

		final BreakPoint bp = new BreakPoint(tokens.getBuffer());

		boolean hasWHEN = false;
		boolean hasAT = false;
		
		while (true)

			/*
			 * BREAK WHEN has a separate compilation phase that takes the
			 * expression and stores bytecode away for it in the breakpoint
			 * descriptor.
			 */
			if (tokens.assumeNextToken("WHEN")) {
				if( hasWHEN )
					return new Status(Status.DUPCLAUSE, "WHEN");
				hasWHEN = true;
				bp.compileWhen(session, tokens);
			}

		/*
		 * Otherwise it is BREAK AT.
		 */
			else if (tokens.assumeNextToken("AT")) {

				if( hasAT )
					return new Status(Status.DUPCLAUSE, "AT");
				hasAT = true;
				
				/*
				 * If it is BREAK AT <program>@<line> then we must
				 * store the program name and skip past the "@" token.
				 */
				if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {
					bp.setProgram(tokens.nextToken());
					tokens.assumeNextSpecial("@");
				}

				/*
				 * The next token must be a line number.
				 */
				final String iString = tokens.nextToken();
				if (tokens.getType() != Tokenizer.INTEGER)
					return new Status(Status.UNKBPT, iString);

				bp.setLine(Integer.parseInt(iString));

			} 
		
		/*
		 * If we're at the end of the buffer then we're done.
		 */
			else if (tokens.endOfStatement())
				break;
			else
				return new Status(Status.SYNINVTOK, tokens.nextToken());

		/* The breakpoint object is filled in, store it away */

		theDebugger.addBreakPoint(bp);

		return new Status();

	}

}
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
package org.fernwood.jbasic.runtime;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.value.Value;

/**
 * Breakpoint object for JBasic debugger. The Breakpoint object defines a
 * breakpoint, and maintains any state about that breakpoint (such as whether it
 * has been signalled yet or not).
 * <p>
 * The breakpoint object has the following main functions:
 * <p>
 * <list>
 * <li>Store information about the location where a STOP AT breakpoint occurs
 * <li>Store information about the condition of a STOP WHEN breakpoint,
 * including compiling and saving the WHEN expression.
 * <li>Testing to see if a breakpoint condition has been satisfied, used by the
 * _STMT opcode to temporarily halt execution of the program. <list>
 * <p>
 * 
 * @author tom
 * @version version 1.0 Sep 3, 2006
 * 
 */
public class BreakPoint {

	private static int next_id = 0;

	private int id;

	private ByteCode whenClause;

	private String text;

	private String program;

	private int line;

	private boolean alreadySignalled;

	/**
	 * Create a new breakpoint.  The breakpoint contains a copy of the command
	 * that created it for documentation purposes (for the user's use of SHOW
	 * BREAKPOINTS).  The text is not actually used in the breakpoint itself,
	 * since it must have already been parsed and processed to create a breakpoint.
	 * @param t
	 *            the Text of the breakpoint command
	 */
	public BreakPoint(final String t) {
		id = ++next_id;
		whenClause = null;
		text = t;
	}

	/**
	 * Convert a breakpoint to a string representation, for use in the debugger.
	 */
	public String toString() {
		return text + (alreadySignalled ? " [true]" : "");
	}

	/**
	 * Reset the breakpoint so it will fire again in the future.
	 */
	public void reset() {
		alreadySignalled = false;
	}

	/**
	 * @return A string that uniquely identifies this breakpoint.
	 */

	public String getID() {
		return "BREAK_" + Integer.toString(id);
	}

	/**
	 * Store the name of the program in the breakpoint object.
	 * @param programName the name of the current program
	 */
	public void setProgram(final String programName) {
		program = programName;
	}

	/**
	 * Store the current line number of the breakpoint in the object.
	 * @param lineNumber the current line number
	 */
	public void setLine(final int lineNumber) {
		line = lineNumber;
	}

	/**
	 * Compile a WHEN clause for a BREAK statement.  This compiles an
	 * expression, and saves the resulting byte code in the breakpoint,
	 * which can be executed in the future to test the break point state.
	 * @param session The session containing this breakpoint
	 * @param tokens The token stream containing the WHEN clause, positioned
	 * just after the WHEN verb itself.
	 * @return Status indicating if the WHEN clause could be compiled
	 *         successfully.
	 */
	public Status compileWhen(final JBasic session, final Tokenizer tokens) {

		whenClause = new ByteCode(session);
		final Expression exp = new Expression(session);
		final Status sts = exp.compile(whenClause, tokens);
		if (sts.failed())
			return sts;

		return new Status();
	}

	/**
	 * Determine if the current breakpoint's WHEN clause means we should break
	 * now.  This is done by executing the expression previously compiled for
	 * the WHEN clause, and checking the result.
	 * 
	 * @param t
	 *            Symbol table to use in resolving the current instance of the
	 *            WHEN clause.
	 * @return true if the WHEN clause exists and has been satisfied.
	 */
	public boolean isWhen(final SymbolTable t) {

		if (whenClause == null) {
			//alreadySignalled = false;
			return false;
		}

		final Status sts = whenClause.run(t, 0);
		if (sts.failed()) {
			//alreadySignalled = false;
			return false;
		}

		final Value v = whenClause.getResult();
		if (v == null) {
			//alreadySignalled = false;
			return false;
		}

		boolean stop = v.getBoolean();

		/*
		 * If we are supposed to stop but have already done so, then let's
		 * decide not to stop after all. Otherwise, remember the stop state we
		 * report now.
		 */
		if (alreadySignalled & stop)
			stop = false;
		else
			alreadySignalled = stop;

		return stop;

	}

	/**
	 * Determine if this breakpoint has been hit based on the current location.
	 * A breakpoint can contain a program name and a line number, and each of
	 * these is compared to the current line and program to see if this breakpoint
	 * is considered "hit".
	 * 
	 * @param p
	 *            The name of the current program object
	 * @param i
	 *            The line number we're about to execute in the program object.
	 * @return true if the breakpoint matches the current location.
	 */
	public boolean isWhere(final String p, final int i) {

		boolean b_line = false;
		boolean b_pgm = true;

		if (program != null)
			if (p != null)
				b_pgm = p.equalsIgnoreCase(program);

		if ((line > 0) && (i == line))
			b_line = true;

		final boolean stop = b_line && b_pgm;
		if (stop)
			alreadySignalled = true;
		return stop;
	}

	/**
	 * See if this breakpoint has a location associated with it, which
	 * means this breakpoint should be tested to see if the line and
	 * program match.
	 * @return true if the breakpoint has a location associated with it.
	 */
	public boolean hasWhere() {
		if ((program != null) | (line > 0))
			return true;
		return false;
	}

	/** See if this breakpoint has a WHEN condition compiled for it, which
	 * means that we should consider checking to see if the condition has
	 * been satisfied or not.
	 * @return true if the breakpoint has a WHEN condition associated with it.
	 */
	public boolean hasWhen() {
		return whenClause != null;
	}

}

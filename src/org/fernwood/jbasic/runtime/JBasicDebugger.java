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

import java.util.Iterator;
import java.util.TreeMap;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.statements.Statement;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * A JBasic program debugger object. An instance of this class can be associated
 * with any program object. When a debugger is present (usually attached by the
 * <code>DEBUG</code> command) then each _STMT opcode calls the step() method
 * of the associated debugger to see if a breakpoint or STEP operation needs to
 * be dealt with.
 * <p>
 * The Debug object handles the following basic functions:
 * <p>
 * <list>
 * <li>Accept and process commands from the console when a breakpoint is hit.
 * <li>Manage the STEP INTO versus STEP OVER and STEP RETURN operations.
 * <li>Creating and managing BreakPoint object lists for the active program
 * </list>
 * <p>
 * 
 * @author tom
 * @version version 1.0 Sep 1, 2006
 * 
 */
public class JBasicDebugger {

	/**
	 * This manages the list of breakpoints currently set in this instance of the
	 * debugger.
	 */
	TreeMap<String, BreakPoint> breakList;

	/**
	 * For operations that require execution of a given number of statements, this
	 * is used to count (down) the number of remaining statements to execute.
	 */
	int stepCounter;

	/**
	 * This indicates if the next STEP operation should step into a called 
	 * function or subprogram.
	 */
	boolean stepInto;

	/**
	 * This indicates of a breakpoint should be generated when a function or
	 * subprogram returns to its caller.
	 */
	boolean breakReturn;

	/**
	 * This indicates if a breakReturn was in fact signalled.
	 */
	boolean breakReturnHit;

	/*
	 * The output stream for the debugger to send messages.
	 */
	private JBFOutput stdout;
	
	/*
	 * The input (command) stream for the debugger.
	 */
	private JBFInput stdin;

	/**
	 * @return an Iterator for scanning over the available breakpoints in this
	 *         debugger.
	 */
	public Iterator getBreakpointIterator() {
		if (breakList == null)
			return null;
		return breakList.values().iterator();
	}

	/**
	 * Create a new debugger instance for a bytecode stream.
	 * @param basicFile the file object used for input
	 * @param out the file object used for output
	 */
	public JBasicDebugger( JBasicFile basicFile, JBasicFile out) {
		stdin = (JBFInput) basicFile;
		stdout = (JBFOutput) out;
		stepCounter = 1;
		stepInto = false;
	}

	/**
	 * Step the byte code one statement, based on the nature of the
	 * current STEP mode (OVER, INTO, RETURN) and the presence
	 * of breakpoints.
	 * 
	 * @param bc The bytecode that is currently being executed
	 * @param s The active local symbol table
	 * @return Status indicating if there is a change in execution flow.
	 */
	public Status step(final ByteCode bc, final SymbolTable s) {

		boolean stop = false;
		JBasic session = bc.getSession();
		
		/*
		 * If this statement is actually the entry point to the program, such as
		 * a PROGRAM or FUNCTION statement, then we are in the "prologue" of the
		 * program and don't really have context. In this case, give ourselves
		 * permission to step one additional statement.
		 */

		Instruction i = bc.getInstruction(bc.programCounter);
		if (i.opCode == ByteCode._ENTRY)
			stepCounter++;

		/*
		 * Okay, get the _STMT that triggered the invocation of this method.
		 * We'll use it to find out where we are in the program at this point.
		 */

		final int pc = bc.programCounter - 1;
		i = bc.getInstruction(pc);

		String where = "Debugging command";
		boolean fWhere = false;

		if (bc.statement != null)
			if (bc.statement.program != null) {
				where = bc.statement.program.getName();
				fWhere = true;
			}

		int lineno = i.integerOperand;
		if (lineno < 0)
			lineno = -lineno;

		/*
		 * Update the automatic variables _LINE_ and _PROGRAM_ to indicate the
		 * current statement.
		 */
		if (lineno > 0) try {
			s.insertLocal("_PROGRAM_", new Value(where));
			s.insertLocal("_LINE_", new Value(lineno));
		} catch (JBasicException e ) { /* do nothing */ }

		/*
		 * Based on where we are (and the stored WHEN clauses in the break list,
		 * determine if we are supposed to stop now.
		 */

		String prefix = "";
		
		if (isBreak(s, where, i.integerOperand)) {
			stop = true;
			prefix = "Break at ";
			stepCounter = 0;
		}

		if (breakReturnHit) {
			breakReturnHit = false;
			stop = true;
			stepCounter = 0;
			prefix = "Return to ";
		}
		/*
		 * Decide if we have a step counter that is active. That could also
		 * cause us to stop.
		 */

		if (stepCounter > 0) {
			stepCounter--;
			if (stepCounter == 0) {
				stop = true;
				if (fWhere)
					prefix = "Step to ";
			}
		}

		/*
		 * So based on breakpoints and/or the step counter, do we need to stop
		 * on this statement?
		 */
		if (stop) {

			/*
			 * Yes, so let's print out where we are in the program
			 */

			if (fWhere) {
				where = where + " ";
				if (lineno > 0)
					where = where + Integer.toString(lineno);
				else
					where = where + "clause";
			}

			String label = bc.findLabel(pc);
			if (label == null)
				label = "                ";
			else
				label = "   " + label + ": ";
			while (label.length() < 20)
				label = label + " ";
			stdout.println(prefix + where + ", ");
			stdout.println(label + bc.getStatementText(i.integerOperand));

			/*
			 * Since we've stopped, accept commands.
			 */
			while (true) {
				Status sts = command(bc, s);
				if (sts == null)
					sts = new Status();
				else {
					if (sts.failed()) {
						sts.print(session);
						continue;
					}
					if (sts.equals(Status.QUIT))
						return sts;

					if (sts.equals("*RESUME"))
						break;
				}
			}
		}

		/*
		 * No (more) work to do, let the program continue.
		 */
		return null;
	}

	/**
	 * Process a single DEBUGGER command. This is very similar to the main
	 * command loop in JBasicMain but sets flags in the statement object to
	 * uniquely identify it as a debugger statement - this is necessary to
	 * prevent recursion. Also, an empty command is interpreted as STEP 1 by
	 * default.
	 * 
	 * @param bc
	 *            The bytecode being debugged.
	 * @param table
	 *            A symbol table to use to resolve commands, etc.
	 * @return a Status object indicating the success of the command operation.
	 */
	private Status command(final ByteCode bc, final SymbolTable table) {

		final JBasic jbenv = bc.getSession();
		String line;
		Status status;
		final Statement s = new Statement(bc.getSession(), this);
		s.inDebugger(true);

		String prompt = table.getString("SYS$DEBUG_PROMPT");
		if( prompt == null )
			prompt = "DBG> ";
		

		stdout.print(prompt);
		line = stdin.read();
		if (line == null)
			return new Status(Status.ERROR);

		if (line.length() == 0)
			line = table.getString("SYS$DEBUG_DEFAULTCMD");
			if( line == null )
				line = "STEP 1";


		/*
		 * Some statements will helpfully update the program associated with the
		 * current statement. But we don't want that, because the statement used
		 * at the console can NEVER be part of an existing program. So clear it
		 * each time to be sure. Then store away the line, compiling it as
		 * needed.
		 */
		s.program = null;
		s.store(line);
		if (s.status.failed()) {
			s.status.printError(jbenv);
			return s.status;
		}

		/*
		 * If the result of the operation was to store a statement in the
		 * current program, we're done and no execution is needed (or desired).
		 */
		if (s.status.equals(Status.STMTADDED))
			return new Status();

		try {
			s.inDebugger(true);
			status = s.execute(table, false);

			if (status.equals("*STEP")) {
				bc.debugger.stepCounter = Integer.parseInt(status.getMessageParameter());
				return new Status("*RESUME");
			} else if (status.equals(Status.QUIT))
				return new Status(Status.QUIT);
			else if (status.failed())
				return status;
		} catch (final Exception e) {
			System.out.println("Unexpected runtime debugger error: " + e);
		}

		return new Status();
	}

	/**
	 * Add a breakpoint object to the debugger.
	 * 
	 * @param bp the breakpoint to add to the breakpoint list.
	 */
	public void addBreakPoint(final BreakPoint bp) {

		if (breakList == null)
			breakList = new TreeMap<String, BreakPoint>();

		breakList.put(bp.getID(), bp);

	}

	/**
	 * See if the current location justifies taking a break point.  The
	 * current program location are examined and a break signaled if
	 * we are at one of those locations.  Additionally, all conditional
	 * breakpoints (BREAK WHEN) are executed to see if any return a 
	 * non-zero result.
	 * 
	 * @param t
	 *            The currently active symbol table.
	 * @param myProgram the current program name
	 * @param myLineNumber the current line number in the program
	 * @return true if a breakpoint should be taken.
	 */
	public boolean isBreak(final SymbolTable t, final String myProgram, final int myLineNumber) {

		/*
		 * Get the iterator for the breakpoint list.  If there is no iterator
		 * then there are no active breakpoints, and we have no work to do.
		 */
		final Iterator i = getBreakpointIterator();
		if (i == null)
			return false;

		/*
		 * Scan over the breakpoint list, by fetching each one and
		 * evaluating it.
		 */
		while (i.hasNext()) {
			final BreakPoint bp = (BreakPoint) i.next();

			boolean stop = false;

			/*
			 * Does this breakpoint have a WHERE clause? If so, then
			 * evaluate the WHERE condition to see if it applies.
			 */
			if (bp.hasWhere()) {
				if (bp.isWhere(myProgram, myLineNumber))
					stop = true;
			} 
			
			/*
			 * Does this breakpoint have a WHEN clause?  If so, 
			 * evaluate the expression to see if we should stop.
			 * If we should stop, return true.  Otherwise return
			 * false.
			 */
			if (bp.hasWhen()) {
				if (bp.isWhen(t))
					return true;
			} else
				return stop;
		}
		return false;

	}

	/**
	 * Remove a given breakpoint by name.  Breakpoint names are
	 * generated automatically by a unique sequence number 
	 * proceeded by the word BREAK_.  The user can see this list
	 * using a SHOW BREAKPOINTS command.
	 * @param bname The name of the breakpoint, which must have
	 * been normalized to uppercase already.
	 * @return status indicating if breakpoint existed or not.
	 */
	public Status removeBreakpoint(final String bname) {

		final Status status = new Status(Status.UNKBPT, bname);
		if (breakList == null)
			return status;

		if (breakList.remove(bname) == null)
			return status;

		return new Status();

	}

	/**
	 * Remove all breakpoints from the current debugger instance.  This removes
	 * both the data structure that describes the breakpoints, and the state
	 * information that tells the debugger about breakpoint counts, etc.
	 */
	public void removeAllBreakpoints() {
		breakList = null;
		setReturn(false);
		stepCounter = 0;
		stepInto = false;
	}

	/**
	 * Reset all breakpoints so they can fire again. Normally a breakpoint can
	 * only fire once and then it "rests". However, a RESUME command will reset
	 * the breakpoints so they fire again when needed.
	 */
	public void resetAll() {

		if (breakList == null)
			return;

		final Iterator i = breakList.values().iterator();

		while (i.hasNext()) {
			final BreakPoint bp = (BreakPoint) i.next();
			bp.reset();
		}
	}

	/**
	 * Return the current STEP over or STEP into setting. This is controlled by
	 * the nature of the last STEP command. The default behavior of the debugger
	 * is to step over a call; use the STEP INTO command to set the one-shot
	 * flag.
	 * 
	 * @return true if the debugger is expecting to be active in subordinate
	 *         calls from the current bytecode stream.
	 */
	public boolean stepInto() {
		return stepInto;
	}

	/**
	 * Return the current STEP over or STEP into setting. This is controlled by
	 * the nature of the last STEP command. The default behavior of the debugger
	 * is to step over a call; use the STEP INTO command to set the one-shot
	 * flag.
	 * 
	 * @param fNewStepIntoSetting
	 *            the new setting of the flag to be made active after the old
	 *            setting is returned.
	 * @return true if the debugger is expecting to be active in subordinate
	 *         calls from the current bytecode stream.
	 */
	public boolean stepInto(final boolean fNewStepIntoSetting) {
		final boolean b = stepInto;
		stepInto = fNewStepIntoSetting;
		return b;
	}

	/**
	 * Mark that a _RETURN has been executed, to set breakpoint triggers
	 * appropriately.  This notes that if a STEP RETURN is active, we
	 * signal that a breakpoint is hit, and then clear the STEP RETURN
	 * state, which is a "one shot" trigger.
	 */
	public void markReturn() {
		if (breakReturn) {
			breakReturnHit = true;
			breakReturn = false;
		}
	}

	/**
	 * Indicate that a SET RETURN has been issued
	 * 
	 * @param b
	 *            true if a SET RETURN is issued, or false if the state is to be
	 *            cleared.
	 */
	public void setReturn(final boolean b) {

		breakReturn = b;
		breakReturnHit = false;

	}

}

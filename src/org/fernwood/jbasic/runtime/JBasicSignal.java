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

import java.util.TreeMap;

import org.fernwood.jbasic.JBasic;

/**
 * This class handles signal stacks. These are LIFO stacks describing what
 * actions are to be taken when errors (signals) occur. Each stack entry
 * reflects an ON statement in the running program. Markers can be pushed on the
 * stack as well as ON units that are used to describe an execution frame. When
 * a program exits, all signals on the stack down to the marker are discarded.
 * 
 * <br>
 * There is a current implementation limit of 100 active ON-statements at one
 * time.
 * 
 * @author tom
 * @version version 1.0 Aug 15, 2006
 * 
 */
public class JBasicSignal {

	private TreeMap<String, String> signalStack[];

	private int sp;

	private int size;

	/**
	 * Flag indicating if debugging of signals (both signaling and catching) is
	 * to be performed.
	 */
	public boolean fDebugSignals;

	/**
	 * Determine if the signal stack is valid to have an item popped off of it
	 * or pushed onto it. This basically checks to ensure that the stack pointer
	 * is in range for the size of the signal stack.
	 * 
	 * @return True if the stack pointer is in a valid range, or false if there
	 *         has been an underflow or overflow.
	 */
	public boolean stackValid() {
		return ((sp > 0) && (sp < size - 1));
	}

	/**
	 * Initialize a signal stack, by creating a stack for 200 nested call frames.
	 * 
	 */
	public JBasicSignal() {
		initStack(JBasic.CALL_DEPTH_LIMIT);
	}

	/**
	 * Initialize a signal stack, with a caller-provided size.
	 * 
	 * @param stackSize
	 *            The number of active call frames that can have pending signals
	 *            at one time.
	 */
	public JBasicSignal(final int stackSize) {
		initStack(stackSize);
	}

	/**
	 * Complete the initialization of the signal stack with a known size for the
	 * pending frame stack size. This creates the array of TreeMaps that hold
	 * each signal that is stored for a given stack, and also initializes the
	 * stack pointer.
	 * 
	 * @param count
	 */
	@SuppressWarnings("unchecked") 
	private void initStack(final int count) {
		size = count;
		signalStack = new TreeMap[size];
		sp = 0;
		signalStack[0] = new TreeMap<String,String>();
	}

	/**
	 * Return the number of active stack frames.
	 * 
	 * @return An integer indicating how many active program or function frames
	 *         there are. Returns zero if no program is running.
	 */
	public int stackSize() {
		return sp;
	}

	/**
	 * For a given position in the signal stack, return the number of ON
	 * statements that have been stored for that frame.
	 * 
	 * @param n
	 *            The stack position number, where 0 is the first program run
	 *            from the console, 1 is any program it calls, 2 is a program
	 *            that is called from 1, etc.
	 * @return The count of active ON statements for the named frame. Returns
	 *         zero if there are no active ON statements at the given frame.
	 */
	public int stackSize(final int n) {
		return signalStack[n].size();
	}

	/**
	 * Mark a part of the stack, and return the mark pointer for a subsequent
	 * drop() later. This puts a marker on the stack and also returns a pointer
	 * so the stack can be flushed to this location later. This version does not
	 * include the fDebugExpressions label.
	 * 
	 * @return A stack marker for later use with drop().
	 */
	public int push() {
		return push("anonymous");
	}

	/**
	 * Mark a part of the stack, and return the mark pointer for a subsequent
	 * drop() later. This puts a marker on the stack and also returns a pointer
	 * so the stack can be flushed to this location later.
	 * 
	 * @param label
	 *            A debugging label stored in the stack.
	 * @return A stack marker for later use with drop().
	 */
	public int push(final String label) {
		sp += 1;
		signalStack[sp] = new TreeMap<String,String>();
		if (fDebugSignals)
			System.out.println("DEBUG: on-error mark(\"" + label + "\") = "
					+ sp);
		return sp;
	}

	/**
	 * Drop away stored on unit specifications down to the given marked
	 * location. This is used when a program goes out of scope and must toss
	 * part of the stack. We step down through the stack to the named location,
	 * and also set the stored data items to null so they can be re-collected by
	 * the GC.
	 * 
	 * @param target
	 *            The value from a previous mark() indicating how much of the
	 *            stack is to be discarded.
	 */
	public void pop(final int target) {
		for (; sp >= target; sp--)
			signalStack[sp] = null;
		if (fDebugSignals)
			System.out.println("DEBUG: on-error pop(" + target + ")");
		return;
	}

	/**
	 * Register a new on statement in the current scope. The command can be
	 * retrieved later with a pop() operation.
	 * 
	 * @param code
	 *            String containing the code to store.
	 * @param command
	 *            String containing the command line for the code.
	 */
	public void store(final String code, final String command) {

		final String c = code.toUpperCase();

		signalStack[sp].put(c, command);
		if (fDebugSignals)
			System.out.println("DEBUG: on-error store(\"" + c + "\", \""
					+ command + "\")");

		return;
	}

	/**
	 * Fetch an active ON unit from the top of the stack if there is one. This
	 * will only fetch units that have been declared in the current scope; i.e.
	 * those declared since the last mark().
	 * 
	 * @param code
	 *            The error that has been signaled. This method searches for an
	 *            ON statement that matches this given code, or an ON ERROR,
	 *            whichever it finds first. The search is done by looking first
	 *            at the active call frame, and then each parent frame up to
	 *            frame number zero (the initial program run).
	 * @return String containing the ON statement to execute.
	 */
	public String fetch(final String code) {

		String c = code.toUpperCase();

		if (sp >= 0) {
			final TreeMap<String, String> r = signalStack[sp];
			if (r == null)
				return null;

			String cmd = r.get(c);
			if (cmd == null) {
				c = "*";
				cmd = r.get(c);
			}

			if ((cmd != null) & fDebugSignals)
				System.out.println("DEBUG: on-error fetch(\"" + c + "\") = \""
						+ cmd + "\"");

			return cmd;
		}

		return null;
	}
}

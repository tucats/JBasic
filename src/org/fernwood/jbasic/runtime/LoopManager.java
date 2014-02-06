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
 * Created on Oct 14, 2008 by tom
 *
 */
package org.fernwood.jbasic.runtime;

import java.util.ArrayList;

/**
 * Manager for handling nested loops in an execution context.
 * @author tom
 * @version version 1.0 Oct 14, 2008
 *
 */
public class LoopManager {


	/**
	 * This is a list of the active FOR-NEXT and DO-WHILE loops in the current
	 * program's context.
	 * 
	 * 
	 */
	private ArrayList<LoopControlBlock> loopStack;

	/**
	 * Create a new instance of a Loop Manager, associated with any running
	 * program.
	 */
	public LoopManager() {
		loopStack = new ArrayList<LoopControlBlock>();
	}
	/**
	 * Add a loop to the top of the loop stack for the current program. This
	 * makes the loop "active".
	 * 
	 * @param theLoop the loop control block to add to the loop stack.
	 */
	public void addLoop(final LoopControlBlock theLoop) {
		if (loopStack == null)
			loopStack = new ArrayList<LoopControlBlock>();

		/*
		 * If this is a FOR-NEXT loop and there is already one on the stack of
		 * the same index name, then this one dumps that loop (and anything that
		 * was nested within it)
		 */

		if (theLoop.loopType == LoopControlBlock.LOOP_FOR) {
			final int ixlen = loopStack.size();
			for (int ix = 0; ix < ixlen; ix++) {
				final LoopControlBlock lcb = loopStack
						.get(ix);
				if (lcb.loopType == LoopControlBlock.LOOP_FOR)
					if (lcb.indexVariableName.equalsIgnoreCase(theLoop.indexVariableName)) {
						for (int idx = ix; idx < ixlen; idx++)
							loopStack.remove(ix);
						break;
					}
			}
		}

		/*
		 * Let's add the new on on the stack and we're done.
		 */
		loopStack.add(theLoop);
	}

	/**
	 * Get the size of the current active loopstack.
	 * 
	 * @return The number of active (nested) loops, or zero if there are no
	 *         active loops.
	 */
	public int loopStackSize() {
		if (loopStack == null)
			return 0;
		return loopStack.size();
	}

	/**
	 * Get the top item from the loop stack. The loop stack is not changed by
	 * this operation.
	 * 
	 * @return A LoopControlBlock previously stored via a call to addLoop()
	 */
	public LoopControlBlock topLoop() {
		final int ix = loopStackSize();
		if (ix < 1)
			return null;
		return loopStack.get(ix - 1);
	}

	/**
	 * Remove the top loop stack item. This is done when a loop completes and is
	 * no longer active.
	 */
	public void removeTopLoop() {
		final int ix = loopStackSize();
		if (ix < 1)
			return;
		loopStack.remove(ix - 1);
	}

	/**
	 * Find a loop control block from the stack. Intermediate active (nested)
	 * loops that do not match the index or are not FOR-NEXT loops are
	 * discarded.
	 * 
	 * @param index
	 *            A string containing the name of the index variable for the FOR
	 *            statement.
	 * @return The active loop control block for the matching index variable.
	 *         Null is returned if there is no matching index or there are no
	 *         active loops.
	 */
	public LoopControlBlock findLoop(final String index) {

		int sp = loopStackSize();
		if (sp < 1)
			return null;
		LoopControlBlock loop = null;

		// Get the top item, and make sure it matches
		// our index variable. If not, discard (popping
		// for loops as we go) and try again. If we
		// exhaust the for-loop stack, then error.

		sp = sp - 1; // Convert stack size to index

		while (true) {
			loop = loopStack.get(sp);
			if (loop.equalsFor(index))
				break;
			loopStack.remove(sp);
			sp--;
			if (sp < 0)
				return null;

		}
		return loop;

	}

}

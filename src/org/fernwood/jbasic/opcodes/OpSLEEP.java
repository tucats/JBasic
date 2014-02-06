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
package org.fernwood.jbasic.opcodes;

import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * The _SLEEP opcode causes the current thread to sleep for a given number
 * of seconds.  The delay can be given as an integer or double value in the
 * instruction.  If neither is given, then the argument is popped from the
 * stack (the default case).  The number of seconds can be a fractional
 * value, such as .01 for a hundredth of a second sleep.
 * @author cole
 * 
 */
public class OpSLEEP extends AbstractOpcode {

	/**
	 * <b><code>SLEEP <em>secs</em></code><br><br></b>
	 * Execute the _SLEEP instruction at runtime.  If an integer or
	 * double argument is given, this is the number of seconds to
	 * sleep.  If neither is given, then an argument is taken from
	 * the stack.
	 * 
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li><code>secs</code> - The number of seconds to sleep.</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 *
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */	public void execute(final InstructionContext env) throws JBasicException {

		Value interval = null;

		if( env.instruction.integerValid)
			interval = new Value(env.instruction.integerOperand);
		else
			if( env.instruction.doubleValid)
				interval = new Value(env.instruction.doubleOperand);
			else
				interval = env.pop();
		
		
		long milliseconds = (long) (interval.getDouble() * 1000);

		if (milliseconds < 1)
			return;

		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

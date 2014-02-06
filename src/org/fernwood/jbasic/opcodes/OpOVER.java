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
 * Extract a value from within the stack and put it on the top of the stack.
 * Revised to create a fixed size local array rather than a Vector, for 
 * performance reasons.
 * @author tom
 * 
 */
public class OpOVER extends AbstractOpcode {

	public void execute(final InstructionContext env) throws JBasicException {

		final int count = env.instruction.integerOperand;			
		final Value[] t = new Value[count];
		int ix;

		/*
		 * Pluck items off the stack and copy to the local vector.
		 */

		for (ix = 1; ix < count; ix++)
			t[ix-1] = env.popForUpdate();
		

		/*
		 * Now we are at the target value that will go back on top of stack.
		 */
		Value q = env.popForUpdate();

		/*
		 * Return the other values to the stack IN THE REVERSE ORDER REMOVED so
		 * the stack remains in the same order.
		 */

		for (ix = count - 1; ix > 0; ix--) {
			env.push(t[ix - 1]);
		}

		/*
		 * Finally, put our "target" value back on the stack.
		 */

		env.push(q);

	}

}

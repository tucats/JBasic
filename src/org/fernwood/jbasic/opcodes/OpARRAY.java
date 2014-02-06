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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpARRAY extends AbstractOpcode {

	/**
	 *  <b><code>_ARRAY <em>count</em></code><br><br></b>
	 * Generate an array value on the stack, filled with objects
	 * also removed from the stack.
	 * 
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li><code>count</code> - the number of array elements that are to
	 * be popped off of the stack and placed in the array object that
	 * is to be created.</li>
	 * </list><br><br>
	 * <b>Implicit Arguments:</b><br>
	 * <list>
	 * <li><code>stack[tos..tos-count]</code> - The elements to remove
	 * from the stack and place in the array.</li>
	 * </list><br><br>
	 * The count can be zero,
	 * in which case no elements are removed from the stack and the resulting
	 * array is empty.  If the count is non-zero, then the array elements
	 * are removed from the stack in reverse order.  That is, the top-of-stack
	 * element is placed in the last array slot, and the tos-count array
	 * element is placed in the first array slot.<br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final int arraySize = env.instruction.integerOperand;

		if( arraySize < 0 )
			throw new JBasicException(Status.BADSIZE, Integer.toString(arraySize));
		/*
		 * Construct a new empty array.
		 */
		final Value newArray = new Value(Value.ARRAY, null);

		/*
		 * Items are backwards on the stack, so pop then off and 
		 * store in the array from back to front.
		 */

		for (int ix = 0; ix < arraySize; ix++) {
			newArray.setElement(env.codeStream.pop().copy(), arraySize - ix);
		}
		
		env.codeStream.push(newArray);

	}

}

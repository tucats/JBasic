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

import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;

/**
 * @author cole
 * 
 */
public class OpCONCAT extends AbstractOpcode {

	/**
	 *  <b><code>_CONCAT  ["<em>string</em>"]</code><br><br></b>
	 * Execute the _CONCAT instruction at runtime, which performs
	 * a <em>string</em> concatenation of two values, one of which
	 * may be a constant in the string operand field.  The value to
	 * concatenate will be taken from the stack if it is not in the
	 * instruction itself.
	 * 
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li>&nbsp;&nbsp;<code>"string"</code> - the optional value to concatenate
	 * </list><br><br>
	 * <b>Implicit Arguments:</b><br>
	 * <list>
	 * <li>&nbsp;&nbsp;<code>stack[tos]</code> - value to concatenate to</l1>
	 * <li>&nbsp;&nbsp;<code>stack[tos-1]</code> - optional value to concatenate</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final Instruction i = env.instruction;
		String concatValue;

		/*
		 * The value to concatenate might be a character (integer ASCII value
		 * in the instruction), a string literal in the instruction, or a 
		 * value popped from the stack.
		 */
		if (i.integerValid)
			concatValue = String.valueOf((char) i.integerOperand);
		else if (i.stringValid)
			concatValue = i.stringOperand;
		else
			concatValue = env.pop().getString();

		/*
		 * Value to concatenate onto is always popped from the stack.  The
		 * string values are concatenated and the result pushed back.
		 */
		env.push(env.pop().getString() + concatValue);

	}

}

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
import org.fernwood.jbasic.statements.SortStatement;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpSORT extends AbstractOpcode {

	/**
	 * Sort an array that's on top of the stack. If the string operand is given,
	 * it's for a record sort, else it's a standard sort of scalars.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		Status status = null;

		/*
		 * Get the array that we're to sort, put there previously.
		 */
		Value value1 = env.pop();
		if (value1.fReadonly)
			throw new JBasicException(Status.READONLY, value1.toString());

		value1 = value1.copy();
		if (env.instruction.stringValid) {
			final String memberName = env.instruction.stringOperand;
			status = SortStatement.sortArray(value1, memberName);
		} else
			status = SortStatement.sortArray(value1);

		if (status.success())
			env.push(value1);
		else
			throw new JBasicException(status);

	}

}

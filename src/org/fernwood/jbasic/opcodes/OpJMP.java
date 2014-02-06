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

/**
 * @author cole
 * 
 */
public class OpJMP extends AbstractOpcode {

	/**
	 * Branch to a constant label location.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * This can only be done when there is a program, and therefore
		 * statements to branch to...
		 */
		if (env.codeStream.statement.program == null)
			throw new JBasicException(Status.NOACTIVEPGM);

		/*
		 * If we were already able to divine the address during compile time,
		 * then we already know the destination address. Otherwise, we have been
		 * given the label and must work it out at runtime.
		 */
		int ix;
		if (env.instruction.integerValid)
			ix = env.instruction.integerOperand;
		else
			ix = env.codeStream.statement.program.findLabel(env.instruction.stringOperand);

		/*
		 * If we don't have a valid label at this point, there's no hope.
		 */
		if (ix < 1)
			throw new JBasicException(Status.NOSUCHLABEL, env.instruction.stringOperand);

		throw new JBasicException(Status.FAULT, "Unlinked _JMP");
	}

}

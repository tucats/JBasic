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
public class OpDATA extends AbstractOpcode {

	/**
	 * Process inline data constants. Skip as many instructions as necessary to
	 * keep going with the next statement.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		if (env.codeStream.statement == null)
			throw new JBasicException(Status.NOACTIVEPGM);
		if (env.codeStream.statement.program == null)
			throw new JBasicException(Status.NOACTIVEPGM);

		final int instructionCount = env.instruction.integerOperand;
		if( instructionCount < 1 )
			throw new JBasicException(Status.FAULT, 
					new Status(Status.INVOPARG, instructionCount));

		env.codeStream.programCounter += instructionCount;

		return;
	}

}

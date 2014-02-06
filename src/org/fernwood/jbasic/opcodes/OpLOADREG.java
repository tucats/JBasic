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
public class OpLOADREG extends AbstractOpcode {

	/**
	 * Fetch a value from a temporary register and store in on the stack. If
	 * the caller tries to access a register that does not exist, then a FAULT
	 * is thrown.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		if( env.codeStream.registers == null )
			throw new JBasicException(Status.FAULT, new Status(Status.REGARRSIZ, "0"));

		int regNum = env.instruction.integerOperand;
		
		if ((regNum < 0) | (regNum >= env.codeStream.registers.size()))
			throw new JBasicException(Status.FAULT, new Status(Status.REGNUM, regNum));
		
		env.push(env.codeStream.registers.get(regNum).copy());

	}

}

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
import org.fernwood.jbasic.runtime.RegisterArray;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpSTORREG extends AbstractOpcode {

	/**
	 * Store top of stack in temporary register
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		
		final Value value1 = env.popForUpdate();

		if( env.codeStream.registers == null )
			env.codeStream.registers = new RegisterArray(100);

		final int idx = env.instruction.integerOperand;
		
		if ((idx < 0) | (idx >= env.codeStream.registers.size()))
			throw new JBasicException(Status.FAULT, 
				new Status(Status.REGNUM, idx));
		
		env.codeStream.registers.set(idx, value1);

	}

}

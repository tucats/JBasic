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
public class OpSBOX extends AbstractOpcode {

	/**
	 * Signal an error if we are in "Sandbox" mode.  Check the conditions defined by the
	 * parameter, and throw exceptions if we are attempting an illegal operation. Otherwise,
	 * execution continues normally.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final int mode = env.instruction.integerOperand;
		if( mode != 0 & mode != 1 )
			throw new JBasicException(Status.FAULT,
					new Status(Status.INVOPARG, mode));
		
		boolean requiredState = (mode == 1);
		boolean specificPermission = env.instruction.stringValid;
		boolean sandBox = env.session.getBoolean("SYS$SANDBOX");
		
		if( specificPermission && env.instruction.stringOperand.equals("ASM") && sandBox)
			throw new JBasicException(Status.SANDBOX, "ASM");
			
		if( !specificPermission && sandBox != requiredState)
			throw new JBasicException(Status.SANDBOX, "ANY");

		env.session.checkPermission(env.instruction.stringOperand);		
	}

}

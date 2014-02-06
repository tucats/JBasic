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
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpLINE extends AbstractOpcode {

	/**
	 * Input a line of text into a variable from console or file, with optional
	 * prompt.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value value1;
		JBasicFile lineInputFile = env.session.stdin();
		final int mode = env.instruction.integerOperand;
		
		if (mode == 1) {
			value1 = env.pop();
			lineInputFile = JBasicFile.lookup(env.session, value1);
			if (lineInputFile == null)
				throw new JBasicException(Status.FNOPEN, value1.toString());
		}
		else if( mode != 0 )
			throw new JBasicException(Status.FAULT, 
					new Status(Status.INVOPARG, mode));

		if ((lineInputFile.getMode() != JBasicFile.MODE_INPUT) &&
				(lineInputFile.getMode() != JBasicFile.MODE_PIPE) &&
				(lineInputFile.getMode() != JBasicFile.MODE_SOCKET) &&
				(lineInputFile.getMode() != JBasicFile.MODE_QUEUE))
			throw new JBasicException(Status.WRONGMODE, "INPUT");

		if (env.instruction.stringValid)
			env.session.stdout.print(env.instruction.stringOperand);
		env.push(lineInputFile.read());

		return;
	}

}

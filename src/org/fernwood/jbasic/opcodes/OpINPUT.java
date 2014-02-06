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
import org.fernwood.jbasic.runtime.JBFInput;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpINPUT extends AbstractOpcode {

	/**
	 * INPUT fileflag <br>
	 * <br>
	 * 
	 * Input a data item from the console or input stream, and leave the
	 * resulting value on the stack.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		JBFInput inputFile = (JBFInput) env.session.stdin();
		final int mode = env.instruction.integerOperand;
		
		if (mode == 1) {
			Value fileDescriptor = env.pop();
			final JBasicFile t = JBasicFile.lookup(env.session, fileDescriptor);
			if (t == null)
				throw new JBasicException(Status.FNOPEN);
			if (t.getMode() != JBasicFile.MODE_INPUT &&
				t.getMode() != JBasicFile.MODE_PIPE &&
				t.getMode() != JBasicFile.MODE_SOCKET )
				throw new JBasicException(Status.WRONGMODE, "INPUT");
			inputFile = (JBFInput) t;
		}
		else
		if( mode != 0 )
			throw new JBasicException(Status.FAULT, 
					new Status(Status.INVOPARG, mode));

		/*
		 * Parse a single token item from the input stream. Keep unused buffer
		 * contents in the file variable.
		 */
	
		env.push(inputFile.readValue());

	}

}

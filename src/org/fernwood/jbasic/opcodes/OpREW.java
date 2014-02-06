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

import org.fernwood.jbasic.runtime.JBasicException;

/**
 * @author cole
 * 
 */
public class OpREW extends AbstractOpcode {

	/**
	 * Rewind READ stream to first DATA statement element.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		env.checkActive();
		int code = 0;
		if( env.instruction.integerValid)
			code = 1;
			else
				if( env.instruction.stringValid)
					code = 2;
		
		switch( code ) {
		
		case 0:
			env.codeStream.statement.program.rewindDataElements();
			break;
			
		case 1:
			env.codeStream.statement.program.rewindDataElements(env.instruction.integerOperand);
			break;
			
		case 2:
			env.codeStream.statement.program.rewindDataElements(env.instruction.stringOperand);
			break;
		}

	}

}

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

import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;

/**
 * @author cole
 * 
 */
public class OpGOTO extends AbstractOpcode {

	/**
	 * Branch to a statement label location.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * This can only be done when there is a program, and therefore
		 * statements to branch to...
		 */
		
		Program pgm = env.codeStream.statement.program;
		
		if (pgm == null)
			throw new JBasicException(Status.NOACTIVEPGM);

		int ix = 0;
		int target = -1;
		int lineNumber = env.instruction.integerOperand;

		for( ix = 0; ix < env.codeStream.size(); ix++) {
			Instruction inst = env.codeStream.getInstruction(ix);
			if( inst.opCode != ByteCode._STMT)
				continue;
			
			if( inst.integerValid && inst.integerOperand == lineNumber ) {
				target = ix + 1;
				env.instruction.opCode = ByteCode._BR;
				env.instruction.integerValid = true;
				env.instruction.integerOperand = target;
				break;
			}
		}
		
		if( target < 0 ) {
			throw new JBasicException(Status.LINENUM, Integer.toString(lineNumber));
		}
		/*
		 * Set the next statement to execute, and return the status code that
		 * says that the next pointer has been explicitly set.
		 */
		
		env.codeStream.programCounter = target;


	}

}

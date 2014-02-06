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
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpLE extends AbstractOpcode {

	/**
	 * Pop two items from stack, and push back a boolean that says if the first
	 * item is less than or equal to the second one.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		Value value2 = null;
		
		if( env.instruction.integerValid)
			value2 = new Value(env.instruction.integerOperand);
		else
			if( env.instruction.doubleValid )
				value2 = new Value(env.instruction.doubleOperand);
			else
				if( env.instruction.stringValid)
					value2 = new Value(env.instruction.stringOperand);
				else 
					value2 = env.pop();
		
		final Value value1 = env.pop();

		env.push(value1.compare(value2) <= 0);

		return;
	}

}

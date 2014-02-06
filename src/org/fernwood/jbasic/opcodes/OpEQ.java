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
public class OpEQ extends AbstractOpcode {

	/**
	 * Compare top two stack items and push a boolean indicating if they were
	 * equal.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		final Value value2 = env.pop();
		final Value value1 = env.pop();
		
		boolean state;
		
		/*
		 * If both types are RECORD types, then we must use the MATCH function
		 * which can only test for equal or not equal.  For other data types,
		 * we can do an ordinal comparison.
		 */
		if( value1.getType() == Value.RECORD && value2.getType() == Value.RECORD)
			state = value1.match(value2);
		else
			state = (value1.compare(value2) == 0);

		if( env.instruction.integerOperand > 0 ) {
			if( state )
				env.codeStream.programCounter = env.instruction.integerOperand;
		}
		else
			env.push(state);
		
		return;
	}

}

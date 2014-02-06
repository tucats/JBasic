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
public class OpLENGTH extends AbstractOpcode {

	/**
	 * Determine the lenght of the top item on the stack, depending on type, and
	 * push the length back as an integer.  If the item is a numeric type, the
	 * length pushed is the length of the item if formatted as a string.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		
		
		Value value1 = null;
		
		if( env.instruction.stringValid) {
			final String symbolName = env.instruction.stringOperand;
			value1 = env.localSymbols.reference(symbolName);
		}
		else
			value1 = env.pop();
		final int theType = value1.getType();
		
		/*
		 * Check type based in order of most likely underlying data type.
		 */
		if (theType == Value.STRING)
			env.push(new Value(value1.getString().length()));
		
		else if (theType == Value.ARRAY)
			env.push(new Value(value1.size()));
		
		else if (theType == Value.RECORD)
			env.push(new Value(value1.memberCount()));
		else if (theType == Value.TABLE )
			env.push(new Value(value1.size()));
		else
			env.push(value1.displayFormat().length());


		return;
	}

}

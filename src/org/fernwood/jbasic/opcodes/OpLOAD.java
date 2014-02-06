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
public class OpLOAD extends AbstractOpcode {

	/**
	 * Load a symbol value on the stack
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * Use the string argument to locate a copy of the actual value.
		 */
		final String symbolName = env.instruction.stringOperand;
		Value item = env.localSymbols.value(symbolName);
		env.codeStream.refPrimary(symbolName, false);
		
		/*
		 * If the LOAD contains a required type, do the coercion now.
		 */

		if (env.instruction.integerValid && (env.instruction.integerOperand > 0))
			if(env.instruction.integerOperand == Value.QUOTED_FORMATTED_STRING) {
				item = new Value(Value.toString(item, true));
			}
			else
				item = item.newAsType(env.instruction.integerOperand);

		/*
		 * Put it back on the stack and we're done.
		 */
		env.push(item);

		return;
	}

}

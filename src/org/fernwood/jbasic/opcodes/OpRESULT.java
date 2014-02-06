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
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpRESULT extends AbstractOpcode {

	/**
	 * Get the result from a program/function call and store it in the variable
	 * identified by the string result.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		Value value1;
		final String varName = env.instruction.stringOperand;

		try {
			value1 = env.localSymbols.reference("ARG$RESULT");
		} catch (JBasicException e) {
			throw new JBasicException(Status.EXPRETVAL, varName);
		}


		/*
		 * If there is a designated name to store the value to in the instruction,
		 * we'll use that. Otherwise, we just leave it on the stack since it
		 * must have a more complex LVALUE requirement and code is generated
		 * separately to store it (and example would be an array reference).
		 */

		if( env.instruction.stringValid) {
			/*
			 * Attempt to store the result in the local variable of the given name
			 * (which must not be readonly). 
			 */

			env.localSymbols.insertLocal(varName, value1.copy());
		}
		else
			env.push(value1.copy());
		
		/*
		 * Last step, delete the temporary that held the result value.
		 */
		env.localSymbols.delete("ARG$RESULT");

	}
}

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
public class OpREAD extends AbstractOpcode {

	/**
	 * READ a datum from the active DATA statement(s) and push on the stack.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value value1;

		if (env.codeStream.statement == null)
			throw new JBasicException(Status.NOACTIVEPGM);
		if (env.codeStream.statement.program == null)
			throw new JBasicException(Status.NOACTIVEPGM);

		value1 = env.codeStream.statement.program.getNextDataElement(env.localSymbols);
		if( value1 == null )
			throw new JBasicException(Status.EOD);
		
		/**
		 * If we have an LVALUE symbol as our string parameter, store directly
		 * into that.  Otherwise, push it on the stack for more processing.
		 */
		
		if( env.instruction.stringValid)
			env.localSymbols.insert(env.instruction.stringOperand, value1);
		else
			env.push(value1);

		return;
	}

}

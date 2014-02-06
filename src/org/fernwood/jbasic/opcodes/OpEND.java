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
public class OpEND extends AbstractOpcode {

	/**
	 * Terminate execution of the ByteCode stream
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * If there was something left on the stack, set it up as the ARG$RESULT
		 * for our caller. This can pass function results back upstream.
		 */

		int popResult = env.codeStream.getPopReturn() ? 1 : 0;
		if (env.instruction.integerValid)
			popResult = env.instruction.integerOperand;

		if ((popResult > 0) && (env.stackSize() > 0)) {
			final Value d = env.pop();
			env.localSymbols.insertLocal("ARG$RESULT", d);
		}

		/*
		 * This instance of the bytecode is done running. Clear out of Dodge.
		 */
		env.codeStream.fRunning = false;
		return;
	}

}

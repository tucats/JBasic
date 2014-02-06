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
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;

/**
 * ENTRY opcode. This is used to define the entry point to a program,
 * function, or method.
 * 
 * @author tom
 * @version version 1.1 Aug 29, 2008
 * 
 */
public class OpENTRY extends AbstractOpcode {

	/**
	 * Entry into a program module. Type code is one of:<br>
	 * <br>
	 * 1 = program <br>
	 * 2 = verb <br>
	 * 3 = function<br>
	 * <p>
	 * Currently the only function this does is to mark the bytecode stream as
	 * needing to capture the top-of-stack if there is one on exit to return as
	 * a function value.
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		if (!env.codeStream.fLinked)
			throw new JBasicException(Status.NOACTIVEPGM);

		if (env.instruction.integerOperand ==  ByteCode.ENTRY_FUNCTION )
			env.codeStream.popReturn(true);

		return;
	}

}

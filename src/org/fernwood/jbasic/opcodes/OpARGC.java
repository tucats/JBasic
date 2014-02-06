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
 * Remove an element from the argument array for the current program and
 * store it in a local variable.
 * @author cole
 * 
 */
public class OpARGC extends AbstractOpcode {

	/**
	 *  <b><code>_ARGC count</code><br><br></b>
	 * Throw an error if the argument count is larger than the operand value.
	 * This is generated as part of FUNCTION and PROGRAM prologs when there
	 * is an argument list to verify that no caller arguments were not matched
	 * to arguments in the declaration.
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li><code>count</code> - the argument count.</l1>
	 * 
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 */

	public void execute(final InstructionContext env) throws JBasicException {

		final Value argList = env.getArgList();

		/*
		 * Determine the size of the argument list.  If there isn't an active
		 * argument list, the size is zero.
		 */
		int size = 0;
		if (argList == null)
			size = 0;
		else
			size = argList.size();
				
		/*
		 * Test this against the instruction operand to see if the argument list
		 * is correctly sized.  If it is too small or too large, throw a suitable
		 * error.
		 */
		if( size < env.instruction.integerOperand )
			throw new JBasicException(Status.INSFARGS);
			
		if( size > env.instruction.integerOperand )
			throw new JBasicException(Status.TOOMANYARGS);
	}

}

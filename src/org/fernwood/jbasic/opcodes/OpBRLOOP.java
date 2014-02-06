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
import org.fernwood.jbasic.runtime.LoopManager;

/**
 * @author cole
 * 
 */
public class OpBRLOOP extends AbstractOpcode {

	/**
	 *  <b><code>_BRLOOP <em>dest</em></code><br><br></b>
	 * Branching is about to be done outside the loop body, so
	 * this instruction clears the top loop descriptor off the
	 * stack.  It then does a transfer to the destination
	 * address.
	 * 
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li><code>dest</code> - destination of branch, outside the loop body.</li>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		env.checkActive();
		
		int dest = env.instruction.integerOperand;
		
		LoopManager lm = env.codeStream.statement.program.loopManager;
		
		if( lm == null || lm.loopStackSize() == 0 )
			throw new JBasicException(Status.NOLOOP);
		lm.removeTopLoop();
		
		env.codeStream.programCounter = dest;
		
	}

}

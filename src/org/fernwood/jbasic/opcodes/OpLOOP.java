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
import org.fernwood.jbasic.runtime.LoopControlBlock;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpLOOP extends AbstractOpcode {

	/**
	 * End of a do-WHILE or do-UNTIL loop
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		if (!env.codeStream.statement.program.hasLoops()) {
			String loopType = "LOOP";
			final String loopTypes[] = new String[] { "LOOP", "UNTIL", "WHILE" };
			if( env.instruction.integerValid)
				loopType = loopTypes[env.instruction.integerOperand];
			throw new JBasicException(Status.NODO, loopType);
		}
		
		
		final LoopControlBlock endloop = env.codeStream.statement.program.loopManager.topLoop();
		Value value1;
		
		/*
		 * If there isn't anything on the stack, this was a DO..LOOP with no
		 * condition.  Treat it as a loop-forever.  Otherwise, get the stack
		 * value.
		 */
		if( env.stackSize() < 1 ) 
			value1 = new Value(true);
		else
			value1 = env.pop();
		
		value1.coerce(Value.BOOLEAN);
		
		if (value1.getBoolean()) {
			env.codeStream.programCounter = endloop.statementID + 1;
		} else
			env.codeStream.statement.program.loopManager.removeTopLoop();
	}

}

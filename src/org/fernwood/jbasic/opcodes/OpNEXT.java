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
import org.fernwood.jbasic.statements.Statement;

/**
 * @author cole
 */
public class OpNEXT extends AbstractOpcode {

	/**
	 * NEXT "indexvar"
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		LoopControlBlock loop;
		boolean done;
		Statement stmt = env.codeStream.statement;
		String index = env.instruction.stringOperand;
		
		if (!stmt.program.hasLoops())
			throw new JBasicException(Status.NOFOR);

		loop = stmt.program.loopManager.findLoop(index);
		if (loop == null)
			throw new JBasicException(Status.FORINDEX, index);

		done = loop.evaluate();

		/*
		 * If we're done, we can delete the loop item and return
		 */

		if (done) {
			stmt.program.loopManager.removeTopLoop();
			return;
		}

		/*
		 * We must loop again, so set the next statement
		 * back to the statement ID of the statement that
		 * follows the _FOR (which is stored in the loop
		 * control block) and indicate that we plan on a
		 * branch return code.
		 */

		stmt.program.next = loop.statementID;

		/*
		 * Use the operand to return to the top of the loop. 
		 */
		
		env.codeStream.programCounter = env.instruction.integerOperand;
		return;

	}

}

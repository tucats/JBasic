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
import org.fernwood.jbasic.runtime.LoopManager;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpFOR extends AbstractOpcode {

	/**
	 * Start of a FOR-NEXT loop. The top three items are popped and used as
	 * start, end, and increment values. The index variable is the string
	 * parameter to the instruction.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final LoopControlBlock loop = new LoopControlBlock(
				LoopControlBlock.LOOP_FOR, env.localSymbols);

		loop.indexVariableName = env.instruction.stringOperand;

		final Value value3 = env.pop(); /* Increment */
		final Value value2 = env.pop(); /* end */
		final Value value1 = env.popForUpdate(); /* start, used as initial value */

		if( !env.codeStream.fDynamicSymbolCreation && env.localSymbols.localReference(loop.indexVariableName) == null)
			throw new JBasicException(Status.UNKVAR, loop.indexVariableName);
		
		env.localSymbols.insert(loop.indexVariableName, value1);

		final Value z = value1.copy();
		z.coerce(Value.DOUBLE);

		final double start_value = z.getDouble();

		loop.end = value2.copy();
		loop.increment = value3.copy();

		// It is possible that the loop won't need to execute at all.
		// In that event, we should find the matching NEXT and toss
		// all the intervening statements. Also, we do this if the
		// increment would never move anything because it's zero.

		final Value x = loop.increment.copy();
		x.coerce(Value.DOUBLE);
		final Value y = loop.end.copy();
		y.coerce(Value.DOUBLE);

		if (x.getDouble() == 0.0)
			throw new JBasicException(Status.FORINCR);

		if (((x.getDouble() > 0.0) && (start_value > y.getDouble()))
				|| ((x.getDouble() < 0.0) && (start_value < y.getDouble()))) {

			/*
			 * The loop need not run at all. 
			 */
			if (env.instruction.integerOperand > 0) {
				env.codeStream.programCounter = env.instruction.integerOperand;
				return;
			}

			throw new JBasicException(Status.FAULT, "Unlinked _FOREACH");
		}
		
		/*
		 * Remember the location we'll branch back to at the end of each loop.
		 */
		loop.statementID = env.codeStream.statement.statementID + 1;

		/*
		 * Add the loop to the loop manager.  The loop manager normally
		 * already exists, but not if this is a FOR..DO statement.  In 
		 * such cases, go ahead and create a loop manager even though it
		 * will live only as long as the FOR..DO statement.
		 */
		if( env.codeStream.statement.program.loopManager == null )
			env.codeStream.statement.program.loopManager = new LoopManager();
		env.codeStream.statement.program.loopManager.addLoop(loop);

		return;
	}

}

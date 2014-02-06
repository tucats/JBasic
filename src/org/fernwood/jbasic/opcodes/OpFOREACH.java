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
public class OpFOREACH extends AbstractOpcode {

	/**
	 * Start of a FOR-NEXT loop. The top three items are popped and used as
	 * start, end, and increment values. The index variable is the string
	 * parameter to the instruction.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final LoopControlBlock loop = new LoopControlBlock(
				LoopControlBlock.LOOP_FOREACH, env.localSymbols);

		loop.indexVariableName = env.instruction.stringOperand;

		Value elementList = env.pop();
		
		/*
		 * If the data isn't already an array, then we need to make a copy of it and coerce
		 * the type.  The copy is required so references don't get unwanted conversions. Because
		 * these are usually arrays, we don't use popForUpdate() above because we usually don't
		 * need (or want) to make cumbersome copies of large data...
		 */
		if( elementList.getType() != Value.ARRAY) {
			elementList = elementList.copy();
			elementList.coerce(Value.ARRAY);
			
			//throw new JBasicException(Status.WRONGTYPE, "ARRAY");
		}
		
		String elementListName = loop.createEachList();
		env.localSymbols.insert(elementListName, elementList);
		loop.eachLength = elementList.size();
		loop.eachCounter = 1;
		
		// It is possible that the loop won't need to execute at all.
		// In that event, we should find the matching NEXT and toss
		// all the intervening statements. Also, we do this if the
		// increment would never move anything because it's zero.

		if( loop.eachLength == 0 ) {
			/*
			 * The loop need not run at all. If we are not LINKED, then we'll
			 * have to search for the NEXT statement that goes with this. If we
			 * are linked, then the linker already told us where the instruction
			 * is we care about. Use it.
			 */
			if (env.instruction.integerOperand > 0) {
				env.codeStream.programCounter = env.instruction.integerOperand;
				return;
			}
			throw new JBasicException(Status.FAULT, "Unlinked _FOREACH");
		}
		
		/*
		 * Remember the location where we branch to at the bottom of each loop.
		 */
		loop.statementID = env.codeStream.statement.statementID + 1;
		
		/*
		 * And make sure that the index variable exists and has the initial
		 * value from the array of possible values.
		 */
		
		if( !env.codeStream.fDynamicSymbolCreation && env.localSymbols.localReference(loop.indexVariableName) == null)
			throw new JBasicException(Status.UNKVAR, loop.indexVariableName);
		
		env.localSymbols.insert(loop.indexVariableName, elementList.getElement(1));
		
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

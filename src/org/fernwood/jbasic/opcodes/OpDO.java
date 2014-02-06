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

import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.LoopControlBlock;

/**
 * @author cole
 * 
 */
public class OpDO extends AbstractOpcode {

	/**
	 * Top of a DO-while or DO-until loop
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		env.checkActive();
		
		final LoopControlBlock loop = new LoopControlBlock(
				LoopControlBlock.LOOP_DO, env.localSymbols);
		
		/*
		 * Store the starting and ending points of the loop in
		 * the loop control block.
		 */
		if (env.codeStream.fLinked) {
			ByteCode bc = env.codeStream;

			loop.statementID = env.codeStream.programCounter - 1;
			loop.startAddress = bc.programCounter;

			/*
			 * Search ahead for the matching _LOOP
			 */
			
			int nest = 0;
			
			for( int idx = bc.programCounter+1; idx < bc.size(); idx++ ) {
				final Instruction i = bc.getInstruction(idx);
				if( i.opCode == ByteCode._DO)
					nest++;
				else
					if( i.opCode == ByteCode._LOOP) {
						if( nest == 0 ) {
							loop.exitAddress = idx+1;
							break;
						}
						nest--;
					}
			}
		}
		else
			loop.statementID = env.codeStream.statement.statementID;
		
		
		env.codeStream.statement.program.loopManager.addLoop(loop);

		/*
		 * If this is a DO..LOOP where the comparison is done at the
		 * top of the loop (such as DO..WHILE) then branch now to the
		 * end of the loop to do the comparison before anything else.
		 */
		
		if( env.instruction.integerOperand > 0 )
			env.codeStream.programCounter = env.instruction.integerOperand;
		
		return;
	}

}

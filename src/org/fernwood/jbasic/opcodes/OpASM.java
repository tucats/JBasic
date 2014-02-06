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

import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * _ASM accepts a string or array of strings, and compiles them at runtime rather 
 * than compile time.  This is used for creating self-generating algorithms.
 * @author cole
 * 
 */
public class OpASM extends AbstractOpcode {

	/**
	 *  <b><code>_ASM</code><br><br></b>
	 * Assemble and execute a string representing BYTECODE instructions
	 * at runtime.
	 * 
	 * <p><br>
	 * <b>Implicit Arguments:</b><br>
	 * <list>
	 * <li>&nbsp;&nbsp;<code>stack[tos]</code> - instruction(s)</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		if( env.instruction.stringValid)
			throw new JBasicException(Status.INVOPARG, env.instruction.stringOperand);
		

		/*
		 * Really two modes... if there is an integer operand, it defines the
		 * number of instructions that FOLLOW this opcode that are part of an
		 * ASM statement.  Otherwise, it's a runtime request to pull the data
		 * from the stack.
		 */
		
		if( env.instruction.integerValid) {
			/* 
			 * We have a block of code created by the ASM statement.  This
			 * implies sandbox checking, so do that now.
			 */
			env.session.checkPermission(Permissions.ASM);
			
			/*
			 * No other work done here, the following instructions are placed
			 * by the compiler and just execute normally.
			 */
			return;
		}

		/*
		 * Looks like it's going to be runtime assembly, so get the expression
		 * of the code to assemble.
		 */
		Value opcodes = env.pop();
		StringBuffer asmText = new StringBuffer();

		/*
		 * IF the item to assemble is an array, then construct a string 
		 * from each of the elements, separated by commas.  So an
		 * argument of ["_INTEGER 3", "_OUTNL"] becomes "_INTEGER 3, _OUTNL"
		 */
		if( opcodes.getType() == Value.ARRAY) {
			for( int ix = 1; ix <= opcodes.size(); ix++) {
				if( ix > 1)
					asmText.append(", ");
				asmText.append(opcodes.getString(ix));
			}
		}
		else
			
			/*
			 * Not an array, so just get the string argument.
			 */
			asmText.append(opcodes.getString());
		
		/*
		 * The instructions we're given are initially compiled into a
		 * local bytecode array.  If there is an error assembling the
		 * resulting string, then signal the error.
		 */
		ByteCode bc = new ByteCode(env.session);
		
		Status status = bc.assemble( asmText.toString());
		if( status.failed())
			throw new JBasicException(status);
	
		/*
		 * If optimization of assembly code is enabled, do that now.
		 */
		if( env.session.getBoolean("SYS$OPT_ASM"))
			env.session.pmOptimizer.optimize(bc);
		
		/*
		 * Run the resulting instructions locally.  If there is an
		 * error running them, then signal an error.
		 */
		status = bc.run(env.localSymbols, 0);
		if( status.failed())
			throw new JBasicException(status);
		
		return;
	}
}

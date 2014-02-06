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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.RandomNumberGenerator;

/**
 * @author cole
 * 
 */
public class OpRAND extends AbstractOpcode {

	/**
	 * Generate a pseudo-random-number, or seed the pseudo-random-number 
	 * generator.  Seeding can be done either with a system-generated
	 * timer-based value, or with a seed value on the stack.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		if( JBasic.random == null ) {
			JBasic.random = new RandomNumberGenerator();
		}

		/*
		 * What action are we performing?  The integer operand tell us what
		 * to do.
		 */
		int operation = 0;
		
		if( env.instruction.integerValid )
			operation = env.instruction.integerOperand;

		/*
		 * Code 0 means generate an integer pseudo-random value and push it
		 * on the stack. This supports the RND pseudo-variable.  You can also
		 * get the random value by using the RNDVAR() function which calls the
		 * same generator.
		 */
		
		if( operation == 0 )
		{
			env.push(JBasic.random.get());
			return;
		}
		
		/*
		 * If we are a RANDOMIZE TIMER sort of instruction, then call the
		 * randomizer with no parameter and it will use a bit mast from
		 * the current millisecond counter to initialize the random number
		 * generator.
		 */
		if( operation == 1 ) {
			JBasic.random.randomize();
			return;
		}
		
		/*
		 * Otherwise we have a specific seed value we are to use that 
		 * is popped from the stack.
		 */
		
		if( operation == 2 ) {
			JBasic.random.randomize( env.pop().getInteger());
			return ;
		}
		
		throw new JBasicException(Status.FAULT, new Status(Status.INVOPARG, operation));
	}

}

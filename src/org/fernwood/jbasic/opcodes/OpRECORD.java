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
 * @author cole
 * 
 */
public class OpRECORD extends AbstractOpcode {

	/**
	 * Load an arrayValue constant on the stack, using the top stack items.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final int argc = env.instruction.integerOperand;
		
		/*
		 * There must be an integer argument count given, and it must be a
		 * positive value. This number indicates the number of PAIRS of items
		 * to be pulled from the stack.
		 */
		if( argc < 0 || !env.instruction.integerValid) 
			throw new JBasicException(Status.FAULT, new Status(Status.INVOPARG, Integer.toString(argc)));
		
	
		/*
		 * Items are backwards on the stack, but it doesn't matter since
		 * these are pairs of items with a value and a string key.  Pop
		 * 'em off in pairs and put them in the RECORD object.
		 */
		
		final Value newRecord = new Value(Value.RECORD, null);

		for (int ix = 0; ix < argc; ix++) {
			final Value value = env.pop(); /* Value */
			final Value key = env.pop();   /* member */
			newRecord.setElement(value, key.getString().toUpperCase());

		}
		env.push(newRecord);

	}

}

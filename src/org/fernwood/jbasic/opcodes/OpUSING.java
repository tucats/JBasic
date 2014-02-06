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
public class OpUSING extends AbstractOpcode {

	/**
	 * Format a string buffer using values from stack and a format string.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		String fmtString = env.pop().getString();
		StringBuffer buffer = new StringBuffer();
		StringBuffer remainingFormatString = new StringBuffer(fmtString);
		
		/*
		 * Data pushed on stack left-to-right so we must pull it from the stack
		 * in reverse order.
		 */
		final int argc = env.instruction.integerOperand;
		final int top = env.codeStream.stackSize();
		int ix;
		
		if( argc < 0 )
			throw new JBasicException(Status.FAULT,
					new Status(Status.INVOPARG, argc));

		for (ix = top - argc; ix < top; ix++) {

			final Value value1 = env.codeStream.getStackElement(ix);

			/*
			 * Format the value. We'll use the result (which contains any
			 * "prefix" text as the value to add to the output buffer. But we'll
			 * also use the length as the number of characters to strip off of
			 * the format string so we can process multiple values from the same
			 * string.
			 */
			final String vBuffer = Value.format(value1, remainingFormatString.toString());
			if (vBuffer == null)
				throw new JBasicException(Status.FORMATERR, fmtString);
			
			int remainingChars = remainingFormatString.length();
			int formatLen = vBuffer.length();
			
			if( formatLen > remainingChars)
				throw new JBasicException(Status.FORMATERR, fmtString);

			/*
			 * Remove the consumed characters from the format string, and
			 * add the formatted text to the output buffer.
			 */
			remainingFormatString.delete(0, formatLen);
			buffer.append(vBuffer);

		}

		/*
		 * Now that we've used all the arguments, make sure they're really off
		 * the stack.
		 */

		env.codeStream.discard(argc);

		/*
		 * Put the formatted string buffer back and we're done.
		 */
		env.push(buffer.toString());

	}

}

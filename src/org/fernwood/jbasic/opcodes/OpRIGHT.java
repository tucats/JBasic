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

import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * _RIGHT executes the RIGHT(string, count) function to return the 'count'
 * characters on the right end of 'string'.
 * 
 * @author tom
 * 
 */
public class OpRIGHT extends AbstractOpcode {


	public void execute(final InstructionContext env) throws JBasicException {

		String s = null;
		int slength = 0;
		Value array = null;
		Value source = null;
		
		int count;

		/*
		 * get arguments for _RIGHT/right() function. These are on the stack
		 * in REVERSE ORDER!  Note that the first argument, the count, may
		 * be in the instruction operand.
		 */

		if( env.instruction.integerValid)
			count = env.instruction.integerOperand;
		else
			count = env.pop().getInteger();
		
		
		/*
		 * Get the string, and calculate it's length.  Make sure the count
		 * of characters isn't greater than the actual string length.
		 */
		source = env.pop();
		boolean isArray = (source.getType() == Value.ARRAY);
		if( isArray ) {
			array = new Value(Value.ARRAY, null);
			slength = source.size();
		}
		else {
			s = source.getString();
			slength = s.length();
		}

		/*
		 * If the count isn't positive, then result is just an empty string.
		 */
		if( count < 1 ) {
			if( isArray )
				env.push(array);
			else
				env.push("");
			return;
		}

		count = Math.min(count, slength);

		/*
		 * Push the right-most 'count' characters on the stack and done.
		 */
		
		if( isArray ) {
			for( int ix = slength-count+1; ix<= slength; ix++ )
				array.addElement(source.getElement(ix));
			env.push(array);
		}
		else
			env.push( s.substring(slength - count));
		return ;

	}

}

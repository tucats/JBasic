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
 * _SUBSTR executes the SUBSTR(string, startpos, endpos) function, and is used
 * as runtime support for LEFT(string, count) and RIGHT(string, count) builtins
 * as well.
 * <p>
 * The integer argument indicates if there are two arguments or three... when
 * there are only two arguments, the end of the string is assumed to be the end
 * of the substring range to place back on the stack.
 * 
 * @author tom
 * 
 */
public class OpSUBSTR extends AbstractOpcode {

	public void execute(final InstructionContext env) throws JBasicException {

		
		/*
		 * get arguments for _SUBSTR/substr() function. These are on the stack
		 * in REVERSE ORDER!
		 * 
		 * The instruction integer operand tells how many operands we have (2 or
		 * 3). If only two, then we assume the last operand is the length of the
		 * string.
		 * 
		 */

		int first, last;
		final int argc = env.instruction.integerOperand;
		if( argc < 2 | argc > 3 )
			throw new JBasicException(Status.FAULT,
					new Status(Status.INVOPARG, argc));
		if (argc == 3)
			last = env.pop().getInteger();
		else
			last = 0;
		first = env.pop().getInteger();
		
		Value source = env.pop();
		int slength;
		String s = null;
		boolean isArray = source.getType() == Value.ARRAY;
		
		if( isArray ) 
			slength = source.size();
		else {
			s = source.getString();
			slength = s.length();
		}
		/*
		 * If the end position was implied, find the length of the string
		 * argument now. If it was explicitly given, don't allow selection of
		 * range beyond string length.
		 */

		if (argc == 2)
			last = slength;
		else if (last > slength)
			last = slength;

		/*
		 * If start is past end, it's an empty string
		 */
		if (first > slength) {
			if( isArray )
				env.push(new Value(Value.ARRAY, null));
			else
				env.push("");
			return;
		}

		if (last < first) {
			if( isArray )
				env.push(new Value(Value.ARRAY, null));
			else
				env.push("");
			return;

		}
		/*
		 * Make sure negative numbers result in an empty range.
		 */

		if (first < 1)
			first = 1;
		if (last < 1)
			last = 1;

		/*
		 * If resulting string has no length, just push empty string.
		 */
		if (last - first < 0) {
			if( isArray )
				env.push(new Value(Value.ARRAY, null));
			else
				env.push("");
			return;
		}

		/*
		 * Do the calculation to get the actual substring, and render it as a
		 * new string, and put the result back on the stack and we're done.
		 */
		if( isArray ) {
			Value array = new Value(Value.ARRAY, null);
			for( int ix = first; ix <= last; ix++) {
				array.addElement( source.getElement(ix) );
			}
			env.push( array );
		}
		else
			if( s != null )
				env.push(s.substring(first - 1, last));

	}

}

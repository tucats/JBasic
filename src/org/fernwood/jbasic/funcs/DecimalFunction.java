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
package org.fernwood.jbasic.funcs;

import java.math.BigDecimal;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>DECIMAL()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Convert a value to a DECIMAL string.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>v1 = DECIMAL( v2 )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Return an string containing the integer parameter expressed as a value in the
 * second parameter's radix (1,8, 10, or 16).
 * 
 * @author cole
 * 
 */
public class DecimalFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException if an argument or execution error occurs
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		
		/*
		 * Let's see if this is really a CAST to a DECIMAL data type.
		 */
		
		try {
			arglist.validate(1,2,new int[] { Value.NUMBER, Value.INTEGER});
			int scale = 0;
			if (arglist.size() > 1 )
				scale = arglist.intElement(1);
			
			Value v = new Value(Value.DECIMAL, null);
			Value arg = arglist.element(0);
			switch (arg.getType()) {
			
			case Value.BOOLEAN:
				v.setDecimal(new BigDecimal( arg.getBoolean() ? 1 : 0).setScale(scale, BigDecimal.ROUND_HALF_UP));
				break;
			case Value.INTEGER:
				v.setDecimal(new BigDecimal( arg.getInteger()).setScale(scale, BigDecimal.ROUND_HALF_UP));
				break;
			case Value.DOUBLE:
				v.setDecimal(new BigDecimal( arg.getDouble()).setScale(scale, BigDecimal.ROUND_HALF_UP));
				break;
			case Value.DECIMAL:
				v.setDecimal(arg.getDecimal().setScale(scale, BigDecimal.ROUND_HALF_UP));
				break;
			}
			return v;
		} catch (JBasicException e) {
			
		}
		
		
		final char digits[] = 
			{ '0', '1', '2', '3', '4', '5', '6', '7',
			  '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
			  'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
			  'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
			  'W', 'X', 'Y', 'Z' };

		int base;
		int result = 0;
		
		// There must be one or two arguments, which we make be a string.
		arglist.validate(1, 2, new int[] { Value.STRING, Value.INTEGER });

		String source = arglist.stringElement(0);

		if (arglist.size() > 1)
			base = arglist.intElement(1);
		else
			base = 16;

		if ((base < 2) || (base > 36)) {
			throw new JBasicException(Status.ARGERR, Integer.toString(base));
		}

		for( int ix = 0; ix < source.length(); ix++ ) {
			char ch = source.charAt(ix);
			boolean invalidCharacter = true;
			for( int nibble = 0; nibble < base; nibble++ ) {
				if( ch == digits[nibble]) {
					result = result * base + nibble;
					invalidCharacter = false;
					break;
				}
			}
			if( invalidCharacter) {
				throw new JBasicException(Status.ARGERR, source);
			}
		}

		return new Value(result);

	}
}

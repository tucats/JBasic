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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;
/**
 * <b>RADIX()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Convert value to arbitrary radix</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = RADIX( <em>value</em>, <em>base</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String or Integer</td></tr>
 * </table>
 * <p>
 * If the value argument is a number, then return an string containing 
 * the integer parameter expressed as a value in the
 * second parameter's radix (such as 2, 8, 10, or 16).  For example,
 * <code>RADIX(65,16)</code> returns the string "41" since the 65 expressed
 * in base 16 (hexadecimal) is the value 41.
 * <p>
 * If the value argument is a string, it is assumed to be a value in the
 * given base, and is converted to a decimal integer number.  For example,
 * <code>RADIX("101", 2)</codE> returns the integer 5, since 101 in base 2
 * (binary) is the decimal number 5.
 * 
 * @author cole
 * 
 */
public class RadixFunction extends JBasicFunction {

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

		final char digits[] = 
		{ '0', '1', '2', '3', '4', '5', '6', '7',
		  '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
		  'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
		  'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
		  'W', 'X', 'Y', 'Z' };

		int i, base;
		String s;

		if( arglist.size()  > 0 ) {
			if( arglist.element(0).getType() == Value.STRING ) {
				base = 10;
				i = arglist.size();
				if( i == 2 )
					base = arglist.intElement(1);
				if( base < 2 | base > 35 | i > 2 ) {
					throw new JBasicException(Status.ARGERR);
				}
				s = arglist.stringElement(0).toUpperCase();
				
				int result = 0;
				char ch;
				
				for( i = 0; i < s.length(); i++ ) {
					ch = s.charAt(i);
					if( Character.isWhitespace(ch))
						continue;
					int found = -1;
					for( int ix = 0; ix < digits.length; ix++ ) {
						if( ch == digits[ix]) {
							found = ix;
							break;
						}
					}
					if( found < 0 ) {
						throw new JBasicException(Status.ARGERR);
					}
					if ( found >= base ) {
						throw new JBasicException(Status.ARGERR);
					}
					result = (result * base) + found;
				}
				return new Value(result);
			}
		}
		
		// There must be a single argument, which we make be a string.
		arglist.validate(1, 2, new int[] { Value.INTEGER, Value.INTEGER });

		i = arglist.intElement(0);

		if (arglist.size() > 1)
			base = arglist.intElement(1);
		else
			base = 10;

		if ((base < 2) || (base > 36)) {
			throw new JBasicException(Status.ARGERR);
		}
		if (i < 0) {
			throw new JBasicException(Status.ARGERR);
		}

		s = "";
		if (i == 0)
			s = "0";
		else
			while (i > 0) {
				final int d = i % base;
				i = i / base;
				s = digits[d] + s;
			}

		return new Value(s);

	}
}

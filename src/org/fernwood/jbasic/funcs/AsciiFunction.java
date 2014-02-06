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

import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>ASCII()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>ASCII character value</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>i = ABS( s )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Integer</td></tr>
 * </table>
 * <p>
 * Return an integer value with the ASCII code of the first character of a
 * string. If the string is longer than one character, only the first character
 * is used. For example, <codE>ASCII("A")</code> results in the integer value
 * 65.
 * @author cole
 */

public class AsciiFunction extends JBasicFunction {

	/**
	 * Execute the function
	 * 
	 * @param arglist
	 *            The function argument list
	 * @param symbols
	 *            The runtime symbol table
	 * @return the Value of the function.
	 * @throws JBasicException  if a parameter count or type error occurs
	 */
	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		String s;

		arglist.validate(1, 1, new int[] {Value.STRING});

		s = arglist.stringElement(0);
		if (s.length() < 1)
			return new Value(0);

		/*
		 * If the argument is a single character, return the value as a
		 * scalar integer.
		 */
		if( s.length() == 1 )
			return new Value(s.charAt(0));
		
		/*
		 * If the argument is a string, return an array with each character
		 * resolved as an array element value.
		 */
		Value result = new Value(Value.ARRAY, null);
		
		for( int ix = 0; ix < s.length(); ix++ )
			result.addElement(new Value(s.charAt(ix)));
		
		return result;
	}

}

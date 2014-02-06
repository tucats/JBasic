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
 * <b>CRYPTSTR()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Encode an encrypted array as a string</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = CRYPTSTR( <em>array</em> )</code></td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = CRYPTSTR( <em>string</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String or Array</td></tr>
 * </table>
 * <p>
 * The runtime code for CIPHER() and DECIPHER() functions produce arrays of
 * integers from encrypted strings, or produce strings by decrypting the
 * array of integers.  However, an array of integers is cumbersome to store
 * away as a single value.  So at runtime, the CRYPTSTR() function is called
 * automatically by the compiled code that used CIPHER() or DECIPHER().  This
 * function takes the array of integers and creates a string representation
 * of the array.  Or if given a string as an argument, produces the original
 * array of integer values.  This function is not normally called directly by
 * the user.
 * @author cole
 */

public class CryptstrFunction extends JBasicFunction {
	
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param argList the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException if an argument or execution error occurs
	 */

	public Value run(final ArgumentList argList, final SymbolTable symbols) throws JBasicException {

		argList.validate(1, 1, null);
		Value v = argList.element(0);

		if (v.getType() == Value.ARRAY) {
			return arrayToString(v);
		}

		String s = v.getString();
		return stringToArray(s);

	}

	/**
	 * Convert a string of encoded data into an array of integers in a Value.
	 * This is a utility function associated wiht the CIPHER function which
	 * converts an arbitrary string into an encrypted array of integers.  That
	 * array is not terribly useful for storing or passing around, so this
	 * worker function puts it into a human-readable and transmittable (but
	 * still encrypted) form.
	 * @param sourceString the encoded string to convert
	 * @return a Value containing an array of integers
	 */
	public static Value stringToArray(String sourceString) {
		Value v;
		String hex = "ABDWFGHJKMNPRSTX";
		StringBuffer s = new StringBuffer();
		for( int i = 0; i < sourceString.length(); i++) {
			char c = sourceString.charAt(i);
			if(!Character.isWhitespace(c))
				s.append(c);
		}
		
		int len;
		int ix;
		v = new Value(Value.ARRAY, null);
		len = s.length() / 2;
		for (ix = 0; ix < len; ix++) {
			char c1 = s.charAt(ix * 2);
			char c2 = s.charAt(ix * 2 + 1);

			int in, x;
			x = 0;
			for (in = 0; in < 16; in++)
				if (c1 == hex.charAt(in))
					break;
			x = in * 16;
			for (in = 0; in < 16; in++)
				if (c2 == hex.charAt(in))
					break;
			x = x + in;
			if (x > 127)
				x = x - 256;
			v.setElement(new Value(x), ix + 1);
		}
		return v;
	}

	/**
	 * Convert an array of integers into an encode string.<p>
	 * This is a utility function associated wiht the CIPHER function which
	 * converts  an encrypted array of integers to an arbitrary string.  The
	 * array is not terribly useful for storing or passing around, so this
	 * worker function puts it into a human-readable and transmittable (but
	 * still encrypted) form.
	 * @param v the array of integers convert
	 * @return a Value containing an encoded string
	 */
	
	public static Value arrayToString(Value v) {
		int len;
		String hex = "ABDWFGHJKMNPRSTX";
		int ix;
		len = v.size();
		char[] b = new char[len * 2];
		for (ix = 0; ix < len; ix++) {
			int i = v.getInteger(ix + 1);
			if (i < 0)
				i = 256 + i; /* Convert signed byte to unsigned byte */
			b[ix * 2 + 0] = hex.charAt(i / 16);
			b[ix * 2 + 1] = hex.charAt(i % 16);
		}
		return new Value(String.valueOf(b));
	}
}

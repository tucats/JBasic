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
 * <b>REPEAT()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Create multiple copies of a string</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = REPEAT( <em>string</em>, <em>count</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * The first argument must be a string value.  The second argument is the number
 * of times that the argument should be repeated in the resulting string.  For
 * example, <code>REPEAT("XO", 3)</code> results in the string "XOXOXO".
 * <p>
 * This functionality can be accomplished in an expression, as in ("XO"*3)
 * which resolves to the same string value.
 * @author cole
 *
 */

public class RepeatFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException the arguments are not correct
	 */
	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(2, 2, new int[] {Value.STRING, Value.NUMBER});

		String dest = "";
		final String source = arglist.stringElement(0);
		final long copies = arglist.intElement(1);

		if (copies < 1)
			return new Value("");

		for (int i = 0; i < copies; i++)
			dest = dest + source;
		return new Value(dest);
	}

}

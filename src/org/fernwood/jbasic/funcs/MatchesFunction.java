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

import java.util.regex.Pattern;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>MATCHES()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Regular Expression pattern match</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>b = MATCHES( <em>pattern</em>, <em>string</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Boolean</td></tr>
 * </table>
 * <p>
 * The pattern argument is treated as a RegEx regular expression, and is
 * used to determine if the string argument matches the pattern. A boolean
 * result is returned, true if the string matches the pattern, else false.
 * @author cole
 *
 */
public class MatchesFunction extends JBasicFunction {
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param argList the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException an error occurred in the number or type of
	 * function arguments, or an invalid regular expression pattern was
	 * given.
	 */

	public Value run(final ArgumentList argList, final SymbolTable symbols) throws JBasicException {
		argList.validate(2, 2, new int[] { Value.STRING, Value.STRING });

		boolean result = false;
		final String pattern = argList.stringElement(0);

		try {
			result = Pattern.matches(pattern, argList.stringElement(1));
		} catch (final Exception e) {
			throw new JBasicException(Status.INVPATTERN, pattern);
		}

		return new Value(result);

	}

}

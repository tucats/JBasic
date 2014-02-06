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
 * <b>FORMAT()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Format a value.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = FORMAT( <em>value</em>, <em>format-string</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 *
 * Intrinsic function to format an expression as a string. Given an
 * expression, will call the value formatter used by the PRINT function to
 * convert the expression to a string. For scalar arguments, the standard
 * formatter applies. For arrays, the array formatter is used which
 * yields a syntax that can be sent back to the expression() function to
 * reconstruct the value.
 * 
 * If the second argument is omitted, then the routine simply does a format
 * conversion to a "formatted string".

 * @author cole
 *
 */
public class FormatFunction extends JBasicFunction {
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  an argument count or type error occurred
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(1, 2, new int[] { Value.UNDEFINED, Value.STRING });

		String fmtString = null;
		if (arglist.size() == 2)
			fmtString = arglist.stringElement(1);
		else
			fmtString = "";

		final String fmtBuffer = Value.format(arglist.element(0), fmtString);
		if (fmtBuffer == null) 
			throw new JBasicException(Status.ARGTYPE, arglist.element(0).toString());

		return new Value(fmtBuffer);

	}
}

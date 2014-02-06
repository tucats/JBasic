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
 * <b>NAN()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Test for a "Not a Number" value</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>b = NAN( d )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Boolean</td></tr>
 * </table>
 * <p>
 * Determine if the argument is a NaN or "not-a-number" value. Returns true
 * if the argument is a NaN, else returns false.  This function was created
 * before the "=" equality test could correctly handle a test for a NaN by
 * comparing with a "." which is the constant value that means a NaN.  Use of
 * this function is deprecated.
 * @author cole
 */
public class NanFunction extends JBasicFunction {
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException an error occurred in the number or type of
	 * function arguments.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {
		arglist.validate(1, 1, new int[] {Value.NUMBER});

		boolean dv = false;
		try {
			dv = Double.isNaN(arglist.doubleElement(0));
		} catch (Exception e ) {
			throw new JBasicException(Status.ARGTYPE, e.toString());
		}
		return new Value(dv);

	}

}

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
 * <b>FLOOR()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Arithmetic floor.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>v1 = FLOOR( v2 )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Same type as parameter</td></tr>
 * </table>
 * <p>
 * Calculates the arithmetic floor, which is the integer value that is 
 * less than or equal to the argument.  The parameter must be 
 * an INTEGER or DOUBLE and the return type is
 * the same as the parameter type.
 * @author cole
 */
public class FloorFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException   an argument count or type error occurred
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {
		arglist.validate(1, 1, new int[] {Value.NUMBER});
		double dv = 0.0;
		try {
			dv = Math.floor(arglist.doubleElement(0));
		} catch (Exception e ) {
			throw new JBasicException(Status.ARGTYPE, e.toString());
		}
		return new Value(dv);	
		}
}

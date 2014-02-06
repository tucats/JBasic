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
 * <b>ABS()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Absolute value.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>v1 = ABS( v2 )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Same type as parameter</td></tr>
 * </table>
 * <p>
 * Calculates the arithmetic absolute value.  The parameter must be 
 * an INTEGER or DOUBLE and the return type is
 * the same as the parameter type.
 * @author cole
 */
public class AbsFunction extends JBasicFunction {
	
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException if a math error occurs
	 */
	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {
		arglist.validate(1, 1, new int[] {Value.NUMBER});
		final Value d = arglist.element(0);
		if (d.isType(Value.INTEGER))
			return new Value(Math.abs(d.getInteger()));

		double dv = 0.0;
		try {
			dv = Math.abs(arglist.doubleElement(0));
		} catch(Exception e ) {
			throw new JBasicException(Status.ARGTYPE);
		}
		return new Value(dv);
	}

}

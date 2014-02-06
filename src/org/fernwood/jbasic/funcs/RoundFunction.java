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
 * <b>ROUND()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Arithmetic rounding</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>d = ROUND( <em>value</em>, <em>digits</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Double</td></tr>
 * </table>
 * <p>
 * Round the floating point argument up or down according to standard math
 * rules ( round up if >= .5 threshold).  The second parameter indicates the
 * number of decimal places to which to round the value.
 * 
 * @author cole
 *
 */

public class RoundFunction extends JBasicFunction {

	/**
	 * Round the floating point argument up or down according to standard math
	 * rules ( round up if >= .5 threshhold).
	 * 
	 * @param arglist
	 *            The function argument list
	 * @param symbols
	 *            The runtime symbol table
	 * @return The value of the function
	 * @throws JBasicException the argument count or types are incorrect
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(2, 2, new int[] {Value.NUMBER, Value.NUMBER});

		double d = arglist.doubleElement(0);
		final int x = arglist.intElement(1);
		if ((x < 0) | (x > 10))
			throw new JBasicException(Status.ARGERR);

		double f = 1.0;
		for (int ix = 0; ix < x; ix++)
			f = f * 10.0;

		d = Math.floor(d * f) / f;

		return new Value(d);

	}

}

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
 * <b>TIMECODE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return a millisecond-precision time code</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>r = TIMECODE()</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Integer</td></tr>
 * </table>
 * <p>
 * Return an integer value that contains the millisecond (fractions of a second)
 * element of the current time.  This is intended to produce a somewhat non-deterministic
 * integer value that is suitable for use in seeding a random number function or generating
 * a component of a unique ID value.  For example, this is used to generate a semi-random
 * string of characters that is added to multi-user workspace file names, to reduce the 
 * chance of a file name collision.
 * @author cole
 *
 */
public class TimecodeFunction extends JBasicFunction {

	/**
	 * Generate an integer value that is based on the current time in milliseconds.
	 * This is a non-deterministic value that can be used as a seed for random
	 * number generators, etc.
	 * 
	 * @param argList the argument list for the call
	 * @param symbols the active symbol table.
	 * @return function value containing an integer
	 * @throws JBasicException  An error in the count or type of argument
	 * occured
	 */
	public Value run(ArgumentList argList, final SymbolTable symbols) throws JBasicException {
		argList.validate(0, 0, null);
		long millis = System.currentTimeMillis();
		return new Value(millis % 10000000);
	}
}

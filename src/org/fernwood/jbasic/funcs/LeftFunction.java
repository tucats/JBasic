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
import org.fernwood.jbasic.compiler.CompileContext;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;

/**
  <b>LEFT()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Substring of left-most characters.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s2 = LEFT( s1, count )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Extracts the left-most 'count' characters from 's1'.  If count is less than 
 * 1 then an empty string is returned.  If count is greater than the length of
 * s1 then the result is the entire string.
 * @author cole
 *
 */
public class LeftFunction extends JBasicFunction {
	/**
	 * Compile the function
	 * @param work the compiler context, including the current byteCode
	 * stream.
	 * @return a Status value indicating if the compilation was successful.
	 * @throws JBasicException an error occurred in the number or type of
	 * function arguments.
	 */

	public Status compile(final CompileContext work) throws JBasicException {
		work.validate(2, 2);
		work.byteCode.add(ByteCode._LEFT);
		return new Status();

	}

}

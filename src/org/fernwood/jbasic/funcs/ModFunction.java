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
 * <b>MOD()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Modulo (remainder)</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>v = MOD( v1, v2 )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Same type as V1</td></tr>
 * </table>
 * <p>
 * Calculate the numerical remainder of dividing V1 by V2; i.e. the result of V1 modulo v2.
 * If the V1 argument is an integer, the calculation is done using integer math and the
 * result is an integer value.  If V1 is a double precision floating point value, the result
 * is expressed as a double as well.
 * @author cole
 */
public class ModFunction extends JBasicFunction {

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
		work.byteCode.add(ByteCode._MOD);
		return new Status();
	}

}

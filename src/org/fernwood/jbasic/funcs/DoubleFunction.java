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
import org.fernwood.jbasic.value.Value;

/**
 * <b>DOUBLE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Convert to a DOUBLE value</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>v1 = DOUBLE( v2 )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Double</td></tr>
 * </table>
 * <p>
 * Convert the argument to a double-precision floating point value. If the
 * data type is numeric (integer, etc.) then a simple numeric conversion is
 * performed.  If the value is a string, then the string is parsed to try to
 * convert it to a double value.
 * @author cole
 */
public class DoubleFunction extends JBasicFunction {
	/**
	 * Compile the function
	 * @param work the compiler context, including the current byteCode
	 * stream.
	 * @return a Status value indicating if the compilation was successful.
	 * @throws JBasicException if an argument or execution error occurs
	 */
	public Status compile(final CompileContext work) throws JBasicException {
		work.validate(1,1);
		work.byteCode.add(ByteCode._CVT, Value.DOUBLE);
		return new Status();
	}

}

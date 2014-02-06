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
 * <b>LENGTH()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return length of value</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>i = LENGTH(<em>value</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Integer</td></tr>
 * </table>
 * <p>
 * 
 * Return the length of the function argument. For a string, this is the
 * length of the string.  For an array, it is the number of elements in the
 * array.  For a record, it is the number of fields in the record.  The 
 * function returns an error when called with a scalar numeric value.
 * @author cole
 *
 */
public class LengthFunction extends JBasicFunction {


	/**
	 * Compilation module for LENGTH function, which returns the length of the
	 * top item on the stack. This is implemented as a single ByteCode
	 * instruction _LENGTH, which handles it at runtime.
	 * 
	 * @param work the compilation descriptor
	 * @return Status indicating that the compilation succeeded without error.
	 * @throws JBasicException an error occurred in the number or type of
	 * function arguments.
	 */
	public Status compile(final CompileContext work) throws JBasicException {
		work.validate(1, 1);
		work.byteCode.add(ByteCode._LENGTH);
		return new Status();
	}

}

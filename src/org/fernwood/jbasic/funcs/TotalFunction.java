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
 * <b>TOTAL()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Calculate the sum of arguments.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>v = TOTAL( <em>p1</em> [, <em>p2</em>...] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Same type as <em>p1</em></td></tr>
 * </table>
 * <p>
 * Calculates the sum of all the arguments.  The type is based on the types
 * of the arguments.  If all arguments are strings, then the result is the
 * arguments concatenated together.  Otherwise it is the arithmetic sum
 * of the values.  The resulting type is whatever type is required to store
 * the result; that is, if all the arguments are integers, then an integer
 * value is returned.  If some are integer and some are double values, then
 * a double will be returned.
 * <p>
 * <em><b>Note:</b></em> This is identical in implementation to the <code>SUM()</code>
 * function. The <code>TOTAL()</code> function should be deprecated and may be
 * removed from a future version of JBasic.
 * @author cole
 */
public class TotalFunction extends JBasicFunction {

	/**
	 * Compile the intrinsic TOTAL function. Code has already been generated
	 * with the items on the stack <em>in reverse order.</em> Generate code to
	 * work through the list backwards, creating an accumulator on top of the
	 * stack. The order is critical because, for strings, the total must be the
	 * left-to-right concatenation of the string values.
	 * 
	 * @param work the compilation work unit.
	 * @return Status indicating that the compilation succeeded without error.
	 * @throws JBasicException  An error in the count or type of argument
	 * occurred.
	 */
	public Status compile(final CompileContext work) throws JBasicException {

		if (work.argumentCount < 1)
			throw new JBasicException(Status.INSFARGS);
		
		if (work.argumentCount == 1)
			return new Status();

		for (int i = 1; i < work.argumentCount; i++)
			work.byteCode.add(ByteCode._ADD);
		return new Status();
	}

}

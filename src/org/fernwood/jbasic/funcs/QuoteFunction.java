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
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;

/**
 * <b>QUOTE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Place quotes around string argument</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = QUOTE( <em>string</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * The argument is returned with quotation marks (") surrounding the string.
 * 
 * @author cole
 *
 */
public class QuoteFunction extends JBasicFunction {

	/**
	 * Compilation module for the QUOTE function, which uses ByteCode
	 * compilation to implement the function inline, at assembly time. Returns
	 * SUCCESS if the compilation was possible/successful. If an error occurs or
	 * the function is determined to be un-compilable, then return any error -
	 * the error is ignored, and a standard _CALLF invocation lets it be
	 * resolved at runtime instead of compile-time.
	 * 
	 * @param work the compile context
	 * @return Status indicating that the compilation succeeded without error.
	 * @throws JBasicException if an argument error occurs
	 */
	public Status compile(final CompileContext work) throws JBasicException {

		work.validate(1, 1);
		
		/*
		 * If the value on the stack is a constant, then we can cheat a little 
		 * bit if it's already a string.
		 */

		Instruction i = work.byteCode.getInstruction(work.byteCode.size()-1);
		if( work.constantArguments &&  i.opCode == ByteCode._STRING && i.stringValid ) {
			StringBuffer b = new StringBuffer();
			b.append('"');
			b.append(i.stringOperand);
			b.append('"');
			i.stringOperand = b.toString();
			return new Status();
		}
		/*
		 * Stack at runtime has string to be quoted on top of stack. So we must
		 * generate code to arrange the stack to be "\"", string, "\"" and the
		 * concatenate all three strings together, leaving the result on the
		 * stack.
		 */

		work.byteCode.add(ByteCode._CHAR, 34);
		work.byteCode.add(ByteCode._SWAP);
		work.byteCode.add(ByteCode._CHAR, 34);
		work.byteCode.add(ByteCode._ADD);
		work.byteCode.add(ByteCode._ADD);

		return new Status();
	}

}

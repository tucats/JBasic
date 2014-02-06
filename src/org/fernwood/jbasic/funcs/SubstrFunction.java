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
 * <b>SUBSTR()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Extract a substring</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = SUBSTRING( <em>string</em>, <em>p1</em> [, <em>p2</em>] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Extract a substring of a parameter, and return it as the argument.  If
 * there is only one position argument, then the characters of the string
 * from that position to the end of the string are returned (this is 
 * identical to the operation of the <code>RIGHT()</code> function).  If
 * both position arguments are given, the the characters from <em>p1</em> to
 * <em>p2</em> are returned.  The first character in the string is at
 * position 1.  So <code>SUBSTRING("THIS TEST", 3)</code> returns "IS TEST"
 * and <code>SUBSTRING("THIS TEST", 3, 4)</code> returns "IS".
 * 
 * @author cole
 *
 */
public class SubstrFunction extends JBasicFunction {

	/**
	 * Compile the intrinsic SUBSTR function. Code has already been generated
	 * with the items on the stack <em>in reverse order.</em>
	 * 
	 * @param work the compilation unit
	 * @return Status indicating that the compilation succeeded without error.
	 * @throws JBasicException  if an error in the count or type of arguments
	 * is found.
	 */
	public Status compile(final CompileContext work) throws JBasicException {

		work.validate(2, 3);
		work.byteCode.add(ByteCode._SUBSTR, work.argumentCount);
		return new Status();
	}
}

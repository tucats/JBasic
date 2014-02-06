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
 * <b>EOD()</b> JBasic Function
 * <p>
 * <table>
 * <tr>
 * <td><b>Description:</b></td>
 * <td>Test for end-of-DATA</td>
 * </tr>
 * <tr>
 * <td><b>Invocation:</b></td>
 * <td><code>b = EOD()</code></td>
 * </tr>
 * <tr>
 * <td><b>Returns:</b></td>
 * <td>Boolean</td>
 * </tr>
 * </table>
 * <p>
 * Test to see if the next READ statement will have a DATA item to read.  If
 * there is at least one more un-READ item, then the function returns false.  If
 * there are no more DATA elements to READ, then the function returns true.
 * 
 * @author cole
 */
public class EodFunction extends JBasicFunction  {

	/**
	 * Compile the function
	 * @param work the compiler context, including the current byteCode
	 * stream.
	 * @return a Status value indicating if the compilation was successful.
	 * @throws JBasicException if an argument or execution error occurs
	 */

	public Status compile(final CompileContext work) throws JBasicException {
		work.validate(0,0);
		work.byteCode.add(ByteCode._EOD);
		return new Status();
	}

}

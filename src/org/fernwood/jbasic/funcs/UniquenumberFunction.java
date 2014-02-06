
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
 * <b>UNIQUENUMBER()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Generate a unique integer.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = UNIQUENUMBER()</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Integer</td></tr>
 * </table>
 * <p>
 * 
 * Generate a unique integer number.  This function returns a number that is
 * unique for the process that it is run in, but may not be unique across
 * multiple processes or computer systems.  This should only be used for
 * sequencing information among threads in the same process.
 * @author cole
 *
 */
public class UniquenumberFunction extends JBasicFunction {
	
	/**
	 * Compile the function
	 * @param work the compiler context, including the current byteCode
	 * stream.
	 * @return a Status value indicating if the compilation was successful.
	 * @throws JBasicException  An error in the count or type of argument
	 * occurred.
	 */
	public Status compile(final CompileContext work) throws JBasicException {

		work.validate(0,0);
		work.byteCode.add(ByteCode._UID);
		return new Status();

	}


}

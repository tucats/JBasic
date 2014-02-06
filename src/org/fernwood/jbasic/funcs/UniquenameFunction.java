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
 * <b>UNIQUENAME()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Generate a unique identifier.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = UNIQUENAME()</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Generates a unique identifier name.  This takes the form "UID_" followed by
 * an integer value that is guaranteed to be unique during the life of the
 * JBasic session.  This is suitable for use when generating labels or variable
 * names that are needed during the execution of a program, but are not intended
 * to persist after the JBasic session terminates.
 * @author cole
 *
 */

public class UniquenameFunction extends JBasicFunction {
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
		
		if (work.argumentCount != 0)
			return new Status(Status.ARGERR);
		work.byteCode.add(ByteCode._STRING, "UID_");
		work.byteCode.add(ByteCode._UID);
		work.byteCode.add(ByteCode._CVT, Value.STRING);
		work.byteCode.add(ByteCode._ADD);
		return new Status();

	}

}

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
 * <b>RECORD()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Create a RECORD</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = RECORD( <em>string</em>, <em>value</em> [, <em>string</em>, <em>value</em>...])</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Record</td></tr>
 * </table>
 * <p>
 * Create a RECORD object and return it as the function result. There must be
 * an even number of arguments.  The first argument is used as a member name,
 * and the second argument is that member's value.  The third argument is another
 * member name, etc.  So <code> RECORD("AGE", 18, "NAME", "SUSAN")</code>
 * results in the record { AGE:18, NAME:"SUSAN"}.  This is provided for 
 * compatibility with previous releases.
 * 
 * @deprecated.  It has no functionality that cannot be expressed as a
 * RECORD literal.
 * 
 * @author cole
 *
 */

public class RecordFunction extends JBasicFunction {

	/**
	 * Compile the function
	 * @param work the compiler context, including the current byteCode
	 * stream.
	 * @return a Status value indicating if the compilation was successful.
	 * @throws JBasicException if an argument or compilation error occurs
	 */

	public Status compile(final CompileContext work ) throws JBasicException {
		/* 
		 * There must be an even number of arguments, indicating pairs of
		 * keys and values.
		 */
		if( work.argumentCount % 2 != 0 )
			throw new JBasicException(Status.ARGERR);
		
		work.byteCode.add(ByteCode._RECORD, work.argumentCount / 2);
		return new Status();
	}
	
}

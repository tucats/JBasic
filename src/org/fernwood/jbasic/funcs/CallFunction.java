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

/**
 * <b>CALL()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Call a function indirectly</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = CALL( <em>name-string</em>, ... )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Any Value</td></tr>
 * </table>
 * <p>
 * Call a function where the name is encoded as a string expression.  The first
 * argument is the actual string; the rest of the arguments are passed to the
 * function as-is.
 * 
 * @author cole
 * 
 */
public class CallFunction extends JBasicFunction {

	/**
	 * Compile the function.  This checks to see if the most common
	 * case of an integer constant has been given.  In that case, we
	 * can generate a _STRING instruction to load the single character.
	 * If the conditions aren't right for this optimization, we return
	 * an error so the compilation is not performed.
	 * 
	 * @param work the compiler context, including the current byteCode
	 * stream.
	 * @return a Status value indicating if the compilation was successful.
	 */

	public Status compile(final CompileContext work)  {
		
		if( work.argumentCount < 1 ) 
			return new Status(Status.ARGERR);
		
		work.byteCode.add(ByteCode._CALLF, work.argumentCount-1);
		
		return new Status();
		
	}

}

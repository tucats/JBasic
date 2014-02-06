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
 * <b>EOF()</b> JBasic Function
 * <p>
 * <table>
 * <tr>
 * <td><b>Description:</b></td>
 * <td>Test for end-of-file</td>
 * </tr>
 * <tr>
 * <td><b>Invocation:</b></td>
 * <td><code>b = EOF(<em>file-identifier</em>)</code></td>
 * </tr>
 * <tr>
 * <td><b>Returns:</b></td>
 * <td>Boolean</td>
 * </tr>
 * </table>
 * <p>
 * Indicate if a named file is positioned at the EOF marker. This calls the
 * JBasicFile <code>eof()</code> method, which attempts a lookahead to see 
 * if the file input
 * stream is exhausted yet or not. This function returns a boolean value of
 * <code>true</code> if the file is positioned at the end already, or
 * <code>false</code> if there is more data to read.
 * <p>
 * The file identifier can be a named identifier or an integer, which represents
 * the file number in the <em>#n</em> notation.
 * 
 * @author cole
 * 
 */
public class EofFunction extends JBasicFunction {
	
	/**
	 * Compile the function
	 * @param work the compiler context, including the current byteCode
	 * stream.
	 * @return a Status value indicating if the compilation was successful.
	 * @throws JBasicException if an argument or compilation error occurs
	 */

	public Status compile(final CompileContext work) throws JBasicException {
		work.validate(1, 1);
		work.byteCode.add(ByteCode._EOF);
		return new Status();
	}

}

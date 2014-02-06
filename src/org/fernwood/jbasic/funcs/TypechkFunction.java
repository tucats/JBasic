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
  <b>TYPECHK()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Calculate types of two values.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>i = TYPECHK( <em>value</em>,<em>string</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Boolean</td></tr>
 * </table>
 * <p>
 * Returns the results of comparing (deeply) two values for matching types, record
 * members, etc.  The string descriptor (second parameter) uses the words
 * <code>INTEGER</code>, <code>STRING</code>, <code>BOOLEAN</code>, and <code>DOUBLE
 * </code> to represent the appropriate data types in the declaration.  So a
 * declaraton string of "[STRING, STRING]" requires that the value be an array of
 * two strings.
 * @author cole
 *
 */
public class TypechkFunction extends JBasicFunction {

	
	public Status compile(final CompileContext work) throws JBasicException {

		work.validate(2,2);
		work.byteCode.add(ByteCode._PROTOTYPE);
		work.byteCode.add(ByteCode._TYPECHK);
		return new Status();
	}
}

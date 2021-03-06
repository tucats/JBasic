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

import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>LOADED()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Determine if Program is in memory</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>b = LOADED(<em>name-string</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Boolean</td></tr>
 * </table>
 * <p>
 * This function returns a boolean value indicating if the named program is
 * loaded into memory or not. If the program is in memory then it returns
 * <code>true</code>, else <code>false</code>.
 * 
 * @author cole
 * 
 */
public class LoadedFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException an error occurred in the number or type of
	 * function arguments.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(1, 1, new int [] {Value.STRING});

		return new Value(arglist.session.programs.find(arglist.stringElement(0)) != null);
	}

}

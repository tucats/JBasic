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
import org.fernwood.jbasic.Utility;

/**
 * <b>PATHNAME()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Path component of full path string</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = PATHNAME( f )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Given a full or partial path name, return the the fully qualified pathname element of the
 * string.  For example, "/tmp/jbasic/guest/data.txt" could return "/private/tmp/jbasic/guest".
 * 
 * @author cole
 * 
 */
public class PathnameFunction extends JBasicFunction {

	/**
	 * Execute the function
	 * 
	 * @param arglist
	 *            The function argument list
	 * @param symbols
	 *            The runtime symbol table
	 * @return the Value of the function.
	 * @throws JBasicException if an argument or execution error occurs
	 */
	
	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(1, 1, new int [] { Value.STRING });
		
		return new Value(Utility.pathName(arglist.stringElement(0)));

	}
}

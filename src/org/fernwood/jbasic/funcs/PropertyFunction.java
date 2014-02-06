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
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>PROPERTY()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return a Java property</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = PROPERTY( <em>name-string</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Returns string containing the Java runtime property value.  For example, 
 * <code>PROPERTY("file.separator")</code> would return the string "/" on a Unix system,
 * indicating that the forward-slash is the file separator on the current system.  You
 * can use the <code>PROPERTIES()</code> function to list all know properties.  Note that
 * most invocations of Java allow you to add additional properties to the runtime environment
 * for JBasic, so you can send your own custom property values into the JBasic runtime and
 * get those values using this function.
 * @author cole
 *
 */
public class PropertyFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException if an argument or execution error occurs
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {
		String type = null;

		arglist.validate(0, 1, new int[] { Value.STRING });

		if (arglist.size() == 0)
			type = "java.version";
		else
			type = arglist.stringElement(0);

		String prop = System.getProperty(type);
		if( prop == null ) {
			if( arglist.session.signalFunctionErrors())
				throw new JBasicException(Status.INVNAME, type);
			prop = "";
		}
		return new Value(prop);

	}

}

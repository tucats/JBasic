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

import java.util.Enumeration;
import java.util.Properties;

import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.statements.SortStatement;
import org.fernwood.jbasic.value.Value;

/**
 * <b>PROPERTIES()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return list of PROPERTY names</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = PROPERTIES( )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of strings</td></tr>
 * </table>
 * <p>
 * Returns an array of strings.  Each string is the name of a currently
 * available PROPERTY() value.  For example, "file.separator" would be one
 * of the return values, which can be passed to the PROPERTY function to get
 * this value.
 * @author cole
 *
 */
public class PropertiesFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException if an argument or execution error occurs
	 */

	public Value run(final ArgumentList arglist,
			final SymbolTable symbols) throws JBasicException {

		arglist.validate(0, 0, null);


		final Properties props = System.getProperties();
		final Enumeration e = props.keys();

		/*
		 * Get next element, make a data element and stuff it in the
		 * arrayValue
		 */

		final Value result = new Value(Value.ARRAY, null);
		while (e.hasMoreElements())
			result.addElement(new Value((String) e.nextElement()));

		/*
		 * Use the SORT statement's sort function to alphabetize the list.
		 */
		SortStatement.sortArray(result);
		
		return result;

	}

}

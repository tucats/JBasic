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
 * <b>LOCATE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Locate a value in an array</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>i = LOCATE( v, array)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Integer</td></tr>
 * </table>
 * <p>
 * Find location of first argument in the array identified by the second
 * argument. If second argument is not an array, then it must be a
 * string and we will search the string for the target value. If argument is
 * not found, return 0.  Otherwise return the array subscript or substring
 * position where the value was found.
 *
 * @author cole
 *
 */
public class LocateFunction extends JBasicFunction {

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

		arglist.validate(2, 2, null);

		final Value item = arglist.element(0);
		final Value list = arglist.element(1);

		/*
		 * See if we're doing a string search. If so, use the builtin function
		 * for this, and return the offset. We add one, because the Java
		 * standard is -1 for no match, but JBasic assumes 0 means no match.
		 */
		if (list.getType() == Value.STRING) {
			return new Value(list.getString().indexOf(item.getString()) + 1);
		}

		/*
		 * We must be doing an array search or there's an error.
		 */
		if (list.getType() != Value.ARRAY)
			throw new JBasicException(Status.ARGTYPE);

		final int listSize = list.size();
		Value e;

		/*
		 * Scan over the list, extracting each arrayValue element by
		 * (coerced-to-string) key value. Use the Value match() method to
		 * determine if the items match in both type and value. If they do, then
		 * return the arrayValue position.
		 */
		for (int i = 1; i <= listSize; i++) {
			e = list.getElement(i);
			if (e == null)
				continue;
			if (e.match(item))
				return new Value(i);
		}

		/*
		 * Nothing ever matched, so we return zero.
		 */
		return new Value(0);
	}
}

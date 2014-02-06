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
 * <b>TABLES()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>List of active symbol tables.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = TABLES( [<em>starting-table-name</em>])</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of strings</td></tr>
 * </table>
 * <p>
 * Returns an array value with the names of each of the symbol tables starting
 * with the most local table. If given, a string parameter can be used to
 * specify the starting table to begin returning the list from; i.e. "LOCAL" for
 * the local table, "PARENT" for it's parent, "GLOBAL" for the global table.
 * 
 * @author cole
 * 
 */
public class TablesFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException if an error in the count or type of arguments
	 * is found, or the table name is not found.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		Value result = null;
		arglist.validate(0, 1, new int[] { Value.STRING });

		result = new Value(Value.ARRAY, null);
		SymbolTable start = symbols;

		/*
		 * Search to see if we can find an exact (case-insensitive) match to the
		 * starting table name, if one was given.
		 */
		if (arglist.size() == 1) {
			String tableName = arglist.stringElement(0);
			start = symbols.findTable(tableName);
			if (start == null) {
				if( arglist.session.signalFunctionErrors())
					throw new JBasicException(Status.NOTABLE, tableName);
				return result;
			}
		}

		/*
		 * Now we build a list of symbol tables, starting with the named table.
		 */

		for (; start != null; start = start.parentTable)
			result.addElement(new Value(start.name));
		return result;
	}

}

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

import java.util.Iterator;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;
/**
 * <b>SYMBOLS()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Information about all symbols in a table.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>r = SYMBOLS( [<em>table-name</em>] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of records</td></tr>
 * </table>
 * <p>
 * Returns an array value with the names of each of the symbol names in a given
 * table. The LOCAL table is assumed if no name is given. if given, a string
 * parameter can be used to specify the table to returning the list of symbols
 * from; i.e. "LOCAL" for the local table, "PARENT" for it's parent, "GLOBAL"
 * for the global table.
 * <p>
 * Items returned in each record include:
 * <p>
 * <table>
 * <tr><td><b>Item</b></td><td><b>Description</b></td></tr>
 * <tr><td>NAME</td><td>The name of the symbol</td></tr>
 * <tr><td>READONLY</td><td>Is the symbol read-only?</td></tr>
 * <tr><td>TABLE</td><td>The table the symbol was found in</td></tr>
 * <tr><td>VALUE</td><td>XML representation of the symbol value</td></tr>
 * <tr><td>TYPE</td><td>Integer indicating the value type</td></tr>
 * <tr><td>TYPENAME</td><td>String name of the value type</td></tr>
 * <tr><td>LENGTH</td><td>Length if the value is a string or array</td></tr>
 * </table>
 * 
 * @author cole
 * 
 */
public class SymbolsFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  if an error in the count or type of arguments
	 * is found, or the symbol table or symbol name do not exist.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		Value result = null;
		arglist.validate(0, 1, new int[] { Value.STRING });

		SymbolTable start = symbols;
		result = new Value(Value.ARRAY, null);
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
		 * Now we build a list of symbol names from the given table.
		 */


		for (final Iterator i = start.table.keySet().iterator(); i.hasNext();) {
			String name = (String) i.next();
			result.addElement(new Value(name));
		}
		return result;
	}
}

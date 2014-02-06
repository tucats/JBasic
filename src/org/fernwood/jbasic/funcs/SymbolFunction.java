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
import org.fernwood.jbasic.runtime.XMLManager;
import org.fernwood.jbasic.value.Value;

/**
 * <b>SYMBOL()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Information about a symbol.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>r = SYMBOL( <em>symbol-name</em> [, <em>table-name</em>] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Record</td></tr>
 * </table>
 * <p>
 * Create a RECORD object that contains information known about the 
 * symbol identified by name as the string parameter.  By default, the
 * symbol is located using the same rules for finding a symbolic value;
 * the most local symbol table is searche first, then the parent of the
 * local table, and so on up to the global symbol table.  You can optionally
 * specify the name of the symbol table to start searching in as the
 * second parameter
 * <p>
 * Items returned in the record include:
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
 * @author cole
 *
 */
public class SymbolFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  if an error in the count or type of arguments
	 * is found, or the symbol name or table do not exist.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		Value result = null;
		arglist.validate(1, 2, new int[] { Value.STRING, Value.STRING });

		SymbolTable start = symbols;

		/*
		 * Search to see if we can find an exact (case-insensitive) match to the
		 * starting table name, if one was given.
		 */
		if (arglist.size() == 2) {
			String tableName = arglist.stringElement(1).toUpperCase();
			start = symbols.findTable(tableName);
			if (start == null) {
				if( arglist.session.signalFunctionErrors())
					throw new JBasicException(Status.NOTABLE, tableName);
				return new Value(Value.RECORD, null);
			}
		}

		/*
		 * Given the starting table, find a table that contains the given symbol
		 * name, and make that the new start. This lets us unambiguously
		 * resolve what table the symbol exists in, so we can set that
		 * information in the resulting record data about the symbol.
		 */

		final String variableName = arglist.stringElement(0).toUpperCase();
		start = start.findTableContaining(variableName);
		if (start == null) {
			if( arglist.session.signalFunctionErrors())
				throw new JBasicException(Status.UNKVAR, variableName);
			return new Value(Value.RECORD, null);
		}
		
		/*
		 * Get the specific item referenced, if it exists.
		 */

		Value v;
		v = start.reference(variableName);

		/*
		 * Now we build a record describing the symbol.
		 */

		result = new Value(Value.RECORD, null);

		result.setElement(new Value(variableName), "NAME");
		result.setElement(new Value(v.fReadonly), "READONLY");
		result.setElement(new Value(start.name), "TABLE");

		XMLManager xml = new XMLManager(arglist.session);
		result.setElement(new Value(xml.toXML(null, v, 0)), "VALUE");

		final int theType = v.getType();
		result.setElement(new Value(theType), "TYPE");
		result.setElement(new Value(Value.typeToName(theType)), "TYPENAME");

		if (theType == Value.ARRAY)
			result.setElement(new Value(v.size()), "LENGTH");

		if (theType == Value.RECORD)
			result.setElement(new Value(v.size()), "LENGTH");

		return result;
	}

}

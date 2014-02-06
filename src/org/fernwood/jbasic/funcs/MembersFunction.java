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

import java.util.ArrayList;

import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>MEMBERS()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return list of member names of a record</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = MEMBERS( <em>record</em> [, <em>flag</em>])</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of strings</td></tr>
 * </table>
 * <p>
 * The argument must be a record data type.  The result is an array of strings, where
 * each element is the name of a member in the given record.  
 * <p>
 * By default, member names
 * that start with an underscore ("_") character are considered invisible.  If the optional
 * second argument is given and is <code>true</code> then the list of members returned includes
 * the invisible members as well as the conventional member names.
 * @author cole
 */
public class MembersFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist
	 *            the function argument list and count already popped from the
	 *            runtime data stack
	 * @param symbols
	 *            the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  an error occurred in the number or type of
	 * function arguments
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		final Value e = new Value(Value.ARRAY, null);
		int count = 0;

		arglist.validate(1, 2, new int[] { Value.RECORD, Value.BOOLEAN});

		boolean showAll = false;
		if( arglist.size() > 1 )
			showAll = arglist.booleanElement(1);
		
		final Value record = arglist.element(0);
		ArrayList v = record.recordFieldNames();
		
		for (int ix = 0; ix < v.size(); ix++) {
			final String recordMember = (String) v.get(ix);
			if( !showAll & recordMember.startsWith("_"))
				continue;
			count++;
			e.setElement(new Value(recordMember), count);
		}

		return e;

	}

}

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
import org.fernwood.jbasic.value.RecordStreamValue;
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
public class JoinFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist
	 *            the function argument list and count already popped from the
	 *            runtime data stack
	 * @param symbols
	 *            the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException an error occurred in the number or type of
	 * function arguments.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(3, 3, new int[] { Value.TABLE, Value.TABLE, Value.STRING});

		RecordStreamValue left = (RecordStreamValue) arglist.element(0);
		RecordStreamValue right = (RecordStreamValue) arglist.element(1);
		String key = arglist.stringElement(2).toUpperCase();
		
		/*
		 * First, a sanity check
		 */
		
		left.streamValidate( key );
		right.streamValidate( key );

		/*
		 * Make sure the data is sorted by the key value so the merge works.
		 */
		left.sort( key);
		right.sort( key);
		
		/*
		 * Scan using the value in the left (base) array, searching for the next
		 * match in the right (match) array.  When found, the two records are
		 * merged into the result set.  Keys that have no match are ignored.
		 */
		int count = left.size();
		int position = 0;
		
		Value columnList = left.columnNames();
		columnList.addElement(right.columnNames());
		final Value r = new RecordStreamValue(columnList);

		for( int leftIndex = 1; leftIndex <= count; leftIndex++ ) {
			Value match = left.getElement(leftIndex);
			int rightIndex = right.streamFind( position, key, match.getElement(key));
			if( rightIndex > 0 ) {
				position = rightIndex;
				r.addElement(RecordStreamValue.streamMerge( left, leftIndex, right, rightIndex));
			}
		}
		return r;

	}


	

}

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
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * <b>SELECT()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return a subset of a members of a record </td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = SELECT( <em>record</em>, <em>name</em>[,<em>name</em>])</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Record</td></tr>
 * </table>
 * <p>
 * The first argument must be a record data type. This is followed by one or more
 * string arguments that must all be names.  The result is a record containing just
 * the selected members of the input record.
 * <p>
 
 * @author cole
 */
public class SelectFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist
	 *            the function argument list and count already popped from the
	 *            runtime data stack
	 * @param symbols
	 *            the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  There was an error in the type or count of
	 * function arguments.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		int count = arglist.size();
		
		if( count < 2 )
			throw new JBasicException(Status.INSFARGS);
		
		final Value sourceR = arglist.element(0);
		if( sourceR.getType() != Value.TABLE)
			throw new JBasicException(Status.ARGTYPE, 1);
	
		RecordStreamValue source = (RecordStreamValue) sourceR;
		
	
		if( arglist.element(1).getString().equals("*")) {
			return source.copy();
		}
		
		int newCount = arglist.size()-1;
		String[] columns = new String[newCount];
		int[] columnPos = new int[newCount];
		
		for( int i = 1; i<arglist.size(); i++ )
			columns[i-1] = arglist.stringElement(i).toUpperCase();
		
		Value sourceColumnList = source.columnNames();
		Value newNames = new Value(Value.ARRAY, null);
		
		for( int i = 0; i < newCount; i++ ) {
			boolean found = false;
			for( int j = 1; j <= sourceColumnList.size(); j++ ) {
				String sourceName = sourceColumnList.getString(j);
				String namePart = sourceName.substring(0, sourceName.indexOf('@'));
				if( namePart.equals(columns[i])) {
					found = true;
					newNames.addElement(new Value(sourceName));
					columnPos[i] = j;
				}
			}
			if(!found) {
				throw new JBasicException(Status.NOSUCHMEMBER, columns[i]);
			}
		}
		
		final RecordStreamValue r = new RecordStreamValue(newNames);

		for( int idx = 1; idx <= source.size(); idx++ ) {
			Value row = source.getElementAsArray(idx);
			Value newRow = new Value(Value.ARRAY, null);
			for( int c = 1; c <= newCount; c++ ) {
				newRow.addElement(row.getElement(columnPos[c-1]));
			}
			r.addElement(newRow);
		}

		return r;

	}

	

}

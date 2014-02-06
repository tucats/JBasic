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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.statements.SortStatement;
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;


/**
  <b>SORT()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Sort an array.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = SORT( v1 [, v2...] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array</td></tr>
 * </table>
 * <p>
 * All the arguments are placed in a single array.  If the arguments are
 * themselves arrays, each element of the argument array is copied into the
 * new array.  The array is then sorted, and returned as the function
 * result.  For example, <code>SORT( 3, 1, 2, 4)</code> returns the array
 * <code>[1, 2, 3, 4]</code> and <code>SORT( "TOM", ["SUE", "ANN"])</code>
 * returns the array <code>["ANN", "SUE", "TOM"]</code>.
 * @author cole
 *
 */

public class SortFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  There was an error in the type or count of
	 * function arguments.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		Status status = null;
		
		/**
		 * There's a special case where the first element is an array of records and the
		 * second argument is a string - which is the key to sort by.
		 */
		if( arglist.size() >= 2 && arglist.element(0).getType() == Value.TABLE ) {
			
			/*
			 * All additional arguments must be strings as they are column keys.
			 */
			
			ArrayList<String> keyList = new ArrayList<String>();
			for( int ix = 1; ix < arglist.size(); ix++ ) {
				if( arglist.type(ix) != Value.STRING)
					throw new JBasicException(Status.ARGTYPE);
				keyList.add(arglist.stringElement(ix).toUpperCase());
			}
					
			RecordStreamValue array = (RecordStreamValue) arglist.element(0);

			status = SortStatement.sortArray(array, keyList);

			if( status.failed())
				throw new JBasicException(status);
			return array;
		}
		
		/**
		 * There's a special case where the first element is an array of records and the
		 * second argument is a string - which is the key to sort by.
		 */
		if( arglist.size() == 2 &&
				arglist.element(0).getType() == Value.ARRAY &&
				arglist.element(1).isType(Value.STRING)) {
			Value array = arglist.element(0);
			String key = arglist.stringElement(1).toUpperCase();
			Value result = array.copy();
			SortStatement.sortArray(result, key);
			return result;
		}
		
		/*
		 * Start by creating a new empty array.  This will be the result
		 * of the function if all goes well.
		 */
		Value array = new Value(Value.ARRAY, null);
		
		/*
		 * Scan over the arguments and put them in the new array.  If the
		 * element in the list is in fact already an array, copy the members
		 * of the old array to the new array.  This lets the user specify
		 * one or more arrays or scalar elements in the argument list.
		 */
		
		for( int ix = 0; ix < arglist.size(); ix++ ) {
			Value element = arglist.element(ix);
			
			if( element.getType() == Value.ARRAY) {
				for( int k = 1; k <= element.size(); k++ ) 
					array.addElement(element.getElement(k));
			}
			else
				array.addElement(element);
			
		}

		/*
		 * Call the SORT statement's sort tool
		 */
		status = SortStatement.sortArray(array);
		
		/*
		 * If all went well, return the array as the function result.  Otherwise,
		 * print out whatever error information we have and indicate a runtime
		 * error by returning a null value.
		 */
		if (status.success())
			return array;
		
		throw new JBasicException(status);
	}

}

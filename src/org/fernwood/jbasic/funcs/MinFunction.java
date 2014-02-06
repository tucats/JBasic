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
 * <b>MIN()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Calculate numerical minimum of argument list</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>v = MIN( v1, v2 [, v3 [,v4...]])</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Boolean</td></tr>
 * </table>
 * <p>
 * Calculate the numerical minimum value of the argument list.  There must be at least
 * two arguments.  They can be of any type, but are converted to double precision floating
 * point for comparison purposes.
 * <p>
 * The parameters cannot be records. Parameters that are arrays are treated as a list of
 * arguments, and the result will be the smallest  value in any individual or array
 * argument.
 * @author cole
 */
public class MinFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  an error occurred in the number or type of
	 * function arguments.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		int j;

		/*
		 * There must be at least one argument
		 */
		if( arglist.size() < 1 ) 
			throw new JBasicException(Status.INSFARGS);


		/*
		 * Create a local array that contains all the arguments, flattened.
		 */
		final Value list = new Value(Value.ARRAY, null);
		for( j = 0; j < arglist.size(); j++) {
			
			Value v = arglist.element(j);
	
			if( v.getType() == Value.RECORD ) {
				throw new JBasicException(Status.ARGTYPE);
			}
			list.addElement(v);			
		}
		

		/*
		 * Seeding with the first value in the array, search the array
		 * to see if any other elements are bigger than the first one.
		 */
		Value result = new Value( list.getDouble(1));

		for (j = 2; j <= list.size(); j++) {
			final double d = list.getDouble(j);

			if (d < result.getDouble())
				result.setDouble(d);
		}
		
		/*
		 * Return the largest value seen.
		 */
		return result;

	}

}

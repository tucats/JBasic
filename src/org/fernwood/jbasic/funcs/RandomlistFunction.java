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
import org.fernwood.jbasic.value.Value;

/**
 * <b>RANDOMLIST()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Generate a random list of integers</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = RANDOMLIST( <em>v1</em> [, <em>v2</em>] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of integers</td></tr>
 * </table>
 * <p>
 *
 * Generate a randomly ordered list of integers.  If one parameter is given,
 * the list is contains all integer values from 1 to that number.  If two 
 * parameters are given, the list
 * includes all integers between the two values.  The list is always a list
 * of integers regardless of the type of the parameters.
 * @author cole
 *
 */
public class RandomlistFunction extends JBasicFunction {

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
		arglist.validate(1, 2, new int[] {Value.NUMBER, Value.NUMBER});

		int size, min, max;

		if (arglist.size() == 1) {
			min = 1;
			max = arglist.intElement(0);
		} else {
			min = arglist.intElement(0);
			max = arglist.intElement(1);
		}

		size = max - min + 1;

		if ((size < 1) || (size > 65536))
			return null;

		final int values[] = new int[size];
		final double sortkey[] = new double[size];
		int ix;
		for (ix = 0; ix < size; ix++) {
			sortkey[ix] = Math.random();
			values[ix] = ix + min;
		}

		for (int i = 0; i < size; i++)
			for (int j = i; j < size; j++)
				if (sortkey[i] > sortkey[j]) {
					final double tdouble = sortkey[i];
					sortkey[i] = sortkey[j];
					sortkey[j] = tdouble;

					final int tint = values[j];
					values[j] = values[i];
					values[i] = tint;
				}

		final Value array = new Value(Value.ARRAY, null);
		for (ix = 0; ix < size; ix++)
			array.setElement(new Value(values[ix]), ix + 1);
		return array;

	}

}

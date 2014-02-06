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
import org.fernwood.jbasic.informats.InputProcessor;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>INPUT()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Process a string using InFormats</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>b = INPUT(<em>format-array</em>, <em>input-string</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of values</td></tr>
 * </table>
 * <p>
 * 
 * Use the JBasic input processor library to process an input string, using an array of
 * format specifications.  The array of specification can be created using a FORMAT 
 * statement, or can be explicitly constructed as an array of strings, each of which specifies
 * a formation operation.
 * <p>
 * The input string is scanned using the format specification in the array, and each input
 * format
 * operation that yields a data result places the value in the resulting array.  The length
 * of the array will never be larger than the number of input formats specified, but may be
 * less if the input buffer does not contain enough data to satisfy the input specification.
 * 
 * @author cole
 */

public class InputFunction extends JBasicFunction {
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  an argument count or type error occurred,
	 * or the input function was not syntactically valid.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		/*
		 * The first argument MUST be an array, and the second MUST be a
		 * string.  The array will contain the input format specification
		 * and the string will contain the input data to be processed.
		 */
		arglist.validate(2, 2, new int[] { Value.ARRAY, Value.STRING});
		
		/*
		 * We will store the values processed via the INPUT format in
		 * an array that is the function's result.  The actual work is
		 * done by an instance of the InputProcessor class.
		 */
		Value result = new Value(Value.ARRAY, null);
		InputProcessor input = new InputProcessor();
		Status status = null;
		
		/*
		 * Use the first argument as an array of format specifications.  Scan
		 * over each element and add it to the INPUT processor.  If the value
		 * is a record then it is pre-compiled (such as the result of an INFORMAT
		 * statement compilation) else it is treated as a string and is compiled
		 * on-the-fly.
		 */
		Value formatElements = arglist.element(0);
		for( int ix = 1; ix <= formatElements.size(); ix++ ) {
			Value formatElement = formatElements.getElement(ix);
			if( formatElement.getType() == Value.RECORD)
				status = input.addInputFormat(formatElement);
			else
				status = input.addInputFormat(formatElement.getString());
			if( status.failed()) {
				throw new JBasicException(status);
			}
		}
		
		/*
		 * Use the string value of the second argument list element (ordinal
		 * position 1 in the argument list) as the
		 * buffer for reading data via the INPUT specification.
		 */
		input.setBuffer(arglist.element(1).getString());
		
		/*
		 * As long as there is data to be read, process the next input
		 * value using the format specification.  If the result is null
		 * then there is no more valid input data and we return waht we have.
		 * If the input operation was successful, add the value to the
		 * array of results we will eventually return.
		 */
		while( input.status.success()) {
			Value item = input.getNextValue();
			if( item == null )
				return result;
			if( input.status.success())
				result.addElement(item);
		}
		return result;

	}
}

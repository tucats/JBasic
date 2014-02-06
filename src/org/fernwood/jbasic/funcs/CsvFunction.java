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
 * <b>CSV()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Convert a list to a comma-separated-value string</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = CSV( p1 [,p2[,p3...]] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Given a list of one or more values, produce a string suitable for storage
 * in a file as a CSV or comma-separated-value string.  All values that do not
 * already contain a comma are simply copied to the output string.  Each
 * value is separated by a comma.  If the value contains a comma itself, the
 * value is written out in quotation marks.
 * @author cole
 */

public class CsvFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException if an argument or execution error occurs
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		StringBuffer result = new StringBuffer();
		int count = 0;
		String delimiter = arglist.session.getString("SYS$CSVDELIMITER");
		if( delimiter == null )
			delimiter = ",";

		for( int ix = 0; ix < arglist.size(); ix++ ) {

			Value arg = arglist.element(ix);
			if( count++ > 0)
				result.append(delimiter);
		result.append(format_csv(arg, delimiter));
				
		}
		return new Value(result.toString());
	}
	
	/**
	 * Recursively format a comma-separated value list of a given
	 * value and a specified delimiter
	 * @param arg The value to format.  If this is a scalar item,
	 * then it is formatted and returned.  If it is an array or a
	 * record, then each element is recursively formatted and including
	 * in the result with the proper delimiter.
	 * @param delimiter The delimiter character to put between compound
	 * elements of the result string.
	 * @return
	 */
	private String format_csv(Value arg, String delimiter) {
		int argType = arg.getType();
		int count = 0;
		StringBuffer result = new StringBuffer();
		
		if( argType == Value.ARRAY )
			for( int n = 1; n <= arg.size(); n++) {
				Value element = arg.getElement(n);
				if( count++ > 0)
					result.append(delimiter);
				result.append(format_csv(element, delimiter));
			}
		else 
			if( argType == Value.RECORD ) {
				ArrayList<String> keys = arg.recordFieldNames();
				for( int n = 0; n < keys.size(); n++ ) {
					Value element = arg.getElement(keys.get(n));
					if( count++ > 0)
						result.append(delimiter);
					result.append(format_csv(element, delimiter));
				}
			}

			else {
				if( count++ > 0)
					result.append(delimiter);
				String formattedValue = arg.getString();
				if( formattedValue.contains(delimiter)) {
					result.append('"');
					result.append(formattedValue);
					result.append('"');
				}
				else
					result.append(formattedValue);
			}
		return result.toString();
	}
}

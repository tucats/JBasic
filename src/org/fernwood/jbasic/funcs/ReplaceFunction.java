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
 * <b>REPLACE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Replace a value in a string</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = REPLACE( <em>string</em>, <em>search</em>, <em>new-string</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * The first argument is searched for any occurrence of the second argument.  If
 * it is found, the substring is replaced with the new string value.  For
 * example, <code>REPLACE("This is a test", "test", "text")</code> will result
 * in the string value "This is a text".
 * @author cole
 *
 */

public class ReplaceFunction extends JBasicFunction {

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

		arglist.validate(3, 4, new int[] { Value.STRING, Value.STRING,
				Value.STRING, Value.NUMBER});

		String source = arglist.stringElement(0);
		final String find = arglist.stringElement(1);
		final String replace = arglist.stringElement(2);
		int minimum = 0;
		int count = 0;

		if( arglist.size() > 3)
			count = arglist.intElement(3);
		else
			count = 1;

		if( count < 1 )
			throw new JBasicException(Status.ARGERR);

		/*
		 * First, see if we can find the "find" string in our master string. If
		 * the string isn't found, then just return the string un-edited.
		 */

		while( count > 0 ) {
			int n = source.substring(minimum).indexOf(find);
			if (n < 0)
				return new Value(source);
			n = n + minimum;
			
			/*
			 * Otherwise, re-assemble the string using the replace value.
			 */

			StringBuffer result = new StringBuffer();

			result.append(source.substring(0, n));
			result.append(replace);
			result.append(source.substring(n + find.length()));
			source = result.toString();
			minimum = n + replace.length()-1;
			count--;
		}

		return new Value(source);
	}

}

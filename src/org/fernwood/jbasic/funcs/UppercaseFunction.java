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
 * <b>UPPERCASE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Convert string to upper case</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = UPPERCASE(<em>string</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Convert the argument to an uppercase string
 * @author cole
 *
 */
public class UppercaseFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  An error in the count or type of argument
	 * occurred.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) 
													throws JBasicException {
		
		arglist.validate(1, 1, null);		
		
		Value v = arglist.element(0);
		int vType = v.getType();
		if( vType == Value.ARRAY ) {
			Value resultArray = new Value(Value.ARRAY, null);
			for( int ix = 1; ix <= v.size(); ix++ ) {
				Value element = v.getElement(ix);
				if( element.getType() == Value.STRING )
					resultArray.setElement(new Value(element.getString().toUpperCase()), ix);
				else
					resultArray.setElement(element.copy(), ix);
			}
			return resultArray;
		}
		
		if( vType == Value.STRING )
			return new Value(v.getString().toUpperCase());
		
		throw new JBasicException(Status.ARGTYPE);
	}

}

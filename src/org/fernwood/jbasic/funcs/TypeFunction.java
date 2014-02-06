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
 * <b>TYPE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Determine data type of expression</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = TYPE( <em>expression</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 *
 * Returns the type of the argument passed in. Used to identify string versus
 * numeric variables, etc. The argument can be a complex expression, or just
 * the name of a single variable.  This is especially helpful in handling 
 * function argument lists where the type of the argument is not known until
 * runtime.
 * 
 * @author cole
 * 
 */
public class TypeFunction extends JBasicFunction {

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

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(1, 1, null);
		
		StringBuffer typeName = new StringBuffer(Value.typeToName(arglist.type(0)));
		
		if( arglist.type(0) == Value.DECIMAL) {
			int scale = arglist.element(0).getDecimal().scale();
			if( scale != 0 ) {
				typeName.append('(');
				typeName.append(Integer.toString(scale));
				typeName.append(')');
			}
		}
		return new Value(typeName.toString());
	}

}

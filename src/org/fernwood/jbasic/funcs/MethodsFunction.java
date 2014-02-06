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

import java.lang.reflect.Method;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>METHODS()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return list of method names of a Java object</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = METHODS( <em>object</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of strings</td></tr>
 * </table>
 * <p>
 * The argument must be a record data type representing a wrapper on a Java object.
 *  The result is an array of strings, where
 * each element is the name of a member in the given record.  
 * <p>
 
 * @author cole
 */
public class MethodsFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist
	 *            the function argument list and count already popped from the
	 *            runtime data stack
	 * @param symbols
	 *            the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  an error occurred in the number or type of
	 * function arguments.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		final Value e = new Value(Value.ARRAY, null);

		arglist.validate(1, 1, new int[] { Value.RECORD});

		Value obj = arglist.element(0);
		if( !obj.isObject()) {
			if( arglist.session.signalFunctionErrors())
				throw new JBasicException(Status.INVOBJECT);
			return e;
		}
		
		Method [] methodList = obj.getObjectClass().getMethods();
		for( Method method : methodList ) 
			e.addElement(new Value(method.toString()));

		return e;

	}

}

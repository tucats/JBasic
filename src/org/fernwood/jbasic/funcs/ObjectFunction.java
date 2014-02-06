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
 * <b>OBJECT()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return object information</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>r = OBJECT( <em>object</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Record</td></tr>
 * </table>
 * <p>
 * Function that returns the object description record for the passed object.
 * If the optional second parameter is given, it is a string containing the
 * name a field of the record to be returned, as in OBJECT(foo, "CLASS") to 
 * get the name of the object's class.
 * 
 * @author cole
 * 
 */
public class ObjectFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException an error occurred in the number or type of
	 * function arguments.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {


		arglist.validate(1, 2, new int[] { Value.RECORD, Value.STRING});

		Value obj = arglist.element(0);
		Value objData = obj.getElement(Value.OBJECT_DATA);
		if( objData == null ) 
			throw new JBasicException(Status.INVOBJECT, "object");
				
		if( arglist.size() == 1 )
			return objData.copy();
		
		String keyName = arglist.stringElement(1).toUpperCase();
		Value fieldData = objData.getElement(keyName);
		if( fieldData == null )
			throw new JBasicException(Status.NOMEMBER, keyName);
		
		return fieldData.copy();

	}

}

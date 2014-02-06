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
 * <b>MEMBER()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Get a record member by name</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>v = MEMBER( <em>record</em>, <em>member-name-string</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Value</td></tr>
 * </table>
 * <p>
 * The first argument must be a  RECORD data type. The second argument is a string constant
 * or expression that resolves to the name of a member of the record.  The function result is
 * the value at that member location.  This allows programmatic extraction of a record member
 * whose name is calculated as opposed to constant.  Note that this same effect can be
 * accomplished by using array notation for the record.  
 * <p>
 * That is,
 * <code> x = MEMBER( myrecord, "AGE" )</code>
 * has the same effect as
 * <code> x = myrecord["AGE"]</code>
 * @author cole
 */
public class MemberFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  an error occurred in the number or type of
	 * function arguments, or the member name does not exist in the
	 * given record.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(2, 2, new int[] { Value.RECORD, Value.STRING });

		final Value e = arglist.element(0);
		if (e == null)
			return null;
		if (e.getType() != Value.RECORD)
			return null;

		final Value key = arglist.element(1);
		final String keyName = key.getString().toUpperCase();
		
		/*
		 * At this point, e is the recordValue data element, and key is the
		 * string key. Use the TreeMap part of e to get a Value, with the string
		 * part of key as the lookup value.
		 */
		final Value v = e.getElement(keyName);
		if( v == null ) {
			if( arglist.session.signalFunctionErrors())
				throw new JBasicException(Status.NOMEMBER, keyName);
			return new Value(0);
		}
		return v.copy();
	}

}

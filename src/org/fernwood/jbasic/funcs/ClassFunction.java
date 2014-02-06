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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;


/**
 * <b>CLASS()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Create a JBasic CLASS object for a Java Class</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>r = CLASS(<em>class-name<em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Object record</td></tr>
 * </table>
 * <p>
 * Given the name of an available Java class, create a JBasic CLASS object that represents
 * the native underlying Java class.  The JBasic CLASS object contains meta data that identifies
 * that it is a Java class (as opposed to a JBasic class) and the information needed to
 * create an instance of that class.  This is part of the Java interface that allows JBasic
 * to directly manipulate Java objects when needed.
 * @author cole
 */
public class ClassFunction extends JBasicFunction {

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

		arglist.validate(1, 1, new int [] { Value.STRING});

		String className = arglist.element(0).getString();
		try {
			Class.forName(className);
		} catch (ClassNotFoundException e) {
			if( arglist.session.signalFunctionErrors())
				throw new JBasicException(Status.INVOBJECT, className);
			return new Value(Value.RECORD, null);
		}
		
		Value result = new Value(Value.RECORD, null);
		
		Value objectData = new Value(Value.RECORD, null);
		objectData.setElement(new Value(className), "CLASS");
		objectData.setElement(new Value(true), "ISCLASS");
		objectData.setElement(new Value(JBasic.getUniqueID()), "ID");
		objectData.setElement(new Value(true), "ISJAVA");
		result.setElement(objectData, "_OBJECT$DATA");
		
		return result;
	}

}

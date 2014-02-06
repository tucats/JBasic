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
import org.fernwood.jbasic.compiler.CompileContext;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>CLASSOF()</b> JBasic Function
 * <p>
 * <table>
 * <tr>
 * <td><b>Description:</b></td>
 * <td>Return the argument's Java class name as a string</td>
 * </tr>
 * <tr>
 * <td><b>Invocation:</b></td>
 * <td><code>b = CLASSOF(<em>value</em>)</code></td>
 * </tr>
 * <tr>
 * <td><b>Returns:</b></td>
 * <td>String</td>
 * </tr>
 * </table>
 * <p>
 * Return the argument's Java class name as a string. If the argument is
 * a natural JBasic value, then it will be "int" for an INTEGER or 
 * "java.lang.String" for a string; i.e. it will reflect the underlying
 * Java type used for the JBasic value.  If the argument is a Java object
 * wrapper, then the class returned is that of the object stored in the
 * value.  For example, if X = NEW("java.util.ArrayList") then the result
 * of CLASSOF(X) is "java.util.ArrayList".
 * <p>
 * Similar results can be gotten as OBJECTDATA(X).CLASS for objects, but
 * there is no OBJECTDATA for native JBasic values.
 * 
 * @author cole
 * 
 */
public class ClassofFunction extends JBasicFunction {

	public Status compile(CompileContext work ) throws JBasicException {
		
		work.validate(1, 1);
		if( !work.constantArguments || work.argPosition == 0 )
			return new Status(Status.UNKFUNC);
		
		Status status = new Status();
		
		Value [] r = getArguments( work );
		deleteArguments(work);
		work.byteCode.add( ByteCode._STRING, r[0].getObjectClass().toString());
		
		return status;
		
	}
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

		if( arglist.size() != 1 )
			throw new JBasicException(Status.ARGERR);
		
		String className = null;
		Class classDesignation = arglist.element(0).getObjectClass();
		if( classDesignation == null )
			className = "<unknown>";
		else {
			className = classDesignation.toString();
			if( className.startsWith("class "))
				className = className.substring(6);
		}
		
		return new Value(className);

	}

}

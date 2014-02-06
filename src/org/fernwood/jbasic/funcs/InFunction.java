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
 * <b>IN()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Determine if a value is in a list</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>b = IN( v, list)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Boolean</td></tr>
 * </table>
 * <p>
 * IF the first argument is found in any subsequent argument, returns true.
 * @author cole
 */
public class InFunction extends JBasicFunction {
	/**
	 * Compile the function.  The primary purpose here is to detect when there
	 * are more than two arguments, indicating that the argument list itself is
	 * meant to be the array we compare against.  In this case, code is generated
	 * to convert the argument list to an array, and then the regular runtime code
	 * is called.
	 * @param work the compiler context, including the current byteCode
	 * stream.
	 * @return a Status value indicating if the compilation was successful.
	 * @throws JBasicException   an argument count or type error occurred
	 */

	public Status compile(final CompileContext work) throws JBasicException {
		work.validate(2,100);
		
		if( work.argumentCount > 2)
			work.byteCode.add(ByteCode._ARRAY, work.argumentCount-1);
		work.byteCode.add(ByteCode._CALLF, 2, "IN");

		return new Status();
	}

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(2, 2, new int [] { Value.UNDEFINED, Value.UNDEFINED} );
				
		Value testValue = arglist.element(0);
		Value compareList = arglist.element(1);
		
		if (compareList.isType(Value.ARRAY)) {

			int count = compareList.size();
			for (int ix = 1; ix <= count; ix++) {
				if (testValue.compare(compareList.getElement(ix)) == 0)
					return new Value(true);
			}
			return new Value(false);
		}
		
		if (compareList.isType(Value.RECORD) && testValue.isType(Value.STRING)) {

			Value member = compareList.getElement(testValue.getString().toUpperCase());
			return new Value(member != null);
		}
		
		throw new JBasicException(Status.ARGTYPE);
	}
}

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
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>CHARACTER()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Character value</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = CHARACTER( i )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Convert a numeric (integer) value to a single character having the character
 * value of the integer argument. For example, <code>CHARACTER(65)</code> will
 * result in the string "A", since the letter "A" is 65 in the ASCII code set.
 * <p>
 * If the argument list is a list of integer values or an array of values, then
 * the result is a string created by using each value as an integer that
 * represents each character of the string.  So <code>CHARACTER(65,[66,67]) </code>
 * will
 * result in the string "ABC", since the integers 65..67 are the character
 * values for 'A', 'B', and 'C'.
 * 
 * @author cole
 * 
 */
public class CharacterFunction extends JBasicFunction {

	/**
	 * Compile the function.  This checks to see if the most common
	 * case of an integer constant has been given.  In that case, we
	 * can generate a _STRING instruction to load the single character.
	 * If the conditions aren't right for this optimization, we return
	 * an error so the compilation is not performed.
	 * 
	 * @param work the compiler context, including the current byteCode
	 * stream.
	 * @return a Status value indicating if the compilation was successful.
	 */

	public Status compile(final CompileContext work)  {
		
		if( work.argumentCount == 1 & work.byteCode.size() > 0 ) {
			Instruction i = work.byteCode.getInstruction(work.byteCode.size()-1);
			if( i.opCode == ByteCode._INTEGER) {
				i.opCode = ByteCode._STRING;
				return new Status();
			}
		}

		/*
		 * It's not possible to generate the optimized form, return UNKFUNC
		 * which tells the compiler to generate a standard CALLF invocation.
		 */

		return new Status(Status.UNKFUNC);
		
	}

	/**
	 * Execute the function
	 * 
	 * @param arglist
	 *            The function argument list
	 * @param symbols
	 *            The runtime symbol table
	 * @return the Value of the function.
	 * @throws JBasicException if an argument or execution error occurs
	 */
	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		/*
		 * There must be at least one argument.
		 */

		arglist.validate(1, -1, null);

		/*
		 * Take the argument(s) and make a flat array of them.
		 */
		Value list = new Value(Value.ARRAY, null);
		for( int i = 0; i < arglist.size(); i++ ) {
			if(list.addElement(arglist.element(i)) == 0 ) {
				throw new JBasicException( Status.ARGERR);
			}
		}
		
		/*
		 * Scan over the array and build a string buffer from the
		 * integer values in each element.
		 */

		StringBuffer result = new StringBuffer();
		
		for( int i = 1; i <= list.size(); i++ ) {
			int charCode = list.getInteger(i);
			result.append((char)charCode);
		}
		return new Value(result.toString());
	
	}
}

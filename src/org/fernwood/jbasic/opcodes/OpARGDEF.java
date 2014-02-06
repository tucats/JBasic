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
package org.fernwood.jbasic.opcodes;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * Remove an element from the argument array for the current program and
 * store it in a local variable.  Also pop an item from the stack, and if the
 * argument is not available in the argument list, then use the stack item
 * as the default value.
 * <p>
 * If the string argument is not present, then the resulting argument value
 * (either the one from the argument list or the one on the stack) is placed
 * back on the stack. This is used for explicit typing of arguments, since the
 * ARGDEF is followed by a CVT and a STOR operation.
 * @author cole
 */
public class OpARGDEF extends AbstractOpcode {

	/**
	 *  <b><code>_ARGDEF item, "name"</code><br><br></b>
	 * Execute the _ARGDEF instruction at runtime, removes an item by position
	 * from the argument list structure and stores it in a local variable.  If
	 * there is no such argument in the list, then a default value is used 
	 * from the stack.
	 * 
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li><code>item</code> - the argument position number, 1-based,
	 * that is to be copied to local storage.</l1>
	 * <li><code>name</code> - The local variable name in which to store
	 * the argument value.</l1>
	 * </list><br><br>
	 * <b>Implicit Arguments:</b><br>
	 * <list>
	 * <li><code>stack[tos]</code> - The default value to apply if there
	 * is no argument at the 'item' position.  This item is removed from
	 * the stack by this instruction regardless of whether it is used or
	 * not.</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 */

	public void execute(final InstructionContext env) throws JBasicException {

		final SymbolTable s = env.localSymbols;
		final Instruction i = env.instruction;
		final Value defaultValue = env.codeStream.pop();
		final Value argList = env.getArgList();
		final int argNum = i.integerOperand;
		
		if( argNum < 1 )
			throw new JBasicException(Status.FAULT, new Status(Status.INVOPARG, argNum));
		
		Value argValue = null;

		/*
		 * If there is no argument list (possibly because the program was
		 * invoked with a RUN command rather than a CALL) then create an
		 * argument list now, and set the element number to the provided
		 * default value.
		 */
		if (argList == null) {
			env.argList = new Value(Value.ARRAY, null );
			argList.setElement(argValue, i.integerOperand);
			argValue = defaultValue;
		}
		else {
			
			/*
			 * See if there is already a value for this argument list item.
			 * If not (null returned from getElement()) then set the
			 * argument to the default value.
			 */
			argValue = argList.getElement(argNum);
			if (argValue == null) {
				argValue = defaultValue;
				argList.setElementOverride(argValue, argNum);
			}
		}

		/*
		 * Store the argument value (or the default) in the target variable
		 * if there is one, or put it on the stack if no target specified.
		 */
		if( i.stringValid)
			s.insertLocal(i.stringOperand, argValue);
		else
			env.push(argValue);
	}

}

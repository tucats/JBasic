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
 * store it in a local variable.
 * @author cole
 * 
 */
public class OpARG extends AbstractOpcode {

	/**
	 *  <b><code>_ARG item, "name"</code><br><br></b>
	 * Execute the _ARG instruction at runtime, removes an item by position
	 * from the argument list structure and stores it in a local variable.
	 * <p>
	 * If the string argument is not present, then the resulting argument value
	 * is placed back on the stack. This is used for explicit typing of arguments, 
	 * since the _ARG is followed by a CVT and a STOR operation.
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li><code>item</code> - the argument position number, 1-based,
	 * that is to be copied to local storage.</l1>
	 * <li><code>name</code> - The local variable name in which to store
	 * the argument value.</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 */

	public void execute(final InstructionContext env) throws JBasicException {

		final SymbolTable s = env.localSymbols;
		final Instruction i = env.instruction;
		final Value argList = env.getArgList();
		final int argNum = i.integerOperand;
		
		/*
		 * If there isn't an active argument list then we can't make this
		 * call.
		 */
		if (argList == null)
			throw new JBasicException(Status.ARGERR);

		if( argNum < 1)
			throw new JBasicException(Status.FAULT, new Status(Status.INVOPARG, argNum));
		
		/*
		 * Get the indicated argument by index number.  If the argument does
		 * not exist, throw an error.  When this happens, report the error
		 * by using either the argument name or the string "parameter n" where
		 * "n" is the argument number.
		 */
		final Value argValue = argList.getElement(argNum);
		if (argValue == null) {
			String msgArg = null;
			if( i.stringValid)
				msgArg = i.stringOperand;
			else
				msgArg = "parameter " + argNum;
			
			throw new JBasicException(Status.ARGNOTGIVEN, msgArg);
		}
		
		/*
		 * If we have a string argument, it's the name of the local variable that
		 * we store the value in.  If there is no string argument, then put the
		 * value on the stack; subsequent instructions will convert and store it.
		 */
		if( i.stringValid)
			s.insertLocal(i.stringOperand, argValue);
		else
			env.push(argValue);
		
	}

}

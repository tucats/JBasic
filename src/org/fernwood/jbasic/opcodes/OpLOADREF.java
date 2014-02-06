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

import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpLOADREF extends AbstractOpcode {

	/**
	 * Flag indicating that after the load, the variable should be cleared
	 * from the symbol table.
	 */
	public static final int LOADREF_AND_CLEAR = -1;

	/**
	 * Load a reference to an item on the stack - does not make a copy!
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * Locate the value using the symbol name given.
		 */

		final String symbolName = env.instruction.stringOperand;
		env.codeStream.refPrimary(symbolName, false);

		Value value1 = env.localSymbols.reference(symbolName);

		if( env.instruction.integerOperand == LOADREF_AND_CLEAR)
			env.localSymbols.deleteAlways(symbolName);
		
		/*
		 * Otherwise, if there is type coercion in the instruction,
		 * deal with that now.  When a type coercion is required,
		 * we must generate a new instance of the object instead of
		 * a reference after all.
		 * 
		 * Then push the result.
		 */

		if (env.instruction.integerValid && env.instruction.integerOperand > 0) {
			if (value1.fSymbol)
				value1 = value1.copy();
			value1.coerce(env.instruction.integerOperand);
		}

		env.push(value1);
	}

}

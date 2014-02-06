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
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpPROT extends AbstractOpcode {

	/**
	 * Mark a variable READONLY
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * If the integer parameter was given, then it means to skip that many
		 * tables up, relative to our own. So a value of 1 means your parent
		 * table, and a value of 2 means your grandparent table, and so on. A
		 * negative value means use the global table in all cases.
		 * 
		 */
		final int argc = env.instruction.integerValid ? env.instruction.integerOperand : 0;

		SymbolTable localTable = env.localSymbols;

		if (argc < 0)
			localTable = env.session.globals();
		else
			for (int ix = 0; ix < argc; ix++)
				if (localTable.parentTable != null)
					localTable = localTable.parentTable;

		final Value value3 = env.localSymbols.reference(env.instruction.stringOperand);
		value3.fReadonly = true;


	}

}

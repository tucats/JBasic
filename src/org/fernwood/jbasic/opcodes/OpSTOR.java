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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpSTOR extends AbstractOpcode {

	/**
	 * Store top stack item in a variable.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value value1 = env.popForUpdate();
				
		/*
		 * If the integer parameter was given, then it means to skip that many
		 * tables up, relative to our own. So a value of 1 means your parent
		 * table, and a value of 2 means your grandparent table, and so on. A
		 * value of -1 means the global symbol table, and -2 means the absolute
		 * root table.
		 * 
		 * The most common case is a skip parameter of zero, which means use
		 * your nearest local table, so we optimize around that case.
		 */

		final int argc = env.instruction.integerValid ? env.instruction.integerOperand : 0;

		SymbolTable localTable = env.localSymbols;

		if (argc != 0)
			if (argc == -1)
				localTable = env.session.globals();
			else if (argc == -2)
				localTable = JBasic.rootTable;
			else if (argc == -3)
				localTable = env.session.macroTable;
		
			else
				for (int ix = 0; ix < argc; ix++)
					if (localTable.parentTable != null)
						localTable = localTable.parentTable;

		if( !env.codeStream.fDynamicSymbolCreation && localTable.localReference(env.instruction.stringOperand) == null)
			throw new JBasicException(Status.UNKVAR, env.instruction.stringOperand);
		
		/*
		 * Insert the new value in the table and we're done.  If the insert
		 * method returns false, then there was an access violation writing the
		 * symbol, and we flag an error.
		 */

		if( localTable.fRootTable)
			localTable.insertSynchronized(env.instruction.stringOperand, value1);
		else
			localTable.insert(env.instruction.stringOperand, value1);
		
		
	}

}

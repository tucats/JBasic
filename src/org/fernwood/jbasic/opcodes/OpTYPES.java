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

/**
 * @author cole
 * 
 */
public class OpTYPES extends AbstractOpcode {

	/**
	 * Set the dynamic typing status of the currently running program, or specify
	 * the return type that any function value is to be coerced as.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {


		int op = env.instruction.integerOperand;

		if( op == 0 || op == 1 ) {
			final boolean flag = env.instruction.integerOperand != 0;

			/*
			 * There really should be a program, or something has gone funny. But
			 * let's code defensively. Set the enclosing program's strong typing
			 * flag. This is almost certainly already set, but just in case...
			 */
			if (env.codeStream.statement != null)
				if (env.codeStream.statement.program != null)
					env.codeStream.statement.program.fStaticTyping = flag;

			/*
			 * More importantly from a runtime point of view, set the flag in the
			 * symbol table that says we are honoring types of existing data
			 * elements.
			 */
			env.localSymbols.setStrongTyping(flag);
		}

		else if( op > 1 ) {
			env.codeStream.returnType = op;
		}

	}

}

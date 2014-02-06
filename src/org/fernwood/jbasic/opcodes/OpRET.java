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
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.ScopeControlBlock;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpRET extends AbstractOpcode {

	/**
	 * Return from a program with no RETURN value
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		Value value1;
		/*
		 * If this is the kind with a RETURN value then we store it away in our
		 * exit vector.
		 */
		final int returnKind = env.instruction.integerOperand;
		if (returnKind == 1) {
			value1 = env.pop();

			if( env.codeStream.returnType != Value.UNDEFINED)
				value1.coerce(env.codeStream.returnType);
			
			/*
			 * Store the result in the reserved name ARG$RESULT, in the local
			 * table. The CALLF and CALLP opcodes and the function processor
			 * will use this.
			 */
			env.localSymbols.insertLocal("ARG$RESULT", value1);

			if (env.codeStream.debugger != null)
				env.codeStream.debugger.markReturn();
			throw new JBasicException(Status.RETURN);

		}

		/*
		 * No return value, this might be a GOSUB return. Check it out.
		 */

		if (env.codeStream.statement.program != null) {

			int stackSize = 0;

			if (env.codeStream.fLinked) {
				if (env.codeStream.statement.program.gosubStack != null)
					if ((stackSize = env.codeStream.statement.program.gosubStack.size()) != 0) {

						final ScopeControlBlock scope = env.codeStream.statement.program.gosubStack
								.get(stackSize-1);
						env.codeStream.programCounter = scope.returnStatement + 1;
						env.codeStream.statement.program.gosubStack.remove(stackSize-1);

						return;
					}
			} else {
				throw new JBasicException(Status.FAULT, "Unlinked _RET");
			}
		}

		if (env.codeStream.debugger != null)
			env.codeStream.debugger.markReturn();

		throw new JBasicException(Status.RETURN);

	}

}

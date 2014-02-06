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

import java.util.ArrayList;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Linkage;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.ScopeControlBlock;

/**
 * @author cole
 * 
 */
public class OpJSB extends AbstractOpcode {

	/**
	 * GOSUB (local procedure call) to a label in the current program. The label
	 * is defined by the string operand.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		if (env.codeStream.fLinked) {
			final ScopeControlBlock scope = new ScopeControlBlock();

			/*
			 * Destination could be a symbolic address or a bytecode address
			 * depending on the behavior of the optimizer and linker.
			 */

			if (env.instruction.stringValid) {
				final Linkage t = env.codeStream.labelMap
						.get(env.instruction.stringOperand);
				scope.targetStatement = t.byteAddress;
			} else
				scope.targetStatement = env.instruction.integerOperand;

			scope.scopeType = ScopeControlBlock.GOSUB;
			scope.returnStatement = env.codeStream.programCounter;
			scope.activeProgram = env.codeStream.statement.program;

			if (env.codeStream.statement.program.gosubStack == null)
				env.codeStream.statement.program.gosubStack = new ArrayList<ScopeControlBlock>();

			env.codeStream.statement.program.gosubStack.add(scope);
			env.codeStream.programCounter = scope.targetStatement;
			return;
		}

		throw new JBasicException(Status.FAULT, "Unlinked _JSB");

	}

}

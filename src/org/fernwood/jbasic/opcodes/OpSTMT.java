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

import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;

/**
 * @author cole
 * 
 */
public class OpSTMT extends AbstractOpcode {

	/**
	 * Marker for statement boundaries. This is relevant when more than one
	 * statement is present in the bytecode. No operation is required by the
	 * program at execution time.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		/*
		 * Track total statements executed.
		 */
		if( env.session != null )
			env.session.statementsExecuted++;
		final int lineNumber = env.instruction.integerOperand;
		if( lineNumber > 0 )
			env.codeStream.lastLineNumber = lineNumber;
		
		/*
		 * If tracing is requested, but we're a protected program, then we don't
		 * trace.
		 */

		boolean doTrace = env.fStatementTrace && env.codeStream.fLinked;
		if (doTrace)
			if (env.codeStream.statement != null)
				if (env.codeStream.statement.program != null)
					if (env.codeStream.statement.program.isProtected())
						doTrace = false;

		/*
		 * If statement tracing is on (the TRACE verb) then print out something
		 * helpful to know we're executing this statement. We only do this when
		 * we are in linked mode to prevent duplicate output.
		 */

		if (doTrace) {
			String fmtNum = "      ";
			if (env.instruction.integerOperand > 0) {
				if (env.instruction.integerOperand > 0)
					fmtNum = Integer.toString(env.instruction.integerOperand);
				else {
					fmtNum = Integer.toString(-(env.instruction.integerOperand));
					fmtNum = "(" + fmtNum + ")";
				}
				fmtNum = Utility.pad(fmtNum, 5 );
			}
			String fmtFile = "<console>";
			if (env.codeStream.statement != null)
				if (env.codeStream.statement.program != null)
					fmtFile = env.codeStream.statement.program.getName();

			int statementID = env.instruction.integerOperand;
			
			env.session.stdout.println("Trace " + fmtFile + ";  " + fmtNum + " "
					+ env.codeStream.getStatementText(statementID));

		}

		/*
		 * If there is a debugger attached to this code stream, then give it
		 * a chance to take control on a statement STEP boundary.
		 */
		if (env.codeStream.debugger != null) {
			Status sts = env.codeStream.debugger.step(env.codeStream, env.localSymbols);
			if (sts != null)
				if (sts.equals(Status.QUIT)) {
					env.codeStream.debugger = null;
					return;
				}
			if( sts != null )
				throw new JBasicException(sts);
		}
		
		/*
		 * If there is anything on the stack at this moment, then it is indicative
		 * of a code generation error or an unhandled runtime fault, since the stack
		 * must be exhausted on each statement boundary.
		 */
		if( env.codeStream.dataStack.size() > 0 ) {
			/* This is only permitted when we are doing ASM operations */
			Instruction nextOp = env.codeStream.getInstruction(env.codeStream.programCounter);
			if( nextOp.opCode == ByteCode._SBOX)
				if( nextOp.stringValid)
					if( nextOp.stringOperand.equals(Permissions.ASM))
						return;
			
			throw new JBasicException(Status.FAULT, "data left on stack after previous statement");
		}
		/*
		 * All done.
		 */
		return;
	}

}

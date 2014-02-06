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
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpTRACE extends AbstractOpcode {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fernwood.jbasic.opcodes.OpCode#execute(org.fernwood.jbasic.opcodes.OpEnv)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * Use the integer argument to set the trace flag for the environment
		 * object.
		 */

		final int mode = env.instruction.integerOperand;
		
		if( mode < 0 | mode > 3 )
			throw new JBasicException(Status.FAULT,
					new Status(Status.INVOPARG, mode));
		
		boolean byteTrace = ((mode % 2) == 0) ? false : true;
		boolean stmtTrace = ((mode / 2) == 0) ? false : true;

		/*
		 * If we are part of a statement in a protected program, then
		 * we cannot ever set TRACE to be enabled, so in this case just
		 * override the flag silently.
		 */
		if( env.codeStream.statement != null )
			if( env.codeStream.statement.program != null )
				if( env.codeStream.statement.program.isProtected()) {
					byteTrace = false;
					stmtTrace = false;
				}
		
		Value v = env.session.globals().findReference("SYS$TRACE_BYTECODE", false);
		v.setBoolean(byteTrace);
		
		v = env.session.globals().findReference("SYS$TRACE_STATEMENTS", false);
		v.setBoolean(stmtTrace);
	}

}

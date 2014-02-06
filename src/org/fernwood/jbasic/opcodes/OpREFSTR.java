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

/**
 * _AND performes boolean AND on top two stack items.
 * @author Tom Cole
 */
public class OpREFSTR extends AbstractOpcode {

	/**
	 *  <b><code>_REFSTR <em>code</em></code><br><br></b>
	 * Enable capture of a reference string, or finish the capture
	 * and leave the reference string on the stack.  The code value
	 * is zero for starting a new capture, and non-zero (1) to
	 * put the capture string on the stack and clear it in the
	 * bytecode.  This is used to support <code>PRINT <em>reference</em>=</code>
	 * syntax.
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 */

	public void execute(final InstructionContext env) throws JBasicException {

		int code = env.instruction.integerOperand;
		if( !env.instruction.integerValid)
			code = 0;
	
		switch( code ) {
		case 0:	/* Start a new capture */
			env.codeStream.setCaptureBuffer(true);
			return;
		
		case 1: /* End capture, leave result on stack */
		case 2: /* End capture, leave result on stack with trailing "=" also */
			
			if( !env.codeStream.hasCaptureBuffer()) {
				throw new JBasicException(Status.FAULT, new Status(Status.REFSTR));
			}
			
			if( code == 2 )
				env.codeStream.refSecondary("=");
			
			env.push(env.codeStream.getCaptureBuffer());
			env.codeStream.setCaptureBuffer(false);
			return;
			
		}
		throw new JBasicException(Status.FAULT, "Invalid _REFSTR " + code);

	}
}

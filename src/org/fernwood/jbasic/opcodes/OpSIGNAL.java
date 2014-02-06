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
public class OpSIGNAL extends AbstractOpcode {

	/**
	 *  <b><code>_SIGNAL [<em>flag</em>] [<em>"code"</em>]</code><br><br></b>
	 * Create a JBasic Status() object and signal it.
	 * <li>
	 * The <em>code</em> contains the code, and if <em>flag</em> is 1 then an argument popped from the stack.
	 * <li>The <em>code</em> is empty, in which case the code is the string on the stack and if <em>flag</em> is 1
	 * it also includes the argument from the stack</l1>
	 * <li>The <em>string</em> is empty and the top of stack is a RECORD with 
	 * <code>CODE</code> and optional <code>PARM</code> fields used to construct the signal.
	 * 
	 * <br><p><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value signalArgument = null;
		String signalCode = null;

		if (env.instruction.stringValid)
			signalCode = env.instruction.stringOperand;
		else {
			Value v = env.pop();
			if( v.getType() == Value.RECORD) {
				if( env.instruction.integerOperand != 0)
					throw new JBasicException(Status.FAULT, 
							new Status(Status.INVOPARG, env.instruction.integerOperand));
				throw new JBasicException(new Status(v));
			}
			signalCode = v.getString().toUpperCase();
		}

		int argc = 0;

		if (env.instruction.integerValid)
			argc = env.instruction.integerOperand;

		if (argc == 1) {
			signalArgument = env.pop();
			throw new JBasicException(signalCode, signalArgument.getString());
		}
		else if (argc != 0 )
			throw new JBasicException(Status.FAULT,
					new Status(Status.INVOPARG, argc));

		/*
		 * Special case.  If this is *SUCCESS then even though it
		 * is success and will not cause a redirection, it needs
		 * to reset the SYS$STATUS variable.  Do that manually 
		 * here.
		 */
		
		if( signalCode.equals(Status.SUCCESS)) {
			Value status = new Value(Value.RECORD,null);
			status.setElement(new Value("*SUCCESS"), "CODE");
			status.setElement(new Value(""), "PARM");
			status.setElement(new Value(true), "SUCCESS");
			env.session.globals().insert("SYS$STATUS", status);
		}
		
		throw new JBasicException(signalCode);
	}

}

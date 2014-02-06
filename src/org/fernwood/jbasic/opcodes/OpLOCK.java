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

import org.fernwood.jbasic.LockManager;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpLOCK extends AbstractOpcode {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fernwood.jbasic.opcodes.OpCode#execute(org.fernwood.jbasic.opcodes.OpEnv)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		String tname = null;
		env.session.checkPermission(Permissions.THREADS);
		int code = env.instruction.integerOperand;

		if (env.instruction.stringValid)
			tname = env.instruction.stringOperand.toUpperCase();
		else {
			Value v = env.pop();
			if (v.getType() != Value.STRING)
				throw new JBasicException(Status.ARGTYPE);

			tname = v.getString().toUpperCase();
		}

		switch (code) {

		case 0:
			LockManager.lock(env.session, tname);
			break;

		case 1:
			if (!LockManager.create(env.session, tname))
				throw new JBasicException(Status.INVLOCK, tname);
			break;

		default:
			throw new JBasicException(Status.INVOPARG, code);
		}
	}

}

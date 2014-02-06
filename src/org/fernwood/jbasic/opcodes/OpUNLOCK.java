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
public class OpUNLOCK extends AbstractOpcode {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fernwood.jbasic.opcodes.OpCode#execute(org.fernwood.jbasic.opcodes.OpEnv)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Status status = null;
		String lockName;
		env.session.checkPermission(Permissions.THREADS);
		
		if (env.instruction.stringValid) {
			lockName = env.instruction.stringOperand.toUpperCase();
			if( !LockManager.isMine(env.session, lockName))
				throw new JBasicException(Status.INVLOCK, lockName);
			status = LockManager.release(env.session, lockName, false);
			if( status.failed())
				throw new JBasicException(status);
		}
		else {
			
			Value v = env.pop();
			if( v.getType() == Value.STRING) {
				lockName = v.getString().toUpperCase();
				if( !LockManager.isMine(env.session, lockName))
					throw new JBasicException(Status.INVLOCK, lockName);
				status = LockManager.release(env.session, v.getString().toUpperCase(), false);
				if( status.failed())
					throw new JBasicException(status);

			}
			else {
				if( v.getType() != Value.ARRAY ) 
					throw new JBasicException(Status.ARGTYPE);
				for( int i = 1; i <= v.size(); i++ ) {
					Value e = v.getElement(i);
					boolean allFlag = false;
					/*
					 * Based on the element type, find the name.  It could be
					 * an array of strings, in which case use the string.  Or
					 * it could be an array of records, in which case it must
					 * use the "NAME" field, assuming this is output from a
					 * LOCKS() function call.
					 */

					lockName = null;
					if( e.getType() == Value.RECORD ) {
						lockName = e.getElement("NAME").getString().toUpperCase();
						allFlag = true;
					}
					else if( e.getType() == Value.STRING)
						lockName = e.getString().toUpperCase();
					
					if( lockName == null )
						throw new JBasicException(Status.INVTYPE);

					if( !LockManager.isMine(env.session, lockName))
						continue;
					
					status = LockManager.release(env.session, lockName, allFlag);
					if( status.failed())
						throw new JBasicException(status);

				}
			}
		}
	}

}

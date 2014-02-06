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
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpCONSOLE extends AbstractOpcode {

	/**
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		/*
		 * See if this is a CONSOLE reset operation
		 */
		
		env.session.checkPermission(Permissions.FILE_IO);
		
		Value fref = null;
		
		switch( env.instruction.integerOperand) {

		case 0:
			fref = env.localSymbols.findReference("CONSOLE_INPUT", false);
			if( fref == null )
				throw new JBasicException(Status.NOSUCHFID, "CONSOLE_INPUT");
			env.session.stdin = JBasicFile.lookup(env.session, fref);
			fref = env.localSymbols.findReference("CONSOLE_OUTPUT", false);
			if( fref == null )
				throw new JBasicException(Status.NOSUCHFID, "CONSOLE_OUTPUT");
			env.session.stdout = JBasicFile.lookup(env.session, fref);
			break;
			
		case 1:
			fref = env.pop();
			env.session.stdin = JBasicFile.lookup(env.session, fref);
			break;
			
		case 2:
			fref = env.pop();
			env.session.stdout = JBasicFile.lookup(env.session, fref);
			break;
			
		default:
			throw new JBasicException(Status.FAULT, 
					new Status(Status.INVOPARG, env.instruction.integerOperand));
		}
		
		return;
	}

}

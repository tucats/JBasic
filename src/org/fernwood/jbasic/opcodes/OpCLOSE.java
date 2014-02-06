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
import org.fernwood.jbasic.runtime.JBFPipe;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpCLOSE extends AbstractOpcode {

	/**
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		/*
		 * See if this is a CLOSE ALL operation.  This is true when
		 * the integer operand is 1 (normally zero).  It could also
		 * be a CLOSE PIPE which closes the write side of a pipe
		 * but lets us capture any output from the pipe still. This
		 * is indicated by a mode switch of 2.
		 */
		boolean pipeClose = false;
		
		if( env.instruction.integerOperand == 1 ) {
			JBasicFile.closeUserFiles(env.session, env.localSymbols);
			return;
		}
		else
			if( env.instruction.integerOperand == 2 )
				pipeClose = true;
			else
				if( env.instruction.integerOperand != 0 )
					throw new JBasicException(Status.FAULT, 
							new Status(Status.INVOPARG, env.instruction.integerOperand));

		/*
		 * Otherwise we're closing a specific file.  The name might be on
		 * the stack or it might be in the CLOSE operation itself.  Get the
		 * identifier name from whichever place has it.
		 */
		Value fileID = null;
		if( env.instruction.stringValid)
			fileID = new Value(env.instruction.stringOperand);
		else
			fileID = env.pop();
		
		final JBasicFile fileToClose = JBasicFile.lookup(env.session, fileID);

		if (fileToClose == null)
			throw new JBasicException(Status.FNOPEN, fileID.toString());

		/*
		 * If it is a system file, then we don't have the power to close it.
		 * This prevents the user from closing files that we need like the
		 * console.
		 */

		if (fileToClose.isSystem())
			throw new JBasicException(Status.FILESYS);

		/*
		 * If it's a pipe close operation, verify this is a PIPE first
		 */
		if( pipeClose ) {
			if( fileToClose.getMode() != JBasicFile.MODE_PIPE)
				throw new JBasicException(Status.FILE, new Status(Status.WRONGMODE, "PIPE"));
			JBFPipe pf = (JBFPipe) fileToClose;
			pf.closePipe();
			if( pf.getStatus().failed())
				throw new JBasicException(pf.getStatus());
			return;
		}
		
		/*
		 * Otherwise, let's delete the identifier and close the file.
		 */
		env.localSymbols.delete(fileToClose.getIdentifier());
		fileToClose.close();
	}

}

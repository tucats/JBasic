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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Loader;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;


/**
 * @author cole
 * 
 */
public class OpLOADFILE extends AbstractOpcode {

	/**
	 * Load a symbol value on the stack
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * Get the file name we are to load.
		 */
		String fname = env.pop().getString();
		String pname = fname.toUpperCase();
		boolean loadIF = (env.instruction.integerOperand == 1 );
		/*
		 * If this is a LOAD IF and the program already exists, we're done
		 */

		if (loadIF)
			if (env.session.programs.find(pname) != null)
				return;

		/*
		 * A real load is permitted if we are sandbox mode unless you have FILE_IO
		 * privileges.
		 */
		env.session.checkPermission(Permissions.FILE_IO);


		String fsName;
		try {
			fsName = JBasic.userManager.makeFSPath(env.session, fname);
		} catch (JBasicException e) {
			throw new JBasicException( e.getStatus());
		}
		Status status = Loader.loadFile(env.session, fsName);
		if( status.failed())
			throw new JBasicException(status);
	}
}
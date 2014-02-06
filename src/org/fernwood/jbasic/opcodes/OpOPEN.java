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
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpOPEN extends AbstractOpcode {

	/**
	 * File open. Mode is in integer parameter, name is on stack... store the
	 * result in the variable name give as the string parameter.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * See if we are in the "Sandbox".  If so, then check to see if we have
		 * FILE IO permissions.
		 */

		env.session.checkPermission(Permissions.FILE_IO);
			
		String fileIdentifier = null;
		int fileMode = 0;

		/*
		 * There are two modes, conventional and "GWBASIC" mode.  In the
		 * GWBASIC mode, all the operators are on the stack.  We tell we
		 * are in this mode because we have no operands to the OPEN at all.
		 */
		
		if( !env.instruction.integerValid && !env.instruction.stringValid) {
			
			int fileNumber = env.pop().getInteger();
			fileIdentifier = JBasic.FILEPREFIX + Integer.toString(fileNumber);
			
			String tempFileMode = env.pop().getString().toUpperCase();
			char firstChar = tempFileMode.charAt(0);
			switch( firstChar ) {
			case 'I':	fileMode = JBasicFile.MODE_INPUT;
						break;
						
			case 'O':	fileMode = JBasicFile.MODE_OUTPUT;
						break;
			
			case 'A':	fileMode = JBasicFile.MODE_APPEND;
						break;
			
			case 'P':	fileMode = JBasicFile.MODE_PIPE;
						break;
			
			case 'Q':	fileMode = JBasicFile.MODE_QUEUE;
						break;
						
			default:
				throw new JBasicException(Status.FILEMODE, tempFileMode);
			}
			
		}
		else {
			fileMode = env.instruction.integerOperand;
			if( !env.instruction.stringValid) {
				fileIdentifier = env.pop().getString();
			}
			else
				fileIdentifier = env.instruction.stringOperand;
		}
		
		Value value1 = env.pop(); /* Get the name */

		/*
		 * Don't let the user re-use an open file reference. See if the
		 * identifier exists and is a valid file. IF so, then we have a runtime
		 * error.
		 */
		Value oldfid;
		try {
			oldfid = env.localSymbols.reference(fileIdentifier);
		} catch (JBasicException e) {
			oldfid = null;
			}
		if (oldfid != null) {
			final JBasicFile oldfile = JBasicFile.lookup(env.session, oldfid);
			if (oldfile != null) {

				/*
				 * As a kindness, fix the file prefix to look like a user file
				 * number, i.e. "__FILE_3" becomes "#3"
				 */
				if (fileIdentifier.startsWith(JBasic.FILEPREFIX))
					fileIdentifier = "#"
							+ fileIdentifier.substring(JBasic.FILEPREFIX
									.length());
				throw new JBasicException(Status.FILEINUSE, fileIdentifier);
			}
		}

		
		/*
		 * No conflict with identifier space, let's try to create the file
		 * object (and access the underlying file as well.
		 */
		final JBasicFile newfile = JBasicFile.newInstance(env.session, fileMode);
		newfile.open(value1, env.localSymbols);

		/*
		 * If it went well, then let's register the identifier created to
		 * represent the new file using the identifier name.
		 */
		if (newfile.getStatus().success()) {
			newfile.setIdentifier(fileIdentifier);
			env.localSymbols.insert(fileIdentifier, newfile.getFileID());
		}

		/*
		 * All done. Let the runtime engine know if we succeeded in creating the
		 * new file object...
		 */
		return;

	}

}

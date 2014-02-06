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

import java.io.File;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.FSMConnectionManager;
import org.fernwood.jbasic.runtime.FSMFile;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpKILL extends AbstractOpcode {

	/**
	 * KILL (or delete a file). The TOS tells what to delete. The integer
	 * operand is 1 if this is a file reference, 0 if it is a string file name.
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		Status status = null;
		final Value fileIdentifier = env.pop(); /* File name or identifier */
		JBasicFile killFile = null;
		File f = null;
		String fName = null;
		final boolean isOpenFile = env.instruction.integerOperand > 0;


		/*
		 * If the syntax specified a file identifier, then we must track down an
		 * already-open file.
		 */
		if (isOpenFile) {

			/*
			 * To kill an open file, you must have FILE_IO privilege.
			 */
			env.session.checkPermission(Permissions.FILE_IO);

			killFile = JBasicFile.lookup(env.session, fileIdentifier);

			/*
			 * If the file was not a valid file handle, error.
			 */
			if (killFile == null)
				throw new JBasicException(Status.INVFID);

			/*
			 * If the file is opened by the system, error.
			 */
			if (killFile.isSystem())
				throw new JBasicException(Status.FILESYS);

			/*
			 * Okay, get it's name so we can do the file delete operation.
			 */
			fName = killFile.getName();

		} else {

			/*
			 * To kill a file by name, you must have DIR_IO privilege
			 */
			if( !env.session.hasPermission(Permissions.DIR_IO))
				throw new JBasicException(Status.SANDBOX, Permissions.DIR_IO);

			/*
			 * Not a file identifier syntax, so the stack item is expected to be
			 * the filename. Get the filename string.
			 */
			fName = fileIdentifier.getString();
		}

		/*
		 * If after all that the fname is null, then it means we didn't ever get
		 * a value that could be reasonably used as a string. (Attempting to
		 * close by identifier when the identifier isn't a file but rather an
		 * array or something would cause this).
		 */

		if (fName == null)
			throw new JBasicException(Status.INVFID);

		/*
		 * See if it's an FSM name
		 */
		
		if( fName.length()>6 && fName.substring(0,6).equalsIgnoreCase("FSM://")) {
		
			FSMConnectionManager cnx = new FSMConnectionManager();
			cnx.parse(fName);
			String path = cnx.getPath();
			cnx.setPath(null);
			
			FSMFile fsm = null;
			try {
				if (isOpenFile) {
					if( killFile != null ) {
						killFile.close();
						env.localSymbols.delete(killFile.getIdentifier());
					}
				}

				fsm = new FSMFile(cnx.toString());
				fsm.delete(path);
				fsm.terminate();
			} catch (Exception e) {
				if( fsm != null )
					throw new JBasicException(Status.FNDEL, new Status(Status.FAULT, fsm.status()));
				throw new JBasicException(Status.FNDEL, fName);
			}
		} else {
		
		/*
		 * Try to create a file object to reference this, so we can do the
		 * delete.  Handle name munging for multiuser mode.
		 */
		
		fName = JBasic.userManager.makeFSPath(env.session, fName);
		f = new File(fName);

		/*
		 * Anticipate the possibility of an error by setting the status to show a
		 * problem. We'll fix it to success later if we succeed.
		 */
		status = new Status(Status.FNDEL, fName);

		/*
		 * Try to delete the file. If it was an already-opened file, then we'll
		 * first have to close it and remove the symbol. If the delete succeeds,
		 * we can return success.
		 */
		
		boolean success = false;
		
		try {

			if (isOpenFile) {
				if( killFile != null ) {
					killFile.close();
					env.localSymbols.delete(killFile.getIdentifier());
				}
			}

			// If a security manager is present, this can throw exception.
			success = f.delete();

		} catch (final Exception e) {
			throw new JBasicException(status);
		}
		if( !success )
			throw new JBasicException(status);
		}
	}

}

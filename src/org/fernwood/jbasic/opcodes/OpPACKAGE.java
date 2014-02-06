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
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpPACKAGE extends AbstractOpcode {

	static String PACKAGE_RESTORE = "SYS$PACKAGES_RESTORE";

	/**
	 * Add or delete an entry from the SYS$PACKAGES list used to qualify class
	 * names for user-written statements and functions, and for the NEW()
	 * function that creates Java objects by class name.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		env.session.checkPermission("JAVA");

		Value packagePath = null;
		if (env.instruction.stringValid)
			packagePath = new Value(env.instruction.stringOperand);
		else if (env.instruction.integerOperand != 2)
			packagePath = env.pop();

		if( packagePath == null )
			processPackagePath(env.instruction.integerOperand, 
					env.session.globals(), null);
		else
		if( packagePath.getType() == Value.ARRAY ) {
			for( int idx = 1; idx <= packagePath.size(); idx++) {
				Value pathElement = packagePath.getElement(idx);
				if( pathElement.getType() != Value.STRING)
					throw new JBasicException(Status.INVTYPE);
				processPackagePath(env.instruction.integerOperand, 
						env.session.globals(), pathElement);
			}
		}
		else
			if( packagePath.getType() == Value.STRING)
				processPackagePath(env.instruction.integerOperand, 
						env.session.globals(), packagePath);
			else
				throw new JBasicException(Status.ARGTYPE);
		
		return;
	}

	/**
	 * Process a single string path item to be inserted or deleted from the 
	 * package list
	 * @param mode the mode 0 - add, 1 - delete, 2 - restore
	 * @param symbols the symbol table to find SYS$PACKAGES
	 * @param packagePath
	 * @throws JBasicException
	 */
	private void processPackagePath(int mode, SymbolTable symbols,
			Value packagePath) throws JBasicException {
		/*
		 * Make sure it's a string, and has no trailing period(s) in the path
		 * name.
		 */
		String s = null;
		if (packagePath != null) {
			s = packagePath.getString();
			while (s.endsWith(".")) {
				s = s.substring(0, s.length() - 1);
			}
		}
	
		/*
		 * Get the package list. If it doesn't exist, create it as an empty
		 * array.
		 */
		Value packageList = symbols.findReference(JBasic.PACKAGES,
				false);
		if (packageList == null) {
			packageList = new Value(Value.ARRAY, null);
			symbols.insertReadOnly(JBasic.PACKAGES, packageList);
		}

		/*
		 * Get the backup package list. If it doesn't exist, create it now as a
		 * copy of the current package list.
		 */
		Value backupPackageList = symbols.findReference(PACKAGE_RESTORE, false);
		if (backupPackageList == null) {
			backupPackageList = packageList.copy();
			symbols.insertReadOnly(PACKAGE_RESTORE,
					backupPackageList);
		}

		/*
		 * Based on the instruction opcode, add, delete, or restore items in the
		 * package list.
		 */
		int index = 0;
		switch (mode) {

		case 0: /* Add a package to the list */
			/*
			 * See if this item is already on the list. If so, don't add it
			 * again.
			 */

			if( packagePath == null )
				throw new JBasicException(Status.INVOPARG, 	mode);
			for (index = 1; index <= packageList.size(); index++)
				if (packageList.getString(index).equals(s))
					return;

			index = packageList.size() + 1;
			packageList.setElementOverride(new Value(s), index);
			break;

		case 1: /* Remove item from the list if found */

			if( packagePath == null )
				throw new JBasicException(Status.INVOPARG, mode);
			for (index = 1; index <= packageList.size(); index++)
				if (packageList.getString(index).equals(s))
					break;
			if (index > packageList.size())
				return;

			packageList.removeArrayElement(index);
			break;

		case 2: /*
				 * Reset the package list to the value before we started messing
				 * with it
				 */
			packageList.fReadonly = false;
			symbols.insertReadOnly(JBasic.PACKAGES, backupPackageList);
			symbols.deleteAlways(PACKAGE_RESTORE);
			break;

		default:
			throw new JBasicException(Status.INVOPARG, mode);
		}
	}
}

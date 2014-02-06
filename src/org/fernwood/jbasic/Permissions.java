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
 * Created on May 15, 2008 by tom
 *
 */
package org.fernwood.jbasic;

import org.fernwood.jbasic.statements.SortStatement;
import org.fernwood.jbasic.value.Value;

/**
 * This class defines the permission names used for "Sandbox" management
 * in JBasic.  These constants are used in the argument of the _SBOX
 * opcode, in explicit permission checks, and are stored in the user
 * database file on disk for server use.
 * 
 * @author tom
 * @version version 1.0 May 15, 2008
 *
 */
public class Permissions {

	/**
	 * Permission: able to create and manage threads
	 */
	public final static String THREADS = "THREADS";
	
	/**
	 * Permission: able to read a file
	 */
	public final static String FILE_READ = "FILE_READ";
	
	/**
	 * Permission: able to write to a file
	 */
	public final static String FILE_WRITE = "FILE_WRITE";
	
	/**
	 * Permission: able to use the assembler
	 */
	public final static String ASM = "ASM";

	/**
	 * Permission: able to perform file-level IO
	 */
	public static final String FILE_IO = "FILE_IO";

	/**
	 * Permission: able to perform directory IO
	 */
	public static final String DIR_IO = "DIR_IO";

	/**
	 * Permission: able to perform user-level administration functions
	 */
	public static final String ADMIN_USER = "ADMIN_USER";

	/**
	 * Permission: able to spawn subprocesses to execute native OS commands
	 */
	public static final String SHELL = "SHELL";

	/**
	 * Permission: able to change one's own password
	 */
	public static final String PASSWORD = "PASSWORD";

	/**
	 * Permission: able to change the state of the running server
	 */
	public static final String ADMIN_SERVER = "ADMIN_SERVER";

	/**
	 * Permission: able to manipulate host file system path names.
	 */
	public static final String FS_NAMES = "FSNAMES";

	/**
	 * Permission: able to change the system logging level
	 */
	public static final String LOGGING = "LOGGING";
	
	/**
	 * Permission: able to create Java wrapper objects
	 */
	
	public static final String JAVA = "JAVA";
	

	/**
	 * This is the list of names. This is used to determine if 
	 * a permission name is valid, and to create the list of all
	 * names for the "ALL" case.  This list must be complete!
	 */
	public static String [] validNames = new String [] { 
			THREADS,		FILE_READ,
			ASM,			FILE_WRITE,
			FILE_IO,		DIR_IO,
			ADMIN_USER,		ADMIN_SERVER,
			SHELL,			FS_NAMES,
			PASSWORD,		LOGGING,
			JAVA
	};

	/**
	 * Determine if a given permission name is valid; i.e. is it a real
	 * permission name.
	 * 
	 * @param permission a String containing the permission name to be
	 * validated.
	 * @return true if the permission name is valid.
	 */
	public static boolean valid(String permission) {
		
		for( String name : validNames) {
			if( permission.equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	/**
	 * Return an array containing the names of all the permissions.
	 * @return An ARRAY value containing a list of string Values
	 */
	public static Value allPermissions() {
		Value v = new Value(Value.ARRAY, null);
		for( String name : validNames ) {
			v.addElement(new Value(name));
		}
		SortStatement.sortArray(v);

		return v;
	}

}

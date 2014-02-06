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
package org.fernwood.jbasic.funcs;

import java.io.File;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>MKDIR()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Path name of directory to create</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>g = MKDIR( f )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Boolean</td></tr>
 * </table>
 * <p>
 * Given a file path name, attempt to create a directory of that name.  Intermediate
 * directories are created as necessary.  Returns true if the directory was
 * created, else false if it already existed.
 * 
 * @author cole
 * 
 */
public class MkdirFunction extends JBasicFunction {

	/*
	 * File system separator.
	 */
	final static String fileSystemSeparator = System.getProperty("file.separator");

	/**
	 * Execute the function
	 * 
	 * @param arglist
	 *            The function argument list
	 * @param symbols
	 *            The runtime symbol table
	 * @return the Value of the function.
	 * @throws JBasicException an error occurred in the number or type of
	 * function arguments.
	 */
	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {
		
		arglist.validate(1, 2, new int [] { Value.STRING, Value.BOOLEAN });
		String fullPath = JBasic.userManager.makeFSPath(arglist.session, arglist.stringElement(0));
		boolean recurse = true;
		if( arglist.size() > 1 )
			recurse = arglist.booleanElement(1);
		
		String sep = System.getProperty("file.separator");
		
		if(! fullPath.substring(fullPath.length() - sep.length()).equals(sep)) {
			fullPath = fullPath + sep;
		}
		
		return new Value(makeDir(fullPath, recurse));

	}
	
	/**
	 * Create a directory given a path name.  
	 * @param path The path name of the directory to create
	 * @param recurse True if intermediate directories are to be created as needed.  False if the 
	 * intermediate paths are required to already exist.
	 * @return true if the new directory could be created.
	 * @throws JBasicException if the directory could not be created
	 */
	public static boolean makeDir( String path, boolean recurse ) throws JBasicException {
		
		/*
		 * Does this location already exist?
		 */
		
		File f = new File(path);
		if( f.exists())
			return false;
		
		/*
		 * Can we create this location? If so then we're done.
		 */
		
		if( f.mkdir())
			return true;
		
		/*
		 * If we're not supposed to create the intermediate paths, then we're done.
		 */
		if( !recurse )
			throw new JBasicException(Status.FILE, "permission error creating directory");
				
		/*
		 * Let's see if the root above us exists.
		 */
		int i = 0;
		int fsLen = fileSystemSeparator.length();
		
		for( i = (path.length()-fsLen) - 1; i > 0; i-- ) {
			String subPath = path.substring(i,i+fsLen);
			if( subPath.equals(fileSystemSeparator))
				break;
		}
		
		/*
		 * If there is a "parent" part of the path, call ourselves
		 * recursively to try to create the parent. Then, try to create
		 * the current path again before returning.
		 * 
		 */
		if( i > 0 ) {
			//System.out.println("Attempting " + path.substring(0,i));
			makeDir(path.substring(0, i), recurse);
			if( f.mkdir())
				return true;
		}
		
		/*
		 * No dice, can't create this one.
		 */
		throw new JBasicException(Status.FILE, "permission error creating directory");
		

	}
}

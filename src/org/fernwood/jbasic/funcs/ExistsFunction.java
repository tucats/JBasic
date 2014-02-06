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
import java.io.InputStream;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.FSMConnectionManager;
import org.fernwood.jbasic.runtime.JBFInput;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>EXISTS()</b> JBasic Function
 * <p>
 * <table>
 * <tr>
 * <td><b>Description:</b></td>
 * <td>Test to see if a file exists</td>
 * </tr>
 * <tr>
 * <td><b>Invocation:</b></td>
 * <td><code>b = EXISTS(<em>path-string</em>)</code></td>
 * </tr>
 * <tr>
 * <td><b>Returns:</b></td>
 * <td>Boolean</td>
 * </tr>
 * </table>
 * <p>
 * Test to see if a named file exists or not. The name is a string
 * parameter that must be the fully qualified path name of the file (since
 * "default directories" are machine dependent). This function returns a boolean
 * value of <code>true</code> if the file exists and can be read, or
 * <code>false</code> if the file does not exist or cannot be opened for
 * input. <br>
 * <br>
 * This function adopts the same syntax notation as the OPEN statement; a file
 * name that starts with an at-sign character is not in the file system but is
 * part of the resource path of JBasic instead (typically, stored in the JAR
 * file itself). This is used by the HELP verb to determine if the help text
 * file can be found, for example.
 * 
 * @author cole
 * 
 */
public class ExistsFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException if an argument or execution error occurs
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.session.checkPermission(Permissions.DIR_IO);

		String fname;

		arglist.validate(1, 1, new int [] { Value.STRING});

		fname = arglist.stringElement(0);

		/*
		 * If it's a resource, we see if it's possible to get it as a stream.
		 */
		if (fname.substring(0, 1).equals("@")) {

			final String fisname = fname.substring(1, fname.length());
			final InputStream fis = JBasic.class.getResourceAsStream(fisname);
			if (fis == null)
				return new Value(false);

			return new Value(true);
		}

		/*
		 * See if it's an FSM file.
		 */
		
		String fsmURL = FSMConnectionManager.convertToURL(fname);
		if( fsmURL != null ) {
			JBFInput f = new JBFInput(arglist.session);
			try {
				f.open(arglist.element(0), symbols);
				f.close();
			}
			catch( JBasicException e ) {
				return new Value(false);
			}
			return new Value(true);
		}
		
		
		/*
		 * It's not a resource, so use the File class to see if it exists or
		 * not.  Process it through the UserManager to convert any remote
		 * user path syntax.
		 */

		String fsPath = JBasic.userManager.makeFSPath(arglist.session, fname);
		final File f = new File(fsPath);
		return new Value(f.exists());

	}

}

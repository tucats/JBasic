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

import java.util.Iterator;

import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;


/**
 * <b>OPENFILES()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return list of open files</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = OPENFILES()</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Record of records</td></tr>
 * </table>
 * Return an RECORD containing an element for each open file. The element key is
 * the file identifier, and the value is the file identification record. Use the
 * <code>MEMBERS()</code> function to get the list of members of the record.
 * 
 * @author cole
 * 
 */
public class OpenfilesFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  if an argument or execution error occurs
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(0, 0, null);
		final Value result = new Value(Value.RECORD, null);

		if (arglist.session.openUserFiles == null)
			return result;
		
		arglist.session.checkPermission(Permissions.FILE_IO);


		for (final Iterator i = arglist.session.openUserFiles.values().iterator(); i
				.hasNext();) {
			final JBasicFile m = (JBasicFile) i.next();
			final String name = m.getIdentifier();
			if( name != null )
				result.setElement(m.getFileID(), name);
		}
		return result;
	}

}

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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>USERS()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return list of user data</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = USERS(["name"])</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of records</td></tr>
 * </table>
 * <p>
 * Returns an array of records, one for each active user in the multiuser
 * server.  If the server is not running (SYS$MODE is "SINGLEUSER") then
 * this array will be empty. It is an error to call this when you are a
 * remote user and do not have the ADMIN_USER privilege.  
 * <p>
 * Each record contains the user's name, ACTIVE state, current program, etc.
 * <p>
 * If the optional string parameter is given, it is the name of a specific
 * user whose information is to be returned.  IF the user does not exist, 
 * then an empty RECORD is returned.
 * @author cole
 *
 */
public class UsersFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException   An error in the count or type of argument
	 * occurred.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {
		
		
		arglist.session.checkPermission(Permissions.ADMIN_USER);

		Value list = JBasic.userManager.userList();
		
		if( arglist.size() == 0 )
			return list;
		
		if( arglist.size() > 1 )
			throw new JBasicException(Status.TOOMANYARGS);

		
		String key = arglist.stringElement(0).toUpperCase();
		Value user = null;
		for( int ix = 1; ix <= list.size(); ix++ ) {
			user = list.getElement(ix);
			String userName = user.getElement("USER").getString();
			if( userName.equals(key))
				return user;
		}
		return new Value(Value.RECORD, null);
		
	}

}

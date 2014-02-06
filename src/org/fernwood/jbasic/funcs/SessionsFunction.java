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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>SESSIONS()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return list of user data</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = USERS(["name"])</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of records</td></tr>
 * </table>
 * <p>
 * Returns an array of records, one for each active session.  The record
 * contains the key value SESSION which is the session ID, and the key
 * value USER which is the USER name of that session.
 * @author cole
 *
 */
public class SessionsFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  There was an error in the type or count of
	 * function arguments, or there was a permission error for a sandboxed user.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {
		
		
		arglist.session.checkPermission(Permissions.ADMIN_USER);
		
		arglist.validate(0, 1, new int [] { Value.STRING });
		
		Value result = new Value(Value.RECORD, null);
		String key = null;
		if( arglist.size() > 0 )
			key = arglist.stringElement(0);
		
		if( JBasic.activeSessions == null )
			return result;
		
		Iterator i = JBasic.activeSessions.keySet().iterator();
		while( i.hasNext()) {
			String id = (String) i.next();
			JBasic session = JBasic.activeSessions.get(id);
			String userName = session.getUserIdentity().getName();
			if( key != null )
				if(!key.equals(userName))
					continue;
			
			/* Members are sessions names, value is user name of session */
			result.setElement(new Value(userName), id);
		}
		return result;
		
	}

}

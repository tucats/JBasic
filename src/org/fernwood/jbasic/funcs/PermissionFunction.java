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
 * <b>PERMISSION()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Test to see if current user has a given permission</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>b = PERMISSION( s )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Boolean</td></tr>
 * </table>
 * <p>
 * Tests to see if the current user has a given permission, expressed as a string.
 * If the permission does not exist or is not granted to the current user, then
 * the function returns false.  If the current user has the given permission, 
 * then the function returns true.
 * <p>
 * If the user has ADMIN_USER privilege, then the permission name is optional.
 * In this case, the return value in an array of strings, with the names of
 * all the permissions currently granted to the user.
 * @author cole
 *
 */
public class PermissionFunction extends JBasicFunction {

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

		if( arglist.size() == 0 ) {
			/*
			 * If we aren't in a sandbox, then it requires no permission to see this info.
			 * 
			 */
			if( !arglist.session.inSandbox()) {
				if( arglist.session.getUserIdentity() == null )
					return Permissions.allPermissions();
				return arglist.session.getUserIdentity().getPermissions();
			}
			/*
			 * If we have ADMIN_USER then return the array of permission
			 * names.  If we don't have permission, then this will throw
			 * an error to our caller.
			 
			arglist.session.checkPermission(Permissions.ADMIN_USER);
			 */
			return JBasic.userManager.permissions(arglist.session.getUserIdentity().getName());
			
		}
		
		/*
		 * All cases of the empty argument list have been handled, so at this
		 * point we'd require a single argument the permission name being
		 * tested for.
		 */
		arglist.validate(1, 1, new int [] { Value.STRING });
		
		String permission = arglist.stringElement(0).toUpperCase();
		if( !Permissions.valid(permission)) {
			throw new JBasicException(Status.INVPERM, permission);
			
		}
		boolean state = arglist.session.hasPermission(permission);
		
		return new Value(state);

	}

}

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
import org.fernwood.jbasic.runtime.FSMConnectionManager;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>MOUNTS()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return current FSM mount point list.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>r = MOUNTS(  )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of array of strings</td></tr>
 * </table>
 * <p>
 * Return an array of arrays. Each member array contains two strings; the
 * virtual mount point and the FSM URL that matches it.
 * <p>
 * 
 * @author cole
 *
 */
public class MountsFunction extends JBasicFunction {
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param argList the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException   an argument count or type error occurred or a
	 * permissions error occurred for a sandboxed user.
	 */

	public Value run(final ArgumentList argList, final SymbolTable symbols) throws JBasicException {

		argList.validate(0, 0, new int[] {});
		argList.session.checkPermission(Permissions.DIR_IO);

		Value result = new Value(Value.ARRAY, null);
		
		Iterator<String> i = FSMConnectionManager.mountPointIterator();
		while( i.hasNext()) {
			String mountPointName = i.next();
			String fsmName = FSMConnectionManager.findMountPoint(mountPointName);
			Value arrayElement = new Value(Value.ARRAY, null);
			arrayElement.setElement(new Value(mountPointName), 1);
			arrayElement.setElement(new Value(fsmName), 2);
			result.addElementAsIs(arrayElement);
		}

		return result;
		
	}
}

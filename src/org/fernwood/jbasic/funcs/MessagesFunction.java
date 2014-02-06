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

import org.fernwood.jbasic.Message;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>MESSAGES()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return list known MESSAGE codes</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = MESSAGES()</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of strings</td></tr>
 * </table>
 * <p>
 * This returns an array containing the string code names of every known MESSAGE for which
 * there is a code->text translation.  
 * @author cole
 */

public class MessagesFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException an error occurred in the number or type of
	 * function arguments
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(0, 0, null);
		final Value result = new Value(Value.ARRAY, "MESSAGES");
		int idx = 1;
		for (final Iterator i = arglist.session.messageManager.iterator(); i
				.hasNext();) {
			final Message m = (Message) i.next();
			String code = m.getMessageCode();
			final int len = code.length();
			if (len < 5)
				continue;

			if (!code.substring(len - 4).equals("(EN)"))
				continue;

			code = code.substring(0, len - 4);
			result.setElement(new Value(code), idx++);
		}

		return result;
	}

}

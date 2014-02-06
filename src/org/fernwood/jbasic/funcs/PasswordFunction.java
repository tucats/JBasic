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
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SimpleCipher;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.runtime.UserManager;
import org.fernwood.jbasic.value.Value;

/**
 * <b>PASSWORD()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Hash of a user password string</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = PASSWORD( p )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Return a string that is an encoded hash of the password.  The encoded hash
 * is the same as the value stored in the user password database.
 * @author Tom Cole
 *
 */
public class PasswordFunction extends JBasicFunction {

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

		int wordCount = 0;
		if( arglist.size() == 1 && arglist.element(0).getType() == Value.INTEGER)
			wordCount = arglist.element(0).getInteger();
		else
			arglist.validate(0, 1, new int [] {Value.STRING});

		if( wordCount < 0 || wordCount > 10)
			wordCount = 3;
		
		if( arglist.size() == 0 || wordCount > 0 )
			return new Value(JBasic.userManager.generatePassword(wordCount));
		
		SimpleCipher cipher = new SimpleCipher(arglist.session);
		JBasic.userManager.initializePasswordHash();
		return new Value( cipher.encryptedString(arglist.stringElement(0), UserManager.secret));
	}

}

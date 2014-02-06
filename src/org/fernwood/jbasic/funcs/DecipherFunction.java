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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.CompileContext;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SimpleCipher;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>DECIPHER()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Decrypt a string</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s1 = DECIPHER( s [, p] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Decrypt a string s using a salted password cipher.  If a specific password
 * is to be used, it is the second parameter.  If not supplied, a default 
 * internal password is used.  The password originally used with CIPHER() must
 * be supplied to decrypt the string.  If the string to be decrypted has been
 * tampered with or the password is not valid, then DECIPHER() returns an
 * empty string.
 * <p>
 * This function uses the java.crypt package, but only uses the moderately secure
 * password encryption functions.  As a result, this encryption scheme does not
 * fall into the category of cryptography export restrictions.
 * @author cole
 */
public class DecipherFunction extends JBasicFunction {

	/**
	 * Compile the DECIPHER function.  This involves a conversion of the
	 * encrypted string into an encoded integer array, that can be used
	 * by the DECIPHER runtime function.  Note that this function is
	 * called to support compilation of the function call, but the runtime
	 * support still actually calls the run() method to execute the work
	 * of the function.
	 * 
	 * @param work the compilation context
	 * @return Status indicating that the function was compiled successfully.
	 * @throws JBasicException if an argument or execution error occurs
	 */
	public Status compile(final CompileContext work) throws JBasicException {
		
		work.validate(1, 2);
		
		if( work.argumentCount == 2 ) {
			work.byteCode.add(ByteCode._SWAP );
		}
		work.byteCode.add(ByteCode._CALLF, 1, "CRYPTSTR");
		if( work.argumentCount == 2 ) {
			work.byteCode.add(ByteCode._SWAP );
		}
		work.byteCode.add(ByteCode._CALLF, work.argumentCount, "DECIPHER");
		return new Status();
	}
	
	
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param argList the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException if an argument or execution error occurs
	 */

	public Value run(final ArgumentList argList, final SymbolTable symbols) throws JBasicException {

		argList.validate(1, 2, new int[] { Value.ARRAY, Value.STRING });
		
		SimpleCipher cipher = new SimpleCipher(argList.session);
		
		if( argList.size() == 2 )
			cipher.setPassword( argList.stringElement(1));
		else {
			SymbolTable globals = argList.session.globals();
			Value dfpw;
			try {
				dfpw = globals.reference("SYS$SECRET");
			} catch (JBasicException e) {
				dfpw = null;			}
			cipher.setPassword(dfpw);
		}
		
		return cipher.decrypt(argList.element(0));
	}

}

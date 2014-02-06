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

import javax.swing.JOptionPane;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>INPUTDIALOG()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Display a modal dialog to capture a text string</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>b = INPUTDIALOG([<em>prompt-string</em>])</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * This uses the Java SWING library to display a modal dialog to the user, and accept text
 * input.  The optional argument is a prompt string placed in the dialog; the default is "Input?".
 * The user can enter text and click the Okay or Cancel buttons; if the Cancel button is selected
 * the function result is an empty string.
 * 
 * @author cole
 */
public class InputdialogFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function for INPUTDIALOG()
	 * @param argList the argument list to the function
	 * @param symbols the symbol table to use at runtime
	 * @return a string value that was the user's input, or an empty string if the
	 * user selected the cancel operation from the dialog.
	 * @throws JBasicException  an argument count or type error occurred, or
	 * a permissions error occurred for a sandboxed user.
	 */
	public Value run( ArgumentList argList, SymbolTable symbols ) throws JBasicException {
		
		String promptString = null;
		if( argList.session.getBoolean("SYS$SANDBOX")) {
			throw new JBasicException(Status.SANDBOX);
		}
	
		if( argList.size() > 0 )
			promptString = argList.stringElement(0);
		else
			promptString = "Input?";
		
		String input = JOptionPane.showInputDialog(promptString);
		if( input == null )
			input = "";

		return new Value(input);
	}
}

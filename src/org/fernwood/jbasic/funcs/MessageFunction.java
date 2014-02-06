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
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;


/**
 * <b>MESSAGE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Format a status message</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>v = MESSAGE( <em>code</em> [,<em>parameter</em>])</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * The status message code is formatted using the current language definition (the default is EN
 * for English).  If supplied, the second argument is a string containing the parameter for the
 * status code, if any.  When a parameter is expected by the message but none is supplied, the
 * parameter is set to the string "<none>".  If the message code given does not have a corresponding
 * message text, then the code is returned as the function result. 
 * <p>
 * Message text is normally defined by JBasic, using the "Messages.jbasic" program that is stored
 * as part of JBasic.  When system messages are created, the text equivalents (in each supported
 * language) are placed in this file.  Additionally, user-defined message text can
 * be defined using MESSAGE statements in user programs. 
 * <p>
 * The first parameter can also be an entire STATUS record (such as the global variable
 * <code>SYS$STATUS</code>)
 * which contains both the code and parameter data.  In this case there is no second argument, and
 * the message is formatted using the contents of the status record.
 * @author cole
 */
public class MessageFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  an error occurred in the number or type of
	 * function arguments
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		/*
		 * Must be 1 or two parameters.  We don't do type checking here
		 * because they could be more than one type.
		 */
		arglist.validate(1, 2, null);
		
		String code = null;
		Status sts = null;

		/*
		 * Peek at the first element.  Its type tells us how to process
		 * the arguments.
		 */
		
		Value cv = arglist.element(0);
		
		/*
		 * If the argument is a string, then it is a string containing the
		 * message code and there might be an optional message substitution value
		 * parameter.
		 */
		if (cv.getType() == Value.STRING) {
			
			code = cv.getString().toUpperCase();
			/*
			 * If the code value has no message, try it again with an "*"
			 * in front, which is used internally to indicate successful
			 * return codes.  This allows "SUCCESS" to be passed and 
			 * report the "No error" message, for example.
			 */
			if (!arglist.session.messageManager.hasMessageText(code))
				if (arglist.session.messageManager.hasMessageText( "*" + code))
					code = "*" + code;
			/*
			 * Create a new status code with the given code name.
			 */
			sts = new Status(code);

			/*
			 * If there was an argument given, get the value (string or
			 * integer) and store it as the substitution value for the
			 * status object.
			 */
			if (arglist.size() > 1)
					sts.setMessageParameter(arglist.stringElement(1));
		} 
		/*
		 * The argument might also be a RECORD in which case it has a CODE field
		 * and an optional PARM field that are the message code and the optional
		 * message substitution value.
		 */
		else if (cv.getType() == Value.RECORD) {
			
			/*
			 * The CODE field is a required field
			 */
			Value v = cv.getElement("CODE");
			if (v == null) {
				return null;
			}
			code = v.getString().toUpperCase();
			sts = new Status(code);

			/*
			 * The PARM field is optional; if it's found the set it as the
			 * substitution string.  It could be a simple string value, or
			 * it could be a nested status value.
			 */
			v = cv.getElement("PARM");
			if (v != null) {
				if( v.getType() == Value.RECORD)
					sts.setNestedStatus(new Status(v));
				else
					sts.setMessageParameter(v.getString());
			}
		} 
		/*
		 * If the first argument was a string or a valid record, then signal an
		 * argument error.
		 */
		else
			throw new JBasicException(Status.ARGERR);

		/*
		 * Otherwise, ask the status object we constructed to format itself and
		 * return the result as a string value.
		 */
		return new Value(sts.getMessage(arglist.session));
	}

}

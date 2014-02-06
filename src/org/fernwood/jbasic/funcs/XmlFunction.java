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

import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.runtime.XMLManager;
import org.fernwood.jbasic.value.Value;

/**
 * <b>XML()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return XML representation</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = XML(<em>any-expression</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Convert the argument which is any Value into an XML string that
 * contains a portable representation of the value.  IF no argument
 * is given, then the result is the generic XML header.  If a second
 * argument is given, it is a boolean flag that indicates if the
 * string is to be formatted for readability.  If a third argument is
 * given,it contains a comment string to be inserted in the XML before
 * the formatted value.
 * @author cole
 *
 */
public class XmlFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException An error in the count or type of argument
	 * occurred, or the XML is incorrectly formed.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols)
													throws JBasicException {

		arglist.validate(0, 3, new int[] { Value.UNDEFINED, Value.INTEGER, Value.STRING});
		
		/*
		 * If no arguments, just produce the generic XML header
		 */
		if( arglist.size() == 0 )
			return new Value("<?xml version=\"1.0\" encoding=\"UTF-8\">");
		
		
		XMLManager xml = new XMLManager( arglist.session);
		
		int formatted = 1;
		if( arglist.size() >= 2 )
			formatted = arglist.intElement(1);

		StringBuffer result = new StringBuffer();
		
		String xmlName = null;
		if( arglist.size() >= 3 )
			xmlName = arglist.stringElement(2);
		
		if( formatted > 2)
			xml.setPassword("cheerios");
		
		Value v = arglist.element(0);
		result.append(xml.toXML(xmlName, v, formatted));
		
		return new Value(result.toString());
	}
}

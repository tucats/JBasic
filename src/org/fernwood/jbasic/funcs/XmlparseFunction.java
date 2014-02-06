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
 * <b>XMLPARSE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Parse a value from an XML string</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = XMLPARSE(<em>xml-string</em>)</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Any value</td></tr>
 * </table>
 * <p>
 * Convert the argument which is an XML string into a Value object,
 * assuming that the XML contains a valid JBasicValue structure.
 * @author cole
 *
 */
public class XmlparseFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  An error in the count or type of argument
	 * occurred.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols)
													throws JBasicException {

		arglist.validate(1, 2, new int[] { Value.STRING, Value.STRING});		
		String xml = arglist.element(0).getString();
		
		String rootName = null;
		if( arglist.size() >= 2 )
			rootName = arglist.stringElement(1);
		
		XMLManager parser = new XMLManager( arglist.session, xml);
		Value v = parser.parseXML(rootName);
		if( v == null )
			throw new JBasicException( parser.getStatus());
		
		return v;
	}
}

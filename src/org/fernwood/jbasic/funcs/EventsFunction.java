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

import java.util.ArrayList;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;
import org.fernwood.jbasic.JBasic;

/**
 * <b>EVENTS()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return event queue</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>i = EVENTS()</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of records</td></tr>
 * </table>
 * <p>
 * Return an array of records with the time stamp and the name of each event.
 * 65.
 * @author cole
 */

public class EventsFunction extends JBasicFunction {

	/**
	 * Execute the function
	 * 
	 * @param arglist
	 *            The function argument list
	 * @param symbols
	 *            The runtime symbol table
	 * @return the Value of the function.
	 * @throws JBasicException  if a parameter count or type error occurs
	 */
	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		if( arglist.size() > 0 )
			throw new JBasicException(Status.TOOMANYARGS);
		
		ArrayList<JBasic.EventObject> events = arglist.session.getEvents();
		
		
		/*
		 * If the argument is a string, return an array with each character
		 * resolved as an array element value.
		 */
		Value result = new Value(Value.ARRAY, null);

		for( int i = 0; i < events.size(); i++ ) {
			
			JBasic.EventObject eo = events.get(i);
			Value record = new Value(Value.RECORD, null);
			String msg = eo.name;
			String msgClass = "USER";
			if( msg.startsWith("$")) {
				msg = msg.substring(1);
				msgClass = "SYSTEM";
			}
			if( msg.startsWith("+")) {
				msg = msg.substring(1);
				msgClass = "MAIN";
			}
			record.setElement(msgClass, "CLASS");
			record.setElement(msg, "NAME");
			record.setElement(new Value(eo.time), "DURATION");
			record.setElement(new Value(eo.stamp), "TIME");
			result.addElement(record);
		}
		
		return result;
	}

}

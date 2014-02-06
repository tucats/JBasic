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
import org.fernwood.jbasic.runtime.JBasicThread;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;


/**
 * <b>THREAD()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Information about a thread.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>r = THREAD( <em>thread-name</em>  )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Record</td></tr>
 * </table>
 * <p>
 * Create a RECORD object that contains information known about the 
 * thread identified by name as the string parameter.
 * <p>
 * Items returned in the record include:
 * <p>
 * <table>
 * <tr><td><b>Item</b></td><td><b>Description</b></td></tr>
 * <tr><td>NAME</td><td>The name of the thread</td></tr>
 * <tr><td>RUNNING</td><td>Is the thread currently running?</td></tr>
 * <tr><td>CODE</td><td>The final status code for the thread</td></tr>
 * <tr><td>MESSAGE</td><td>The CODE formatted as a string</td></tr>
 * <tr><td>START_TIME</td><td>Floating point representation of the thread's start time</td></tr>
 * <tr><td>CMD</td><td>The statement used to start the thread</td></tr>
 * </table>
 * @author cole
 *
 */
public class ThreadFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException An error in the count or type of argument
	 * occured, or the thread identifier was not valid.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(1, 1, new int[] { Value.STRING });
		final Value v = new Value(Value.RECORD, null);

		final String name = arglist.stringElement(0).toUpperCase();
		final JBasicThread t = arglist.session.getChildThreads().get(name);
		if (t == null) {
			if( arglist.session.signalFunctionErrors())
				throw new JBasicException(Status.UNKTHREAD, name);
			return v;
		}
		v.setElement(new Value(name), "NAME");
		v.setElement(new Value(t.isAlive()), "RUNNING");
		v.setElement(new Value(t.getStatus().getCode()), "CODE");
		v.setElement(new Value(t.getStatus().getMessage(t.getJBasic())),
				"MESSAGE");
		v.setElement(new Value(t.startTime), "START_TIME");
		v.setElement(new Value(t.cmd), "CMD");

		return v;
	}

}

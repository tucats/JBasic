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
 * <b>MEMORY()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return information about free memory</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = MEMORY( <em>keyword-string</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Integer</td></tr>
 * </table>
 * <p>
 * Returns the amount of free memory.  By default, the amount of memory returned is the memory
 * (in bytes) that is currently used by JBasic in the process. If supplied, the argument is a
 * string containing a keyword that describes the kind of memory to report upon:
 * <p>
 * <table>
 * <tr><td><b>ARGUMENT</b></td><td><b>DESCRIPTION</b></td></tr>
 * <tr><td><code>USED<code></td><td>The memory currently used by JBasic</td></tr>
 * <tr><td><code>MAX<code></td><td>The maximum amount available</td></tr>
 * <tr><td><code>TOTAL<code></td><td>The memory allocated to JBasic</td></tr>
 * <tr><td><code>FREE<code></td><td>The memory allocated to JBasic but not currently in use</td></tr>
 * <tr><td><code>GC<code></td><td>FREE memory after "garbage collection"</td></tr>
 * </table> 
 * 
 * @author cole
 */
public class MemoryFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist
	 *            the function argument list and count already popped from the
	 *            runtime data stack
	 * @param symbols
	 *            the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  an error occurred in the number or type of
	 * function arguments, a perimission error occurred for a sandboxed
	 * user, or the specification for the class of memory reporting is invalid.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		long memsize = -1;
		String type = null;

		arglist.validate(0, 1,  new int [] { Value.STRING });

		if( arglist.session.getBoolean("SYS$SANDBOX")) {
			throw new JBasicException(Status.SANDBOX);
		}

		if (arglist.size() == 0)
			type = "USED";
		else
			type = arglist.stringElement(0).toUpperCase();

		if (type.equals("FREE"))
			memsize = Runtime.getRuntime().freeMemory();

		if (type.equals("MAX"))
			memsize = Runtime.getRuntime().maxMemory();

		if (type.equals("TOTAL"))
			memsize = Runtime.getRuntime().totalMemory();

		if (type.equals("USED"))
			memsize = Runtime.getRuntime().totalMemory()
					- Runtime.getRuntime().freeMemory();

		if (type.equals("GC")) {
			Runtime.getRuntime().gc();
			memsize = Runtime.getRuntime().freeMemory();
		}

		if( memsize < 0 )
			throw new JBasicException(Status.ARGERR);
		
		return new Value(memsize);

	}

}

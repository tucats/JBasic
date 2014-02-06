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
import org.fernwood.jbasic.runtime.RandomNumberGenerator;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>RNDVAR()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Implements the RND variable</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>d = RNDVAR()</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Integer</td></tr>
 * </table>
 * <p>
 * This function implements the <code>RND</code> pseudo-variable.  This
 * variable (available as long as GW-BASIC emulation is enabled, which is
 * the default) can be used in a JBasic program, and always contains a 
 * random integer value.  The compiler implements this variable by generating
 * a call to the <code>RNDVAR()</code> function. This would not normally be
 * called by the user.  
 * <p>
 * The <code>RANDOM()</code> function provides greater control and flexibility
 * in the range of values generated, etc. and is a more random number.  By 
 * contrast, the <code>RND</code> pseudo-variable and associated runtime
 * function have deterministic behavior based on the <code><b>RANDOMIZE</b></code>
 * statement, and will produce the identical sequence of integer values given
 * the same initial seed value.
 * 
 * @author cole
 */
public class RndvarFunction extends JBasicFunction {
	
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException the argument count is incorrect
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {
		arglist.validate(0, 0, null);

		if( JBasic.random == null )
			JBasic.random = new RandomNumberGenerator();
		
		int i = JBasic.random.get();
		return new Value(i);
	}
}

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
package org.fernwood.jbasic.opcodes;

import org.fernwood.jbasic.runtime.JBasicException;

/**
 * _AND performes boolean AND on top two stack items.
 * @author Tom Cole
 */
public class OpAND extends AbstractOpcode {

	/**
	 *  <b><code>_AND</code><br><br></b>
	 * Execute the _AND instruction at runtime to boolean and two values.  The
	 * left and right operators are bitwise ANDed and the result pushed back
	 * on the stack.
	 * 
	 * <p><br>
	 * <b>Implicit Arguments:</b><br>
	 * <list>
	 * <li>&nbsp;&nbsp;<code>stack[tos]</code> - left operator</l1>
	 * <li>&nbsp;&nbsp;<code>stack[tos-1]</code> - right operator</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 */

	public void execute(final InstructionContext env) throws JBasicException {

		final boolean left = env.codeStream.pop().getBoolean();
		final boolean right = env.codeStream.pop().getBoolean();

		env.codeStream.push(left && right);
	}
}

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
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpDUPREF extends AbstractOpcode {

	/**
	 * Duplicates the top stack item.  A second reference to the object at the
	 * top of the stack is made and pushed on the stack, so the top two items
	 * both point to the same underlying value.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * We don't want to pop this item off the stack (too inefficient) since
		 * we really don't want to disturb the top item anyway; just duplicate 
		 * it.  So see how big the stack is, and get a reference to the top
		 * item.
		 */
		final int tos = env.codeStream.stackSize();
		final Value value = env.codeStream.getStackElement(tos-1);
		
		/* 
		 * Now add the reference to the same item to the stack again.
		 */
		
		env.push(value);

		return;
	}

}

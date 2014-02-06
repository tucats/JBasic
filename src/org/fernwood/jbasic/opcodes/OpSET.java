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
public class OpSET extends AbstractOpcode {

	/**
	 * Pop the top stack item, and set it's contents to be the value at
	 * the S(2) position.  Generally a LOADREF, LOCREF, INDEX, or LOADR
	 * operation are used to get the item to write into.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

			
		/*
		 * Get the target which is a value that MUST have come from the
		 * symbol table.
		 */
		
		Value target = env.pop();
	
		/*
		 * Get the value that we are to store in the variable.  This must
		 * be a copy if the value was itself a symbolic item.
		 */
		Value src = env.popForUpdate();

		/*
		 * Move the target's data into the source object.
		 */
		
		target.set(src);
		
	}

}

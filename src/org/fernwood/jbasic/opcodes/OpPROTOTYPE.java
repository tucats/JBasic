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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;


/**
 * @author cole
 * 
 */
public class OpPROTOTYPE extends AbstractOpcode {

	/**
	 *  <b><code>_PROTOTYPE [<em>"descriptor"</em>]</code><br><br></b>
	 * 
	 * If the string argument is present, it should be a constant expression describing the
	 * required data type, using the words <code>INTEGER</code>, <code>STRING</code>,
	 * <code>DOUBLE</code>, and <code>BOOLEAN</code> to represent data types. Array and
	 * record syntax are also permitted.  For example, <code>"[string, string]"</code> is a descriptor
	 * that indicates an array that must contain exactly two string values.
	 * <p><br>
	 * If the string is not present, then a string item is popped from the stack and is
	 * used as the prototype.
	 * <p><br>
	 * The actual values are not important,
	 * the comparison is only done based on the types of each item.  However, the comparison
	 * is done as a deep traversal, so arrays and records are recursively processed to
	 * determine that all members and array elements exist and have the required type(s).
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li><code>descriptor</code> - The optional string descriptor.</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		String descriptor = null;
		if( env.instruction.stringValid)
			descriptor = env.instruction.stringOperand;
		else
			descriptor = env.pop().getString();
		
		Value result = Utility.protoType(env.session, descriptor);
		if( result == null )
				throw new JBasicException(Status.BADTYPE, descriptor);
		
		env.push(result);
			
	}

}

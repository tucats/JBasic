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
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * Test a value to verify it is of a given type.
 * @author cole
 * 
 */
public class OpASSERTTYPE extends AbstractOpcode {

	/**
	 *  <b><code>_ASSERTTYPE  <em>integer-type</em>, [<em>"signal"</em>]</code><br><br></b>
	 * Test the top of the stack to verify that it matches the <em>integer-type</em>
	 * type. If it does not, signal the given signal name.
	 * 
	 * <p><br>
	 * <b>Implicit Arguments:</b><br>
	 * <list>
	 * <li>&nbsp;&nbsp;<code>stack[tos]</code> - data to test</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		if( !env.instruction.integerValid)
			throw new JBasicException(Status.INVOPARG, "missing integer");
		
		final Value sourceValue = env.pop();
		int theType = env.instruction.integerOperand;
		if( sourceValue.getType() == theType)
			return;
		
		String name = sourceValue.getName();
		
		/*
		 * Did the caller give us a signal name to use?
		 */
		if( env.instruction.stringValid) {
			
			/*
			 * If the value has a name, make that the optional argument for the
			 * exception.  If no name,then just throw the exception as-is.
			 */
			if( name == null )
				throw new JBasicException(env.instruction.stringOperand);
			
			throw new JBasicException(env.instruction.stringOperand, name);
		}
		
		/*
		 * No name was supplied, so throw WRONGTYPE with the expected type supplied.
		 */
		
		throw new JBasicException(Status.WRONGTYPE, Value.typeToName(theType));
		
	}
}

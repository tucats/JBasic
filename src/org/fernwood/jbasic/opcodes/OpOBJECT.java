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
 * @author cole
 * 
 */
public class OpOBJECT extends AbstractOpcode {

	/**
	 * Object lookup. The member name is on top of the stack, and the object
	 * name is the string operand.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final String member = env.pop().getString(); /* member name */

		String lookupObject;

		if (env.instruction.stringValid) {
			lookupObject = env.instruction.stringOperand;
			env.codeStream.refPrimary(env.instruction.stringOperand, false);
		}
		else {
			final Value obj = env.pop();
			lookupObject = obj.getName();
			if (lookupObject == null)
				throw new JBasicException(Status.INVOBJECT, obj.toString());
		}
		Value value2, value3;
		env.codeStream.refSecondary("->" + lookupObject);
		while (true) {
			value2 = env.localSymbols.reference(lookupObject);
			if (!value2.isType(Value.RECORD))
				throw new JBasicException(Status.INVOBJECT, lookupObject);

			/*
			 * See if this object hold the item we seek. If so, push the value
			 * and we're done.
			 */
			value3 = value2.getElement(member);
			if (value3 != null) {
				env.push(value3.copy());
				break;
			}

			/*
			 * See if we have a PARENT object that let's us keep looking up a
			 * chain.
			 */

			value3 = value2.getObjectAttribute("PARENT");
			if (value3 == null)
				throw new JBasicException(Status.UNKVAR, member);
			lookupObject = value3.getString();
		}

		return;
	}

}

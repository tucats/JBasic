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

import java.math.BigDecimal;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpDIVI extends AbstractOpcode {

	/**
	 * Divide by an integer value, which is the operand value. This is a clone
	 * of DIV in most respects.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final int dividend = env.instruction.integerOperand;
		Value value1 = env.popForUpdate();

		/*
		 * Based on a the type of the variable value, do the right divide.
		 * For string and boolean which are not normally divisible, convert
		 * them to a double first.
		 */
		switch (value1.getType()) {

		case Value.DECIMAL:
			BigDecimal d1 = value1.getDecimal();
			BigDecimal d2 = new BigDecimal(dividend);
			BigDecimal d3 = d1.divide(d2,BigDecimal.ROUND_HALF_UP);
			value1.setDecimal(d3);
			break;
			
		case Value.STRING:
		case Value.BOOLEAN:
			value1.coerce(Value.DOUBLE);
			value1.setDouble(value1.getDouble() / dividend);
			break;
			
		case Value.DOUBLE:
			value1.setDouble(value1.getDouble() / dividend);
			break;

		case Value.INTEGER:
			value1.setInteger(value1.getInteger() / dividend);
			break;

		default:
			throw new JBasicException(Status.TYPEMISMATCH);

		}
		env.push(value1);

		return;
	}

}

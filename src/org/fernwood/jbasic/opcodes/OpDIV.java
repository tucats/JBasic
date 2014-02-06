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
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpDIV extends AbstractOpcode {

	/**
	 * Divide top two stack items, push result back on stack.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final Value sourceValue = env.pop();
		Value targetValue = env.popForUpdate();

		/*
		 * Based on a mutually agreed-upon type, do the right kind of math.
		 */
		int bestType = Expression.bestType(targetValue, sourceValue);
		targetValue.coerce(bestType);
		
		switch (bestType) {

		case Value.DECIMAL:
			BigDecimal d1 = targetValue.getDecimal();
			BigDecimal d2 = sourceValue.getDecimal();
			BigDecimal d3 = d1.divide(d2,BigDecimal.ROUND_HALF_UP);
			targetValue.setDecimal(d3);
			break;
			
		case Value.DOUBLE:
			targetValue.setDouble(targetValue.getDouble() / sourceValue.getDouble());
			break;

		case Value.INTEGER:
			targetValue.setInteger(targetValue.getInteger() / sourceValue.getInteger());
			break;

		default:
			throw new JBasicException(Status.TYPEMISMATCH);

		}
		env.push(targetValue);

		return ;
	}

}

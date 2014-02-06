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
public class OpMOD extends AbstractOpcode {

	/**
	 * Divide top two items on stack, push remainder (modulo) value back on the
	 * stack.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final Value value2 = env.pop();
		Value value1 = env.popForUpdate();

		Expression.coerceTypes(value1, value2);
		switch (value1.getType()) {

		case Value.DECIMAL:
			BigDecimal d1 = value1.getDecimal();
			BigDecimal d2 = value2.getDecimal();
			
			value1.setDecimal(d1.remainder(d2));
			break;
			
		case Value.DOUBLE:
			value1.setDouble(value1.getDouble() % value2.getDouble());
			break;

		case Value.INTEGER:
			value1.setInteger(value1.getInteger() % value2.getInteger());
			break;

		default:
			throw new JBasicException(Status.TYPEMISMATCH);

		}
		env.push(value1);
	}

}

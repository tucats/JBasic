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
public class OpMULTI extends AbstractOpcode {

	/**
	 * Multiply top two stack items, push result back on stack.  There is a
	 * special case where one is a string and the other is an integer; these
	 * are used to indicate string mutliply operations, or REPEAT() functions.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value sourceValue = new Value(env.instruction.integerOperand);
		final Value targetValue = env.popForUpdate();

		/*
		 * See if this is the special case of the REPEAT operation on
		 * a string.
		 */
		
		if( targetValue.getType() == Value.STRING ) {
			StringBuffer result = new StringBuffer();
			int count = sourceValue.getInteger();
			String repeatString = targetValue.getString();
			if( count > 0 )
				for( int ix = 0; ix < count; ix++ )
					result.append(repeatString);
			env.push(new Value(result.toString()));
			return;
		}
		/*
		 * Use a mutually agreed-upon type to do the right kind of math.
		 */
		int bestType = Expression.bestType(targetValue, sourceValue);
		targetValue.coerce(bestType);
		
		switch (bestType) {

		case Value.DECIMAL:
			BigDecimal d1 = targetValue.getDecimal();
			BigDecimal d2 = sourceValue.getDecimal();
			BigDecimal d3 = d1.multiply(d2);
			targetValue.setDecimal(d3);
			break;
			
		case Value.DOUBLE:
			targetValue.setDouble(sourceValue.getDouble() * targetValue.getDouble());
			break;

		case Value.INTEGER:
			targetValue.setInteger(sourceValue.getInteger() * targetValue.getInteger());
			break;

		case Value.BOOLEAN:
			targetValue.setBoolean(sourceValue.getBoolean() && targetValue.getBoolean());
			break;

		default:
			throw new JBasicException(Status.TYPEMISMATCH);

		}
		env.push(targetValue);

	}

}

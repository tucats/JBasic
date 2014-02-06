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
public class OpMULT extends AbstractOpcode {

	/**
	 * Multiply top two stack items, push result back on stack.  There is a
	 * special case where one is a string and the other is an integer; these
	 * are used to indicate string mutliply operations, or REPEAT() functions.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value targetValue = env.popForUpdate();
		final Value sourceValue = env.pop();

		/*
		 * See if this is the special case of the REPEAT operation on
		 * a string.
		 */
		
		if((targetValue.getType() == Value.STRING && sourceValue.getType() == Value.INTEGER) ||
		   (sourceValue.getType() == Value.STRING && targetValue.getType() == Value.INTEGER)) {
			StringBuffer result = new StringBuffer();
			String source = ( targetValue.getType() == Value.STRING)? targetValue.getString() : sourceValue.getString();
			int count = ( targetValue.getType() == Value.INTEGER) ? targetValue.getInteger() : sourceValue.getInteger();
			if( count > 0 )
				for( int ix = 0; ix < count; ix++ )
					result.append(source);
			env.push(new Value(result.toString()));
			return;
		}

		/*
		 * Use a mutually agreed-upon type to do the right kind of multiply.
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
			targetValue.setDouble( targetValue.getDouble() * sourceValue.getDouble());
			break;

		case Value.INTEGER:
			targetValue.setInteger(targetValue.getInteger() * sourceValue.getInteger());
			break;

		case Value.BOOLEAN:
			targetValue.setBoolean( targetValue.getBoolean() && sourceValue.getBoolean());
			break;

		default:
			throw new JBasicException(Status.TYPEMISMATCH);

		}
		env.push(targetValue);
	}

}

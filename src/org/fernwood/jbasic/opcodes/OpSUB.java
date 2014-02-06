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
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpSUB extends AbstractOpcode {

	/**
	 * Subtract top two stack items, push result back on stack.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value sourceValue = env.pop();
		Value targetValue = env.popForUpdate();
		
		/*
		 * You can subtract a string from a record which removes the
		 * named field if found.  
		 */
		if( targetValue.getType() == Value.RECORD && sourceValue.getType() == Value.STRING ) {
		
			targetValue.removeElement(sourceValue.getString().toUpperCase());
			env.push(targetValue);
			return;
		}
		
		
		/*
		 * Use a mutually agreed-upon type to do the right kind of math.
		 */
		int bestType = Expression.bestType(targetValue, sourceValue);
		targetValue.coerce(bestType);
		switch( bestType ) {
		
		case Value.ARRAY:

			if( sourceValue.getType() != Value.ARRAY)  {
				Value tempValue = sourceValue;
				sourceValue = new Value(Value.ARRAY, null );
				sourceValue.addElement(tempValue);
			}

			/*
			 * Array subtraction means locating value(s) in one array and
			 * removing them if found in the second array.
			 */
			int candidateLen = targetValue.size();
			final int testLen = sourceValue.size();

			for (int candidateIdx = 1; candidateIdx <= candidateLen; candidateIdx++) {
				final Value candidateValue = targetValue.getElement(candidateIdx)
						.copy();

				for (int testIdx = 1; testIdx <= testLen; testIdx++) {
					final Value testValue = sourceValue.getElement(testIdx).copy();

					final int compare = candidateValue.compare(testValue);
					//System.out.println("Compare " + candidateValue + " to "
					// + testValue + ": " + compare );

					if (compare == 0) {
						targetValue.removeArrayElement(candidateIdx);
						candidateIdx--;
						candidateLen--;
						break;
					}
				}
			}
			break;

		case Value.DECIMAL:
			targetValue.setDecimal(targetValue.getDecimal().subtract(sourceValue.getDecimal()));
			break;

		case Value.DOUBLE:
			targetValue.setDouble(targetValue.getDouble() - sourceValue.getDouble());
			break;

		case Value.INTEGER:
			targetValue.setInteger(targetValue.getInteger() - sourceValue.getInteger());
			break;

		case Value.STRING:

			int pos;

			pos = targetValue.getString().indexOf(sourceValue.getString());

			/*
			 * If the string to be subtracted doesn't exist, then just put the
			 * original value back.
			 */
			if (pos < 0)
				break;

			/*
			 * Get the part before and after the string to be removed.
			 */
			final String h1 = targetValue.getString().substring(0, pos);
			final String h2 = targetValue.getString().substring(
					pos + sourceValue.getString().length());

			targetValue = new Value(h1 + h2);
			break;

		default:
			throw new JBasicException(Status.TYPEMISMATCH);

		}
		env.push(targetValue);

	}

}

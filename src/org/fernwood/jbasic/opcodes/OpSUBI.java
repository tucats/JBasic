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
public class OpSUBI extends AbstractOpcode {

	/**
	 * Subtract top two stack items, push result back on stack. <br>
	 * <br>
	 * There is a special case for arrays. Subtracting anything from an array
	 * means locate matching values in the array and delete them. So the
	 * expression <code>[1,2,3,4] - 3</code> results in <code>[1,2,4]</code>.
	 * Note that all instances that match are deleted, so
	 * <code>[1,2,3,5,3,6] - 3</code> results in <code>[1,2,5,6]</code>,
	 * with both instances of <code>3</code> removed. The subtracted element
	 * can be a single item or an array; any members of the subtrahend are
	 * removed from the result.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value sourceValue = new Value(env.instruction.integerOperand);
		Value targetValue = env.popForUpdate();
	
		int bestType = Expression.bestType(targetValue, sourceValue);
		targetValue.coerce(bestType);

		switch (bestType) {

		case Value.ARRAY:


			if( sourceValue.getType() != Value.ARRAY)  {
				sourceValue = new Value(Value.ARRAY, null );
				Value temp  = new Value(env.instruction.integerOperand);
				sourceValue.addElement(temp);
			}

			int candidateLen = targetValue.size();
			final int testLen = sourceValue.size();
			
			for (int candidateIdx = 1; candidateIdx <= candidateLen; candidateIdx++) {
				final Value candidateValue = targetValue.getElement(candidateIdx).copy();

				for (int testIdx = 1; testIdx <= testLen; testIdx++) {
					final Value testValue = sourceValue.getElement(testIdx).copy();

					final int compare = candidateValue.compare(testValue);

					// System.out.println("Compare " + candidateValue + " to "
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

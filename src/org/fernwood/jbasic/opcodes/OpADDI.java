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
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * ADDI adds a constant value to the top of stack.
 * @author tom
 * @version version 1.0 Jun 23, 2007
 *
 */
public class OpADDI extends AbstractOpcode {

	/**
	 *  <b><code>_ADDI const</code><br><br></b>
	 * Execute the _ADDI instruction at runtime, which adds an integer
	 * constant to the top of stack.
	 * 
	 * <p><br>
	 * <b>Implicit Arguments:</b><br>
	 * <list>
	 * <li><code>stack[tos]</code> - addend</l1>
	 * </list><br><br>
	 * <b>Explicit Argument:</b><br>
	 * <list>
	 * <li><code>const</code> - addend</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 */

	public void execute(final InstructionContext env) throws JBasicException {

		final Instruction i = env.instruction;
		
		/*
		 * The source addend is always the integer argument from the
		 * instruction. Construct a new Value from that, and get the
		 * target addend as well.
		 */
		final Value sourceValue = new Value(i.integerOperand);
		Value targetValue = env.popForUpdate();
		
		Value newValue = null;

		/*
		 * If the target is an array, just add the scalar value to the 
		 * mutable target array and declare that the new value.
		 */
		if (targetValue.isType(Value.ARRAY)) {
			
			targetValue.addElement(sourceValue);
			newValue = targetValue;
		} else {
			/*
			 * Based on the agreed-upon mutual type, do the right kind of math.
			 */
			int bestType = Expression.bestType(targetValue, sourceValue);
			targetValue.coerce(bestType);
			
			switch (bestType) {

			case Value.DECIMAL:
				targetValue.setDecimal(targetValue.getDecimal().add(sourceValue.getDecimal()));
				break;

			case Value.BOOLEAN:
				targetValue.setBoolean(targetValue.getBoolean() | sourceValue.getBoolean());
				break;

			case Value.DOUBLE:
				targetValue.setDouble(targetValue.getDouble() + sourceValue.getDouble());
				break;

			case Value.INTEGER:
				targetValue.setInteger(targetValue.getInteger() + sourceValue.getInteger());
				break;

			case Value.STRING:
				targetValue = new Value(targetValue.getString() + sourceValue.getString());
				break;

			default:
				throw new JBasicException(Status.TYPEMISMATCH);

			}
			newValue = targetValue;
		}

		env.push(newValue);

		return;
	}
}

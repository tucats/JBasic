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
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * _ADD adds top two stack items.  For scalar numerics, this is an arithmetic
 * addition.  For strings, arrays, and records this results in a concatenation.
 * @author cole
 * 
 */
public class OpADD extends AbstractOpcode {

	/**
	 *  <b><code>_ADD</code><br><br></b>
	 * Execute the _ADD instruction at runtime.
	 * 
	 * <p><br>
	 * <b>Implicit Arguments:</b><br>
	 * <list>
	 * <li>&nbsp;&nbsp;<code>stack[tos]</code> - addend</l1>
	 * <li>&nbsp;&nbsp;<code>stack[tos-1]</code> - addend</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final Value sourceValue = env.pop();
		final Value targetValue = env.popForUpdate();

		env.push(addValue(sourceValue, targetValue));

		return;
	}

	/**
	 * @param sourceValue The first addend
	 * @param targetValue The second addend
	 * @return sum of source and target
	 * @throws JBasicException if the types cannot be reconciled
	 */
	public static Value addValue(final Value sourceValue, Value targetValue)
			throws JBasicException {
		Value newValue = null;
		
		/*
		 * If both values are tables, insert the values of the second table into
		 * the first.
		 */
		
		if( targetValue.isType(Value.TABLE) && sourceValue.isType(Value.TABLE)) {
			RecordStreamValue targetTable = (RecordStreamValue) targetValue;
			RecordStreamValue sourceTable = (RecordStreamValue) sourceValue;
			int count = targetTable.merge(sourceTable);
			if( count < 0 )
				throw new JBasicException(Status.TYPEMISMATCH);
			return targetTable;
		}
		/*
		 * If the values to add are both records, then add all members of the
		 * two records together to create a new record.
		 */
		if (targetValue.isType(Value.RECORD)) {

			if (!sourceValue.isType(Value.RECORD))
				throw new JBasicException(Status.NOTRECORD, sourceValue.toString());
			/* The target is already a copy, so just add the source */
			targetValue.addElementRecord(sourceValue.copy());
			newValue = targetValue;
		} 
		else if( targetValue.getType() == Value.TABLE && sourceValue.isType(Value.ARRAY)) {
			targetValue.setElement( sourceValue, targetValue.size()+1);
			newValue = targetValue;
		}
		/*
		 * If the target is an array, add all elements of the source (array or
		 * scalar) to the target array.
		 */
		else if (targetValue.isType(Value.TABLE) & sourceValue.isType(Value.RECORD)) {
			RecordStreamValue table = (RecordStreamValue)targetValue;
			table.addElement(sourceValue);
			newValue = table;
		} else if (targetValue.isType(Value.ARRAY)) {
			newValue = targetValue;
			newValue.addElementShallow(sourceValue.copy());
		} else {
			
			/*
			 * Based on the agreed-upon type, do the right kind of ADD.  First
			 * find out what the best type is, and then make sure the target
			 * is already set to that type.
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
		return newValue;
	}
}

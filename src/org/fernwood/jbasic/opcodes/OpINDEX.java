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
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpINDEX extends AbstractOpcode {

	/**
	 * Dereference an arrayValue element.
	 * <p>
	 * If the integer operand is valid, it contains the constant value to use as
	 * the index.  Otherwise, the constant value must be the top stack item.
	 * <p>
	 * If the string parameter is valid, it contains the name of the arrayValue
	 * to index. If it is not valid, then the second stack item must contain the
	 * array reference.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value elementValue = null;
		Value indexValue = null;
		Value arrayValue = null;

		if( env.instruction.integerValid)
			indexValue = new Value(env.instruction.integerOperand);
		else
			indexValue = env.popForUpdate(); /* index */

		if (env.instruction.stringValid) {
			arrayValue = env.localSymbols.reference(env.instruction.stringOperand);
			env.codeStream.refPrimary(env.instruction.stringOperand, false);
		} else
			arrayValue = env.pop(); /* Array */

		/*
		 * If it's a table then we have special processing to do. Return
		 * an array containing all members of the table from the given
		 * column.
		 */
		if( arrayValue.getType() == Value.TABLE) {
			RecordStreamValue t = (RecordStreamValue) arrayValue;
			
			if( indexValue.getType() == Value.INTEGER) {
				
			    int ix = indexValue.getInteger();
				Value r = t.getElementAsArray(ix);
				if( r == null )
					throw new JBasicException(Status.ARRAYBOUNDS, ix);
				env.push(r);
				return;
			}
			final String memberName = indexValue.getString().toUpperCase();
			int pos = t.getColumnNumber(memberName); 
			if( pos < 1 ) {
				throw new JBasicException(Status.NOMEMBER, memberName);
			}
			Value result = new Value(Value.ARRAY, null);
			for( int idx = 1; idx <= t.size(); idx++ ) {
				Value row = t.getElementAsArray(idx);
				result.addElement(row.getElement(pos));
			}
			env.push(result);
			return;
		}
		
		/*
		 * If the array is really a record, then the index value is really a field
		 * name and we must extract the field name.
		 */
		if (arrayValue.isType(Value.RECORD)) {
			final String key = indexValue.getString().toUpperCase();
			elementValue = arrayValue.getElement(key);
			env.codeStream.refSecondary("." + key );
			if (elementValue == null)
				throw new JBasicException(Status.NOMEMBER, key);
		} else {
		
		/*
		 * The array is really just an array, so we're going to extract one or
		 * more rows from the array as the result.
		 */
			
			/*
			 * First, if the index value it itself an array, then we are creating
			 * a subset array.
			 */
			
			if( indexValue.isType(Value.ARRAY) && indexValue.size() > 0) {
				int arrayType = indexValue.getElement(1).getType();
				for( int ix = 2; ix <= indexValue.size(); ix++) {
					if( indexValue.getElement(ix).getType() != arrayType ) {
						throw new JBasicException(Status.TYPEMISMATCH);
					}
				}
				
				/*
				 * If it's an array of integers, use that as a list of subscripts
				 * and get each of those elements from the source array to populate
				 * the new array.
				 */
				if( arrayType == Value.INTEGER ) {
					elementValue = new Value(Value.ARRAY, null);
					for( int ix = 1; ix <= indexValue.size(); ix++ ) {
						int elementIndex = indexValue.getInteger(ix);
						if( elementIndex < 1 || elementIndex > arrayValue.size()) 
							throw new JBasicException(Status.ARRAYBOUNDS, elementIndex);
						elementValue.addElement(arrayValue.getElement(elementIndex));
					}
					env.push(elementValue);
					return;
				}
				
				/*
				 * If it's an array of booleans, then use that as a selector/indicator
				 * list and use the flag to choose or exclude relevant members of the
				 * source array.
				 */
				else if( arrayType == Value.BOOLEAN) {
					elementValue = new Value(Value.ARRAY, null);
					int maxIndex = indexValue.size();
					if( maxIndex > arrayValue.size())
						maxIndex = arrayValue.size();
					for( int ix = 1; ix <= maxIndex; ix++ ) {
						boolean elementSelector = indexValue.getElement(ix).getBoolean();
						if( elementSelector )
							elementValue.addElement(arrayValue.getElement(ix));
					}
					env.push(elementValue);
					return;

				}
				else 
					throw new JBasicException(Status.TYPEMISMATCH);
			}
			/*
			 * No, the index is a single value so let's just fetch that single 
			 * element from the array.
			 */
			indexValue.coerce(Value.INTEGER);

			if (arrayValue.isType(Value.ARRAY)) {
				final int ix = indexValue.getInteger();
				if ((ix < 1) | (ix > arrayValue.size()))
					throw new JBasicException(Status.ARRAYBOUNDS, Integer.toString(ix));
				elementValue = arrayValue.getElement(ix);
				env.codeStream.refSecondary("[" + ix + "]");
			}
			else
				throw new JBasicException(Status.INVCVT, "ARRAY");
		}
		if (elementValue == null) {
			Value temp = arrayValue.copy();
			arrayValue.set(new Value(Value.ARRAY, null));
			arrayValue.setElement(temp, 1);
			int ix = indexValue.getInteger();
			if( ix > 1 )
				for( int i = 2; i <= ix; i++ )
					arrayValue.setElement(new Value(0), i);				
			elementValue = arrayValue.getElement(ix);
		}

		env.push(elementValue);

		return;
	}

}

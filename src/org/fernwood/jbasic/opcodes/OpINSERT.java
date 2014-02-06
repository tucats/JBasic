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
 * _INSERT a row to a TABLE.  Any other data types throw an error.
 * 
 */
public class OpINSERT extends AbstractOpcode {

	/**
	 *  <b><code>_INSERT</code><br><br></b>
	 * Execute the _INSERT instruction at runtime.
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
		final Value targetValue = env.pop();
		RecordStreamValue table = null;
		
		/*
		 * If both values are tables, insert the values of the second table into
		 * the first.
		 */
		
		if( targetValue.isType(Value.TABLE) && sourceValue.isType(Value.TABLE)) {
			RecordStreamValue targetTable = (RecordStreamValue) targetValue;
			RecordStreamValue sourceTable = (RecordStreamValue) sourceValue;
			int count = targetTable.merge(sourceTable);
			targetTable.dirty(true);
			if( count < 0 )
				throw new JBasicException(Status.TYPEMISMATCH);
			return;
		}
		if( targetValue.getType() == Value.TABLE && sourceValue.isType(Value.ARRAY)) {
			targetValue.setElement( sourceValue, targetValue.size()+1);
			table = (RecordStreamValue)targetValue;
			table.dirty(true);
		}
		else
			if( targetValue.getType() == Value.TABLE && sourceValue.isType(Value.RECORD)) {
				Value row = new Value(Value.ARRAY, null);
				Value map = table.columnNames();
				for( int ix = 1; ix <= map.size(); ix++ ) {
					String name = map.getElement(ix).getString();
					name = name.substring(0,name.indexOf('@'));
					row.setElement(sourceValue.getElement(name), ix);
				}
				table.dirty(true);
				targetValue.setElement( row, targetValue.size()+1);
			}

		/*
		 * If the target is an array, add all elements of the source (array or
		 * scalar) to the target array.
		 */
		else if (targetValue.isType(Value.TABLE) & sourceValue.isType(Value.RECORD)) {
			table = (RecordStreamValue)targetValue;
			table.dirty(true);
			table.addElement(sourceValue);
		}
		else
			throw new JBasicException(Status.TYPEMISMATCH);

	}
}

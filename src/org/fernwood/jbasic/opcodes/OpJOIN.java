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

import java.util.ArrayList;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * Join two tables using an ORDER BY expression captured as a sequence
 * of instructions following the _JOIN instruction.
 * @author cole
 * 
 */
public class OpJOIN extends AbstractOpcode {

	static String LEFT_INDEX_NAME = "_LEFT_INDEX_";
	static String RIGHT_INDEX_NAME = "_RIGHT_INDEX_";
	
	/**
	 * Execute the _JOIN operation. The integer argument indicates the number
	 * of byte codes to skip over in the program stream, which have already
	 * been gathered up during link time.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final int count = env.instruction.integerOperand;
		if( count < 0 )
			throw new JBasicException(Status.FAULT, 
					new Status(Status.INVOPARG, count));

		/*
		 * Get the LEFT argument from the stack. It must be a Value.TABLE
		 * item or there's an error.  Convert it to the correct class
		 * so we can manipulate the value more directly.  Also, be sure
		 * there is at least one row and one column in the table.
		 */
		Value tableValue = env.pop();
		if( !tableValue.isType(Value.TABLE))
			throw new JBasicException(Status.INVTABLE, "left table");

		RecordStreamValue leftTable = (RecordStreamValue) tableValue;

		if( leftTable.size() < 1 || leftTable.rowSize() < 1 )
			throw new JBasicException(Status.INVTABLE, "left table");

		/*
		 * Get the RIGHT argument from the stack. It must be a Value.TABLE
		 * item or there's an error.  Convert it to the correct class
		 * so we can manipulate the value more directly.  Also, be sure
		 * there is at least one row and one column in the table.
		 */
		tableValue = env.pop();
		if( !tableValue.isType(Value.TABLE))
			throw new JBasicException(Status.INVTABLE, "right table");

		RecordStreamValue rightTable = (RecordStreamValue) tableValue;

		if( rightTable.size() < 1 || rightTable.rowSize() < 1 )
			throw new JBasicException(Status.INVTABLE, "right table");

		/*
		 * Prepare the output result stream, using the column metadata
		 * from the left and right tables.  The merged column definitions
		 * represent the unique values from each contibuting table.
		 */

		Value leftColumnNames = leftTable.columnNames();
		Value rightColumnNames = rightTable.columnNames();
		Value allColumnNames = new Value(Value.ARRAY, null);
		allColumnNames.addElementShallow(leftColumnNames);
		for( int idx = 1; idx <= rightColumnNames.size(); idx++ ) {
			Value v = rightColumnNames.getElement(idx);
			if( !allColumnNames.contains(v))
				allColumnNames.addElement(v);
		}
		
		RecordStreamValue result = new RecordStreamValue(allColumnNames);

		/*
		 * Set up the execution area where we will run the short
		 * bytecode sequence that describes the selection clause.
		 * Copy the bytes from the original instruction to the new
		 * area.
		 */

		ByteCode joinCompareStream = new ByteCode(env.session);
		joinCompareStream.statement = env.codeStream.statement;

		int pc = env.codeStream.programCounter;
		for( int idx = 0; idx < count; idx++ )
			joinCompareStream.add(env.codeStream.getInstruction(pc+idx));		

		/*
		 * Create the symbol table for use by the join execution stream.  Go
		 * ahead and set up symbols for the index pointers.
		 */
		SymbolTable joinSymbols = new SymbolTable(env.session, "Local to JOIN clause", env.localSymbols);
		joinSymbols.insert(LEFT_INDEX_NAME, 0);
		Value leftRowIndex = joinSymbols.findReference(LEFT_INDEX_NAME, false);
		joinSymbols.insert(RIGHT_INDEX_NAME, 0);
		Value rightRowIndex = joinSymbols.findReference(RIGHT_INDEX_NAME, false);

		Status status = null;

		/*
		 * Step one, count the member names and make an array with the member
		 * names for the Left table.
		 */
		int leftMemCount = leftColumnNames.size();

		String leftMemNames[] = new String[leftMemCount];
		for( int idx = 1; idx <= leftMemCount; idx++) {
			String name = leftColumnNames.getString(idx);
			int atPos = name.indexOf('@');
			if( atPos > 1 )
				name = name.substring(0,atPos);
			leftMemNames[idx-1] = name;
		}

		/*
		 * Step two, repeat for the right table.
		 */
		int rightMemCount = leftColumnNames.size();

		String rightMemNames[] = new String[rightMemCount];
		for( int idx = 1; idx <= rightMemCount; idx++) {
			String name = rightColumnNames.getString(idx);
			int atPos = name.indexOf('@');
			if( atPos > 1 )
				name = name.substring(0,atPos);
			rightMemNames[idx-1] = name;
		}
	
		/*
		 * Step three, make a list of the column names that
		 * are unique to the left or right table; these names
		 * will be available as unqualified names in the
		 * JOIN evaluation code's symbol table.
		 */
		
		ArrayList<String> uniqueLeftNames = new ArrayList<String>();
		for( String left : leftMemNames) {
			boolean found = false;
			for( String right : rightMemNames)
				if( right.equals(left)) {
					found = true;
					break;
				}
			if( !found )
				uniqueLeftNames.add(left);
		}

		ArrayList<String> uniqueRightNames = new ArrayList<String>();
		for( String right : rightMemNames) {
			boolean found = false;
			for( String left : leftMemNames)
				if( right.equals(left)) {
					found = true;
					break;
				}
			if( !found )
				uniqueRightNames.add(right);
		}
		

		boolean fDebugSymbols = env.localSymbols.getBoolean("SYS$SQL_DBGSYMS");
		
		/*
		 * Now step over each row in the input array. For each
		 * row, copy the column data into the symbol table for the
		 * join clause.
		 */
		for( int leftIdx = 0; leftIdx < leftTable.size(); leftIdx++ ) {
			Value leftRow = leftTable.getElement(leftIdx+1);

			for( int memberIdx = 0; memberIdx < uniqueLeftNames.size(); memberIdx++) {
				String key = uniqueLeftNames.get(memberIdx);
				Value v = leftRow.getElement(key);
				joinSymbols.insertLocal(key, v);
			}
			joinSymbols.insertLocal("LEFT", leftRow);
			if( leftTable.getName() != null)
				joinSymbols.insertLocal(leftTable.getName(), leftRow);

			/*
			 * Set the value of the _INDEX_ pseudo variable that is
			 * also always available when the code stream runs, and then
			 * execute the code.
			 */
			leftRowIndex.setInteger(leftIdx+1);

			/*
			 * Now match each of the rows of the right table against 
			 * the current row of the left table.
			 */

			for( int rightIdx = 0; rightIdx < rightTable.size(); rightIdx++) {
				Value rightRow = rightTable.getElement(rightIdx+1);
				rightRowIndex.setInteger(rightIdx+1);

				for( int memberIdx = 0; memberIdx < uniqueRightNames.size(); memberIdx++) {
					String key = uniqueRightNames.get(memberIdx);
					Value v = rightRow.getElement(key);
					joinSymbols.insertLocal(key, v);
				}
				joinSymbols.insertLocal("RIGHT", rightRow);
				if( rightTable.getName() != null )
					joinSymbols.insertLocal(rightTable.getName(), rightRow);
				
				if( fDebugSymbols ) {
					//fDebugSymbols = false;
					joinSymbols.dumpTable(env.session, false);
				}
				status = joinCompareStream.run(joinSymbols, 0);

				/*
				 * If the stream had an error, throw it now.  Otherwise,
				 * determine if the boolean result tells us to include the
				 * current row in the output result.
				 */
				if( status.failed())
					throw new JBasicException(status);

				Value include = joinCompareStream.getResult();

				/*
				 * Construct a new row for the output table.
				 */
				if( include != null && include.getBoolean()) {

					Value newRow = new Value(Value.RECORD, null);
					ArrayList<String> members = leftRow.recordFieldNames();

					for( int ix = 1; ix <= leftRow.memberCount(); ix++ ) {
						String memberName = members.get(ix-1);
						newRow.setElement(leftRow.getElement(memberName), memberName);
					}

					members = rightRow.recordFieldNames();
					for( int ix = 1; ix <= rightRow.memberCount(); ix++ ) {
						String memberName = members.get(ix-1);
						newRow.setElement(rightRow.getElement(memberName), memberName);
					}
					result.addElement(newRow);
				}
			}

		}

		result.dirty(true);
		env.codeStream.programCounter += count;
		env.push(result);

		return;
	}

}

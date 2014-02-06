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
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpWHERE extends AbstractOpcode {

	static String WHERE_INDEX_NAME = "_INDEX_";
	/**
	 * Execute the _WHERE operand. The integer argument indicates the number
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
		 * Get the argument from the stack. It must be a Value.TABLE
		 * item or there's an error.  Convert it to the correct class
		 * so we can manipulate the value more directly.  Also, be sure
		 * there is at least one row and one column in the table.
		 */
		Value arrayArg = env.pop();
		if( !arrayArg.isType(Value.TABLE))
			throw new JBasicException(Status.INVTABLE);

		RecordStreamValue array = (RecordStreamValue) arrayArg;

		if( array.size() < 1 || array.rowSize() < 1 )
			throw new JBasicException(Status.INVTABLE, 1);

		/*
		 * Prepare the output result stream, using the column metadata
		 * from the source array.
		 */

		Value columnNames = array.columnNames();
		RecordStreamValue result = new RecordStreamValue(columnNames);

		/*
		 * Set up the execution area where we will run the short
		 * bytecode sequence that describes the selection clause.
		 * Copy the bytes from the original instruction to the new
		 * area.
		 * 
		 * Note this is only done when there is a non-empty WHERE
		 * clause.  The use of an empty WHERE clause indicates that
		 * the table is going to selection no rows. This can happen
		 * in a SQL statement for CREATE TABLE...LIKE... which builds
		 * a new table patterned off of an old table but doesn't copy
		 * any of the rows.
		 */

		if( count > 0 ) {
			ByteCode whereStream = new ByteCode(env.session);
			whereStream.statement = env.codeStream.statement;

			boolean fDebugSymbols = env.localSymbols.getBoolean("SYS$SQL_DBGSYMS");

			int pc = env.codeStream.programCounter;
			for( int idx = 0; idx < count; idx++ )
				whereStream.add(env.codeStream.getInstruction(pc+idx));		

			SymbolTable whereTable = new SymbolTable(env.session, "Local to WHERE clause", env.localSymbols);
			whereTable.insert(WHERE_INDEX_NAME, 0);
			Value whereIndex = whereTable.findReference(WHERE_INDEX_NAME, false);

			Status status = null;

			/*
			 * Step one, count the member names.
			 */
			int memberCount = columnNames.size();

			/*
			 * Step two, make an array with the member names without the type 
			 * metadata.
			 */
			String memberNames[] = new String[memberCount];
			for( int idx = 1; idx <= memberCount; idx++) {
				String name = columnNames.getString(idx);
				int atPos = name.indexOf('@');
				if( atPos > 1 )
					name = name.substring(0,atPos);
				memberNames[idx-1] = name;
			}

			Value element = null;

			/*
			 * Now step over each row in the input array. For each
			 * row, copy the column data into the symbol table for the
			 * where clause and execute it.
			 */
			for( int idx = 0; idx < array.size(); idx++ ) {
				element = array.getElementAsArray(idx+1);

				for( int memberIdx = 0; memberIdx < memberNames.length; memberIdx++) {
					String key = memberNames[memberIdx];
					int at = key.indexOf("@");
					if( at >=0  )
						key = key.substring(0,at);
					Value v = element.getElement(memberIdx+1);
					whereTable.insertLocal(key, v);
				}

				/*
				 * Set the value of the _INDEX_ pseudo variable that is
				 * also always available when the code stream runs, and then
				 * execute the code.
				 */
				whereIndex.setInteger(idx+1);

				if( fDebugSymbols )
					whereTable.dumpTable(env.session, true);
				
				status = whereStream.run(whereTable, 0);

				/*
				 * If the stream had an error, throw it now.  Otherwise,
				 * determine if the boolean result tells us to include the
				 * current row in the output result.
				 */
				if( status.failed())
					throw new JBasicException(status);

				Value include = whereStream.getResult();

				if( include != null && include.getBoolean())
					result.addElement(element);
			}
		}

		env.codeStream.programCounter += count;
		
		if( array.getName() !=null )
			result.setName(array.getName());
		
		env.push(result);

		return;
	}

}

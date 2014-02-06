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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpLOADR extends AbstractOpcode {

	/**
	 * Load a record member on the stack.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final String memberName = env.pop().getString(); /* Get member name */
		
		Value theRecord = null;
		String recordName = null;
		if (env.instruction.stringValid) {
			recordName = env.instruction.stringOperand;
			theRecord = env.localSymbols.reference(recordName);
			env.codeStream.refPrimary(recordName, false);
		} else {
			theRecord = env.pop(); /* Get record value from stack */
			recordName = theRecord.toString();
		}

		if (theRecord == null)
			throw new JBasicException(Status.NOTRECORD, recordName);

		/*
		 * This is not great design... when a SELECT clause is actively executing it will
		 * set a local hidden variable called __SQL_SELECT_ACTIVE to true.  When this is
		 * the case, we do extra work to interpret a member read from a record that is really
		 * a DATABASE file handle as an attempt to read the member from that file.  If the
		 * __SQL_SELECT_ACTIVE flag is not on, then we just do a regular member read...
		 */
		String whereClause = null;
		
		Value selectActive = env.localSymbols.findReference("__SQL_SELECT_ACTIVE", false);
		boolean fSelect = false;
		if( selectActive != null )
			fSelect = selectActive.getBoolean();
		
		if( fSelect && theRecord.getType() == Value.RECORD) {
			
			/*
			 * Verify that it's an actual file reference
			 */
			boolean valid = true;
			
			JBasicFile f = null;
			try {
				f = env.session.openUserFiles.get(theRecord.getElement("SEQNO").getInteger());
			} catch(Exception e ) {
				valid = false;
			}
			if( f == null )
				valid = false;
			else
				if( f.getMode() != JBasicFile.MODE_DATABASE)
					valid = false;
			
			if( valid ) {
				/* Peek past the "DISABLE SQL" flag and see if there is a WHERE clause we need to steal. */
				Instruction nextInst = env.codeStream.getInstruction(env.codeStream.programCounter);
				if( nextInst.opCode == ByteCode._WHERE) {
					int whereSize = nextInst.integerOperand;
					if( whereSize == 0 ) 
						whereClause = " WHERE 0=1";
					else {
						whereClause = " WHERE " + nextInst.stringOperand;
						env.codeStream.programCounter += (whereSize + 1);
					}
				}
				else
					whereClause = "";
				
			}
			/*
			 * If this is a valid DATABASE file reference, then we really don't want to do a 
			 * record dereference at all; we want to load the database fetch buffer as a table.
			 */
			if( valid ) {
				
				/*
				 * We are doing a catalog.table read of a database, turn off the "SELECT ACTIVE" 
				 * flag as it is a one-shot flag set just before the select clause execution.
				 * 
				 */
				env.localSymbols.delete("__SQL_SELECT_ACTIVE");

				
				/*
				 * We're going to generate a new code stream to do the read of the results
				 * into a table we can push on the stack.
				 */
				
				ByteCode select = new ByteCode(env.session);
				
				/*
				 * Generate code to create the query and register the result set map
				 */
				select.add(ByteCode._LOADFREF, 0, recordName);
				select.add(ByteCode._OUTNL, 1, "SELECT * FROM " + memberName + whereClause);
				
				
				/*
				 * Generate code to create a table using the map, which is left on the
				 * top of the stack
				 */
				select.add(ByteCode._STRING, "MAP");
				select.add(ByteCode._LOADR, recordName);
				select.add(ByteCode._TABLE);
				String tableName = "__GEN_TABLE_" + Integer.toString(JBasic.getUniqueID());
				select.add(ByteCode._STOR, tableName);
				
				/*
				 * Generate code to read the data from the database
				 */
		
				int top = select.size();
				select.add(ByteCode._EOF, recordName);
				int fwd = select.size();
				select.add(ByteCode._BRNZ, 0);
				select.add(ByteCode._LOADREF, tableName);
				select.add(ByteCode._LOADFREF, recordName);
				select.add(ByteCode._GET, 2);
				select.add(ByteCode._INSERT);
				select.add(ByteCode._BR, top);
				Instruction patch = select.getInstruction(fwd);
				patch.integerOperand = select.size();
				select.add(ByteCode._LOADREF, tableName);
				
				/*
				 * Run the resulting code.  If there is an error, complain.  If not, then put
				 * the result on the stack and we're done.
				 */
				
				Status status = select.run(env.localSymbols, 0);
				if( status.failed())
					throw new JBasicException(Status.SQL, status);
				env.push(select.getResult());
				return;

				//throw new JBasicException(Status.FAULT, 
					//	"Attempt to read a database file; SELECT * FROM " + memberName + whereClause);
			}
		}
		/*
		 * If it's a table then we have special processing to do. Return
		 * an array containing all members of the table from the given
		 * column.
		 */
		if( theRecord.getType() == Value.TABLE) {
			RecordStreamValue t = (RecordStreamValue) theRecord;
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
		 * Plain old record dereference; this must be a RECORD type or
		 * there's a problem.
		 */
		if (theRecord.getType() != Value.RECORD)
			throw new JBasicException(Status.NOTRECORD, recordName);

		/*
		 * Get the record element from the record, and return that value.
		 */
		final Value recordElement = theRecord.getElement(memberName);
		if (recordElement == null)
			throw new JBasicException(Status.NOMEMBER, memberName);
		env.push(recordElement);
		env.codeStream.refSecondary("." + memberName);

		return;
	}

}

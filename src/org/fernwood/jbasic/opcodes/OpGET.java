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
import org.fernwood.jbasic.runtime.BitFieldMap;
import org.fernwood.jbasic.runtime.JBFBinary;
import org.fernwood.jbasic.runtime.JBFDatabase;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpGET extends AbstractOpcode {


	/**
	 * Get a record from a BINARY file. Top of stack is record definition array,
	 * second on stack is file identifier.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		int n;
		int mode;

		/*
		 * Get the stack information. The mode switch in the instruction tells
		 * us what order to get things from the stack.
		 */

		mode = env.instruction.integerOperand;
		Value fieldList = null;

		if (mode < 2)
			fieldList = env.pop();

		final Value fileID = env.pop();

		final JBasicFile tempf = JBasicFile.lookup(env.session, fileID);
		JBFBinary inFile = null;
		JBFDatabase db = null;

		if (tempf == null)
			throw new JBasicException(Status.FNOPENOUTPUT, fileID.toString());

		final int fileMode = tempf.getMode();
		boolean isBinary = false;

		if (fileMode == JBasicFile.MODE_BINARY) {
			inFile = (JBFBinary) tempf;
			isBinary = true;
		} else if (fileMode == JBasicFile.MODE_DATABASE)
			db = (JBFDatabase) tempf;
		else
			throw new JBasicException(Status.NOTBINARY);

		/*
		 * If we are a database, we need to advance to the next result set.
		 */

		if (!isBinary)
			if (!db.nextResult()) {
				Status dbStatus = db.getStatus();
				if( !dbStatus.success())
					throw new JBasicException(dbStatus);
				return;
			}
		/*
		 * See what kind of mode we are in.
		 * 
		 * 0 - write to describe variables 1 - write to a field, using explicit
		 * field definition 2 - write to a field, using implicit result set
		 * definition
		 */
		if (env.instruction.integerValid)
			mode = env.instruction.integerOperand;
		else
			mode = 0;

		Value destination = null;
		if ((mode == 1) || (mode == 2))
			destination = new Value(Value.RECORD, null);

		/*
		 * Get the definition array. Let's make sure it's valid; it must be an
		 * array and each element must be a record.
		 */

		if (mode == 2) {
			if (isBinary)
				throw new JBasicException(Status.IOERROR,
				"implicit records invalid for BINARY file");
			fieldList = db.getFieldList();
		}
		else
			if( mode == 4) {
				fieldList = fileID.getElement("FIELD");
				JBasic.log.debug("FIELD spec: " + fileID);
				mode = 0;
			}

		/*
		 * Validate that the field list is an array of records.
		 */
		if (fieldList.getType() != Value.ARRAY)
			throw new JBasicException(Status.INVRECDEF, fieldList.toString());

		final int fCount = fieldList.size();
		if (fCount < 1)
			throw new JBasicException(Status.INVRECDEF, fieldList.toString());

		Value fieldRecord = null;
		for (n = 1; n <= fCount; n++) {
			fieldRecord = fieldList.getElement(n);
			if (fieldRecord.getType() != Value.RECORD)
				throw new JBasicException(Status.INVRECDEF, fieldList.toString());
		}

		/*
		 * Loop over each item in the field list and get the datum from the
		 * input file stream, wherever it is currently positioned.
		 */

		for (n = 1; n <= fCount; n++) {
			fieldRecord = fieldList.getElement(n);
			final Value fieldName = fieldRecord.getElement("NAME");
			if (fieldName == null)
				throw new JBasicException(Status.INVRECDEF, "missing NAME");

			final Value fieldType = fieldRecord.getElement("TYPE");
			if (fieldType == null)
				throw new JBasicException(Status.INVRECDEF, "missing TYPE");
			final String typeString = fieldType.getString().toUpperCase();
			int size = 0;
			int kind = Value.UNDEFINED;
			Value datum = null;

			if (isBinary & typeString.equals("VARYING")) {
				Value fieldSize = fieldRecord.getElement("SIZE");
				if( fieldSize == null )
					fieldSize = new Value(256);

				size = fieldSize.getInteger();
				int actualSize = inFile.getInteger(4).getInteger();
				if( actualSize > size )
					actualSize = size;
				datum = inFile.getString(size);
				if( actualSize < size )
					datum = new Value(datum.getString().substring(0, actualSize));
				kind = Value.STRING;
				if( datum == null )
					throw new JBasicException(Status.EOF);
			}
			
			else if( typeString.equals("BITFIELD")) {
				/*
				 * Is this a BitFieldMap type?
				 * 
				 * for the fieldRecord, TYPE:"FIELD".  In this case,
				 * there must be an item MAP:[] which is an array
				 * of records describing each field.
				 * 
				 * { FIELD_TYPE: "STRING", "DOUBLE", or "INTEGER"
				 *   FIELD_POS: starting-bit-position
				 *   FIELD_LEN: bit length }
				 */

				BitFieldMap bm =  BitFieldMap.buildBitMap(fieldRecord);
				if( bm == null )
					throw new JBasicException(Status.FAULT, "failure to build BitFieldMap");
				
				/*
				 * Now we have a map of what to read. Read in a byte buffer
				 * of the correct size.
				 */
				String[] nameArray = bm.getNames();
				int[] typeArray = bm.getTypes();

				int bufferLen = bm.getBufferSize();
				int mapCount = nameArray.length;
				
				byte[] buffer = inFile.getBytes(bufferLen);
				if( buffer == null || buffer.length < bufferLen)
					throw new JBasicException(Status.EOF);
				
				for( int ix = 0; ix < mapCount; ix++ ) {
					switch( typeArray[ix]) {
					
					case Value.INTEGER:
						env.localSymbols.insert(nameArray[ix], bm.getInt(buffer, ix));
						break;
						
					case Value.DOUBLE:
						env.localSymbols.insert(nameArray[ix], bm.getDouble(buffer, ix));
						break;
						
					case Value.STRING:
						env.localSymbols.insert(nameArray[ix], bm.getString(buffer, ix));
						break;
						
					}
				}
				/*
				 * All the work is done at this point, so let the field loop
				 * run again without getting to the bottom of this code, which
				 * cannot handle FIELD anyway.
				 */
				continue;
				
			}
			else if (typeString.equals("STRING")) {
				Value fieldSize = fieldRecord.getElement("SIZE");

				if (isBinary) {
					/* 
					 * If the field size isn't given, it's a varying string with a 
					 * integer length first.  Get that value.
					 */
					if (fieldSize == null) {
						if( inFile.defaultStringSize == null )
							fieldSize = inFile.getInteger(4);
						else
							fieldSize = inFile.defaultStringSize;
					}
					/*
					 * Read a string of the appropriate size from the file.
					 */
					size = fieldSize.getInteger();
					datum = inFile.getString(size);

				} else
					datum = db.getString(fieldName.getString());
				kind = Value.STRING;
				if( datum == null )
					throw new JBasicException(Status.EOF);
			} 
			else if ( isBinary & typeString.equals("UNICODE")) {
				Value fieldSize = fieldRecord.getElement("SIZE");

				/* 
				 * If the field size isn't given, it's a varying string with a 
				 * integer length first.  Get that value.
				 */
				if (fieldSize == null) {
					if( inFile.defaultStringSize == null )
						fieldSize = inFile.getInteger(4);
					else
						fieldSize = inFile.defaultStringSize;
				}
				/*
				 * Read a string of the appropriate size from the file.
				 */
				size = fieldSize.getInteger();
				datum = inFile.getUnicode(size);

				kind = Value.STRING;
				if( datum == null )
					throw new JBasicException(Status.EOF);
			} else if (typeString.equals("INTEGER")) {
				kind = Value.INTEGER;
				Value fieldSize = fieldRecord.getElement("SIZE");
				if( fieldSize == null)
					size = 4;
				else
					size = fieldSize.getInteger();
				if( isBinary ) 
					datum = inFile.getInteger(size);
				else {
					if( size != 4 )
						throw new JBasicException(Status.INVRECDEF, "invalid INTEGER size " + size);
					datum = db.getInteger(fieldName.getString());
				}
			} else if (isBinary & typeString.equals("BYTE")) {
				size = 1;
				kind = Value.INTEGER;
				datum = inFile.getInteger(1);
			} else if (isBinary & typeString.equals("WORD")) {
				size = 2;
				kind = Value.INTEGER;
				datum = inFile.getInteger(2);
			} else if (typeString.equals("DOUBLE")) {
				size = 8;
				kind = Value.DOUBLE;
				if (isBinary)
					datum = inFile.getDouble();
				else
					datum = db.getDouble(fieldName.getString());
			} else if ( isBinary & typeString.equals("FLOAT")) {
				kind = Value.DOUBLE;
				Value fieldSize = fieldRecord.getElement("SIZE");
				if( fieldSize == null)
					size = 4;
				else
					size = fieldSize.getInteger();
				switch(size) {
				case 4:	datum = inFile.getFloat();
						break;
				case 8: datum = inFile.getDouble();
						break;
				default:
						throw new JBasicException(Status.INVRECDEF, "invalid FLOAT size " + size);
				}
				
			} else if (typeString.equals("BOOLEAN")) {
				size = 1;
				kind = Value.BOOLEAN;
				if (isBinary)
					datum = inFile.getBoolean();
				else
					datum = db.getBoolean(fieldName.getString());

			}

			if (kind == Value.UNDEFINED)
				throw new JBasicException(Status.INVRECDEF, "unknown type "
						+ typeString);

			/*
			 * If we never got a data item on this iteration, then there was
			 * some kind of error, probably in the database read. If we're a
			 * database GET, then report the error.
			 */

			if (datum == null) {
				if (!isBinary) {
					Status dbStatus = db.getStatus();
					if( !dbStatus.success())
						throw new JBasicException(dbStatus);
				}
				return;
			}

			/*
			 * If the mode is zero, we are writing directly to scalar values. If
			 * the mode is 1, then we are assembling a record on the stack.
			 */

			final String targetName = fieldName.getString().toUpperCase();
			if (mode == 0)
				env.localSymbols.insertLocal(targetName, datum);
			else
				destination.setElement(datum, targetName);

		}
		if (mode > 0)
			env.push(destination);

		return;
	}


}

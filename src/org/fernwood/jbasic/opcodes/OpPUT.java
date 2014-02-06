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
import org.fernwood.jbasic.runtime.BitFieldMap;
import org.fernwood.jbasic.runtime.JBFBinary;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpPUT extends AbstractOpcode {

	/**
	 * Write a record to a BINARY file. Top of stack is record definition array,
	 * second on stack is file identifier.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		JBFBinary randomFile = null;
		int n;

		/*
		 * The flag on the instruction indicates if we are writing a record on
		 * the stack or fetching scalar values by name using the record
		 * definition.
		 */
		boolean fromRecord = false;
		if (env.instruction.integerValid)
			fromRecord = (env.instruction.integerOperand == 1);

		/*
		 * If there was a FROM clause that named an expression to read from,
		 * then we use that value (which was left on the stack for us) as the
		 * source of data for the write. Otherwise, we'll be fetching values
		 * directly from scalars.
		 */
		Value source = null;
		if (fromRecord)
			source = env.pop();

		/*
		 * Get the definition array, if it's on the stack.
		 */
		
		Value fieldList = null;
		if( env.instruction.integerOperand  != 4 )
			fieldList = env.pop();

		/*
		 * Get the file identifier.
		 */

		final Value fileID = env.pop();
		randomFile = (JBFBinary) JBasicFile.lookup(env.session, fileID);
		if (randomFile == null)
			throw new JBasicException(Status.FNOPENOUTPUT, fileID.toString());

		if (randomFile.getMode() != JBasicFile.MODE_BINARY)
			throw new JBasicException(Status.NOTBINARY);

		/*
		 * If it was a FIELD reference, get the field value now.
		 */
		if( fieldList == null & env.instruction.integerOperand == 4 )
			fieldList = fileID.getElement("FIELD");
		
		/*
		 * Validate the record array that defines the fields to PUT.
		 */
		if( fieldList == null)
			throw new JBasicException(Status.INVRECDEF, new Status(Status.EXPREC));
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
				throw new JBasicException(Status.INVRECDEF, new Status(Status.EXPMEMBER, "NAME"));
			final String nameString = fieldName.getString().toUpperCase();

			final Value fieldType = fieldRecord.getElement("TYPE");
			if (fieldType == null)
				throw new JBasicException(Status.INVRECDEF, new Status(Status.EXPMEMBER, "TYPE"));
			final String typeString = fieldType.getString().toUpperCase();

			Value datum = null;
			if (fromRecord && source != null)
				datum = source.getElement(nameString);
			else 
				datum = env.localSymbols.findReference(nameString, false);
			

			int size = 0;
			int kind = Value.UNDEFINED;

			if (!typeString.equals("BITFIELD") && datum == null)
				throw new JBasicException(Status.UNKVAR, nameString);


			if (typeString.equals("UNICODE")) {
				final String stringValue = datum.getString();

				/*
				 * If there is no size, then use the actual string length, 
				 * and assume we're writing a varying string with a length
				 * byte.  If there is an explicit size, we don't write a
				 * length at all.  Note, you can set SYS$BINARY_STRING_SIZE
				 * to specify the default string size when SIZE isn't
				 * given to prevent the creation of variable length strings
				 * entirely.  This may be more compatible with other versions
				 * of BASIC...
				 */
				final Value fieldSize = fieldRecord.getElement("SIZE");
				if (fieldSize == null) {
					if( randomFile.defaultStringSize == null ) {
						randomFile.putInteger(stringValue.length(), 4);
						size = stringValue.length();
					}
					else 
						size = randomFile.defaultStringSize.getInteger();
				}
				else
					size = fieldSize.getInteger();

				randomFile.putUnicode(stringValue, size);
				kind = Value.STRING;
			} 
			else if (typeString.equals("VARYING")) {
				Value fieldSize = fieldRecord.getElement("SIZE");
				if( fieldSize == null )
					fieldSize = new Value(256);
				size = fieldSize.getInteger();
				
				String writeString = datum.getString();
				if( writeString.length() > size)
					writeString = writeString.substring(0,size);
				
				randomFile.putInteger(writeString.length(), 4);
				randomFile.putString(writeString, size);
				kind = Value.STRING;
			} else if (typeString.equals("STRING")) {
				final String stringValue = datum.getString();

				/*
				 * If there is no size, then use the actual string length, 
				 * and assume we're writing a varying string with a length
				 * byte.  If there is an explicit size, we don't write a
				 * length at all.  Note, you can set SYS$BINARY_STRING_SIZE
				 * to specify the default string size when SIZE isn't
				 * given to prevent the creation of variable length strings
				 * entirely.  This may be more compatible with other versions
				 * of BASIC...
				 */
				final Value fieldSize = fieldRecord.getElement("SIZE");
				if (fieldSize == null) {
					if( randomFile.defaultStringSize == null ) {
							randomFile.putInteger(stringValue.length(), 4);
							size = stringValue.length();
					}
					else 
						size = randomFile.defaultStringSize.getInteger();
				}
				else
					size = fieldSize.getInteger();

				randomFile.putString(stringValue, size);
				kind = Value.STRING;
			} else if (typeString.equals("INTEGER")) {
				Value fieldSize = fieldRecord.getElement("SIZE");
				if( fieldSize == null)
					size = 4;
				else
					size = fieldSize.getInteger();
				kind = Value.INTEGER;
				int v = datum.getInteger();
				if( size != 1 & size != 2 & size != 4 )
					throw new JBasicException(Status.INVRECDEF, new Status(Status.INTSIZE, size));
				
				randomFile.putInteger(v, size);
			} else if( typeString.equals("BITFIELD")) {
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
				
				byte[] buffer = new byte[bufferLen];
				
				for( int ix = 0; ix < mapCount; ix++ ) {
					String localName = nameArray[ix];
					switch( typeArray[ix]) {
					
					case Value.INTEGER:
						bm.setInt(buffer, ix, env.localSymbols.getInteger(localName));
						break;
						
					case Value.DOUBLE:
						bm.setDouble( buffer, ix, env.localSymbols.getDouble(localName));
						break;
						
					case Value.STRING:
						bm.setString(buffer, ix, env.localSymbols.getString(localName));						
						break;
						
					}
				}
				/*
				 * All the work is done at this point, so let the field loop
				 * run again without getting to the bottom of this code, which
				 * cannot handle FIELD anyway.
				 */
				randomFile.putBytes(buffer);
				continue;
				
			} else if (typeString.equals("BYTE")) {
				size = 1;
				kind = Value.INTEGER;
				randomFile.putInteger(datum.getInteger(), 1);
			} else if (typeString.equals("WORD")) {
				size = 2;
				kind = Value.INTEGER;
				randomFile.putInteger(datum.getInteger(), 2);
			} else if (typeString.equals("DOUBLE")) {
				size = 8;
				kind = Value.DOUBLE;
				randomFile.putDouble(datum.getDouble());
			} else if (typeString.equals("FLOAT")) {
				Value fieldSize = fieldRecord.getElement("SIZE");
				if( fieldSize == null)
					size = 4;
				else
					size = fieldSize.getInteger();
				kind = Value.DOUBLE;
				
				switch( size ) {
				case 4:	randomFile.putFloat(datum.getDouble());
						break;
				case 8: randomFile.putDouble(datum.getDouble());
						break;
				default:
						throw new JBasicException(Status.INVRECDEF, new Status(Status.FLTSIZE, size));

				}
			} else if (typeString.equals("BOOLEAN")) {
				size = 1;
				kind = Value.BOOLEAN;
				randomFile.putBoolean(datum.getBoolean());
			}

			if (kind == Value.UNDEFINED)
				throw new JBasicException(Status.INVRECDEF, 
						new Status(Status.BADTYPE, typeString));

		}
		return;
	}

}

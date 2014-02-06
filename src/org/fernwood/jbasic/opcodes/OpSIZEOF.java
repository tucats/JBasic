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
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpSIZEOF extends AbstractOpcode {

	/**
	 * Calculate the size of the TOS in bytes. The mode is controlled by the
	 * integer flag.
	 * 
	 * 0 - calculate the absolute size of the top stack item. 1 - calculate the
	 * TOS as a record definition to determine record size
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final int mode = env.instruction.integerValid ? env.instruction.integerOperand : 0;
		
		if (mode == 1) {

			final Value fieldList = env.pop();
			int totalSize = sizeof(fieldList);
			env.push(totalSize);
			return;
		}

		if (mode == 0) {

			final Value v = env.pop();

			env.push(v.sizeOf());
			return;
		}

		throw new JBasicException(Status.FAULT,
				new Status(Status.INVOPARG, mode));

	}

	/**
	 * Calculate size of a FIELD definition.
	 * @param fieldList a RECORD data type containing the field definition
	 * information.
	 * @return The size in bytes of the resulting field definition.
	 * @throws JBasicException if the field value isn't a correctly
	 * formed RECORD.
	 */
	public static int sizeof(Value fieldList ) throws JBasicException {
		int n;
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

		int totalSize = 0;

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

			if (typeString.equals("FLOAT")) {
				final Value fieldSize = fieldRecord.getElement("SIZE");
				if (fieldSize == null)
					size = 4;
				else
					size = fieldSize.getInteger();
				if( size != 4 & size != 8 )
					throw new JBasicException(Status.INVRECDEF,
							new Status(Status.FLTSIZE, size));
				kind = Value.DOUBLE;
			}
			if (typeString.equals("UNICODE")) {

				final Value fieldSize = fieldRecord.getElement("SIZE");
				if (fieldSize == null)
					throw new JBasicException(Status.INVRECDEF,
							new Status(Status.EXPSIZE));
				
				size = fieldSize.getInteger() *2 + 4 /* length field */;
				kind = Value.STRING;
			} else if (typeString.equals("VARYING")) {

				final Value fieldSize = fieldRecord.getElement("SIZE");
				if (fieldSize == null)
					throw new JBasicException(Status.INVRECDEF,
							new Status(Status.EXPSIZE));
				
				size = fieldSize.getInteger()  + 4 /* length field */;
				kind = Value.STRING;
			} else if (typeString.equals("STRING")) {

				final Value fieldSize = fieldRecord.getElement("SIZE");
				if (fieldSize == null)
					throw new JBasicException(Status.INVRECDEF,
							new Status(Status.EXPSIZE));
				
				//size = fieldSize.getInteger() * 2 + 4 /* length field */;
				size = fieldSize.getInteger();
				kind = Value.STRING;
			} else if (typeString.equals("INTEGER")) {
				Value fieldSize = fieldRecord.getElement("SIZE");
				if( fieldSize == null)
					size = 4;
				else
					size = fieldSize.getInteger();
				if( size != 1 & size != 2 & size != 4 )
					throw new JBasicException(Status.INVRECDEF, 
							new Status(Status.INTSIZE, size));
				kind = Value.INTEGER;
			} else if (typeString.equals("BYTE")) {
				size = 1;
				kind = Value.INTEGER;
			} else if (typeString.equals("WORD")) {
				size = 2;
				kind = Value.INTEGER;
			} else if (typeString.equals("DOUBLE")) {
				size = 8;
				kind = Value.DOUBLE;
			} else if (typeString.equals("BOOLEAN")) {
				size = 1;
				kind = Value.BOOLEAN;
			}

			if (kind == Value.UNDEFINED)
				throw new JBasicException(Status.INVRECDEF, "unknown type "
						+ typeString);

			totalSize = totalSize + size;
		}
		return totalSize;
	}
}

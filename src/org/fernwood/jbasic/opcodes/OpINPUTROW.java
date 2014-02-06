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
 * Created on Nov 11, 2009 by tom
 *
 */
package org.fernwood.jbasic.opcodes;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBFInput;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * Implement the _INPUTROW opcode.
 * 
 * @author tom
 * @version version 1.0 Nov 16, 2009
 *
 */
public class OpINPUTROW extends AbstractOpcode {


	/**
	 * INPUTROW fileflag, "table-name"<br>
	 * <br>
	 * 
	 * Input data items from the console or input stream, and create a
	 * new row in the pre-existing table.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * First we must validate the argument. This must be present, and must
		 * be an existing TABLE data type.
		 */
		if( !env.instruction.stringValid)
			throw new JBasicException(Status.FAULT, "missing TABLE name");
		String tableName = env.instruction.stringOperand;
		Value tempValue = env.localSymbols.findReference(tableName, false);
		if( tempValue == null )
			throw new JBasicException(Status.UNKVAR, tableName);
		if( tempValue.getType() != Value.TABLE )
			throw new JBasicException(Status.WRONGTYPE, "TABLE");
		
		RecordStreamValue table = (RecordStreamValue) tempValue;
		
		/*
		 * Determine where the input comes from - file or console.
		 */
		JBFInput inputFile = (JBFInput) env.session.stdin();
		final int mode = env.instruction.integerOperand;

		if (mode == 1) {
			Value fileDescriptor = env.pop();
			final JBasicFile t = JBasicFile.lookup(env.session, fileDescriptor);
			if (t == null)
				throw new JBasicException(Status.FNOPEN);
			if (t.getMode() != JBasicFile.MODE_INPUT &&
					t.getMode() != JBasicFile.MODE_PIPE )
				throw new JBasicException(Status.WRONGMODE, "INPUT");
			inputFile = (JBFInput) t;
		}
		else
			if( mode != 0 )
				throw new JBasicException(Status.FAULT, 
						new Status(Status.INVOPARG, mode));

		
		/*
		 * For as many data elements as needed to satisfy a row, read an
		 * input value from the input file.  The resulting data is put in
		 * an array.
		 */
		int count = table.rowSize();
		Value row = new Value(Value.ARRAY, null);
		
		for( int column = 1; column <= count; column ++ ) 
			row.addElement(inputFile.readValue());
		
		/*
		 * Finally, add the array that contains the row into the table.
		 * The table will do the type conversions as needed. Then we're done!
		 */
		
		table.addElement(row);		
	}
}

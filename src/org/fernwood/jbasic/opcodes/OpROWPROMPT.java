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
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * Implement the _ROWPROMPT opcode.
 * 
 * @author tom
 * @version version 1.0 Nov 16, 2009
 *
 */
public class OpROWPROMPT extends AbstractOpcode {

	@Override
	public void execute(InstructionContext env) throws JBasicException {
		
		if(!env.instruction.stringValid)
			throw new JBasicException(Status.FAULT, "missing TABLE name");
		
		Value temp = env.localSymbols.findReference(env.instruction.stringOperand, false);
		if( temp == null )
			throw new JBasicException(Status.UNKVAR, env.instruction.stringOperand);
		if( temp.getType() != Value.TABLE)
			throw new JBasicException(Status.WRONGTYPE, "TABLE");
		
		StringBuffer buff = new StringBuffer();
		RecordStreamValue table = (RecordStreamValue) temp;
		Value names = table.columnNames();
		
		for( int idx = 1; idx <= names.size(); idx++ ) {
			if( idx > 1 )
				buff.append(", ");
			String columnMetaData = names.getString(idx);
			buff.append(columnMetaData.substring(0, columnMetaData.indexOf('@')));
		}
		buff.append(' ');
		buff.append(env.localSymbols.getString("SYS$INPUT_PROMPT"));
		env.push(new Value(buff.toString()));
	}

}

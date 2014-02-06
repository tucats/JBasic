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
public class OpSTORR extends AbstractOpcode {

	/**
	 * Store a value in a record element, where the value and member name are
	 * taken from the stack. The record name itself is the string argument to
	 * the instruction.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final String memberName = env.pop().getString(); /* Get member name */
		Value theValue = env.popForUpdate(); /* Get value */

		Value theRecord;
		final String recordName = env.instruction.stringOperand;
		if( env.localSymbols.isReadOnly(recordName))
			throw new JBasicException(Status.READONLY, recordName);
		
		try {
			theRecord = env.localSymbols.reference(recordName);
		} catch (JBasicException e) {
			if( !env.codeStream.fDynamicSymbolCreation)
				throw new JBasicException(Status.UNKVAR, recordName);
			theRecord = null;
		}

		if (theRecord == null) {
			theRecord = new Value(Value.RECORD, recordName);
			env.localSymbols.insert(recordName, theRecord);
		}

		if (!theRecord.isType(Value.RECORD))
			theRecord = new Value(Value.RECORD, null);

		
		if (env.hasStaticTyping()) {
			final Value tempValue = theRecord.getElement(memberName);
			if (tempValue != null)
				theValue.coerce(tempValue.getType());
		}
		
		boolean success = theRecord.setElement(theValue, memberName);
		if( !success)
			throw new JBasicException(Status.INVOBJFLD, memberName);
		

	}

}

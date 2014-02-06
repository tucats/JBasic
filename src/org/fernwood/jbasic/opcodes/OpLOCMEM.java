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
public class OpLOCMEM extends AbstractOpcode {

	/**
	 * Load a record member on the stack, create it if necessary.
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

		/*
		 * If the target variable exists but isn't a record, then
		 * convert it to an empty record.
		 */
		if( theRecord != null && theRecord.getType() != Value.RECORD ) {
			if( !env.codeStream.fDynamicSymbolCreation )
				throw new JBasicException(Status.INVRECUSE);
			
			theRecord.set(new Value(Value.RECORD, null));
		}
		
		/*
		 * If the target didn't exist at all, then create a new RECORD
		 * type of the given name.
		 */

		env.codeStream.refSecondary("." + memberName);
		Value recordElement = theRecord == null ? null : theRecord.getElement(memberName);
		if (recordElement == null) {
			if( !env.codeStream.fDynamicSymbolCreation )
				throw new JBasicException(Status.NOMEMBER);
			recordElement = new Value(0);
			if( theRecord != null ) {
				theRecord.setElement(recordElement, memberName);
				recordElement = theRecord.getElement(memberName);
			}
		}
		env.push(recordElement);

		return;
	}

}

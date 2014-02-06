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
public class OpSTORA extends AbstractOpcode {

	/**
	 * Store value(stack[top]) into named arrayValue using index(stack[top-1])
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value value1 = env.popForUpdate(); /* Value to store */
		final Value value2 = env.pop(); /* Index to store at */

		/*
		 * See if we already know about this item. Even if we do, if it's not an
		 * arrayValue, we need to force it's type by re-creating it.
		 */

		final String arrayName = env.instruction.stringOperand;
		Value value3 = null;
		try {
			value3 = env.localSymbols.reference(arrayName);
		}
		catch (JBasicException e ) {
			/* do nothing */;
		}

		if (value3 != null) {
			if (value3.fReadonly)
				throw new JBasicException(Status.READONLY, arrayName);

			if (!value3.isType(Value.ARRAY) && !value3.isType(Value.RECORD))
				value3 = null;
		}

		/*
		 * If we must create the arrayValue, do it now. We guess as to whether
		 * it's meant to be a record versus a real array based on the type of
		 * the index value - a string implies a record field name.
		 */
		if (value3 == null) {
			
			if( !env.codeStream.fDynamicSymbolCreation && env.localSymbols.localReference(arrayName) == null)
				throw new JBasicException(Status.UNKVAR, arrayName);

			final int kind = value2.isType(Value.STRING) ? Value.RECORD
					: Value.ARRAY;
			value3 = new Value(kind, arrayName);
			env.localSymbols.insert(arrayName, value3);
		}

		/*
		 * Based on the type of the target (array or record), do the right thing
		 * with the index/field-name and store the value.
		 */
		if (value3.isType(Value.ARRAY)) {
			value3.setElement(value1, value2.getInteger());
		} else {
			value3.setElement(value1, value2.getString().toUpperCase());
		}
		return;
	}

}

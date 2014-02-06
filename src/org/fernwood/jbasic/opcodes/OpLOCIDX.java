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
public class OpLOCIDX extends AbstractOpcode {

	/**
	 * Dereference an arrayValue element.  Create it if it does not exist.
	 * <p>
	 * If the integer operand is valid, it contains the constant value to use as
	 * the index.  Otherwise, the constant value must be the top stack item.
	 * <p>
	 * If the string parameter is valid, it contains the name of the arrayValue
	 * to index. If it is not valid, then the second stack item must contain the
	 * array reference.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value elementValue = null;
		Value indexValue = null;
		Value arrayValue = null;

		if( env.instruction.integerValid)
			indexValue = new Value(env.instruction.integerOperand);
		else
			indexValue = env.pop(); /* index */

		if (env.instruction.stringValid) {
			arrayValue = env.localSymbols.reference(env.instruction.stringOperand);
			env.codeStream.refPrimary(env.instruction.stringOperand, false);

		} else
			arrayValue = env.pop(); /* Array */

		if (arrayValue.isType(Value.RECORD)) {
			final String key = indexValue.getString().toUpperCase();
			elementValue = arrayValue.getElement(key);
			env.codeStream.refSecondary("." + key );
			if (elementValue == null) {
				if( !env.codeStream.fDynamicSymbolCreation)
					throw new JBasicException(Status.NOMEMBER);
				elementValue = new Value(0);
				arrayValue.setElement(elementValue, key);
			}
		} else {
			indexValue.coerce(Value.INTEGER);

			if (arrayValue.isType(Value.ARRAY)) {
				final int ix = indexValue.getInteger();
				if ((ix < 1))
					throw new JBasicException(Status.ARRAYBOUNDS, Integer.toString(ix));
				if( ix > arrayValue.size())
					for( int i = arrayValue.size()+1; i <= ix; i++ )
						arrayValue.setElement(new Value(0), i);
				elementValue = arrayValue.getElement(ix);
				env.codeStream.refSecondary("[" + ix + "]");
				
			} 
		}
		if (elementValue == null) {
			if( !env.codeStream.fDynamicSymbolCreation)
				throw new JBasicException(Status.ARRAYBOUNDS);
			Value temp = arrayValue.copy();
			arrayValue.set(new Value(Value.ARRAY, null));
			arrayValue.setElement(temp, 1);
			int ix = indexValue.getInteger();
			if( ix > 1 )
				for( int i = 2; i <= ix; i++ )
					arrayValue.setElement(new Value(0), i);				
			elementValue = arrayValue.getElement(ix);
		}

		env.push(elementValue );

		return;
	}

}

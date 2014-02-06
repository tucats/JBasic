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

import java.math.BigDecimal;

import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpCVT extends AbstractOpcode {

	/**
	 * Coerce the top item to a particular type, defined in the opcode using the
	 * Value.VALUE_* constants.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value value = env.popForUpdate();

		/*
		 * If we are given the name of a symbol, see if that symbol exists
		 * already. If so, then use that as the type to coerce to. Note that we
		 * only check the local symbol table for static type matching.
		 */
		int targetType = env.instruction.integerOperand;
		
		if(targetType == Value.QUOTED_FORMATTED_STRING &&
				value.getType() == Value.STRING) {
			env.push(new Value(Value.toString(value, true)));
			return;
		}
		
		/**
		 * If the target is a normalized string and the value is a 
		 * string already, then denormalize it for converted output.
		 * Otherwise, remap to just STRING value type.
		 */
		if(targetType == Value.NORMALIZED_STRING ) {
			
			if( value.getType() == Value.STRING) {
				env.push(new Value(value.denormalize()));
				return;
			}
			targetType = Value.STRING;
		}
		
		int scale = 0;
		if (env.instruction.stringValid) {

			final Value likeValue = env.localSymbols.localReference(env.instruction.stringOperand);
			if (likeValue != null) {
				targetType = likeValue.getType();
				if( targetType == Value.DECIMAL)
					scale = likeValue.getDecimal().scale();
			}
		}

		/*
		 * Use the conversion code given in the instruction or taken from the 
		 * "LIKE" variable to convert our value and put it back on the stack.
		 */
		value.coerce(targetType);
		if( scale != 0 )
			value.setDecimal(((BigDecimal)value.getObject()).setScale(scale, BigDecimal.ROUND_HALF_UP));
		
		env.push(value);

	}

}

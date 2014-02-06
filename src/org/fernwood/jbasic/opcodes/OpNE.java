package org.fernwood.jbasic.opcodes;

import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpNE extends AbstractOpcode {

	/**
	 * Pop two items from stack, and push a boolean that tells if they are not
	 * equal.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value value1, value2;

		value2 = env.pop();
		value1 = env.pop();
		
		boolean state;
		
		/*
		 * If both types are RECORD types, then we must use the MATCH function
		 * which can only test for equal or not equal.  For other data types,
		 * we can do an ordinal comparison.
		 */
		if( value1.getType() == Value.RECORD && value2.getType() == Value.RECORD)
			state = !value1.match(value2);
		else
			state = (value1.compare(value2) != 0);

		if( env.instruction.integerOperand > 0 ) {
			if( state )
				env.codeStream.programCounter = env.instruction.integerOperand;
		}
		else
			env.push(state);
		
		return;
	}

}

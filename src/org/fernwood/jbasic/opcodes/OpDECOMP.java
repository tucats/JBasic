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
 * <code>DECOMP</code>
 * <p>
 * This opcode decomposes an array on the stack into discrete elements.  So
 * a stack item of [1,2,3] becomes three items on the stack instead.  This is
 * used in (among other things) a compound assignment statement.
 * <p>
 * 
 * @author cole
 * @version 1.0 May 2013 Initial creation
 */
public class OpDECOMP extends AbstractOpcode {

	
	/*
	 * Execute the _DECOMP opcode
	 * 
	 * @see org.fernwood.jbasic.opcodes.OpCode#execute(org.fernwood.jbasic.opcodes.OpEnv)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value array = env.pop();
		if( !array.isType(Value.ARRAY))
			throw new JBasicException(Status.TYPEMISMATCH);
		
		int count = array.size();
		if( env.instruction.integerValid)
			count = env.instruction.integerOperand;
		
		if( count > array.size()) {
			for( int i = array.size(); i < count; i++ )
				env.push(new Value(Double.NaN));
			count = array.size();
		}
		for( int i = count; i > 0 ; i-- )
			env.push(array.getElement(i));
		
		return;
	}



}

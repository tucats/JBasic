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
public class OpDCLVAR extends AbstractOpcode {

	/**
	 * <b><code>_DCLVAR <em>type</em>, "<em>name</em>"</code><br><br></b>
	 * Create a variable of the given name and type.  This requires that
	 * the variable not already exist.
	 * <p>
	 * If the type is a negative number, then the top of stack contains a
	 * data item that is used as the initial value of the declared variable.
	 * If the type is a positive number, then a null/empty/zero value is
	 * used as the initial value.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		if( !env.instruction.integerValid )
			throw new JBasicException(Status.INVOPARG, "<type>");

		if( !env.instruction.stringValid )
			throw new JBasicException(Status.INVOPARG, "<name>");

		if( env.instruction.doubleValid )
			throw new JBasicException(Status.INVOPARG, Double.toString(env.instruction.doubleOperand));

		/*
		 * Get the data type and name that will be created. Determine if we are
		 * in "STORE" mode which says that the variable must be created and then
		 * initially filled with the value on the stack.
		 */
		int dataType = env.instruction.integerOperand;
		boolean storeData = false;
		
		if( dataType < 0 ) {
			storeData = true;
			dataType = -dataType;
		}
		String varName = env.instruction.stringOperand.toUpperCase();
		
		
		if( Value.typeToName(dataType).equals("UNDEFINED"))
			throw new JBasicException(Status.INVOPARG, dataType);
		
		if( !env.codeStream.fDynamicSymbolCreation && (env.localSymbols.localReference(varName) != null ))
			throw new JBasicException(Status.DUPVAR, varName);
		
		Value data = null;
		if( !storeData )
			data = new Value(dataType, null);
		else {
			data = env.pop();
			data.coerce(dataType);
		}
		env.localSymbols.insert(varName, data);
		
	}

}

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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * The INCR instruction class. Add a signed integer value to a previously
 * existing storage location. Used by the optimizer to replace sequences of
 * load/add/store with incr.
 * 
 * @author tom
 * @version version 1.0 Aug 15, 2006
 * 
 */
public class OpINCR extends AbstractOpcode {

	public void execute(final InstructionContext env) throws JBasicException {

		final Instruction i = env.instruction;

		final Value value = env.localSymbols.reference(i.stringOperand);
		if (value == null)
			throw new JBasicException(Status.UNKVAR, i.stringOperand);

		int type = value.getType();
		switch( type ) {
		
		case Value.DECIMAL:
			value.setDecimal(value.getDecimal().add(new BigDecimal(i.integerOperand)));
			break;
			
		case Value.INTEGER:
			value.setInteger(value.getInteger() + i.integerOperand);
			break;
		case Value.DOUBLE:
			value.setDouble(value.getDouble() + i.integerOperand);
			break;
		case Value.STRING:
			value.setString(value.getString() + Integer.toString(i.integerOperand));
			break;
		case Value.ARRAY:
			value.addElement(new Value(i.integerOperand));
			break;
			
		default:
			throw new JBasicException(Status.INVCVT, "numeric");
		}
		return;
	}
}

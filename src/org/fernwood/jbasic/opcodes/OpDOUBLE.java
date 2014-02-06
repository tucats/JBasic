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

/**
 * @author cole
 * 
 */
public class OpDOUBLE extends AbstractOpcode {

	/**
	 * Push a double constant onto the runtime stack.  If the integer argument
	 * is present, it indicates special case constants.<p>
	 * <list>
	 * <li> 0 - The constant zero
	 * <li> 1 - The MAXIMUM floating point value
	 * <li> 2 - The MINIMUM floating point value.
	 * <li> 3 - A NaN
	 * <li> 4 - Positive INF
	 * <li> 5 - Negative INF
	 * </list>
	 * <p><br>
	 * If the integer value is not present, then the double operand must be present
	 * and contains the constant value.
	 * <br><br>
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		if (env.instruction.integerValid) {

			double d = 0.0;

			switch (env.instruction.integerOperand) {

			case 0:
				d = 0.0;
				break;

			case 1:
				d = Double.MAX_VALUE;
				break;

			case 2:
				d = Double.MIN_VALUE;
				break;

			case 3:
				d = Double.NaN;
				break;

			case 4:
				d = Double.POSITIVE_INFINITY;
				break;
			case 5:
				d = Double.NEGATIVE_INFINITY;
				break;

			default:
				throw new JBasicException(Status.FAULT, "invalid _DOUBLE type "
						+ env.instruction.integerOperand);

			}
			env.push(d);
			return;
		}

		if( !env.instruction.doubleValid)
			throw new JBasicException(Status.FAULT, "missing double operand");
		
		env.push(env.instruction.doubleOperand);
		return;
	}

}

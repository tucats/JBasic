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
public class OpNEGATE extends AbstractOpcode {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fernwood.jbasic.opcodes.OpCode#execute(org.fernwood.jbasic.opcodes.OpEnv)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value value1 = env.popForUpdate();
		int ix;

		switch (value1.getType()) {

		case Value.DECIMAL:
			value1.setDecimal(value1.getDecimal().negate());
			break;
		case Value.BOOLEAN:
			value1.setBoolean( !value1.getBoolean());
			break;

		case Value.DOUBLE:
			value1.setDouble(-(value1.getDouble()));
			break;

		case Value.INTEGER:
			value1.setInteger(-(value1.getInteger()));
			break;

		case Value.STRING:

			String revString = "";
			for (ix = 0; ix < value1.getString().length(); ix++)
				revString = value1.getString().substring(ix, ix + 1)
						+ revString;
			value1 = new Value(revString);
			break;

		default:
			throw new JBasicException(Status.TYPEMISMATCH);

		}
		env.push(value1);

		return;
	}

}

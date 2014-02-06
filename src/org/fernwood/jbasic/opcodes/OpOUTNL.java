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
import org.fernwood.jbasic.runtime.JBFDatabase;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpOUTNL extends AbstractOpcode {

	/**
	 * Print string on top of stack. File designation may be under it if the
	 * integer flag is 1. Output a trailing newline.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		JBasicFile outFile = env.session.stdout;

		Value outputValue = null;
		if( env.instruction.stringValid)
			outputValue = new Value(env.instruction.stringOperand);
		else
			outputValue = env.pop();
		
		String outputString = null;
		if( outputValue.getType() == Value.TABLE) {
			outputString = outputValue.toString();
		}
		else
		if( outputValue.getType() != Value.RECORD) {
			outputValue = outputValue.copy();
			outputValue.coerce(Value.STRING);
			outputString = outputValue.getString();
		}
		else
			outputString = outputValue.toString();
		
		int mode = env.instruction.integerOperand;
		
		if (mode == 1) {
			final Value fileReference = env.pop();
			final JBasicFile tempf = JBasicFile.lookup(env.session, fileReference);

			if (tempf == null)
				throw new JBasicException(Status.FNOPENOUTPUT, fileReference.toString());

			/*
			 * This might be a DATABASE file. If so, we handle it differently by
			 * sending the output to the file and then fetching the return code
			 * from the database statement processor.
			 */
			if (tempf.getMode() == JBasicFile.MODE_DATABASE) {
				final JBFDatabase db = (JBFDatabase) tempf;
				db.execute(outputString, true);
				return;
			}

			outFile = tempf;

		}
		else
			if( mode != 0 )
				throw new JBasicException(Status.FAULT, 
						new Status(Status.INVOPARG, mode));

		mode = outFile.getMode();

		if ((mode != JBasicFile.MODE_OUTPUT) && 
				(mode != JBasicFile.MODE_QUEUE) &&
				(mode != JBasicFile.MODE_SOCKET) &&
				(mode != JBasicFile.MODE_PIPE))
			throw new JBasicException(Status.WRONGMODE, "OUTPUT");

		outFile.println(outputString);

	}

}

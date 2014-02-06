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
public class OpOUT extends AbstractOpcode {

	/**
	 * Print string on top of stack. File designation may be under it if the
	 * integer flag is 1.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		JBasicFile outFile = env.session.stdout;

		/*
		 * We must use coerce() explicitly since that knows how to format all
		 * the complicated record types like RECORD or ARRAY.  So make a local
		 * copy of the value to print so we don't screw up the symbolic value,
		 * etc.
		 */
		Value outputValue = null;
		if( env.instruction.stringValid)
			outputValue = new Value(env.instruction.stringOperand);
		else
			outputValue = env.pop().copy();
		outputValue.coerce(Value.STRING);
		
		final String outputString = outputValue.getString();
		int mode = env.instruction.integerOperand;
		
		if (mode == 1) {
			final Value fileReference = env.pop();
			final JBasicFile outputFile = JBasicFile.lookup(env.session, fileReference);

			/*
			 * This might be a DATABASE file. If so, we handle it differently by
			 * sending the output to the file and then fetching the return code
			 * from the database statement processor.
			 */
			if (outputFile.getMode() == JBasicFile.MODE_DATABASE) {
				final JBFDatabase db = (JBFDatabase) outputFile;
				db.execute(outputString, false);
				return;
			}

			outFile = outputFile;
		}
		else if( mode != 0 )
			throw new JBasicException(Status.FAULT, 
					new Status(Status.INVOPARG, mode));


		mode = outFile.getMode();
		if ((mode != JBasicFile.MODE_OUTPUT) && 
				(mode != JBasicFile.MODE_QUEUE)  &&
				(mode != JBasicFile.MODE_SOCKET)  &&
			(mode != JBasicFile.MODE_PIPE))
			throw new JBasicException(Status.WRONGMODE, "OUTPUT");

		outFile.print(outputString);

		return;
	}

}

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
import org.fernwood.jbasic.runtime.JBFOutput;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpNL extends AbstractOpcode {

	/**
	 * Print a newline. If the integer argument is 1, use the top of stack as
	 * the file designation, else send to the console.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		JBFOutput outFile = (JBFOutput) env.session.stdout;

		if (env.instruction.integerOperand == 1) {
			final Value outputFileIdentifier = env.pop();
			final JBasicFile tempf = JBasicFile.lookup(env.session, outputFileIdentifier);

			/*
			 * This might be a DATABASE file. If so, tell it to execute whatever
			 * it has, by adding nothing but setting the immediate flag.
			 */
			if (tempf.getMode() == JBasicFile.MODE_DATABASE) {
				final JBFDatabase db = (JBFDatabase) tempf;
				db.execute(null, true);
			}

			if( tempf.getMode() != JBasicFile.MODE_OUTPUT )
				throw new JBasicException(Status.WRONGMODE, "OUTPUT");
			
			outFile = (JBFOutput) tempf;

		}

		outFile.println();

		return;
	}

}

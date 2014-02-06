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

import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Linkage;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpJMPIND extends AbstractOpcode {

	/**
	 * Branch to a label in the current program. The label is a string value on
	 * the top of the stack, presumably the result of a string expression. This
	 * supports GOTO USING() clauses.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * This can only be done when there is a program, and therefore
		 * statements to branch to...
		 */
		if (env.codeStream.statement.program == null)
			throw new JBasicException(Status.NOACTIVEPGM);

		/*
		 * See where we're going.  This can be an integer or string.
		 */

		Value destination = env.pop();		
		if( destination.getType() == Value.STRING) {

			/*
			 * Get the destination label, and force to upper case so we can find it in
			 * the linkage map.
			 */
			final String labelString = destination.getString().toUpperCase();

			/*
			 * Use the symbol table map to locate the label to branch to.
			 */

			final Linkage lx = env.codeStream.labelMap.get(labelString);
			if (lx == null)
				throw new JBasicException(Status.NOSUCHLABEL, labelString);
			env.codeStream.programCounter = lx.byteAddress;
		}
		else if (destination.getType() == Value.INTEGER) {
			int lineNumber = 0;
			if( env.codeStream.statement != null ) {
				Program activePgm = env.codeStream.statement.program;
				if( activePgm == null)
					throw new JBasicException(Status.INVPGMLIN, destination.getInteger());
				lineNumber = activePgm.findExecutableLine(destination.getInteger());
				int addr = env.codeStream.findLineNumber(lineNumber);
				if( addr > 0)
					env.codeStream.programCounter = addr;
				else
					lineNumber = 0;
				}
			
			if( lineNumber == 0 )
				throw new JBasicException(Status.INVPGMLIN, destination.getInteger());
			
		}
		else
			throw new JBasicException(Status.INVPGMLIN);

		return;
	}
}

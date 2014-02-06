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
import org.fernwood.jbasic.runtime.JBFOutput;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * COLUMN operator. This pops a fileID, width, and column count and uses them to
 * set the column attributes of the given file.
 * 
 * @author tom
 * @version version 1.0 Aug 15, 2006
 * 
 */
public class OpCOLUMN extends AbstractOpcode {

	public void execute(final InstructionContext env) throws JBasicException {

		Value fileID = null;
		if( env.instruction.stringValid )
			fileID = env.localSymbols.reference(env.instruction.stringOperand);
		else
			fileID = env.pop();
		final Value columnWidth = env.pop(); 
		final Value columnCount = env.pop();

		final JBFOutput fileToSet = (JBFOutput) JBasicFile.lookup(env.session,
				fileID);
		if (fileToSet == null)
			throw new JBasicException(Status.FNOPEN, fileID.toString());
		
		fileToSet.columnOutput(columnWidth.getInteger(), columnCount.getInteger());
	}

}

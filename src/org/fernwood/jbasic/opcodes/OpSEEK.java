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
import org.fernwood.jbasic.runtime.JBFBinary;
import org.fernwood.jbasic.runtime.JBFInput;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpSEEK extends AbstractOpcode {

	/**
	 * Position a BINARY file to a given location. The top of stack is the
	 * location, and the second item is the file identifier.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		

		/*
		 * Get the file position value.
		 */
		final Value filePos = env.pop();
		long newPos = filePos.getInteger();
		int mode = env.instruction.integerOperand;
		
		/*
		 * Get the file identifier.
		 */

		final Value fileID = env.pop();
		JBasicFile aFile = JBasicFile.lookup(env.session, fileID);
		if (aFile == null)
			throw new JBasicException(Status.FNOPENOUTPUT, fileID.toString());

		/*
		 * If it's a binary file then it handles random IO and we can just
		 * seek to the position and be done.
		 */
		if( aFile.getClass() == JBFBinary.class ) {
			
			/*
			 * There could be an implicit FIELD bound to this file.  If so, we've
			 * got to use that to position the records.  IF no FIELD is found,
			 * the use the position as an absolute byte value.  Also, if the
			 * mode (_SEEK 1) is non-zero, then ignore the field spec.  This is
			 * done for REWIND and SEEK..USING statements.
			 */
			
			Value fieldSpec = fileID.getElement("FIELD");
			if( fieldSpec != null && mode == 0 && newPos > 0) {
				newPos = (newPos-1) * OpSIZEOF.sizeof(fieldSpec);
			}
			JBFBinary randomFile =  (JBFBinary) aFile;
			randomFile.setPos(newPos);
			return;
		}
		
		/*
		 * If it's an INPUT file then we have to close and reopen the file
		 * because it's a stream.  Also, we can only do this if the seek
		 * location is zero.
		 */
		if( aFile.getClass() == JBFInput.class) {
			if( newPos != 0 )
				throw new JBasicException(Status.WRONGMODE, "BINARY");
			JBFInput inputFile = (JBFInput) aFile;
			inputFile.setPos(0);
			
			return;
		}
		/*
		 * Not handled, so throw an error indicating that it was an invalid
		 * file type.
		 */
		throw new JBasicException(Status.WRONGMODE, "BINARY or INPUT");


	}

}

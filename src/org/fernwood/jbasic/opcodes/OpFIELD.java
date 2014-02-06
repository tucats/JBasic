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
public class OpFIELD extends AbstractOpcode {

	/**
	 * STACK[0] is the file reference.  STACK[1] is the record array that
	 * describes the field data.  Load the array and bind it to the FIELD 
	 * field of the file reference.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value fileRef = env.pop();
		Value fNameValue = fileRef.getElement("FILENAME");
		if( fNameValue == null ) 
			throw new JBasicException(Status.INVFID, fileRef.toString());

		/*
		 * Check for mode of 1, which means clear the field from the fileref
		 * entirely.
		 */
		if( env.instruction.integerOperand == 1 ) {
			fileRef.removeElement("FIELD");
			return;
		}
		
		/*
		 * No, we're adding/changing the reference, so get the field definition
		 * record from the stack.
		 */
		Value recordDef = env.pop();
		
		/*
		 * A little validation is in order to see if the field definition is
		 * really legitimate, the file is in BINARY mode, and we have a valid
		 * file id.
		 */
		if( fileRef.getType() != Value.RECORD )
			throw new JBasicException(Status.INVFID, fileRef.toString());
						
		if( !fileRef.getElement("MODE").getString().equals("BINARY")) 
			throw new JBasicException(Status.IOERROR, 
					new Status(Status.FILENOTBIN, fNameValue.toString()));
		
		fileRef.setElement(recordDef, "FIELD");

		return;
	}

}

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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpLOADFREF extends AbstractOpcode {

	/**
	 * Load a reference to an item on the stack - does not make a copy! Checks the validity
	 * of the reference, and formats error messages appropriately.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * Check to see if we are in "indirect" mode or not, where the fileref
		 * symbol is really the name of the variable containing the fileref.
		 */
		
		boolean indirect = ( env.instruction.integerOperand == 1 );
		boolean create = (env.instruction.integerOperand == 3);
		/*
		 * Locate the value using the symbol name given.
		 */

		String symbolName = env.instruction.stringOperand;
		Value fileReference = null;
		
		if( indirect ) {
			Value intermediate = env.localSymbols.findReference(symbolName, false);
			if( intermediate == null )
				throw new JBasicException(Status.NOSUCHFID, JBasicFile.name(symbolName));
			symbolName = intermediate.getString().toUpperCase();
		}

		fileReference = env.localSymbols.findReference(symbolName, false);
		if( fileReference != null && fileReference.getType() == Value.STRING
				&& fileReference.getString().length() == 0 )
				fileReference = null;
		
		if( fileReference == null) {
			if( !create )
				throw new JBasicException(Status.NOSUCHFID, JBasicFile.name(symbolName));
			fileReference = new Value("__GENFILEREF_" + JBasic.getUniqueID());
			env.localSymbols.insert(symbolName, fileReference );
		}
		String name = symbolName;
		if( name.startsWith(JBasic.FILEPREFIX))
			name = "#" + name.substring(JBasic.FILEPREFIX.length());
		
		if( !create && fileReference.getType() != Value.RECORD )
			throw new JBasicException(Status.NOSUCHFID, JBasicFile.name(name));
		

		env.push(fileReference);
	}

}

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
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpNOTEOF extends AbstractOpcode {

	/**
	 * Test to see if there is more data to be read from a file.  The file can be
	 * identified as a value on the stack, as a numeric argument, or a string argument.
	 * <strong>This has the inverse sense of the _EOF opcode</strong> in that it returns
	 * true if there is more data (as opposed to true when the end of file is hit.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		JBasicFile f = null;

		
		Value v;
		
		if( env.instruction.integerValid ) {
			final String fid = JBasic.FILEPREFIX + env.instruction.integerOperand;
			v = env.localSymbols.reference(fid);			
		}
		else
		if( env.instruction.stringValid) {
			v = env.localSymbols.reference(env.instruction.stringOperand);
			f = JBasicFile.lookup(env.session, v);
		}
		else {
			v = env.pop();
			if( v.getType() == Value.INTEGER ) {
				final String fid = JBasic.FILEPREFIX + v.getString();
				v = env.localSymbols.reference(fid);							
			}
			f = JBasicFile.lookup(env.session, v);			
		}
		
		if (f == null)
			env.push(new Value(true));
		else
			env.push(new Value(!f.eof()));
		return;
	}

}

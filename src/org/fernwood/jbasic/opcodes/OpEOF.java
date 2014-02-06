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
public class OpEOF extends AbstractOpcode {

	/**
	 * Test to see if there is more data to be read from a file.  The file can be
	 * identified as a value on the stack, as a numeric argument, or a string argument.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		JBasicFile f = null;
		
		Value v;
		
		/*
		 * If there is an integer, it represents the #file constant number.
		 */
		if( env.instruction.integerValid ) {
			final String fid = JBasic.FILEPREFIX + env.instruction.integerOperand;
			v = env.localSymbols.reference(fid);			
			if( v != null )
				f = JBasicFile.lookup(env.session, v);
		}
		else
			/*
			 * If there is a string it contains the name of the reference variable
			 */
		if( env.instruction.stringValid) {
			v = env.localSymbols.reference(env.instruction.stringOperand);
			if( v != null )
				f = JBasicFile.lookup(env.session, v);
		}
		else {
			/*
			 * The actual reference value is found on the stack.
			 */
			v = env.pop();
			if( v.getType() == Value.INTEGER ) {
				final String fid = JBasic.FILEPREFIX + v.getString();
				v = env.localSymbols.reference(fid);							
			}
			if( v != null )
				f = JBasicFile.lookup(env.session, v);			
		}
		
		/*
		 * If after all that we found a valid file reference, see if it is
		 * at EOF.  Otherwise, assume false because it wasn't an open file.
		 */
		if (f == null) {
			env.push(new Value(false));
			throw new JBasicException(Status.FNOPEN, 
					v == null ? "null" : v.toString());
		}

		env.push(new Value(f.eof()));

		return;
	}

}

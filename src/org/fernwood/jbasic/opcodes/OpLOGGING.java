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
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpLOGGING extends AbstractOpcode {

	/**
	 *  <b><code>_LOGGING</code><br><br></b>
	 * Set the JBasic logging file's sensitivity to messages based on the
	 * integer on the top of the stack. If the value is 1, then only errors
	 * are printed.  If set to 2, both errors and warnings are printed.  If
	 * set to 3, then errors, warning, and informational messages are
	 * printed.
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		env.session.checkPermission(Permissions.LOGGING); 

		int logLevel = env.pop().getInteger();
		
		if( JBasic.log == null )
			env.session.initializeLog();
		
		if( logLevel < 1 )
			logLevel = 1;
		else
			if( logLevel > 3 )
				logLevel = 3;
		
		
		Value v = env.session.globals().reference("SYS$LOGLEVEL");
		v.setInteger(logLevel);
		JBasic.log.setLogging(logLevel);

	}
}

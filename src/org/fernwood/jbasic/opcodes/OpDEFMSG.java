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

/**
 * @author cole
 * 
 */
public class OpDEFMSG extends AbstractOpcode {

	/**
	 * Command indicating that the DEFMSG is to load message definitions
	 * from an XML file, who name is in the string argument if present 
	 * else as the Value on the stack.
	 */
	public static final int DEFMSG_LOAD_FILE = 3;

	/**
	 * Execute the _DEFMSG operand. The integer argument indicates if a separate
	 * language code is on the stack, or if it is already bound into the code
	 * argument
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		if( env.instruction.integerOperand == 5 ) {
			env.session.messageManager.clearMessages();
			return;
		}
		/*
		 * Get the message code from the string operand or stack as
		 * appropriate
		 */
		String code = env.instruction.stringOperand;
		if( !env.instruction.stringValid)
			code = env.pop().getString();

		/*
		 * If the integer flag 1 is used, then get the language code from the stack
		 * and modify the code to include the language specification.
		 */
		if (env.instruction.integerOperand == 1) {
			final String language = env.pop().getString();
			code = code.toUpperCase() + "(" + language + ")";
		}

		/*
		 * If the integer flag 2 is used, then the code is really a string
		 * with an XML specification.
		 */
		if( env.instruction.integerOperand == 2 ) {
			Status sts = env.session.messageManager.loadMessageXML(code);
			if( sts.failed())
				throw new JBasicException(sts);
			return;
		}

		/*
		 * If the integer flag 3 is used, then the code is really the 
		 * file name of teh file containing the XML specification.
		 */
		if( env.instruction.integerOperand == DEFMSG_LOAD_FILE ) {
			Status sts = env.session.messageManager.loadMessageFile(code);
			if( sts.failed())
				throw new JBasicException(sts);
			return;
		}

		if( env.instruction.integerOperand == 4 ) {
			Status sts = env.session.messageManager.saveMessageXML(code);
			if( sts.failed())
				throw new JBasicException(sts);
			return;
		
		}
		if( env.instruction.integerOperand != 0)
			throw new JBasicException(Status.INVOPARG, env.instruction.integerOperand);
		
		/*
		 * No reading whole message XML, so just get the message 
		 * text that goes with the code.
		 */
		final String msg = env.pop().getString();
		code = code.toUpperCase();
		
		/*
		 * Define the message in the internal message vector.
		 */
		env.session.messageManager.defineMessage(code, msg);

		return;
	}

}

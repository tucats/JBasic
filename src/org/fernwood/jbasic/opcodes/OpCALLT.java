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

import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicThread;
import org.fernwood.jbasic.value.Value;

/**
 * Take an argument list from the stack and construct a command with the
 * resolved arguments that can be passed to a new thread. Currently, threads
 * cannot be passed execution context, only a text command to compile and run.
 * 
 * @author cole
 * 
 */
public class OpCALLT extends AbstractOpcode {

	/**
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final Instruction i = env.instruction;
		final int argc = i.integerOperand;
		int ix;
		if( argc < 0 )
			throw new JBasicException(Status.FAULT, new Status(Status.INVOPARG, argc));

		env.session.checkPermission(Permissions.THREADS);
		
		/*
		 * The argument list was pushed on the stack in the order found, so the
		 * top item is the last argument. For this reason, we're going to
		 * manually remove them from the dataStack by address, and then
		 * discard them from the stack.
		 */

		ArgumentList funcArgs = fetchArgs(env);

		/*
		 * If the program name is given, we use it. Otherwise, we have to pop an
		 * item from the stack and use it as the string name. This supports the
		 * USING clause.
		 */

		String targetProgramName;

		if (i.stringValid)
			targetProgramName = i.stringOperand;
		else {
			final Value pNameItem = env.codeStream.pop();
			if (pNameItem == null)
				throw new JBasicException(Status.UNDERFLOW);
			targetProgramName = pNameItem.getString().toUpperCase();
		}

		/*
		 * We now begin the tiresome process of reconstructing the CALL
		 * statement so we can pass a command to a new thread.  This is
		 * done by building up a new StringBuffer containing all the 
		 * arguments, resolved in the current CALL statement's context.
		 */

		StringBuffer cmd = new StringBuffer();
		cmd.append("CALL ");
		cmd.append(targetProgramName);
		
		if (argc > 0) {

			/*
			 * Copy the argument list to the command
			 */
			cmd.append("(");
			for (ix = 0; ix < argc; ix++) {
				final Value item = funcArgs.element(ix);
				if (ix > 0)
					cmd.append(", ");
				cmd.append(Value.toString(item, true));

			}
			cmd.append(")");

		}

		/*
		 * Now that we've got a CALL statement with resolved arguments, pass it
		 * to a new thread!
		 */
		final JBasicThread newThread = new JBasicThread(env.session, cmd.toString());
		final String instanceName = newThread.getID();
		
		env.session.getChildThreads().put(instanceName, newThread);

		newThread.start();
		env.push(new Value(instanceName));

	}

}

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
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * Call a LOCAL function.  This is a function that is defined within the
 * currently running program.  This is detected at link time and/or runtime
 * and the CALLF opcode is replaced with a CALLFL opcode for efficiency.
 * 
 * @author cole
 * 
 */
public class OpCALLFL extends AbstractOpcode {

	/**
	 * <b><code>CALLFL count, "name"</code><br><br></b>
	 * Execute the _CALLFL instruction at runtime.  Pops arguments from the
	 * stack in reverse order and calls a local function.  Local functions 
	 * are segments of ByteCode that are scoped locally to the current
	 * program.
	 * 
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li><code>count</code> - The number of arguments to take
	 * from the stack</l1>
	 * <li><code>name</code> - The name of the local function 
	 * (such as created by a DEFFN statement)</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 *
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final JBasic session = env.session;
		final String name = env.instruction.stringOperand;
		Value result = null;
		
		/*
		 * The argument list was pushed on the stack in the order found, so the
		 * top item is the last argument. For this reason, we're going to
		 * manually remove them from the dataStack, and then delete them
		 * as a block from the stack.
		 */

		ArgumentList funcArgs = this.fetchArgs(env);

		/*
		 * Make the function call with the packaged argument list.
		 */
				
		ByteCode localFunction = env.codeStream.findLocalFunction(name);
		if( localFunction == null ) 
			throw new JBasicException(Status.UNKFUNC, name);
		
		result = localFunction.call(session, name, funcArgs, env.localSymbols);
		
		if (result == null)
			throw new JBasicException(Status.EXPRESSION, new Status(Status.EXPRETVAL));
		
		env.codeStream.push(result);

	}

}

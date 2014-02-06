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
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * @author cole
 * 
 */
public class OpCONSTANT extends AbstractOpcode {

	/**
	 *  <b><code>_CONSTANT <em>count</em>, <em>"name"</em></code><br><br></b>
	 * scoop up the next <em>count</em> instructions, evaluate them as
	 * a constant, and store them in the named location.  This allows
	 * references to them to resolve correctly later.
	 * 
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li><code>count</code> - An integer value, indicating the number 
	 * of following instructions to execute.</li>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		int count = env.instruction.integerOperand;
		String name = env.instruction.stringOperand;
		
		ByteCode code = new ByteCode(env.session);
		if( count < 1 )
			throw new JBasicException(Status.FAULT, 
				new Status(Status.INVOPARG, env.instruction.integerOperand));

		for( int ix = 0; ix < count; ix++ ) {
			code.add(env.codeStream.getInstruction(env.codeStream.programCounter+ix));
		}
		
		code.run(new SymbolTable(env.session, "Local to constant", null), 0);
		if( code.status.failed())
			throw new JBasicException(code.status);
		
		SymbolTable table = env.localSymbols;
		while( table != null && table.size() == 0 )
			table = table.parentTable;
		
		env.localSymbols.insert(name, code.getResult());
		env.codeStream.programCounter += count;
		
	}

}

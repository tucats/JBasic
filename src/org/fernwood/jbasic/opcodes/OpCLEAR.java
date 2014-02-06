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

import org.fernwood.jbasic.LockManager;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpCLEAR extends AbstractOpcode {

	/**
	 * _CLEAR integer argument indicating a symbol is to be deleted.
	 */
	public final static int CLEAR_SYMBOL = 0;

	/**
	 * _CLEAR integer argument indicating a symbol is to be deleted
	 * even if it is marked READONLY.
	 */
	public final static int CLEAR_SYMBOL_ALWAYS = 1;

	/**
	 * _CLEAR integer argument indicating a Program is to be deleted.
	 */
	public final static int CLEAR_PROGRAM = 2;
	
	/**
	 * _CLEAR integer argument indicating a Thread is to be deleted.
	 */
	public final static int CLEAR_THREAD = 3;
	
	/**
	 * _CLEAR integer argument indicating a MESSAGE binding is to be deleted.
	 */
	public final static int CLEAR_MESSAGE = 4;
	
	/**
	 * _CLEAR integer argument indicating a Lock is to be deleted.
	 */
	public final static int CLEAR_LOCK = 5;

	/**
	 * _CLEAR integer arguemnt indicating that there is a reference to a
	 * record on the stack and the string is a member to be deleted from
	 * that record.
	 */
	public static final int CLEAR_MEMBER = 6;
	
	/**
	 * Clear an item by name. The integer argument says what kind of object, and
	 * the string is the object name.
	 * <p>
	 * <list>
	 * <li>0 Symbol
	 * <li>1 Symbol (regardless of READONLY setting)
	 * <li>2 Program object
	 * <li>3 Hanging thread list
	 * <li>4 MESSAGE text
	 * <li>5 LOCK object </list>
	 * <p>
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		String name = null;
		if (env.instruction.stringValid)
			name = env.instruction.stringOperand.toUpperCase();

		/*
		 * Case 3 (CLEAR THREADS) has no name argument. Everything else does, so
		 * if we didn't get a name in the argument, get the name from the stack.
		 * This supports USING clauses.
		 */

		if ((env.instruction.integerOperand != 3) && (name == null))
			name = env.pop().getString().toUpperCase();
		
		switch (env.instruction.integerOperand) {

		case CLEAR_SYMBOL: /* Delete symbols if not readonly */
			final Value value1 = env.localSymbols.reference(name);
			if ((value1 == null) && (env.instruction.integerOperand == 0))
				throw new JBasicException(Status.UNKVAR, name);
			env.localSymbols.delete(name);
			break;

		case CLEAR_SYMBOL_ALWAYS:
			env.localSymbols.deleteAlways(name);
			break;

		case CLEAR_PROGRAM:
			final Program oldProgram = env.session.programs.find(name);

			if (oldProgram == null)
				throw new JBasicException( Status.PROGRAM, name);

			oldProgram.clear();
			break;
			
		case CLEAR_MESSAGE:
			int count = env.session.messageManager.removeMessage(name);
			if( count == 0 )
				throw new JBasicException(Status.NOMSG, name);
			break;
		
		case CLEAR_LOCK:
			Status s = LockManager.clear(env.session, name);
			if( s.failed())
				throw new JBasicException(s);
			
			break;

		case CLEAR_MEMBER:
			
			Value record = env.pop();
			if( record.getType() != Value.RECORD )
				throw new JBasicException(Status.EXPREC);
			record.removeElement(name);
			break;
		default:
			throw new JBasicException(Status.FAULT, 
				new Status(Status.INVOPARG, env.instruction.integerOperand));

		}
	}

}

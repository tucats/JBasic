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

import java.util.Iterator;

import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicDebugger;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * CHAIN operator. This scrubs the current symbol table of all symbols not
 * marked as being in the COMMON area.
 * 
 * @author tom
 * @version version 1.0 Feb 2007
 * 
 */
public class OpCHAIN extends AbstractOpcode {

	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * If there is an integer operand and it is non-zero then it tells
		 * us to pop the line number as an expression from the stack.  
		 * Otherwise we assume a starting line number of zero, which means
		 * start the chained program at the first line.
		 */
		int startingLineNumber = 0;
		boolean hasLineNumber = false;
		if( env.instruction.integerValid)
			hasLineNumber = (env.instruction.integerOperand > 0);
		if( hasLineNumber )
			startingLineNumber = env.pop().getInteger();
			
		/*
		 * Get the program object name as a string from the stack, and
		 * look up the program object.
		 */
		String targetProgramName = env.pop().getString();
		final Program newPgm = env.session.programs.find(targetProgramName);
		if (newPgm == null)
			throw new JBasicException(Status.PROGRAM, targetProgramName);

		/*
		 * Create a new symbol table for this instance of the program.  Because
		 * CHAIN is a co-routine call, the parent of the new table is the same
		 * as the parent of the current table.  During this process, we'll
		 * orphan the current table after copying out COMMON variables.
		 */
		final SymbolTable newTable = new SymbolTable(env.session, "Local to "
				+ newPgm.getName(), env.localSymbols.parentTable);

		/*
		 * Copy the argument list to a new ARG$LIST arrayValue
		 */
		final Value argArray = new Value(Value.ARRAY, "$ARGS");
		newTable.insertLocal("$ARGS", argArray);

		/*
		 * Copy COMMON variables from the program-of-origin.  Mark
		 * them as COMMON to this symbol table as well, since once
		 * declared as COMMON, the variable retains that attribute
		 * for all the tables that it is moved to.
		 */
		
		Iterator i = env.localSymbols.table.keySet().iterator();
		while( i.hasNext()) {
			String varName = (String)i.next();
			Value v = env.localSymbols.findReference(varName, false);
			if( env.localSymbols.isCommon(varName)) {
				newTable.insertLocal(varName, v);
				newTable.setCommon(varName);
			}
		}

		/*
		 * Set up additional ancestry information the program might want
		 */

		Value chainer = env.localSymbols.findReference("$THIS", false);
		if (chainer == null)
			chainer = new Value("Console");
		
		Value thisParent = env.localSymbols.findReference("$PARENT", false);
		if (thisParent == null)
			thisParent = new Value("Console");

		newTable.insertLocal("$PARENT", thisParent);
		newTable.insertLocal("$CHAINED_FROM", chainer);
		newTable.insertLocal("$THIS", new Value(newPgm.getName()));
		newTable.insertLocal("$MODE", new Value("CHAIN"));
		newTable.insertLocal("$START_TIME", new Value(System
				.currentTimeMillis()));

		/*
		 * See if we are under control of a debugger. If so, is STEP INTO
		 * active? If not then the next scope we call doesn't inherit the
		 * debugger.
		 */
		JBasicDebugger dbg = env.codeStream.debugger;
		if (dbg != null && (!dbg.stepInto()))
				dbg = null;

		/*
		 * CHAIN replaces the current program with the target, so make
		 * the new program the default current program before we do the
		 * invocation.  Use the starting line number we popped from the
		 * stack, else the default value of zero.
		 */
		
		env.session.programs.setCurrent(newPgm);
		Status returnStatus = newPgm.run(newTable, startingLineNumber, dbg);

		/*
		 * If the run went okay, then terminate execution... we don't
		 * allow control to return to the program that did the CHAIN.
		 */
		
		if (returnStatus.success()) {
			throw new JBasicException(Status.RETURN);
		}
		throw new JBasicException(returnStatus);

	}

}

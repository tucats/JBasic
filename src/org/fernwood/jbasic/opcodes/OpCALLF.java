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

import java.lang.reflect.InvocationTargetException;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Linkage;
import org.fernwood.jbasic.runtime.ArgumentList;
//import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Functions;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicDebugger;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpCALLF extends AbstractOpcode {

	/**
	 * <b><code>CALLF count, "name"</code><br><br></b>
	 * Execute the _CALLF instruction at runtime.  Pops arguments from the
	 * stack in reverse order and calls a function, which can be either a
	 * builtin function or a user-written function.
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
	 * @throws JBasicException  if there is an argument error or the function
	 * cannot be found.
	 *
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final Instruction instruction = env.instruction;
		final SymbolTable localSymbols = env.localSymbols;
		final JBasic session = env.session;

		final int argc = instruction.integerOperand;

		if( argc < 0 )
			throw new JBasicException(Status.FAULT, new Status(Status.INVOPARG, argc));
		
		/*
		 * Get the argument list from the stack.  Also, make sure any old
		 * value left hanging around has been dispatched.
		 */
		
		ArgumentList funcArgs = fetchArgs(env);
		env.localSymbols.deleteAlways("ARG$RESULT");
		
		/*
		 * See if we are under control of a debugger. If so, is STEP INTO
		 * active? If not then the next scope we call doesn't inherit the
		 * debugger.
		 */
		JBasicDebugger dbg = env.codeStream.debugger;
		if (dbg != null && (!dbg.stepInto()))
				dbg = null;

		/*
		 * Make the function call with the packaged argument list.
		 */

		String funcName = null;
		if( instruction.stringValid )
			funcName = instruction.stringOperand;
		else
			funcName = env.pop().getString().toUpperCase();
		
		/*
		 * One possibility is that this is a local function (implemented using
		 * the SUB statement) in the current program.  If so, then we need
		 * to address it differently.  Call it directly as a new program,
		 * using the starting byteCode address (expressed as a negative number
		 * to distinguish it from line numbers).  Also, in this case the caller
		 * must provide the symbol table already constructed, and $THIS is
		 * used to pass the actual name of the function to the callee.
		 */
		
		int entryAddress = env.codeStream.findLabel(Linkage.ENTRY_PREFIX + funcName);
		if( entryAddress > 0 ) {
			Program newPgm = env.codeStream.statement.program;
			
			SymbolTable parent = env.localSymbols;
			if( env.codeStream.fLocallyScoped)
				parent = new SymbolTable(session, "Temp Table " + JBasic.getUniqueID(), env.localSymbols.findGlobalTable());
			
			SymbolTable symbols = new SymbolTable(session, "Local to " + funcName,
					parent);
			symbols.insert(Linkage.ENTRY_PREFIX, funcName);
			Value v = Functions.callUserFunction(funcArgs, symbols, dbg, newPgm, -entryAddress);
			env.push(v);
			return;
		}
		/*
		 * Invoke the function. There are a number of Java arguments that translate
		 * into internal programmer errors such as incorrectly designated function 
		 * method names... report these to the programmer.  IF there was a more
		 * conventional error, report that to the user.
		 */
		
		Value value1 = null;
		try {
			value1 = Functions.invokeFunction(session, funcName, funcArgs, localSymbols,
					dbg);
		} catch (IllegalArgumentException e) {
			throw new JBasicException(Status.FAULT, "illegal argument exception");
		} catch (IllegalAccessException e) {
			throw new JBasicException(Status.FAULT, "illegal access exception");
		} catch (InvocationTargetException e) {
			throw (JBasicException) e.getCause();
		}
		
		if (value1 == null)
			throw new JBasicException(Status.EXPRESSION, new Status(Status.EXPRETVAL));
		
		env.codeStream.push(value1);

	}

}

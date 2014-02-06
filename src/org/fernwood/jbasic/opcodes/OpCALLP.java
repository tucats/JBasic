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
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Linkage;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicDebugger;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.ObjectValue;
import org.fernwood.jbasic.value.Value;

/**
 * <code>CALLP <em>argument-count</em> [<em>"name"</em>]</code>
 * <p>
 * Handles invoking another program, method, or Java method. If the entry name
 * is explicitly given in the instruction's string argument, it is used as
 * specified.  If there is no string argument, then the entry is popped off of
 * the stack.  If the value popped from the stack is a Java object, then a
 * method name is also popped from the stack, and a method call is made.  If
 * not a Java object wrapper, it is a string with the routine name.
 * <p>
 * The argument count also indicates execution mode for RUN versus CALL:
 * <p>
 * <table border="1">
 * <tr><td><code>CALLP -1</code></td> <td>Execute a program by name as a RUN command</td> <tr>
 * <tr><td><code>CALLP  x</code></td> <td>If x is >= 0, execute as a CALL command</td> <tr>
 * </table>
 * <p>
 * If an argument list must be popped from the stack,it is moved into an
 * ArgumentList object to pass to the underlying function or program name.
 * @author cole
 * @version 1.3 Feb 2009 Added support for Java wrapper objects.
 */
public class OpCALLP extends AbstractOpcode {

	/**
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final Instruction i = env.instruction;
		int argc = i.integerOperand;
		int ix;

		/*
		 * If we are here because of a RUN command, then the argument list will
		 * be negative. If so, remember it was a RUN and then zero out the
		 * argument list (RUN cannot pass arguments).
		 */

		boolean isRun = false;
		if (argc < 0) {
			isRun = true;
			argc = 0;
		}
		
		/*
		 * The argument list was pushed on the stack.
		 */

		ArgumentList funcArgs = fetchArgs(env);

		/*
		 * If the program name is given, we use it. Otherwise, we have to pop an
		 * item from the stack and use it as the string name. This supports the
		 * USING clause.  It's also how actual method calls to real Java objects
		 * are encoded.
		 */

		String targetProgramName = null;

		if (i.stringValid)
			targetProgramName = i.stringOperand;
		else {
			final Value pNameItem = env.codeStream.pop();
			
			/*
			 * It is *possible* that this is a Java object wrapper and
			 * not a string Value.  If so, we are doing a real method call
			 */
			
			if( pNameItem.isObject()) {
				callMethod(env, funcArgs, pNameItem);
				return;
			}
			
			/*
			 * Nope, not a real object, so proceed to construct a JBasic-style
			 * program invocation.
			 */
			if (pNameItem.getString().length() == 0 )
				throw new JBasicException(Status.NOPGM);

			targetProgramName = pNameItem.getString().toUpperCase();
		}

		/*
		 * The label can be in the current program, in which case we
		 * arrange to branch to the internal label as if it was a
		 * nested program with it's own execution context.  Otherwise,
		 * it's the name of an external program.  We check internal
		 * first.
		 */
		
		int entryAddress = env.codeStream.findLabel(Linkage.ENTRY_PREFIX + targetProgramName);
		Program newPgm = null;
		
		/*
		 * Look up the program object
		 */
	
		if( entryAddress > 0 )
			newPgm = env.codeStream.statement.program;
		
		else {
			if( targetProgramName.length() == 0 ) 
				throw new JBasicException(Status.NOACTIVEPGM);
			newPgm = env.session.programs.find(targetProgramName);
			if (newPgm == null)
				throw new JBasicException(Status.PROGRAM, targetProgramName);
		}
		
		/*
		 * Create a new symbol table for this instance of the program.
		 */
		
		SymbolTable parentTable = env.localSymbols;
		if( env.codeStream.fLocallyScoped)
			parentTable = new SymbolTable(env.session, "Temp Table " + JBasic.getUniqueID(), 
					env.localSymbols.findGlobalTable());		

		final SymbolTable newTable = new SymbolTable(env.session, "Local to "
				+ targetProgramName, parentTable);

		/*
		 * Copy the argument list to a new ARG$LIST arrayValue
		 */
		final Value argArray = new Value(Value.ARRAY, "$ARGS");
		for (ix = 0; ix < argc; ix++) {
			final Value item = funcArgs.element(ix);
			argArray.setElement(item, ix + 1);
		}
		newTable.insertLocal("$ARGS", argArray);

		/*
		 * If this is a METHOD call, then we make a reference in the local table
		 * to the actual invoking object, so the object can be modified in the
		 * method subroutine if needed.  We know this because the instruction is
		 * _CALLM, which is implemented in this method along with _CALLP.
		 */

		Value targetObject = null;
		if (i.opCode == ByteCode._CALLM) {
			targetObject = env.codeStream.pop();
			newTable.insertLocal("THIS", targetObject);
		}
		
		/*
		 * Set up additional ancestry information the program might want.
		 * If the current environment doesn't have a $THIS variable, assume
		 * the Console.
		 */

		Value parent = env.localSymbols.findReference("$THIS", false);
		if( parent == null ) {
			parent = new Value("Console");
		}
		
		String modeName = null;
		if( isRun )
			modeName = "RUN";
		else
			modeName = i.opCode == ByteCode._CALLM ? "METHOD" : "CALL";
			
		newTable.insertLocal("$PARENT", parent);
		newTable.insertLocal("$THIS", new Value(newPgm.getName()));
		newTable.insertLocal("$MODE", new Value(modeName));
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
		 * Run the program and see what happens. Any return processing will
		 * happen in byteCode after this instruction. If this CALLP is from a
		 * RUN command, then also make the target program the new current
		 * program.
		 * 
		 * Note that the entry address is passed as a negative number; this
		 * signals that it is a bytecode address rather than a line number.
		 */

		if (isRun)
			env.session.programs.setCurrent(newPgm);
		JBasic savedSession = newPgm.session();
		if( savedSession != null && savedSession != env.session ) {
			//env.session.stdout.println("UNEXPECTED CHANGE OF SESSIONS!");
			newPgm.setSession(env.session);
		}
		
		ByteCode newPgmExec = newPgm.getExecutable();
		if( newPgmExec != null )
			newPgmExec.setSession(env.session);
		
		final Status returnStatus = newPgm.run(newTable, -entryAddress, dbg);
		newPgm.setSession(savedSession);
		
		/*
		 * If the run went okay, then there might have been a result value in
		 * the caller's table. If so, then let's fetch it out now.
		 */
		if (returnStatus.success()) {
			Value resultValue;
			resultValue = newTable.localReference("ARG$RESULT");
			if( resultValue != null) {
				env.localSymbols.insertLocal("ARG$RESULT", resultValue);
			}
			else
				env.localSymbols.deleteAlways("ARG$RESULT");
		}
		else
			throw new JBasicException(returnStatus);
	}




	
	
	/**
	 * Given a value that contains Java object, call the underlying
	 * method by name.
	 * @param env the execution environment for this instruction
	 * @param funcArgs a previously-built function argument package.
	 * @param targetObject the Java object wrapper Value
	 * @throws JBasicException
	 */
	private void callMethod(final InstructionContext env,
			ArgumentList funcArgs, final Value targetObject)
			throws JBasicException {
		
		/*
		 * Convert the object to a wrapper object so we can access
		 * its' methods.
		 */
		ObjectValue obj = (ObjectValue) targetObject;
		
		/*
		 * Get the method name string from the stack and invoke 
		 * the method via the object wrapper.
		 */
		String methodName = env.pop().getString();
		Value r = obj.invokeMethod(methodName, funcArgs);
		
		/*
		 * If there was a result, store it in the ARG$RESULT reserved
		 * variable.
		 */
		if( r != null )
			env.localSymbols.insertLocal("ARG$RESULT", r);
		
		/*
		 * There will be a redundant copy of the object on the stack, based
		 * on the normal object call mechanisms, where a copy of the object
		 * is passed as a parameter to become the $THIS variable.  It isn't
		 * used in a Java object call, so toss it away.
		 */
		r = env.pop();
		
		return;
	}

}

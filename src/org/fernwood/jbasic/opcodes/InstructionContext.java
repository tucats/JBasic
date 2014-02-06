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
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * The OpEnv contains the current execution environment for bytecode operations.
 * This contains copies of the current JBasic container object, active symbol
 * tables, etc. This is passed as the parameter to all OpCode execution methods
 * to help them get context information when needed.
 * 
 * @author tom
 * @version version 1.0 Aug 15, 2006
 * 
 */
public class InstructionContext {
	
	/**
	 * The code stream being executed.
	 */
	ByteCode codeStream;

	/**
	 * The JBasic session containing the executing code stream.
	 */
	JBasic session;

	/**
	 * The most-local symbol table for the current instruction steram.
	 */
	SymbolTable localSymbols;

	/**
	 * The current instruction being executed.
	 */
	Instruction instruction;

	/**
	 * True if statement tracing is on.  This is only used when _STMT
	 * opcodes are executed.
	 */
	boolean fStatementTrace;

	/**
	 * The argument list for the current instruction; only used for some
	 * specific cases where argument lists are rendered as an array of
	 * Values.
	 */
	Value argList;

	/**
	 * Return the active argument list array value for this exectuion block.
	 * @return a Value that is the argument array, or null if there is no
	 * argument array.
	 */
	public Value getArgList() {
		if( argList == null ) {
			argList = this.localSymbols.localReference("$ARGS");
			if( argList == null ) {
				argList = new Value(Value.ARRAY, null);
				try {
					localSymbols.insert("$ARGS", argList);
				} catch (JBasicException e) {
					e.print(this.session);
					return null;
				}
			}
		}
		return argList;
	}
		
	/**
	 * Create a bytecode Operation Environment object.
	 * 
	 * @param e
	 *            The JBasic environment that contains the current session.
	 * @param b
	 *            The ByteCode stream which is used for branch handling, etc.
	 * @param x
	 *            The Instruction sequence object being executed, which contains
	 *            parameters, etc.
	 * @param t
	 *            The active symbol table for dynamic binding
	 * @param trace
	 *            A flag indicating if tracing of this execution is required.
	 */
	public InstructionContext(final JBasic e, final ByteCode b, final Instruction x,
			final SymbolTable t, final boolean trace) {
		codeStream = b;
		session = e;
		localSymbols = t;
		instruction = x;
		fStatementTrace = trace;
		argList = null;
	}

	/**
	 * Pop a Value from the top of the runtime stack.  The stack size is
	 * reduced by one when this is returned.  This is always the actual
	 * object on the stack, not a copy.
	 * @return the Value that was on the top of the stack.
	 * @throws JBasicException if a stack undeflow occurs
	 */
	Value pop() throws JBasicException {
		return codeStream.pop();
	}

	/**
	 * Pop an item from the stack, with the intention of using it to write
	 * an updated value.  Because some items on the stack are references
	 * to symbols, if this is one we have to make a copy of it now, so the
	 * original symbol value is not changed by the update operation(s).  Only
	 * _STORE operations are allowed to modify symbolic values.
	 * 
	 * @return a value that is suitable for update-in-place
	 * @throws JBasicException
	 */
	Value popForUpdate() throws JBasicException {
		Value v = codeStream.pop();
				
		if( v.fSymbol )
			return v.copy();
		
		return v;
	}

	/**
	 * Determine the current size of the runtime stack.
	 * @return an integer indicating the number of Values that are on the
	 * current runtime stack.
	 */
	int stackSize() {
		return codeStream.stackSize();
	}

	/**
	 * Get a specific element from the stack, without disturbing the stack.
	 * @param ix the zero-based index on the stack to get; i.e. 0 is the 
	 * bottom-most stack item, and stackSize()-1 is the top-most item
	 * @return
	 */
	Value get(final int ix) {
		return codeStream.getStackElement(ix);
	}

	/**
	 * Push a value on the stack
	 * @param v the value to push.
	 */
	void push(final Value v) {
		codeStream.push(v);
	}

	/**
	 * Push an integer value on to the runtime stack.
	 * @param i the integer value to push.
	 */
	void push(final int i) {
		codeStream.push(i);
	}

	/**
	 * Push a String value on to the runtime stack.
	 * @param i the string value to push.
	 */	
	void push(final String s) {
		 codeStream.push(s);
	 }

	 /**
	  * Push a double value on to the runtime stack.
	  * @param i the double value to push.
	  */
	 void push(final double d) {
		  codeStream.push(d);
	 }

	  /**
	   * Push a boolean value on to the runtime stack.
	   * @param i the boolean value to push.
	   */
	 void push(final boolean b) {
		   codeStream.push(b);
	 }

	/**
	 * Bind an instruction to this runtime environment.  This is used to
	 * make a reference to the instruction being executed that can be passed
	 * into each OpCode as it executes, to fetch its arguments, etc.
	 * @param newInstruction a reference to the next instruction to be executed
	 * using this OpcodeEnvironment.
	 */
	public void setInstruction(Instruction newInstruction) {
		instruction = newInstruction;
	}

	/**
	 * Does the current runtime bytecode stream ahve static or dynamic typing?
	 * 
	 * @return true if static (strong) typing is enabled.
	 */
	public boolean hasStaticTyping() {
		if (codeStream.statement != null)
			if (codeStream.statement.program != null)
				return codeStream.statement.program.fStaticTyping;
		return session.getBoolean("SYS$STATIC_TYPES");
	}

	/**
	 * Determine if there is an active valid program code stream being run.
	 * If not, then throw the NOACTIVEPGM error.
	 * 
	 * @throws JBasicException There is no active program
	 */
	public void checkActive() throws JBasicException {
		if (codeStream.statement == null)
			throw new JBasicException(Status.NOACTIVEPGM);
		if (codeStream.statement.program == null)
			throw new JBasicException(Status.NOACTIVEPGM);
		
	}

}

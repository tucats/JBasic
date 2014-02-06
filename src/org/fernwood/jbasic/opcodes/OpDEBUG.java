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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicDebugger;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <code>DEBUG <em>code</em> [<em>"name"</em>]</code>
 * <p>
 * This opcode handles a variety of debugging chores.  Some are directly
 * related to the user-level debugger.  Others are used to help debug
 * JBasic itself.  Some of the debugger statements are compiled into this
 * opcode, while other versions are intended for inclusion in ASM streams
 * being debugged by the user or the JBasic development team.
 * <p>
 * The integer operand code defines the specific sub-function
 * operation to be performed.
 * <p>
 * <table border="1">
 * <tr><td><code>&nbsp;0 </code></td> <td>Execute a user debugger <code>STEP</code> operation</td> <tr>
 * <tr><td><code>&nbsp;1 </code></td> <td>Execute a user debugger <code>STEP INTO</code> operation</td> <tr>
 * <tr><td><code>&nbsp;2 </code></td> <td>Print the entire current runtime stack</td> <tr>
 * <tr><td><code>&nbsp;3 </code></td> <td>Print the top stack item (without removing it)</td> <tr>
 * <tr><td><code>&nbsp;4 </code></td> <td>Print the TOS "deeply" without removing it</td> <tr>
 * <tr><td><code>&nbsp;5 </code></td> <td>Print a symbol (<em>string argument</em>) deeply</td> <tr>
 * </table>
 * <p>
 * All output is directed to the current session's "stdout" file object. The term
 * "deeply" here refers to whether complex objects (arrays or records) are dumped
 * out recursively so each member or element is also dumped.  A shallow print
 * (code=3) only prints the Value object itself, with a formatted output.  A
 * deep print recurses over the contained Value objects and prints them as well.
 * This is used to determine if references versus copies are being made in array
 * elements, for example.
 * <p>
 * The printout includes the Java hash ID of the value.  This is nearly always
 * unique but isn't guaranteed by Java to always be unique.  You can usually
 * use it to determine if an object is a unique value or a reference to another
 * value, but there may be occasional "false indicators" due to hash collisions.
 * <p>
 * When the value being printed is a Java wrapper object, two hash values are
 * printed.  The first is the hash id for the Value being used by JBasic.  The
 * second hash id is the actual underlying Java object's hash id.  Note that
 * when a wrapper object is copied, the reference is copied but not the underlying
 * Java object itself.
 * <p>
 * 
 * @author cole
 * @version 1.0 Dec 2007 Initial creation
 * @version 1.2 Sep 2009 Added support for printing stack objects in detail
 */
public class OpDEBUG extends AbstractOpcode {

	/**
	 * Instruction opcode for _DEBUG meaning that a step OVER is to be done.
	 */
	public static final int STEP = 0;

	/**
	 * Instruction opcode for _DEBUG meaning a STEP INTO is to be done.
	 */
	public static final int STEP_INTO = 1;

	/**
	 * Instruction opcode for _DEBUG meaning a stack dump is to be printed.
	 */
	public static final int PRINT_STACK = 2;

	/**
	 * Instruction opcode for _DEBUG meaning to print the object on top of the stack.
	 */
	public static final int PRINT_OBJECT = 3;

	/**
	 * Instruction opcode for _DEBUG meaning to print the object on top of the stack,
	 * with deep recursion as needed.
	 */
	public static final int PRINT_DEEP_OBJECT = 4;

	/**
	 * Instruction opcode for _DEBUG meaning to print the object referenced by
	 * the symbol named in the string argument,
	 * with deep recursion as needed.
	 */

	public static final int PRINT_SYMBOL_DATA = 5;
	
	/*
	 * Execute the _DEBUG opcode
	 * 
	 * @see org.fernwood.jbasic.opcodes.OpCode#execute(org.fernwood.jbasic.opcodes.OpEnv)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		JBasicDebugger dbg = null;
		Value v = null;
		
		switch (env.instruction.integerOperand) {

		case STEP_INTO:
			dbg = new JBasicDebugger(env.session.stdin(), env.session.stdout);
			dbg.stepInto(true);
			/* Fall through to STEP now */

		case STEP:
			if (env.codeStream.debugger == null)
				env.codeStream.debugger = dbg;
			break;

		case PRINT_STACK:
			if( env.codeStream.dataStack == null )
				env.session.stdout.println("NO RUNTIME STACK AVAILABLE");
			else {
				int count = env.codeStream.dataStack.size();
				if( count == 0 ) 
					env.session.stdout.println("RUNTIME STACK IS EMPTY");
				else {
					env.session.stdout.println("RUNTIME STACK (SIZE=" + count + "):");
					for( int i = 0; i < count; i++ ) {
						Value element = env.codeStream.dataStack.get(i);
						String idx = null;
						if( i == 0 )
							idx = "(TOP)0";
						else
							idx = ("     " + Integer.toString(i));
						idx = idx.substring(idx.length()-6);
						idx = idx + ": " + Utility.pad(Value.typeToName(element.getType()), 9) + " ";
						env.session.stdout.println(idx + Value.toString(element, true));
					}
				}
			}
			break;

		case PRINT_OBJECT:
			printStackObject(env.session.stdout, env.codeStream);
			break;
			
		case PRINT_DEEP_OBJECT:
			int stackSize = env.codeStream.stackSize();
			if( stackSize < 1 )
				env.session.stdout.println("No stack object to print");
			v = env.codeStream.getStackElement(stackSize - 1);
			printDeepObject(env.session.stdout, null, v, 0);

			break;
		
		case PRINT_SYMBOL_DATA:
			String name = env.pop().getString().toUpperCase();
			SymbolTable table = env.localSymbols.findTableContaining(name);
			if( table == null )
				throw new JBasicException(Status.UNKVAR, name);
			v = env.localSymbols.findReference(name, false);
			env.session.stdout.println("Symbol " + name + " is in " + table);
			printDeepObject(env.session.stdout, null, v, 0);
			
			break;

		default:
			throw new JBasicException(Status.FAULT, 
					new Status(Status.INVOPARG, env.instruction.integerOperand));
		}

		return;
	}



	/**
	 * Print the top of stack in such a way that it is unambiguously identified.
	 * 
	 * @param basicFile the JBasic output file to direct the output to. Usually the
	 * session's stdout file.
	 * @param codeStream  the active bytecode stream being executed, which
	 * gives access to the runtime stack.
	 * @return the value that was printed. 
	 */
	public static Value printStackObject(JBasicFile basicFile, ByteCode codeStream) {
		int stackSize = codeStream.stackSize();
		StringBuffer message = new StringBuffer("Stack element");
		message.append('[');
		message.append(Utility.pad(Integer.toString(stackSize), -3));
		message.append("] ");
		Value v = null;
		if( stackSize < 1 ) {
			message.append(" - no object on stack!");
		}
		else {
			v = codeStream.getStackElement(stackSize - 1);
			

			message.append(objectHashData(null, v, 0));
		}
		
		basicFile.println(message.toString());
		return v;
	}

	/**
	 * @param message
	 * @param v
	 */
	private static String objectHashData(String prefix, Value v, int depth) {
		StringBuffer message = new StringBuffer();
		if( depth > 0 )
			message.append(Utility.pad(" ", depth*2));
		if( prefix != null )
			message.append(prefix);
		message.append(v.getObjectClass().toString());
		message.append(" object(");
		message.append(Integer.toHexString(System.identityHashCode(v)));
		if( v.isObject()) {
			message.append(", ");
			message.append(Integer.toHexString(System.identityHashCode(v.getObject())));
		}
		message.append(") = ");
		
		message.append(v.toString());
		message.append("   ");
		
		StringBuffer codes = new StringBuffer("<");
		if( v.fCommon )
			codes.append("common");
		if( v.fReadonly) {
			if( codes.length() > 1)
				codes.append(",");
			codes.append("readonly");
		}
		if( v.fSymbol) {
			if( codes.length() > 1 )
				codes.append(",");
			codes.append("symbol");
		}
		if( codes.length() > 1 ) {
			message.append(codes);
			message.append('>');
		}
		return message.toString();
	}


	private static void printDeepObject( JBasicFile f, String prefix, Value v, int depth ) {
		
		f.println( objectHashData(prefix, v, depth));
		int vType = v.getType();
		if( vType == Value.ARRAY) {
			int len = v.size();
			for( int idx = 1; idx <= len; idx++ )
				printDeepObject(f, "[" + idx + "]", v.getElement(idx), depth+1);
		}
		else if( vType == Value.RECORD) {
			Iterator i = v.getIterator();
			if( i != null )
				while( i.hasNext()) {
					String name = (String) i.next();
					Value e = v.getElement(name);
					printDeepObject(f, name + ": ", e, depth+1);
				}
		}
	}
	
}

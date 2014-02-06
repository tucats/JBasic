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
package org.fernwood.jbasic.compiler;

import java.lang.reflect.InvocationTargetException;
import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.statements.Statement;
import org.fernwood.jbasic.compiler.CompileContext;
import org.fernwood.jbasic.funcs.JBasicFunction;

/**
 * Compiler support for built-in functions. Some functions (like QUOTE("A")) are
 * easily implemented as in-line functions in the ByteCode streams, rather than by
 * making runtime calls to relatively simple JBasic or built-in functions.
 * <p>
 * 
 * This is done by using reflection-based lookup to see if, for any given
 * function named "foo", there is a method called compile() which will take
 * responsibility for compiling the function as an in-line.
 * <p>
 * 
 * If the function exists, it must assume that the ByteCode stream already has
 * all the code necessary to push the function arguments on the stack (in
 * <em>reverse</em> order from the way they were declared!) and can emit the
 * remaining logic necessary to perform the intrinsic function.
 * <p>
 * 
 * If a compile module does not exist, or determines that it cannot compile the
 * Intrinsic after all, it just returns any non-success code. This causes the
 * expression compiler to emit a _CALLF code which resolves the issue at
 * runtime.
 * <p>
 * These compile() methods are found in each individual function located in the 
 * org.fernwood.jbasic.funcs package.
 * 
 * @author tom
 * 
 */
public class CompileFunction {


	/**
	 * Given a function name (and an argument list that has already been parsed
	 * and generated code to put them on the stack), see if this function can be
	 * simplified as a BUILTIN function. That is, can we generate in-line code
	 * that will support this function? If not, then a traditional _CALLF
	 * ByteCode is generated and the function is located at runtime.
	 * 
	 * @param session
	 *            The instance of JBasic which hosts the global symbol table,
	 *            which is used to resolve certain debug flags.
	 * @param name
	 *            The name of the function to compile a builtin invocation for.
	 * @param argc
	 *            The count of arguments that were pushed on the stack as part
	 *            of the function call code generation.
	 * @param b
	 *            The ByteCode vector in which we are generating code.
	 * @param fConstantArgs true if all arguments were generated as constant values.
	 * @param argGeneratedPosition the position in the bytecode where the argument
	 * data is generated.  This is helpful when the arguments are constant, because the
	 * function compiler can "scoop" them up if needed.
	 * @return a Status valid indicating if the function could be compiled as a
	 *         builtin function. If not, then the return code is UNKFUNC.
	 */
	static Status invokeFunctionCompiler(final JBasic session, final String name,
			final int argc, final ByteCode b, boolean fConstantArgs, int argGeneratedPosition) {

		/*
		 * Special case for JBasic which throws the wrong kind of error
		 * since it's a shell of a class.
		 */
		
		if( name.equals("JBASIC"))
			return new Status(Status.UNKFUNC, "JBASIC");
		
		/*
		 * Let's give the in-line function a chance to compile itself, if
		 * it wants to or can.  If there is no such function, or there is
		 * no compile() method for the given function, then this routine
		 * throws an UNKFUNC JBasic exception.
		 */
		Status sts = new Status();
		final CompileContext work = new CompileContext(name, argc, b, fConstantArgs);
		work.setArgPosition(argGeneratedPosition);

		try {
			final Class c = Class.forName("org.fernwood.jbasic.funcs."
					+ Statement.verbForm(name) + "Function");

			sts = ((JBasicFunction)(c.newInstance())).compile(work);
			
		} catch (final Exception e) {
			
			/* Map a few common Java errors into the JBasic signal */
			if( e.getClass() == NoSuchMethodException.class )
				return sts = new Status(Status.UNKFUNC, name);
			if( e.getClass() == ClassNotFoundException.class )
				return sts = new Status(Status.UNKFUNC, name);
			if( e.getClass() == InvocationTargetException.class ) {
				JBasicException eb = (JBasicException) e.getCause();
				return eb.getStatus();
			}
			if( e.getClass() == JBasicException.class) {
				JBasicException eb = (JBasicException)e;
				return eb.getStatus();	
			}
			/* I do not know what is going on.  Panic. */
			e.printStackTrace();
		}
		return sts;
	}

	
}

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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;

/**
 * This class stores information about a compilation unit, which describes
 * information to be passed to each in-line function compiler.
 * <p>
 * When the compiler needs support compiling a portion of a statement, it creates
 * a compilation unit.  Currently, the only time this is used is for supporting
 * compilation of function calls which might be implemented as in-line functions, 
 * though the use of compilation units in the future will support allowing user-
 * written extensions to the language at compile time.
 * <p>
 * The compile context defines the work to be done (the name of the function call)
 * and a pointer to the compiled bytecode stream.  At the time this is called, the
 * bytecode stream already has code generated that puts the function calls on the
 * stack in the reverse order they occurred in the statement (that is, the last
 * function argument is on top of the stack).
 * <p>
 * The compile support functions can then choose to generate code to directly support
 * the function by adding additional instructions to the bytecode stream, or they
 * can elect to let the default compilation operation occur, which is to generate 
 * code for a runtime call to the function interpreter.
 * <p>
 * All the information in a compile context could be passed as parameters to the
 * function compilation units, but by encapsulating them in an object, the context
 * can be extended for other purposes without breaking the functions or requiring
 * parameter changes.
 * 
 * @author tom
 * @version version 1.0 Feb 20, 2006
 * 
 */

public class CompileContext {

	/**
	 * The name of the function being called.
	 */
	public String name;

	/**
	 * The instruction stream to add the code for the in-line function.
	 */
	public ByteCode byteCode;

	/**
	 * The number of arguments that are passed in the function call. At the time
	 * the intrinsic builtin function is compiled, the instructions to place the
	 * arguments on the stack has already been generated.
	 */
	public int argumentCount;

	/**
	 * True if all arguments in the compilation unit are constants.
	 */
	public boolean constantArguments;

	/**
	 * The location in the code stream where the current argument list was 
	 * created.
	 */
	public int argPosition;

	/**
	 * Constructor to create a new CompileContext object.
	 * @param theName the name of the in-line function being compiled.
	 * @param theArgCount the number of arguments that will are available to the
	 * in-line function.
	 * @param theByteCode the byte code object in which the in-line function call will
	 * be compiled.
	 * @param fConstantArgs a flag indicating if all the arguments on the stack are
	 * constants.  This information might be of use to the compiler which could
	 * choose to optimize the results at compile time.
	 */
	public CompileContext(final String theName, final int theArgCount, final ByteCode theByteCode, boolean fConstantArgs) {
		name = theName;
		byteCode = theByteCode;
		argumentCount = theArgCount;
		constantArguments = fConstantArgs;
	}

	/**
	 * Determine if this compilation unit has valid arguments.  At this point,
	 * we only know the number of arguments, so we test for those. The type of
	 * the arguments is not known at compile time.
	 * @param minArgs the minimum number of arguments the function can accept.
	 * @param maxArgs the maximum number of arguments the function will accept.
	 * @throws JBasicException containing Status indicating if there were
	 * INSFARGS or TOOMANYARGS
	 */
	public void validate(int minArgs, int maxArgs) throws JBasicException {
		if( argumentCount < minArgs)
			throw new JBasicException(Status.INSFARGS);
		if( argumentCount > maxArgs)
			throw new JBasicException(Status.TOOMANYARGS);
		
	}

	/**
	 * Define where the argument data was generated for a function call in the
	 * current code stream.  This is used by function compilers when the argument
	 * data is all constant to perform certain optimizations.
	 * @param argGeneratedPosition the location in the code stream where the
	 * argument list is constructed.
	 */
	public void setArgPosition(int argGeneratedPosition) {
		argPosition = argGeneratedPosition;
		
	}
}

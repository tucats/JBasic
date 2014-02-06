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
package org.fernwood.jbasic.statements;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Declaration;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * COMMON Statement handler.
 * 
 * The COMMON statement declares a variable and gives it a type. This is used
 * to define symbols that are to be marked as being in the "common" symbol
 * area, which can be shared with a chained program. <br>
 * <br>
 * <code>COMMON X(10) AS INTEGER</code> <br>
 * <br>
 * 
 * This statement creates an array with ten elements, each of type <b>INTEGER</b>.
 * The array is initialized to the default value for the type, so in this case
 * it is an array of zeroes. <br>
 * <br>
 * The type value must be one of <b>ARRAY</b>, <b>STRING</b>, <b>DOUBLE</b>,
 * or <b>INTEGER</b>. <br>
 * <br>
 * If you omit the size value in parenthesis, then a simple variable is created
 * of the given type. <br>
 * <br>
 * <code>COMMON FLAG AS BOOLEAN</code> <br>
 * <br>
 * This creates a variable FLAG of type boolean, and sets it to false by
 * default. You can specify more than one variable in a single COMMON statement by
 * using a comma: <br>
 * <br>
 * <code>COMMON A AS INTEGER, B AS STRING, C(10) AS BOOLEAN</code> <br>
 * <br>
 * This creates three variables: an integer "A", a string "B", and an array of
 * boolean values called "C".<br>
 * <br>
 * This statement is functionally the same as a DIM statement except that it
 * sets the isCommon() attribute on each item that it creates.  In fact the
 * bulk of the compilation work is done by the parseDeclaration() method in the
 * DIM statement handler.
 * @author tom
 * 
 */
public class CommonStatement extends Statement {

	/**
	 * Compile 'COMMON' statement. Processes a token stream, and compiles it into a
	 * byte-code stream associated with the statement object. The first token in
	 * the input stream has already been removed, since it was the "verb" that
	 * told us what kind of statement object to create.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the source to
	 *            compile.
	 * @return A Status value that indicates if the compilation was successful.
	 *         Compile errors are almost always syntax errors in the input
	 *         stream. When a compile error is returned, the byte-code stream is
	 *         invalid.
	 */

	public Status compile(final Tokenizer tokens) {

		if( tokens.endOfStatement())
			return status = new Status(Status.EXPVARS);
		
		/*
		 * Generate a bytecode stream for this statement, then use the general
		 * declartion compiler.  The final flag indicate that this is COMMON
		 * code, and adds extra instructions to mark the variables created as
		 * having the "COMMON" attribute in the symbol table.
		 */
		byteCode = new ByteCode(session, this);
		return status = Declaration.compile(byteCode,tokens,Declaration.COMMON);

		//return status = DimStatement.compileDeclarations(byteCode, tokens, true);
	}
}

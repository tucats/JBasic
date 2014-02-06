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
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.compiler.TypeCompiler;
import org.fernwood.jbasic.value.Value;

/**
 * ARRAY statement. Declares one or more variables of the given type
 * and assigns values as needed.
 * <p>
 * The syntax of the ARRAY statement is:
 * <p>
 * <code>
 *       ARRAY <em>variable</em> [=<em>expression</em>] [, <em>variable</em> [=<em>expression</em>]...]
 * </code>
 * The variables must include an array dimension specification in square brackets, indicating the
 * maximum array index value permitted.  Only a single dimension can be specified in the ARRAY
 * statement; use the DIM statement for more complex declaration capabilities.
 * <p>
 * When provided, an initializer expression can be given. When present, each member of the array
 * is initialized to the given value (and takes on it's type). When no initializer is present,
 * the array is filled with arrays containing no elements.
 * <code>
 *      ARRAY X[3]
 * </code>
 * results in a variable x with value [[],[],[]]
 * 
 * 
 * @author tom
 * @version version 1.0 Aug 2007
 */

class ArrayStatement extends Statement {

	/**
	 * Compile 'ARRAY' statement. Processes a token stream, and compiles it into a
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

		/*
		 * Call the general-purpose Type compiler to generate code for
		 * this statement that declares one or more variables of type
		 * ARRAY.
		 */

		return TypeCompiler.compile( session, this, tokens, Value.ARRAY );
		
	}

}

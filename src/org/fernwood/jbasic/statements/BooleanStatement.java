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
 * BOOLEAN statement. Declares one or more variables of the given type
 * and assigns values as needed.
 * <p>
 * The syntax of the BOOLEAN statement is:
 * <p>
 * <code>
 *       BOOLEAN <em>variable</em> [=<em>expression</em>] [, <em>variable</em> [=<em>expression</em>]...]
 * </code>
 * <p>
 * If more than one variable is declared, separate them with a comma.  Each one 
 * can be optionally initialized with an expression, which is evaluated and
 * converted to a BOOLEAN value and stored in the newly-created variable(s).
 * 
 * @author tom
 * @version version 1.0 Aug 2007
 */

class BooleanStatement extends Statement {

	/**
	 * Compile 'BOOLEAN' statement. Processes a token stream, and compiles it into a
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
		 * BOOLEAN.
		 */

		return TypeCompiler.compile( session, this, tokens, Value.BOOLEAN );
		
	}

}

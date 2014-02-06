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
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * LET statement. Parses and expression and creates a symbol that reflects its
 * value.
 * <p>
 * The syntax of the LET statement is
 * <p>
 * <code>
 *
 *            LET <em>variable</em> [<em>class]</em> = <em>expression</em>
 * 
 * </code>
 * <p>
 * The variable can be of any type, and will be created as needed. The type is
 * coerced to match the type of the expression, so a string expression such as
 * <code>"a" + "b"</code> will result in a string variable containing "ab",
 * whereas an expression like <code>SUM(3,5)/2</code> will result in the
 * floating point value of 4.0.
 * <p>
 * You can specify the <em>class</em> of a variable, which means providing
 * additional information about how the variable is to be stored. Class
 * information must be enclosed in angle-brackets, and consists of a list of
 * keywords separated by commas. The valid keywords are:
 * <p>
 * <list>
 * <li> GLOBAL - the symbol is to be stored in the global symbol table
 * <li> LOCAL - the symbol is to be stored in the local table. This is the
 * default.
 * <li> PARENT - the symbol is to be stored in the symbol table of the caller.
 * <li> READONLY - the symbol is to be marked as read-only and cannot be deleted
 * or set to a different value after it is created. </list>
 * <p><br>
 * Note that only one of GLOBAL, LOCAL, or PARENT can be specified in a class
 * declaration. A symbol that starts with a "$" character is always marked as
 * readonly, and a symbol that starts with "SYS$" is always stored in the global
 * table.
 * <p><br>
 * A single LET statement can actually make multiple assignments, by separating
 * them by commas.  For example,<p>
 * <code><br>
 *     LET X=1, NAME="BOB", RANGES=[1,2,3]
 * </code><br>
 * <p><br>
 * 
 * @author tom
 * @version version 1.0 June 24, 2004
 * @see Value
 * @see SymbolTable
 */

class LetStatement extends Statement {

	/**
	 * Compile 'IF' statement. Processes a token stream, and compiles it into a
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
		 * Prepare the statement's generated code, and then parse assignments
		 * in a loop.
		 */
		
		byteCode = new ByteCode(session, this);
		status = new Status();
		
		while (true) {
			/*
			 * Parse the LValue
			 */
			final LValue destination = new LValue(session, strongTyping());
			destination.allowChaining(true);
			
			Status status = destination.compileLValue(byteCode, tokens);
			if (status.failed())
				return status;

			/*
			 * Require an assignment operator. The default is "=" but we also
			 * allow "<-" for compatibility with "Calculator" BASICs
			 */
			if (!tokens.assumeNextSpecial(new String[] { "=", "<-" }))
				return new Status(Status.ASSIGNMENT);

			final Expression exp = new Expression(session);
			status = exp.compile(byteCode, tokens);
			if (status.failed())
				return status;

			/*
			 * Allow a trailing data type setting
			 */
			
			if( tokens.assumeNextToken("AS")) {
				String dataType = tokens.nextToken();
				if( !tokens.isIdentifier())
					return new Status(Status.BADTYPE, dataType);
				int cvtType = Value.nameToType(dataType);
				if( cvtType == Value.UNDEFINED || cvtType == Value.ARRAY)
					return new Status(Status.BADTYPE, dataType);
				byteCode.add(ByteCode._CVT, cvtType);
			}
			if( destination.isChained())
				byteCode.add(ByteCode._DECOMP, destination.count());
			
			status = destination.compileStore();

			if (status.failed())
				return status;
			if (!tokens.assumeNextSpecial(","))
				break;
		}
		return status;

	}

}

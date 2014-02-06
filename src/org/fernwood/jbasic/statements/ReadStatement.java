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
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.value.Value;

/**
 * Input one or more variables from a DATA statement stored somewhere in the
 * program.
 * <p>
 * <code>
 * READ var1 [, varn...]
 * </code>
 * <p>
 * 
 * @author tom
 * @version version 1.0 Mar 2006
 * 
 */

class ReadStatement extends Statement {

	/**
	 * Compile 'READ' statement. Processes a token stream, and compiles it into
	 * a byte-code stream associated with the statement object. The first token
	 * in the input stream has already been removed, since it was the "verb"
	 * that told us what kind of statement object to create.
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
		if (program == null)
			return new Status(Status.NOACTIVEPGM);
		byteCode = new ByteCode(session, this);
		int count = 0;
		while (true) {

			/*
			 * Process the variable name to which we will write.
			 */
			final LValue nextInputVariable = new LValue(session, strongTyping());
			nextInputVariable.compileLValue(byteCode, tokens);

			if (nextInputVariable.error)
				return new Status(Status.INPUTERR, "invalid lvalue");

			/*
			 * Generate bytecode to read the DATA element
			 */
			
			byteCode.add(ByteCode._READ);
			count++;
			
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

			/*
			 * Store the datum away.
			 */
			nextInputVariable.compileStore();

			if (!tokens.assumeNextToken(","))
				break;
		}

		if( count == 0 )
			return new Status(Status.EXPVARS);
		return new Status(Status.SUCCESS);

	}
}
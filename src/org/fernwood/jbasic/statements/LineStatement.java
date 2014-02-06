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
import org.fernwood.jbasic.compiler.FileParse;
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.value.Value;

/**
 * Input a line of text into a single variable. The syntax is:
 * <p>
 * <code>
 * LINE INPUT [<em>"prompt expression"</em>,] <em>variable</em>
 * </code>
 * <p>
 * The prompt expression is used if give, else a default input prompt is used
 * from the variable SYS$INPUT_PROMPT. The value is considered an lvalue and are
 * processed as such.
 * <p>
 * A LINE INPUT statement can also read from a file instead of the console,
 * using
 * <p>
 * <code>
 * LINE INPUT FILE <em>identifier</em>, <em>variable</em>
 * </code>
 * <p>
 * The <em>identifier</em> is a file identifier, typically from an OPEN
 * statement. A prompt string cannot be used with a FILE mode operation. An
 * entire line is read from the input file and stored in the named variable.
 * 
 * @author tom
 * @version version 1.0 Jun 24, 2004
 * 
 */

class LineStatement extends Statement {

	/**
	 * Compile 'LINE INPUT' statement. Processes a token stream, and compiles it
	 * into a byte-code stream associated with the statement object. The first
	 * token in the input stream has already been removed, since it was the
	 * "verb" that told us what kind of statement object to create.
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

		// Require the INPUT term next.

		if (!tokens.assumeNextToken("INPUT"))
			return new Status(Status.SYNEXPTOK, "INPUT");
		
		/*
		 * Initialize the generated code for this statement and get
		 * ready to parse clauses.
		 */
		byteCode = new ByteCode(session, this);

		int hasFile = 0;
		boolean hasPrompt = false;
		String promptString = null;

		/*
		 * See if there is a FILE <ident> clause.
		 */

		final FileParse f = new FileParse(tokens, true);
		if (f.success()) {
			f.generate(byteCode);
			if (!tokens.assumeNextToken(new String[] {";", ","}))
				return new Status(Status.FILECOMMA);

			hasFile = 1;

		} else {

			/*
			 * If not a FILE <ident> then we would allow a prompt string.
			 */

			promptString = tokens.nextToken();
			if (tokens.getType() != Tokenizer.STRING)
				tokens.restoreToken();
			else {
				hasPrompt = true;
				if (!tokens.assumeNextToken(new String[] {";", ","}))
					return new Status(Status.INPUTERR,
						new Status(Status.SYNEXPTOK, "\",\""));
			}
		}
		/*
		 * Parse the LValue
		 */
		final LValue destination = new LValue(session, strongTyping());
		final Status status = destination.compileLValue(byteCode, tokens);
		if (status.failed())
			return status;
		
		if (hasPrompt)
			byteCode.add(ByteCode._LINE, hasFile, promptString);
		else
			byteCode.add(ByteCode._LINE, hasFile);

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
		 * Now generate the code to store the result.
		 */
		return destination.compileStore();
	}

}
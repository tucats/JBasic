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
import org.fernwood.jbasic.compiler.FileParse;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * Position a binary file to an arbitrary location in the file.
 * <p>
 * <code>
 * SEEK FILE <em>identifier</em>, <em>integer-expression</em>
 * </code>
 * <p>
 * The <em>integer-expression</em> defines where in the file to position the
 * next file pointer.
 * <p>
 * 
 * @author tom
 * @version version 1.0 May 27, 2006
 * 
 */

class SeekStatement extends Statement {

	static int sequenceNumber = 0;

	/**
	 * Compile 'SEEK' statement. Processes a token stream, and compiles it into
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

		/*
		 * Set up tokenization and symbols.
		 */
		byteCode = new ByteCode(session, this);
		int mode = 0; /* Default mode */
		
		/*
		 * See if there is a FILE <ident> clause.
		 */

		final FileParse f = new FileParse(tokens, true);
		if (f.success()) {
			f.generate(byteCode);
			if (!tokens.assumeNextToken(new String[] {";", ","}))
				return new Status(Status.FILECOMMA);
		} else
			return new Status(Status.EXPFID);

		/*
		 * See if there is a USING clause that tells us what the record is meant
		 * to be shaped like. If so, we calculate the size of the record and use
		 * that as a multiplier.
		 */

		boolean hasRecord = false;
		final Expression exp = new Expression(session);
		if (tokens.assumeNextToken("USING")) {
			/* Load the record definition on the stack */
			exp.compile(byteCode, tokens);
			if (exp.status.failed())
				return exp.status;
			hasRecord = true;
			byteCode.add(ByteCode._SIZEOF, 1); /* 1 means RECORD calc */
			mode = 1;
			/* There may be a trailing "TO" or comma. Eat 'em if found */
			tokens.assumeNextToken(new String[] { ";", ",", "TO"});
		}
		
		/*
		 * Compile the record position.
		 */
		exp.compile(byteCode, tokens);
		if (exp.status.failed())
			return exp.status;

		/*
		 * If there was record definition, the add the extra calculations.
		 * Record definitions are addressed from a 1-based address, so offset
		 * the record position and the multiply the result by the RECSIZE
		 * calculation.
		 */

		if (hasRecord) {
			byteCode.add(ByteCode._SUBI, 1);
			byteCode.add(ByteCode._MULT);
		}

		byteCode.add(ByteCode._SEEK, mode);

		return new Status();
	}

}

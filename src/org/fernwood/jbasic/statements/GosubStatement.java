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
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;

/**
 * GOSUB statement handler. Accepts a label token, and attempts to set the
 * current program's execution mode to that label location. It is an error to
 * attempt to execute a GOSUB without a program context associated with the
 * statement.
 * <p>
 * The syntax for the GOSUB statement is:
 * <p>
 * 
 * <code>GOSUB label</code>
 * <p>
 * <code>GOSUB USING(expression)</code>
 * <p>
 * Where the 'label' is an identifier in the program that is used as a branch
 * target. An alternate syntax allows GOSUB USING as a way of using an
 * expression to generate the label value, so that indirect branches and
 * "switch"-style statements are possible.
 * <p>
 * The GOSUB statement can also specify a numeric value that is a line number,
 * to support old-style BASIC program dialects.  In the case of a destination
 * that is a number, you cannot use the USING() clause.
 * @author cole
 * 
 */

class GosubStatement extends Statement {

	/**
	 * Compile 'GOSUB' statement. Processes a token stream, and compiles it into
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
		byteCode = new ByteCode(session, this);
		if (program == null)
			return new Status(Status.NOACTIVEPGM);

		/* If it's a USING clause we must do an indirect branch */

		if (tokens.assumeNextToken("USING")) {
			final Expression exp = new Expression(session);

			if (!tokens.assumeNextToken("("))
				return new Status(Status.INVUSING);

			exp.compile(byteCode, tokens);
			if (exp.status.failed())
				return exp.status;

			if (!tokens.assumeNextToken(")"))
				return new Status(Status.INVUSING);

			byteCode.add(ByteCode._JSBIND);
			return new Status(Status.SUCCESS);

		}

		/*
		 * The destination could be a line number if we're supporting an
		 * old dialect of BASIC. Handle with the blunt instrument of a
		 * GOSUB, which will later be optimized away to a JSB.
		 */
		if( tokens.testNextToken(Tokenizer.INTEGER)) {
			int ln = Integer.parseInt(tokens.nextToken());
			try {
				addLineNumberPosition(tokens.getPosition() - 1);
			}
			catch(JBasicException e ) {
				return e.getStatus();
			}
			byteCode.add(ByteCode._GOSUB, ln);
			return new Status(Status.SUCCESS);
		}
		
		/*
		 * It is a label, so use the label form of JSB instead.
		 */
		if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {
			byteCode.add(ByteCode._JSB, tokens.nextToken());
			return new Status(Status.SUCCESS);
		}

		return new Status(Status.NOSUCHLABEL, tokens.nextToken());

	}

}
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

/**
 * Signal statement. This creates a status block and returns it to generate a
 * signal, which should hopefully be caught by an ON unit.
 * 
 * @author tom
 * @version 1.0 October 18, 2004
 */

class SignalStatement extends Statement {

	/**
	 * Compile 'SIGNAL' statement. Processes a token stream, and compiles it
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
		byteCode = new ByteCode(session, this);
		final Expression expr = new Expression(session);
		String signalName = null;

		if( tokens.endOfStatement())
			return status = new Status(Status.EXPNAME);
		
		if (tokens.assumeNextToken("USING")) {

			if (!tokens.assumeNextToken("("))
				return new Status(Status.PAREN);

			expr.compile(byteCode, tokens);
			if (expr.status.failed())
				return expr.status;
			if (!tokens.assumeNextToken(")"))
				return new Status(Status.PAREN);
		} else {
			boolean success=false;
			signalName = tokens.nextToken();
			if( signalName.equals("*")) {
				success = true;
				signalName = tokens.nextToken();
			}
			if (!tokens.isIdentifier())
				return new Status(Status.INVNAME, signalName);
			if( success )
				signalName = "*" + signalName;
		}

		int hasSub = 0;

		if (tokens.assumeNextToken(",")) {
			expr.compile(byteCode, tokens);
			if (expr.status.failed())
				return expr.status;
			hasSub = 1;
		} else if (tokens.assumeNextToken("(")) {
			expr.compile(byteCode, tokens);
			if (expr.status.failed())
				return expr.status;
			if (!tokens.assumeNextToken(")"))
				return new Status(Status.EXPRESSION);
			hasSub = 1;
		}

		/*
		 * IF we have both an indirect signal name, and a substitution
		 * parameter, they were pushed on the stack in the wrong order. At
		 * runtime, reverse them.
		 */
		if ((signalName == null) & (hasSub > 0))
			byteCode.add(ByteCode._SWAP);

		if (signalName != null)
			byteCode.add(ByteCode._SIGNAL, hasSub, signalName);
		else
			byteCode.add(ByteCode._SIGNAL, hasSub);

		return new Status(Status.SUCCESS);

	}
}
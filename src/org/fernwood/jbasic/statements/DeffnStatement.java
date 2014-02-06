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
import org.fernwood.jbasic.compiler.Optimizer;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * DEFFN statement handler. This defines a function in terms of a single
 * expresion.
 * <p>
 * The syntax of the DEFFN statement is:
 * <p>
 * <code>
 * DEFFN function-name( parameters ) = expression
 * </code>
 * <p>
 * 
 * @author cole
 * 
 */

class DeffnStatement extends Statement {

	/**
	 * Compile the 'DEFFN' statement.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status compile(final Tokenizer tokens) {
		
		if (this.program == null)
			return status = new Status(Status.NOACTIVEPGM);


		String fn = tokens.nextToken();
		if (fn == null)
			return status = new Status(Status.EXPSYNTAX,
					"missing function name");

		byteCode = new ByteCode(session, this);
		int pos = byteCode.add(ByteCode._DEFFN, 0, fn);
		
		byteCode.add(ByteCode._ENTRY, ByteCode.ENTRY_FUNCTION, fn);
		int count = 0;
		
		if (tokens.assumeNextSpecial("(")) {
			statementText = statementText + "(";
			while (true) {
				if (!tokens.testNextToken(Tokenizer.IDENTIFIER))
					break;
				count++;
				byteCode.add(ByteCode._ARG, count, tokens.nextToken());
				tokens.assumeNextSpecial(",");
			}
			if (!tokens.assumeNextSpecial(")"))
				return status = new Status(Status.PAREN);
		}
		else
			return status = new Status(Status.ARGERR);
		
		if (!tokens.assumeNextSpecial("="))
			return status = new Status(Status.SYNEXPTOK, "=");
		
		Expression exp = new Expression(session);
		status = exp.compile(byteCode, tokens);
		if (status.failed())
			return status;
		byteCode.add(ByteCode._RET, 1);

		/*
		 * We need to optimize it now so we know the number of instructions
		 * in this sequence.
		 */
		Optimizer opt = new Optimizer();
		opt.optimize(byteCode);
		/*
		 * Update the count in the _DEFFN instruction to reflect the
		 * length of the generated code sequence.
		 */
		byteCode.getInstruction(pos).integerOperand = byteCode.size() - pos - 1;
		
		return new Status();

	}

}
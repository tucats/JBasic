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
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * Renumber the current program's line numbers to a given starting value
 * and a regular increment.  Internal references in the code to line 
 * numbers (such as a <code>GOTO</code> statement) are adjusted accordingly.
 * <p>
 * <code>
 * RENUMBER [[FROM] <em>start</em> [[BY] <em>increment</em>]]
 * </code>
 * <p>
 * The <code>FROM</code> and <code>BY</code> keywords are optional.  An
 * increment can only be specified if a start value is also given.  Both
 * the start value and increment must be positive non-zero integer
 * values.  An increment cannot be greater than 32000.  If not specified,
 * the default <em>start</em> value is 100 and the default <em>increment</em>
 * value is 10.
 * 
 * @author cole
 * 
 */
public class RenumberStatement extends Statement {

	static int nextName = 0;

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		if (session.programs.getCurrent() == null)
			return new Status(Status.NOPGM);

		int nextLineNumber = 100;
		int increment = 10;

		final Expression exp = new Expression(session);

		
		if (!tokens.endOfStatement()) {
			tokens.assumeNextToken("FROM");
			Value result = exp.evaluate(tokens, symbols);
			if (exp.status.failed())
				return exp.status;
			nextLineNumber = result.getInteger();
		}

		
		if (!tokens.endOfStatement()) {
			tokens.assumeNextToken(new String[] { "STEP", "BY"});
			Value result = exp.evaluate(tokens, symbols);
			if (exp.status.failed())
				return exp.status;
			increment = result.getInteger();
		}

		if( nextLineNumber < 1 || nextLineNumber > 65535)
			return status = new Status(Status.INVRENSTART, Integer.toString(nextLineNumber));
		if( increment < 1 || increment > 32000 )
			return status = new Status(Status.INVRENINC, Integer.toString(nextLineNumber));
		
		/*
		 * Make sure a prompt is printed after this statement even if in 
		 * NOPROMPTMODE.
		 */
		this.session.setNeedPrompt(true);
		

		try {
			boolean oldTrimSetting = this.session.getBoolean("SYS$TRIMSOURCE");
			this.session.globals().insert("SYS$TRIMSOURCE", false);
			status = session.programs.getCurrent().renumber(nextLineNumber, increment);
			this.session.globals().insert("SYS$TRIMSOURCE", oldTrimSetting);
		}
		catch (JBasicException e ) {
			e.printError(this.session);
		}
		return status;
	}

}

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
import org.fernwood.jbasic.runtime.JBasicDebugger;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * @author tom
 * @version version 1.0 Aug 16, 2006
 * 
 */
public class StepStatement extends Statement {

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		final JBasicDebugger d = findDebugger();
		if (d == null)
			return new Status(Status.NODBG);

		if (tokens.assumeNextToken("RETURN")) {
			d.setReturn(true);
			return new Status("*STEP", 0);
		}

		if (tokens.assumeNextToken("INTO")) {
			d.stepInto(true);
			return new Status("*STEP", 1);
		}

		d.stepInto(false);

		if (tokens.endOfStatement())
			return new Status("*STEP", 1);

		final Expression exp = new Expression(session);
		final Value v = exp.evaluate(tokens, symbols);
		if (v == null)
			return exp.status;

		return new Status("*STEP", v.getInteger());
	}
}

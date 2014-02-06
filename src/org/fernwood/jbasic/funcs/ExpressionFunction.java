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
package org.fernwood.jbasic.funcs;

import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>EXPRESSION()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Calculate an expression value.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>d = EXPRESSION( <em>string</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Result of expression</td></tr>
 * </table>
 * <p>
 * Returns result of an expression evaluation of the
 * parameter which is a string. The string contains the text of the expression
 * to evaluate. This lets a program dynamically construct expressions to
 * evaluate. It also allows it to construct variable names and access their
 * values.
 * 
 * @author cole
 * 
 */
public class ExpressionFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist
	 *            the function argument list and count already popped from the
	 *            runtime data stack
	 * @param symbols
	 *            the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException an argument count or type error occurred
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		/*
		 * We only allow a string argument
		 */
		arglist.validate(1, 1, new int [] { Value.STRING });

		/*
		 * Convert the argument into a token stream, which is required to
		 * be able to evaluate the expression.
		 */
		
		Tokenizer tokens = new Tokenizer(arglist.stringElement(0));
		
		/*
		 * Evaluate the resulting token stream immediately, and return the resulting
		 * value as the function's result.
		 */
		final Expression exp = new Expression(arglist.session);
		return exp.evaluate(tokens, symbols);

	}

}

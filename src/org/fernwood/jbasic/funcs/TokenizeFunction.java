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

import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;


/**
 * <b>TOKENIZE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Tokenize a string.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = TOKENIZE( <em>string</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of records</td></tr>
 * </table>
 * <p>
 * The string is parsed and converted to individual text tokens using JBasic's 
 * language processor (and tokenization rules) and
 * the results are returned as an array of records.  Each record describes a token in the
 * buffer, including it's spelling (text of the token) and it's type (INTEGER, INDENTIFIER,
 * STRING, etc.).
 * <p>
 * Items returned in the record include:
 * <p>
 * <table>
 * <tr><td><b>Item</b></td><td><b>Description</b></td></tr>
 * <tr><td>SPELLING</td><td>The text of the token</td></tr>
 * <tr><td>KIND</td><td>The token kind, such as INTEGER, STRING, SPECIAL, etc.</td></tr>
 * </table>
 * @author cole
 *
 */
public class TokenizeFunction extends JBasicFunction {

	/**
	 * Parse a string and create an array of records describing each token in
	 * the string.
	 * 
	 * @param arglist
	 *            Function argument list.
	 * @param symbols
	 *            Symbol table to use at runtime to resolve symbol information.
	 * 
	 * @return Array value containing records for each parsed item.
	 * @throws JBasicException  An error in the count or type of argument
	 * occurred.

	 */
	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(1, 1, new int[] { Value.STRING });

		final Value result = new Value(Value.ARRAY, null);

		final Tokenizer t = new Tokenizer(arglist.stringElement(0));

		int i = 1;

		while (true) {
			String token = t.nextToken();
			if (t.getType() == Tokenizer.END_OF_STRING)
				break;

			String kind = "UNKNOWN";
			switch (t.getType()) {
			case Tokenizer.DOUBLE:
				kind = "DOUBLE";
				break;
			case Tokenizer.DECIMAL:
				kind = "DECIMAL";
				break;
			case Tokenizer.IDENTIFIER:
				kind = "IDENTIFIER";
				token = token.toUpperCase();
				break;

			case Tokenizer.INTEGER:
				kind = "INTEGER";
				break;

			case Tokenizer.SPECIAL:
				kind = "SPECIAL";
				break;

			case Tokenizer.STRING:
				kind = "STRING";
				break;
			}

			final Value item = new Value(Value.RECORD, null);
			item.setElement(new Value(token), "SPELLING");
			item.setElement(new Value(kind), "KIND");
			result.setElement(item, i++);
		}

		return result;

	}

}

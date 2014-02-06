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
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * Input a line of text into a single variable. The syntax is:
 * <p>
 * <code>
 * GET [FILE] <em>identifier</em>, <em>type</em> <em>variable</em> [,<em>type variable</em>...]
 * </code>
 * <p>
 * The <em>type</em> must be one of <code>BOOLEAN</code>,
 * <code>STRING(<em>size</em>)</code>, <code>DOUBLE</code>, or
 * <code>INTEGER</code>. The natural number of bytes for a numeric item are
 * read from the file, or the given number of bytes to be stored as a string
 * variable. The datum is stored in the named variable, which is created if
 * needed, and always set to the given <em>type</em>.
 * <p>
 * An alternate approach is to create an array of records, where each record
 * describes a field in the input. This must have the following fields:
 * <p>
 * 
 * <code>NAME </code> - A string containing the variable name to store into.
 * <p>
 * <code>TYPE </code> - A string containing the type of the value, "INTEGER",
 * "STRING", etc.
 * <p>
 * <code>SIZE </code> - An integer with the number of bytes; ignored except for
 * STRING
 * <p>
 * 
 * <p>
 * The following syntax is used to specify this record:
 * <p>
 * <code>
 *GET [FILE] <em>identifier</em>, USING <em>record-array</em>
 *<code>
 *<p>
 * 
 * @author tom
 * @version version 1.0 May 27, 2006
 *
 */

class GetStatement extends Statement {

	/**
	 * Compile 'GET' statement. Processes a token stream, and compiles it into a
	 * byte-code stream associated with the statement object. The first token in
	 * the input stream has already been removed, since it was the "verb" that
	 * told us what kind of statement object to create.
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

		/*
		 * See if there is a FILE <ident> clause.
		 */

		if( tokens.endOfStatement())
			return status = new Status(Status.EXPFID);
		final FileParse f = new FileParse(tokens, false);
		if (f.failed())
			return f.getStatus();
		f.generate(byteCode);

		/*
		 * May be a comma at this point.
		 */

		tokens.assumeNextToken(",");

		/*
		 * If USING is given, then the expression that follows defines the
		 * record scheme. This can be a reference to an existing array of
		 * records, or it can be a constant.
		 */

		boolean hasAS = false;
		LValue destination = null;
		final Expression exp = new Expression(session);

		/*
		 * The logic is a little convoluted. We use the mode value to determine
		 * the mode that the results are written from the GET.
		 * 
		 * 0 - variables written from explicit record spec 1 - record created
		 * from explicit record spec 2 - record created from implicit database
		 * record spec
		 * 
		 * So if there is an AS here, there was no record spec and we must use
		 * the database one. So set mode to 1, and it will be incremented again
		 * when the AS is actually processed.
		 */
		int mode = 0;
		
		if( tokens.endOfStatement()) {
			mode = 4;
		}
		else if (tokens.testNextToken("AS"))
			mode = 1;
		else {

			if (tokens.assumeNextToken("USING")) {
				exp.compile(byteCode, tokens);
				if (exp.status.failed())
					return exp.status;
			} else {

				/*
				 * Otherwise we must construct the record array definition
				 * manually.
				 */

				int fCount = 0;

				while (true) {

					fCount++;
					int rCount = 2;
					final String dataType = tokens.nextToken();
					if (tokens.getType() != Tokenizer.IDENTIFIER)
						return new Status(Status.INVRECDEF, dataType);

					byteCode.add(ByteCode._STRING, "TYPE");
					byteCode.add(ByteCode._STRING, dataType);

					if (dataType.equals("UNICODE")) {
						tokens.assumeNextToken("STRING");
						if (tokens.testNextToken("(")) {
							byteCode.add(ByteCode._STRING, "SIZE");
							rCount = 3;
							exp.compile(byteCode, tokens);
							if (exp.status.failed())
								return exp.status;
						}
						else 
							return new Status(Status.INVRECDEF, new Status(Status.EXPSIZE));
					} else if (dataType.equals("VARYING")) {
						tokens.assumeNextToken("STRING");
						if (tokens.testNextToken("(")) {
							byteCode.add(ByteCode._STRING, "SIZE");
							rCount = 3;
							exp.compile(byteCode, tokens);
							if (exp.status.failed())
								return exp.status;
						}
						else 
							return new Status(Status.INVRECDEF, new Status(Status.EXPSIZE));
					} else if (dataType.equals("STRING")) {
						if (tokens.testNextToken("(")) {
							byteCode.add(ByteCode._STRING, "SIZE");
							rCount = 3;
							exp.compile(byteCode, tokens);
							if (exp.status.failed())
								return exp.status;
						}
						else 
							return new Status(Status.INVRECDEF, new Status(Status.EXPSIZE));
					} else if (dataType.equals("INTEGER")) {
						if (tokens.testNextToken("(")) {
							byteCode.add(ByteCode._STRING, "SIZE");
							rCount = 3;
							exp.compile(byteCode, tokens);
							if (exp.status.failed())
								return exp.status;
						}
			
					} else if (dataType.equals("FLOAT")) {
						if (tokens.testNextToken("(")) {
							byteCode.add(ByteCode._STRING, "SIZE");
							rCount = 3;
							exp.compile(byteCode, tokens);
							if (exp.status.failed())
								return exp.status;
						}
			
					} else if (!dataType.equals("BOOLEAN")
							& !dataType.equals("DOUBLE"))
						return new Status(Status.INVRECDEF, dataType);

					final String varName = tokens.nextToken();
					if (tokens.getType() != Tokenizer.IDENTIFIER)
						return new Status(Status.INVRECDEF, new Status(Status.INVNAME, varName));
					byteCode.add(ByteCode._STRING, "NAME");
					byteCode.add(ByteCode._STRING, varName);
					byteCode.add(ByteCode._RECORD, rCount);

					if (!tokens.assumeNextSpecial(",")) {
						byteCode.add(ByteCode._ARRAY, fCount);
						break;
					}
				}
			}

			/*
			 * See if there is an AS clause with an LVALUE to write the
			 * resulting record to.
			 */
			tokens.assumeNextSpecial(",");
		}

		/*
		 * See if there is a record specification.
		 */
		if (tokens.assumeNextToken("AS")) {
			hasAS = true;
			mode = mode + 1;
			int mark = tokens.getPosition();
			destination = new LValue(session, strongTyping());
			destination.compileLValue(byteCode, tokens);
			if (destination.error) {
				tokens.setPosition(mark);
				return new Status(Status.FILESYNTAX, new Status(Status.EXPCLAUSE, "AS"));
			}
		}

		byteCode.add(ByteCode._GET, mode);

		if (hasAS)
			if( destination != null )
				destination.compileStore();

		return new Status();
	}

}

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

import java.util.ArrayList;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.FileParse;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * Put one or more variables to a record-oriented file.
 * <p>
 * <code>
 * PUT FILE <em>identifier</em>, <em>type</em> <em>variable</em> [,<em>type variable</em>...]
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
 *PUT FILE <em>identifier</em>, USING <em>record-array</em>
 *<code>
 *<p>
 * 
 * @author tom
 * @version version 1.0 May 27, 2006
 *
 */

class PutStatement extends Statement {

	static int sequenceNumber = 0;

	/**
	 * Compile 'PUT' statement. Processes a token stream, and compiles it into a
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
		 * Depending on the syntax, we may need to create one or more temporary
		 * variable names. If so, let's keep a list here. If the vector is null
		 * then it was never used, but if it is non-null at the end of
		 * compilation, we'll need to generate some cleanup code to delete the
		 * temporary variables.
		 */
		ArrayList<String> tempList = null;

		/*
		 * Set up tokenization and symbols.
		 */
		byteCode = new ByteCode(session, this);

		/*
		 * See if there is a FILE <ident> clause.
		 */

		final FileParse f = new FileParse(tokens, true);
		if (f.success()) {
			f.generate(byteCode);

		} else
			return new Status(Status.EXPFID);

		/*
		 * If USING is given, then the expression that follows defines the
		 * record scheme. This can be a reference to an existing array of
		 * records, or it can be a constant.
		 */

		int hasFROM = 0;
		final Expression exp = new Expression(session);

		if( tokens.endOfStatement()) {
			hasFROM = 4;
		} else  {
			if (!tokens.assumeNextToken(new String [] { ";", ","}))
				return new Status(Status.FILECOMMA);
			if (tokens.assumeNextToken("USING")) {

				exp.compile(byteCode, tokens);
				if (exp.status.failed())
					return exp.status;
				tokens.assumeNextSpecial(",");
				if (tokens.assumeNextToken("FROM")) {
					exp.compile(byteCode, tokens);
					if (exp.status.failed())
						return exp.status;
					hasFROM = 1;
				}
			} else {

				/*
				 * Otherwise we must construct the record array definition manually.
				 */

				int fCount = 0;
				tempList = new ArrayList<String>();

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
							return new Status(Status.INVRECDEF,new Status(Status.EXPSIZE));
					} else
						if (dataType.equals("VARYING")) {
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

					exp.compile(byteCode, tokens);
					if (exp.status.failed())
						return exp.status;

					/*
					 * If the expression is a single variable name, then we can
					 * remove the LOAD that was generated, and just use the name of
					 * the variable in our record definition. However, if it was an
					 * expression, we'll need to store the temporary result in a
					 * temp variable and keep up with that temporary name instead.
					 */
					String tempName;

					if (exp.isVariable) {
						byteCode.remove(byteCode.size() - 1);
						tempName = exp.variableName;
					} else {
						tempName = "PUT_TEMP_" + Integer.toString(sequenceNumber++);
						tempList.add(tempName);
						byteCode.add(ByteCode._STOR, tempName);
					}

					byteCode.add(ByteCode._STRING, "NAME");
					byteCode.add(ByteCode._STRING, tempName);
					byteCode.add(ByteCode._RECORD, rCount);

					if (!tokens.assumeNextSpecial(",")) {
						byteCode.add(ByteCode._ARRAY, fCount);
						break;
					}
				}
			}
		}
		/*
		 * At this point, the stack contains the record definition array and the
		 * file identifier, so let's invoke the actual work of the PUT
		 * statement.
		 */
		byteCode.add(ByteCode._PUT, hasFROM);

		/*
		 * If we might have generated temporary names (because no USING clause
		 * was given) then let's generate code to remove the temps from the
		 * local symbol table.
		 */
		if (tempList != null) {
			final int tempListSize = tempList.size();
			for (int ix = 0; ix < tempListSize; ix++)
				byteCode.add(ByteCode._CLEAR, tempList.get(ix));
			tempList = null;

		}
		return new Status();
	}

}

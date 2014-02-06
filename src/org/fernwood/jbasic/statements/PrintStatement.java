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
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.value.Value;

/**
 * Print an expression. The result of the expression is formatted and displayed
 * on the console, but otherwise discarded. A symbol table is optional, but if
 * found can be used to resolve symbolic references.
 * 
 * @author tom
 * @version version 1.0 Jun 24, 2004
 * 
 */

class PrintStatement extends Statement {

	/**
	 * Compile 'PRINT' statement. Processes a token stream, and compiles it into
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
		 * Initialize a new bytecode stream for this statement.
		 */
		byteCode = new ByteCode(session, this);

		/**
		 * Track if we are generating a newline or not at the end of this PRINT
		 * statement using this boolean value.
		 */

		boolean newline = true;

		// Later, we'll parse to see if we should use a different file here.

		int isFile = 0;

		final Expression exp = new Expression(session);

		/*
		 * See if there is a FILE clause. Note that if it's the "#" character,
		 * we convert that to a FILE verb.
		 * 
		 * When there is a FILE statement, code is generated to push the file
		 * identifier on the stack. Additionally, the isFile flag is set to
		 * indicate that there is a file value to be found on the stack; this is
		 * passed to the _OUT bytecode at runtime.
		 */

		final FileParse f = new FileParse(tokens, true);

		//if( f.getFID() == null & f.failed())
		//	return f.getStatus();
		
		if (f.success()) {
			f.generate(byteCode);
			isFile = 1;
			if (!tokens.testNextToken(Tokenizer.END_OF_STRING))
				if (!tokens.assumeNextToken(new String [] { ";", ","}))
					return new Status(Status.FILECOMMA);
		}

		/*
		 * If it's a simple PRINT with nothing else, then generate the call to
		 * make a new line.
		 */
		
		boolean simplePrint = tokens.endOfStatement();
		
		if (simplePrint) {
			byteCode.add(ByteCode._CHAR, '\n');
			byteCode.add(ByteCode._OUT, isFile);
			return status = new Status(Status.SUCCESS);
		}

		int itemCount = 0;
		boolean isUsing = false;
		ByteCode using = null;
		
		while (true) {

			/*
			 * If end-of-string or we hit the compound statement
			 * marker, then we're done.
			 */
			if (tokens.endOfStatement())
				break;
			
			newline = true;
			if (!isUsing)
				if (tokens.assumeNextToken("USING")) {
					if( isUsing || itemCount > 0 ) {
						return status = new Status(Status.INVPRINTUSING);
					}
					isUsing = true;
					using = new ByteCode(session);
					exp.compile(using, tokens);
					if (exp.status.failed()) {
						return status = new Status(Status.EXPRESSION, exp.status.toString());
					}
					if (tokens.endOfStatement())
						break;
					if (!tokens.assumeNextToken(","))
						return status = new Status(Status.INVPRINTUSING);
				}

			/*
			 * Compile next item in the queue.  Remember where we are for
			 * the purposes of capturing the reference expression should we
			 * later find a "=" operator.
			 */
			//int expMark = tokens.getPosition();
			int insertRefHere = byteCode.size();
			
			exp.compile(byteCode, tokens);
			if (exp.status.failed()) {
				return status = exp.status;
			}

			/*
			 * See if it is the X= format where a variable value is
			 * followed by the "=" indicating that it should print
			 * its name as well as value.  Note that we've got to test
			 * for the "isVariable" state indicating a single scalar
			 * variable before the test for assumeNextToken since it
			 * will eat the token in all cases where it is found.
			 */
			if( !exp.isExpression & tokens.testNextToken("=")) {
				
				tokens.nextToken(); /* Eat the "=" */
				
				//String itemName = tokens.format(expMark, tokens.getPosition());
				// byteCode.add(ByteCode._STRING, itemName + " ");
				byteCode.insert(insertRefHere, new Instruction(ByteCode._REFSTR, 0));
				byteCode.add(ByteCode._REFSTR, 2);
				
				byteCode.add(ByteCode._SWAP);
				byteCode.add(ByteCode._CVT, Value.STRING);
				byteCode.add(ByteCode._CONCAT);
			}
			
			/*
			 * Accumulate it into the large string we're building. To do this,
			 * we'll ensure the new item is a string formatted for printing
			 * and then concatenate them.
			 */

			if (!isUsing) {

				/*
				 * If this isn't the first one, it gets concatenated to the
				 * working output string buffer.
				 */
				if (itemCount > 0) {
					byteCode.add(ByteCode._CVT, Value.FORMATTED_STRING);
					byteCode.add(ByteCode._CONCAT);
				}
			}

			itemCount++;

			/*
			 * If the item delimiter is a comma, we output a trailing tab
			 * character and loop again.
			 */
			if (tokens.assumeNextToken(",")) {
				newline = false;
				if (!isUsing)
					byteCode.add(ByteCode._CONCAT, '\t');
				continue;
			}

			/*
			 * If the item delimiter is a semicolon, then we loop again but
			 * don't move the cursor.
			 */
			else if (tokens.assumeNextToken(";")) {
				newline = false;
				continue;
			}

			/*
			 * If there isn't anything else, then drop out and note we need a
			 * newline.
			 */
			else {
				newline = true;
				break;
			}
		}

		if (isUsing) {
			byteCode.concat(using);
			byteCode.add(ByteCode._USING, itemCount);
		}

		/*
		 * If we are to generate a NEWLINE, use the correct bytecode. This is
		 * particularly important for PRINT operations to database files, where
		 * the newline tells us that we have accumulated the entire SQL
		 * statement to execute. <p><br>
		 * 
		 * Previously this was handled by concatenating carriage control
		 * explicitly to the output buffer, but that doesn't seem necessary or
		 * beneficial compared to using the OUTNL code.
		 * this.byteCode.add(ByteCode._CONCAT, (int) '\n');
		 */
		if (newline)
			byteCode.add(ByteCode._OUTNL, isFile);
		else
			byteCode.add(ByteCode._OUT, isFile);

		return new Status(Status.SUCCESS);

	}
}
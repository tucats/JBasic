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
 * COPYRIGHT 2003-2008 BY TOM COLE, TOMCOLE@USERS.SF.NET
 *
 */
package org.fernwood.jbasic.statements;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;

/**
 * If statement. Parses an expression optionally executes the remaining
 * statement if the expression result is non-zero.
 * <p>
 * The syntax of the IF statement is:
 * <p>
 * <code>
 *       IF <em>expression</em> THEN statement [ELSE statement]
 * </code>
 * <p>
 * The <em>expression</em> can be of virtually any type, but the result will
 * always be coerced to a boolean value. Most often, the <em>expression</em>
 * is a relational test such as <code>VAR="value"</code> that results in a
 * true/false result.
 * <p>
 * The statement that follows the expression can be any statement, but it can be
 * only one statement - so it must transfer control somehow if it needs to
 * result in more than one action being performed based on the conditional.
 * IF the ELSE clause is present, it will be executed if the <em>expression</em>
 * is false.
 * <p>
 * You can use a variant of the syntax to define a multi-statement 
 * <code>IF..THEN..ELSE</code> block.  For example,
 * <code>
 * <p>
 * IF SALARY > 10.0 THEN<br>
 * &nbsp;PRINT "LARGE"<br>
 * &nbsp;PAY = SALARY * HOURS<br>
 * ELSE<br>
 * &nbsp;PRINT "SMALL"<br>
 * &nbsp;PAY = SALARY * HOURS * 1.20<br>
 * END IF<br></code>
 * <p>
 * In this case, the two clauses of the <code>IF</code> statement are
 * expressed as multiple statements, signified by the trailing <code>THEN</code>
 * keyword before the block to be executed if the condition is true.  The 
 * <code>ELSE</code> keyword optionally identifies the start of
 * the block to execution if the condition is false.  The entire compound
 * statement is terminated with the <code>END IF</code> statement.
 * <p>
 * @author tom
 * @version version 1.2 Dec 2008
 */

class IfStatement extends Statement {

	/**
	 * Compile 'IF' statement. Processes a token stream, and compiles it into a
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
		 * Set up compilation
		 */

		byteCode = new ByteCode(session, this);

		/*
		 * Start by compiling the comparison expression
		 */

		final Expression rvalue = new Expression(session);
		status = rvalue.compile(byteCode, tokens);
		if (status.failed())
			return status;

		/*
		 * Must be followed by a THEN keyword
		 */

		if (!tokens.assumeNextToken("THEN"))
			return status = new Status(Status.IF);

		/*
		 * If this is the end of the statement, then it's a multi-line
		 * IF-THEN-ELSE-ENDIF construct.  This construct is only allowed
		 * in an active program, not as a command-line statement.
		 */
		
		if( tokens.endOfStatement()) {
			
			if( program == null )
				return status = new Status(Status.NOACTIVEPGM);
			byteCode.add(ByteCode._IF, 1);
			indent = 1;
			return new Status();
		}
		/*
		 * Store the TEST, and remember where this is so we can back-patch it
		 * later.
		 */
		final int thenTestReference = byteCode.add(ByteCode._BRZ);

		/*
		 * If the next item is an integer, this is a very old-school
		 * 
		 * 	IF condition THEN line-number
		 * 
		 * syntax.  If this is true, formulate a GOTO and we're 
		 * almost done.
		 */
		if( tokens.testNextToken(Tokenizer.INTEGER)) {
			
			int lineNumber = Integer.parseInt(tokens.nextToken());
			try {
				addLineNumberPosition(tokens.getPosition() - 1);
			}
			catch(JBasicException e ) {
				return e.getStatus();
			}
			byteCode.add(ByteCode._GOTO, lineNumber );
			return status = new Status(Status.SUCCESS);
		}
		/*
		 * No, it's a real statement (and might also have an ELSE).  The THEN
		 * clause needs a statement context to run in. So copy ours. We
		 * do this (rather than use our own object) because we need the STORE
		 * operation to handle implicit assignment, etc.
		 */
		int basePosition = tokens.getPosition();

		final Statement thenStatement = new Statement(this);
		final String savedThen = tokens.getBuffer();
		tokens.fReserveElse = true;

		thenStatement.store(session, tokens, program);
		
		/*
		 * The THEN clause may have one or more line number references.  IF so,
		 * we need to copy their token positions our current statement.
		 */
		int lineCount = thenStatement.lineNumberTokenCount();
		if( lineCount > 0 ) {
			try {
			for( int linex = 1; linex <= lineCount; linex++ ) {
				addLineNumberPosition( basePosition
					+ thenStatement.getLineNumberPosition(linex));
			}
			}
			catch(JBasicException e ) {
				return e.getStatus();
			}
		}
		/*
		 * If this statement can't be compiled, we'll punt and push it as an
		 * EXEC operation.
		 */
		if (thenStatement.status.equals(Status.NOCOMPILE)) {
			byteCode.add(ByteCode._STRING, savedThen);
			byteCode.add(ByteCode._EXEC);
		} else
			byteCode.concat(thenStatement.byteCode);

		/*
		 * If the THEN statement failed, punt.
		 */

		if (thenStatement.status.failed())
			return thenStatement.status;

		/*
		 * Is there an ELSE block?
		 */

		final int elseReference = byteCode.add(ByteCode._BR);
		byteCode.patch(thenTestReference);
		basePosition = basePosition + 1 + tokens.getPosition();
		
		if (tokens.assumeNextToken("ELSE")) {

			final Statement elseStatement = new Statement(this);
			final String savedElse = tokens.getBuffer();
			elseStatement.store(session, tokens, program);

			/*
			 * If this statement can't be compiled, we'll punt and push it as an
			 * EXEC operation.
			 */
			if (elseStatement.status.equals(Status.NOCOMPILE)) {
				elseStatement.byteCode.add(ByteCode._STRING, savedElse);
				elseStatement.byteCode.add(ByteCode._EXEC);
			} else
				byteCode.concat(elseStatement.byteCode);

			if (elseStatement.status.failed())
				return elseStatement.status;

			lineCount = elseStatement.lineNumberTokenCount();
			if( lineCount > 0 ) 
				this.copyLinePositions(elseStatement,basePosition);	

		}
		byteCode.patch(elseReference);

		return status = new Status(Status.SUCCESS);
	}

}

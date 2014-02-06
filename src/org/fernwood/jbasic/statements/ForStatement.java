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

import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;

/**
 * FOR statement handler. This implements a loop where an index variable is
 * assigned each value from either a calculated increment or a value from a
 * list. This has the syntax: <br>

 * <br>
 * <code>
 * FOR index = <em>start</em> TO <em>end</em> [BY <em>increment</em>] </code>
 * <br>
 * <code>&nbsp;&nbsp;&nbsp; ... statements ...</code>
 * <br>
 * <code>NEXT <em>index</em></code><br><br>
 * ...or...
 * <p>
* <code>
 * FOR index = <em>v1</em> [, <em>v2</em> [, <em>v3</em>]] </code>
 * <br>
 * <code>&nbsp;&nbsp;&nbsp; ... statements ...</code>
 * <br>
 * <code>NEXT <em>index</em><p></code>
 * <p>
 *
 * The index variable will be created if needed. In the first case, the
 * index variable is initialized the the <em>start</em> value, and the 
 * loop body (<em>statements</em>) is executed.  The index value is then
 * incremented by the <em>increment</em> value.  Note that if this value
 * is negative, the index is actually decremented.  The loop body is executed
 * again, and this process continues until the index value exceeds the <em>end</em>
 * value.<p>
 * In the second example, the index value is successively assigned each value
 * in the list.  So the first time the loop body is executed, the <em>index</em>
 * variable has the value <em>v1</em>, the second time it has the value <em>v2</em>,
 * etc.  If one of the values is an array, then the index is assigned each value
 * of the array in turn.
 * <p>
 * <code>NEXT</code> statements much match up with a <code>FOR</code> loop or an
 * error occurs. <p>
 * There is a special case version of the <code>FOR</code> statement that does 
 * not require
 * a <code>NEXT</code> statement. In this instance, the body of the loop is
 * expressed on the same line as the <code>FOR</code> statement, separated by
 * the keyword <code>DO</code> as in this example:
 * <p>
 * <code>
 *  &nbsp;&nbsp;&nbsp;   FOR PGM = PGMLIST DO CALL USING(PGM)
 *     </code>
 * <p>
 * In this case, the index variable <code>PGM</code> will be assigned each
 * element of the array <code>PGMLIST</code> in turn.  During each loop, the name will be
 * used to call the program the the name described by the element string values.
 * 
 * @author cole
 * 
 */

class ForStatement extends Statement {

	/**
	 * Compile 'FOR' statement. Processes a token stream, and compiles it into a
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


		byteCode = new ByteCode(session, this);

		/*
		 * Parse the index variable. This must be a scalar identifier.
		 */

		final String index = tokens.nextToken();
		if (tokens.getType() != Tokenizer.IDENTIFIER)
			return new Status(Status.INVFOR, new Status(Status.INVNAME, index));

		/*
		 * If the index is called RND and we are in GWBASIC compatibility mode,
		 * then this is an error - you can't assign to a pseudo-variable.
		 */
		if (index.equals("RND"))
			return new Status(Status.LVALUE, "RND");

		/*
		 * Parse the "=" sign
		 */

		if (!tokens.assumeNextToken("="))
			return new Status(Status.SYNEXPTOK, "=");

		/*
		 * See if this is FOR index = EACH list
		 */

		if (tokens.assumeNextToken(new String[] { "EACH", "EVERY"})) {

			/*
			 * Eat the optional "OF"
			 */
			if( tokens.getSpelling().equals("EACH")) {
				tokens.assumeNextToken("ITEM");
				tokens.assumeNextToken("OF");
			}
			/*
			 * Parse each element and put them on the stack. We'll sum them
			 * together into an array.
			 */

			byteCode.add(ByteCode._ARRAY, 0);
			int listBase = byteCode.size()-1;
			int listCount = 0;
			Expression exp = new Expression(session);
			while (true) {
				exp.compile(byteCode, tokens);
				byteCode.add(ByteCode._ADD);
				listCount++;
				if (tokens.endOfStatement())
					break;
				if(!tokens.assumeNextSpecial(","))
					break;
			}

			/*
			 * Optimization - if there was only one item in the list
			 * then we don't need to do the work of creating an array
			 * temporary for it.  Delete the leading _ARRAY and 
			 * trailing _ADD operators.
			 */
			
			if( listCount == 1 ) {
				
				byteCode.remove(listBase);
				byteCode.remove(byteCode.size()-1);
				
			}
			
			/*
			 * Generate the FOR processing instruction
			 */
			byteCode.add(ByteCode._FOREACH, index);

		} else {

			/*
			 * Remember our current location because we may need
			 * to insert something here later if this turns out to
			 * be an implicit FOR..EACH statement
			 */
			int backPatch = byteCode.size();
			
			/*
			 * Parse the starting value.
			 */
			final Expression exp = new Expression(session);
			exp.compile(byteCode, tokens);
			if (exp.status.failed())
				return exp.status;

			/*
			 * This could be an implicit EACH
			 */

			if (tokens.testNextToken(",") || tokens.testNextToken("DO") 
					|| tokens.endOfStatement()) {

				/*
				 * First value is already on the stack. Use the backpatch
				 * location to insert the creation of an empty array so
				 * we can do array _ADD operations.
				 */

				int listBase = backPatch;
				int listCount = 1;
				byteCode.insert(backPatch, new Instruction(ByteCode._ARRAY,0));
				byteCode.add(ByteCode._ADD);

				while (true) {
					if (tokens.endOfStatement())
						break;
					if( tokens.testNextToken("DO"))
						break;
					
					if(!tokens.assumeNextSpecial(","))
						break;
					exp.compile(byteCode, tokens);
					byteCode.add(ByteCode._ADD);
					if (tokens.endOfStatement())
						break;
				}
				/*
				 * Optimization - if there was only one item in the list
				 * then we don't need to do the work of creating an array
				 * temporary for it.
				 */
				
				if( listCount == 1 ) {
					byteCode.remove(listBase);
					byteCode.remove(byteCode.size()-1);
					
				}

				byteCode.add(ByteCode._FOREACH, index);
			} else {
				// Parse TO

				if (!tokens.assumeNextToken("TO"))
					return new Status(Status.SYNEXPTOK, "TO");

				// Parse ending value

				exp.compile(byteCode, tokens);
				if (exp.status.failed())
					return exp.status;

				/*
				 * If there is an optional BY statement then handle it. We allow
				 * the possibility of using the keyword STEP as well as BY for
				 * the increment value.
				 */
				if (!tokens.assumeNextToken(new String[] { "STEP", "BY" }))
					byteCode.add(ByteCode._INTEGER, 1);
				else {
					exp.compile(byteCode, tokens);
					if (exp.status.failed())
						return exp.status;
				}

				byteCode.add(ByteCode._FOR, index);
				indent = 1;

			}
		}
		
		/*
		 * See if there is a DO clause that we execute as part of this
		 * statement. This is a statement that is executed as the body
		 * of the loop, with an implicit NEXT for the index generated
		 * automatically.
		 */
		
		if( tokens.assumeNextToken("DO")) {
			
			/*
			 * Remember the current location, because this will be used
			 * to back-patch the FOR or FOREACH bytecode that was just
			 * generated.
			 */
			int forPos = byteCode.size()-1;
			
			/*
			 * Generate the code for the DO statement.  This is done by
			 * fetching the unused part of the existing buffer, and storing
			 * it in a new statement (which also forces a compile of the
			 * code). 
			 * 
			 * Note that we have to copy our current program definition
			 * to the new statement so you can compile statements that are
			 * only legal for inclusion in code (RETURN, etc.) in the DO
			 * clause.  This is accomplished by using the constructor that
			 * includes the parent statement.
			 * 
			 * If an error occurs during the store/compile operation, 
			 * bail out and delete any previously generated code.
			 */
			
			Statement doClause = new Statement(this);

			doClause.store(tokens.getBuffer());

			if( doClause.status.failed()) {
				byteCode = null;
				return status;
			}
			
			/*
			 * The generated code is grabbed from the temporary statement
			 * and added to our existing statement's generated code.  Also,
			 * the token buffer is flushed since all tokens in that stream
			 * where handed off and processed by the DO clause statement.
			 */
			
			byteCode.concat(doClause.byteCode);
			tokens.flush();

			/*
			 * Patch up the FOR/NEXT operations.  The FOR (or FOREACH)
			 * must have a forward link to the NEXT in the event that 
			 * the loop needs to run zero times.  The NEXT is also
			 * generated with the address of the first instruction in
			 * the body of the loop for the backwards branch to run the
			 * loop body again.
			 */
			Instruction forInstruction = byteCode.getInstruction(forPos);
			forInstruction.integerOperand = byteCode.size()+1;
			forInstruction.integerValid = true;
			byteCode.add(ByteCode._NEXT, forPos+1, index);
			
			/*
			 * If there is no program associated with this code stream,
			 * we must generate a stub program. This is needed to hold the
			 * loop stack used by the FOR/FOREACH and NEXT operations.
			 * 
			 * NOTE: This program is *never* registered, so it is not visible
			 * or available for any other use, and after the statement object
			 * is deleted, the program object will be deleted as well.
			 */
			if (program == null ) {
				Program dummyProgram = new Program(session, "DUMMY");
				dummyProgram.fIsStub = true;
				this.program = dummyProgram;
			}
			indent = 0;
			return new Status();
		}
		/*
		 * Otherwise, we MUST have an active program or we cannot execute
		 * this code.
		 */
		if (program == null) {
			byteCode = null;
			return new Status(Status.NOACTIVEPGM);
		}
		indent = 1;
		return new Status();

	}

}

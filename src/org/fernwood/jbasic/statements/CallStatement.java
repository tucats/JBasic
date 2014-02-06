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
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * CALL statement handler. This is a compiled statement (it has a compile()
 * method) and generates bytecode which is later executed.
 * <p>
 * The CALL statement temporarily invokes another program by name, and
 * optionally passes parameters to that program.  The program must be
 * loaded in memory by the time the CALL statement is executed.
 * <p>
 * <b>SYNTAX</b>
 * <p>
 * <code>
 * 
 * 		CALL <em>program</em> ( [<em>arg</em>[, <em>arg</em>[, <em>arg</em>...])
 * 	
 * </code>
 * <p>
 * <b>USAGE</b>
 * <p>
 * <em>program</em> is the name of the program to run.
 * <p>
 * The arguments are stored in the local program's symbol table before it is
 * run, in an array named $ARGS. So the first argument is found in $ARG[1], the
 * second in $ARG[2], etc. The program can also use the LENGTH($ARGS) call to
 * determine the number of elements in the argument array.
 * <p>
 * You can specify the name of the program to call using an expression which
 * resolves to a string used as the procedure name.  This is done by using a
 * <code>USING(<em>string-expression</em>)</code> clause in place of the
 * <em>program</em> name above.  The string expression must contain the name
 * of a valid program or SUB routine, or an error occurs at runtime.
 * <p>
 * You can optionally add the <code>AS THREAD</code> clause after the
 * argument list. In this case, the arguments are all resolved in the local
 * context, and then passed to an instance of the program in a new thread.
 * <p>
 * Alternatively, you can specify a <code>RETURNING <em>name</em></code> clause
 * after the argument list that indicates that if a RETURN statement is executed
 * in the subprocedure, it can pass a value back to the calling program.  This
 * makes the operation act more like a function with a return value.  If the
 * <code>RETURNING</code> clause is used, the called program <em>must</em> 
 * supply the value.
 * @author cole
 * 
 */

class CallStatement extends Statement {

	/**
	 * Compile 'CALL' statement. Processes a token stream, and compiles it into
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
		 * Create the bytecode for generating the statement.
		 */
		byteCode = new ByteCode(session, this);

		/*
		 * Set the initial state of the parsing flags, that will keep up
		 * with which clauses have been seen in the statement.
		 */
		String targetProgramName = "(indirect)";
		boolean isIndirect = false;
		boolean isThread = false;
		boolean hasReturn = false;
		boolean hasTarget = false;
		int argumentCount = 0;

		/*
		 * We will also need an expression compiler to handle the procedure
		 * arguments.
		 */
		final Expression exp = new Expression(session);
		int argOffset = 0;

		LValue returnValue = null;

		LValue threadName = null;
		
		/*
		 * There are three clauses in a CALL Statement in our dialect.
		 * 
		 * 1.  AS THREAD which enables the call to run on a new thread.
		 * 2.  RETURNS <var> which specifies a return LVALUE
		 * 3.  The name of the function and it's parameters
		 * 
		 * This loop parses each one of them as needed.
		 */
		while( true ) {

			/*
			 * If we've come to the end of the statement, break out now.
			 */
			if( tokens.endOfStatement())
				break;

			/*
			 * Look ahead two tokens to see if this is an AS THREAD clause
			 * at the start of the statement.  IF so, eat the tokens and
			 * note that this is a thread invocation.
			 */
			if (tokens.peek(0).equals("AS") & tokens.peek(1).equals("THREAD")) {
				tokens.assumeNextToken("AS");
				tokens.assumeNextToken("THREAD");
				if( isThread ) 
					return new Status(Status.DUPCLAUSE, "AS THREAD");
				
				if( tokens.assumeNextSpecial("(")) {
					threadName = new LValue(session, false);
					threadName.compileLValue(byteCode, tokens);
					if( threadName.error)
						return new Status(Status.LVALUE);
					
					if(!tokens.assumeNextSpecial(")"))
							return new Status(Status.PAREN);
				}
				isThread = true;
				continue;
			}

			/*
			 * If the next token is the RETURNS clause, get the returnName value.
			 */
			if (tokens.assumeNextToken(new String[] { "RETURNING", "RETURNS"})) {

				/*
				 * Note that a RETURNS clause is here.  Also, if there are
				 * parenthesis around the RETURNS(x) variable name then eat
				 * those and remember we found them.
				 */
				
				if( hasReturn )
					return new Status(Status.DUPCLAUSE, "RETURNS");
				
				hasReturn = true;
				boolean hasParens = tokens.assumeNextSpecial("(");
				
				/*
				 * Parse the name of the value to be written to, and then
				 * generate the code to within the lvalue that knows how
				 * to reference the destination lvalue, particularly if it
				 * contains an array subscript reference.  This will be used
				 * later in the main bytecode program.
				 */
				returnValue = new LValue(this.session, false);
				if( !returnValue.error)
					returnValue.compileLValue(byteCode, tokens);
				
				/*
				 * If there was an error in the parsing or compilation of the
				 * LValue reference, report a compilation error.  Otherwise,
				 * if we had parenthesis around the lvalue then require the
				 * closing paren now.  Loop to the next clause.
				 */
				if( returnValue.error)
					return new Status(Status.INVRET);
				if( hasParens ) {
					if( !tokens.assumeNextSpecial(")"))
							return new Status(Status.PAREN);
				}
				continue;

			}
			/*
			 * We allow the user to indirectly specify the program to be called,
			 * with the USING(expr) clause.  If present, parse the expression
			 * so the result is on the stack, and note that this is an indirect
			 * invocation.
			 */
			if (tokens.assumeNextToken("USING")) {

				if( isIndirect & hasTarget )
					return new Status(Status.DUPCLAUSE, "USING");
				
				if( hasTarget ) {
					tokens.restoreToken();
					break;
				}

				if (!tokens.assumeNextToken("("))
					return new Status(Status.PROGRAM);

				exp.compile(byteCode, tokens);
				if (exp.status.failed())
					return exp.status;
				if (!tokens.assumeNextToken(")"))
					return new Status(Status.PAREN);
				isIndirect = true;
				hasTarget = true;
			} else {

				if( hasTarget )
					break;
				/*
				 * It's not a USING indirect invocation, so we just parse the name
				 * of the program.
				 */
				targetProgramName = tokens.nextToken();
				if (!tokens.isType(Tokenizer.IDENTIFIER))
					return new Status(Status.EXPPGM);
				isIndirect = false;
				hasTarget = true;
				/*
				 * It could be a method call of the form OBJECT->METHOD(arg), so
				 * see if that's the case.  If so, add code to resolve the method
				 * name from the object reference, and remember that this has to
				 * now be an indirect call.
				 */
				if (tokens.assumeNextToken("->")) {
					String methName = tokens.nextToken();
					if (!tokens.isType(Tokenizer.IDENTIFIER))
						return new Status(Status.INVNAME, methName);
					byteCode.add(ByteCode._LOADREF, targetProgramName);
					byteCode.add(ByteCode._STRING, methName);
					byteCode.add(ByteCode._METHOD, targetProgramName);
					argOffset = 1;
					isIndirect = true;
				}
			}


			/*
			 * Parse the argument list if there is one, and push on the stack. Allow
			 * for the case of an empty argument list.
			 */

			if (tokens.assumeNextToken("("))
				if (!tokens.assumeNextToken(")"))
					while (!tokens.testNextToken(")")) {

						status = exp.compile(byteCode, tokens);
						if (status.failed())
							return status;
						argumentCount++;
						if (tokens.assumeNextToken(","))
							continue;
						if (!tokens.assumeNextToken(")"))
							return new Status(Status.PAREN);
						break;
					}
			continue;
		}

		/*
		 * All done parsing clauses.  Do some consistency checks.
		 */
		if( isThread & hasReturn ) 
			return new Status(Status.THREADRET, targetProgramName);

		if( !hasTarget ) {
			return new Status(Status.EXPCALL);
		}

		/* Based on the type of call we have, generate the correct bytecode
		 * value.
		 * 
		 * 	_CALLM		Method call
		 * 	_CALLP		Procedure call (name is on stack or instruction arg)
		 * 	_CALLT		Thread invocation
		 */
		if (argOffset > 0)
			byteCode.add(ByteCode._CALLM, argumentCount);
		else {
			int code = ByteCode._CALLP;
			if (isThread)
				code = ByteCode._CALLT;
			if (isIndirect)
				byteCode.add(code, argumentCount);
			else
				byteCode.add(code, argumentCount, targetProgramName);
		}

		/*
		 * If it was a thread call, there is a return value that is the
		 * name of the thread created.  We may want to store it if the
		 * caller asked for it.
		 */
		if( isThread ) {
			if( threadName == null )
				byteCode.add(ByteCode._DROP);
			else
				threadName.compileStore();
		}
		/*
		 * If there was a return value, generate the code to get it now
		 * from the stack and write it to the lvalue.  The code previously
		 * generated to access the LVALUE is now copied to the bytecode stream.
		 */
		if( hasReturn ) {
			byteCode.add(ByteCode._RESULT);
			if( returnValue != null )
				returnValue.compileStore();
		}

		return new Status(Status.SUCCESS);

	}

}
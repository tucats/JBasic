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
package org.fernwood.jbasic.compiler;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Functions;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.MockSQLStatement;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;
import java.util.Vector;

/**
 * This class handles basic expression evaluation, in concert with the symbol
 * table class. This is implemented as a top-down recursive descent parser that
 * generates RPN-style math operators that depend on the runtime stack to hold
 * temporary values.  <br>
 * <br>
 * The scalar operations for numerical values are standard in their
 * implementation; when in doubt, values are promoted to doubles to ensure
 * meaningful math operations. <br>
 * <br>
 * Strings have some specially adapted operators. The "+" operator means
 * concatenation for strings such as
 * <code>"Abe" + "Lincoln" = "AbeLincoln"</code>, and the "-" means a
 * substring is removed from the target string if found; i.e.
 * <code> "ABCDE" - "CD" = "ABE"</code>.<br>
 * <br>
 * Additionally, arrays can have another ARRAY value added to it or have a scalar
 * added to it; in either case the arrayValue or scalar is copied to the end of
 * the arrayValue, adding new elements. So
 * <code>[ 1, 2 ] + [ "tom" ] = [ 1, 2, "tom" ]</code><br>
 * <br>
 * <p>
 * The expression processor can be used to compile an expression into a byte code
 * stream for later execution, or can be asked to process the expression text
 * immediately.  In this later case, the code is actually generated into a temporary
 * byte code buffer and then executed immediately, and the result of the expression
 * removed from the temporary data stack and returned as the expression result.
 * 
 * 
 * @author Tom Cole
 * @version version 1.4 Sept 2008
 * 
 */

public class Expression {

	/**
	 * The session that "owns" this expression.  This is used for accessing global data.
	 */
	JBasic session;

	/**
	 * This flag tracks recursion in the expression handler, and is used to detect when
	 * an expression references a value (even with sub-expressions in indexes, etc.) versus
	 * a temporary expression like <code>N+1</code>
	 */
	int recursionDepth;
	
	/**
	 * This is the initial recursion value when a new expression is evaluated.
	 * This is set to a non-zero value when we want to force the behavior(s)
	 * from recursive evaluation on all aspects of a specific expression 
	 * evaluation operation.  For example, constant pooling is not supported
	 * when in a recursive constant expression; this can be forced by setting
	 * the recursionStart to 1. 
	 */
	private int recursionStart;

	/**
	 * Flag indicating the status of most recent expression evaluation. A failed
	 * expression returns a null pointer but does not have a way of directly
	 * reporting an error. So we store the error code here for later retrieval.
	 */
	public Status status;

	/**
	 * Flag indicating if the expression is a single value or variable
	 * reference, as opposed to a complex operation. That is, did a single load
	 * or constant instruction get generated for this operation?
	 */
	public boolean isSingleton;

	/**
	 * Flag indicating if the expressions is a constant as opposed to one 
	 * containing variable references or function calls.
	 */
	public boolean isConstant;
	
	/**
	 * Flag indicating if the expression is a single scalar variable without
	 * complex addressing modes (like an array or record member reference).
	 * This is used to optimize some operations like a PUT statement which 
	 * can generate a direct load of the variable versus a value on the
	 * stack.  If the expression is a single variable then the name is 
	 * stored in <code>variableName</code>.
	 */
	public boolean isVariable;

	/**
	 * This flag indicates if the expression referenced a value (even if it is a
	 * composite value) as opposed to a temporary value created by an expression.
	 * For example, <code>X[N*2].AGE</code> is a value, but <code>X+3</code> is
	 * an expression.
	 */
	public boolean isExpression;
	
	/**
	 * The most recently parsed variable name. This is used when the isSingleton
	 * flag is set, to indicate the variable that could be optimized for a
	 * direct load or store.
	 */
	public String variableName;

	/**
	 * Vector of operation code for the expression. 
	 */
	private ByteCode byteCodes;

	/**
	 * Internal flag that indicates if we are processing an ELSE clause, which 
	 * can change the way we recognize the keyword as the end of an expression 
	 * as opposed to an identifier.
	 */
	boolean fElse;
	
	/**
	 * Internal flag indicating if we are attempting to pool array constants.
	 */
	private boolean fArrayConstantPooling;

	/**
	 * Internal flag that causes us to ignore pre- and post- increment
	 * operations.  This is needed for statements like ADD 3 TO A[B++].
	 * The increment is processed on the use of A[B++] as an addend, 
	 * but not processed again when it is used as a destination.
	 */
	
	private boolean fIgnoreIncrements;
	
	/**
	 * Flag indicating if last expression element was a NEW() function call.
	 * This is used to conditionally check for the OF clause that can only
	 * follow a NEW() function call to define object container state.
	 */
	private boolean fNEWFunctionCall;

	private Vector <String> postList;

	private boolean fDeferPosts;

	/**
	 * Flag used to indicate that this expression is part of a SQL SELECT
	 * reference.  This means, among other things, that record references
	 * generate extra code to validate that a catalog is really a catalog.
	 */
	private boolean isSQL;

	/**
	 * When constant pooling is enabled, this is the prefix used to create the
	 * name(s) of the constant RECORD data types.  A unique integer sequence
	 * number is added after this string value to form the symbol names.
	 */

	private static final String RECORD_CONSTANT_PREFIX = "__RECORD$";
	
	/**
	 * When constant pooling is enabled, this is the prefix used to create the
	 * name(s) of the constant ARRAY data types.  A unique integer sequence
	 * number is added after this string value to form the symbol names.
	 */

	private static final String ARRAY_CONSTANT_PREFIX = "__ARRAY$";


	/**
	 * Create an expression evaluator object. This object can be used to
	 * <code>evaluate()</code> an expression immediately and return a result,
	 * or <code>compile()</code> an expression into a ByteCode object stream
	 * for later execution.
	 * <p>
	 * The expression does not have any context for evaluation or compilation
	 * after being created, these must be supplied in the evaluate() or
	 * compile() method calls.
	 * 
	 * @param jb
	 *            The JBasic object that contains this session
	 * 
	 */
	public Expression(final JBasic jb) {
		session = jb;
		byteCodes = null;
		isSingleton = true;
		isConstant = true;
		isVariable = false;
		variableName = null;
		status = new Status();
		if( jb != null )
			setArrayConstantPooling(jb.getBoolean("SYS$STRUCTURE_POOLING"));
		else
			setArrayConstantPooling(true);
	}

	/**
	 * Create an expression evaluator object. This object can be used to
	 * <code>evaluate()</code> an expression immediately and return a result,
	 * or <code>compile()</code> an expression into a ByteCode object stream
	 * for later execution.
	 * <p>
	 * The expression retains a pointer to a ByteCode object which will hold any
	 * generated code created on behalf of compilations. If you use the
	 * evaluate() method instead, an internal temporary bytecode area is used to
	 * hold that code stream.
	 * <p>
	 * The expression does not have any context for evaluation or compilation
	 * after being created, these must be supplied in the evaluate() or
	 * compile() method calls.
	 * 
	 * @param jb
	 *            The instance of JBasic which hosts the statement being
	 *            compiled.
	 * @param bc
	 *            The ByteCode object to be used to store code from compile
	 *            operation on this expression.
	 * 
	 */

	public Expression(final JBasic jb, final ByteCode bc) {
		session = jb;
		byteCodes = bc;
		isSingleton = true;
		isConstant = true;
		isVariable = false;
		variableName = null;
		status = new Status();
		if( jb != null )
			setArrayConstantPooling(jb.getBoolean("SYS$STRUCTURE_POOLING"));
		else
			setArrayConstantPooling(true);
	}

	/**
	 * Constructor for an expression that is to be interpretively evaluated
	 * (rather than compiled). The token stream and symbol table are used to
	 * parse, compile, and execute the expression contained in the token stream
	 * immediately. The result of the expression is available via the
	 * <code>getResult()</code> method for the object.
	 * 
	 * @param jb
	 *            The JBasic object that contains this session
	 * @param tokens
	 *            String of tokens from which to immediately parse and execute
	 *            an expression
	 * @param symbols
	 *            Symbol table to use during expression evaluation
	 */
	public Expression(final JBasic jb, final Tokenizer tokens,
			final SymbolTable symbols) {
		byteCodes = null;
		session = jb;
		isSingleton = true;
		isConstant = true;
		isVariable = false;
		variableName = null;
		status = new Status();
		if( jb != null )
			setArrayConstantPooling(jb.getBoolean("SYS$STRUCTURE_POOLING"));
		else
			setArrayConstantPooling(true);
		evaluate(tokens, symbols);
	}

	/**
	 * Evaluate a token stream expression with a symbol table.
	 * 
	 * @param tok
	 *            The tokenizer structure already established.
	 * @param s
	 *            The symbol table to use
	 * @return The result of the expression evaluation.
	 */
	public Value evaluate(final Tokenizer tok, final SymbolTable s) {
		return compileAndRun(tok, s);
	}

	
	/**
	 * Type coercion handler. If the elements are numeric, promote  
	 * to the numeric type which results in no loss of precision. If 
	 * either element is a string, promote both values to strings.
	 * 
	 * @param value1
	 *            first item to coerce
	 * @param value2
	 *            second item to coerce
	 * @return the type that both values have been coerced to.  This may be
	 * the same as the original type if no conversion is needed.
	 * @throws JBasicException if the types are incompatible
	 */

	public static int coerceTypes(final Value value1, final Value value2) throws JBasicException {

		final int type1 = value1.getType();
		final int type2 = value2.getType();

		/*
		 * If the types already match, we're done.
		 */
		if (type1 == type2)
			return type1;

		/*
		 * If we're converting to a string, just do it and be done with it.
		 */

		if (type1 == Value.STRING) {
			value2.coerce(Value.STRING);
			return type1;
		}

		/*
		 * Otherwise we need to find the type that loses the least precision.
		 * Note that this requires that the types be in ascending order of
		 * precision, numerically. See the Values class for more info.
		 */

		final int newType = Math.max(type1, type2);
		value1.coerce(newType);
		value2.coerce(newType);
		return newType;

	}

	/**
	 * Given two values, find the best type to convert them both to, in order
	 * to be able to perform expression-based operations on them. 
	 * @param value1 Value to evaluate
	 * @param value2 Value to evaluate
	 * @return an integer reflecting the simplest truncation-free common type
	 */
	public static int bestType(final Value value1, final Value value2) {

		final int type1 = value1.getType();
		final int type2 = value2.getType();

		/*
		 * If the types already match, we're done.
		 */
		if (type1 == type2)
			return type1;

		/*
		 * If we're converting to a string, just do it and be done with it.
		 */

		if (type1 == Value.STRING) {
			return type1;
		}

		if( type1 == Value.ARRAY && type2 == Value.TABLE ||
			type1 == Value.TABLE && type2 == Value.ARRAY )
			return Value.ARRAY;
		/*
		 * Otherwise we need to find the type that loses the least precision.
		 * Note that this requires that the types be in ascending order of
		 * precision, numerically. See the Values class for more info.
		 */

		final int newType = Math.max(type1, type2);
		
		return newType;

	}

	/*
	 * COMPILE MODULES
	 */

	/**
	 * Compile an expression, generating bytecode into the given ByteCode
	 * object. Consumes as many tokens as are necessary to make a complete
	 * expression.
	 * 
	 * @param outputInstructions
	 *            The previously-created ByteCode object that receives the
	 *            generated code. The code generation stream arranges to process
	 *            the expression and leave the result on the runtime stack.
	 * @param tokens
	 *            The Tokenizer stream used to read the source for the
	 *            expression.
	 * @return A Status value that indicates if a syntax error occurring during
	 *         compilation. Status.SUCCESS is the normal result.
	 * 
	 */
	public Status compile(final ByteCode outputInstructions,
			final Tokenizer tokens) {
		
		/*
		 * Initialize expression state flags to known conditions.
		 */
		isSingleton = true;
		isVariable = false;
		isConstant = false;
		variableName = null;
		byteCodes = outputInstructions;
		status = new Status(Status.SUCCESS);
		isExpression = false;
		recursionDepth = recursionStart;
		
		/*
		 * Special case; if ELSE is the next token and is currently
		 * considered a reserved word by the active tokenizer, then
		 * we have no work to do since there was no expression here.
		 */
		if (tokens.fReserveElse && tokens.testNextToken("ELSE"))
			return status;

		/*
		 * Compile the expression starting with the least-significant
		 * order of precedence operators.
		 */
		isConstant = true;
		status = compileBoolean(tokens);
		if (status == null)
			status = new Status();
		
		if( status.success()) {
			status = Linker.resolveIF(byteCodes);
			if( !fDeferPosts)
				Expression.postIncrements(postList, byteCodes);
		}
		
		return status;
	}

	/*
	 * Compile boolean (AND, OR) expressions, which may contain sub-
	 * expressions themselves.
	 */
	private Status compileBoolean(final Tokenizer tokens) {

		int action = 0;
		recursionDepth++;
		
		status = compileRelations(tokens);
		if (status == null)
			status = new Status();
		if (status.failed()) {
			recursionDepth--;
			return status;
		}
		
		while (tokens.fActiveParse) {
			String t = tokens.nextToken();
			int kind = tokens.getType();
			boolean valid = false;

			if (kind == Tokenizer.IDENTIFIER) {
				if (t.equals("AND")) {
					kind = Tokenizer.SPECIAL;
					t = "&";
					valid = true;
					action = ByteCode._AND;
				} else if (t.equals("OR")) {
					kind = Tokenizer.SPECIAL;
					t = "|";
					valid = true;
					action = ByteCode._OR;
				}
			}
			if (kind != Tokenizer.SPECIAL) {
				tokens.restoreToken();
				recursionDepth--;
				return status;
			}
			if (!valid)
				if (t.equals("&")) {
					valid = true;
					action = ByteCode._AND;
				}
			if (t.equals("|")) {
				valid = true;
				action = ByteCode._OR;
			}

			if (!valid) {
				tokens.restoreToken();
				recursionDepth--;
				return status;
			}
			if( recursionDepth == 1 )
				isExpression = true;

			status = compileRelations(tokens);
			if (status.failed()) {
				recursionDepth--;
				return status;
			}
			isConstant = false;
			byteCodes.add(action);
		}
		recursionDepth--;
		return status;
	}

	private Status compileRelations(final Tokenizer tokens) {

		int action;

		status = compileAdditive(tokens);
		if (status == null)
			status = new Status();
		if (status.failed())
			return status;

		while (tokens.fActiveParse) {

			tokens.nextToken();
			
			if(( tokens.getSpelling().equals("WHERE"))) {

				int wherePos = byteCodes.add(ByteCode._WHERE, 0);
				status = compileRelations(tokens);
				if( status.failed())
					return status;
				Instruction i = byteCodes.getInstruction(wherePos);
				i.integerOperand = (byteCodes.size() - wherePos) - 1;
				isSingleton = false;
				isVariable = false;
				isConstant = false;
				variableName = null;
				
				continue;
			}
			if(( tokens.getSpelling().equals("MIN") || tokens.getSpelling().equals("MAX"))
					&& tokens.getType() == Tokenizer.IDENTIFIER) {
				isSingleton = false;
				isVariable = false;
				isConstant = false;
				variableName = null;
				String fn = tokens.getSpelling();
				
				int op = fn.equals("MIN") ? ByteCode._MIN : ByteCode._MAX;
				
				if( recursionDepth == 1 )
					isExpression = true;

				status = compileAdditive(tokens);
				if (status.failed()) {
					return status;
				}
				
				byteCodes.add(op);
				continue;
			}
			
			if( tokens.getSpelling().equals("IN") && tokens.getType() == Tokenizer.IDENTIFIER) {
				if( !tokens.assumeNextSpecial("("))
					return status = new Status(Status.PAREN);
				
				boolean argsConstant = false;
				if( byteCodes.statement != null )
					if( byteCodes.statement.program != null )
						argsConstant = true;
				
				Expression inArguments = new Expression(session);
				inArguments.setIgnoreIncrements(this.fIgnoreIncrements);
				int listStart = byteCodes.size();
				int count = 0;
				while(true) {
					inArguments.compile(this.byteCodes, tokens);
					if( inArguments.status.failed())
						return inArguments.status;
					if( !inArguments.isConstant)
						argsConstant = false;
					count++;
					if( !tokens.assumeNextSpecial(","))
						break;
				}
				if( count > 1 )
					byteCodes.add(ByteCode._ARRAY, count);

				if( argsConstant) {
					count = byteCodes.size() - listStart;
					String name = "__CONSTANT$" + Integer.toString(JBasic.getUniqueID());
					byteCodes.insert(listStart, new Instruction(ByteCode._CONSTANT, count, name));
					byteCodes.add(ByteCode._LOADREF, name);
				}
				if( !tokens.assumeNextSpecial(")"))
					return status = new Status(Status.PAREN);

				byteCodes.add(ByteCode._CALLF, 2, "IN");
				return new Status();
			}
			else
			if (tokens.isType(Tokenizer.SPECIAL)) {

				int saved = tokens.getPosition()-1;
				
				final String relation = tokens.getSpelling();

				if (relation.equals("<"))
					action = ByteCode._LT;
				else if (relation.equals("<="))
					action = ByteCode._LE;
				else if (relation.equals(">"))
					action = ByteCode._GT;
				else if (relation.equals(">="))
					action = ByteCode._GE;
				else if (relation.equals("="))
					action = ByteCode._EQ;
				else if (relation.equals("<>") | relation.equals("!="))
					action = ByteCode._NE;
				else
					action = 0;

				if (action > 0) {
					if( tokens.assumeNextSpecial(new String [] {",", ";", ":"})) {
						tokens.setPosition(saved);
						return status;
					}
					if( tokens.testNextToken(Tokenizer.END_OF_STRING)) {
						tokens.setPosition(saved);
						return status;
					}

					isSingleton = false;
					isVariable = false;
					isConstant = false;
					variableName = null;
					if( recursionDepth == 1 )
						isExpression = true;

					status = compileAdditive(tokens);
					if (status.failed()) {
						return status;
					}
					
					byteCodes.add(action);

				} else {
					tokens.restoreToken();
					return status;
				}

			} else {
				tokens.restoreToken();
				return status;
			}
		}

		return status;

	}

	private Status compileAdditive(final Tokenizer tokens) {
		int action;

		status = compileMultiplicative(tokens);
		if (status == null)
			status = new Status();
		if (status.failed())
			return status;

		while (tokens.fActiveParse) {

			tokens.nextToken();
			action = 0;


			if (tokens.isType(Tokenizer.SPECIAL)) {
				if (tokens.getSpelling().equals("+"))
					action = ByteCode._ADD;
				else if (tokens.getSpelling().equals("-"))
					action = ByteCode._SUB;
				else if (tokens.getSpelling().equals("||"))
					action = ByteCode._CONCAT;
			}

			if (action > 0) {
				isSingleton = false;
				isVariable = false;
				isConstant = false;
				variableName = null;
				if( recursionDepth == 1 )
					isExpression = true;

				status = compileMultiplicative(tokens);
				if (status.failed())
					return status;

				byteCodes.add(action);

			} else {
				tokens.restoreToken();
				return status;
			}


		}

		return status;

	}

	private Status compileMultiplicative(final Tokenizer tokens) {

		int action;

		status = compileExponentOrOF(tokens);
		if (status == null)
			status = new Status();
		if (status.failed())
			return status;

		while (tokens.fActiveParse) {

			tokens.nextToken();
			if (tokens.isType(Tokenizer.SPECIAL)) {

				if (tokens.getSpelling().equals("*"))
					action = ByteCode._MULT;
				else if (tokens.getSpelling().equals("/"))
					action = ByteCode._DIV;
				else if (tokens.getSpelling().equals("%"))
					action = ByteCode._MOD;
				else
					action = 0;

				if (action > 0) {
					isSingleton = false;
					isConstant = false;
					isVariable = false;
					variableName = null;
					if( recursionDepth == 1 )
						isExpression = true;
					status = compileExponentOrOF(tokens);
					if (status.failed())
						return status;
					byteCodes.add(action);
				} else {
					tokens.restoreToken();
					return status;
				}
			} else {
				tokens.restoreToken();
				return status;
			}
		}

		/* All done. */
		return status;

	}

	private Status compileExponentOrOF(final Tokenizer tokens) {

		int action = 0;

		status = compileAtom(tokens);
		if (status == null)
			status = new Status();
		if (status.failed())
			return status;

		while (tokens.fActiveParse) {

			tokens.nextToken();

			/*
			 * If the last expression atom was a NEW() function call, allow "OF"
			 * as an operator.  This is a one-shot flag; always reset the state.
			 */
			boolean isOF = this.fNEWFunctionCall
					&& tokens.getSpelling().equals("OF")
					&& tokens.getType() == Tokenizer.IDENTIFIER;
			fNEWFunctionCall = false;

			if (tokens.isType(Tokenizer.SPECIAL) || isOF) {

				if (isOF)
					action = ByteCode._OF;
				else 
					if (tokens.getSpelling().equals("^")) {
						if( recursionDepth == 1 )
							isExpression = true;
						action = ByteCode._EXP;
					}
					else
						action = 0;

				if (action > 0) {
					isSingleton = false;
					isVariable = false;
					isConstant = false;
					variableName = null;
					status = compileAtom(tokens);
					if (status.failed())
						return status;
					byteCodes.add(action);
				} else {
					tokens.restoreToken();
					return status;
				}
			} else {
				tokens.restoreToken();
				return status;
			}
		}

		/* Must reset this here in case there were no cascading operators */
		fNEWFunctionCall = false;

		/* All done. */
		return status;

	}

	
	/**
	 * Compile support for atomic expression elements.  These are constant
	 * values, symbols, parenthesis expressions, function calls and object
	 * dereferences.
	 * @param tokens the token stream to compile
	 * @return a Status object indicating the success of the compilation
	 */

	public Status compileAtom(final Tokenizer tokens) {

		if (status == null)
			status = new Status();
		boolean negate = false;
		if (tokens.testNextToken(Tokenizer.END_OF_STRING))
			return status = new Status(Status.EXPRESSION);

		/*
		 * Hokey case. IF the token is ELSE and we are in an IF statement, then
		 * err on the side of caution and assume this is the end of the token
		 * stream.
		 */

		if (tokens.fReserveElse && tokens.testNextToken("ELSE"))
			return status;

		if (tokens.assumeNextToken("IF")) {
			status = compileBoolean(tokens);
			if( status.failed())
				return status;
			
			byteCodes.add(ByteCode._IF, 1);
			if( !tokens.assumeNextToken("THEN"))
				return status = new Status(Status.IF);
			
			status = compileBoolean(tokens);
			if( status.failed())
				return status;
			
			if( !tokens.assumeNextToken("ELSE"))
				return new Status(Status.IF);
			
			byteCodes.add(ByteCode._IF, 2 );
			status = compileBoolean(tokens);
			if( status.failed())
				return status;

			isVariable = false;
			isSingleton = false;
			isConstant = false;
	
			byteCodes.add(ByteCode._IF, 3 );
			
			/*
			 * We need a NOOP as a place-holder to be sure that the optimizer
			 * doesn't get fooled by intra-statement branches.  This means that
			 * IF..THEN..ELSE expressions won't be fully optimized.
			 */
			byteCodes.add(ByteCode._NOOP);
			
			return status;
		}
		
		if (tokens.assumeNextToken("RND")) {
			byteCodes.add(ByteCode._RAND, 0);
			isVariable = false;
			isSingleton = true;
			isConstant = false;
			return status;
		}

		/*
		 * See if it is a pre-increment or pre-decrement operator, in which
		 * case it must be followed by a variable name.
		 */
		
		if( tokens.assumeNextSpecial(new String [] { "--", "++"})) {
			int incrementValue = tokens.getSpelling().equals("--") ? -1 : 1;
			if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
				return new Status(Status.EXPNAME);
			String name = tokens.nextToken();
			
			if(!fIgnoreIncrements) {
				byteCodes.add(ByteCode._INCR, incrementValue, name );
				byteCodes.add(ByteCode._LOADREF, name);
			}
			isVariable = true;
			isSingleton = true;
			isConstant = false;
			return status;
		}
		/*
		 * See if it's the unary NOT prefix. This generates a sub-expression and
		 * then negates it.
		 */

		if (tokens.assumeNextSpecial("!")) {
			tokens.restoreToken("NOT", Tokenizer.IDENTIFIER);
			isSingleton = false;
			isVariable = false;
			isConstant = false;
			variableName = null;
		}

		if (tokens.assumeNextSpecial("-")) {
			status = compileAtom(tokens);
			byteCodes.add(ByteCode._NEGATE);
			isSingleton = false;
			isVariable = false;
			isConstant = false;
			variableName = null;
			if( recursionDepth == 1 )
				isExpression = true;
			return status;
		}

		if (tokens.assumeNextToken("NOT")) {

			status = compileAtom(tokens);
			byteCodes.add(ByteCode._NOT);
			isSingleton = false;
			isVariable = false;
			isConstant = false;
			variableName = null;
			if( recursionDepth == 1 )
				isExpression = true;
			return status;
		}
		/*
		 * Let's see if it's an RECORD atom. This is a string of member
		 * names and expressions in braces {} that is created as a single RECORD element.
		 */

		if (tokens.assumeNextSpecial("{")) {
			isExpression = true;
			return compileRecordValue(tokens);
		}
		/*
		 * Let's see if it's an arrayValue atom. This is a string of expressions
		 * in square brackets "[]" that is created as a single arrayValue element.
		 */

		if (tokens.assumeNextSpecial("[")) {
			isExpression = true;
			return compileArrayValue(tokens);
		}
		// Let's see what the next token is.

		tokens.nextToken();

		if (tokens.isSpecial(".")) {
			byteCodes.add(ByteCode._DOUBLE, 3);
			if( recursionDepth == 1 )
				isExpression = true;
			return new Status();
		}

		/* 
		 * Special cases. If it's a unary sign, then note it, and get
		 * another token. We'll only get this back for cases like
		 * -(exp) or -IDENT
		 */

		if (tokens.isSpecial("-")) {
			tokens.nextToken();
			negate = true;
			isSingleton = false;
			isVariable = false;
			isConstant = false;
			variableName = null;
			isExpression = true;
		} else
			negate = false;

		if (tokens.isSpecial("+")) {
			tokens.nextToken();
			isSingleton = false;
			isVariable = false;
			isConstant = false;
			variableName = null;
			isExpression = true;
			negate = false;
		}

		tokens.fUnarySign = false;

		/*
		 * First case. If this is a parenthiated expression, then recurse
		 * to the top of the expression evaluator, and return the whole
		 * result of the subexpression. Handle unary negation afterwards.
		 */

		if (tokens.isSpecial("(")) {
			isSingleton = false;
			isVariable = false;
			variableName = null;

			status = compileBoolean(tokens);
			tokens.nextToken();
			if (!tokens.isSpecial(")")) {
				status = new Status(Status.PAREN);
				return status;
			}
			if (negate)
				byteCodes.add(ByteCode._NEGATE);
			return status;
		}

		/*
		 * Second case. If the token is an integer or double constant,
		 * then we parse that. I don't think we'll ever get the case of
		 * a unary minus here given how constants are parsed, but just
		 * in case (for example, whitespace between the "-" and the digits)
		 * we check for unary negation in both cases. Note that these two
		 * cases are really the same except for type and value storage.
		 */

		if (tokens.getType() == Tokenizer.INTEGER) {
			int value = Integer.parseInt(tokens.getSpelling());
			if (negate)
				value = -value;
			byteCodes.add(ByteCode._INTEGER, value);
			isVariable = false;
			variableName = null;
			isExpression = true;
			return status = new Status(Status.SUCCESS);
		}

		if (tokens.getType() == Tokenizer.DOUBLE) {
			double value = Double.parseDouble(tokens.getSpelling());
			if (negate)
				value = -value;
			isVariable = false;
			variableName = null;
			isExpression = true;
			byteCodes.add(ByteCode._DOUBLE, value);
			return status = new Status(Status.SUCCESS);
		}


		if (tokens.getType() == Tokenizer.DECIMAL) {
			byteCodes.add(ByteCode._STRING, tokens.getSpelling());
			byteCodes.add(ByteCode._CVT, Value.DECIMAL);
			if( negate )
				byteCodes.add(ByteCode._NEGATE);
			isVariable = false;
			variableName = null;
			isExpression = true;
			return status = new Status(Status.SUCCESS);
		}

		/*
		 * Third case. If it's an identifier, then let's look up the
		 * value in the symbol table. Negate the value as needed. If
		 * no symbol is found, then zero is assumed.
		 */

		if (tokens.getType() == Tokenizer.IDENTIFIER) 
			return status = compileIdentifier(tokens, negate);
		
		if (tokens.getType() == Tokenizer.STRING) {
			isExpression = true;
			return status = compileString(tokens, negate);
		}
		/*
		 * If we got here, the token was not usable for some reason (typically
		 * it was a special character that wasn't meaningful). Put it back on
		 * the string for subsequent processing, and report that we had a
		 * parsing error.
		 */

		tokens.restoreToken();
		return status = new Status(Status.EXPRESSION);

	}

	/**
	 * Compile a record value.  Assumes the leading "{" character has already
	 * been removed from the token stream.  Parses and compiles to the current
	 * bytecode object.
	 * @param tokens the current token stream.
	 * @return a Status indicating if a successful compile was done based on
	 * valid syntax.
	 */

	Status compileRecordValue(Tokenizer tokens) {

		int count = 0;
		
		boolean isRecord = false;
		isSingleton = false;
		isVariable = false;
		variableName = null;
		isExpression = true;
		
		/*
		 * If the arrayValue declaration is empty, return an empty
		 * arrayValue...
		 */
		if (tokens.assumeNextSpecial("}")) {
			byteCodes.add(ByteCode._RECORD, count);
			return status;
		}

		
		/*
		 * New code that tries to do constant pooling when possible
		 */

		if (isArrayConstantPooling()) {
			boolean argsConstant = false;
			if (byteCodes.statement != null)
				if (byteCodes.statement.program != null)
					argsConstant = true;

			Expression inArguments = new Expression(session);
			inArguments.setIgnoreIncrements(this.fIgnoreIncrements);
			inArguments.recursionStart = this.recursionDepth + 1;
			int listStart = byteCodes.size();

			while (true) {

				String key;
				int keyType = Value.UNDEFINED;

				if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {
					key = tokens.nextToken();
					keyType = Value.nameToType(key);
					if (keyType != Value.UNDEFINED && keyType != Value.RECORD
							&& tokens.testNextToken(Tokenizer.IDENTIFIER))
						key = tokens.nextToken();
					else
						keyType = Value.UNDEFINED;
					if (tokens.assumeNextSpecial(":")) {
						isRecord = true;
						byteCodes.add(ByteCode._STRING, key);
					} else {
						if (isRecord) {
							return status = new Status(Status.INVRECORD);
						}
						tokens.restoreToken(key, Tokenizer.IDENTIFIER);
					}
				}
				
				inArguments.compile(byteCodes, tokens);

				if (inArguments.status.failed())
					return inArguments.status;
				
				if (!inArguments.isConstant || recursionDepth > 1)
					argsConstant = false;

				if (keyType != Value.UNDEFINED) {
					byteCodes.add(ByteCode._CVT, keyType);
					argsConstant = false;
				}
				
				count++;
				if (!tokens.assumeNextSpecial(","))
					break;
			}
			if (!tokens.assumeNextSpecial("}"))
				return status=new Status(Status.ARRAY);

			byteCodes.add(isRecord ? ByteCode._RECORD : ByteCode._ARRAY, count);

			if (argsConstant) {
				count = byteCodes.size() - listStart;
				String name = RECORD_CONSTANT_PREFIX
						+ Integer.toString(JBasic.getUniqueID());
				byteCodes.insert(listStart, new Instruction(ByteCode._CONSTANT,
						count, name));
				byteCodes.add(ByteCode._LOADREF, name);
			}
			return status;
		}

		/*
		 * Otherwise, parse one or more comma-separated expressions
		 */
		final Expression exp = new Expression(session);
		exp.setIgnoreIncrements(this.fIgnoreIncrements);
		exp.setArrayConstantPooling(this.fArrayConstantPooling);
		while (true) {

			String key;
			int keyType = Value.UNDEFINED;

			if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {
				key = tokens.nextToken();
				keyType = Value.nameToType(key);
				if (keyType != Value.UNDEFINED && keyType != Value.RECORD
						&& tokens.testNextToken(Tokenizer.IDENTIFIER))
					key = tokens.nextToken();
				else
					keyType = Value.UNDEFINED;
				if (tokens.assumeNextSpecial(":")) {
					isRecord = true;
					byteCodes.add(ByteCode._STRING, key);
				} else {
					if (isRecord) {
						return status = new Status(Status.INVRECORD);
					}
					tokens.restoreToken(key, Tokenizer.IDENTIFIER);
				}
			}
			exp.compile(byteCodes, tokens);
			if (exp.status.failed())
				return exp.status;
			if (keyType != Value.UNDEFINED)
				byteCodes.add(ByteCode._CVT, keyType);
			count = count + 1;

			/*
			 * If a brace is next, we're done
			 */
			if (tokens.assumeNextSpecial("}"))
				break;

			/*
			 * If a comma isn't next, then it's bad syntax
			 */
			if (!tokens.assumeNextSpecial(",")) {

				this.status = new Status(Status.ARRAY);
				return status;
			}
		}

		if (!isRecord)
			return status = new Status(Status.INVRECORD);
		byteCodes.add(isRecord ? ByteCode._RECORD : ByteCode._ARRAY, count);

		return status;
	}

	/**
	 * Compile an array value.  Assumes the leading "[" character has already
	 * been removed from the token stream.  Parses and compiles to the current
	 * bytecode object.
	 * @param tokens the current token stream.
	 * @return a Status indicating if a successful compile was done based on
	 * valid syntax.
	 */
	Status compileArrayValue(Tokenizer tokens) {

		int count = 0;
		isSingleton = false;
		isVariable = false;
		variableName = null;
		isExpression = true;
		
		
		/*
		 * If the arrayValue declaration is empty, return an empty
		 * arrayValue...
		 */
		if (tokens.assumeNextSpecial("]")) {
			byteCodes.add(ByteCode._ARRAY, count);
			return status;
		}
		
		/*
		 * New code that tries to do constant pooling when possible
		 */

		if (isArrayConstantPooling()) {
			boolean argsConstant = false;
			if (byteCodes.statement != null)
				if (byteCodes.statement.program != null)
					argsConstant = true;

			Expression inArguments = new Expression(session);
			inArguments.setIgnoreIncrements(this.fIgnoreIncrements);
			inArguments.recursionStart = this.recursionDepth + 1;
			int listStart = byteCodes.size();

			while (true) {
				inArguments.compile(this.byteCodes, tokens);
				if (inArguments.status.failed())
					return inArguments.status;
				if (!inArguments.isConstant)
					argsConstant = false;
				if( recursionDepth > 1 )
					argsConstant = false;
				
				count++;
				if (!tokens.assumeNextSpecial(","))
					break;
			}
			if (!tokens.assumeNextSpecial("]"))
				return status=new Status(Status.ARRAY);

			byteCodes.add(ByteCode._ARRAY, count);

			if (argsConstant) {
				count = byteCodes.size() - listStart;
				String name = ARRAY_CONSTANT_PREFIX
						+ Integer.toString(JBasic.getUniqueID());
				byteCodes.insert(listStart, new Instruction(ByteCode._CONSTANT,
						count, name));
				byteCodes.add(ByteCode._LOADREF, name);
			}
			return status;
		}
		
		/*
		 * Otherwise, parse one or more comma-separated expressions
		 */
		final Expression exp = new Expression(session);
		exp.setIgnoreIncrements(this.fIgnoreIncrements);
		exp.setArrayConstantPooling(this.fArrayConstantPooling);
		while (true) {

			exp.compile(byteCodes, tokens);
			if (exp.status.failed())
				return exp.status;

			count = count + 1;

			/*
			 * If a brace is next, we're done
			 */
			if (tokens.assumeNextSpecial("]"))
				break;

			/*
			 * If a comma isn't next, then it's bad syntax
			 */
			if (!tokens.assumeNextSpecial(",")) {

				this.status = new Status(Status.ARRAY);
				return status;
			}
		}

		byteCodes.add(ByteCode._ARRAY, count);

		return status;
	}
	
	/**
	 * Parse an identifier and the various dereferencing items that can
	 * follow it, such as function calls, array dereferences, or object
	 * value references.  And even handle the case of a plain old
	 * identifier that is a symbol.
	 * <p>
	 * This assumes that the identifier type is known but that the next
	 * token is that identifier.  
	 * @note This method is different from compileArray and compileRecord
	 * in that they assume that the leading token is already parsed.  This
	 * method is different in that the actual token is an important part of
	 * the compilation (the symbol name).
	 * 
	 * @param tokens the active token stream.
	 * @param negate a flag indicating if a leading negation operator was
	 * already processed that should result in generate a NEG/NOT code.
	 * @return status indicating if the compilation was successful.
	 */
	public Status compileIdentifier(Tokenizer tokens, boolean negate) {

		String name = tokens.getSpelling();

		/*
		 * See if it's a SQL SELECT statement
		 */
		
		if( name.equals("SELECT")) {
			MockSQLStatement msql = new MockSQLStatement(this.session);
			tokens.restoreToken();
			status = msql.prepare(tokens);
			
			if( status.equals(Status.NOCOMPILE)) {
				return status;
			}
			if( status.failed())
				return status;

			byteCodes.concat(msql.generatedCode);
			
			msql = null;
			isVariable = false;
			variableName = null;
			isExpression =true;
			
			return status = new Status(Status.SUCCESS);

		}
		
		/*
		 * See if it's a reserved constant name for a boolean value.
		 */

		if (name.equals("TRUE") || name.equals("FALSE")) {
			byteCodes.add(ByteCode._BOOL, name.charAt(0) == 'T' ? 1 : 0);
			isVariable = false;
			variableName = null;
			isExpression = true;
			return status = new Status(Status.SUCCESS);
		}


		/*
		 * Handle dereference operators. These are ".", "[]", and "->"
		 * operators, as well as the "("..")" function operators.  Note
		 * that this means a function can return a complex object that
		 * is then further dereferences.  An example would be
		 * 
		 *    filetypes(".")[1].name 
		 *    
		 * this returns the name string of the first file of the list of
		 * file description records.
		 */
		boolean hasDeref = false;
		while (true) {
			
			/*
			 * Still in a dereference expression?  If the next token isn't
			 * a special character, then we know we're done.  If we've
			 * been processing dereferences already, then we are done
			 * with the expression entirely.
			 */
			final String derefTest = tokens.nextToken();

			if (tokens.getType() != Tokenizer.SPECIAL || 
					(!derefTest.equals(".") && 
					 !derefTest.equals("[") && 
					 !derefTest.equals("->") && 
					 !derefTest.equals("("))) {
				tokens.restoreToken();
				if (hasDeref)
					return status = new Status(Status.SUCCESS);
				break;
			}

			/*
			 * Because this is now a dereference syntax, we know
			 * some things about the expression (not a singleton,
			 * not a single variable, etc.)
			 */
			isSingleton = false;
			isVariable = false;
			variableName = null;
			isConstant = false;

			/*
			 * If we don't have a pending result on the stack, we do a load
			 * of the first identifier found to start the dereference chain.
			 * We know this is the first because we haven't processed a
			 * dereference operator (even though we know we have one from 
			 * the test above.)
			 */
			if (!hasDeref & !derefTest.equals("("))
				byteCodes.add(ByteCode._LOADREF, name);

			/*
			 * See if it's a function invocation. If so, do that.
			 */

			if (derefTest.equals("(")) {

				final Expression exp = new Expression(session, byteCodes);
				exp.setIgnoreIncrements(this.fIgnoreIncrements);
				int count = 0;
				isExpression = true;
				boolean constantArgs = true;
				int argGeneratedPosition = byteCodes.size();
				
				/*
				 * Parse the argument list and push on the stack. If it's an
				 * empty argument list, do nothing.
				 */

				if (!tokens.assumeNextSpecial(")"))
					while (true) {

						status = exp.compileBoolean(tokens);
						if (status.failed())
							return status;
						count++;
						if( !exp.isConstant)
							constantArgs = false;
						
						if (tokens.assumeNextSpecial(","))
							continue;
						if (!tokens.assumeNextSpecial(")"))
							return status = new Status(Status.PAREN);
						break;
					}

				/*
				 * Depending on dialect, etc there are a number of ways that a
				 * function might be named. Re-map the name if needed.
				 */
				if( session == null ) {
					return status = new Status(Status.NOFUNCS, "Functions");
				}
				name = Functions.remapName(session, name);

				/*
				 * See if it's possible to invoke a compiler unit for an
				 * intrinsic. If that succeeds, we're done because all
				 * additional in-line code has been generated. Otherwise, we'll
				 * do it as a runtime function lookup that might call into user
				 * code, etc.
				 */

				final Status compileStatus = CompileFunction
						.invokeFunctionCompiler(session, name, count,
								byteCodes, constantArgs, argGeneratedPosition);
				
				if( !compileStatus.equals(Status.UNKFUNC))
					return status = compileStatus;
				
				if (compileStatus.failed()) {
					/*
					 * Push the call, with the argument count
					 */
					if (hasDeref)
						byteCodes.add(ByteCode._CALLF, count);
					else
						byteCodes.add(ByteCode._CALLF, count, name);
				}
				
				if (name.equalsIgnoreCase("NEW"))
					this.fNEWFunctionCall = true;
				hasDeref = true;
				
				/* Parse more dereferences if possible */
				continue; 

			}

			if (derefTest.equals("->")) {
				if( session == null ) 
					return status = new Status(Status.NOFUNCS, "Objects");
				
				/*
				 * Check so see if this is really a method call.
				 */
				
				int marker = tokens.getPosition();
				
				if( tokens.testNextToken(Tokenizer.IDENTIFIER)) {
					if( tokens.peek(1).equals("(")) {
						/* 
						 * This is really a method call with a RETURNS clause 
						 * so start by parsing the arguments.
						 */
						int argcount = 0;
						byteCodes.add(ByteCode._DUP);
						String method = tokens.nextToken();
						byteCodes.add(ByteCode._STRING, method);
						byteCodes.add(ByteCode._SWAP);
						byteCodes.add(ByteCode._METHOD);
						tokens.nextToken(); /* Consume the opening parenthesis */
						Expression argExp = new Expression(this.session);
						argExp.setIgnoreIncrements(this.fIgnoreIncrements);
						
						if (!tokens.assumeNextToken(")"))
							while (!tokens.testNextToken(")")) {

								status = argExp.compile(byteCodes, tokens);
								if (status.failed())
									return status;
								argcount++;
								if (tokens.assumeNextToken(","))
									continue;
								if (tokens.assumeNextToken(")"))
									break;
								return new Status(Status.PAREN);
							}
						
						/*
						 * Object reference is on the stack.
						 */
						byteCodes.add(ByteCode._CALLM, argcount);
						byteCodes.add(ByteCode._RESULT);
						hasDeref = true;
						isVariable = false;
						variableName = null;
						isConstant = false;
						isSingleton = false;
						continue;
	
					}
				}
				/* No, it's a member invocation */
				tokens.setPosition(marker);				
				byteCodes.add(ByteCode._STRING, tokens.nextToken());
				byteCodes.add(ByteCode._OBJECT);
			
				hasDeref = true;
				isVariable = false;
				variableName = null;
				isConstant = false;
				isSingleton = false;
				continue;

			}

			/* 
			 * See if it's a recordValue reference. More to do...
			 */

			if (derefTest.equals(".")) {
				if( session == null ) 
					return status = new Status(Status.NOFUNCS, "Records");
				final String member = tokens.nextToken();
				if (!tokens.isIdentifier())
					return status = new Status(Status.NOMEMBER, member);

				/*
				 * If this is a SQL SELECT reference, it means a catalog
				 * reference.  This is a record with special attributes,
				 * so add the instruction that checks for those attributes
				 */
				if( isSQL ) {
					Instruction i = new Instruction(ByteCode._CATALOG, 0, name);
					byteCodes.insert(byteCodes.size()-2, i);
				}
				byteCodes.add(ByteCode._STRING, member);
				byteCodes.add(ByteCode._LOADR);
				hasDeref = true;
				isVariable = false;
				variableName = null;
				isConstant = false;
				isSingleton = false;
				continue;
			}
			
			/* 
			 * See if it's an arrayValue constant. More to do in that case.
			 */

			if (derefTest.equals("[")) {
				isVariable = false;
				variableName = null;
				isSingleton = false;
				isConstant = false;

				final Expression idxexp = new Expression(session);
				idxexp.setIgnoreIncrements(this.fIgnoreIncrements);
				idxexp.setDeferPosts(true);
				idxexp.compile(byteCodes, tokens);
				if (idxexp.status.failed())
					return status = idxexp.status;

				/*
				 * See if it's an array range specification such as
				 * X[I..J] which returns an array of X elements from 
				 * I to J.
				 */
				
				if( tokens.assumeNextSpecial("..")) {
					idxexp.compile(byteCodes, tokens);
					if (idxexp.status.failed())
						return status = idxexp.status;
				
					byteCodes.add(ByteCode._SUBSTR, 3);
				}
				
				/*
				 * Not a range, so generate the array Value index lookup
				 */
				else 
					byteCodes.add(ByteCode._INDEX);
				postIncrements(idxexp.postList, byteCodes);
				
				hasDeref = true;

				/*
				 * See if there are additional indexes, which are handled
				 * the same way.  Note that you cannot mix ranges and
				 * multidimensional array specifications!
				 */
				while (tokens.assumeNextSpecial(",")) {
					idxexp.compile(byteCodes, tokens);
					if (idxexp.status.failed())
						return status = idxexp.status;
					byteCodes.add(ByteCode._INDEX);
				}

				if (!tokens.assumeNextSpecial("]")) {
					status = new Status(Status.EXPSYNTAX, new Status(Status.BRACKETS));
					return status;
				}
				continue;
			}
		}

		/*
		 * All we have now is a name, so generate a load.
		 */

		if( session == null ) 
				return status = new Status(Status.EXPRESSION, new Status(Status.NOFUNCS, "Variable"));

		/*
		 * See if there is a post increment or decrement
		 */
		

		if( tokens.assumeNextSpecial("++")) {
			if( !fIgnoreIncrements) {
				addPostIncrement( name, 1 );
				byteCodes.add(ByteCode._LOAD, name);

			}
			else
				byteCodes.add(ByteCode._LOADREF, name);
		}
		else
			if( tokens.assumeNextSpecial("--")) {
				if( !fIgnoreIncrements) {
					addPostIncrement(name,-1);
					byteCodes.add(ByteCode._LOAD, name);
				}
				else
					byteCodes.add(ByteCode._LOADREF, name);
			}
			else
				byteCodes.add(ByteCode._LOADREF, name);

		isVariable = true;
		variableName = name;
		isConstant = false;

		if (negate) {
			isVariable = false;
			variableName = null;
			isSingleton = false;
			byteCodes.add(ByteCode._NEGATE);
		}
		return status = new Status(Status.SUCCESS);

	}

	/**
	 * Compile a string constant.  This mostly involves just storing
	 * the data, but if it has been "negated" then we reverse the
	 * characters in the string constant.  That is, -"Sue" is "euS".
	 * @param tokens the active token buffer
	 * @param negate flag indicating if a preceding negation was detected.
	 * @return Status indicating if the compilation was successful.
	 */
	Status compileString( Tokenizer tokens, boolean negate ) {

		Value tempString = new Value(tokens.getSpelling());
		String stringConstant = tempString.denormalize();
		isVariable = false;
		variableName = null;
		isExpression = true;
		if (negate) {
			
			/* 
			 * Negating the string means reverse the characters
			 */
			String revString = "";
			for (int ix = 0; ix < stringConstant.length(); ix++)
				revString = stringConstant.substring(ix, ix + 1)
						+ revString;
			stringConstant = revString;
		}
		
		byteCodes.add(ByteCode._STRING, stringConstant);
		return status = new Status(Status.SUCCESS);
	}
	/**
	 * Evaluate an expression immediately. The expression is first
	 * compiled into a temporary bytecode array and then executed, 
	 * and the result passed back to the caller as the result.
	 * 
	 * @param tokens
	 *            Token stream to use to compile the expression.
	 * @param symbols
	 *            Symbol table for runtime binding of compiled code
	 * @return A Value object with the result of the expression.
	 */
	private Value compileAndRun(final Tokenizer tokens, final SymbolTable symbols) {

		final ByteCode bc = new ByteCode(session, null);
		final Expression cx = new Expression(session);
		
		/*
		 * Initialize the compiler state settings to support immediate-mode
		 * compilation.
		 */
		isSingleton = true;
		isVariable = false;
		variableName = null;
		isExpression = false;
		recursionDepth = 0;
		cx.compile(bc, tokens);
		if (cx.status.failed()) {
			status = cx.status;
			return null;
		}
		/*
		 * Seal the bytecode stream and run it.
		 */
		bc.add(ByteCode._END, 0);
		bc.run(symbols, 0);
		
		/*
		 * Restore the current expression states to match the states
		 * stored in the nested expression we just compiled and executed,
		 * in case anyone wants to query these after the execution.
		 */
		status = bc.status;
		isSingleton = cx.isSingleton;
		isVariable = cx.isVariable;
		variableName = cx.variableName;

		/*
		 * Return the results of the expression to the caller.
		 */
		return bc.getResult();
	}

	/**
	 * Set the array constant pooling flag for this expression evaluator.
	 * @param fArrayConstantPooling true if pooling is permitted.  This is
	 * disabled during compilations where pooling is ineffective or invalid,
	 * such as in a DATA statement constant.
	 */
	public void setArrayConstantPooling(boolean fArrayConstantPooling) {
		this.fArrayConstantPooling = fArrayConstantPooling;
	}

	/**
	 * Set the flag causing the code generator to ignore pre- and post-
	 * increment operations.
	 * @param flag true if we are to ignore increment operations
	 */
	public void setIgnoreIncrements(boolean flag) {
		fIgnoreIncrements = flag;
	}
	
	/**
	 * Set defer post flag. When true, a list of
	 * scalar variables to be modified is kept and can be processed after
	 * the expression is complete. For example,<p>
	 * <code>X = A[B++]+B</code><p>
	 * In this case, the increment of B is deferred until after the
	 * index expression calculations are complete, but before the value
	 * is used again as an addend.
	 * @param flag TRUE if the expression being compiled is asked to defer
	 * post-increment and post-decrement operations.  
	 */
	
	public void setDeferPosts(boolean flag ) {
		fDeferPosts = flag;
	}
	
	/**
	 * Set the flag that indicates we are going to compile a SQL refernece.
	 * @param flag true if the item being parsed could be considered a 
	 * SQL catalog.
	 */
	public void setSQL( boolean flag ) {
		isSQL = flag;
	}
	/**
	 * Determine if constant pooling is permitted in this instance of an
	 * expression evaluator.
	 * @return true if pooling is currently permitted.
	 */
	public boolean isArrayConstantPooling() {
		return fArrayConstantPooling;
	}

	/**
	 * When a post increment or post decrement operation is compiled,
	 * the actual operation is stored in a list in the expression, so
	 * it can be processed later.  For example, A[B++] must complete
	 * the dereference operation on B before the increment can take
	 * place.  This call stores the increment operation away for later
	 * code generation
	 * @param name The variable to increment or decrement.
	 * @param value This value <em>must</em> be either 1 or -1 and
	 * indicates if the operation is an increment (1) or decrement(-1)
	 * operation.
	 */
	@SuppressWarnings("unchecked") 
	private void addPostIncrement( String name, int value ) {
		if( postList == null )
			postList = new Vector();
		
		if( value < 0 )
			value = -1;
		if( value >= 0 )
			value = 1;
		
		postList.add((value== 1? "+" : "-") + name);
	}
	
	/**
	 * Given a list of post-increment or post-decrement operations to be
	 * performed in an expression, generate the bytecode needed.  This is
	 * typically done when an expression needs to defer the use of the
	 * increment, such as when an lvalue is used as both the source and
	 * destination of an operation.
	 * @param postList a Vector of strings describing the operations.  Each
	 * string has a "+" or "-" as the first character indicating if it is
	 * an increment or decrement operation, and the rest of the string is
	 * the name of the scalar value to be modified.
	 * @param bc the Byte Code stream in which the code is generated.
	 */
	public static void postIncrements(Vector<String> postList, ByteCode bc) {
		if( postList == null )
			return;
		
		for( int idx= 0; idx < postList.size(); idx++ ) {
			String name = postList.get(idx);
			//bc.add(ByteCode._LOAD, name.substring(1));
			bc.add(ByteCode._INCR, 
					name.charAt(0) == '+' ? 1 : -1,
					name.substring(1));
		}
		postList = null;
		return;
	}
	
	/**
	 * Return the post increment and post decrement list accumulated for this
	 * expression.  This is normally handled in the expression automatically,
	 * but can be deferred if needed, such as when an lvalue is used as both
	 * a source and target in an operation.
	 * @return a Vector<String> containing the list of operations to be
	 * performed.
	 */
	public Vector incrementList() {
		if( postList == null )
			return new Vector<String>();
		return postList;
	}

}
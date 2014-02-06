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
 * Created on Aug 28, 2007 by cole
 *
 */
package org.fernwood.jbasic.compiler;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.statements.Statement;
import org.fernwood.jbasic.value.Value;

/**
 * This module in the compiler section of JBasic is used to compile any
 * type statement, such as the INTEGER or STRING statements.  These are
 * all essentially the same operation with the exception of the underlying
 * data type that is generated as the default value.
 * 
 * @author cole
 * @version version 1.0 Aug 28, 2007
 *
 */
public class TypeCompiler {

	/**
	 * Compile a type statement (INTEGER, STRING, etc.)
	 * @param session The JBasic session that hosts this compilation
	 * @param stmt The statement being generated.
	 * @param tokens The token stream
	 * @param type Value.INTEGER, Value.STRING, etc.
	 * @return Status of the compilation operation
	 */
	public static Status compile( JBasic session, Statement stmt, Tokenizer tokens, int type ) {
		
		/*
		 * There must be stuff to parse, or there is an error
		 */
		
		if( tokens.endOfStatement())
			return new Status(Status.EXPVARS);
		
		/*
		 * Set up compilation.  This requires a new bytecode buffer for the
		 * statement, a new LValue processor used to identify each variable
		 * to be created, and an expression compiler used to process array
		 * initial values for declared variables.
		 */

		stmt.byteCode = new ByteCode(session, stmt);

		LValue variable = new LValue(session, false);
		Expression initialValue = new Expression(session);
		int scale = 0;
		
		/*
		 * If the type is decimal, the scale may be specified in
		 * parenthesis after the type name (which was just parsed).
		 */
		
		if( type == Value.DECIMAL && tokens.assumeNextSpecial("(")) {
			Value size = initialValue.evaluate(tokens, null);
			if( !tokens.assumeNextToken(")"))
				return new Status(Status.PAREN);
			scale = size.getInteger();
		}
		/*
		 * Loop over value specifications
		 */
		while(true) {
			
			/*
			 * If we've come to the end of the string or a statement
			 * separator, then we're done with this statement.
			 */
			if( tokens.endOfStatement())
				break;
			
			/*
			 * Compile the name of the variable.  This includes any
			 * array references, which we use as size definitions.
			 */
			stmt.status = variable.compileLValue(stmt.byteCode, tokens);
			if( variable.error)
				return stmt.status;
			stmt.byteCode.add(ByteCode._DCLVAR, type, variable.name);
			if( scale != 0 ) 
				stmt.byteCode.add(ByteCode._SCALE, scale, variable.name);
		
			/*
			 * If there is an initial value, it will be introduced by an
			 * equals sign.  If present, compile the initial value definition
			 * such that the value is placed on the runtime stack.
			 */
			if( tokens.assumeNextSpecial("=")) {
	
				stmt.status = initialValue.compile(stmt.byteCode, tokens);
				if( stmt.status.failed())
					return stmt.status;
				stmt.byteCode.add(ByteCode._CVT, type);
			}
	
			/*
			 * If no initial value, then based on the type, generate a 
			 * suitable default initial value.
			 */
			else switch(type) {
			case Value.DECIMAL:
				stmt.byteCode.add(ByteCode._INTEGER, 0 );
				stmt.byteCode.add(ByteCode._CVT, Value.DECIMAL);
				break;
				
			case Value.INTEGER:
				stmt.byteCode.add(ByteCode._INTEGER, 0);
				break;
				
			case Value.BOOLEAN:
				stmt.byteCode.add(ByteCode._BOOL, 0);
				break;
				
			case Value.DOUBLE:
				stmt.byteCode.add(ByteCode._DOUBLE, 0.0);
				break;
			
			case Value.STRING:
				stmt.byteCode.add(ByteCode._STRING, "");
				break;
			
			case Value.ARRAY:
				stmt.byteCode.add(ByteCode._ARRAY, 0 );
				break;
				
			case Value.RECORD:
				stmt.byteCode.add(ByteCode._RECORD, 0 );
				break;
				
			default:
				return new Status(Status.FAULT, 
						new Status(Status.BADTYPE, type));
			}
			
			/*
			 * We have either put a constant on the stack or a default
			 * initial value.  Make sure it will be the right type and then
			 * generate the store operation.
			 */
			variable.setType(type);
			stmt.status = variable.compileStore(true);
			if( stmt.status.failed())
				return stmt.status;
			
			/*
			 * If there's a comma, there's more in this list so keep
			 * going.  Otherwise we're done.
			 */
			if( !tokens.assumeNextSpecial(","))
				break;
			
		}
		return stmt.status = new Status(Status.SUCCESS);
	}
}

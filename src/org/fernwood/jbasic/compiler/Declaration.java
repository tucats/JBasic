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
 * Created on Mar 5, 2008 by tom
 *
 */
package org.fernwood.jbasic.compiler;

import java.util.HashMap;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.value.Value;

/**
 * Declaration - generic compiler for symbol declaration strings. This is used
 * in DIM and COMMON statements.  Unlike the TypeCompiler, this does not handle
 * things like initializers, etc.  However, it does allow type definitions to
 * be included in the syntax, unlike the TypeCompiler which is given the
 * type information (usually from the verb context).
 *  
 * @author tom
 * @version version 1.0 Mar 5, 2008
 * 
 */
public class Declaration {

	/**
	 * The symbols created by this compilation are intended to have the COMMON
	 * attribute, meaning they are copied to the symbol table of a program
	 * invoked via the CHAIN statement.
	 */
	public final static boolean COMMON = true;

	/**
	 * The symbols created by this compilation will not have the COMMON
	 * attribute and will not be copied across CHAIN boundaries.
	 */
	public final static boolean NOT_COMMON = false;

	/**
	 * Compile a generalized declaration of symbols. This handles type
	 * definitions and initial values. Optionally it will also set the COMMON
	 * attribute for the created variables.
	 * 
	 * @param byteCode
	 *            the bytecode stream to write instructions to
	 * @param tokens
	 *            the token stream to use to parse the commands
	 * @param isCommon
	 *            a boolean indicating if this is a COMMON statement
	 * @return status indicating if the parse was successful.
	 */
	public static Status compile(ByteCode byteCode, final Tokenizer tokens,
			boolean isCommon) {
		/*
		 * Each statement must allocate and initialize its bytecode area.
		 */
		boolean isArray = false;

		/*
		 * Count of number of dimensions of array declarations.
		 */
		
		int dimCount = 0;
		
		/*
		 * Keep a list of the items to detect duplicates, which while technically
		 * not illegal would be indicative of an error in any reasonable case.
		 */
		
		HashMap<String, String> fieldList = new HashMap<String, String>();
		
		/*
		 * Scan in a loop, picking up each declaration in the statement.
		 */
		while (true) {

			if (!tokens.testNextToken(Tokenizer.IDENTIFIER))
				return new Status(Status.LVALUE, tokens.nextToken());
			String name = tokens.nextToken();

			int type = Value.nameToType(name);

			if(  type != Value.UNDEFINED) {
				
				if( tokens.assumeNextSpecial("(")) {
					return new Status(Status.INVVARLEN, isCommon? "COMMON" : "DIM");
				}
				tokens.assumeNextToken("AS");
				if (!tokens.testNextToken(Tokenizer.IDENTIFIER))
					return new Status(Status.LVALUE, tokens.nextToken());
				name = tokens.nextToken();

			}
			/*
			 * See if this is a duplicate
			 */
			if( fieldList.get(name) != null ) 
				return new Status(Status.DUPFIELD, name);
			fieldList.put(name, name);
			/*
			 * If there is an array then compile the expression that defines the
			 * size of the array.
			 */
			dimCount = 0;
			String closingBracketChar = ")";
			if (tokens.assumeNextToken("[")) {
				closingBracketChar = "]";
				tokens.restoreToken("(", Tokenizer.SPECIAL);
			}
			if (tokens.assumeNextToken("(")) {
				final Expression exp = new Expression(byteCode.getEnvironment());
				while(true) {
					exp.compile(byteCode, tokens);
				if (exp.status.failed())
					return exp.status;
				dimCount++;
				if( !tokens.assumeNextSpecial(","))
					break;
				}
				if (!tokens.assumeNextToken(closingBracketChar))
					return new Status(Status.ARRAY);
				isArray = true;
			} else
				isArray = false;

			/*
			 * Require AS and a valid type, which we re-map to the Value types.
			 * This will be used later in the _DIM ByteCode to initialize
			 * values.
			 */

			boolean fStrongTyping = byteCode.statement.strongTyping();

			if (!fStrongTyping && tokens.endOfStatement() & type == Value.UNDEFINED) {
				tokens.restoreToken("INTEGER", Tokenizer.IDENTIFIER);
				tokens.restoreToken("AS", Tokenizer.IDENTIFIER);
			}

			if( type == Value.UNDEFINED){
			boolean fAS = tokens.testNextToken("AS");

			if (!fStrongTyping && !isArray && !fAS)
				return new Status(Status.EXPAS);

			if (fAS) {
				tokens.assumeNextToken("AS");

				final String dtype = tokens.nextToken();
				if (!tokens.isIdentifier())
					return new Status(Status.BADTYPE, dtype);
				type = Value.nameToType(dtype);

				if (type == Value.UNDEFINED && !isArray)
					return new Status(Status.BADTYPE, dtype);
			} else
				type = LValue.determineType(name);
			}
			
			/*
			 * If it's an array, just use _DIM which uses the type code to create
			 * a value. If it's not an array, use VALUE which generates a
			 * default VALUE of the given type.
			 */
			if (isArray) {
				byteCode.add(ByteCode._INTEGER, type );
				byteCode.add(ByteCode._DIM, dimCount);
			}
			else
				byteCode.add(ByteCode._VALUE, type);

			/*
			 * Store the value or array in the named variable. If there is a
			 * comma, then this is a list and we do more. Else break out of the
			 * loop.
			 */

			byteCode.add(ByteCode._DCLVAR, -(isArray ? Value.ARRAY : type), name);
			//byteCode.add(ByteCode._STOR, name);
			if (isCommon)
				byteCode.add(ByteCode._COMMON, name);

			if (tokens.assumeNextToken(","))
				continue;
			break;
		}
		return new Status();

	}
}
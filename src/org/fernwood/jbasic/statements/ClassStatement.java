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

import java.util.HashMap;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * <code>CLASS <em>name</em> [ : <em>superclass</em>] 
 * [ ( <em>field</em> AS <em>type</em> [,...]) ] </code><br><p>
 * 
 * Define a new CLASS variable.  If the class has a superclass, specify it
 * as an identifier after a colon.  If not given, "CLASS" is assumed.  A class
 * record object is created and given the class name.<p>
 * You can optionally specify fields using the DIM statement syntax in 
 * parenthesis, with fields and types, which are initialized to empty/null/zero
 * values.<p>
 * For example,<br><p>
 * <code> CLASS EMPLOYEE( ID AS INTEGER, NAME AS STRING)</code><br>
 * <code> CLASS MANAGER:EMPLOYEE( DEPT AS STRING )</code><br><p>
 * 
 * This creates two classes, EMPLOYEE and MANAGER.  A MANAGER is a special
 * case of an employee and has an additional field (DEPT).
 * <p>
 * You can then use the NEW() function to create instance objects of these
 * classes, such as:<br><p>
 * <code>    TOM = NEW(EMPLOYEE) </code><br>
 * <code>    SUE = NEW(MANAGER)  </code><br>
 * <p>
 * This creates an object TOM of class EMPLOYEE, and an object SUE of class
 * MANAGER.  These instance objects are records with fields based on the
 * class definitions, so both TOM and SUE have an ID field and a NAME field,
 * but only SUE has a DEPT field.
 *
 * 
 * 
 * @author cole
 *
 */
public class ClassStatement extends Statement {

	/**
	 * Compile 'class' statement.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 * @return a Status value indicating if the compile was successful.
	 */
	public Status compile(final Tokenizer tokens) {

		byteCode = new ByteCode(session, this);

		/*
		 * Get the class name.  It's an error if the name is omitted or
		 * is not an IDENTIFIER token.
		 */

		if (tokens.endOfStatement())
			return status = new Status(Status.INVOBJECT, "<none>");

		String className = tokens.nextToken();
		if (tokens.getType() != Tokenizer.IDENTIFIER)
			return status = new Status(Status.INVOBJECT, className);

		byteCode.add(ByteCode._DCLVAR, Value.RECORD, className);
		
		/*
		 * See if there is a superclass, introduced by ":" or the
		 * keyword "SUPERCLASS", get the superclass name.  Otherwise,
		 * we'll default to a superclass name of CLASS
		 */

		String superclass = "CLASS";
		if (tokens.assumeNextSpecial(":"))
			superclass = tokens.nextToken();
		else if (tokens.assumeNextToken("SUPERCLASS"))
			superclass = tokens.nextToken();

		/*
		 * Generate the call to the NEW function which knows how
		 * to construct a CLASS object.
		 */

		byteCode.add(ByteCode._LOADREF, superclass);
		byteCode.add(ByteCode._STRING, className);
		byteCode.add(ByteCode._CALLF, 2, "NEW");
		byteCode.add(ByteCode._STOR, className);

		/*
		 * We keep a list of the names we see, to detect duplicates
		 */
		
		HashMap<String,String> fieldList = new HashMap<String, String>();
		
		/*
		 * See if there are field initializers
		 */

		if (tokens.assumeNextSpecial("(")) {

			boolean isArray = false;
			int dimCount = 0;
			
			/*
			 * Scan in a loop, picking up each declaration in the statement.
			 */
			while (true) {

				if (tokens.assumeNextSpecial(")"))
					break;

				final String name = tokens.nextToken();
				if (!tokens.isIdentifier())
					return status = new Status(Status.LVALUE, name);

				/*
				 * If there is an array then compile the expression that defines the
				 * size of the array.
				 */

				String closingBracketChar = ")";
				if (tokens.assumeNextToken("[")) {
					closingBracketChar = "]";
					tokens.restoreToken("(", Tokenizer.SPECIAL);
				}
				if (tokens.assumeNextToken("(")) {
					final Expression exp = new Expression(session);
					while(true ) {
						exp.compile(byteCode, tokens);
						if (exp.status.failed())
							return status = exp.status;
						dimCount++;
						if( !tokens.assumeNextSpecial(","))
							break;
					}
					if (!tokens.assumeNextToken(closingBracketChar))
						return status = new Status(Status.ARRAY);
					isArray = true;
				} else
					isArray = false;

				/*
				 * Require AS and a valid type, which we re-map to the Value types.
				 * This will be used later in the _DIM ByteCode to initialize
				 * values.
				 */

				boolean fStrongTyping = strongTyping();

				if (!fStrongTyping
						& tokens.endOfStatement()) {
					tokens.restoreToken("INTEGER", Tokenizer.IDENTIFIER);
					tokens.restoreToken("AS", Tokenizer.IDENTIFIER);
				}

				boolean fAS = tokens.testNextToken("AS");

				if (!fStrongTyping & !fAS)
					return status = new Status(Status.INVDIM);

				int type = Value.UNDEFINED;
				if (fAS) {
					tokens.assumeNextToken("AS");

					final String dtype = tokens.nextToken();

					if (dtype.equalsIgnoreCase("INTEGER"))
						type = Value.INTEGER;
					else if (dtype.equalsIgnoreCase("STRING"))
						type = Value.STRING;
					else if (dtype.equalsIgnoreCase("DOUBLE"))
						type = Value.DOUBLE;
					else if (dtype.equalsIgnoreCase("BOOLEAN"))
						type = Value.BOOLEAN;
					else if (dtype.equalsIgnoreCase("RECORD"))
						type = Value.RECORD;

					if (type == Value.UNDEFINED)
						return status = new Status(Status.INVDIM);
				} else
					type = LValue.determineType(name);

				/*
				 * If it's an array, just use _DIM which uses a type code
				 * from the stack to create a value with the given number
				 * of dimensions.
				 * If it's not an array, use VALUE which generates a 
				 * default VALUE of the given type.
				 */
				if (isArray) {
					byteCode.add(ByteCode._INTEGER, type);
					byteCode.add(ByteCode._DIM, dimCount);
				}
				else if (type == Value.STRING)
					byteCode.add(ByteCode._STRING, "");
				else
					byteCode.add(ByteCode._VALUE, type);

				/*
				 * Make sure it isn't a duplicate name
				 */
				
				if( fieldList.get(name) != null )
					return new Status(Status.DUPFIELD, name);
				fieldList.put(name, name);
				
				/*
				 * Store the value or array in the named variable. If there is a
				 * comma, then this is a list and we do more. Else break out of the
				 * loop.
				 */
				byteCode.add(ByteCode._STRING, name);
				byteCode.add(ByteCode._STORR, className);
				if (tokens.assumeNextToken(","))
					continue;
				if (tokens.assumeNextSpecial(")"))
					break;
			}
		}

		return status = new Status();
	}

}

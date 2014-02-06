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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.FileParse;
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.opcodes.OpCLEAR;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.value.Value;

/**
 * Input one or more variables from an input buffer. The stdin input stream is
 * used to input the values. The syntax is:
 * <p>
 * <code>
 * INPUT [#file-identifier] ["prompt expression",] var1 [, varn...]
 * </code>
 * <p>
 * The prompt expression is used if give, else a default input prompt is used
 * from the variable SYS$INPUT_PROMPT. The values are considered lvalues and are
 * processed as such. Each value is read from the input stream and assigned to
 * an lvalue. <br>
 * <br>
 * If you wish, you can read the values from an input file, by giving the file
 * identifier (from the <code>OPEN</code> statement) and the input stream used
 * will be the file stream rather than the console. If an input file is used,
 * the prompt is ignored.
 * 
 * @author cole
 * @version version 1.2 Add support for ..AS XML and ..AS RAWXML to allow conversion
 * from formatted XML or a unparsed XML object to be read.  Added ability to specify
 * the root tag used to locate the value as a clause in the AS XML("name").
 * 
 */

class InputStatement extends Statement {

	/**
	 * Compile 'INPUT' statement. Processes a token stream, and compiles it into
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

		int hasFile = 0;
		ByteCode fileVar = null;
		byteCode = new ByteCode(session, this);
		boolean defaultPrompt = false;
		
		/*
		 * See if there is a FILE <ident> clause.
		 */

		final FileParse f = new FileParse(tokens, true);
		if (f.success()) {

			fileVar = new ByteCode(session);
			f.generate(fileVar);

			if (!tokens.assumeNextToken(new String [] { ";", ","}))
				return new Status(Status.FILECOMMA);

			hasFile = 1;

		} else {
			if (tokens.testNextToken(Tokenizer.STRING)) {
				byteCode.add(ByteCode._STRING, tokens.nextToken());
				if (!tokens.assumeNextToken(new String [] { ";", ","}))
					return status = new Status(Status.INPUTERR,
							new Status(Status.SYNEXPTOK, "\",\""));
			} else {
				byteCode.add(ByteCode._LOAD, "SYS$INPUT_PROMPT");
				defaultPrompt = true;
			}

			byteCode.add(ByteCode._OUT, 0);
		}
		
		/*
		 * See if this is a BY NAME which trumps everything else.
		 */
		
		int tokenPos = tokens.getPosition();
		
		/* First test is for FOO BY NAME, which is the simplest 
		 * and possibly most common case, reading all input and
		 * storing it in a single record FOO.
		 */
		
		if( tokens.testNextToken(Tokenizer.IDENTIFIER))  {
			String name = tokens.nextToken();
			if( tokens.assumeNextToken("BY"))
				if( tokens.assumeNextToken("NAME")) {
					
					if( hasFile == 1 )
						byteCode.concat(fileVar);
					byteCode.add(ByteCode._LINE, hasFile);
					byteCode.add(ByteCode._ARRAY, 0 );
					byteCode.add(ByteCode._CALLF, 2, "BYNAME");
					byteCode.add(ByteCode._STRING, "_UNEXPECTED");
					byteCode.add(ByteCode._LOADR);
					byteCode.add(ByteCode._STOR, name);
					return new Status();
				}
		}
		/* Not that form, try again - could by BY NAME() form */
		tokens.setPosition(tokenPos);
		boolean fByName = false;
		if( tokens.assumeNextToken("BY"))
			if( tokens.assumeNextToken("NAME"))
				fByName = true;
		
		if( fByName ) {
			
			if( hasFile == 1 )
				byteCode.concat(fileVar);
			
			byteCode.add(ByteCode._LINE, hasFile);

			/*
			 * Process the list of variable names.  If there
			 * is no list, then the vector of names is just
			 * empty.
			 */
			
			ArrayList<String> names = new ArrayList<String>();
			ArrayList<String> alias = new ArrayList<String>();
	
			if( tokens.assumeNextSpecial("(")) {

				while( true ) {
					String name = tokens.nextToken();
					if( tokens.getType() != Tokenizer.IDENTIFIER)
						return new Status(Status.INPUTERR, 
								new Status(Status.INVNAME, name));
					for( int ix = 0; ix < names.size(); ix++)
						if( name.equals(names.get(ix)))
							return new Status(Status.INPUTERR, 
									new Status(Status.DUPFIELD, name));


					names.add(name);

					if( tokens.assumeNextToken("AS")) {
						name = tokens.nextToken();
						if( tokens.getType() != Tokenizer.IDENTIFIER)
							return new Status(Status.INPUTERR, 
									new Status(Status.INVNAME, name));
					}
					alias.add(name);
					byteCode.add(ByteCode._STRING, name);
					if( tokens.assumeNextSpecial(")"))
						break;
					if(!tokens.assumeNextSpecial(","))
						return new Status(Status.INPUTERR, 
								new Status(Status.SYNEXPTOK, "\",\""));
				}
			}
			byteCode.add(ByteCode._ARRAY, names.size());
			byteCode.add(ByteCode._CALLF, 2, "BYNAME");
			String recordName = "__BYNAME" + JBasic.getUniqueID();
			byteCode.add(ByteCode._STOR, recordName);
			for( int ix = 0; ix < names.size(); ix++ ) {
				byteCode.add(ByteCode._STRING, alias.get(ix));
				byteCode.add(ByteCode._LOADR, recordName);
				byteCode.add(ByteCode._STOR, names.get(ix));
			}
			
			tokenPos = tokens.getPosition();
			boolean fUnexpected = false;
			
			if( tokens.assumeNextToken("REMAINDER"))
				tokens.restoreToken("UNEXPECTED", Tokenizer.IDENTIFIER);
			if( tokens.assumeNextToken("REST"))
				tokens.restoreToken("UNEXPECTED", Tokenizer.IDENTIFIER);
			
			if( names.size() == 0 ) {
				int temp = tokens.getPosition();
				tokens.assumeNextToken("UNEXPECTED");
				if( tokens.assumeNextToken("AS"))
					fUnexpected = true;
				else
					tokens.setPosition(temp);
			}
			if( tokens.assumeNextToken("UNEXPECTED"))
				if( tokens.assumeNextToken("AS")) 
					fUnexpected = true;
			if( fUnexpected ) {
				String name = tokens.nextToken();
				if( tokens.getType() != Tokenizer.IDENTIFIER)
					return new Status(Status.INPUTERR, 
						new Status(Status.INVNAME, name));
				
				byteCode.add(ByteCode._STRING, "_UNEXPECTED");
				byteCode.add(ByteCode._LOADR, recordName);
				byteCode.add(ByteCode._STOR, name);
			}
			else {
				tokens.setPosition(tokenPos);
				if( names.size() == 0 ) 
					return new Status(Status.INPUTERR, 
							new Status(Status.EXPCLAUSE, "AS"));
			}
			
			byteCode.add(ByteCode._CLEAR, OpCLEAR.CLEAR_SYMBOL_ALWAYS, recordName);
			return status = new Status();
		}
		
		tokens.setPosition(tokenPos);
		
		/*
		 * See if it is INPUT ROW(table-name) format
		 */
		
		boolean fRow = false;
		boolean fRowOf = false;
		if( tokens.assumeNextToken("ROW")) {
			if( tokens.assumeNextToken("("))
				fRow = true;
			else
				if( tokens.assumeNextToken("OF")) {
					fRow = true; 
					fRowOf = true;
				}
		}
		
		if( fRow ) {
			
			/*
			 * Get the table name, which must be a valid identifier.
			 */
			if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
				return status = new Status(Status.EXPNAME);
			String tableName = tokens.nextToken();
			
			/*
			 * If the default prompt was used, then change it now. 
			 */
			
			if( defaultPrompt) {
				Instruction i = new Instruction(ByteCode._ROWPROMPT, tableName);
				byteCode.setInstruction(i, byteCode.size()-2);

			}
			if( hasFile == 1 )
				byteCode.concat(fileVar);
			/*
			 * Generate code to read the row.
			 */
			byteCode.add(ByteCode._INPUTROW, hasFile, tableName);
			if( !fRowOf && !tokens.assumeNextToken(")"))
				return status = new Status(Status.PAREN);
			return status = new Status();
		}
		
		tokens.setPosition(tokenPos);
		
		/*
		 * Nothing fancy, just plain old INPUT with lvalues.
		 */
		int variableCount = 0;
		
		while (true) {

			if( tokens.endOfStatement()) {
				status = new Status(Status.EXPVARS);
				return status;
			}
			
			final LValue nextInputVariable = new LValue(session, strongTyping());
			status = nextInputVariable.compileLValue(byteCode, tokens);

			if (status.failed())
				return status;

			if (hasFile == 1)
				byteCode.concat(fileVar);

			/*
			 * Peek ahead to see if there is an AS XML clause, which
			 * changes the opcode we'd use.
			 */
			
			int cvtType = Value.UNDEFINED;
			int opCode = ByteCode._INPUT;
			boolean xmlConversion = false;
			ByteCode xmlTagCode = null;
			
			if( tokens.assumeNextToken("AS")) {
				String dataType = tokens.nextToken();
				if( !tokens.isIdentifier())
					return new Status(Status.BADTYPE, dataType);

				if( dataType.equals("RAW") & tokens.peek(0).equals("XML")) {
					opCode = ByteCode._INPUTXML;
					xmlConversion = false;
					dataType = "RAWXML";
					tokens.nextToken();
				} else if( dataType.equals("RAWXML")) {
					opCode = ByteCode._INPUTXML;
					xmlConversion = false;
				} else if( dataType.equals("XML")) {
					opCode = ByteCode._INPUTXML;
					xmlConversion = true;
					if( tokens.assumeNextSpecial("(")) {
						Expression typeExpression = new Expression(session);
						xmlTagCode = new ByteCode(session);
						typeExpression.compile(xmlTagCode, tokens);
						if(typeExpression.status.failed())
							return typeExpression.status;
						xmlTagCode.add(ByteCode._CVT, Value.STRING);
						if(!tokens.assumeNextSpecial(")"))
							return new Status(Status.PAREN);
					}
				} else {
					cvtType = Value.nameToType(dataType);
					if( cvtType == Value.UNDEFINED || cvtType == Value.ARRAY)
						return new Status(Status.BADTYPE, dataType);
				}
			}

			/*
			 * Now that we know if the opcode changes to INPUTXML and
			 * we know if there is a type declaration, emit the correct
			 * instruction(s). If a root tag type was given in the AS XML()
			 * clause, generate code to define that in the call to XMLPARSE()
			 */
			byteCode.add(opCode, hasFile);
			if( xmlConversion ) {
				int argCount = 1;
				if( xmlTagCode != null ) {
					argCount = 2;
					byteCode.concat(xmlTagCode);
				}
				byteCode.add(ByteCode._CALLF, argCount, "XMLPARSE");
			}
			if( cvtType != Value.UNDEFINED)
				byteCode.add(ByteCode._CVT, cvtType);
			

			/*
			 * Store the result and loop again if there are more terms.
			 */
			nextInputVariable.compileStore();
			variableCount++;
			if (!tokens.assumeNextToken(","))
				break;
		}

		if( variableCount == 0 ) {
			return new Status(Status.INPUTERR, 
					new Status(Status.EXPVARS));
		}
		return new Status(Status.SUCCESS);

	}
}
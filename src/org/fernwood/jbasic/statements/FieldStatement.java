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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.FileParse;
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * Create a FIELD (record) definition for use with BINARY file formats.
 * <p>
 * <code>
 * FIELD <em>file-identifier</em>, <em>type</em> <em>variable</em> 
 * [,<em>type variable</em>...]
 * </code>
 * <p>
 * The file identifier indicates which file this FIELD definition is
 * used for.  File or integer references are allowed; i.e. <code>FILE EMPDATA</code>
 * or <code>#3</code> are both permitted.
 * <p>
 * The <code>FIELD</code> statement is an executable statement; each time a <code>FIELD</code> statement is
 * given, a new record definition is bound to the given BINARY file.  The file must
 * already be open, or there is no file definition to which to bind the record
 * definition, so always execute the <code>OPEN</code> statement before any
 * <code>FIELD</code> statement(s).  Of course, you can just explicitly give a
 * record definition in each <code>PUT</code> or <code>GET</code> statement with the
 * <code>USING</code> clause instead of implicitly binding records with this
 * statement.
 * are executed.
 * <p>
 * The <em>type</em> must be one of <code>BOOLEAN</code>,
 * <code>STRING(<em>size</em>)</code>, <code>FLOAT</code>, <code>DOUBLE</code>, or
 * <code>INTEGER</code>. The natural number of bytes for a numeric item are
 * read from the file unless a specific size value is given.  For string variables,
 * a size value is required to specify the maximum number of characters or bytes to
 * be processed for each string
 * variable. The datum is read or written using the named variable, which is
 * created if needed and always set to the given <em>type</em>.
 * <p>
 * There is a special case of <code>FIELD <em>file-identifier</em> CLEAR</code>
 * which removes the active field specification from the file, without replacing
 * it with something new.
 * 
 * <p>
 * 
 * @author tom
 * @version version 1.0 December 11, 2008
 * 
 */

class FieldStatement extends Statement {

	/**
	 * Compile 'FIELD' statement. Processes a token stream, and compiles it
	 * into a byte-code stream associated with the statement object. The first
	 * token in the input stream has already been removed, since it was the
	 * "verb" that told us what kind of statement object to create.
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

		if( tokens.endOfStatement())
			return status = new Status(Status.EXPFID);

		/*
		 * Create a new byte code object for this statement
		 * that we'll generate code for.
		 */
		byteCode = new ByteCode(session, this);

		/*
		 * Parse the file specification that this record will be bound to.
		 * We'll generate the code to reference this later after we've
		 * processed the record definition.
		 */

		FileParse fileReference = null;
		LValue recordName = null;

		int mark = tokens.getPosition();
		fileReference = new FileParse(tokens, true);
		boolean isFileReference = true;
		if( fileReference.getStatus().failed()) {
			isFileReference = false;
			tokens.setPosition(mark);
			recordName = new LValue(session, true);
			status = recordName.compileLValue(byteCode, tokens);
			if( status.failed())
				return status;
		}

		/*
		 * Is this the CLEAR form?  If so, we generate code to de-assign the
		 * field designation.
		 */
		if( isFileReference && tokens.assumeNextToken("CLEAR")) {
			fileReference.generate(byteCode);
			byteCode.add(ByteCode._FIELD, 1 ); /* 1 means clear the field */
			return new Status();
		}

		/*
		 * Scan over the rest of the statement processing each
		 * field definition, which can include a size expression.
		 */
		final Expression exp = new Expression(session);
		int fCount = 0;
		String varName = null;

		/*
		 * There could be a USING <ident> clause, in which case we use the
		 * given expression, which must be a record.  This lets us build
		 * fields before a file is open, and then assign them to a new field.
		 */
		if( isFileReference && tokens.assumeNextToken("USING")) {
			exp.compile(byteCode, tokens);
		} else {


			/*
			 * Syntactic sugar. 
			 */
			if (!tokens.assumeNextToken(new String [] { "AS", ","}))
				return new Status(Status.FILECOMMA);

			/*
			 * Keep a list of field names to be sure there isn't
			 * a duplicate
			 */
			HashMap<String, String> fieldList = new HashMap<String, String>();
			
			/*
			 * No USING clause, so parse an explicit specification.
			 */
			while (true) {

				fCount++;
				int rCount = 2;

				/*
				 * First, need a field type
				 */
				String dataType = tokens.nextToken();
				if (tokens.getType() != Tokenizer.IDENTIFIER)
					return new Status(Status.INVRECDEF, dataType);

				if( dataType.equals("MAP") || dataType.equals("BITFIELDS"))
					dataType = "BITFIELD";
				
				byteCode.add(ByteCode._STRING, "TYPE");
				byteCode.add(ByteCode._STRING, dataType);

				/*
				 * If the type is a MAP then there is a whole different
				 * set of tasks.
				 */
				
				if( dataType.equals("BITFIELD")) {

					/*
					 * Build the MAP definition on the stack
					 */
					
					if( !tokens.assumeNextSpecial("("))
						return new Status(Status.INVRECDEF, "missing ( after MAP");
					
					/*
					 * Process list of bitmap fields
					 */
					dataType = "FIELD";
					
					int mapCount = 0;
					byteCode.add(ByteCode._STRING, "MAP");
					varName = "__BITFIELD" + JBasic.getUniqueID();
					
					while(true) {

						
						int mapFieldCount = 4;
						
						byteCode.add(ByteCode._STRING, "TYPE");
						byteCode.add(ByteCode._STRING, "INTEGER");
						
						
						byteCode.add(ByteCode._STRING, "LEN");
						exp.compile(byteCode, tokens);
						if( exp.status.failed())
							return new Status(Status.INVRECDEF, exp.status);

						/*
						 * If there is a position field, parse it and put it
						 * in the definition.  If it is omitted, then "next
						 * available bit" is assumed
						 */
						if(!tokens.assumeNextSpecial("@"))
							mapFieldCount--;
						else {
							byteCode.add(ByteCode._STRING, "POS");
							exp.compile(byteCode, tokens);
							if( exp.status.failed())
								return new Status(Status.INVRECDEF, exp.status);
						}

						if( !tokens.assumeNextToken("AS"))
							return new Status(Status.INVRECDEF, new Status(Status.EXPAS));
						
						if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
							return new Status(Status.INVRECDEF, new Status(Status.EXPNAME));
						
						byteCode.add(ByteCode._STRING, "NAME");
						byteCode.add(ByteCode._STRING, tokens.nextToken());
						byteCode.add(ByteCode._RECORD, mapFieldCount);
						mapCount++;
						
						if( !tokens.assumeNextSpecial(","))
							break;
							
					}
					
					if(!tokens.assumeNextSpecial(")"))
						return new Status(Status.INVRECDEF, "missing ) after MAP");
					
					/* Add code such that it gets added to the
					 * record definition for the fiend.
					 */
					byteCode.add(ByteCode._ARRAY, mapCount);
					
					rCount++;
				}
				else {

					/*
					 * If the type is string, there may be an optional
					 * size expression in parenthesis.  Note that the
					 * keywords VARYING and UNICODE may have a token
					 * STRING following them just for readability.
					 */

					boolean isString = false;
					if( dataType.equals("VARYING") || dataType.equals("UNICODE")) {
						tokens.assumeNextToken("STRING");
						isString = true;
					} else
						if( dataType.equals("STRING"))
							isString = true;

					if (isString) {

						if (tokens.testNextToken("(")) {
							byteCode.add(ByteCode._STRING, "SIZE");
							rCount = 3;
							exp.compile(byteCode, tokens);
							if (exp.status.failed())
								return exp.status;
						}
						else 
							return new Status(Status.INVRECDEF, new Status(Status.EXPSIZE));
					} 
					else if (dataType.equals("INTEGER")) {
						byteCode.add(ByteCode._STRING, "SIZE");
						rCount = 3;
						if (tokens.testNextToken("(")) {
							exp.compile(byteCode, tokens);
							if (exp.status.failed())
								return exp.status;
						}
						else
							byteCode.add(ByteCode._INTEGER, 4 );


					}
					else if (dataType.equals("FLOAT")) {
						byteCode.add(ByteCode._STRING, "SIZE");
						rCount = 3;
						if (tokens.testNextToken("(")) {
							exp.compile(byteCode, tokens);
							if (exp.status.failed())
								return exp.status;
						}
						else
							byteCode.add(ByteCode._INTEGER, 4 );

					}
					/*
					 * Not an item with a SIZE specification, so check the other scalars
					 */
					else if (!dataType.equals("BOOLEAN") 
							& !dataType.equals("DOUBLE"))
						return new Status(Status.INVRECDEF, new Status(Status.INVDATTYP, dataType));

					/*
					 * Next we get the field name.  Skip the optional "AS" between
					 * the type and name if given and the next token is really the
					 * identifier name.
					 */

					varName = tokens.nextToken();
					if (tokens.getType() != Tokenizer.IDENTIFIER)
						return new Status(Status.INVRECDEF, new Status(Status.INVNAME, varName));

					if( varName.equals("AS") & tokens.testNextToken(Tokenizer.IDENTIFIER))
						varName = tokens.nextToken();

					/*
					 * See if it is a duplicate - a no-no
					 */
					if( fieldList.get(varName) != null ) 
						return new Status(Status.DUPFIELD, varName);
					fieldList.put(varName, varName);

				}
				/*
				 * Construct remainder of the record definition.
				 */
				byteCode.add(ByteCode._STRING, "NAME");
				byteCode.add(ByteCode._STRING, varName);
				
				byteCode.add(ByteCode._RECORD, rCount);

				if (!tokens.assumeNextSpecial(",")) {
					byteCode.add(ByteCode._ARRAY, fCount);
					break;
				}
			}
		}

		/*
		 * At this point, the stack contains the record definition array.  We want
		 * to bind this to the file reference or the record name, depending on 
		 * what kind of FIELD state this was.
		 */

		if( recordName != null ) {
			recordName.compileStore();
		} else {
			fileReference.generate(byteCode);
			byteCode.add(ByteCode._FIELD, 0);  /* 0 means SET the field specification */
		}			
		return new Status();
	}

}

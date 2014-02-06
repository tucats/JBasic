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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.opcodes.OpCLEAR;
import org.fernwood.jbasic.opcodes.OpCATALOG;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;

class SaveStatement extends Statement {

	/**
	 * Execute 'save' statement.
	 * <p>
	 * <br>
	 * <br>
	 * 
	 * <code> SAVE "filename"</code>
	 * <p>
	 * <code> SAVE WORKSPACE [<em>"filename"]</em></code>
	 * <p>
	 * <br>
	 * 
	 * @param tokenStream
	 *            The token buffer being processed that contains the expression.
	 */

	public Status compile(final Tokenizer tokenStream) {

		status = new Status();
		this.byteCode = new ByteCode(this.session);
		
		/*
		 * Could be SAVE MESSAGES which saves the message database
		 * to an XML file.
		 */
		
		if( tokenStream.assumeNextToken("MESSAGES")) {
			Expression messageFile = new Expression(session);
			messageFile.compile(byteCode, tokenStream);
			if( messageFile.status.failed())
				return messageFile.status;
			byteCode.add(ByteCode._DEFMSG, 4);
			byteCode.add(ByteCode._NEEDP, 1);
			return new Status();
		}
		
		/*
		 * Saving a CATALOG can be done if the catalog has
		 * a persistance name assigned to it.
		 */
		
		if( tokenStream.assumeNextToken("CATALOG")) {
			
			if( !tokenStream.testNextToken(Tokenizer.IDENTIFIER)) 
				return new Status(Status.EXPNAME);
			/*
			 * Convert the record definition to XML code on the stack.
			 */
			
			String catName = tokenStream.nextToken();
			byteCode.add(ByteCode._LOADREF, catName);
			byteCode.add(ByteCode._INTEGER, 0);
			byteCode.add(ByteCode._STRING, "JBasicCatalog");
			byteCode.add(ByteCode._CALLF, 3, "XML");

			/*
			 * If there is an "AS" clause we use that as the
			 * catalog persistence name. Otherwise, if the catalog
			 * has no file name yet, then assign the default.
			 */
			
			if( tokenStream.assumeNextToken("AS")) {
				Expression e = new Expression(session);
				e.compile(byteCode, tokenStream);
				byteCode.add(ByteCode._CATALOG, OpCATALOG.SET_NAME, catName);
			}
			else {
				byteCode.add(ByteCode._STRING, catName + "_catalog.xml");
				byteCode.add(ByteCode._CATALOG, OpCATALOG.SET_NAME_IF, catName );
			}
			/*
			 *  Generate code to verify that this is a valid catalog
			 *  name and that it has a save name.
			 */
			byteCode.add(ByteCode._CATALOG, OpCATALOG.IS_VALID, catName);
			byteCode.add(ByteCode._CATALOG, OpCATALOG.IS_NAMED, catName);
			
			/*
			 * Generate a temporary name we'll use for manipulating some info here.
			 */
			String idName = "__XML$" + JBasic.getUniqueID();

			/*
			 * Generate code to open the file, create a valid XML header
			 * by calling XML() with no parameters, and then save the 
			 * catalog object on the stack.
			 */

			byteCode.add(ByteCode._STRING, "__CATALOG_NAME");
			byteCode.add(ByteCode._LOADR, catName);

			byteCode.add(ByteCode._OPEN, JBasicFile.MODE_OUTPUT, idName);
			byteCode.add(ByteCode._LOADREF, idName );
			byteCode.add(ByteCode._CALLF, 0, "XMLCOMMENT");
			byteCode.add(ByteCode._OUTNL, 1 );
			byteCode.add(ByteCode._LOADREF, idName);
			byteCode.add(ByteCode._SWAP);
			byteCode.add(ByteCode._OUTNL, 1);
			byteCode.add(ByteCode._LOADREF, idName);
			byteCode.add(ByteCode._CLOSE);
			byteCode.add(ByteCode._CLEAR, OpCLEAR.CLEAR_SYMBOL_ALWAYS, idName);
			byteCode.add(ByteCode._CATALOG, OpCATALOG.CLEAR_DIRTY_FLAG, catName);

			return new Status();
			
		}
		
		/*
		 * Could be SAVE XML which is quite different from saving a whole
		 * workspace; it only saves the current program.
		 */
		
		if( tokenStream.assumeNextToken("XML")) {
		
			/*
			 * Generate code to leave the XML on the stack of the current
			 * program. Note that the program name is determined at compile
			 * time, so this shouldn't be used in a program itself.
			 */
			
			if( session.programs.getCurrent() == null )
				return new Status(Status.NOPGM);
		
			/*
			 * Generate a temporary name we'll use for manipulating some info here.
			 */
			String idName = "__XML$" + JBasic.getUniqueID();

			/*
			 * Generate code to extract the LINES, USER and NAME fields from 
			 * a PROGRAM() definition.
			 */
			
			
			byteCode.add(ByteCode._STRING, session.programs.getCurrent().getName());
			byteCode.add(ByteCode._CALLF, 1, "PROGRAM");
			byteCode.add(ByteCode._STOR, idName );
			
			byteCode.add(ByteCode._STRING, "USER");
			byteCode.add(ByteCode._STRING, "USER");
			byteCode.add(ByteCode._LOADR, idName );
			byteCode.add(ByteCode._STRING, "LINES");
			byteCode.add(ByteCode._STRING, "LINES");
			byteCode.add(ByteCode._LOADR, idName );
			byteCode.add(ByteCode._STRING, "NAME");
			byteCode.add(ByteCode._STRING, "NAME");
			byteCode.add(ByteCode._LOADR,	idName );
			byteCode.add(ByteCode._RECORD, 	3);
			byteCode.add(ByteCode._CLEAR,  idName);
			/*
			 * Convert the record definition to XML code on the stack.
			 */
			byteCode.add(ByteCode._INTEGER, 1);
			byteCode.add(ByteCode._STRING, "JBasicProgram");
			byteCode.add(ByteCode._CALLF, 3, "XML");

			/*
			 * Generate code to open the user's file name.
			 */
			
			Expression fName = new Expression(session);
			fName.compile(byteCode, tokenStream);
			if( fName.status.failed())
				return fName.status;
			
			/*
			 * Generate code to open the file, create a valid XML header
			 * by calling XML() with no parameters, and then save the 
			 * compound object on the stack containing the program
			 * definition.
			 */
			byteCode.add(ByteCode._OPEN, JBasicFile.MODE_OUTPUT, idName);
			byteCode.add(ByteCode._LOADREF, idName );
			byteCode.add(ByteCode._CALLF, 0, "XMLCOMMENT");
			byteCode.add(ByteCode._OUTNL, 1 );
			byteCode.add(ByteCode._LOADREF, idName);
			byteCode.add(ByteCode._SWAP);
			byteCode.add(ByteCode._OUTNL, 1);
			byteCode.add(ByteCode._LOADREF, idName);
			byteCode.add(ByteCode._CLOSE);
			return new Status();
		}
		
		/*
		 * Could be SAVE PROTECTED which is similar to SAVE XML
		 * but saves the code without the source.
		 */
		
		if( tokenStream.assumeNextToken("PROTECTED")) {
		
			/*
			 * Generate code to leave the XML on the stack of the current
			 * program. Note that the program name is determined at compile
			 * time, so this shouldn't be used in a program itself.
			 */
			
			if( session.programs.getCurrent() == null )
				return new Status(Status.NOPGM);
		
			/*
			 * Generate a temporary name we'll use for manipulating some info here.
			 */
			String idName = "__XML$" + JBasic.getUniqueID();

			/*
			 * Generate code to extract the LINES, USER and NAME fields from 
			 * a PROGRAM() definition.
			 */
			
			String programName = session.programs.getCurrent().getName();
			byteCode.add(ByteCode._STRING, programName);
			byteCode.add(ByteCode._CALLF, 1, "BYTECODE");
			
			/*
			 * Convert the record definition to XML code on the stack.
			 */
			byteCode.add(ByteCode._INTEGER, 3);
			byteCode.add(ByteCode._STRING, "JBasicProgram");
			byteCode.add(ByteCode._CALLF, 3, "XML");
			
			/*
			 * Generate code to open the user's file name.
			 */
			
			Expression fName = new Expression(session);
			fName.compile(byteCode, tokenStream);
			if( fName.status.failed())
				return fName.status;
			
			/*
			 * Generate code to open the file, create a valid XML header
			 * by calling XML() with no parameters, and then save the 
			 * compound object on the stack containing the program
			 * definition.
			 */
			byteCode.add(ByteCode._OPEN, JBasicFile.MODE_OUTPUT, idName);
			byteCode.add(ByteCode._LOADREF, idName );
			byteCode.add(ByteCode._CALLF, 0, "XMLCOMMENT");
			byteCode.add(ByteCode._OUTNL, 1 );
			byteCode.add(ByteCode._LOADREF, idName);			
			byteCode.add(ByteCode._STRING, "Protected program ");
			byteCode.add(ByteCode._STRING, programName);
			byteCode.add(ByteCode._CONCAT);
			byteCode.add(ByteCode._STRING, " saved on ");
			byteCode.add(ByteCode._CONCAT);
			byteCode.add(ByteCode._CALLF, 0, "DATE");
			byteCode.add(ByteCode._CONCAT);
			byteCode.add(ByteCode._CALLF, 1, "XMLCOMMENT");
			byteCode.add(ByteCode._OUTNL, 1);
			byteCode.add(ByteCode._LOADREF, idName);
			byteCode.add(ByteCode._SWAP);
			byteCode.add(ByteCode._OUTNL, 1);
			byteCode.add(ByteCode._LOADREF, idName);
			byteCode.add(ByteCode._CLOSE);
			return new Status();
		}
		
	
		/*
		 * Eat the required WORKSPACE keyword, as in SAVE WORKSPACE. If there is
		 * additional text after the verb, parse it as a string expression that
		 * contains the name of the file to which the text is saved.
		 * 
		 * If there was no token, create a value from the default workspace
		 * name.
		 */

		if (tokenStream.assumeNextToken("WORKSPACE")) {

			if (!tokenStream.endOfStatement()) {
				final Expression expr = new Expression(session);
				status = expr.compile(byteCode, tokenStream);
				if (status.failed())
					return status;
				byteCode.add(ByteCode._CVT, Value.STRING);
				byteCode.add(ByteCode._SAVE, 2);
			} else
				byteCode.add(ByteCode._SAVE, 3);
			byteCode.add(ByteCode._NEEDP, 1 );
			return status;
		}

		tokenStream.assumeNextToken("AS");
		if (tokenStream.endOfStatement()) {
			return status = new Status(Status.EXPFNAME);
		}
		/*
		 * Parse the file name
		 */
		final Expression expr = new Expression(session);
		status = expr.compile(byteCode, tokenStream);
		if (status.failed())
			return status;
		byteCode.add(ByteCode._CVT, Value.STRING);
		byteCode.add(ByteCode._SAVE, 1);
		byteCode.add(ByteCode._NEEDP, 1 );

		return status;

	}
}
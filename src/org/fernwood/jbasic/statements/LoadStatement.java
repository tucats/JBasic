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
import org.fernwood.jbasic.MessageManager;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.opcodes.OpCLEAR;
import org.fernwood.jbasic.opcodes.OpCATALOG;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicFile;

/**
 * LOAD statement handler. This loads an external file into memory as a stored
 * program, that can be subsequently executed with a RUN command.
 * <p>
 * The syntax of the LOAD statement is:
 * <p>
 * <code>
 * LOAD "name"
 * </code>
 * <p>
 * When a program is loaded into memory, it can then be run. The most recently
 * loaded program becomes the default program which can be executing using the
 * <code>RUN</code> command with no arguments. Alternatively, the
 * <code>RUN</code> command can specify the specific program to run by name,
 * as defined in the <code>LOAD</code> command here.
 * <p>
 * 
 * @author cole
 * @version 1.1 August 2008 Removed LOAD FROM ARRAY syntax.
 */

class LoadStatement extends Statement {

	public Status compile( final Tokenizer tokens ) {

		Status status = null;

		this.byteCode = new ByteCode(session, this);
		byteCode.add(ByteCode._NEEDP, 1 );

		boolean loadIf = tokens.assumeNextToken("IF");
		
		if( tokens.assumeNextToken("CATALOG")) {
		
			if( !tokens.testNextToken(Tokenizer.IDENTIFIER)) 
				return new Status(Status.EXPNAME);

			String catName = tokens.nextToken();
			
			if( !tokens.assumeNextToken("FROM")) {
				byteCode.add(ByteCode._STRING, catName + "_catalog.xml");
			}
			else {
				Expression e = new Expression(session);
				e.compile(byteCode, tokens);
			}
			
			byteCode.add(ByteCode._DUP);
			
			/*
			 * Generate a temporary name we'll use for manipulating some info here.
			 */
			String idName = "__XML$" + JBasic.getUniqueID();

			/*
			 * Generate code to open the file, create a valid XML header
			 * by calling XML() with no parameters, and then save the 
			 * catalog object on the stack.
			 */
			int s1 = 0;
			int s2 = 0;
			
			if( loadIf) {
				byteCode.add(ByteCode._DUP);
				byteCode.add(ByteCode._CALLF, 1, "EXISTS");
				s1 = byteCode.add(ByteCode._BRNZ, 0);
				byteCode.add(ByteCode._DROP);
				byteCode.add(ByteCode._DROP);
				s2 = byteCode.add(ByteCode._BR, 0);
				Instruction i = byteCode.getInstruction(s1);
				i.integerOperand = s2+1;
				s1 = s2;
			}
			byteCode.add(ByteCode._OPEN, JBasicFile.MODE_INPUT, idName);
			byteCode.add(ByteCode._LOADFREF, idName);
			byteCode.add(ByteCode._INPUTXML, 1);
			byteCode.add(ByteCode._STRING, "JBasicCatalog");
			byteCode.add(ByteCode._CALLF, 2, "XMLPARSE");
			byteCode.add(ByteCode._DUPREF);			
			byteCode.add(ByteCode._CATALOG, OpCATALOG.IS_VALID);
			byteCode.add(ByteCode._STOR, catName);
			
			byteCode.add(ByteCode._CATALOG, OpCATALOG.SET_NAME, catName);
			byteCode.add(ByteCode._CATALOG, OpCATALOG.CLEAR_DIRTY_FLAG, catName);

			byteCode.add(ByteCode._LOADREF, idName);
			byteCode.add(ByteCode._CLOSE);
			s2 = byteCode.add(ByteCode._CLEAR, OpCLEAR.CLEAR_SYMBOL_ALWAYS, idName);
			if( loadIf ) {
				Instruction i = byteCode.getInstruction(s1);
				i.integerOperand = s2 + 1;
			}
			return new Status();
		}
		
		
		if (tokens.endOfStatement())
			return new Status(Status.EXPFNAME);

		final Expression fname = new Expression(session);
		
		boolean fMessages = tokens.assumeNextToken("MESSAGES");
		if( fMessages && tokens.endOfStatement())
			byteCode.add(ByteCode._STRING, MessageManager.MESSAGE_FILE_NAME);
		else {
			status = fname.compile(byteCode, tokens);
			if (status.failed())
				return fname.status;
		}
		
		if( fMessages )
			byteCode.add(ByteCode._DEFMSG, 3);
		else
			byteCode.add(ByteCode._LOADFILE, loadIf ? 1 : 0);
		
		return new Status();
	}

}
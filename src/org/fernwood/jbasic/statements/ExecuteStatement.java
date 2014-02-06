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
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.opcodes.OpLOADREF;
import org.fernwood.jbasic.opcodes.OpSYS;
import org.fernwood.jbasic.opcodes.OpTHREAD;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * EXECUTE statement handler. This parses a string expression an executes it as
 * a statement. This allows a program to support indirect execution, etc.
 * 
 * @author cole
 * 
 */

class ExecuteStatement extends Statement {

	/**
	 * Compile 'EXECUTE' statement. Processes a token stream, and compiles it
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
		byteCode = new ByteCode(session, this);
		int returnFlag = 0;
		int sandBoxFlag = 0;
		LValue returnValue = null;
		ArrayList<String> permList = new ArrayList<String>();
		
		if( tokens.assumeNextToken("SANDBOX")) {
			sandBoxFlag = 10;
			status = parsePermList(tokens, permList);
			if( status.failed())
				return status;
		}
		
		final Expression exp = new Expression(session);
		exp.compile(byteCode, tokens);
		if (exp.status.failed())
			return exp.status;

		while( !tokens.endOfStatement()) {
			if( tokens.assumeNextToken("SANDBOX")) {
				sandBoxFlag = 10;
				
				status = parsePermList(tokens, permList);
				if( status.failed())
					return status;
				continue;
			}
			if (tokens.assumeNextToken(new String[] { "RETURNING", "RETURNS"})) {
				final boolean fStrongTyping = session.globals().getBoolean("SYS$STATIC_TYPES");
				returnValue = new LValue(session, fStrongTyping);
				Status sts = returnValue.compileLValue(byteCode, tokens);
				if (sts.failed())
					return sts;
				returnFlag = 1;
				continue;
			}
			if( tokens.peek(0).equals("AS") & tokens.peek(1).equals("THREAD")) {
				tokens.assumeNextToken("AS");
				tokens.assumeNextToken("THREAD");

				if( returnFlag == 1 )
					return new Status(Status.THREADRET);

				if( tokens.assumeNextSpecial("(")) {
					String vName = tokens.nextToken();
					if( tokens.getType() != Tokenizer.IDENTIFIER)
						return status = new Status(Status.LVALUE, vName);
					if( !tokens.assumeNextSpecial(")"))
						return status = new Status(Status.PAREN);

					byteCode.add(ByteCode._THREAD, OpTHREAD.EXEC_THREAD, vName);
				} else
					byteCode.add(ByteCode._THREAD, OpTHREAD.EXEC_THREAD);
				
				return status = new Status();
			}
			return new Status(Status.SYNINVTOK, tokens.getSpelling());
		}
		/*
		 * Because the EXEC returns the last error generated, which may not
		 * be the current error, let's force the status code to be success
		 * before we do this execute, if we are expected to get a statement-
		 * specific return code.
		 */
		
		if( returnFlag == 1 ) {
			byteCode.add(ByteCode._SIGNAL, Status.SUCCESS);
		}
		
		/*
		 * Generate the instruction to execute the string.
		 */
		
		String pListName = null;
		
		if( sandBoxFlag > 0 ) {
			byteCode.add(ByteCode._SYS, OpSYS.SYS_SHOWPERM);
			pListName = "__PERMLIST_" + JBasic.getUniqueID();
			byteCode.add(ByteCode._STOR, pListName);
			
			/*
			 * Clear all permissions, then add in any explicitly
			 * named in the SANDBOX(...) clause
			 */
			byteCode.add(ByteCode._SYS, OpSYS.SYS_SETPERM, "NONE");
			for( int ix = 0; ix < permList.size(); ix++ )
				byteCode.add(ByteCode._SYS, OpSYS.SYS_SETPERM, permList.get(ix));			
		}
		
		byteCode.add(ByteCode._EXEC, sandBoxFlag + returnFlag);

		if( sandBoxFlag > 0 ) {
			/* 
			 * Load the list of saved permissions (and clear the temp symbol)
			 * Then, set that list as the new current permissions list
			 */
			byteCode.add(ByteCode._LOADREF, OpLOADREF.LOADREF_AND_CLEAR, pListName);
			byteCode.add(ByteCode._SYS, OpSYS.SYS_SETPERM);
		}
		/*
		 * If there is a RETURNS clause, generate the code to define the
		 * location to store the result, etc.
		 */

		Status sts = new Status();
		if (returnFlag == 1 && returnValue != null) {
			sts = returnValue.compileStore();

		}

		return sts;
	}

	/**
	 * Parse a list of permission names in parenthesis, separated by 
	 * commas.  Store the result in the ArrayList passed in.
	 * @param tokens
	 * @param permList
	 */
	private Status parsePermList(final Tokenizer tokens,
			ArrayList<String> permList) {
		if( tokens.assumeNextSpecial("(")) {
			while( true ) {
				String pName = tokens.nextToken();
				if( tokens.getType() != Tokenizer.IDENTIFIER || 
						!Permissions.valid(pName))
					return new Status(Status.INVPERM, pName);
				permList.add(pName);
				if( tokens.assumeNextSpecial(","))
					continue;
				if( tokens.assumeNextSpecial(")"))
					break;
				return new Status(Status.INVPERM, tokens.nextToken());
			}
		}
		return new Status();
	}
}
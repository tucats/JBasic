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
 * Created on Feb 21, 2007 by tom
 *
 */
package org.fernwood.jbasic.statements;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * @author tom
 * @version version 1.0 Feb 21, 2007
 *
 */
public class MidXStatement extends Statement {

	/**
	 * Compile 'MID$' statement.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */
	@SuppressWarnings("unchecked") 
	public Status compile(final Tokenizer tokens) {
		
		byteCode = new ByteCode(session, this);
		final int START = 1;
		final int END = 2;
		
		if( !tokens.assumeNextSpecial("(")) {
			return status = new Status(Status.SYNEXPTOK, "'('");
		}
		
		LValue target = new LValue(session, strongTyping());
		target.forceReference(true);

		Expression exp = new Expression(session);
		exp.setDeferPosts(true);
		
		/*
		 * Get the target value.
		 */
		
		target.compileLValue(byteCode, tokens);
		if( target.error)
			return status = new Status(Status.EXPSYNTAX, new Status(Status.LVALUE));
		
		/*
		 * Compile start byte expression
		 */
		
		if( !tokens.assumeNextSpecial(","))
			return status = new Status(Status.EXPRESSION, 
					new Status(Status.SYNEXPTOK, "','"));
		
		exp.compile(byteCode, tokens);
		if( exp.status.failed())
			return exp.status;
		byteCode.add(ByteCode._STORREG, START);
		
		/*
		 * Compile end byte expression
		 */
		if( !tokens.assumeNextSpecial(","))
			return status = new Status(Status.EXPRESSION, 
					new Status(Status.SYNEXPTOK, "','"));
		
		exp.compile(byteCode, tokens);
		if( exp.status.failed())
			return exp.status;
		byteCode.add(ByteCode._STORREG, END);
		
		/*
		 * Skip past the closing paren and equals sign, and get the string
		 * expression that is to be stuffed into the target string variable.
		 */
		if( !tokens.assumeNextSpecial(")"))
			return status = new Status(Status.EXPSYNTAX, 
					new Status(Status.SYNEXPTOK, "')'"));

		if( !tokens.assumeNextSpecial("="))
			return status = new Status(Status.EXPSYNTAX,  
					new Status(Status.SYNEXPTOK, "'='"));
		
		exp.compile(byteCode, tokens);
		if( exp.status.failed())
			return exp.status;

		target.compileLoad();
		byteCode.add(ByteCode._INTEGER, 1);
		byteCode.add(ByteCode._LOADREG, START);
		byteCode.add(ByteCode._SUBI, 1);
		byteCode.add(ByteCode._SUBSTR, 3 );
		
		byteCode.add(ByteCode._SWAP);
		
		byteCode.add(ByteCode._INTEGER, 1);
		byteCode.add(ByteCode._LOADREG, END);
		byteCode.add(ByteCode._LOADREG, START);
		byteCode.add(ByteCode._SUB);
		byteCode.add(ByteCode._ADDI, 1 );
		byteCode.add(ByteCode._SUBSTR, 3);
		
		byteCode.add(ByteCode._CONCAT);
		
		target.compileLoad();
		byteCode.add(ByteCode._LOADREG, END);
		byteCode.add(ByteCode._ADDI, 1);
		byteCode.add(ByteCode._SUBSTR, 2 );
		byteCode.add(ByteCode._CONCAT);
		
		target.compileStore();
		Expression.postIncrements(exp.incrementList(), byteCode);
		
		return new Status(Status.SUCCESS);
	}
}

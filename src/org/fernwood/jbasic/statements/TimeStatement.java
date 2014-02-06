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
import org.fernwood.jbasic.compiler.Optimizer;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.opcodes.OpTIME;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;

/**
 * TIME statement handler. Accepts a command, and runs it while timing the
 * execution latency. A temporary hidden symbol is created by the operation
 * that holds a record with details used to track execution statistics. This
 * is a uniquely-generated name that is used only for a specific compilation
 * of the TIME statement, allowing for nested TIME statements should that
 * be needed.
 * 
 * @author cole
 * @version 1.0 August 12, 2004
 * 
 */

class TimeStatement extends Statement {

	
	/**
	 * Compile the TIME statement
	 */
	public Status compile(final Tokenizer tokens) {

		byteCode = new ByteCode(session, this);
		Statement s = new Statement(session);
		
		/*
		 * It is possible that there is an iteration count on the statement; 
		 * if so get that now.  It must be a constant integer value.
		 */
		
		int count = 1;
		int sign = 1;
		
		if( tokens.assumeNextSpecial("(")) {
			if( tokens.assumeNextSpecial("-"))
				sign = -1;
			tokens.nextToken();
			if( tokens.getType() != Tokenizer.INTEGER) 
				return new Status(Status.EXPCONST);
			count = Integer.parseInt(tokens.getSpelling()) * sign;
			if( count < 1 )
				return new Status(Status.INVCOUNT, count);
			if( !tokens.assumeNextSpecial(")"))
				return new Status(Status.PAREN);
			
		}
		
		/*
		 * IF there is no statement to time, then boo-boo.
		 */
		
		if( tokens.endOfStatement()) 
			return new Status(Status.NOEXE);
		
		/*
		 * Take remainder of the buffer and store it in the new statement. We
		 * will then use the compiled results of that statement.
		 */
		s.program = program;
		s.statementLabel = statementLabel;
		s.statementID = statementID;
		
		s.store(tokens.getBuffer());
		if( s.status.failed())
			return s.status;
		tokens.flush();		
		
		/*
		 * We must optimize the code stream now so a subsequent pass won't result
		 * in unwanted intra-statement optimizations. 
		 */
		Optimizer o = new Optimizer();
		o.optimize(s.byteCode);
		
		/*
		 * Generate the code to capture the time info, execute the code from the
		 * compiled statement, and then calculate and print the delta time.
		 */
		String dataName = "__TIME_" + Integer.toString(JBasic.getUniqueID());
		String indexName = "__INDEX_" + Integer.toString(JBasic.getUniqueID());
		byteCode.add( ByteCode._TIME, 0, dataName );
		int mark = 0;
		boolean bigLoop = (count>OpTIME.BIG_LOOP);
		
		if( bigLoop ) {
			byteCode.add(ByteCode._INTEGER, count );
			byteCode.add(ByteCode._FORX, 0, indexName);
			mark = byteCode.size()-1;	
			byteCode.concat(s.byteCode);
			byteCode.add(ByteCode._NEXT, mark+1, indexName);
			Instruction i = byteCode.getInstruction(mark);
			i.integerOperand = byteCode.size();
		}
		else {
			for( int idx = 0; idx < count; idx++)
				byteCode.concat(s.byteCode);
		}
		
		byteCode.add( ByteCode._TIME, count, dataName );
		
		return new Status();
	}
}
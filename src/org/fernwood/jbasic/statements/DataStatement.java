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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;

/**
 * Compile a <code>DATA</code> statement. A <code>DATA</code> statement is 
 * not executable in the normal sense of the word in a JBasic program, but
 * instead represents the expression of one or more data values; typically 
 * constants. The elements of a <code>DATA</code> statement are accessed by 
 * a <code>READ</code> statement, which reads the "next" data element 
 * available, regardless of where the <code>DATA</code> statement(s) are found
 * in the program. 
 * <p>
 * Because <code>DATA</code> elements are actually implemented by compiling 
 * expressions, a <code>DATA</code> statement could technically contain 
 * references to variables or other non-constant expressions. However, these 
 * would be evaluated in the context of the <code>READ</code> statement, 
 * resulting in hard-to-predict behavior.  This is because variable name 
 * resolution is bound only at the time the data element is 
 * read.  At this time, use of non-constant values in a <code>DATA</code>
 * statement results in a compile error.
 * 
 * @author tom
 * @version version 1.0 Aug 16, 2006
 * 
 */
public class DataStatement extends Statement {

	public Status compile(final Tokenizer tokens) {

		if (program == null)
			return new Status(Status.NOACTIVEPGM);

		byteCode = new ByteCode(session, this);
		int updateAddress = 0;
		int count = 0;

		/*
		 * Get an expression compiler ready.  Disable constant pooling,
		 * which would result in symbolic references which are not allowed
		 * in DATA values.
		 */
		final Expression exp = new Expression(session);
		exp.setArrayConstantPooling(false);

		/*
		 * For each comma-separate expression on the line, generate a new
		 * _DATA block that contains the code for that expression (usually
		 * just a constant load).  These code segments will be collected
		 * up later during link time to support runtime READ operations.
		 */


		while (true) {
			
			/*
			 * Mark the start of a data item.  The zero is a place-holder,
			 * later this will be patched up to reflect how many bytes are
			 * generated for the expression.
			 */
			byteCode.add(ByteCode._DATA, 0);
			updateAddress = byteCode.size() - 1;
			
			/*
			 * Parse the expression and generate the code for it.
			 */
			
			exp.compile(byteCode, tokens);
			if (exp.status.failed())
				return exp.status;
			
			/*
			 * The DATA statement really needs to be a constant, not an
			 * expression.
			 */
			if( !exp.isConstant)
				return new Status(Status.EXPCONST);
			
			/*
			 * Figure out how many bytes we generated, and locate the 
			 * _DATA instruction again.  Patch it's integer argument to
			 * be the count of instructions from end-of-bytecode to the
			 * location of the _DATA instruction.
			 */
			count = byteCode.size() - updateAddress - 1;
			final Instruction i = byteCode.getInstruction(updateAddress);
			i.integerOperand = count;

			/*
			 * DATA items are separated by a comma.  If there isn't one 
			 * the we've come to the end of the statement.
			 */
			if (!tokens.assumeNextToken(","))
				break;
		}

		return new Status();

	}
}

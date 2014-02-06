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
 * ASM statement handler. This allows in-line ByteCode to be generated in a
 * program, to create constructs not allowed by the BASIC syntax.
 * <p>
 * <b>SYNTAX</b>
 * <p>
 * <code>ASM opcode [integer-arg] [ double-arg] [ string-arg] [, opcode...]</code>
 * <p>
 * The opcode must be first item after the ASM statement  The opcode arguments
 * do not have commas between them; the comma indicates that there are more than
 * one opcode defined in a single ASM statement.  If the given argument is not to
 * be included in the instruction, then omit it.  Double values must be explicitly
 * in DOUBLE format; i.e. 3.0 rather than just 3 so they are distinguishable from
 * integer arguments.
 * <p>
 * <b>USAGE</b>
 * <p>
 * No attempt is made to validate that the instruction is correctly 
 * formed; that is, if the instruction requires a string argument, there
 * is no check that there is an argument present until runtime.
 * 
 * @author cole
 * @see ByteCode
 */
public class AsmStatement extends Statement {

	public Status compile(final Tokenizer tokens) {

		if( tokens.endOfStatement())
			return status = new Status(Status.ASMEXPOPCODE);
		
		/*
		 * Generate a new bytecode buffer to hold the assembled
		 * bytecode operations.
		 */
		byteCode = new ByteCode(session, this);
		
		/*
		 * The generated code stream must ensure that sandbox mode is clear,
		 * so you can't assemble instructions that defeat the sandbox mode.
		 * So this opcode will check to see if the current user has the ASM
		 * privilege, and will generate a runtime error if not.
		 */
		
		byteCode.add(ByteCode._ASM, 0);
		int blockStart = byteCode.size()-1;
		
		/*
		 * If the statement is ASM USING(exp) then compile the expression
		 * and call the runtime assembler.
		 */
		if( tokens.assumeNextToken("USING")) {
			Expression exp = new Expression(session);
			exp.compile(byteCode, tokens);
			byteCode.add(ByteCode._ASM);
			return exp.status;
		}
		/*
		 * The remainder of the line of text is assumed to be assembler
		 * code.  Grab the rest of the token buffer and pass it to the
		 * bytecode assembler.  Then flush the token buffer so it is all
		 * considered "consumed" and report back on the status of the
		 * assembly operation.
		 */
		
		status = byteCode.assemble(tokens.getBuffer());
		
		/*
		 * Patch up the _ASM instruction that started this block.
		 */
		
		Instruction i = byteCode.getInstruction(blockStart);
		i.integerOperand = byteCode.size() - blockStart - 1;
		i.integerValid = true;
		tokens.flush();
		return status;
	}
}

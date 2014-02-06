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
package org.fernwood.jbasic.funcs;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.statements.Statement;
import org.fernwood.jbasic.value.Value;

/**
 * <b>COMPILE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Compile a statement to bytecode.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = COMPILE( <em>string</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of strings</td></tr>
 * </table>
 * <p>
 * Accepts a string that contains a JBasic statement.  The statement is 
 * compiled to the internal <em>bytecode</em> representation, and the result
 * is stored as an array of strings.  Each string contains a text representation
 * of the bytecode that executes the compiled statement.  This array can be
 * later used in an ASM USING(<em>array</em>) to store the bytecode in an
 * executable program.
 * @author cole
 * @version 1.0 August 12, 2004
 * 
 */

public class CompileFunction extends JBasicFunction {

	/**
	 * Compile a statement.
	 * 
	 * @param symbols
	 *            The symbol table to use to resolve identifiers, or null if no
	 *            symbols allowed.
	 */

	public Value run (final ArgumentList argList, final SymbolTable symbols) throws JBasicException {

		argList.validate(1,1,new int[] {Value.STRING});
		/*
		 * Construct a new statement, and store/compile the statement
		 */
		Status status = new Status(Status.SUCCESS);
		final Statement theStatement = new Statement(argList.session);

		/*
		 * Store the string argument containing the text in the statement object we
		 * created. This will generate a compilation.  If the compile fails,
		 * then we are done here.
		 */
		Tokenizer tokens = new Tokenizer( argList.stringElement(0), JBasic.compoundStatementSeparator);
		
		status = theStatement.store(argList.session, tokens, null);
		if( status.failed()) {
			if( argList.session.signalFunctionErrors())
				throw new JBasicException( status );
			return new Value(Value.ARRAY, null);
		}
		
		ByteCode bc = theStatement.byteCode;
		Value result = new Value(Value.ARRAY, null);
		
		for( int ix = 0; ix < bc.size(); ix++ ) {
			Instruction i = bc.getInstruction(ix);
			StringBuffer b = new StringBuffer(ByteCode.disassembleInstruction(-1, i));
			if( b.charAt(0) == ':')
				b.deleteCharAt(0);
			result.addElement(new Value(b.toString().trim()));
		}
		
		return result;
	}
}
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

/**
 * SYSTEM statement handler. This statement lets a JBasic program run a native
 * shell command. The implementation in in the OpSYS class in the opcodes
 * package.
 * <p>
 * The code is sensitive to the runtime environment to the degree that it knows
 * how to create appropriate Windows invocations of commands. Output is directed
 * to the console, and identified as error or standard output text. The result
 * of the system operation is stored in the variable SYS$STATUS for future use.
 * <p>
 * Most of the code here was stolen from, er, derived from the very helpful
 * information on the web site at:
 * <p>
 * <code>
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html
 * </code>
 * </p>
 * written by Michael C. Daconta, which explains a lot of the pitfalls in using
 * the exec() runtime call on various systems, including interactions with
 * eating the process output in a timely fashion and how to synchronize
 * correctly. Many thanks for an excellent article.
 * <p>
 * 
 * @author cole
 * 
 */

class SystemStatement extends Statement {

	public Status compile(final Tokenizer tokens) {

		/*
		 * Create a new bytecode buffer and an expression processor
		 */
		byteCode = new ByteCode(session);
		final Expression exp = new Expression(session);
		int arrayCount = 0;
		
		/*
		 * Compile the rest of the statement as a string expression.  The
		 * generated code leaves the expression result on the top of the
		 * runtime stack.  Note that if there are multiple expresions
		 * separated by commas, we create an array on the stack of them.
		 */
		
		while( true ) {
			exp.compile(byteCode, tokens);
			if (exp.status.failed())
				return exp.status;
			arrayCount++;
			if( tokens.assumeNextSpecial(","))
				continue;
			break;
		}
		/*
		 * Generate the _SYS instruction which takes the string on the top
		 * of the stack and executes it as a native operating system command.
		 * Also generate code that requires that a new command prompt be
		 * printed after this statement executes if in NOPROMPTMODE.
		 */
		
		if( arrayCount > 1 )
			byteCode.add(ByteCode._ARRAY, arrayCount);
		
		byteCode.add(ByteCode._SYS, 0);
		byteCode.add(ByteCode._NEEDP, 1 );
		return new Status(); 
	}

}

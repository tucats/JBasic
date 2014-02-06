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
import org.fernwood.jbasic.compiler.FileParse;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.opcodes.OpTHREAD;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * 
 * <code>KILL "file-name"</code>
 * 
 * The <code>KILL</code> command is used to delete a file from the file
 * system. The command requires a string expression that identifies the file to
 * delete. This name must be expressed in the native filename syntax.
 * <p>
 * Alternatively, the <code>KILL</code> statement can be used to kill and
 * close an open file, by using the syntax:
 * 
 * <code> KILL FILE <em>identifier</em> </code>
 * 
 * The keyword <code>FILE</code> is required. In this case, the identifier is
 * a symbol containing the file identification record. This is usually the
 * identifier from the OPEN statement. If you <code>KILL</code> an open file,
 * the file is closed as well as deleted.
 * 
 * @author tom
 * @version version 1.0 Jan 30, 2006
 * 
 */
public class KillStatement extends Statement {

	/**
	 * Compile 'KILL' statement. Processes a token stream, and compiles it into
	 * a byte-code stream associated with the statement object. The first token
	 * in the input stream has already been removed, since it was the "verb"
	 * that told us what kind of statement object to create.
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

		final Expression exp = new Expression(session);
		int isFileHandle = 0;

		/*
		 * You can KILL THREAD <name>. This must be reinterpreted at runtime as
		 * a THREAD STOP command with the expression resolved.
		 */

		if (tokens.assumeNextToken("THREAD")) {

			
			/*
			 * Compile the expression that
			 * identifies the thread name.  Generated code leaves this on
			 * the top of the execution stack.
			 */
			exp.compile(byteCode, tokens);
			if (exp.status.failed())
				return exp.status;
			
			/*
			 * Generate the _THREAD operation that stops the thread identified
			 * by the string on top of the execution stack.
			 */
			byteCode.add(ByteCode._THREAD, OpTHREAD.STOP_THREAD);

			return new Status();
		}
		/*
		 * You can KILL FILE <fileid> or KILL "filename". See if this is the
		 * KILL FILE case.
		 */

		final FileParse f = new FileParse( tokens, true);
		if (f.success()) {
			isFileHandle = 1;
			f.generate(byteCode);
		}

		/*
		 * Nope, it's a string expression for a file name.
		 */
		else {
			exp.compile(byteCode, tokens);
			if (exp.status.failed())
				return exp.status;
		}

		/*
		 * Either way, kill it. Pass the flag indicating if the top-of-stack is
		 * a file reference (1) or a file name (0);
		 * 
		 */
		byteCode.add(ByteCode._KILL, isFileHandle);

		return new Status();
	}

}

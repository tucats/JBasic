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
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Linker;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * Utility statement to force the link of a compiled program. Normally a program
 * is not linked until it needs to be run, to defer the runtime expense until it
 * is really justified. This speeds up loading very large workspaces, for
 * example.
 * <p>
 * Howeer, it is convenient to be able to force the link of a program,
 * particularly to see a DISASM output of the linked program.
 * <p>
 * Note that if the program is already linked, it is first unlinked and then
 * linked again.  This causes each statement to be recompiled and then the
 * program re-constructed as a linked program.  This is useful if you change
 * optimization settings, for example, and which to observe the effect.
 * @author tom
 * @version version 1.1 December 15, 2006
 * 
 */
public class LinkStatement extends Statement {

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		/*
		 * The LINK statement should be followed by an explicit
		 * prompt even if we are in NOPROMPTMODE.  So set the session
		 * flag indicating we think a prompt is required after this
		 * command.
		 */
		this.session.setNeedPrompt(true);

		/*
		 * See if STRIP mode is enabled.  In this mode, when a program
		 * is linked, the program text is removed from the linked program
		 * to make the resulting bytecode smaller.  This will be passed on
		 * to the linker.
		 */
		boolean doStrip = session.getBoolean("SYS$STRIP");
		
		/*
		 * If there is nothing after the LINK command, then it is assuming
		 * the current program.
		 */
		if (tokens.endOfStatement()) {
			
			/*
			 * If there is no current program, then it is an error.
			 */
			if (session.programs.getCurrent() == null)
				return new Status(Status.NOPGM);
			
			/*
			 * Before we link, explicitly unlink. This results in
			 * the recompilation of each statement as an individual
			 * statement bytecode buffer.  It also discards many
			 * runtime data structures dependent on the linked bytecode,
			 * such as the DATA statement cache.
			 */
			
			status = Linker.unlink(session.programs.getCurrent());
			
			/*
			 * If the unlink is successful, follow it with the new link
			 * operation.
			 */
			if( status.success())
				status = session.programs.getCurrent().link(doStrip);
			return status;
		}


		/*
		 * There is text after the verb which is either the program
		 * name, or a prefix name like PROGRAM or FUNCTION followed
		 * by the actual name.  Start by assuming that it's a program
		 * and that the next (only) token is the name.
		 */

		String prefix = JBasic.PROGRAM;
		String pgmName = tokens.nextToken();
		if( !tokens.isIdentifier())
			return status = new Status(Status.EXPPGM);
		
		/*
		 * See if the name is a reserved word of PROGRAM, FUNCTION,
		 * TEST, or VERB.  IF any is true, then use the token to
		 * set the program type and get another token which now must
		 * be the actual name.
		 */

		if( pgmName.equals("PROGRAM"))
			pgmName = tokens.nextToken();
		else
			if( pgmName.equals("FUNCTION")) {
				prefix = JBasic.FUNCTION;
				pgmName = tokens.nextToken();
			}
			else
				if( pgmName.equals("VERB")) {
					prefix = JBasic.VERB;
					pgmName = tokens.nextToken();
				}
				else
					if( pgmName.equals("TEST")) {
						prefix = JBasic.TEST;
						pgmName = tokens.nextToken();
					}
		
		/*
		 * If the last token parsed isn't an identifier then it can't
		 * be a valid program name.  Throw an error.
		 */
		if( !tokens.isIdentifier())
			return new Status(Status.EXPPGM);

		/*
		 * Using the prefix for the program type and the program name,
		 * locate the actual program object.  If it can't be found
		 * then throw an error.
		 */

		final Program pgmValue = session.programs.find(prefix + pgmName);
		if( pgmValue == null )
			return status = new Status(Status.PROGRAM, pgmName);
			
		/*
		 * Before we link, explicitly unlink. This results in
		 * the recompilation of each statement as an individual
		 * statement bytecode buffer.  It also discards many
		 * runtime data structures dependent on the linked bytecode,
		 * such as the DATA statement cache.
		 */

		status = Linker.unlink(pgmValue);
		if( status.failed())
			return status;
		
		/*
		 * If the unlink is successful, follow it with the new link
		 * operation.  Pass the STRIP flag as appropriate.
		 */

		return status = pgmValue.link(doStrip);
	
	}
}

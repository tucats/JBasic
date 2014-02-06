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
 * Created on May 30, 2007 by cole
 *
 */
package org.fernwood.jbasic.opcodes;

import java.util.Date;
import java.util.Iterator;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.runtime.JBFOutput;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.statements.Statement;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * 
 * @author cole
 * @version version 1.0 May 30, 2007
 * 
 */
public class OpSAVE extends AbstractOpcode {

	/**
	 *  <b><code>_SAVE <em>mode</em></code><br><br></b>
	 * Execute the _SAVE instruction at runtime.
	 * 
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li>&nbsp;&nbsp;<code><em>mode</em></code> - SAVE mode type
	 * </list><p><br>
	 * <b>Implicit Arguments:</b><br>
	 * <list>
	 * <li>&nbsp;&nbsp;<code>stack[tos]</code> - filename</l1>
	 * </list><br><br>
	 *
	 * The SAVE mode type is an integer that tells what kind of SAVE operation
	 * we are to perform.
	 * 
	 * <list>
	 * <li> 1 - save the current program to a file whose name is on the stack
	 * <li> 2 - save the workspace to a file whose name is on the stack
	 * <li> 3 - save the workspace to the default filename.
	 * </list><br><br>
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 * may be null for a successful operation.
	 */
	public void execute(InstructionContext env) throws JBasicException {

		int mode = env.instruction.integerOperand;

		int count = 0;
		Value name = null;
		
		/*
		 * Determine the maximum line length we will permit, and then convert
		 * it to be a zero-based value.  If no global is set to define this,
		 * use the built-in constant MAX_SOURCE_LINE_LENGTH.  This defines
		 * the width beyond which source lines will be broken into multiple
		 * lines of code with continuation characters.  The default is 80.
		 */
		int maxLineLength = JBasic.MAX_SOURCE_LINE_LENGTH;
		
		name = env.session.globals().findReference("SYS$SOURCE_LINE_LENGTH", false);
		if( name != null )
			maxLineLength = name.getInteger();
		if( maxLineLength < 30)
			maxLineLength = 30;
		maxLineLength--;
		
		name = null;
		
		int labelWidth = 22;
		name = env.session.globals().findReference("SYS$LABELWIDTH", false);
		if( name != null )
			labelWidth = name.getInteger();
		if( labelWidth < 1)
			labelWidth = 1;
		
		name = null;

		JBFOutput outFile = null;

		/*
		 * If the mode is other than 3 and we are in "sandbox" mode we are
		 * not allowed to save files unless we have FILE_IO privileges
		 */
		
		if( mode != 3 )
			env.session.checkPermission(Permissions.FILE_IO);
		
		/*
		 * If the mode is other than 3, we need a filename expression from the
		 * stack.
		 */

		if (mode == 3)
			name = new Value(Utility.normalize(env.session.getWorkspaceName()));
		else
			name = new Value(Utility.normalize(env.pop().getString()));

		/*
		 * Depending on the mode, do the write thing.
		 */

		switch (mode) {

		case 2:
		case 3:

			/*
			 * SAVE WORKSPACE.
			 * 
			 * Try to open the file.
			 */

			outFile = new JBFOutput(env.session);
			outFile.open(name, null);

			/*
			 * Scan over the list of all programs. Skip empty ones or those that
			 * are system objects never modified.
			 */

			for (final Iterator i = env.session.programs.iterator(); i.hasNext();) {
				final Program storedProgram = (Program) i.next();
				if (storedProgram.getName() == null)
					continue;
				if (storedProgram.isSystemObject())
					continue;

				if (count == 0) {
					final Date rightNow = new Date();
					outFile.println("// SAVE file " + name.getString());
					outFile.println("//");
					outFile.println("// Created " + rightNow.toString());
					outFile.println("//");
				}
				count++;

				int len;
				if (storedProgram.isProtected()) {
					outFile.println("PROGRAM " + storedProgram.getName());
					storedProgram.getExecutable().saveProtectedBytecode(outFile);
					continue;
				}

				len = storedProgram.statementCount();
				for (int ix = 0; ix < len; ix++) {
					final Statement stmt = storedProgram.getStatement(ix);

					String lineNumberText;
					if (stmt.lineNumber > 0)
						lineNumberText = Utility.pad(Integer.toString(stmt.lineNumber), 5);
					else
						lineNumberText = Utility.spaces(5);
					
					String labelText;
					if (stmt.statementLabel != null)
						labelText = Utility.pad(stmt.statementLabel + ":", labelWidth);
					else
						labelText = Utility.spaces(labelWidth);

					/*
					 * Assemble the parts of the output into a buffer
					 */
					StringBuffer output = new StringBuffer(lineNumberText);
					output.append(' ');
					output.append(labelText);
					output.append(' ');
					output.append(stmt.statementText);
					
					/*
					 * Write the pieces of the buffer that are over 80 characters
					 * long, with line continuation characters.  We break at
					 * spaces or commas.
					 */
					while( output.length() > maxLineLength ) {
						int xp;
						for( xp = maxLineLength; xp > 0; xp--)
							if( output.charAt(xp) == ' ' || output.charAt(xp) == ',')
								break;
						if( xp == 0 )
							xp = maxLineLength;
						outFile.print(output.substring(0, xp).toString());
						outFile.println("\\");
						output.delete(0, xp);
					}
					outFile.println(output.toString());
				}

				/*
				 * Now that it's been saved, we can clear this bit.
				 */
				storedProgram.clearModifiedState();
			}
			outFile.close();
			env.session.setWorkspaceName(name.getString());
			
			Value wsn = env.session.globals().localReference("SYS$WORKSPACE");
			if( wsn == null )
				env.session. globals(). insertReadOnly("SYS$WORKSPACE", name);
			else {
				wsn.setString(env.session.getWorkspaceName());
			}
			env.session.stdout.println("Workspace saved to " + env.session.getWorkspaceName());
			break;

		case 1:

			/*
			 * SAVE "program-name"
			 * 
			 * Start by making sure there is a current program.
			 */

			Program program = null;
			if (env.codeStream.statement != null)
				program = env.codeStream.statement.program;

			if (program == null)
				program = env.session.programs.getCurrent();

			if (program == null)
				throw new JBasicException(Status.NOPGM);

			/*
			 * Try to open the file.
			 */

			outFile = new JBFOutput(env.session);
			outFile.open(name, null);
			
			final Date rightNow = new Date();
			outFile.println("// PROGRAM MODULE " + program.getName()
					+ ", SAVED " + rightNow.toString());

			int len;
			if (program.isProtected()) {
				outFile.println("PROGRAM " + program.getName());
				program.getExecutable().saveProtectedBytecode(outFile);
				return;
			}

			len = program.statementCount();

			for (int ix = 0; ix < len; ix++) {
				final Statement stmt = program.getStatement(ix);

				String lineNumberText;
				if (stmt.lineNumber > 0)
					lineNumberText = Utility.pad(Integer.toString(stmt.lineNumber), 5);
				else
					lineNumberText = Utility.spaces(5);

				String labelText;
				if (stmt.statementLabel != null)
					labelText = Utility.pad(stmt.statementLabel + ":", labelWidth);
				else
					labelText = Utility.spaces(labelWidth);

				/*
				 * Assemble the parts of the output into a buffer
				 */
				StringBuffer output = new StringBuffer(lineNumberText);
				output.append(' ');
				output.append(labelText);
				output.append(' ');
				output.append(stmt.statementText);
				
				/*
				 * Write the pieces of the buffer that are over 80 characters
				 * long, with line continuation characters.  We break at
				 * spaces or commas.
				 */
				while( output.length() > maxLineLength ) {
					int xp;
					for( xp = maxLineLength; xp > 0; xp--)
						if( output.charAt(xp) == ' ' || output.charAt(xp) == ',')
							break;
					if( xp == 0 )
						xp = maxLineLength;
					outFile.print(output.substring(0, xp).toString());
					outFile.println("\\");
					output.delete(0, xp);
				}
				outFile.println(output.toString());
			}

			/*
			 * Now that it's been saved, we can clear this bit.
			 */
			program.clearModifiedState();
			env.session.stdout.println("Program " + program.getName()
					+ " saved to " + name);
			break;
			
			
		default:
			throw new JBasicException(Status.FAULT, 
					new Status(Status.INVOPARG, env.instruction.integerOperand));

		}
		return;
	}

}

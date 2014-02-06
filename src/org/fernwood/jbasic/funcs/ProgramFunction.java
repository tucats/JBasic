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

import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.statements.Statement;
import org.fernwood.jbasic.value.Value;

/**
 * <b>PROGRAM()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return info about a program in memory</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>r = PROGRAM( s [, n] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Record or String</td></tr>
 * </table>
 * <p>
 * This function returns information about a program in memory. The program
 * must be identified by name as a string expression in the first parameter.
 * If the optional second parameter is given, then the result is the text of
 * the given ordinal line number in the program (i.e. 1 returns the first line,
 * 2 returns the second line, etc.) regardless of line numbering.
 * <p>
 * If the line number is not given, then the result is a RECORD that describes
 * the state of the program, such as ACTIVE which is a boolean that indicates
 * if the program is running, or COUNT which tells the number of times the
 * program has been run.
 * 
 * @author cole
 * 
 */
public class ProgramFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws  JBasicException if an argument or execution error occurs 
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(1, 2, new int[]{Value.STRING, Value.NUMBER});
	
		String programName = arglist.stringElement(0);
		Program p = arglist.session.programs.find(programName);

		if (p == null) {
			if( arglist.session.signalFunctionErrors())
				throw new JBasicException(Status.PROGRAM, programName);
			return new Value(Value.RECORD, null);
		}	
		/*
		 * Was there a line number argument?  If so, then get that line
		 * and format it as text and return it as the function argument.
		 */
		if( arglist.size() > 1 ) {
			int ix = arglist.intElement(1);
			Statement st = null;
						
			if( ix < 1 || ix > p.statementCount())
				throw new JBasicException(Status.LINENUM, Integer.toString(ix));
			
			st = p.getStatement(ix-1);
			if( st == null )
				throw new JBasicException(Status.LINENUM, Integer.toString(ix));

			return new Value(st.toString());
		}
		
		/*
		 * Generate a record describing the overall program.
		 */
		final Value result = new Value(Value.RECORD, null);
		result.setElement(new Value(programName), "NAME");
		result.setElement(new Value(p.isProtected()), "PROTECTED");
		result.setElement(new Value(p.fHasData), "HASDATA");
		result.setElement(new Value(p.hasExecutable()), "LINKED");
		result.setElement(new Value(p.isModified()), "MODIFIED");
		result.setElement(new Value(!p.isSystemObject()), "USER");
		result.setElement(new Value(p.getRunCount()), "COUNT");
		result.setElement(new Value(p.isActive()), "ACTIVE");
		if( p.hasLoops())
			result.setElement(new Value(p.loopManager.loopStackSize()), "LOOPS");
		else
			result.setElement(new Value(0), "LOOPS");
		
		result.setElement(new Value(p.executableSize()), "BYTECODES");
		result.setElement(new Value(p.fStaticTyping), "STATICTYPES");

		/*
		 * Generate the array containing all the lines of text.  Note that
		 * we don't do this if the program is "protected".
		 */
		final Value statementArray = new Value(Value.ARRAY, null);
		
		if( !p.isProtected())
			for( int i = 0;  i < p.statementCount(); i++ ) {
				Statement st = p.getStatement(i);
				statementArray.addElement(new Value(st.toString()));
			}
		
		result.setElement(statementArray, "LINES");

		return result;
	}

}

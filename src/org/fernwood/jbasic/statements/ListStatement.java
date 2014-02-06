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
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.JBFOutput;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * List a program or an arrayValue. The output is similar, as are the abilities
 * of the running program to index into them.
 * 
 * LIST [ FILE id, ] name
 * 
 * Where 'name' is an arrayValue name or a fully-qualified program name, such as
 * ABOUT or FUNCTION$PAD.
 * 
 * @author tom
 * @version version 1.0 Jan 12, 2006
 * 
 */

class ListStatement extends Statement {

	static int printIndent = 0;
	static String padding = "  ";
	static int labelWidth = 10;
	
	/**
	 * Execute 'list' statement.
	 * 
	 * @param symbols
	 *            The symbol table to use to resolve identifiers, or null if no
	 *            symbols allowed.
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */
	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		int startLine = 0;
		int endLine = 100000;
		ListStatement.printIndent = 0;
		
		final Program program = session.programs.getCurrent();

		if (program == null)
			return new Status(Status.NOPGM);

		if (program.isProtected())
			return new Status(Status.PROTECTED, program.getName());
		/*
		 * See if we're given a range of line(s) to print.
		 */
		if (!tokens.endOfStatement()) {

			if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {
				startLine = program.findLabel(tokens.nextToken());
				if (startLine < 0)
					return new Status(Status.NOSUCHLABEL, tokens.getSpelling());
				startLine = program.getStatement(startLine).lineNumber;
			} else if (tokens.testNextToken(Tokenizer.INTEGER))
				startLine = Integer.parseInt(tokens.nextToken());

			if (tokens.assumeNextToken("-")) {

				if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {
					endLine = program.findLabel(tokens.nextToken());
					if (endLine < 0)
						return new Status(Status.NOSUCHLABEL, tokens
								.getSpelling());
					endLine = program.getStatement(endLine).lineNumber;
				} else if (tokens.testNextToken(Tokenizer.INTEGER))
					endLine = Integer.parseInt(tokens.nextToken());
			} else
				endLine = startLine;
		}

		final JBasicFile outputFile = session.stdout;
		int vectorLength = 0;
		final boolean fDisassemble = symbols.getBoolean("SYS$DISASSEMBLE");
		final boolean fProfile = symbols.getBoolean("SYS$PROFILE");

		Value pv;
		try {
			pv = symbols.reference("SYS$INDENT");
		} catch (JBasicException e) {
			pv = null;
		}
		if( pv != null ) {
			if( pv.getType() == Value.STRING)
				ListStatement.padding = pv.getString();
			else
				ListStatement.padding = padding(pv.getInteger());
		}
		
		try {
			pv = symbols.reference("SYS$LABELWIDTH");
		} catch (JBasicException e) {
			pv = null;
		}
		if( pv != null )
			ListStatement.labelWidth = pv.getInteger();
		
		if( ListStatement.labelWidth < 0 || ListStatement.labelWidth > 30)
			ListStatement.labelWidth = 10;
		
		/*
		 * Only turn this on when debugging line number tokenization
		 * problems with RENUMBER; it prints out the token position
		 * array for each statement that has one during a LIST or 
		 * SHOW PROGRAMS command.  LNA = Line Number Array.
		 */
		final boolean fLineNumberArray = symbols.getBoolean("SYS$LNA");
		
		/*
		 * Make sure a prompt is printed after this statement even if in 
		 * NOPROMPTMODE.
		 */
		this.session.setNeedPrompt(true);
		
		/*
		 * List the selected line(s)
		 */
		
		Program currentProgram = session.programs.getCurrent();
		vectorLength = currentProgram.statementCount();

		for (int i = 0; i < vectorLength; i++) {

			final Statement stmt = currentProgram.getStatement(i);

			if (stmt.lineNumber < startLine)
				continue;
			if (stmt.lineNumber > endLine)
				break;

			statementPrint(session, outputFile, fDisassemble, fProfile, stmt,
					fLineNumberArray);
		}

		return status = new Status(Status.SUCCESS);

	}

	/**
	 * Return a String containing the given number of spaces.
	 * @param integer
	 * @return a String of blanks
	 */
	private String padding(int count) {
		StringBuffer blanks = new StringBuffer("");
		for( int ix = 0; ix < count; ix++ )
			blanks.append(" ");
		return blanks.toString();
	}

	/**
	 * Print out a single statement to an output file. Flags control the format
	 * of the operation which can include bytecode disassembly, profiling data,
	 * and the statement text itself. This is used by the LIST command, but can
	 * also be used elsewhere as a general tool for formatted statement output,
	 * such as in a debugging function.
	 * 
	 * @param session
	 *            The JBasic session hosting the execution context.
	 * @param theOutputFile
	 *            The file to send the formatted text to. This must be a
	 *            JBasicFile which supports indentation, padding, etc. The
	 *            default is to pass session.stdout which is the console.
	 * @param fDisassemble
	 *            If true, the statement is followed by a disassembly of the
	 *            bytecode stream associated with the statement, if there is
	 *            one.
	 * @param fProfile
	 *            If true, the statement is prefixed by a counter of the number
	 *            of times it has been executed.
	 * @param pgmStatement
	 *            The Statement that is to be printed out.
	 */
	static void statementPrint(final JBasic session,
			final JBasicFile theOutputFile, final boolean fDisassemble,
			final boolean fProfile, final Statement pgmStatement,
			final boolean fLineNumberArray) {

		/*
		 * This will blow sky high if we don't get a statement.
		 */
		if( pgmStatement == null )
			return;
		
		/*
		 * If no file has been given, then assume the console output file.
		 */
		final JBFOutput outputFile = (JBFOutput) ((theOutputFile == null) ? session.stdout
				: theOutputFile);

		/*
		 * Calculate the indentation for the text for this line, if any.
		 */
		String indentPad = "";
		if( pgmStatement.indent < 0 )
			ListStatement.printIndent += pgmStatement.indent;

		for( int padx = 0; padx < ListStatement.printIndent; padx++ )
			indentPad = indentPad + ListStatement.padding;
		if( pgmStatement.indent > 0 )
			ListStatement.printIndent += pgmStatement.indent;

		/*
		 * Start to construct the first part of the output, which includes the
		 * statementID or line number.
		 */

		String label = "";
		final int i = pgmStatement.statementID;

		if (pgmStatement.statementLabel != null)
			label = label + pgmStatement.statementLabel + ":";

		while (label.length() < ListStatement.labelWidth)
			label = label + " ";

		/*
		 * Add the indent padding now, so labels all line up but the 
		 * statement is indented appropriately.
		 */
		
		label = label + indentPad;
		
		/*
		 * Begin to accumulate the output text.
		 */
		String outputText = label + pgmStatement.statementText;

		String formattedIndex = null;
		if (pgmStatement.lineNumber > 0)
			formattedIndex = Integer.toString(pgmStatement.lineNumber);
		else
			formattedIndex = "[" + Integer.toString(i) + "]";
		while (formattedIndex.length() < 5)
			formattedIndex = " " + formattedIndex;

		formattedIndex = formattedIndex + " ";
		if (fProfile) {
			String countString = Integer.toString(pgmStatement.executionCount);
			while (countString.length() < 7)
				countString = " " + countString;
			formattedIndex = countString + "#  " + formattedIndex;
		}

		if( fLineNumberArray ) {
			String pxString = "";
			for( int px = 1; px < 5; px++ ) {
				int pos = pgmStatement.getLineNumberPosition(px);
				if( pos > 0 ) {
					if( pxString.length() > 0 )
						pxString = pxString + ", ";
					else
						pxString = "  // **LNA** = [ ";
					pxString = pxString + Integer.toString(pos);
				}
			}
			if( pxString.length() > 0 ) {
				outputText = outputText + pxString + " ]";
			}
		}
		


		outputFile.println(formattedIndex + outputText);

		if (fDisassemble & pgmStatement.hasByteCode())
					pgmStatement.disassemble();	
	}

}
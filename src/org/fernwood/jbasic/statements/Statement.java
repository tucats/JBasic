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

import java.lang.reflect.Method;


import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Loader;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Linker;
import org.fernwood.jbasic.compiler.Optimizer;
import org.fernwood.jbasic.compiler.ReservedWords;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicDebugger;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * Generic statement class. This is the superclass of all actual runtime
 * statements. A statement object has the responsibility for providing either a
 * compile() method or a run() method, but not both. <br>
 * <br>
 * The format of all statements is basically identical: <br>
 * <code>
 * <br>
 * <em>label:</em>  verb <em>arguments  // comment</em>
 *  <br> <br>
 * </code>
 * 
 * The label is optional, and consists of an identifier followed by a colon
 * character. This is used to label the statement so it can be used as the
 * destination of a branch operation (<code>GOTO</code> or <code>GOSUB</code>
 * statements).
 * <p>
 * The first token after the label is the verb, and defines the operation of the
 * statement. All statements must be identifiable by the first token. For
 * statements that have multiple words in the verb (such as
 * <code>LINE INPUT</code>, the verb is LINE and the verb must parse the
 * INPUT token as an argument.
 * <p>
 * There may be any number of tokens following the verb that are part of the
 * statement arguments; these tokens define the details of how the verb is to
 * operate. These arguments are specific to the implementation of each verb; the
 * compilation or execution modules must take care of parsing all tokens from
 * the statement arguments. It is an error for a statement to leave tokens on
 * the statement line unprocessed, except for the optional comment (identified
 * by the comment delimiter "//").
 * <p>
 * Execution of a statement is handled in one of two ways. The compile() method
 * is used to compile the text into bytecode, which can be executed later. The
 * run() method parses the statement and interprets it at runtime. If a
 * compile() method is found for a statement based on it's verb, it is called
 * once and the bytecode stored in the statement object. Subsequent attempts to
 * run the statement will result in running the bytecode if found, else calling
 * the run() method to dynamically interpret the statement.
 * <p>
 * Statements are processed using two basic operations. The store() method is
 * used to place text in a statement object, and defines what the statement is
 * intended to do. The execute() method indicates that the statement is to be
 * executed now. It is an internal choice in the Statement object to determine
 * when compilation occurs (store versus execute) if the compile() method is
 * supported.
 * 
 * @author cole
 * @version 2.0 January 2006
 * 
 */

public class Statement {

	/**
	 * Label string for this statement, if it has one. This is used to transfer
	 * control from one statement to another, via GOTO or GOSUB statements.
	 */
	public String statementLabel;

	/**
	 * Status block reflecting the last execution of this statement.
	 */
	public Status status;

	/**
	 * This is the text of the statement. This can be processed using a
	 * tokenizer object.
	 */
	public String statementText;

	/**
	 * The statement id value (always a positive non-zero integer) for this
	 * statement when it's is a stored program. The statement ID is really a
	 * sequence number that identifies the statement position in a program. The
	 * first statement has a statementID of 1, the second has a statement ID of
	 * 2, etc.
	 */

	public int statementID;

	/**
	 * The line number for the line when created/edited in interactive mode.
	 * This may be zero if line numbers are not being used in this program.
	 */
	public int lineNumber;

	/**
	 * The actual statement object of the specific (correct) class. Null if no
	 * class has been assigned.
	 */
	public Statement statementObject;

	/**
	 * The program that contains this statement, if any.
	 */
	public Program program;

	/**
	 * If this is a declaration statement (PROGRAM, FUNCTION, etc.) then this is
	 * the name of the program.
	 */

	public String declarationName;

	/**
	 * Boolean flag that indicates if the line is "empty"; that is, it contains
	 * only whitespace or a comment. Empty statements are skipped when compiled
	 * or interpreted.
	 */
	public boolean fEmptyStatement;

	/**
	 * Flag set to indicate if the current statement is being executed as part
	 * of a debugger command.
	 */
	public boolean fInDebugger;

	/**
	 * Compiled code (if any) for this statement.
	 */
	public ByteCode byteCode;

	/**
	 * Flag indicating if we are currently compiling this statement.
	 */
	boolean fCompiling;

	/**
	 * Flag indicating if this statement is a program object declaration
	 * statement, such as PROGRAM or VERB.
	 */
	public boolean fDeclaration;
	
	/**
	 * Profiling information that counts the number of times this specific
	 * statement has been executed.
	 */
	public int executionCount;

	/**
	 * Indicator of the indentation contribution of this line to formatted
	 * listings. The normal value is zero, which means no change in the
	 * indentation of lines.  If the value is non-zero, it is added to the
	 * current indentation.  A DO or FOR statement adds one to this value,
	 * a WHILE or NEXT statement subtracts one from this value, etc.
	 */
	public int indent;

	/**
	 * If the statement has a line number reference (such as "GOTO 300") then
	 * this is the list of position(s) of the token in the statement with that 
	 * reference.
	 */
	public int[] lineNumberPositions;

	/**
	 * This is the count of elements in the lineNumberPositions array.
	 */
	public int   lineNumberPosCount;

	/**
	 * Private pointer back to the enclosing JBasic environment object. We use
	 * this pointer to access session-wide data like the program list, etc.
	 */
	protected JBasic session;

	/**
	 * Instance of the debugger that this command runs under. Normally this is
	 * null, unless the statement is a debugger command statement object.
	 */
	public JBasicDebugger debugger;

	/**
	 * Flag indicating if the current statement text contains more than one
	 * statement, separated by the statement separator token, usually ":".
	 * This is also set when this statement contains a subclause such as ELSE
	 * or DO.
	 */
	private boolean fIsCompound;

	/**
	 * Mark this statement as a sub-clause, which means it does not do label
	 * scanning.
	 */
	public void setCompound() {
		fIsCompound = true;
	}


	/**
	 * Constructor to create a new statement object. Must specify a JBasic
	 * session object.
	 * 
	 * @param jb
	 *            The JBasic session object that owns this statement.
	 */
	public Statement(final JBasic jb) {
		session = jb;
		debugger = null;
	}

	/**
	 * A statement created without any session context. This exists because it
	 * is an implicit superclass constructor for sub-classed statement objects.
	 */
	public Statement() {
		session = null;
	}

	/**
	 * Create a statement object for use as a debugger command statement.
	 * 
	 * @param jbenv2
	 *            The JBasic environment that contains the current session.
	 * @param debugger2
	 *            The JBasicDebugger object that owns the current statement.
	 */
	public Statement(final JBasic jbenv2, final JBasicDebugger debugger2) {
		session = jbenv2;
		debugger = debugger2;
	}

	/**
	 * Create a new statement for use as an adjunct to an existing statement.
	 * For example, the FOR ... DO.. clause requires a subordinate statement
	 * in the DO clause.  This is implemented using this constructor, which
	 * copies key elements from the parent statement to the subordinate to 
	 * allow it to compile in the same context.
	 * @param parentStatement The statement that is used to clone the info
	 * needed by this new subordinate statement we are constructing.
	 */
	public Statement(Statement parentStatement) {
		session = parentStatement.session;
		program = parentStatement.program;
		statementID = parentStatement.statementID;
		statementLabel = parentStatement.statementLabel;

	}


	/**
	 * Method that clones a statement from another statement. Used in the
	 * store() operation, among other things.
	 * 
	 * @return A new copy of the current object.
	 */

	public Statement copy() {
		final Statement newStatement = new Statement(session);
		newStatement.byteCode = byteCode;
		newStatement.statementText = statementText;
		newStatement.fCompiling = false;
		newStatement.fDeclaration = fDeclaration;
		newStatement.fEmptyStatement = fEmptyStatement;
		newStatement.lineNumber = lineNumber;
		newStatement.program = program;
		newStatement.statementObject = statementObject;
		newStatement.statementID = statementID;
		newStatement.statementLabel = statementLabel;
		newStatement.declarationName = declarationName;
		newStatement.status = status;
		newStatement.executionCount = executionCount;
		newStatement.copyLinePositions(this, 0);
		newStatement.indent = indent;

		return newStatement;
	}

	/**
	 * Make appropriate mixed-case string of verbs. Given a string, ensure it is
	 * of the form Mixed Case and return the result.
	 * 
	 * @param command
	 *            The name to coerce to verb-normal form.
	 * @return The command name in mixed case as a string.
	 */
	public static String verbForm(final String command) {

		if( command.length() < 2 )
			return command.toUpperCase();
		
		/*
		 * The default translation to verb form is to upper-case the first
		 * character and lower case the rest of the string.
		 */
		String newName = command.substring(0, 1).toUpperCase()
		+ command.substring(1).toLowerCase();

		/*
		 * If the last character is a dollar sign (such as the pseudo-verb MID$)
		 * then change it to a capital X. Java does not approve of class names
		 * with "$" characters in them, and the verb form is often used to
		 * create class names. So Print can become the Java class
		 * PrintStatement, and MID$ can become MidXStatement.
		 */
		final int nameLength = newName.length();
		if (newName.charAt(nameLength - 1) == '$') {
			newName = newName.substring(0, nameLength - 1) + "X";
		}
		return newName;
	}

	/**
	 * Format a statement for output. This method is mostly called from the
	 * Eclipse debugger to format a Statement object, though it is also used in
	 * the ByteCode formatting routine.
	 */
	public String toString() {

		int width = 22;
		
		if( this.session != null ) {
			Value widthValue = this.session.globals().findReference("SYS$LABELWIDTH", false);
			if( widthValue != null )
				width = widthValue.getInteger();
		}
		StringBuffer buffer = new StringBuffer();
		int pad = 0;
		if (lineNumber > 0) {
			String lineNumberString = Integer.toString(lineNumber);
			buffer.append("      ".substring(5-lineNumberString.length()));
			buffer.append(lineNumberString);
			pad = 7;
		}
		
		if (statementLabel != null) {
			buffer.append(' ');
			buffer.append(statementLabel);
			buffer.append(": ");
		}
		if( pad > 0 )
			while (buffer.length()-pad < width) {
			buffer.append(' ');
		}
		if (statementText != null) {
			buffer.append(statementText);
		}
		return buffer.toString();
	}

	/**
	 * Method to store a source line of text in a statement object. The
	 * statement is compiled if possible.
	 * 
	 * @param line
	 *            The line of source text to store in the statement object
	 */

	public void store(final String line) {
		store(line, null);
	}

	/**
	 * Method to store a source line of text and symbol table context for a
	 * statement object. This is used when a statement object is executed from
	 * stored code rather than immediately.
	 * 
	 * @param line
	 *            The line of source text to store in the statement object
	 * @param pgm
	 *            The encapsulating program, or null if not known
	 */
	public void store(final String line, final Program pgm) {

		String buffer = null;

		if( session != null )
			buffer = Utility.resolveMacros(session, line);
		else
			buffer = line;
		
		/*
		 * Do we retokenize?
		 */
		boolean fRetokenize = false;
		if( session != null )
			fRetokenize = session.getBoolean("SYS$RETOKENIZE");
		if (fRetokenize)
			buffer = reTokenize(buffer);

		/*
		 * After optional retokenize operation, process as a normal token buffer.
		 */
		statementText = buffer;
		fEmptyStatement = false;

		final Tokenizer tokens = new Tokenizer(statementText);
		store(session, tokens, pgm);
	}



	/**
	 * Given a line of text in JBasic, use the tokenizer to scan the string, and
	 * reconstruct it in "regular" fashion, so the spacing and capitalization
	 * are uniform.
	 * <p>
	 * <In general, spaces are put between every token. Exceptions to this are:
	 * <p>
	 * <list>
	 * <li>No space is put before a comma or semicolon
	 * <li>No space is put on either side of a period
	 * <li>No space is put between an identifier and an opening parenthesis or
	 * bracket. </list>
	 * <p>
	 * This re-tokenization is done by the Tokenizer object itself.
	 * <p>
	 * Also note that comments indicated by "REM" or the apostrophe are
	 * converted to the double-slash notation automatically. The comment text is
	 * not formatted in any way. You can change the comment substitution string
	 * by setting the undocumented global symbol SYS$COMMENT_TOKEN. However,
	 * only the values "REM" and "//" will yield syntactically valid program
	 * statements.
	 * 
	 * @param line
	 *            The unformatted source line.
	 * @return The formatted source line.
	 */
	private String reTokenize(final String line) {

		String buffer;
		buffer = "";
		final Tokenizer rt = new Tokenizer(line);

		/*
		 * Preemptive test on REM which is a comment. Could start with a line
		 * number or label as well...
		 */
		String remTest = rt.nextToken();
		String lineNo = "";

		if (rt.getType() == Tokenizer.INTEGER) {
			lineNo = remTest;
			remTest = rt.nextToken();
		}

		String commentToken = session.getString("SYS$COMMENT_TOKEN");
		if (commentToken == null) {
			commentToken = "//";
		}

		if ((rt.getType() == Tokenizer.IDENTIFIER) & remTest.equals("REM")) {
			return lineNo + " " + commentToken + " " + rt.getBuffer();
		}

		if ((rt.getType() == Tokenizer.SPECIAL) & remTest.equals("'")) {
			return lineNo + " " + commentToken + " " + rt.getBuffer();
		}

		/*
		 * No mangling of the comment necessary, so restore the token buffer to
		 * be the whole line and let's get serious about this line.
		 */
		rt.loadBuffer(line + "");
		String remainder = rt.getRemainder();
		buffer = rt.reTokenize() + " " + remainder;
		
		return buffer;
	}

	/**
	 * Store the text of a program statement in the current Statement
	 * object.  The text is represented by a tokenizer that contains the
	 * token stream to add.  This causes a compilation to occur.
	 * @param theSession the parent session of the program being
	 * stored to.
	 * @param tokens the token stream for the statement
	 * @param pgm the program the statement is stored in.
	 * @return Status indicating if the compile was successful.
	 */
	public Status store(final JBasic theSession, final Tokenizer tokens,
			final Program pgm) {
		/*
		 * Because we're changing the statement, first kill any specific
		 * instance of a statement we knew about from before. Also, flush any
		 * byteCode information.
		 */

		byteCode = null;
		lineNumber = 0;
		statementObject = null;
		statementID = 0;
		fDeclaration = false;
		statementLabel = null;
		fEmptyStatement = false;
		session = theSession;

		/*
		 * This flag tells if we added a LET which might require
		 * subsequent retokenization of the statement.
		 */
		boolean addedVerb = false;

		SymbolTable global = theSession == null? null : theSession.globals();
		
		status = new Status(Status.SUCCESS);
		boolean hasLineNumber = false;
		
		/*
		 * If we already know the program this goes with, set it now. We'll need
		 * it later for compiling the statement.
		 */
		if (pgm != null) {
			program = pgm;
		}

		statementText = tokens.getBuffer();

		/*
		 * See if there's a line number. If so, save it and permanently strip it
		 * out of the text buffer.
		 */

		if (tokens.testNextToken(Tokenizer.INTEGER)) {
			lineNumber = Integer.parseInt(tokens.nextToken());
			statementText = tokens.getBuffer() + "";
			hasLineNumber = true;
		} else {
			lineNumber = 0;
		}

		/*
		 * If the statement has a line number, it's an attempt to store a 
		 * statement in the program.  See if the program is protected, which
		 * would disallow such behavior.
		 */
		
		if( program != null && program.isProtected() && hasLineNumber )
			return new Status(Status.INVPGMLIN, new Status(Status.PROTECTED));
		
		if( program != null && program.isActive())
			return new Status(Status.INVDBGOP);
		
		/*
		 * A rookie error is to just type in an expression. However, this won't
		 * work (expressions, by themselves, are meaningless without context in
		 * JBasic like in a specific statement). Worse, it will silently tend to
		 * ignore you, because if the expression started with a number then it
		 * will just be stored in program code. Not what the user wanted...
		 * There are lots of expressions that we can't detect, but we sure can
		 * get the silent ones with the number is followed by an operator. So if
		 * at this point, the next token is an operator, then this is the user
		 * typing something like "3+5" on the command line. Let's help the user
		 * out...
		 */
		if (hasLineNumber) {

			final String opLookAhead = tokens.nextToken();
			final String invOperator[] = new String[] { "+", "-", "*", "/", "^" };

			for (String operator : invOperator) {
				if (operator.equals(opLookAhead)) {
					return status = new Status(Status.INVPGMLIN, 
							new Status(Status.UNEXPTOK, opLookAhead ));
				}
			}

			/*
			 * Put back the operator.
			 */
			tokens.restoreToken();

		}

		/*
		 * See if there is a label next.  We don't do this check if we are
		 * a clause in a compound statement since there can't be labels in
		 * a compound statement anyway.
		 */
		if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {

			/*
			 * Read the identifier off the string, and keep it around in case it
			 * turns out to be a label.
			 */
			String label = tokens.nextToken();
			boolean noLabel = ReservedWords.isReserved(label) | fIsCompound;

			/*
			 * See if the next token after the identifier is a ":" indicating a
			 * label. If so, take the label and store it, and then remove it
			 * from the text buffer.
			 */

			if (!noLabel & tokens.assumeNextToken(":")) {
				statementLabel = label;
				statementText = tokens.getBuffer().trim() + "";

				/*
				 * After the label, we might have an assignment. So check ahead
				 * to see if it's another identifier.
				 */

				if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {
					label = tokens.nextToken();
				}
			}

			/*
			 * The next statement could STILL be a LET statement even after
			 * that. So keep looking. See if the second token lets us figure out
			 * if it's an implicit assignment statement. "Label" is the variable
			 * name, though we're probably not going to need it since at this
			 * point it's a straightforward prepend of LET.
			 */
			String item = null;

			/*
			 * Test the next token for all the possible cues that would tell us
			 * this is an assignment statement.
			 */
			if (! isVerb(label) && !label.equalsIgnoreCase("ASM")
					&& !label.equalsIgnoreCase("PRINT")
					&& !label.equalsIgnoreCase("ADD")
					&& !label.equalsIgnoreCase("RETURN")
					&& !label.equalsIgnoreCase("SUBTRACT")
					&& !label.equalsIgnoreCase("DATA")) {
				if (tokens.testNextToken(".")) {
					item = "LET"; // Assume the verb
				} else if (tokens.testNextToken("[")) {
					item = "LET"; // Assume the verb
				} else if (tokens.testNextToken("=")) {
					item = "LET"; // Assume the verb.
				} else if (tokens.testNextToken("<")) {
					item = "LET"; // Assume the verb
				} else if (tokens.testNextToken("|")) {
					item = "LET"; // Assume the verb
				} else if (tokens.testNextToken("/")) {
					item = "LET"; // Assume the verb
				} else if (tokens.testNextToken("<-")) {
					item = "LET";
				} else if (tokens.testNextToken(",")) {
					item = "LET";
				}
			}

			
			/*
			 * IF we've learned that this should have been an assignment with an
			 * implicit LET, then slap the LET on here now. This lets us
			 * dispense with this each time we execute the statement.
			 * 
			 * Adjust the way we add it on so formatting works properly.
			 */
				
			if (item != null) {
				addedVerb = true;
				boolean fRetokenize = false;
				if( global != null )
					fRetokenize = global.getBoolean("SYS$RETOKENIZE");
				if (fRetokenize) {
					if (statementText.startsWith(" ")) {
						statementText = " " + item + statementText;
					} else {
						statementText = " " + item + " " + statementText;
					}
				} else {
					statementText = item + " " + statementText;
				}
			}

		}

		/*
		 * If (after removing the optional label) the statement is empty, then
		 * mark it as such. We have to reload the tokenizer at this point
		 * because of the label vs. LET issues above.
		 */
		statementText = statementText.trim();
		tokens.loadBuffer(statementText);
		if (tokens.testNextToken(Tokenizer.END_OF_STRING)) {
			fEmptyStatement = true;
		}

		/*
		 * Is there an ALIAS for this we need to use instead?
		 */

		Value aliasRecord = null;
		if( global != null )
			try {
				aliasRecord = global.reference("SYS$ALIASES");
			} catch (JBasicException e1) {
				
			}
		
		if (aliasRecord != null) {
			if (aliasRecord.isType(Value.RECORD)) {
				final String verb = tokens.nextToken().toUpperCase();
				final Value sub = aliasRecord.getElement(verb);
				if (sub != null) {
					tokens.loadBuffer(sub.getString() + " "
							+ tokens.getBuffer());
				} else {
					tokens.restoreToken();
				}
			}
		}

		/*
		 * Last step - we've set everything up so let's see if we can compile
		 * the statement. If there's a line number, we must be storing this in
		 * the current program, so force the program setting to allow for
		 * correct compilation behavior. Also set the statement ID, which will
		 * be needed for compilation purposes.
		 * 
		 * While we're here, check to see if the program is protected, which
		 * would disallow such behavior.
		 */
	

		if (hasLineNumber) {
			if( theSession != null )
				program = theSession.programs.getCurrent();
		}

		if( program != null && program.isProtected() && hasLineNumber )
			return new Status(Status.PROTECTED);

		if( addedVerb && global != null && global.getBoolean("SYS$RETOKENIZE")) {
			tokens.reTokenize();
			statementText = tokens.getBuffer();
		}

		status = compile(tokens);

	
		/*
		 * If we're the first chance to print this message, do so now. The test
		 * to see if it has been printed handles statement compilers that call
		 * other statement compilers, such as IF..THEN..ELSE, to prevent the
		 * same message printing twice - once by the THEN sub-statement and then
		 * again by the IF statement.
		 */
		if (theSession != null && status.failed() && !status.printed()) {
			final String txt = new Status(Status.INSTATEMENT).getMessage(theSession);
			theSession.stdout.println(txt);
			theSession.stdout.println("  " + tokens.errorBuffer());
			theSession.stdout.println("  " + tokens.errorPointer());
			status.printError(theSession);
		}
		if (theSession != null && byteCode != null) {
			theSession.statementsCompiled++;
		}

		/*
		 * If we were given a line number, then this is really a statement meant
		 * to be stored in the current program. So do so now, creating a copy of
		 * the statement so it can be stored away without interfering with
		 * "this" which is really owned by the JBasic main loop in many cases.
		 * 
		 * Note that we used to discard statements that had errors.  As of this
		 * version (2.7) I'm allowing the text to be stored anyway, since it
		 * may be useful for the ad hoc programmer to see the "wrong" text as
		 * entered before correcting it. This change is paired with improved
		 * error messaging at link time, so if the erroneous statement is 
		 * still present at link time, a helpful error describing what is 
		 * (still) wrong with the statement is printed.
		 */
		
		if( status.failed() && pgm != null)
			status.setWhere(pgm.getName(), this.lineNumber);
		
		if ( /* status.success() & */ (lineNumber > 0)) {

			if( theSession == null ) {
				return status = new Status(Status.NOACTIVEPGM);
			}
			
			Program currentProgram = theSession.programs.getCurrent();
			if (currentProgram == null) {
				final Statement newStmt = new Statement(theSession);
				newStmt.execute("new " + Integer.toString(lineNumber), global);
				currentProgram = theSession.programs.getCurrent();
			}

			final Statement newStatement = copy();

			newStatement.program = currentProgram;
			try {
				currentProgram.insertStatement(lineNumber, newStatement);
				status = new Status(Status.STMTADDED);	
			} catch (JBasicException e) {
				status = e.getStatus();
			}

			if (currentProgram.hasExecutable()) {
				Linker.unlink(currentProgram);
			}

			currentProgram.initDataElements();
		}
		return status;
	}

	private boolean isVerb(String label) {

		String programName = label.toUpperCase();
		
		if( ReservedWords.isVerb(label))
			return true;
		
		if( session == null || session.programs == null )
			return false;
		
		if( session.programs.find(JBasic.VERB + programName) != null )
			return true;
		

		return false;
	}


	/**
	 * Execute the statement in a string buffer. The label (if any) is skipped
	 * and the next token is assumed to be the verb. The verb name is used to
	 * locate a class name in the Statement subclass, and invokes it's run
	 * method to execute the statement itself.
	 * 
	 * @param line
	 *            The string buffer containing the text to execute
	 * @param symbols
	 *            The symbol table used for all symbol resolution
	 * @param fDebug
	 *            Flag indicating if debugging messages are printed
	 * @return A status object reflecting the success of the statement
	 */

	Status execute(final String line, final SymbolTable symbols,
			final boolean fDebug) {
		statementText = line + "";
		return execute(new Tokenizer(line, JBasic.compoundStatementSeparator), symbols, fDebug);
	}

	/**
	 * Execute the statement last stored in this object. The line of text first
	 * stored away is executed, using the symbol table set at the same time. A
	 * fDebugExpressions flag can be passed in via this interface.
	 * 
	 * @param symbols
	 *            The symbol table used to handle name binding when executing
	 *            the statement.
	 * @param debugFlag
	 *            True if debugging messages about the dispatching and execution
	 *            of the statement are to be directed to the console.
	 * @return A status object reflecting the success of the statement.
	 */
	public Status execute(final SymbolTable symbols, final boolean debugFlag) {
		return execute(new Tokenizer(statementText), symbols,
				debugFlag);
	}

	/**
	 * Execute the statement in a string buffer. The label (if any) is skipped
	 * and the next token is assumed to be the verb. The verb name is used to
	 * locate a class name in the Statement subclass, and invokes it's run
	 * method to execute the statement itself. No debugging messages are issued.
	 * 
	 * @param line
	 *            The string buffer containing the text to execute
	 * @param symbols
	 *            The symbol table used for all symbol resolution
	 * @return A status object reflecting the success of the statement
	 */

	Status execute(final String line, final SymbolTable symbols) {
		statementText = line + "";
		return execute(new Tokenizer(line), symbols, false);
	}

	/**
	 * Execute the statement in a token buffer. The label (if any) is skipped
	 * and the next token is assumed to be the verb. The verb name is used to
	 * locate a class name in the Statement subclass, and invokes it's run
	 * method to execute the statement itself. No debugging messages are issued.
	 * 
	 * @param tokens
	 *            The token buffer object representing the text to execute
	 * @param symbols
	 *            The symbol table used for all symbol resolution
	 * @return A status object reflecting the success of the statement
	 */

	Status execute(final Tokenizer tokens, final SymbolTable symbols) {
		statementText = tokens.getBuffer() + "";
		return execute(tokens, symbols, false);
	}

	/**
	 * Execute the statement in a token buffer, with explicit specification of
	 * the debug flag setting. <br>
	 * The label (if any) is skipped and the next token is assumed to be the
	 * verb. The verb name is used to locate a class name in the Statement
	 * subclass, and invokes it's run method to execute the statement itself.
	 * 
	 * @param tokens
	 *            The token buffer containing the text to execute
	 * @param symbols
	 *            The symbol table used for all symbol resolution
	 * @param debug_flag
	 *            A flag indicating if debugging messages are issued.
	 * @return A status object reflecting the success of the statement
	 */

	Status execute(final Tokenizer tokens, final SymbolTable symbols,
			final boolean debug_flag) {

		executionCount++;
		String command = null;
		
		/* Do we allow non-compiled statements to have compound form? No! */
		final boolean allowCompoundExecutes = false;

		if (byteCode != null) {
			if (byteCode.statement != null) {
				byteCode.statement.statementID = statementID;
			}
		}

		/*
		 * If the (expensive) debug flag is set, let's output a line describing
		 * the nature of the statement being executed.
		 */
		if (debug_flag) {
			printTrace(tokens, symbols);
		}

		/*
		 * Check for malformed immediate-mode statement with a label. If there
		 * is a label but no active program, then the label will be ignored -
		 * this is possibly indicative of an error on the part of the user, so
		 * complain about it.
		 */

		if ((statementLabel != null) & !isActive()) {
			return new Status(Status.INVLABEL, statementLabel);
		}

		/*
		 * If this is an empty statement, then we're done.
		 */

		if (fEmptyStatement) {
			return new Status(Status.SUCCESS);
		}

		/*
		 * If there is compiled bytecode for this statement, execute it. This is
		 * the most typical execution path, since the previously executed
		 * store() method will have compiled the code if it could.
		 */

		if (byteCode != null) {
			session.statementsByteCodeExecuted++;
			byteCode.debugger = debugger;
			return byteCode.run(symbols, 0);
		}

		/*
		 * Not compilable, so we're going to have to do this brute force.
		 * 
		 * Get the first token, which defines the verb. Even if we've previously
		 * decoded this line, we have to skip past the verb so the statement's
		 * token scanner can pick up the arguments to the verb.
		 */

		command = tokens.nextToken().trim();

		/*
		 * If at this point there's still no text, then we had an empty
		 * statement. @NOTE I don't think this should happen, as the compiler
		 * would have detected the empty statement already.
		 */

		if (command.length() == 0) {
			return status = new Status(Status.SUCCESS);
		}

		/*
		 * Use the verb dispatcher to locate a run() method and call it.
		 */
		status = invokeVerb(verbForm(command), tokens, symbols);

		/*
		 * Let's see what went wrong, if anything...
		 */
		if (status.failed()) {

			/*
			 * Since there was a non-success result, store it in the appropriate
			 * global variable. That's all we need to do in case of error, the
			 * code that invoked the statement will decide what to do about
			 * printing the error text.
			 */

			try {
				session.globals().insert("SYS$STATUS", new Value(status));
			} catch (JBasicException e) {
				return e.getStatus();
			}

			return status;
		}

		/*
		 * If the interpretive run() method consumed all the tokens, then we
		 * just percolate the return code (usually success) from the statement
		 * handler.
		 */
		if (tokens.testNextToken(Tokenizer.END_OF_STRING)) {
			return status;
		}

		/*
		 * Was there a compound statement here? If there's a colon we try to
		 * process the remainder of the statement text as a new statement.
		 */

		if (tokens.assumeNextSpecial(JBasic.compoundStatementSeparator)) {
			if (allowCompoundExecutes) {
				final Statement compoundStatement = new Statement(session);
				compoundStatement.fIsCompound = true;
				compoundStatement.fEmptyStatement = false;
				compoundStatement.statementID = statementID;
				compoundStatement.statementLabel = statementLabel;
				compoundStatement.store(session, tokens, program);
				if (compoundStatement.status.failed()) {
					return compoundStatement.status;
				}
				compoundStatement.execute(symbols, debug_flag);
				status = compoundStatement.status;
			} else {
				return status = new Status(Status.NOCOMPOUND, command);
			}
		}
		/*
		 * ...but if there is text left over that wasn't processed, then this is
		 * considered a syntax error and we complain about unused tokens. This
		 * allows each statement handler to handle "normal" syntax and terminate
		 * naturally; we'll catch dangling tokens here.
		 */
		return status = new Status(Status.EXTRA, tokens.getBuffer().trim());

	}

	/**
	 * Format and print a trace message for the execution of this statement.
	 * This is only done when SYS$TRACE_STATEMENTS is set to true, and is an
	 * expensive operation so shouldn't be done unless the output is necessary.
	 * 
	 * @param tokens
	 *            The current tokenizer buffer being processed for execution.
	 * @param symbols
	 *            The symbol table being used to execute this statement - this
	 *            is needed to resolve additional debug switch flags.
	 */
	private void printTrace(final Tokenizer tokens, final SymbolTable symbols) {
		String label = "<console>";

		if (program != null) {
			label = program.getName() + ";" + Integer.toString(statementID);
		}

		if (fEmptyStatement) {
			label = label + "~";
		}
		if (byteCode != null) {
			label = label + "#";
		}

		while (label.length() < 18) {
			label = label + " ";
		}

		if (statementLabel != null) {
			label = label + " " + statementLabel + ":";
		}

		while (label.length() < 32) {
			label = label + " ";
		}

		session.stdout.print("Trace " + label);
		session.stdout.println(tokens.getBuffer());
		if (symbols.getBoolean("SYS$DISASSEMBLE") & (byteCode != null)) {
			byteCode.disassemble();
		}
	}

	/**
	 * Superclass expression of the run method. This should never be called, and
	 * if it is called, it means someone invoked the method on a statement that
	 * had not been dispatch-encoded... that is, no one had decoded the
	 * statement to determine what the correct Statement subclass is for this
	 * statement.
	 * 
	 * @param tokens
	 *            The token buffer to run
	 * @param symbols
	 *            Symbol table used to execute statements
	 * @return Always returns the FAULT status flag.
	 */
	public Status run(final Tokenizer tokens, final SymbolTable symbols) {
		return new Status(Status.FAULT,
				"Attempt to dispatch undecoded statement " + statementText);
	}

	/**
	 * Invoke the method for a given verb. This takes a string name for a verb
	 * and locates and invokes the method for it. This assumes that all name
	 * manipulations are complete and the token buffer contains only the
	 * remaining tokens for this statement.
	 * 
	 * @param theVerb
	 *            A string representation of the verb to execute, such as
	 *            "Print" or "Return".
	 * @param tokens
	 *            The tokenizer stream that will be passed to the verb handler
	 *            for compilation or interpretation.
	 * @param symbols
	 *            The currently active local symbol table.
	 * @return Status of executed verb method
	 */
	Status invokeVerb(final String theVerb, final Tokenizer tokens,
			final SymbolTable symbols) {

		final String verb = theVerb;

		session.statementsInterpreted++;

		/*
		 * Do we already know what the statement type of this is? If so, then
		 * perhaps we can just run it directly?
		 */

		if (statementObject != null) {
			return statementObject.run(tokens, symbols);
		}

		/*
		 * We might have contributing packages that all have an interest in
		 * adding verbs. So we first try our own, but if that doesn't work out,
		 * then we try the list of contributed packages.
		 */
		String aClass = "org.fernwood.jbasic.statements." + verb + "Statement";
		Class c = null;
		try {
			c = Class.forName(aClass);
		} catch (final Exception e) {
			c = null;
		}

		boolean failed = (c == null);

		if (failed) {

			Value packageList;
			try {
				packageList = symbols.reference(JBasic.PACKAGES);
			} catch (JBasicException e1) {
				packageList = null;
			}

			if (packageList != null) {

				int n = 0;

				/*
				 * It should never be possible for SYS$PACKAGES to be other than
				 * an array, but just in case someone screwed with the system
				 * variables, let's trap for that case here.
				 */
				if (packageList.isType(Value.ARRAY)) {
					n = packageList.size();
				}

				/*
				 * Scan over each member of the SYS$PACKAGES array, and try out
				 * those package names to locate a workable statement class.
				 */
				for (int i = 1; i <= n; i++) {
					final String item = packageList.getString(i);

					aClass = item + "." + verb + "Statement";
					try {
						c = Class.forName(aClass);
						JBasic.log.debug("Foreign package invocation of " + aClass);
						failed = false;
						break;
					} catch (final Exception e) {
						c = null;
					}

				}
			}

		}

		/*
		 * At this point we found a class, then let's try to see if we can
		 * invoke it via reflection mechanisms. Class c identifies what we are
		 * trying to invoke.
		 */

		if (!failed) {
			/*
			 * Do the "reflection" thing and construct an invocation of the
			 * class
			 */

			//final String aMethod = "run";

			try {
				// Class c = Class.forName(aClass);
				//final Method m = c.getDeclaredMethod(aMethod, new Class[] {
				//		Tokenizer.class, SymbolTable.class });
				statementObject =  (Statement) c.newInstance();
				
				/*
				 * Because the dynamic execution requires the creation of a new
				 * object to invoke, we've got to put a few data items in the
				 * statement that it wouldn't normally have, copied from the
				 * actual statement object we're running.
				 */

				statementObject.program = program;
				statementObject.statementID = statementID;
				statementObject.statementText = statementText;
				statementObject.statementLabel = statementLabel;
				statementObject.fEmptyStatement = fEmptyStatement;
				statementObject.session = session;
				statementObject.debugger = debugger;
				statementObject.fInDebugger = fInDebugger;

				try {
					final Status r = statementObject.run(tokens, symbols);
					return r;
				} catch (final Exception e) {
					return new Status(Status.FAULT, verb + ": " + e.toString());
				}
			} catch (final Exception e) {
				failed = true;
			}
		}

		/*
		 * If the invoke failed because we couldn't find a working class to
		 * support the statement, then let's try to see if there is a program
		 * out there that will handle the statement instead.
		 */
		if (failed) {

			if (fInDebugger) {
				debugger = null;
			}

			Program p = session.programs.find(JBasic.VERB + verb.toUpperCase());
			boolean didLoad = false;
			if (p == null) {

				/*
				 * See if a demand load will find a file with the program we
				 * need. First, save the pointer to the current Program object
				 * and it's name because the load could destroy them.
				 */
				final SymbolTable g = session.globals();
				final Program savedProgram = session.programs.getCurrent();
				String savedProgramName = g.getString("SYS$CURRENT_PROGRAM");

				/*
				 * Try to load a file with the verb name from all the well-known
				 * path search locations.
				 */
				try {
					session.addEvent("$Attempting demand load of verb " + verb.toUpperCase());
					Loader.pathLoad(session, verb);
					String upperVerb = verb.toUpperCase();
					p = session.programs.find(JBasic.VERB + upperVerb);
					if( p != null ) {
						session.addEvent("$Successful demand load completed");
						JBasic.log.debug("Demand load for verb " + upperVerb );
						didLoad = true;
					}
				} catch (JBasicException e) {
					p = null;
					session.addEvent("$No successful demand load");
				}

				/*
				 * The load could have messed with current program definitions,
				 * so set them back the way they were when we started this.
				 */
				session.setCurrentProgramName(savedProgramName);
				session.programs.setCurrent(savedProgram);

			}

			/*
			 * Cool, we found a problem object named "VERB$verb" where "verb" is
			 * the verb we're trying to execute. An example is VERB$HELP when
			 * the verb was "Help".
			 */
			if (p != null) {

				String string = "Local to autocall " + verb;
				/*
				 * The call frame will need it's own symbol table. Give it a
				 * name that tells us it's a verb (the "autocall" prefix) for
				 * debugging support.
				 */
				final SymbolTable local = new SymbolTable(session,
						string, symbols);

				/*
				 * Give the verb some information about what it is being asked
				 * to do. It gets an array $ARGS[] that contains each token of
				 * the rest of the statement, parsed using standard JBasic
				 * tokenization rules. And just in case the verb needs to have
				 * it's own parsing conventions, it gets a text copy of the
				 * original text of the line that follows the verb.
				 */

				try {
					local.insertLocal("$COMMAND_LINE",
						new Value(tokens.getBuffer()));
				} catch(JBasicException e ) { /* do nothing */ }
				
				final Value argArray = new Value(Value.ARRAY, "$ARGS");

				int argcount = 0;
				while (true) {
					String tok = tokens.nextToken();
					if (tokens.getType() == Tokenizer.END_OF_STRING) {
						break;
					}
					argcount++;
					if (tokens.getType() != Tokenizer.STRING) {
						tok = tok.toUpperCase();
					}

					argArray.setElement(new Value(tok), argcount);

				}

				/*
				 * All invoked programs have three symbols that tell them about
				 * themselves. $THIS contains the name of the invoked program
				 * itself. $MODE describes how the program came to be run, it
				 * supports values like "VERB", "RUN", "CALL", "FUNCTION", and
				 * "METHOD". Also indicate what program was running that caused
				 * it to be invoked, or "Console" if it was a verb entered at
				 * the command line.
				 */

				try {
					local.insertLocal("$ARGS", argArray);
					local.insertLocal("$THIS", new Value(verb.toUpperCase()));
					local.insertLocal("$MODE", new Value("VERB"));

					if (session.programs.getCurrent() != null) {
						local.insertLocal("$PARENT", new Value(
								session.programs.getCurrent().getName()));
					} else {
						local.insertLocal("$PARENT", new Value("Console"));
					}
				} catch(JBasicException e ) { /* do nothing */ }

				/*
				 * Do we use the debugger in this case? If we were given a
				 * debugger to use pass it down by default. However, if this
				 * command it itself executed by the debugger, don't recursively
				 * debug the verb, etc.
				 */
				JBasicDebugger dbg = debugger;

				if (fInDebugger) {
					dbg = null;
				}

				/*
				 * Run the program. We temporarily save the current program, and
				 * run the new one. When we're done, make the previous program
				 * current, and return the status from the called program's
				 * execution of the verb.
				 */

				tokens.loadBuffer("");
				final Program savedProgram = session.programs.getCurrent();
				session.programs.setCurrent(p);
				if( didLoad ) {
					p.link(false);
					session.addEvent("$Link of verb " + verb + " completed");
				}
				status = p.run(local, 0, dbg);
				session.programs.setCurrent(savedProgram);
				return status;
			}
		}

		/*
		 * Everything failed to find a sponsor for this verb (no built-in
		 * method, contributed package, or program verb). So signal an error.
		 */
		return new Status(Status.VERB, verb);
	}

	/**
	 * Invoke the compilation method for a verb. This takes a string name for a
	 * verb and locates and invokes the method for it. This assumes that all
	 * name manipulations are complete and the token buffer contains only the
	 * remaining tokens for this statement.
	 * 
	 * @param tokens
	 *            The token stream containing the text to compile. The leading
	 *            token (the Verb) has already been removed, so this token
	 *            stream contains the remainder of the statement to be used in
	 *            the compilation.
	 * 
	 * @return Status of compilation. An error usually indicates a syntax error
	 *         in the user-provided source represented by the tokenizer stream.
	 *         IF an error is returned the executable ByteCode associated with
	 *         the statement is invalid.
	 * @see ByteCode
	 */
	@SuppressWarnings("unchecked") 
	public Status compile(final Tokenizer tokens) {

		/*
		 * If we already know this is an empty statement, there is no
		 * compilation to do.
		 */
		status = new Status(Status.SUCCESS);
		fDeclaration = false;
		indent = 0;

		if (fEmptyStatement) {
			return status;
		}

		/*
		 * If at this point we cannot find anything to parse, we're done.
		 */
		if (tokens.testNextToken(Tokenizer.END_OF_STRING)) {
			fEmptyStatement = true;
			return new Status(Status.SUCCESS);
		}

		final String fullText = tokens.getBuffer() + "";
		String verb = tokens.nextToken();
		if( tokens.getType() != Tokenizer.IDENTIFIER)
			return new Status(Status.VERB, verb);
		
		if (verb == null) {
			fEmptyStatement = true;
			return new Status(Status.SUCCESS);
		}

		verb = Statement.verbForm(verb);

		/*
		 * If the verb is a program declaration verb, then set the flag so we
		 * know this statement is special. Capture the token name, even though
		 * we have to put it back for successful parsing.
		 */

		if (verb.equalsIgnoreCase("FUNCTION") | verb.equalsIgnoreCase("TEST")
				| verb.equalsIgnoreCase("PROGRAM")
				| verb.equalsIgnoreCase("VERB")) {
			declarationName = tokens.nextToken() + "";

			if (verb.equalsIgnoreCase("FUNCTION")) {
				declarationName = JBasic.FUNCTION + declarationName;
			} else if (verb.equalsIgnoreCase("TEST")) {
				declarationName = JBasic.TEST + declarationName;
			} else if (verb.equalsIgnoreCase("VERB")) {
				declarationName = JBasic.VERB + declarationName;
			}

			tokens.restoreToken();
			fDeclaration = true;
		}

		Class c = findStatementClass(session, verb);		
		boolean failed = (c == null);
		
		if (!failed) {

			/*
			 * Do the "reflection" thing and construct an invocation of the
			 * class
			 */

			final String aMethod = "compile";
			boolean fOptimize = true;
			if( session != null )
				fOptimize = session.getBoolean("SYS$OPTIMIZE");
			
			try {
				// Class c = Class.forName(aClass);
				Method m = null;
				if( c != null ) {
					m = c.getDeclaredMethod(aMethod,
						new Class[] { Tokenizer.class });
					statementObject = (Statement)  c.newInstance();
				}

				/*
				 * Because the dynamic execution requires the creation of a new
				 * object to invoke, we've got to put a few data items in the
				 * statement that it wouldn't normally have, copied from the
				 * actual statement object we're running.
				 */

				statementObject.program = program;
				statementObject.statementID = statementID;
				statementObject.statementText = statementText;
				statementObject.statementLabel = statementLabel;
				statementObject.fEmptyStatement = fEmptyStatement;
				statementObject.lineNumber = lineNumber;
				statementObject.fInDebugger = fInDebugger;
				statementObject.indent = indent;

				statementObject.session = session;
				

				try {
					Status r = null;
					if( m != null )
						r = (Status) m.invoke( statementObject, new Object[] { tokens });

					if (r == null) {
						r = new Status("EXPRESSION");
					}
					/*
					 * The compilation is done in a temporary statement i, so if
					 * it was successful we need to fetch the generated code and
					 * store it in the persistent statement object.
					 */
					if (r.success() &&!r.equals(Status.NOCOMPILE)) {
						byteCode = statementObject.byteCode;
						byteCode.end();
						indent += statementObject.indent;

						/*
						 * We also need a copy of the token position of any line
						 * number found in the statement.
						 */

						copyLinePositions(statementObject, 0);

						/*
						 * See if there is a compound statement at work here.
						 */

						if (tokens.assumeNextSpecial(JBasic.compoundStatementSeparator)) {
							Statement compoundStatement = new Statement(session);
							compoundStatement.fIsCompound = true;
							compoundStatement.fEmptyStatement = false;
							compoundStatement.statementID = statementID;
							compoundStatement.statementLabel = statementLabel;
							compoundStatement.indent = indent;
							final int baseTokenPos = tokens.getPosition();
							compoundStatement.store(session, tokens, program);
							if (compoundStatement.status.failed()) {
								return compoundStatement.status;
							}
							indent += compoundStatement.indent;

							this.byteCode.concat(compoundStatement.byteCode);
							this.copyLinePositions(compoundStatement, baseTokenPos);
						}
						/*
						 * See if there is unclaimed text at the end of the
						 * string... if so, it's an error. The exception is when
						 * ELSE is next and it's allowed.
						 */
						boolean extraText = !tokens.testNextToken(Tokenizer.END_OF_STRING);
						if (tokens.fReserveElse & tokens.testNextToken("ELSE")) {
							extraText = false;
						}

						if (extraText) {
							r = new Status(Status.EXTRA, tokens.getBuffer().trim());
							byteCode = null;
						} 
						else if (fOptimize) {
							final Optimizer opt = new Optimizer();
							opt.optimize(byteCode);
						}
					} else {
						byteCode = null; /* Make sure no leftovers found */
					}
					
					if( fDeclaration & r.getCode().equals(Status.NOCOMPILE))
						return new Status(Status.SYNTAX, new Status(Status.INVPGM));
					return this.status = r;
				} catch (final Exception e) {
					return status = new Status(Status.FAULT, verb + ": "
							+ e.toString());
				}
			} catch (final Exception e) {
				failed = true;
			}
		}

		/*
		 * It can't be natively compiled, so to make sure we run all ByteCode
		 * when possible, generate an _EXEC function
		 */

		byteCode = new ByteCode(session, this);
		byteCode.add(ByteCode._STRING, fullText);
		byteCode.add(ByteCode._EXEC);
		byteCode.add(ByteCode._END);

		return status = new Status(Status.NOCOMPILE, verb);
	}


	/**
	 * Given a verb name, find the class that implements the statement.  This
	 * first checks for built-in statements, and then searches any added
	 * packages that might contain statements.
	 * @param session The session we search for the class
	 * @param theVerb the name of the verb to search for.
	 * @return null if there is no class to implement the statement, else a
	 * Java Class object for the given statement handler.
	 */
	public static Class findStatementClass(JBasic session, String theVerb) {
		/*
		 * We might have contributing packages that all have an interest in
		 * adding verbs. So we first try our own, but if that doesn't work out,
		 * then we try the list of contributed packages.
		 */
		
		String verb = theVerb;
		
		String aClass = "org.fernwood.jbasic.statements." + verb + "Statement";
		Class c = null;
		try {
			c = Class.forName(aClass);
		} catch (final Exception e) {
			c = null;
		}

		if (c == null) {

			Value packageList;
			try {
				packageList = session.globals().reference(JBasic.PACKAGES);
			} catch (JBasicException e1) {
				packageList = null;
			}

			if (packageList != null) {

				int n = 0;

				if (packageList.getType() == Value.ARRAY) {
					n = packageList.size();
				}

				for (int i = 1; i <= n; i++) {
					final String item = packageList.getString(i);

					aClass = item + "." + verb + "Statement";
					try {
						c = Class.forName(aClass);
						break;
					} catch (final Exception e) {
						c = null;
					}
				}
			}
		}
		return c;
	}


	/**
	 * Determine if the statement being executed is part of an active program
	 * execution context or not. This is used by statements like DO and FOR
	 * which require a running context to store information; they cannot be
	 * issued as direct commands. This is mostly used during the compilation
	 * phase...
	 * 
	 * @return true if the current statement is being executed by an active
	 *         program. False if there is no active program, or the program is
	 *         not currently running.
	 */
	public boolean isActive() {
		if (program == null) {
			return false;
		}
		return program.isActive();
	}

	/**
	 * @return the current active Debugger object, or null if there isn't one.
	 */
	public JBasicDebugger findDebugger() {

		if (debugger != null) {
			return debugger;
		}

		if (byteCode != null) {
			if (byteCode.debugger != null) {
				return byteCode.debugger;
			}
		}

		if (program != null) {
			ByteCode linkedCode = program.getExecutable();
			if( linkedCode == null)
				return null;
			
			final JBasicDebugger d = linkedCode.debugger;
			if (d != null) {
				return d;
			}
		}
		return null;
	}

	/**
	 * Set the special "in debugger" flag, which is used to prevent some ugly
	 * recursion issues with commands in the debugger itself.
	 * 
	 * @param b true if we are in a debugg mode.
	 * @return previous state of the flag
	 */
	public boolean inDebugger(final boolean b) {
		final boolean last = fInDebugger;
		fInDebugger = b;
		return last;
	}

	/**
	 * Indicate if the program that contains this statement is expecting strong
	 * typing. If there is no program, then use the global state.
	 * 
	 * @return true if strong typing is to be used for LVALUEs.
	 */
	public boolean strongTyping() {
		boolean fStrongTyping = false;
		if (program != null) {
			fStrongTyping = program.fStaticTyping;
		} else {
			if( session != null )
				fStrongTyping = session.getBoolean("SYS$STATIC_TYPES");
		}
		return fStrongTyping;
	}

	/**
	 * Return the token position of the nth line number reference in the
	 * statement.
	 * @param pos the 1-based position in the line number array that is
	 * to be returned.  
	 * @return the token position, or zero if there is no such line number
	 * position in the array.
	 */
	public int getLineNumberPosition(final int pos) {
		if( lineNumberPositions == null) {
			return 0;
		}
		if( lineNumberPosCount < pos ) {
			return 0;
		}
		return lineNumberPositions[pos-1];

	}

	/**
	 * Offset line number positions in an existing statement by a constant
	 * amount.  This is used in the IF..THEN..ELSE processor to handle
	 * line numbers in compound statements.
	 * @param offset the number of positions to shift the linenumber data.
	 */
	public void offsetLineNumberPositions(final int offset ) {
		if( lineNumberPositions == null ) {
			return;
		}
		for( int ix = 0; ix < lineNumberPosCount; ix++ ) {
			lineNumberPositions[ix] += offset;
		}
		return;
	}
	/**
	 * Copy the line number token positions from one statement to another.
	 * @param statement the source statement whose line number token position
	 * array is copied to the current statement object.
	 * @param baseTokenPos The base position of that the inbound tokens should
	 * be offset by.  This is zero for an operation that just copies the token
	 * array from one statement to another. However, when processing compound
	 * statements the array must be offset by the position in the primary 
	 * statement token buffer.
	 */
	protected void copyLinePositions(final Statement statement, final int baseTokenPos) {

		if( lineNumberPositions == null ) {
			lineNumberPositions = new int[JBasic.LINENUMBERSPERSTATEMENT];
			lineNumberPosCount = 0;
		}
		if( baseTokenPos == 0 ) {
			lineNumberPosCount = 0;
		}

		for( int ix = 0; ix < statement.lineNumberPosCount; ix++ ) {
			try {
				addLineNumberPosition(statement.lineNumberPositions[ix] + baseTokenPos);
			} catch (final JBasicException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add a line number token position to the list of line number tokens in
	 * this statement.  This is used by the renumber operation to determine
	 * where renumbered line numbers must be updated in statement text.
	 * @param position the line number position to add to the list for
	 * this statement.
	 * @throws JBasicException if too many line number references exist on
	 * one line (typically caused by a very complex if-then-else on a single
	 * line that contains more than 10 line number references).
	 */
	public void addLineNumberPosition( final int position ) throws JBasicException {

		if( lineNumberPosCount > JBasic.LINENUMBERSPERSTATEMENT ) {
			throw new JBasicException(Status.FAULT, "too many line number references");
		}
		if( lineNumberPositions == null ) {
			lineNumberPositions = new int[JBasic.LINENUMBERSPERSTATEMENT];
			lineNumberPosCount = 0;
		}
		lineNumberPositions[lineNumberPosCount++] = position;
	}

	/**
	 * Return count of line number tokens in  this statement.
	 * @return integer count of tokens in this statement that represent
	 * line numbers, which would need updating if there was a RENUMBER
	 * operation.  Returns zero if there are no tokens.
	 */
	public int lineNumberTokenCount() {
		if( lineNumberPositions == null) {
			return 0;
		}
		return lineNumberPosCount;
	}

	/**
	 * Does the current statement have any associated generated code to go
	 * with it?
	 * @return True if there is generated code for this statement, else false.
	 */
	boolean hasByteCode() {
		if( this.byteCode == null )
			return false;
		if( this.byteCode.size() == 0 )
			return false;
		return true;
	}

	/**
	 * Disassemble the generated code for the current statement, if any.
	 */
	void disassemble() {
		if( hasByteCode())
			byteCode.disassemble();
	}


	/**
	 * Clear the text of the current statement.  This is used by the
	 * Program.strip() method to remove the text of the first statement,
	 * which is the only statement left in a protected program.
	 */
	public void clearText() {
		this.statementText = "<protected>";
	}

	/**
	 * Generate the code to handle an argument list.  This can be used from either
	 * a FUNCTION or PROGRAM statement.
	 * @param tokens the token buffer that has the arguments to be generated
	 * @param session the JBasic session for this compilation (needed for messaging)
	 * @return Status indicating if the compilation was successful.
	 */
	public Status generateArgumentList(JBasic session, final Tokenizer tokens) {
		/*
		 * If there are arguments, let's take care of that now.
		 */

		if (tokens.assumeNextToken("(")) {

			int count = 0;
			boolean defaultGiven = false;

			while (true) {

				/*
				 * If it's the ... variable argument list operator, then we can
				 * stop now.  In this case we don't generate the test for exact
				 * argument counts either.
				 */
				if (tokens.assumeNextToken("...")) {
					if (!tokens.assumeNextToken(")"))
						return new Status(Status.PAREN);
					break;
				}
				/*
				 * If we're done with the list, then generate the instruction to
				 * test for the argument count and we're done.
				 */
				if (tokens.assumeNextToken(")")) {
					byteCode.add(ByteCode._ARGC, count);
					break;
				}
				/*
				 * The user might have specified explicit types for the arguments.
				 */

				int typeCode = Value.UNDEFINED;

				if( tokens.assumeNextToken(new String[] { "ARRAY", "BOOLEAN", "DOUBLE", "INTEGER", "STRING"})) {
					typeCode = Value.nameToType(tokens.getSpelling());
					if( typeCode == Value.UNDEFINED ) 
						return new Status(Status.TYPEMISMATCH);
					
					/* If the next token is actually a comma, it wasn't a type name */
					String peek = tokens.peek(0);
					if( peek.equals(",") || peek.equals(")") || peek.equals("..."))
						tokens.restoreToken();
				}

				/*
				 * Else get the argument name.
				 */

				final String symbolName = tokens.nextToken();
				if (!tokens.isIdentifier())
					return new Status(Status.ARGERR);
				count++;
				/*
				 * See if it has a default value
				 */
				if (tokens.assumeNextToken("=")) {
					defaultGiven = true;
					Expression expr = new Expression(session);
					expr.compile(byteCode, tokens);
					if (expr.status.failed())
						return status = expr.status;
					if( typeCode != Value.UNDEFINED ) {
						byteCode.add(ByteCode._ARGDEF, count );
						byteCode.add(ByteCode._CVT, typeCode);
						byteCode.add(ByteCode._STOR, symbolName);
					}
					else
						byteCode.add(ByteCode._ARGDEF, count, symbolName);
				} else {

					/*
					 * Has a default already been given?  If so, then you
					 * can't follow it with non-default arguments.
					 */
					if( defaultGiven )
						return status = new Status(Status.ARGERR);

					/*
					 * No default. However, there may be a type code.
					 */
					if( typeCode != Value.UNDEFINED ) {
						byteCode.add(ByteCode._ARG, count );
						byteCode.add(ByteCode._CVT, typeCode);
						byteCode.add(ByteCode._STOR, symbolName);
					}
					else
						byteCode.add(ByteCode._ARG, count, symbolName);
				}

				if (tokens.assumeNextToken(","))
					continue;
				/*
				 * If we're done with the list, then generate the instruction to
				 * test for the argument count and we're done.
				 */
				if (tokens.assumeNextToken(")")) {
					byteCode.add(ByteCode._ARGC, count);
					break;
				}

				return new Status(Status.ARGERR);
			}

		}
		return new Status();
	}
}

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
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Message;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.compiler.CompileContext;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.funcs.JBasicFunction;
import org.fernwood.jbasic.opcodes.OpCLEAR;
import org.fernwood.jbasic.opcodes.OpDEBUG;
import org.fernwood.jbasic.opcodes.OpDEFMSG;
import org.fernwood.jbasic.opcodes.OpSYS;
import org.fernwood.jbasic.opcodes.OpTHREAD;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Functions;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.runtime.JBasicQueue;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * SHOW statement. It parses the second token and uses it to dispatch to the
 * appropriate method for displaying the requested information.
 * 
 * @author tom
 * @version 1.2 December 2010
 */

class ShowStatement extends Statement {

	

	/**
	 * Execute 'show' statement. Parse the sub-verb (the word that follows
	 * <code>SHOW</code>, and uses it to call an internal method that dumps
	 * out information as requested by the user.
	 * 
	 * @param symbols
	 *            The symbol table to use to resolve identifiers, or null if no
	 *            symbols allowed.
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		String hasScopeKeyword = null;
		String verb = tokens.nextToken().toUpperCase();
		if (tokens.isType(Tokenizer.END_OF_STRING))
			verb = "STATUS";

		/*
		 * Because we're going to dump out stuff, let's make sure we get
		 * the command prompt when we're done.
		 */
		
		this.session.setNeedPrompt(true);
		boolean showHidden = false;
		
		if (tokens.isType(Tokenizer.IDENTIFIER))
			if (verb.equals("HIDDEN")|| verb.equals("ALL")) {
			showHidden = true;
			verb = tokens.nextToken();
		}
		/*
		 * If we have a prefix value of GLOBAL or LOCAL then we must put it
		 * after the next word. So
		 * 
		 * SHOW GLOBAL SYMBOLS
		 * 
		 * becomes
		 * 
		 * SHOW SYMBOLS GLOBAL
		 */

		if (tokens.isType(Tokenizer.IDENTIFIER))
			if (verb.equalsIgnoreCase("LOCAL")
					| verb.equalsIgnoreCase("GLOBAL")
					| verb.equalsIgnoreCase("PARENT")
					| verb.equalsIgnoreCase("MACRO")
					| verb.equalsIgnoreCase("CONSTANT")) {

				final String t2 = tokens.nextToken();
				hasScopeKeyword = verb;
				tokens.restoreToken(verb, Tokenizer.IDENTIFIER);
				verb = t2;
			}


		/*
		 * SHOW EVENTS dumps the event queue
		 */
		if( verb.equals("EVENTS")) {
			ByteCode bc = new ByteCode(session);
			bc.add(ByteCode._SYS, showHidden? OpSYS.SYS_DUMP_ALL_EVENTS : OpSYS.SYS_DUMP_EVENTS);
			bc.end();
			return bc.run(symbols, 0);
		}
		
		/*
		 * SHOW OPTIMIZER dumps the optimizer statistics database.
		 */
		if( verb.equals("OPTIMIZER") || verb.equals("OPTIMIZERS")) {
			if( showHidden ) 
				return status = new Status(Status.INVALL);
			String optName = null;
			
			if( tokens.testNextToken(Tokenizer.STRING)) {
				optName = tokens.nextToken().toLowerCase();
			}
			else
				if( tokens.testNextToken(Tokenizer.IDENTIFIER)) {
					optName = tokens.nextToken().toLowerCase();
				}
			
			ByteCode bc = new ByteCode(session);
			if( optName == null )
				bc.add(ByteCode._SYS, OpSYS.SYS_STAT_OPT);
			else
				bc.add(ByteCode._SYS, OpSYS.SYS_STAT_OPT, optName);
			
			bc.end();
			return bc.run(symbols, 0);
		}

		/*
		 * SHOW LOCKS is executed locally as _THREAD 9
		 */
		
		if( verb.equals("LOCKS")) {
			if( showHidden ) 
				return status = new Status(Status.INVALL);
			ByteCode bc = new ByteCode(session);
			bc.add(ByteCode._THREAD, OpTHREAD.LIST_LOCKS);
			bc.end();
			return bc.run(symbols, 0);
		}
	
		/*
		 * SHOW PERMISSIONS is executed locally.
		 */
		
		if( verb.equals("PERMISSION") || verb.equals("PERMISSIONS")) {
			if( showHidden ) 
				return status = new Status(Status.INVALL);
			ByteCode bc = new ByteCode(session);
			bc.add(ByteCode._DEFMSG, OpDEFMSG.DEFMSG_LOAD_FILE, "!PermissionNames.xml");
			bc.add(ByteCode._CALLP, 0, "$SHOWPERMISSIONS");
			bc.add(ByteCode._CLEAR, OpCLEAR.CLEAR_MESSAGE, "_PERM_*");
			bc.end();
			return bc.run(symbols, 0);
		}
		/*
		 * SHOW SYMBOL X shows details on a single symbol
		 */
		
		if( verb.equals("SYMBOL")) {
			String name = tokens.nextToken();
			if( tokens.getType() != Tokenizer.IDENTIFIER) 
				return status = new Status(Status.EXPNAME);
			if( showHidden ) 
				return status = new Status(Status.INVALL);
			
			ByteCode bc = new ByteCode(session);
			bc.add(ByteCode._STRING, name);
			bc.add(ByteCode._DEBUG, OpDEBUG.PRINT_SYMBOL_DATA);
			bc.add(ByteCode._END);
			return bc.run(symbols, 0);
			
		}
		/*
		 * The SHOW SERVER command remaps to execution of the SERVER 
		 * command.  If no sub-verb is given, then STATUS is assumed.
		 */
		
		if (verb.equals("SERVER")) {
			final String[] subVerbs = new String[] 
			              { "ACTIVE", "INACTIVE", "LIST", "USERS", "USER",
			              	"SESSIONS", "STATUS", "LOGICALS", "LOGICAL" };
			ByteCode bc = new ByteCode(session);
			if( showHidden ) 
				return status = new Status(Status.INVALL);
			boolean valid = false;
			if (tokens.endOfStatement()) {
				bc.add(ByteCode._STRING, "SERVER STATUS");
				valid = true;
			} else
				for (String subVerb : subVerbs)
					if (tokens.testNextToken(subVerb)) {
						bc.add(ByteCode._STRING, "SERVER "
								+ tokens.getBuffer());
						valid = true;
						break;
					}

			if (valid) {
				bc.add(ByteCode._EXEC);
				bc.end();
				status = bc.run(symbols, 0);
				tokens.flush();
				return status;
			}
			return new Status(Status.VERB, tokens.nextToken());
		}

		/*
		 * The singular form of SHOW VERB, SHOW FUNCTION, and SHOW TEST are
		 * re-mapped into SHOW PROGRAM with the name mangled with the required
		 * prefix.
		 * 
		 * This way, SHOW VERBS will display all the verbs, and SHOW VERB X will
		 * become SHOW PROGRAM VERB$X and list the named verb, for example.
		 */

		if (verb.equals("VERB")) {
			tokens.restoreToken(JBasic.VERB + tokens.nextToken(),
					Tokenizer.IDENTIFIER);
			verb = "PROGRAM";
		}

		if (verb.equals("TEST")) {
			tokens.restoreToken(JBasic.TEST + tokens.nextToken(),
					Tokenizer.IDENTIFIER);
			verb = "PROGRAM";
		}

		if (verb.equals("FUNCTION")) {
			tokens.restoreToken(JBasic.FUNCTION + tokens.nextToken(),
					Tokenizer.IDENTIFIER);
			verb = "PROGRAM";
		}

		/*
		 * If the user issues SHOW SYMBOL TABLE, then remap it to the SHOW
		 * SYMBOLS command. This way, SHOW SYMBOL TABLE LOCAL and SHOW SYMBOLS
		 * LOCAL are the same thing.
		 */
		if (verb.equals("SYMBOL") & tokens.assumeNextToken("TABLE"))
			verb = "SYMBOLS";

		/*
		 * Now, based on the sub-verb, dispatch the correct display operation.
		 * Some of these parse additional options (like the showSymbols()
		 * method, while others just call a dump routine directly.
		 */

		/*
		 * The first group are the ones that accept the LOCAL and GLOBAL words
		 * moved from the beginning to the end of the keyword. SHOW SYMBOLS is
		 * the example here.
		 */

		if (verb.equals("SYMBOLS"))
			return status = showSymbols(tokens, symbols, showHidden);

		/*
		 * At this point, we've used all the SHOW commands that are interested
		 * in the LOCAL/GLOBAL prefix. So if we had one and still haven't
		 * dispatched a command, it's a syntax error.
		 */
		if (hasScopeKeyword != null)
			return status = new Status(Status.VERB, "SHOW " + hasScopeKeyword
					+ " " + verb);

		/*
		 * The rest of these don't permit the scope keyword, and are parsed
		 * as-is.
		 */

		if (verb.equals("MEMORY"))
			return status = showMemory(session.stdout, tokens, symbols);
		
		if (verb.equals("QUEUES"))
			return status = showQueues(tokens, symbols);

		if (verb.equals("FILES"))
			return status = showFiles(tokens, symbols);

		if (verb.equals("FUNCTIONS"))
			return status = showFunctions(tokens, symbols);

		if (verb.equals("THREADS")) {
			
			ByteCode byteCode = new ByteCode(session);
			
			byteCode.add(ByteCode._THREAD, OpTHREAD.LIST_THREADS);
			byteCode.add(ByteCode._NEEDP, 1 );
			return byteCode.run(symbols, 0);
		}


		if (verb.equals("MESSAGES"))
			return status = showMessages(tokens, symbols);

		if (verb.equals("MESSAGE"))
			return status = showMessage(tokens, symbols);

		if (verb.equals("OPTIONS"))
			return status = showOptions(tokens, symbols);

		if (verb.equals("CALLS"))
			return status = showCalls(tokens, symbols);

		if (verb.equals("PROGRAMS") | verb.equals("PROGRAM"))
			return status = showProgram(tokens, symbols, showHidden);

		if (verb.equals("TESTS")) {
			if( showHidden ) 
				return status = new Status(Status.INVALL);
			if (session.programs.find("TEST$MAIN") == null) {
				final Statement stmt = new Statement(session);
				stmt.execute("LOAD \"@Tests.jbasic\"", symbols);
			}
			return filteredProgramList("Test programs", JBasic.TEST);
		}

		if (verb.equals("VERBS")) {
			if( showHidden ) 
				return status = new Status(Status.INVALL);
			return filteredProgramList("Verb programs", JBasic.VERB);
		}
		
		if (verb.equals("VERSION")) {
			if( showHidden ) 
				return status = new Status(Status.INVALL);
			String version = symbols.getString("$VERSION");
			if( version == null )
				version = "Bogus JBasic instance, no $VERSION!";
			
			session.stdout.println("Version " + version);
			return status = new Status(Status.SUCCESS);
		}
		if (verb.equals("STATUS")) {
			if( showHidden ) 
				return status = new Status(Status.INVALL);
			return dumpStatus();
		}
		
		/*
		 * Nothing I've ever seen before, so error out.
		 */
		
		return status = new Status(Status.INVSHOW, verb);
	}

	/**
	 * @param tokens
	 * @param symbols
	 * @return a Status indicating that the command succeeded.
	 */
	private Status showQueues(final Tokenizer tokens, final SymbolTable symbols) {

		if (JBasic.queueList == null)
			return new Status();

		final Iterator i = JBasic.queueList.values().iterator();
		int count = 0;
		while (i.hasNext()) {
			final JBasicQueue q = (JBasicQueue) i.next();
			final String s = q.toString();
			count = count + 1;
			if (count == 1)
				session.stdout.println("QUEUES:");

			session.stdout.println("   " + s);
		}
		session.stdout.println(count + " queue" + (count == 1 ? "" : "s"));
		return new Status();
	}

	private String plural(final int n, final String singularText,
			final String pluralText) {
		return Integer.toString(n) + " "
				+ ((n == 1) ? singularText : pluralText);
	}

	private Status dumpStatus() {

		final JBasicFile f = session.stdout;
		final Date rightNow = new Date();
		String text = null;
		int count = 0;
		int i = 0;

		f.println("JBasic Status as of " + rightNow.toString());
		f.indent(3);
		f.println("JBasic version:        " + JBasic.version /* session.getString("$VERSION") */);

		Program currentProgram = session.programs.getCurrent();
		if (currentProgram == null)
			text = "None";
		else {
			count = currentProgram.statementCount();
			text = currentProgram.getName() + ", "
					+ plural(count, "statement", "statements");
		}

		f.println("Current program:       " + text);
		f.println("Global symbol table:   "
				+ session.globals().size() + " entries");

		count = session.onStatementStack.stackSize();
		f.println("ON Statement stack:    "
				+ plural(count, "call level", "call levels"));
		for (i = 1; i <= count; i++)
			f.println("                      Level "
					+ i
					+ ", "
					+ plural(session.onStatementStack.stackSize(i), "signal",
							"signals"));
		f.println("Open files:            "
				+ plural(session.openUserFiles.size(), "file", "files"));

		int count_verbs = 0;
		int count_tests = 0;
		int count_functions = 0;
		int count_programs = session.programs.count();

		for (final Iterator ix = session.programs.iterator(); ix.hasNext();) {
			final Program p = (Program) ix.next();

			if (p.getName().startsWith(JBasic.VERB))
				count_verbs++;
			else if (p.getName().startsWith(JBasic.FUNCTION))
				count_functions++;
			else if (p.getName().startsWith(JBasic.TEST))
				count_tests++;
		}

		f.println("Stored programs:       "
				+ plural(count_programs, "program", "programs"));
		count_programs = count_programs
				- (count_tests + count_verbs + count_functions);
		f.println("  User programs:       " + count_programs);
		f.println("  Functions:           " + count_functions);
		f.println("  Verbs:               " + count_verbs);
		f.println("  Tests:               " + count_tests);

		f.println("Error messages:        "
				+ plural(session.messageManager.getMessageCount(), "message", "messages"));

		final long elapsed = System.currentTimeMillis() - session.startTime;

		f.println("Uptime:                " + Utility.formatElapsedTime((int) (elapsed / 1000)));

		f.println("Statement Statistics:");
		f.println("   Executed:           " + session.statementsExecuted);
		f.println("   Compiled:           " + session.statementsCompiled);
		f.println("   Exec as bytecode:   " + session.statementsByteCodeExecuted);
		f.println("Instructions executed: " + session.instructionsExecuted);
		showMemory(f, null, null);
		f.println();
		f.indent(0);
		f.println(SetStatement.getDebugString(session,
				"   Options:              "));

		f.indent(0);

		return new Status();
	}

	private Status showCalls(final Tokenizer t, final SymbolTable syms) {

		/*
		 * See if there is a maximum depth the user wants to see.
		 */

		int depth = -1; // dump all frames by default.

		if (!t.testNextToken(Tokenizer.END_OF_STRING)) {
			final Expression exp = new Expression(session);
			final Value fc = exp.evaluate(t, syms);
			if (fc == null)
				return exp.status;
			depth = fc.getInteger();
		}
		/*
		 * Starting with the current symbol table, look for stack frame info.
		 */

		SymbolTable s = syms;
		int count = 0;
		Value labelWidth = s.findReference("SYS$LABELWIDTH", false);
		int oldWidth = labelWidth.getInteger();
		labelWidth.setInteger(1);

		while (depth != 0) {

			/*
			 * If we found the console frame, we're done. Also, if we found the
			 * global symbol table, there was no console but we're still done...
			 */

			if (s.fGlobalTable)
				break;
			if (s.name.equals("Local to Console"))
				break;

			count++;

			StringBuffer buff = new StringBuffer(Utility.pad(Integer.toString(count) + ": ", 4));

			Value modeValue = s.findReference("$MODE", false);
			String mode = "RUN";
			if( modeValue != null )
				mode = modeValue.getString();
			
			buff.append(Utility.pad(mode, 6));
			buff.append(' ');
			String pn  = s.getString("_PROGRAM_");
			if (pn == null)
				buff.append("<no debug data>");
			else {
				buff.append(pn);

				while (buff.length() < 26)
					buff.append(' ');

				final int ln = s.getInteger("_LINE_");
				Program p = session.programs.find(pn);
				Statement st = p.findLineNumber(ln);
				if( st == null ) {
					st = p.getStatement(ln);
					String ls = "#" + Integer.toString(ln) + " ";
					while( ls.length() < 4 )
						ls = " " + ls;
					buff.append(ls);
					buff.append(st.toString());
				}
				else
					buff.append(st.toString().trim());
			}

			session.stdout.println(buff.toString());
			depth--;
			s = s.parentTable;
		}
		labelWidth.setInteger(oldWidth);
		String cs;
		if (count == 0)
			cs = "No";
		else {
			cs = Integer.toString(count);
			session.stdout.println();
		}
		String plural = "s";
		if (count == 1)
			plural = "";
		session.stdout.println(cs + " stack frame" + plural);
		return new Status();

	}


	/**
	 * Execute '<code>show table <em>[kind]</em></code>' statement.
	 * 
	 * @param symbols
	 *            The symbol table to use to resolve identifiers, or null if no
	 *            symbols allowed.
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 * @return a Status indicating if the SHOW SYMBOLS command could be parsed
	 *         and executed correctly.
	 */

	private Status showSymbols(Tokenizer tokens, final SymbolTable symbols, boolean hidden) {

		SymbolTable st;

		st = symbols;
		
		if (tokens.assumeNextToken("ALL")) {
			while (st != null) {
				st.dumpTable(session, hidden);
				st = st.parentTable;
			}
			return status = new Status(Status.SUCCESS);
		}

		if (tokens.assumeNextToken("MACRO"))
			return session.macroTable.dumpTable(session, hidden);

		if (tokens.assumeNextToken("LOCAL"))
			return symbols.dumpTable(session, hidden);

		if (tokens.assumeNextToken("GLOBAL"))
			return session.globals().dumpTable(session, hidden);

		if (tokens.assumeNextToken("CONSTANTS"))
			return session.globals().parentTable.dumpTable(session,hidden);

		if (tokens.assumeNextToken("PARENT"))
			return symbols.parentTable.dumpTable(session, hidden);

		int index = 0;
		String indexStr = "enough";

		if (!tokens.testNextToken(Tokenizer.END_OF_STRING)) {
			indexStr = tokens.nextToken();
			if (tokens.getType() != Tokenizer.INTEGER)
				return new Status(Status.INVSHOW, "SYMBOLS " + indexStr);

			index = Integer.parseInt(indexStr);
		}

		st = symbols;
		int ix = index;
		while ((ix > 0) & (st != null)) {
			ix--;
			if( st != null)
				st = st.parentTable;
		}
		if (st == null)
			session.stdout.println("There are only " + (index - ix - 1)
					+ " symbol table frames above \"" + symbols.name + "\"");
		else {
				while (st != null) {

					st.dumpTable( session, hidden);
					st = st.parentTable;
					if( st != null && st.fGlobalTable )
						break;
				}
				return status = new Status(Status.SUCCESS);
			}

		return new Status();
	}

	/**
	 * Execute the <code>SHOW PROGRAM <em>[name]</em></code> statement. This
	 * lists all the programs in stored memory (as opposed to functions, verbs,
	 * or tests). If a specific name is given, then that program is LIST'ed to
	 * the console. The program name may be a fully-qualified name, such as MAIN
	 * for program MAIN, or VERB$JAVA for the code for the JAVA verb.
	 * 
	 * @param tokens
	 *            The token stream used to parse the optional program name.
	 * @param symbols
	 *            The symbol table to use during operation
	 * @param showHidden
	 * 			  If true, programs that are normally hidden are displayed.
	 * @return A Status value indicating success of the statement.
	 */
	@SuppressWarnings("unchecked") 
	private Status showProgram(final Tokenizer tokens, 
								final SymbolTable symbols,
								boolean showHidden) {

		// Later, we'll parse to see if we should use a different file here.

		JBasicFile outputFile = session.stdout;
		int vectorLength = 0;
		boolean fWriteToFile = false;
		boolean fArray = false;
		boolean fDisassemble = symbols.getBoolean("SYS$DISASSEMBLE");
		boolean fProfile = symbols.getBoolean("SYS$PROFILING");
		/*
		 * Only turn this on when debugging line number tokenization
		 * problems with RENUMBER; it prints out the token position
		 * array for each statement that has one during a LIST or 
		 * SHOW PROGRAMS command.
		 */
		final boolean fLineNumberArray = symbols.getBoolean("SYS$LNA");
		

		final Expression exp = new Expression(session);

		// See if this is a SHOW PROGRAMS command with no arguments, which asks
		// us
		// to list the programs in memory.

		if (tokens.testNextToken(Tokenizer.END_OF_STRING)) {

			if (session.programs.count() == 0) {
				session.stdout.println("No stored programs");
				return new Status(Status.SUCCESS);
			}

			session.stdout.println();
			session.stdout.println("Loaded programs:");
			session.stdout.println();

			final JBasicFile outFile = session.stdout;

			outFile.columnOutput(25, 3);
			outFile.indent(5);

			int count = 0;
			for (final Iterator i = session.programs.iterator(); i.hasNext();) {
				final Program storedProgram = (Program) i.next();
				if (storedProgram.getName() == null)
					continue;

				String name = storedProgram.getName() + "";
				
				/*
				 * Program names that start with a "$" are considered to be
				 * "invisible". The typical example is $PREFERENCES
				 */
				
				if( !showHidden && name.charAt(0) == '$')
					continue;
				
				/* See if this is a PROGRAM or not. */
				if (name.startsWith(JBasic.FUNCTION)
						| name.startsWith(JBasic.VERB)
						| name.startsWith(JBasic.TEST))
					continue;

				if (storedProgram.isModified())
					name = name + "#";
				else if (!storedProgram.isSystemObject())
					name = name + "*";

				/* See if this is a PROGRAM or not. */
				if (name.startsWith(JBasic.FUNCTION)
						| name.startsWith(JBasic.VERB)
						| name.startsWith(JBasic.TEST))
					continue;

				outFile.print(name);
				count++;
			}
			outFile.columnOutputEnd();
			outFile.indent(0);
			outFile.println();
			outFile.println(count + " programs");

			return new Status(Status.SUCCESS);
		}

		boolean moreFlags = true;
		boolean fSavedProfile = false;
		
		if (tokens.assumeNextSpecial("(")) {
			while (moreFlags) {
				if (tokens.assumeNextSpecial(","))
					continue;

				if (tokens.assumeNextToken("DISASM")) {
					fDisassemble = true;
					continue;
				}
				if (tokens.assumeNextToken("PROFILE")) {
					fSavedProfile = fProfile;
					fProfile = true;
					fDisassemble = true;
					continue;
				}
				// See if there is a FILE clause. Note that if it's the "#"
				// character, we convert that to a FILE verb.

				if (tokens.assumeNextToken("#"))
					tokens.restoreToken("FILE", Tokenizer.IDENTIFIER);

				if (tokens.assumeNextToken("FILE")) {
					final Value outputFileName = exp.evaluate(tokens, symbols);
					if (exp.status.failed())
						return exp.status;
					try {
						outputFile = JBasicFile.lookup(session, outputFileName);
					} catch (JBasicException e) {
						outputFile = null;
					}
					if (outputFile == null)
						return new Status(Status.FNOPENOUTPUT, outputFileName
								.toString());
					if (!tokens.assumeNextToken(","))
						return new Status(Status.FILECOMMA);
					fWriteToFile = true;
					continue;
				}
				moreFlags = false;
			}
			if (!tokens.assumeNextSpecial(")"))
				return new Status(Status.PAREN);
		}
		

		// Parse the item to be listed as an expression. An attempt
		// to list other than an arrayValue or program will be easy to
		// detect this way.  If there is nothing after the () list, then
		// we are to use the current program.

		String pName = null;
		
		if (tokens.testNextToken(Tokenizer.END_OF_STRING)) {
			pName = session.getString("SYS$CURRENT_PROGRAM");
		} 
		else
		if( tokens.testNextToken(Tokenizer.IDENTIFIER)) {
			pName = tokens.nextToken();
		}
		else
			return status = new Status(Status.PROGRAM);

		Program p = session.programs.find(pName);
		if( p == null ) {
			if( pName.startsWith(JBasic.FUNCTION)) {
				String verbName = pName.substring(JBasic.FUNCTION.length()).toUpperCase();
				Class c;
				try {
					c = Functions.findFunctionClass(session, verbName);
				} catch (JBasicException e1) {
					c = null;
				}
				if( c != null ) {
					outputFile.print( verbName + " is a");
					
					Method m = null;
					try {
						m = c.getDeclaredMethod("compile", CompileContext.class );
					} catch (SecurityException e) {
						m = null;
					} catch (NoSuchMethodException e) {
					}
					if( m == null )
						outputFile.print(" runtime");
					else
						outputFile.print(" compiled-in");
					outputFile.println(" JBasic function");
					return new Status();
				}
			}
			else if( pName.startsWith(JBasic.VERB)) {
				String verbName = pName.substring(JBasic.VERB.length()).toUpperCase();
				Class c = Statement.findStatementClass(session, Statement.verbForm(verbName));
				if( c != null ) {
					outputFile.print( verbName + " is a");
					
					Method m = null;
					try {
						m = c.getDeclaredMethod("compile", Tokenizer.class );
					} catch (SecurityException e) {
						m = null;
					} catch (NoSuchMethodException e) {
					}
					if( m == null )
						outputFile.print("n interpreted");
					else
						outputFile.print(" compiled");
					outputFile.println(" JBasic program statement");
					return new Status();
				}
			}
			return status = new Status(Status.PROGRAM, pName);
		}
		
		if (p.isProtected() & !session.getBoolean("SYS$ROOTUSER"))
				return new Status(Status.PROTECTED, pName);

		String listingName = "";

		if (!fWriteToFile) {
			outputFile.println("");
			outputFile.println(listingName + "PROGRAM " + pName );
			outputFile.println("");
		} else if (fArray)
			outputFile.print("{");

		vectorLength = p.statementCount();

		try {
			this.session.globals().insert("SYS$PROFILING",fProfile);
		} catch (JBasicException e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < vectorLength; i++) {

			// Print each statement/arrayValue element
			
			ListStatement.statementPrint(session, outputFile, fDisassemble,
						false, p.getStatement(i),
						fLineNumberArray);
		}
		try {
			this.session.globals().insert("SYS$PROFILING",fSavedProfile);
		} catch (JBasicException e) {
			e.printStackTrace();
		}

		/*
		 * If we are disassembling AND there are locally defined functions, 
		 * we should dump them out now as well as long as it's not to a file.
		 */
		
		if( !fWriteToFile & fDisassemble & p.hasLocalFunctions()) {
			Iterator i = p.localFunctionIterator();
			outputFile.println();
			outputFile.println("LOCAL FUNCTIONS:");
			while( i.hasNext()) {
				String functionName = (String) i.next();
				ByteCode bc = p.findLocalFunction(functionName);
				outputFile.println("   " + Utility.pad(functionName, 16)
						+ bc.size() + " bytecodes" );
			}
		}
		
		/*
		 * Trailing newline for the display to end cleanly.
		 */
		if (!fWriteToFile)
			outputFile.println("");

		return status = new Status(Status.SUCCESS);
	}

	/**
	 * Execute the <code>SHOW OPTIONS</code> statement. This calls the routine
	 * in the SetStatement class that handles option formatting, and displays
	 * the result on the console.
	 * 
	 * @param tokens
	 *            The token stream <em>(currently unused)</em>
	 * @param symbols
	 *            The symbol table <em>(currently unused)</em>
	 * @return A Status value indicating success of the statement.
	 */

	private Status showOptions(final Tokenizer tokens, final SymbolTable symbols) {

		session.stdout.println(SetStatement.getDebugString(session, "Options: "));
		return status = new Status(Status.SUCCESS);
	}

	private Status showMessage(final Tokenizer tokens, final SymbolTable symbols) {

		String flag = "";
		if (tokens.assumeNextToken("*"))
			flag = "*";

		if (!tokens.testNextToken(Tokenizer.IDENTIFIER))
			return new Status(Status.NOMSG);

		final String code = flag + tokens.nextToken().toUpperCase();

		String parm = "{parameter}";
		if (tokens.testNextToken(Tokenizer.IDENTIFIER)
				| tokens.testNextToken(Tokenizer.STRING))
			parm = tokens.nextToken();

		String msg = new Status(code, parm).getMessage(session);

		if (msg.equals(code))
			msg = "Error: Message " + code + " is undefined";
		else
			msg = "  " + code + ": " + msg;
		session.stdout.println(msg);

		return new Status();

	}

	/**
	 * Execute the <code>SHOW MESSAGES</code> statement. This displays the
	 * error code and error text of all defined messages.
	 * 
	 * @param tokens
	 *            The token stream, used to parse the optional PROGRAM keyword.
	 * @param symbols
	 *            The symbol table <em>(currently unused)</em>
	 * @return A Status value indicating success of the statement.
	 */

	private Status showMessages(final Tokenizer tokens,
			final SymbolTable symbols) {

		if( session.messageManager == null ||
				session.messageManager.messages == null ||
				session.messageManager.messages.size() == 0 )
			return new Status(Status.FAULT, "no message database");
		/*
		 * See if this is PROGRAM mode output.
		 */

		boolean programMode = false;
		if (tokens.assumeNextToken("PROGRAM"))
			programMode = true;

		final JBasicFile outFile = session.stdout;

		if (programMode) {
			outFile.println();
			outFile.println("PROGRAM $INIT_MESSAGES");
			final Date rightNow = new Date();
			outFile
					.println("// JBasic message definition file.  This file contains");
			outFile
					.println("// the text used to display messages.  The language is");
			outFile
					.println("// indicated in parenthesis, and corresponds to the two-");
			outFile
					.println("// character Java language encoding.  For example, ");
			outFile.println("//");
			outFile.println("//    (EN)   English");
			outFile.println("//    (FR)   French");
			outFile.println("//    (ES)   Spanish");
			outFile.println("//");
			outFile
					.println("// If a message does not have a non-English translation,");
			outFile.println("// then the (EN) message text is used.");
			outFile.println("//");
			outFile.println("// Created by   "
					+ System.getProperty("user.name"));
			outFile.println("// Generated on " + rightNow.toString());
			outFile.println("//");
			outFile.println("//");

			outFile.println();
		}

		/*
		 * Scan over the TreeMap and pull out the message objects, and format
		 * the strings.
		 */

		for (final Iterator i = session.messageManager.iterator(); i.hasNext();) {

			final Message m = (Message) i.next();

			/*
			 * We'll be messing with the code to format it, so make a copy.
			 */
			String padded = new String(m.getMessageCode());

			/*
			 * Success return codes start with a "*", align non-success
			 * messages.
			 */
			if (!padded.substring(0, 1).equals("*"))
				padded = " " + padded;
			/*
			 * Pad all messages to 20 characters so the columns line up nicely.
			 */
			padded = Utility.pad( padded, 15);
			
			//while (padded.length() < 15)
			//	padded = padded + " ";

			String prefix = "";
			if (programMode)
				prefix = "MESSAGE ";

			if (programMode)
				outFile.println(prefix + padded + " \"" + m.getMessageText()
						+ "\"");
			else
				outFile.println(prefix + padded + " " + m.getMessageText());
		}
		outFile.println();
		if (programMode)
			outFile.println("RETURN");

		return status = new Status(Status.SUCCESS);
	}

	/**
	 * Execute the <code>SHOW FUNCTIONS</code> statement. This iterates
	 * 
	 * @param tokens
	 *            The token stream <em>(currently unused)</em>
	 * @param symbols
	 *            The symbol table <em>(currently unused)</em>
	 * @return A Status value indicating success of the statement.
	 */

	private Status showFunctions(final Tokenizer tokens,
			final SymbolTable symbols) {

		Program m;
		final JBasicFile outFile = session.stdout;

		Value funcList = JBasicFunction.functionList();
		
			/*
			 * Now that we have the list, iterate over it again in sorted order.
			 * This lets us print the values in alphabetical order instead of in
			 * order of discovery from the method scan.
			 */

			outFile.indent(4);
			outFile.columnOutput(25, 3);

			int len = funcList.size();
			
			for (int ix = 1; ix <= len; ix++ ) {
				final String fn = "  " + funcList.getString(ix) + "()";

				outFile.print(fn);
			}
			outFile.columnOutputEnd();
			outFile.indent(0);


		if (session.programs.count() == 0) {
			session.stdout.println("No user-written functions");
			return new Status(Status.SUCCESS);
		}

		outFile.println();
		outFile.println("USER-WRITTEN FUNCTIONS:");
		outFile.indent(4);
		outFile.columnOutput(25, 3);

		for (final Iterator i = session.programs.iterator(); i.hasNext();) {

			String fname;

			m = (Program) i.next();

			fname = m.getName();

			// Skip over non-function names
			if (!fname.startsWith(JBasic.FUNCTION))
				continue;

			// Strip off the FUNCTION prefix

			fname = fname.substring(JBasic.FUNCTION.length(), fname.length())
					+ "()";
			String sysName = " ";

			if (m.isModified())
				sysName = "#";
			else if (!m.isSystemObject())
				sysName = "*";

			outFile.print(sysName + fname);
		}
		outFile.indent(0);
		outFile.columnOutputEnd();

		return status = new Status(Status.SUCCESS);
	}

	private Status showFiles(final Tokenizer tokens, final SymbolTable symbols) {

		JBasicFile file;

		if (session.openUserFiles == null) {
			session.stdout.println("No open files");
			return new Status(Status.SUCCESS);
		}

		/*
		 * We want to display this information in alphabetical order, so copy the
		 * data into a sorted tree, and use that sorted tree to traverse the file
		 * object list.  This is expensive, but only done for the SHOW command,
		 * and allows the normal list to be kept as a HashMap() that is faster.
		 */
		TreeMap<Integer, JBasicFile> sortedList = new TreeMap<Integer, JBasicFile>();
		sortedList.putAll(session.openUserFiles);
		int count = 0;
		
		for (final Iterator i = sortedList.values().iterator(); i
				.hasNext();) {
						
			file = (JBasicFile) i.next();
			String identifierName = file.getIdentifier();
			if( identifierName == null )
				continue;
			
			StringBuffer messageBuffer = new StringBuffer("  ");

			if( identifierName.startsWith(JBasic.FILEPREFIX))
				identifierName = "#" + identifierName.substring(JBasic.FILEPREFIX.length());
			messageBuffer.append(Utility.pad(identifierName, 25));
			
			Value fileID = file.getFileID();
			StringBuffer modeDesc = new StringBuffer();
			modeDesc.append(Utility.mixedCase(fileID.getString("MODE")));
			if( fileID.getElement("FIELD") != null )
				modeDesc.append(", Field");
			if( fileID.getElement("QUERY") != null )
				modeDesc.append(", Query");
			if( fileID.getElement("SYSTEM").getBoolean())
				modeDesc.append(", Perm");
			
			messageBuffer.append(Utility.pad( modeDesc.toString(), 20));
			
			messageBuffer.append(Utility.pad(fileID.getElement("SEQNO").getString(),-5));
			messageBuffer.append("  ");
			messageBuffer.append(fileID.getElement("FILENAME"));
		
			/* If this is the first one, put out the header */
			if( count == 0 ) {
				//session.stdout.println("Open Files:");
				session.stdout.println("  Identifier               Open Mode            Seq#  Name");
				session.stdout.println("  --------------------     ---------------      ----  --------------------------");
			}
			count++;
			session.stdout.println(messageBuffer.toString());

		}
		if( count == 0 )
			session.stdout.println("No open files");

		return status = new Status(Status.SUCCESS);
	}

	private Status showMemory(JBasicFile f, Tokenizer tokens, SymbolTable symbols ) {
	
		boolean doGarbageCollection = false;
		
		if( tokens != null )
			if( tokens.assumeNextToken(new String [] { "GC", "COMPRESS" }))
				doGarbageCollection = true;

		if( doGarbageCollection )		
			Runtime.getRuntime().gc();

		
		long freeMemory = Runtime.getRuntime().freeMemory();
		long totalMemory = Runtime.getRuntime().totalMemory();
		long usedMemory = totalMemory - Runtime.getRuntime().freeMemory();
		
		f.println("Memory" + (doGarbageCollection ? " after compression" : "") + ":");
		f.println(  "  In use:" + Utility.pad(Long.toString(usedMemory), -10)
				+	"  Free:  " + Utility.pad(Long.toString(freeMemory), -10)
				+	"  Total: " + Utility.pad(Long.toString(totalMemory), -10));
		
		return new Status();
	}
	
	
	/**
	 * List all program(s) that match a filter string. The output is in a
	 * three-column listing format. This is used in the statements for SHOW
	 * VERBS and SHOW TESTS, for example.
	 * 
	 * @param label
	 *            A string to print at the top of the listing indicating what
	 *            type of listing we are producing. An exmaple would be "Stored
	 *            TESTS:".
	 * @param filter
	 *            A prefix string like JBasic.FUNCTION ("FUNC$"). This is used
	 *            to identify the program(s) that are to be <em>included</em>
	 *            in the listing. All other program names are skipped.
	 * @return Status indicating success of the listing.
	 */
	private Status filteredProgramList(final String label, final String filter) {
		Program m;
		String formattedName = null;

		if (session.programs.count() == 0) {
			session.stdout.println("No " + label.toLowerCase());
			return new Status(Status.SUCCESS);
		}

		session.stdout.println(label + ": ");

		for (final Iterator i = session.programs.iterator(); i.hasNext();) {
			m = (Program) i.next();

			// Skip over names that don't use the prefix.

			if (!m.getName().startsWith(filter))
				continue;

			// Strip off theprefix

			formattedName = m.getName().substring(filter.length(),
					m.getName().length());

			// If it still starts with "$" (i.e. was a TEST$$ program) skip it
			// as well

			if (formattedName.startsWith("$"))
				continue;
			if (m.isModified())
				formattedName = formattedName + "#";
			else if (!m.isSystemObject())
				formattedName = formattedName + "*";

			String invocations = m.getRunCount() + " invocation";
			if (m.getRunCount() != 1)
				invocations = invocations + "s";
			else
				invocations = invocations + " ";
			
			invocations = Utility.pad(invocations, -16 );
			formattedName = Utility.pad(formattedName, 32 );

			String statements = Utility.pad(m.statementCount() + " statements", -18);

			session.stdout.println(" " + formattedName + statements + invocations);

		}

		return new Status(Status.SUCCESS);
	}

}

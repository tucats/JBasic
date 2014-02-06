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

package org.fernwood.jbasic;

import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.ObjectValue;
import org.fernwood.jbasic.value.Value;


/**
 * The main JBasic command line program object. 
 * <p>
 * This class exists to host an
 * instance of the main() method, used by the jar load-and-go function of Java.
 * This performs the following basic tasks:
 * <p>
 * <list>
 * <li>Create and initialize a single instance of a JBasic
 * <li>Process any command line arguments and store them in the global symbol
 * table.
 * <li>Running the MAIN program in the built-in library if it exists.
 * <li>If the MAIN program didn't execute a command from the command line, run
 * a console-input loop to read lines and execute them as statements. <list>
 * <p>
 * This was factored out of the JBasic object to allow the creation of multiple
 * JBasic session objects when used within another program in July 2006.
 * <p>
 * 
 * @author Tom Cole
 * @version version 1.0 Jul 4, 2006
 * 
 */

public class JBasicMain {


	/**
	 * Main entry point for JBasic. This is run when JBasic is invoked with the
	 * java invocation for a jar. This sets up the environment for running
	 * JBasic and runs a loop accepting commands from the user and executing
	 * them (to run programs or invoke immediate commands).
	 * 
	 * @param args
	 *            Standard java argument list for a main program. Contains
	 *            statements to be executed by JBasic.
	 */
	public static void main(final String args[]) {

		final JBasic session = new JBasic("JBasic Console");
		JBasic.firstSession = session;
		JBasic.rootTable.session = session;
		
		Status status;

		boolean fWorkspace = true;
		boolean fInterruptHandler = true;
		boolean fPreferences = true;
		boolean fMain = true;
		boolean fShell = true;
		boolean fCommand = false;
		boolean fJavaSession = false;
		String initialCommand = "";

		/*
		 * Set the mode of the JBasic session to reflect that are we
		 * (currently) in single-user mode.  This is a readonly
		 * value in the global table after session initialization,
		 * so we cheat and just set the string value directly.
		 */

		Value mode = session.globals().localReference("SYS$MODE");
		mode.setString("SINGLEUSER");

		/*
		 * Create the system arrayValue variable that holds all the program
		 * arguments.
		 */
		final Value argArray = new Value(Value.ARRAY, "SYS$ARGS");
		
		/*
		 * If there are arguments to the invocation, then go ahead and store
		 * them away in the SYS$ARGS arrayValue so the MAIN program can process
		 * them if it wishes.  Options start with a "-" and are processed here
		 * as well.
		 */

		
		if (args.length > 0) {

			int i;

			for (i = 0; i < args.length; i++) {
				if (args[i].charAt(0) == '-') {

					String opt = args[i].substring(1).toLowerCase();
					if (opt.equals("sandbox"))
						session.enableSandbox(true);
					else if (opt.equals("version")) {
						fCommand = true;
						fShell = false;
						initialCommand = "PRINT $VERSION";
					}
					else if (opt.equals("javasession"))
						fJavaSession = true;
					else if(opt.equals("help") || opt.equals("?")) {
						fCommand = true;
						fShell = false;
						initialCommand = "HELP COMMAND LINE";
					}
					else if( opt.equals("server")) {
						fCommand = true;
						fShell = true;
						initialCommand = "SERVER START DETACHED";
					}
					else if (opt.equals("noworkspace") || opt.equals("nows"))
						fWorkspace = false;
					else if (opt.equals("nointerrupt") || opt.equals("noint"))
						fInterruptHandler = false;
					else if (opt.equals("nopreferences")
							|| opt.equals("noprefs"))
						fPreferences = false;
					else if (opt.equals("nomain"))
						fMain = false;
					else if (opt.equals("shell"))
						fShell = true;
					else if (opt.equals("noshell"))
						fShell = false;
					else if (opt.equals("sandbox"))
						session.enableSandbox(true);
					else if (opt.equals("command") || opt.equals("cmd") ||
							 opt.equals("exec") || opt.equals("e")) {
						fCommand = true;
						fShell = false; 
						StringBuffer cmdParts = new StringBuffer();
						if( initialCommand.length() > 0 )
							cmdParts.append(initialCommand + " : ");
						
						for( ++i; i<args.length;i++) {
							cmdParts.append(' ');
							cmdParts.append(args[i]);
						}
						initialCommand = cmdParts.toString();
						break;
					} else
						System.out.println(session.getMessage(Status._UNKOPT) + opt);
				} else
					argArray.setElement(new Value(args[i]), i + 1);
			}
		}
		session.addEvent("+Command line arguments processed");
		
		/*
		 * After we've collected up the initial command from the command
		 * line parsing operation, store it away as system global for
		 * use by the $MAIN program later.
		 */
		try {
			session.globals().insert("SYS$INITCMD", initialCommand);
		} catch (JBasicException e1) {
			e1.print(session);
		}

		/*
		 * Save the argument list for user consumption, and mark readonly.
		 */
		try {
			session.globals().insertReadOnly("SYS$ARGS", argArray);
		} catch (JBasicException e1) {
			new Status(Status.FAULT, "unable to initialize arguments").print(session);
		}
		
		
		/*
		 * Create the ABOUT program explicitly.
		 */
		final Program aboutProgram = session.initializeAboutProgram();
		session.addEvent("+ABOUT initalized");
		/*
		 * Load the default workspace, if it exists.
		 */
		Status wsts = new Status();
		if (fWorkspace)
			wsts = Loader.loadFile(session, session.getWorkspaceName());
		session.addEvent("+Workspace loaded");
		
		/*
		 * Now that we're about to start executing JBasic language statements
		 * let's be sure the user can interrupt them if they go badly...
		 */
		if (fInterruptHandler)
			InterruptHandler.install("INT");

		/*
		 * Since this is the "main" program, it has all the permissions on
		 * by default.
		 */
		
		session.setPermission("ALL", true);
		//session.getUserIdentity().setPermissionMask();
		
		/*
		 * See if we have a program named "$MAIN" at this point. If so, then run
		 * it as the default initialization action.
		 */

		if (fMain)
			try {
				Value initState = new Value("INIT");
				initState.fReadonly = true;
				
				final Program mainProgram = session.programs.find(JBasic.PROGRAM + "$MAIN");
				if (mainProgram != null) {
					session.programs.setCurrent(mainProgram);
					final SymbolTable mainSymbols = new SymbolTable(session,
							"Local to MAIN", session.globals());
					mainSymbols.insertReadOnly("$MODE", initState);
					mainSymbols.markReadOnly("$MODE");
					status = mainProgram.run(mainSymbols, 0, null);
					session.addEvent("+MAIN completed");
					if (status.equals(Status.QUIT))
						session.running(false);
				} else {
					/*
					 * No main program ever given, so we just run the ABOUT program
					 * we know exists because we created it explicitly.
					 */
					final SymbolTable aboutSymbols = new SymbolTable(session,
							"Local to ABOUT", session.globals());
					aboutSymbols.insertReadOnly("$MODE", initState);
					aboutProgram.run(aboutSymbols, 0, null);
					session.stdout.println(session.getMessage(Status._NOMAIN));
					session.addEvent("+ABOUT completed");
				}
			} catch (JBasicException e) {
				new Status(Status.FAULT, e.getStatus()).print(session);
			}

		/*
		 * If there is a $PREFERENCES program (usually loaded from the default
		 * user work space file) then run that as well.
		 */

		try {
			Program preferencesProgram = null;
			Value initState = new Value("INIT");
			initState.fReadonly = true;
			if (fPreferences)
				preferencesProgram = session.programs.find(JBasic.PROGRAM + "$PREFERENCES");
			if (preferencesProgram != null) {
				session.programs.setCurrent(preferencesProgram);
				final SymbolTable mainSymbols = new SymbolTable(session,
						"Local to PREFERENCES", session.globals());
				mainSymbols.insertReadOnly("$MODE", initState);
				status = preferencesProgram.run(mainSymbols, 0, null);
				session.addEvent("+Preferences processed");
				if (status.equals(Status.QUIT))
					session.running(false);
			}
		} catch (JBasicException e) {
			new Status(Status.FAULT, "error loading preferences").print(session);
			e.getStatus().print(session);
		}

		/*
		 * If we had a successful workspace load, then print
		 * that information now that we're done with the main
		 * program.  We don't print this if we are executing
		 * a direct command from the command line.
		 */

		if (session.isRunning() & fWorkspace & !fCommand)
			if (wsts.success()) {
				System.out.println(session.getMessage(Status._LOADEDWS) + session.getWorkspaceName());
				System.out.println();
			}
		
		/*
		 * Start with no active program, and see what the user wants to do.
		 */
		session.programs.setCurrent(null);
		session.setCurrentProgramName("");
		
		/*
		 * Create a symbol table scope for console commands. This is where
		 * "local" variables created in immediate mode live, for example.
		 */

		final SymbolTable consoleSymbols = new SymbolTable(session,
				"Local to Console", session.globals());
		try{
			consoleSymbols.insert("$THIS", "Console");
			consoleSymbols.insert("$PARENT", "None");
			if( fJavaSession )
				consoleSymbols.insert("$SESSION", new ObjectValue( session ));
		} catch (JBasicException e2) {
			e2.print(session);
		}
		
		/*
		 * If there was an initial command given, use it.  If it
		 * results in an error, print the error message to the
		 * console.
		 */
		if (fCommand) {
			status = session.run(initialCommand);
			session.addEvent("+Initial command completed");
			status.printError(session);
		}

		/*
		 * Run a command line shell until we grow tired and quit.
		 */

		if (fShell) {
			String prompt = consoleSymbols.getString("SYS$PROMPT");
			if( prompt == null)
				prompt = "SHELL> ";
			status = session.shell(consoleSymbols, prompt);
		}

		/*
		 * If we got into multiuser mode we must tear it down now.
		 */

		if (JBasic.telnetDaemon != null)
			JBasic.telnetDaemon.stop();
	}

}

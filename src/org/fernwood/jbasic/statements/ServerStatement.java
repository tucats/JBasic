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

import java.util.Iterator;
import java.util.Vector;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.opcodes.OpCLEAR;
import org.fernwood.jbasic.opcodes.OpSERVER;
import org.fernwood.jbasic.opcodes.OpTHREAD;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SimpleCipher;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.runtime.UserManager;
import org.fernwood.jbasic.value.Value;

/**
 * SERVER statement handler.
 * <p>
 * This statement handles multiuser mode command functions. These are also also
 * aliased to various SET SERVER and SHOW SERVER commands, which are recoded as
 * SERVER statements and executed.
 * <p>
 * In general, the SERVER statements manipulate the state of the multiuser
 * server, or the data structures managed in memory to support that server.
 * These data structures take the form of the user account list, the active user
 * list, and the logical name table. These data structures are actually held in
 * the JBasic object that "owns" the multiuser server, and is almost always the
 * top-level (first) JBasic session instance created.
 * <p>
 * The SERVER command is interpreted; that is, the command is parsed at runtime
 * and executed by decoding the tokens in the statement, as opposed to compiling
 * them. There are some operations in the SERVER code that are generated as code
 * streams and immediately executed, but in general the SERVER statement is
 * interpreted. One primary benefit of this is that the execution of the
 * interpreter run() method is synchronized, so only one SERVER command at a
 * time can be executed. This prevents collisions in accessing the user list,
 * etc. from multiple threads at the same time.
 * <p>
 * <table>
 * <tr>
 * <td><b>LOGICAL NAMES</b></td>
 * <td>This displays the list of logical names available to sessions in this
 * server instance.</td>
 * </tr>
 * <tr>
 * <td><b>SESSIONS</b></td>
 * <td>This displays the active sessions; an entry is printed for each instance
 * of a user logged in to the current server.</td>
 * </tr>
 * <tr>
 * <td><b>GRANT</b></td>
 * <td>This is used to grant one or more permissions to a user.</td>
 * </tr>
 * <tr>
 * <td><b>REVOKE</b></td>
 * <td>This is used to revoke a permission from a user.</td>
 * </tr>
 * <tr>
 * <td><b>USERS</b></td>
 * <td>This lists all active user accounts and, for each user, indicates if they
 * are logged in at the moment or not.</td>
 * </tr>
 * <tr>
 * <td><b>DELETE LOGICAL NAME</b></td>
 * <td>This deletes a logical name from the server and all sessions logged into
 * it.</td>
 * </tr>
 * <tr>
 * <td><b>DELETE USER</b></td>
 * <td>This deletes a user from the database. This is not permitted if the user
 * is currently logged in.</td>
 * </tr>
 * <tr>
 * <td><b>QUIT</b></td>
 * <td>This is used to force a user session to exit, logging that user out.</td>
 * </tr>
 * <tr>
 * <td><b>ADD USER</b></td>
 * <td>This command adds a new user definition to the running server instance.</td>
 * </tr>
 * <tr>
 * <td><b>MODIFY USER</b></td>
 * <td>This command modifies a user definition in the running server instance.</td>
 * </tr>
 * <tr>
 * <td><b>LOAD</b></td>
 * <td>This loads user and logical name data from a disk file, containing XML
 * specifications for active accounts and logical names which are added to the
 * current server.</td>
 * </tr>
 * <tr>
 * <td><b>SAVE</b></td>
 * <td>This saves all current user and logical name definitions to an XML disk
 * file.</td>
 * </tr>
 * <tr>
 * <td><b>START</b></td>
 * <td>Start the server. Optionally specify a port number and logging level.</td>
 * </tr>
 * <tr>
 * <td><b>STOP</b></td>
 * <td>Stop the server, log out active users.</td>
 * </tr>
 * </table>
 * <p>
 * 
 * @author tom
 * @version 1.3 January 2009
 */

class ServerStatement extends Statement {

	private String defaultDatabaseName = JBasic.USER_DATA;

	/**
	 * Execute 'SERVER' statement. Parse the sub-verb (the word that follows
	 * <code>SERVER</code>, and uses it to specify specific user manager
	 * operations.
	 * 
	 * @param symbols
	 *            The symbol table to use to resolve identifiers, or null if no
	 *            symbols allowed.
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		if (JBasic.userManager == null) {
			JBasic.userManager = new UserManager(this.session, true);
		}

		/*
		 * These commands modify shared data structures; let's make sure it
		 * isn't possible for multiple threads to modify the user data at one
		 * time. Synchronize on the singular UserManager object.
		 */

		synchronized (JBasic.userManager) {

			if (JBasic.userManager.getSession() == null)
				JBasic.userManager.setSession(this.session);

			String verb = tokens.nextToken();

			/*
			 * There may be a space filling "SHOW" verb here. IF so, just eat
			 * it. The handlers invoked nextmust be the ones that allow the SHOW
			 * verb prefix.
			 */

			boolean fShow = false;
			if (verb.equals("SHOW")) {
				verb = tokens.nextToken();
				fShow = true;
			}

			/*
			 * Display list of logical names associated with the session.
			 */

			if (verb.equals("LOGICAL") & tokens.peek(0).equals("NAMES")) {
				verb = "LOGICALS";
				tokens.nextToken();
			}
			if (verb.equals("LOGICALS"))
				return showLogicalNames();

			/*
			 * Display active sessions
			 */
			if (verb.equals("SESSIONS"))
				return showSessions();

			/*
			 * List all known users and whether they are active or not.
			 */

			boolean displayOnlyActive = false;
			if (verb.equals("ACTIVE")) {
				displayOnlyActive = true;
				verb = tokens.nextToken();
			}

			if (verb.equals("LIST") || verb.equals("USERS")
					|| verb.equals("USER"))
				return showUsers(tokens, symbols, displayOnlyActive);

			/*
			 * Display info about server status.
			 */
			if (verb.equals("STATUS"))
				return showServerStatus(symbols);

			/*
			 * Everything that allows the SHOW and ACTIVE prefixes has been
			 * processed. If these flags are set at this point, there is a
			 * syntax error. ALL SUBCOMMANDS THAT DO NOT PERMIT 'SHOW' OR
			 * 'ACTIVE' MUST FOLLOW THIS POINT.
			 */
			if (displayOnlyActive || fShow)
				return new Status(Status.KEYWORD, verb);

			/*
			 * Generate code to setup directory structures, etc. for the active
			 * UserManager.
			 */
			if (verb.equals("SETUP")) {
				String pw = null;
				if( tokens.testNextToken(Tokenizer.STRING))
					pw = tokens.nextToken();
				return serverSetup(symbols, pw);
			}

			/*
			 * Grant or revoke a permission
			 */
			if (verb.equals("GRANT") | verb.equals("REVOKE"))
				return editUserPermissions(tokens, symbols, verb);

			/*
			 * Remove a user from the data.
			 */

			if (verb.equals("DELETE") | verb.equals("REMOVE"))
				return deleteUserRecord(tokens, symbols, verb);

			/*
			 * Terminate a user session that is active.
			 */

			if (verb.equals("QUIT"))
				return quitSession(tokens, symbols, verb);

			/*
			 * Add a new user to the data
			 */

			if (verb.equals("ADD") || verb.equals("MODIFY"))
				return editUserRecord(tokens, symbols, verb);

			/*
			 * Load an external file or an array of records into the user
			 * manager.
			 */
			if (verb.equals("LOAD"))
				return loadDatabase(tokens, symbols);

			/*
			 * Save the user manager state to a file.
			 */
			if (verb.equals("SAVE"))
				return saveDatabase(tokens, symbols);

			/*
			 * Start multi user mode.
			 */
			if (verb.equals("START"))
				return startServer(tokens, symbols);

			/*
			 * Stop multiuser mode
			 */
			if (verb.equals("STOP"))
				return stopServer(symbols);

			if (verb.equals("DEFINE"))
				return defineLogicalName(tokens, symbols);
			/*
			 * Nothing I've ever seen before, so error out.
			 */
			return status = new Status(Status.KEYWORD, verb);
		}
	}

	/**
	 * SERVER DEFINE statement handler
	 * 
	 * @param tokens
	 * @param symbols
	 * @param verb
	 * @param fShow
	 * @return
	 */
	private Status defineLogicalName(final Tokenizer tokens,
			final SymbolTable symbols) {
		{
			if (!session.hasPermission(Permissions.ADMIN_SERVER)) {
				JBasic.log.permission(session, Permissions.ADMIN_SERVER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_SERVER);
			}
			if (!tokens.testNextToken(Tokenizer.IDENTIFIER))
				return new Status(Status.UNKLN, tokens.nextToken());
			String logicalName = tokens.nextToken();
			
			byteCode = new ByteCode(session,this);
			
			byteCode.add(ByteCode._STRING, logicalName);
			
			if (!tokens.assumeNextSpecial("="))
				return new Status(Status.ASSIGNMENT);
			Expression pathExpression = new Expression(this.session);
			status = pathExpression.compile(byteCode, tokens);
			if( status.failed())
				return status;
			byteCode.add(ByteCode._SERVER, OpSERVER.SERVER_DEFINE );
			
			return byteCode.run(symbols, 0);

		}
	}

	/**
	 * @return
	 */
	private Status showServerStatus(SymbolTable symbols) {
		byteCode = new ByteCode(session,this);
		byteCode.add(ByteCode._SERVER, OpSERVER.SERVER_SHOW_STATUS );
		return byteCode.run(symbols, 0);
	}

	/**
	 * SERVER STOP handler
	 * 
	 * @param symbols
	 * @param verb
	 * @param fShow
	 * @return
	 */
	private Status stopServer(final SymbolTable symbols) {
		{

			if (!session.hasPermission(Permissions.ADMIN_SERVER)) {
				JBasic.log.permission(session, Permissions.ADMIN_SERVER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_SERVER);
			}
			ByteCode bc = new ByteCode(session);
			bc.add(ByteCode._THREAD, OpTHREAD.STOP_SERVER);
			return bc.run(symbols, 0);
		}
	}

	/**
	 * @param symbols runtime symbol table
	 * @param pw the password, if any, to assign to the ADMIN account
	 * @return status indicating success
	 */
	private Status serverSetup(final SymbolTable symbols, String pw) {
		{
			if (!session.hasPermission(Permissions.ADMIN_SERVER)) {
				JBasic.log.permission(session, Permissions.ADMIN_SERVER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_SERVER);
			}
			ByteCode bc = new ByteCode(session);
			if( pw != null)
				bc.add(ByteCode._THREAD, OpTHREAD.USER_SETUP, pw);
			else
				bc.add(ByteCode._THREAD, OpTHREAD.USER_SETUP);
			return bc.run(symbols, 0);
		}
	}

	/**
	 * @return
	 */
	private Status showLogicalNames() {
		{
			if (!session.hasPermission(Permissions.FS_NAMES)) {
				JBasic.log.permission(session, Permissions.FS_NAMES);
				return new Status(Status.SANDBOX, Permissions.FS_NAMES);
			}
			int count = 0;
			Iterator keys = session.getNamespace().list.keySet().iterator();
			while (keys.hasNext()) {
				String name = (String) keys.next();
				String path = session.getNamespace().getPhysicalPath(name);
				if (count == 0)
					session.stdout.println("LOGICAL NAMES:");
				count++;
				session.stdout.println("  " + Utility.pad(name, 8) + "  \""
						+ path + "\"");
			}
			if (count == 0)
				session.stdout.println("No logical names defined");
			return new Status();
		}
	}

	/**
	 * @return
	 */
	private Status showSessions() {
		{
			if (!session.hasPermission(Permissions.ADMIN_SERVER)) {
				JBasic.log.permission(session, Permissions.ADMIN_SERVER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_SERVER);
			}
			int count = 0;
			if (session.getString("SYS$MODE").equals("SINGLEUSER")) {
				session.stdout.println("Server not active");
				return new Status();
			}
			if (JBasic.activeSessions == null || JBasic.activeSessions.size() == 0) {
				session.stdout.println("No users have connected.");
				return new Status();
			}
			
			StringBuffer m = new StringBuffer();
			m.append(Utility.pad("SESSION",12));
			m.append(Utility.pad("USERNAME",12));
			m.append(Utility.pad("LOGIN TIME",30));
			m.append(Utility.pad("PROGRAM", 12));
			m.append(Utility.pad("SCNT", -7));
			m.append(Utility.pad("BCNT", -7));
			session.stdout.println(m.toString());

			Iterator i = JBasic.activeSessions.keySet().iterator();
			while (i.hasNext()) {
				String key = (String) i.next();
				JBasic userSession = JBasic.activeSessions.get(key);
				if (userSession == null)
					continue;
				count++;
				String un = userSession.getString("SYS$USER");
				String lt = userSession.getString("SYS$LOGIN_TIME");
				String si = userSession.getString("SYS$INSTANCE_NAME");

				m = new StringBuffer();
				m.append(Utility.pad(si.toUpperCase(), 12));
				m.append(Utility.pad(un.toUpperCase(), 12));
				m.append(Utility.pad(lt, 30));
				
				//session.stdout.println(" " + si.toUpperCase());
				//session.stdout.println("   USERNAME:     " + un.toUpperCase());
				//session.stdout.println("   LOGIN TIME:   " + lt);
				String pgmName = "<none>";
				if (userSession.programs.getCurrent() != null)
					pgmName = userSession.programs.getCurrent().getName();
				
				m.append(Utility.pad(pgmName, 12));
				m.append(Utility.pad(Integer.toString(userSession.statementsExecuted), -7));
				m.append(Utility.pad(Integer.toString(userSession.instructionsExecuted), -7));
				//session.stdout.println("   CURRENT PGM:  " + pgmName);
				//session.stdout.println("   STMTS EXEC:   "
				//		+ userSession.statementsExecuted);
				//session.stdout.println("   BCODE EXEC:   "
				//		+ userSession.instructionsExecuted);
				session.stdout.println(m.toString());
			}

			if (count == 0)
				session.stdout.println("No active sessions");
			else
				this.session.stdout.println(Integer.toString(count)
						+ " session" + (count == 1 ? "" : "s"));
			return new Status();

		}
	}

	/**
	 * GRANT and REVOKE statement handler
	 * 
	 * @param tokens
	 * @param symbols
	 * @param verb
	 * @param exp
	 * @param fShow
	 * @return
	 */
	private Status editUserPermissions(final Tokenizer tokens,
			final SymbolTable symbols, String verb) {
		{
			if (!session.hasPermission(Permissions.ADMIN_USER)) {
				JBasic.log.permission(session, Permissions.ADMIN_USER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_USER);
			}
			boolean state = verb.equals("GRANT");
			String userName = null;
			String permission = null;
			Vector<String> permissionList = new Vector<String>();
			Expression exp = new Expression(session);
			while (true) {

				if (tokens.assumeNextToken("USER")) {
					Value d = exp.evaluate(tokens, symbols);
					if (d == null)
						return exp.status;
					userName = d.getString();
					continue;
				}

				boolean hasALL = false;

				if (tokens.assumeNextToken("PERMISSION")) {
					Value d = exp.evaluate(tokens, symbols);
					if (d == null)
						return exp.status;
					permission = d.getString().toUpperCase();
					if (permission.equals("ALL"))
						hasALL = true;
					if (!hasALL & !Permissions.valid(permission))
						return new Status(Status.INVPERM, permission);
					permissionList.add(permission);
					if (!hasALL)
						continue;
				}

				/*
				 * A list is permitted as well.
				 */
				if (tokens.assumeNextToken("PERMISSIONS")) {
					while (true) {
						Value d = exp.evaluate(tokens, symbols);
						if (d == null)
							return exp.status;
						permission = d.getString().toUpperCase();
						if (permission.equals("ALL"))
							hasALL = true;
						if (!hasALL & !Permissions.valid(permission))
							return new Status(Status.INVPERM, permission);
						permissionList.add(permission);
						if (!tokens.assumeNextSpecial(","))
							break;
						;
					}
					if (!hasALL)
						continue;
				}
				break;

			}

			if (userName == null)
				return new Status(Status.EXPCLAUSE, "USERNAME");
			if (permission == null)
				return new Status(Status.EXPCLAUSE, "PERMISSION");

			
			/*
			 * Run the list of permissions we have and set them for this user.  Note
			 * special case; if we are the controlling session (as opposed to a user) then
			 * we turn off the mask; that is, any permission can be set as there is no
			 * mask that prevents it.
			 */
			
			for (int ix = 0; ix < permissionList.size(); ix++) {
				permission = permissionList.get(ix);
				if( JBasic.firstSession == session)
					permission = "!" + permission;
				JBasic.userManager.setPermission(userName, permission, state);
			}
			return new Status();
		}
	}

	/**
	 * DELETE USER statement handler
	 * 
	 * @param tokens
	 * @param symbols
	 * @param verb
	 * @param exp
	 * @param fShow
	 * @return
	 */
	private Status deleteUserRecord(final Tokenizer tokens,
			final SymbolTable symbols, String verb) {
		{

			/*
			 * IS this a DELETE LOGICAL [NAME] "foo" command?
			 */
			if (tokens.assumeNextToken("LOGICAL")) {
				tokens.assumeNextToken("NAME");

				if (!session.hasPermission(Permissions.FS_NAMES)) {
					JBasic.log.permission(session, Permissions.FS_NAMES);
					return new Status(Status.SANDBOX, Permissions.FS_NAMES);
				}
				String name = tokens.nextToken();
				if (session.getNamespace().list.remove(name) == null)
					return new Status(Status.UNKLN, name);
				return new Status();
			}

			/*
			 * Nope, looks like a DELETE "user" command
			 */
			if (!session.hasPermission(Permissions.ADMIN_USER)) {
				JBasic.log.permission(session, Permissions.ADMIN_USER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_USER);
			}
			Expression exp = new Expression(session);
			Value d = exp.evaluate(tokens, symbols);
			if (d == null)
				return exp.status;
			String key = d.getString();
			if (JBasic.userManager.isActive(key)) {
				return new Status(Status.ACTUSER, key);
			}
			return JBasic.userManager.deleteUser(key);

		}
	}

	/**
	 * @param tokens
	 * @param symbols
	 * @param verb
	 * @param exp
	 * @param fShow
	 * @return
	 */
	private Status quitSession(final Tokenizer tokens,
			final SymbolTable symbols, String verb ) {
		{
			if (!session.hasPermission(Permissions.ADMIN_USER)) {
				JBasic.log.permission(session, Permissions.ADMIN_USER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_USER);
			}
			if (!session.hasPermission(Permissions.ADMIN_SERVER)) {
				JBasic.log.permission(session, Permissions.ADMIN_SERVER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_SERVER);
			}
			Expression exp = new Expression(session);
			Value d = exp.evaluate(tokens, symbols);
			if (d == null)
				return exp.status;
			String key = d.getString().toUpperCase();
			if( JBasic.activeSessions == null )
				return new Status(Status.NOUSER, "instance id " + key);
			JBasic userSession = JBasic.activeSessions.get(key);
			if (userSession != null) {
				userSession.setAbort(true);
				userSession.running(false);
				JBasic.activeSessions.remove(key);
				return new Status();
			}
			return new Status(Status.NOUSER, "instance id " + key);

		}
	}

	/**
	 * ADD USER and DELETE USER statement handler
	 * 
	 * @param tokens
	 * @param symbols
	 * @param verb
	 * @param exp
	 * @param fShow
	 * @return
	 */
	private Status editUserRecord(final Tokenizer tokens,
			final SymbolTable symbols, String verb) {
		{
			if (!session.hasPermission(Permissions.ADMIN_USER)) {
				JBasic.log.permission(session, Permissions.ADMIN_USER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_USER);
			}
			Value userRecord = new Value(Value.RECORD, null);
			Value d = null;
			Expression exp = new Expression(session);
			boolean pwSet = false;
			
			while (true) {
				if (tokens.endOfStatement())
					break;
								
				if (tokens.assumeNextToken("USER")) {
					d = exp.evaluate(tokens, symbols);
					if (d == null)
						return exp.status;
					userRecord.setElement(d.getString(), "USER");
					continue;
				}

				if( tokens.assumeNextToken("NOPASSWORD")) {
					userRecord.setElement("*", "PASSWORD");
					pwSet = true;
					continue;
				}
				if (tokens.assumeNextToken("PASSWORD")) {
					d = exp.evaluate(tokens, symbols);
					if (d == null)
						return exp.status;
					SimpleCipher crypto = new SimpleCipher(session);
					String hashedPassword = crypto.encryptedString(d
							.getString(), UserManager.secret);
					userRecord.setElement(hashedPassword, "PASSWORD");
					pwSet = true;
					continue;
				}

				if (tokens.assumeNextToken("HOME")) {
					d = exp.evaluate(tokens, symbols);
					if (d == null)
						return exp.status;
					userRecord.setElement(d.getString(), "HOME");
					continue;
				}

				if (tokens.assumeNextToken("WORKSPACE")) {
					d = exp.evaluate(tokens, symbols);
					if (d == null)
						return exp.status;
					userRecord.setElement(d.getString(), "WORKSPACE");
					continue;
				}

				if (tokens.assumeNextToken("ACCOUNT")) {
					d = exp.evaluate(tokens, symbols);
					if (d == null)
						return exp.status;
					userRecord.setElement(d.getString(), "ACCOUNT");
					continue;
				}

				if (tokens.assumeNextToken("NAME")) {
					d = exp.evaluate(tokens, symbols);
					if (d == null)
						return exp.status;
					userRecord.setElement(d.getString(), "NAME");
					continue;
				}

				return new Status(Status.KEYWORD, tokens.nextToken());

			}
			boolean createUser = verb.equals("ADD");
			if( createUser && !pwSet) {
				String gpw = JBasic.userManager.generatePassword(3);
				userRecord.setElement(gpw, "PASSWORD");
				JBasic.log.printMessage("SERVERGENPW", gpw);
			}
			
			return JBasic.userManager.loadUserRecord(userRecord, createUser);
		}
	}

	/**
	 * SERVER LOAD statement handler.
	 * 
	 * @param tokens
	 * @param symbols
	 * @param verb
	 * @param exp
	 * @param fShow
	 * @return
	 */
	private Status loadDatabase(final Tokenizer tokens, final SymbolTable symbols) {
		{
			if (!session.hasPermission(Permissions.ADMIN_USER)) {
				JBasic.log.permission(session, Permissions.ADMIN_USER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_USER);
			}
			if (tokens.endOfStatement()) {
				try {
					JBasic.userManager.load(defaultDatabaseName);
				} catch (JBasicException e) {
					return e.getStatus();
				}
				return new Status();
			}
			Expression exp = new Expression(session);
			Value source = exp.evaluate(tokens, symbols);
			if (exp.status.failed())
				return exp.status;
			if (source.isType(Value.ARRAY))
				try {
					JBasic.userManager.load(source);
				} catch (JBasicException e) {
					return e.getStatus();
				}
			else
				try {
					JBasic.userManager.load(source.getString());
				} catch (JBasicException e) {
					return e.getStatus();
				}
			return new Status();
		}
	}

	/**
	 * @param tokens
	 * @param symbols
	 * @param verb
	 * @param exp
	 * @param fShow
	 * @return
	 */
	private Status saveDatabase(final Tokenizer tokens, final SymbolTable symbols) {
		{

			if (!session.hasPermission(Permissions.ADMIN_USER)) {
				JBasic.log.permission(session, Permissions.ADMIN_USER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_USER);
			}
			if (tokens.endOfStatement()) {
				try {
					JBasic.userManager.save(defaultDatabaseName);
				} catch (JBasicException e) {
					return e.getStatus();
				}
				return new Status();
			}

			Expression exp = new Expression(session);
			Value destination = exp.evaluate(tokens, symbols);
			if (exp.status.failed())
				return exp.status;
			try {
				JBasic.userManager.save(destination.getString());
			} catch (JBasicException e) {
				return e.getStatus();
			}
			return new Status();
		}
	}

	/**
	 * @param tokens
	 * @param symbols
	 * @param exp
	 * @param displayOnlyActive
	 * @return
	 */
	private Status showUsers(final Tokenizer tokens, final SymbolTable symbols,
			boolean displayOnlyActive) {
		{
			int count = 0;
			String match = null;
			if (!tokens.endOfStatement()) {
				if (displayOnlyActive)
					return new Status(Status.SYNTAX, "ACTIVE not permitted");
				Expression exp = new Expression(session);
				Value uv = exp.evaluate(tokens, symbols);
				if (uv == null)
					return exp.status;
				match = uv.getString().toUpperCase();
			}
			if (!session.hasPermission(Permissions.ADMIN_USER)) {
				JBasic.log.permission(session, Permissions.ADMIN_USER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_USER);
			}
			Value userList = JBasic.userManager.userList();
			if (userList == null)
				return new Status(Status.FAULT, "user manager error");
			if (userList.size() == 0) {
				session.stdout.println("There are no users defined");
				return new Status();
			}
			for (int ix = 1; ix <= userList.size(); ix++) {
				Value user = userList.getElement(ix);
				String userName = user.getElement("USER").getString();
				if (match != null)
					if (!match.equals(userName))
						continue;

				boolean isActive = user.getElement("ACTIVE").getBoolean();
				if (displayOnlyActive && !isActive)
					continue;
				count++;
				session.stdout.println(Utility.pad(" " + userName, 30));

				session.stdout.print("   LAST LOGIN    "
						+ JBasic.userManager.lastLogin(userName));
				session.stdout.println(isActive ? " (ACTIVE)" : "");
				session.stdout.println("   ACCOUNT:      "
						+ user.getString("ACCOUNT"));
				session.stdout.println("   FULL NAME:    "
						+ user.getString("NAME"));
				session.stdout.println("   HOME DIR:     "
						+ user.getString("HOME"));
				session.stdout.println("   WORKSPACE:    "
						+ user.getString("WORKSPACE"));

				Value permissions = JBasic.userManager.permissions(userName);
				if (permissions.size() > 0)
					session.stdout.println("   PERMISSIONS:  "
							+ permissions.toString());
				if (isActive) {
					Iterator i = JBasic.activeSessions.keySet().iterator();
					while (i.hasNext()) {
						String sessionID = (String) i.next();
						JBasic userSession = JBasic.activeSessions
								.get(sessionID);
						if (!userSession.getUserIdentity().getName()
								.equals(userName))
							continue;
						String pgmName = "<none active>";
						if (userSession.programs.getCurrent() != null)
							pgmName = userSession.programs.getCurrent()
									.getName();
						session.stdout.print("     SESSION:      " + sessionID);
						session.stdout.print(", PGM=" + pgmName);
						session.stdout.print(", STMTS="
								+ userSession.statementsExecuted);
						session.stdout.println(", INSTS="
								+ userSession.instructionsExecuted);
					}
				}
				session.stdout.println();
			}
			if (match != null) {
				if (count == 0)
					return new Status(Status.NOUSER, match);
				return new Status();
			}
			StringBuffer msg = new StringBuffer();
			if (count == 0)
				msg.append("No");
			else
				msg.append(Integer.toString(count));
			if (displayOnlyActive)
				msg.append(" active");
			msg.append(" user");
			if (count != 1)
				msg.append("s");
			msg.append(".");
			session.stdout.println(msg.toString());
			return new Status();
		}
	}

	/**
	 * SERVER START handler
	 * 
	 * @param tokens
	 * @param symbols
	 * @param verb
	 * @param exp
	 * @param fShow
	 * @return
	 */
	private Status startServer(final Tokenizer tokens, final SymbolTable symbols) {
		{
			if (!session.hasPermission(Permissions.ADMIN_SERVER)) {
				JBasic.log.permission(session, Permissions.ADMIN_SERVER);
				return new Status(Status.SANDBOX, Permissions.ADMIN_SERVER);
			}

			if (!session.getString("SYS$MODE").equals("SINGLEUSER")) {
				return new Status(Status.SERVER, new Status(Status.RUNNING));
			}

			ByteCode bc = new ByteCode(session);
			if (symbols.getInteger("SYS$PORT") <= 0)
				try {
					symbols.insert("SYS$PORT", 6100);
				} catch (JBasicException e) {
					return e.getStatus();
				}

			String shell = "Default Shell";
			boolean hasShell = false;
			Expression exp = new Expression(session);
			boolean isDetached = false;
			
			while (!tokens.endOfStatement()) {

				/*
				 * For legacy support, allow a stand-alone integer to describe
				 * the active port.
				 */
				if (tokens.testNextToken(Tokenizer.INTEGER)) {
					status = exp.compile(bc, tokens);
					if (status.failed())
						return status;
					bc.add(ByteCode._STOR, -1, "SYS$PORT");
					continue;
				}

				if (tokens.assumeNextToken("DETACHED")) {
					isDetached = true;
					continue;
				}
				if (tokens.assumeNextToken("SHELL")) {
					if (!tokens.assumeNextSpecial("="))
						return new Status(Status.ASSIGNMENT);
					Value vShell = exp.evaluate(tokens, symbols);
					if (vShell == null || exp.status.failed())
						return exp.status;
					shell = vShell.getString().toUpperCase();
					hasShell = true;
					continue;
				}
				/*
				 * PORT=n specifies the port number to use for the server, and
				 * also sets the SYS$PORT system variable.
				 */
				if (tokens.assumeNextToken("PORT")) {
					if (!tokens.assumeNextSpecial("="))
						return new Status(Status.ASSIGNMENT);

					status = exp.compile(bc, tokens);
					if (status.failed())
						return status;
					bc.add(ByteCode._CLEAR, OpCLEAR.CLEAR_SYMBOL_ALWAYS, "SYS$PORT");
					bc.add(ByteCode._STOR, "SYS$PORT");
					continue;
				}

				/*
				 * LOGGING=n sets the logging level
				 */
				if (tokens.assumeNextToken(new String[] { "LOG", "LOGGING" })) {
					if (!tokens.assumeNextSpecial("="))
						return new Status(Status.ASSIGNMENT);

					status = exp.compile(bc, tokens);
					if (status.failed())
						return status;
					bc.add(ByteCode._LOGGING);
					continue;
				}
				return new Status(Status.KEYWORD, tokens.nextToken());
			}

			if (hasShell && !JBasic.userManager.isUser(shell)) {
				this.session.run("SERVER ADD USER \"" + shell
						+ "\" PASSWORD \"none\" ACCOUNT \"AUTO RUN\"");
			}

			bc.add(ByteCode._CLEAR, OpCLEAR.CLEAR_SYMBOL_ALWAYS, "$INIT_SHELL");
			bc.add(ByteCode._STRING, shell);
			bc.add(ByteCode._STOR, -2, "$INIT_SHELL");

			bc.add(ByteCode._LOADREF, "SYS$PORT");
			bc.add(ByteCode._CVT, Value.INTEGER);
			bc.add(ByteCode._THREAD, OpTHREAD.START_SERVER);
			bc.add(ByteCode._THREAD, OpTHREAD.USER_SETUP);
			if( isDetached )
				bc.add(ByteCode._THREAD, OpTHREAD.DETACH);
			
			return bc.run(symbols, 0);
		}
	}
}

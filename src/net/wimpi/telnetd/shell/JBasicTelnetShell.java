//License
/***
 * Java TelnetD library (embeddable telnet daemon)
 * Copyright (c) 2000-2005 Dieter Wimberger 
 * All rights reserved.
 *
 * Modifications to support JBasic performed by Tom Cole.  These modifications
 * in no way are intended or permitted to modify the license set for here.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the author nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ***/

package net.wimpi.telnetd.shell;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.net.Connection;
import net.wimpi.telnetd.net.ConnectionEvent;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.LockManager;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Loader;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBFInput;
import org.fernwood.jbasic.runtime.JBFOutput;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * This class is an example implmentation of a Shell.<br>
 * It is used for testing the system.<br>
 * At the moment you can see all io toolkit classes in action, pressing "t" at
 * its prompt (instead of the enter, which is requested for logging out again).
 * 
 * The original author of this code was Dieter Wimberger. This has been modified
 * for use with JBasic by Tom Cole.
 * 
 * @author Tom Cole
 * @version 2.1 (October 13, 2007)
 */
public class JBasicTelnetShell implements Shell {

	private static JBFOutput log = JBasic.log;

	private Connection m_Connection;

	private BasicTerminalIO m_IO;

	/**
	 * Method that runs a shell
	 * 
	 * @param con
	 *            Connection that runs the shell.
	 */
	public void run(Connection con) {
		String un = "default";
		String pw = "default";
		JBasic session = new JBasic();
		try {
			m_Connection = con;
			// mycon.setNextShell("nothing");
			m_IO = m_Connection.getTerminalIO();
			// dont forget to register listener
			m_Connection.addConnectionListener(this);
			session.addEvent("$Connection listener established");
			
			/*
			 * Store the source port number we were started from.
			 */
			
			String portStr = (String) this.m_Connection.getM_ConnectionData().getEnvironment().get("INIT");
			String initPgm = null;
			if( portStr != null ) {
				session.globals().insert("SYS$INITPORT", portStr);
				initPgm = session.getString("$INIT_SHELL").toUpperCase();
				if( initPgm != null && initPgm.equalsIgnoreCase("DEFAULT SHELL"))
					initPgm = null;
				else
					un = initPgm;
			}
			
			int port = m_Connection.getConnectionData().getPort();
			String from_addr = m_Connection.getConnectionData()
					.getHostAddress();
			String from_name = m_Connection.getConnectionData().getHostName();
			/*
			 * Prepare a new shell to host this connection.
			 */

			session.setStdin(new JBFInput(session));
			session.stdin().openTerminal(session, m_IO);

			session.stdout = new JBFOutput(session);
			session.stdout.openTerminal(m_IO);
			session.addEvent("$Socket shell sessions opened");
			
			SymbolTable globals = session.globals();

			Value mode = globals.localReference("SYS$MODE");
			String modeName = "REMOTE" + ( initPgm == null ? "USER" : "PGM-" + initPgm);
			if (mode == null)
				globals.insertReadOnly("SYS$MODE", new Value(modeName));
			else
				mode.setString(modeName);

			session.enableSandbox(true);
			

			/*
			 * Prompt for username and password
			 */

			String prompt = new Status(Status._LOGIN)
					.getMessage(JBasic.userManager.getSession());

			
			if (initPgm == null & JBasic.userManager.requireAuthentication()) {
				session.stdout.println(prompt);
				session.stdout.println();
				for (int tries = 0; tries < 3; tries++) {
					session.stdout.print("Username: ");
					un = session.stdin().read().toLowerCase();

					if( !JBasic.userManager.hasPassword(un)) {
						session.stdout.println();
						break;
					}
					
					session.stdout.print("Password: ");
					session.stdin().setEcho(false);
					pw = session.stdin().read();
					session.stdin().setEcho(true);
					session.stdout.println();

					if (JBasic.userManager.authentic(un, pw)) {
						session.stdout.println();
						break;
					}
					session.stdout.println("Invalid username or password");
					if( un.length() == 0)
						un = "<none>";
					log.info("Login password failure, user=" + un);
					un = null;
					pw = null;
				}
			}

			if (pw == null) {
				session.stdout.close();
				session.stdin().close();
				return;
			}
			
			session.addEvent("$User " + un + " logged in");
			
			JBasic.userManager.active(un, session, true);
			log.info("Multiuser LOGIN, username=" + un);

			/*
			 * Any program already in memory belongs to someone else, possibly
			 * the system... so protect them all.
			 */

			Iterator pgms = session.programs.iterator();
			while (pgms.hasNext()) {
				Program pgm = (Program) pgms.next();
				pgm.protect();
			}

			session.addEvent("$Program library protected");
			
			/*
			 * The user must have a location to read and write from/to if they
			 * have FILE_IO or FILE_IO privileges.
			 */

			if (session.hasPermission(Permissions.FILE_IO) ||
					session.hasPermission(Permissions.FILE_READ) ||
					session.hasPermission(Permissions.FILE_WRITE) ||
					session.hasPermission(Permissions.DIR_IO)) {
				String sep = System.getProperty("file.separator");
				String testName = session.getUserIdentity().getHome() + sep
						+ ".JBTest";
				boolean writable = false;
				try {
					File testFile = new File(testName);
					testFile.createNewFile();
					writable = testFile.canWrite();
					testFile.delete();
				} catch (IOException e) {
					writable = false;
				}

				if (!writable) {
					log.error("User " + session.getUserIdentity().getName() + " has no valid home directory");
					session.stdout
							.println("WARNING: Your home directory is not properly set up.");
				}
			}
			/*
			 * Set up the workspace name so it uniquely identifies this user.
			 * Attempt to load this user's session.
			 */
			session.setWorkspaceName(JBasic.userManager.getWorkspace(un));
			Value wsName = globals.localReference("SYS$WORKSPACE");
			wsName.setString(session.getWorkspaceName());

			String fsName = JBasic.userManager.makeFSPath(session, session.getWorkspaceName());
			if( fsName != null )
				Loader.loadFile(session, fsName);
			
			session.programs.setCurrent(null);
			session.addEvent("$Workspace directory established");
			
			/*
			 * Set up the remaining per-user state and we're ready to go.
			 */

			String savePrompt = "There are unsaved programs.  You must use the SAVE WORKSPACE command to permanently\nstore them to disk. Are you sure you want to QUIT [y/n] ?";
			globals.insertReadOnly("SYS$SAVEPROMPT", new Value(savePrompt));

			globals.deleteAlways("SYS$HOME");
			globals.insertReadOnly("SYS$HOME", new Value(JBasic.userManager
					.home(un)));
			Value user = globals.localReference("SYS$USER");
			user.setString(un);
			globals.insertReadOnly("SYS$ARGS", new Value(Value.ARRAY, null));
			session.initializeAboutProgram();
			globals.insertReadOnly("SYS$PORT", new Value(port));
			globals.insertReadOnly("SYS$HOST_ADDR", new Value(from_addr));
			globals.insertReadOnly("SYS$HOST_NAME", new Value(from_name));
			globals.insertReadOnly("SYS$INITCMD", new Value(""));
			session.run("run $MAIN");
			if (session.programs.find("$LOGIN") != null)
				session.run("run $LOGIN");
			session.addEvent("$MAIN program completed");
			
			/*
			 * After all this silliness, make sure there isn't a current
			 * program left lingering around.
			 */
			session.programs.setCurrent(null);
			Value v = globals.localReference("SYS$CURRENT_PROGRAM");
			v.setString("");
			
			/*
			 * We must lock down the "SYS$PACKAGES" variable, if it exists.  This
			 * is needed to prevent a shell user from modifying this variable to
			 * load arbitrary JBasic statement handlers.
			 */
			
			Value packageArray= session.globals().findReference(JBasic.PACKAGES, false);
			if( packageArray == null )
				session.globals().insert(JBasic.PACKAGES, new Value(Value.ARRAY, null));
			session.globals().markReadOnly(JBasic.PACKAGES);

			
			/*
			 * Run the user shell as long as needed.
			 */
			
			Status status = null;
			
			if( initPgm == null )
				status = session.shell(null, "[" + un + "] BASIC> ");
			else
				status = session.run("RUN " + initPgm);
			
			/*
			 * User has quit from the sessions, disconnect him.
			 */
			JBasic.userManager.active(un, session, false);
			log.info("Multiuser LOGOUT, username=" + un + ", status=" + status);
			session.addEvent("$Logout of user " + un);
			
			/*
			 * Be sure we don't leave any dangling locks that could cause
			 * deadlock.
			 */
			LockManager.releaseAll(session);
			
		} catch (Exception ex) {
			log.error("run()", ex);
			JBasic.userManager.active(un, session, false);
		}
	}// run

	// this implements the ConnectionListener!
	public void connectionTimedOut(ConnectionEvent ce) {
		try {
			m_IO.write("CONNECTION_TIMEDOUT");
			m_IO.flush();
			// close connection
			m_Connection.close();
		} catch (Exception ex) {
			log.error("connectionTimedOut()", ex);
		}
	}// connectionTimedOut

	public void connectionIdle(ConnectionEvent ce) {
		try {
			m_IO.write("CONNECTION_IDLE");
			m_IO.flush();
		} catch (IOException e) {
			log.error("connectionIdle()", e);
		}

	}// connectionIdle

	public void connectionLogoutRequest(ConnectionEvent ce) {
		try {
			m_IO.write("CONNECTION_LOGOUTREQUEST");
			m_IO.flush();
		} catch (Exception ex) {
			log.error("connectionLogoutRequest()", ex);
		}
	}// connectionLogout

	public void connectionSentBreak(ConnectionEvent ce) {
		try {
			m_IO.write("CONNECTION_BREAK");
			m_IO.flush();
		} catch (Exception ex) {
			log.error("connectionSentBreak()", ex);
		}
	}// connectionSentBreak

	/**
	 * Factory for creating a new instance of a user shell.
	 * 
	 * @return an instance of the JBasic remote user shell.
	 */
	public static Shell createShell() {
		return new JBasicTelnetShell();
	}

}// class JBasicTelnetShell

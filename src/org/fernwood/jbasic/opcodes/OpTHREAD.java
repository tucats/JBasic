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
package org.fernwood.jbasic.opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import net.wimpi.telnetd.BootException;
import net.wimpi.telnetd.TelnetD;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.LockManager;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicThread;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * @author cole
 * 
 */
public class OpTHREAD extends AbstractOpcode {

	/**
	 * Subcommand to list active locks.
	 */
	
	public final static int LIST_LOCKS = 9;
	
	/**
	 * Subcommand to do UserManager setup - create any directories that
	 * do not already exist to support User Manager activities.
	 */
	
	public final static int USER_SETUP = 8;
	
	/**
	 * Subcommand to set the current user's password using the instruction
	 * string value if present else the string value on top of the stack.
	 */
	public final static int SET_PASSWORD = 7;
	
	/**
	 * Subcommand to stop the multi-user server mode.
	 */
	public final static int STOP_SERVER = 6;
	
	/**
	 * Subcommand to start the multi-user server mode
	 */
	public final static int START_SERVER = 5;

	/**
	 * Subcommand to release Thread() objects and structures that have
	 * completed execution.
	 */
	public static final int RELEASE_THREADS = 4;

	/**
	 * Subcommand to list active threads.
	 */
	public static final int LIST_THREADS = 3;


	/**
	 * Subcommand to execute command in string on top of stack as a thread.
	 */
	public static final int EXEC_THREAD = 1;

	/**
	 * Subcommand to stop a thread
	 */
	public final static int STOP_THREAD = 0;

	/**
	 * When in SERVER mode, this indicates that the main thread should just
	 * wait on completion of the server; there is no more command activity
	 * to be expected on the console until the server is completed.
	 */
	
	public static final int DETACH = 10;

	/**
	 * This command re-attaches us to the command prompt after the next
	 * interval.
	 */
	public static final int ATTACH = 11;

	
	
	/**
	 * Execute the opcode.
	 * @param env The execution environment
	 * @throws JBasicException if the instruction opcode is invalid, or an
	 * implicit parameter is incorrect.
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final int subCommand = env.instruction.integerOperand;

		if (subCommand != SET_PASSWORD ) 
			env.session.checkPermission(Permissions.THREADS);

		switch( subCommand ) {
					
		case USER_SETUP:
			String adminPW = null;
			if( env.instruction.stringValid)
				adminPW = env.instruction.stringOperand;
			JBasic.userManager.setup(adminPW);
			break;

		case SET_PASSWORD:			
			setPassword(env);
			break;

		case STOP_SERVER:
			stopServer(env);
			break;

		case START_SERVER:
			startServer(env);
			break;

		case STOP_THREAD:
			stopThread(env);
			break;

		case LIST_THREADS:
			showThreads(env);
			break;

		case RELEASE_THREADS:
			releaseThreads(env);
			break;

		case EXEC_THREAD:
			startThread(env);
			break;
		
		case LIST_LOCKS:
			listLocks(env);
			break;
			
		case DETACH:
			JBasic.firstSession.detached = true;
			break;
		case ATTACH:
			JBasic.firstSession.detached = false;
			break;
			
		default:
			throw new JBasicException(Status.FAULT,
				new Status(Status.INVOPARG, subCommand));
		}
		
	}

	/**
	 * List the active lock table, as a result of the SHOW LOCKS command
	 * @param env
	 */
	private void listLocks(InstructionContext env) {
		
		Value lockList = LockManager.list();
		int count = lockList.size();
		
		if( count == 0 )
			env.session.stdout.println("No locks defined");
		else {
			StringBuffer output = new StringBuffer();
			output.append(Integer.toString(count));
			output.append(" lock");
			if( count > 1 )
				output.append('s');
			output.append(" defined:");
			env.session.stdout.println(output.toString());
			env.session.stdout.println("   LOCK       OWNER           Hold/Wait");
			for( int ix = 1; ix <= count; ix++ ) {
				Value element = lockList.getElement(ix);
				output = new StringBuffer();
				output.append("   ");
				boolean isMine = element.getElement("ISMINE").getBoolean();
				if( isMine )
					output.append('*');
				else
					output.append(' ');
				output.append(Utility.pad(element.getElement("NAME").getString(), 10));
				output.append(Utility.pad(element.getElement("OWNER").getString(), 16));
				output.append("  ");
				if( isMine ) {
					output.append('(');
					output.append(element.getElement("HOLDCOUNT").getString());
					output.append('/');
					output.append(element.getElement("WAITCOUNT").getString());
					output.append(')');
				}
				else 
					output.append(" n/a");
				env.session.stdout.println(output.toString());
			}
		}
		
	}

	/**
	 * Start a thread and give it a command string taken from the stack
	 * @param env The instruction being executed.
	 * @param session The session that will be the thread parent.
	 * @throws JBasicException if a stack overflow occurs or an error setting
	 * up the thread environment occurs.
	 */
	private void startThread(final InstructionContext env)
			throws JBasicException {
		final String tname = env.instruction.stringOperand;
		final String cmd = env.pop().getString();
		final JBasic session = env.session;

		final JBasicThread newThread = new JBasicThread(session, cmd);
		final String instanceName = newThread.getID();

		if (tname != null)
			env.localSymbols.insert(tname, instanceName);
		
		session.getChildThreads().put(instanceName, newThread);

		newThread.start();
		return;
	}

	/**
	 * Scan the list of threads that are owned by the current session, and
	 * release (delete) the information about those threads that have successfully
	 * completed execution.
	 * @param env
	 * @throws JBasicException 
	 */
	private void releaseThreads(final InstructionContext env) throws JBasicException {
		Iterator i = env.session.getChildThreads().values().iterator();
		while (i.hasNext()) {
			JBasicThread t = (JBasicThread) i.next();
			final String sts = t.getID();

			if (!t.isAlive())
				try {
					env.session.getChildThreads().remove(sts);
					t = null;
					i = env.session.getChildThreads().values().iterator();
				} catch (final Exception e) {
					// Do nothing
					;
				}
		}
		return;
	}

	/**
	 * Print a list of information known about child threads still registered
	 * with the session.
	 * @param env the session owning the threads.
	 * @throws JBasicException
	 */
	private void showThreads(final InstructionContext env) {
		boolean first = true;
		int count = 0;
		
		final Iterator i = env.session.getChildThreads().values().iterator();
		while (i.hasNext()) {
			final JBasicThread t = (JBasicThread) i.next();
			String msg = t.getID();

			if (t.getStatus() != null) {
				final Status sts = t.getStatus();
				String code = sts.getCode();
				if (code.charAt(0) == '*')
					code = code.substring(1);
				msg = msg + ", STATUS=" + code + "("
						+ sts.getMessage(t.getJBasic()) + ")";
			}
			if (t.isAlive())
				msg = msg + ", RUNNING";
			else
				msg = msg + ", STOPPED";

			if (first) {
				env.session.stdout.println("Child threads of " +
						env.session.getInstanceID() + ":");
				first = false;
			}
			count++;
			env.session.stdout.println(msg);

		}
		final String plural = count == 1 ? "" : "s";
		env.session.stdout.println(count + " thread" + plural);
		return;
	}

	/**
	 * Stop a gien thread by name, where the thread name is on the
	 * stack or in the instruction string argument.
	 * @param env
	 * @throws JBasicException
	 */
	private void stopThread(final InstructionContext env)
			throws JBasicException {
		String tname = env.instruction.stringOperand;
		if (tname == null)
			tname = env.pop().getString().toUpperCase();
		JBasic session = env.session;
		
		final JBasicThread t = session.getChildThreads().get(tname);
		if (t == null)
			throw new JBasicException(Status.UNKTHREAD, tname);

		if (!t.isAlive())
			throw new JBasicException(Status.THREADNR, tname);

		t.abort();
		return;
	}

	/**
	 * Start the multi-user server, using the port number stored on the
	 * stack. The logging level must already have been
	 * set.
	 * 
	 * @param env
	 * @throws JBasicException
	 * @throws NumberFormatException
	 */
	private void startServer(final InstructionContext env)
			throws JBasicException, NumberFormatException {
		JBasic session = env.session;
		if (JBasic.telnetDaemon == null) {
			String fname = "telnetd.properties";
			try {

				if (JBasic.telnetProperties == null) {
					/*
					 * See if we can load the properties from the class
					 * path.
					 */
					JBasic.telnetProperties = new Properties();
					InputStream fis = JBasic.class.getResourceAsStream("/"
							+ fname);

					/*
					 * Or try from the external file
					 */
					if (fis == null)
						fis = new FileInputStream(fname);

					JBasic.telnetProperties.load(fis);
				}

				int port = env.pop().getInteger();
				if (port > 0) {
					JBasic.telnetProperties.setProperty("std.port", Integer
							.toString(port));
				} else {
					port = Integer.parseInt(JBasic.telnetProperties
							.getProperty("std.port"));
					env.session.globals().insertLocal(
							"SYS$PORT", new Value(port));
				}
				
				/*
				 * Before we get too carried away, let's ensure that the
				 * port is available. This is the most common server start
				 * error; a port already in use.  Check now before we build
				 * up lots of stuff that is hard to tear down cleanly.
				 */

				Socket skt = null;
				try {
					skt = new Socket("localhost", port);
				} catch( Exception e ) {
					skt = null;
				}
				if( skt != null ) {
					skt = null;
					throw new JBasicException(Status.SERVER, "port " + port + " already in use");	
				}

				/*
				 * Looks like it's worth a shot.  Seal off the system port number,
				 * and try to create a telnet daemon.
				 */
				env.session.globals().markReadOnly("SYS$PORT");
				JBasic.telnetDaemon = TelnetD
						.createTelnetD(JBasic.telnetProperties);

				env.session.globals().insertReadOnly("SYS$SERVER_START", 
						new Value(new Date().toString()));
								
			} catch (BootException e) {
				throw new JBasicException(Status.SERVER, e.toString());
			} catch (IOException e) {
				throw new JBasicException(Status.INFILE, fname);
			}
		}

		/*
		 * Before we can do any IO to the user data files we must
		 * establish which session is doing all the work here.
		 */
		
		JBasic.userManager.setSession(env.session);
		
		/*
		 * If there are no users already loaded, attempt load via
		 * a default file name.
		 */
		if (JBasic.userManager.size() == 0) {
			if(new File(JBasic.USER_DATA).exists())
				JBasic.userManager.load(JBasic.USER_DATA);
			else
				JBasic.log.printMessage("SERVERNOAUTHFILE");
		}

		JBasic.telnetDaemon.start();
		Value mode = session.globals().localReference("SYS$MODE");
		if (mode == null)
			session.globals().insertReadOnly("SYS$MODE",
					new Value("MULTIUSER"));
		else
			mode.setString("MULTIUSER");
		
		Value prompt = session.globals().localReference("SYS$PROMPT");
		prompt.coerce(Value.STRING);
		JBasic.userManager.savedPrompt = prompt.getString();
		prompt.setString("SERVER> ");
		return;
	}

	/**
	 * Stop the multi user server.
	 * @param env
	 * @throws JBasicException
	 */
	private void stopServer(final InstructionContext env) throws JBasicException {
		if (JBasic.telnetDaemon == null)
			throw new JBasicException(Status.SERVER, "MULTIUSER mode not active");
		JBasic.telnetDaemon.stop();
		if(JBasic.userManager.dirtyData())
			JBasic.userManager.save(JBasic.USER_DATA);
		JBasic.userManager.setSession(null);
		SymbolTable globals = env.session.globals();
		globals.localReference("SYS$MODE").setString("SINGLEUSER");
		globals.deleteAlways("SYS$SERVER_START");
		
		Value prompt = globals.localReference("SYS$PROMPT");
		prompt.setString(JBasic.userManager.savedPrompt);
		JBasic.firstSession.detached = false;
		return;
	}

	/**
	 * Change the current remote user's password.  This is the only
	 * one of the _THREAD operations that does not require special
	 * user privileges.
	 * @param env
	 * @throws JBasicException
	 */
	private void setPassword(final InstructionContext env)
			throws JBasicException {
		
		JBasic session = env.session;
		if( JBasic.userManager.getSession() == session )
			throw new JBasicException(Status.NOPWD);
		
		String password = env.instruction.stringOperand;
		if( !env.instruction.stringValid )
			password = env.pop().getString();
		
		String userName = env.session.getUserIdentity().getName();
		boolean authorized = false;
		
		if( session.hasPermission(Permissions.PASSWORD))
			authorized = true;
		if( session.hasPermission(Permissions.ADMIN_USER))
			authorized = true;
		if( !authorized ) {
			JBasic.log.permission(env.session,Permissions.PASSWORD);
			throw new JBasicException(Status.SANDBOX, Permissions.PASSWORD);
		}
		JBasic.userManager.setPassword( userName, password );
		return;
	}
}

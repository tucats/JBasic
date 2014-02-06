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
package org.fernwood.jbasic.runtime;

import java.util.Iterator;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.LockManager;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.value.Value;

/**
 * Thread stub for a child JBasic session. A JBasicThread is created by the
 * THREAD RUN command, and spawns a complete sub-session of JBasic that runs a
 * single command (which can be a program invocation, of course).
 * <p>
 * <br>
 * See the THREAD statement for more information.
 * 
 * @author tom
 * @version version 1.0 Oct 5, 2006
 * 
 */
public class JBasicThread extends Thread {

	/**
	 * The command string used to run the thread.  This is usually a CALL
	 * statement.
	 */
	public String cmd;

	/**
	 * The container hosting the new thread.  This is not the same as the
	 * container that launches the thread.
	 */
	JBasic threadEnv;

	/**
	 * The terminating status of the thread.  This is null until the thread
	 * actually completes.
	 */
	Status status;

	/**
	 * The starting time for the thread, expressed as system time in milliseconds.
	 */
	public long startTime;

	/**
	 * Create a new JBasicThread instance.
	 * 
	 * @param parent
	 *            The JBasic object that the thread is being launched from.
	 * @param theCmd
	 *            The string command to be executed when the thread is started.
	 */
	public JBasicThread(final JBasic parent, final String theCmd) {
		cmd = theCmd;
		// status = new Status();
		threadEnv = new JBasic(parent);
		try {
			threadEnv.globals().insert("SYS$ISTHREAD", true);
		} catch (JBasicException e) {
			new Status(Status.FAULT, "unable to create symbol, " + e.toString()).print(parent);
		}

		/*
		 * Explicitly set the standard input and output to match the parent
		 * thread.  This means threads created on remote sessions will send
		 * output back to the remote telnet sessions, not the primary console!
		 */
		threadEnv.setStdin(parent.stdin());
		threadEnv.stdout = parent.stdout;
		
		/*
		 * Move a copy of programs from the parent session to the child
		 * session.
		 */
		final Iterator i = parent.programs.iterator();
		while (i.hasNext()) {
			final Program p = (Program) i.next();
			threadEnv.programs.add(p.getName(), p);
		}
	}

	/**
	 * Return the last status of the session, if there is one.
	 * 
	 * @return a Status indicating the completion status of the thread. If the
	 *         thread is still running, this will be *SUCCESS
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Return the JBasic session object for this thread.
	 * 
	 * @return a JBasic session.
	 */
	public JBasic getJBasic() {
		return threadEnv;
	}

	/**
	 * Set the abort flag in the session. This is a one-shot abort that tells
	 * the runtime to interrupt. A session that has been aborted cannot be
	 * restarted or run again.
	 * 
	 */
	public void abort() {
		threadEnv.setAbort(true);
	}

	/**
	 * Get the instance ID for the thread.
	 * 
	 * @return a String with the unique name of the JBasic session.
	 */
	public String getID() {
		return threadEnv.getInstanceID();
		
	}

	/**
	 * Run the program statement.
	 */
	public void run() {
		startTime = System.currentTimeMillis();
		final SymbolTable s = threadEnv.globals();
		final String threadStartTime = "SYS$THREAD_START_TIME";

		try {
			s.insertLocal(threadStartTime, new Value(startTime));
			s.markReadOnly(threadStartTime);
		} catch (JBasicException e) {
			new Status(Status.FAULT, "unable to create symbol, " + e.toString()).print(this.threadEnv);
		}

		/*
		 * Execute the command and print any error that occurs
		 */
		status = new Status();
		status = threadEnv.run(cmd);
		status.printError(threadEnv);
		
		/*
		 * Make sure we have released any locks we held before leaving to
		 * prevent deadlocks later.
		 */
		LockManager.releaseAll(threadEnv);
	}

}

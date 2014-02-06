
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

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Interrupt Handle for abort (control-c). 
 * <p>
 * This class implements the mechanisms
 * used to trap operating-system level abort signals (specifically the SIGINT
 * signal on Unix derivatives).
 * <p>
 * It uses the ill-documented but seemingly supported Signal package to create a
 * signal object and install it as the handle. The handler method itself takes
 * responsibility for setting flags in JBasic to indicate that an interrupt
 * request has been made; this is polled periodically during execution (on _STMT
 * bytecode boundaries) to interrupt running programs.
 * 
 * @author Tom Cole
 * 
 */
class InterruptHandler implements SignalHandler {

	/**
	 * Install a new handler to support the named signal.  At this time in
	 * JBasic, the only named signal we use is "INT" for the interrupt signal.
	 * 
	 * @param signalName
	 *            a String containing the name of the signal, such as "INT" for
	 *            SIGINT.
	 * @return An InterruptHandler object bound to the named signal.
	 */
	public static InterruptHandler install(final String signalName) {
		final Signal diagSignal = new Signal(signalName);
		final InterruptHandler diagHandler = new InterruptHandler();
		Signal.handle(diagSignal, diagHandler);
		return diagHandler;
	}

	/**
	 * Signal handler method. This method is called when the SignalHandler
	 * object is triggered by the interrupt.
	 * 
	 * @param sig
	 *            The signal code that was triggered, in the event that a
	 *            handler is supporting more than one signal.
	 */

	public void handle(final Signal sig) {
		try {

			/*
			 * An interrupt is signaled.  Set the global session flag to
			 * show this has occurred; this causes the byte code execution
			 * engine to throw a JBasicException from within the executing
			 * code and stops the program.
			 */
			JBasic.interruptSignalled = true;

			/*
			 * We do NOT chain back to previous handler, if one exists, because
			 * somewhere in the chain is a handler that terminates the JVM, and
			 * we want to keep going instead.
			 */

		} catch (final Exception e) {
			System.out.println("Signal handler failed, reason " + e);
		}
	}
}

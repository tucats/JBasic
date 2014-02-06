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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;

/**
 * This is a generic exception for JBasic. This encapsulates much of the same
 * info as a Status() object but can be used for a "throw" operation. This is
 * mostly used in bytecode execution to signal stack failures, so stack checking
 * does not have to be done after each push() or pop() operation in the
 * bytecodes themselves.
 * 
 * @author tom
 * @version version 1.0 Jan 29, 2006
 * 
 */
public class JBasicException extends Exception {

	static final long serialVersionUID = 1;

	/**
	 * The string code identifying the exception signal. Examples are "VERB" or
	 * "UNKFUNC". The code is always in uppercase.
	 */
	private String code;
	
	/**
	 * The optional signal parameter, or null if no parameter is present.
	 */

	private String parameter;

	/**
	 * The name of the program that was running when the exception occurred,
	 * or null of there was no active program.
	 */
	private String program;
	
	/**
	 * The line number where the fault occurred, or zero if there was no
	 * active program or it is a protected program.
	 */
	private int lineNumber;
	
	/**
	 * Flag indicating if the specific signal carried in this exception has 
	 * already been printed out to the user.  This prevents duplication of 
	 * messaging for a single fault.
	 */
	private boolean fAlreadyPrinted;

	private Status nestedStatus;
	
	/**
	 * Create a JBasic exception object given a signal code.
	 * 
	 * @param theCode
	 *            A String containing the uppercase name of the signal to
	 *            create.
	 */
	public JBasicException(final String theCode) {
		code = theCode;
		parameter = null;
	}

	/**
	 * Create a JBasic exception object given a Status object.
	 * @param status the status object to use to initialize the exception.
	 */
	public JBasicException(Status status) {
		code = status.getCode();
		parameter = status.getMessageParameter();
		program = status.getProgram();
		lineNumber = status.getLine();
		fAlreadyPrinted = status.printed();
		nestedStatus = status.getNestedStatus();
	}

	/**
	 * Create an exception with a name and string argument
	 * @param statusName The status code
	 * @param statusArg the status parameter or null if not present
	 */
	public JBasicException(String statusName, String statusArg) {
		code = statusName;
		parameter = statusArg;
	}


	/**
	 * Create an exception object with a Status as the parameter, rather
	 * than the conventional String argument.
	 * @param fault The error code
	 * @param status The Status object that is nested in this exception.
	 */
	public JBasicException(String fault, Status status) {
		code = fault;
		nestedStatus = status;
	}

	/**
	 * Create an exception object with an integer as a parameter,
	 * which is converted to a string appropriately.
	 * @param fault the status code name
	 * @param parm the status parameter number, or zero if not used.
	 */
	public JBasicException(String fault, int parm) {
		code = fault;
		this.parameter = Integer.toString(parm);
	}

	/**
	 * Get the status object stored in the exception.
	 * @return a Status object constructed from the stored code and optional parameter.
	 */
	public Status getStatus() {
		
		Status s = new Status(code, parameter, nestedStatus);
		s.setWhere(program, lineNumber);
		s.setPrinted( this.fAlreadyPrinted);
		return s;
	}

	/**
	 * Given an exception, print it out to the appropriate destination using
	 * the given session.
	 * @param session The session object used to locate an error console.
	 */
	public void printError(JBasic session) {
		getStatus().print(session);
	}

	/**
	 * Format the exception as a string, mostly for use in debugging.
	 */
	public String toString() {
		StringBuffer fm = new StringBuffer();
		fm.append("JBasicException(");
		fm.append(code);
		if( parameter != null )  {
			fm.append(", \"");
			fm.append(parameter);
			fm.append("\"");
		}
		if( nestedStatus != null ) {
			fm.append(", ");
			fm.append(nestedStatus.toString());
		}
		fm.append(")");
		return fm.toString();
	}

	/**
	 * Print the current exception information to a given JBasic session.
	 * @param session the session used to decode the error message and
	 * display the output text.
	 */
	public void print(JBasic session) {
		getStatus().print(session);
	}
	
	/**
	 * JBasicExceptions do not have a stack trace capability, so let's
	 * stub out an overridden method to prevent this call from taking 
	 * up so much time.
	 */
	public Throwable fillInStackTrace() {
		return this;
	}
}

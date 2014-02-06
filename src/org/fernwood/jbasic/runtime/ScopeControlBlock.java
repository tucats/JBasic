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

import org.fernwood.jbasic.Program;

/**
 * Define the information for managing runtime scope. That is, within a program
 * that has been execute via a CALL or RUN statement, internal subroutines are
 * permitted by the JBasic language, via the GOSUB statement. <p>
 * 
 * The ScopeControlBlock is used to track the kind of internal scope control
 * information, such as the statement that execution is to return to when a
 * RETURN is hit, within the current program. <p>
 * 
 * Currently, only GOSUB statements use this mechanism, but other internal
 * flow-of-control constructs might do so in the future.
 * 
 * @author tom
 * @version version 1.0 Aug 16, 2006
 * 
 */

public class ScopeControlBlock {

	/*
	 * Definition of scope types we might care about. These are 
	 * typically scopes within a running program; scope between 
	 * executing Program objects is handled in the runtime
	 * environment automatically.
	 */

	/**
	 * The current scope block supports a GOSUB statement
	 */
	final public static int GOSUB = 1;

	/**
	 * The current scope block is not assigned to a known statement type.
	 */
	final public static int NONE = 0;

	/**
	 * The statement type of the current scope control block.
	 */
	public int scopeType;

	/**
	 * The bytecode location of the statement to return to when a RETURN
	 * statement is found.
	 */
	public int returnStatement;

	/**
	 * The bytecode location of the destination of the flow-of control
	 * statement, such as the destination of the GOSUB statement.
	 */
	public int targetStatement;

	/**
	 * The Program object that contains this execution scope.
	 */
	public Program activeProgram;

	/**
	 * Create a generic scope control block that has not been configured.
	 */
	public ScopeControlBlock() {
		returnStatement = 0;
		targetStatement = 0;
		activeProgram = null;
		scopeType = NONE;
	}

	/**
	 * Create a scope control block bound to a given Program object.
	 * 
	 * @param p
	 *            The Program object that contains this scope control block.
	 */
	public ScopeControlBlock(final Program p) {
		returnStatement = 0;
		targetStatement = 0;
		activeProgram = p;
		scopeType = NONE;
	}

}

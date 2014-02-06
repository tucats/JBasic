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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;


/**
 * COMMON operator. This sets the "common" attribute for a variable in the 
 * local symbol table whose name is in the string argument.
 * 
 * @author tom
 * @version version 1.1 Feb 2009 Detect attempt to set COMMON attribute on 
 * a nonexistant variable.
 * 
 */
public class OpCOMMON extends AbstractOpcode {

	public void execute(final InstructionContext env) throws JBasicException {

		String name = env.instruction.stringOperand;
		if( name == null )
			throw new JBasicException(Status.INVOPARG, "<null>");
		
		Value v = env.localSymbols.localReference(name);
		if( v == null )
			throw new JBasicException(Status.UNKVAR, name);
		
		env.localSymbols.setCommon(name);
	}

}

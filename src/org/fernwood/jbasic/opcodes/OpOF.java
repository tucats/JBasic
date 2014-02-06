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
 * _OF links two objects together as container and contained objects.
 * @author cole
 * 
 */
public class OpOF extends AbstractOpcode {

	/**
	 * Execute the _OF instruction at runtime.
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final Value container = env.pop();
		Value containedValue = env.popForUpdate();

		if( !containedValue.isType(Value.RECORD) || !container.isType(Value.RECORD))
			throw new JBasicException(Status.INVOBJECT);

		containedValue.setObjectAttribute("PARENT", new Value(container.getName()));
		env.push(containedValue);

		return;
	}
}

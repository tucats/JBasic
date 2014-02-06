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

import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpMETHOD extends AbstractOpcode {

	/**
	 * Method object lookup
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {
		Value value2;
		Value obj = null;

		String lookupObject;
		if( env.instruction.stringValid ) {
			lookupObject = env.instruction.stringOperand;
			obj = env.localSymbols.reference(lookupObject);
		}
		else {
			obj = env.pop();
			lookupObject = "*STACK";
		}
			

		final String method = env.pop().getString().toUpperCase(); 

		boolean methodFound = false;
		env.codeStream.refSecondary("->" + method);
		
		/*
		 * Take a breath here and see if the thing we're invoking is really
		 * a Java object wrapper.
		 */
		
		if( obj.isObject()) {
			//env.session.stdout.println("Java wrapper method invocation " + obj + "->" + method);
			env.push(new Value(method));
			env.push(obj);
			return;
		}
		
		if( env.codeStream.fLocallyScoped)
			throw new JBasicException(Status.METHSCOPE, method);
		
		while (true) {
			if( lookupObject.equals("*STACK"))
					value2 = obj;
			else
				value2 = env.localSymbols.reference(lookupObject);
			if( value2 == null )
				break;
			
			if (value2.getType() != Value.RECORD)
				throw new JBasicException(Status.INVOBJECT, env.instruction.stringOperand);

			/*
			 * See if this object holds the item we seek. If so, push the value
			 * and we're done.
			 */

			Value value3 = value2.getObjectAttribute("CLASS");
			String methodClass = null;
			String mxname = null;
			if( value3 != null ) {
				methodClass = value3.getString();
				mxname = methodClass + "$" + method;
				final Program methodPgm = env.session.programs.find(mxname);
				if (methodPgm != null) {
					env.push(new Value(mxname));
					methodFound = true;
					break;
				}
			}

			value3 = value2.getObjectAttribute("METHODS");
			if (value3 != null)
				if (value3.isType(Value.RECORD)) {
					if (methodClass == null)
						methodClass = "OBJECT$";

					value3 = value3.getElement(method);
					if (value3 != null) {
						mxname = value3.getString().toUpperCase();
						env.push(new Value(mxname));
						methodFound = true;
						return;
					}
				}

			/*
			 * See if we have a CLASS object that let's us keep looking up a
			 * chain.
			 */

			value3 = value2.getObjectAttribute("CLASS");
			if (value3 == null)
				break;
			lookupObject = value3.getString();
		}

		if (!methodFound)
			throw new JBasicException(Status.NOSUCHMETHOD, lookupObject + "->" + method);

		return;
	}

}

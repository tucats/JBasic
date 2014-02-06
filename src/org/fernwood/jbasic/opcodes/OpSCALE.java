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
 * COPYRIGHT 2003-2007 BY TOM COLE, TOMCOLE@USERS.SF.NET
 *
 * Created on Jan 26, 2012 by cole
 *
 */
package org.fernwood.jbasic.opcodes;

import java.math.BigDecimal;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * @version version 1.0 Jan 26, 2012
 *
 */
public class OpSCALE extends AbstractOpcode{

		/**
		 *  <b><code>_SCALE size [, "name"]</code><br><br></b>
		 * Set the scale factor for a DECIMAL value.
		 * <p><br>
		 * <b>Explicit Arguments:</b><br>
		 * <list>
		 * <li><code>size</code> - scale in digits.</li>
		 * <li><code>name</code> - name of variable</li>
		 * </list><br><br>
		 *
		 * @param env The instruction context.
		 * @throws JBasicException indicating a stack over- or under-flow.
		 */

		public void execute(final InstructionContext env) throws JBasicException {

			Value v;
			
			/*
			 * There must be an integer argument or there is an error.
			 */
			if( !env.instruction.integerValid)
				throw new JBasicException(Status.INVOPARG, 0);
			
			/*
			 * If a name is given, that's the name of the variable to
			 * set. Otherwise, use the value from the stack.
			 */
			if( env.instruction.stringValid)
				v = env.localSymbols.findReference(env.instruction.stringOperand, false);
			else
				v = env.popForUpdate();
			
			/*
			 * If the value isn't a DECIMAL then it's an error; no type coercion
			 * for _SCALE
			 */
			if( v.getType() != Value.DECIMAL)
				throw new JBasicException(Status.WRONGTYPE, "DECIMAL");
			
			/*
			 * Set the scale value
			 */
			v.setDecimal(v.getDecimal().setScale(env.instruction.integerOperand, BigDecimal.ROUND_HALF_UP));
			
			/*
			 * If it wasn't a named value, then push it back on the
			 * stack. If it was a named value, we've already changed
			 * it.
			 */
			if( !env.instruction.stringValid)
				env.push(v);
			
		}

	}



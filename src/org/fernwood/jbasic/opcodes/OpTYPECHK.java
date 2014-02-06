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
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;


/**
 * @author cole
 * 
 */
public class OpTYPECHK extends AbstractOpcode {

	/**
	 *  <b><code>_TYPECHK [<em>exception_flag</em>][<em>, "descriptor"]</em></code><br><br></b>
	 * Compare the item on the stack to the second item on the stack, or a model value
	 * created using the descriptor string.  If the exception flag is 1, then this is
	 * done non-destructively and will signal an exception if it fails.  If the flag
	 * is not set, then the stack items are discarded and a boolean result pushed back.
	 * <p>
	 * If the string argument is present, it should be a constant expression describing the
	 * required data type, using the words <code>INTEGER</code>, <code>STRING</code>,
	 * <code>DOUBLE</code>, and <code>BOOLEAN</code> to represent data types. Array and
	 * record syntax are also permitted.  For example, <code>"[string, string]"</code> is a descriptor
	 * that indicates an array that must contain exactly two string values.
	 * <p><br>
	 * If the string is not present, then a second item is popped from the stack and is
	 * used as the model to compare the base item. The actual values are not important,
	 * the comparison is only done based on the types of each item.  However, the comparison
	 * is done as a deep traversal, so arrays and records are recursively processed to
	 * determine that all members and array elements exist and have the required type(s).
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li><code>exception_flag</code> - 1 if the instruction is in exception mode.</l1>
	 * <li><code>descriptor</code> - The optional string descriptor.</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		Value base = null;
		
		boolean fException = false;
		
		/*
		 * See if we are in "exception" mode, which means the instruction doesn't
		 * put a value back on the stack but instead signals an error if there is
		 * a type check failure.
		 * 
		 * In this mode, we don't disturb the top stack item, removing the need to
		 * generate a _DUP before this instruction.
		 */
		if( env.instruction.integerValid && env.instruction.integerOperand == 1 ) {
			fException = true;
			base = env.get(0);
		}
		
		/*
		 * Not in exception mode, so get the item to compare from the stack.
		 */
		else
			base = env.pop();

		Value compare = null;
		boolean result = false;

		if( env.instruction.stringValid) {
			result = Utility.validateTypes(env.session, base, env.instruction.stringOperand);
		}
		else {
			compare = env.pop();
			result = Utility.validateTypes(compare, base);
		}
		
		if( fException ) {
			if( !result )
				throw new JBasicException(Status.TYPEMISMATCH);
		}
		else
			env.push(new Value(result));
			
	}

}

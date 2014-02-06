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
package org.fernwood.jbasic.funcs;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.CompileContext;
import org.fernwood.jbasic.opcodes.OpADD;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;


/**
 * <b>SUM()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Calculate the sum of arguments.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>v = SUM( <em>p1</em> [, <em>p2</em>...] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Same type as <em>p1</em></td></tr>
 * </table>
 * <p>
 * Calculates the sum of all the arguments.  The type is based on the types
 * of the arguments.  If all arguments are strings, then the result is the
 * arguments concatenated together.  Otherwise it is the arithmentic sum
 * of the values.  The resulting type is whatever type is required to store
 * the result; that is, if all the arguments are integers, then an integer
 * value is returned.  If some are integer and some are double values, then
 * a double will be returned.
 * @author cole
 *
 */
public class SumFunction extends JBasicFunction {



	/**
	 * Compile the intrinsic SUM function. Code has already been generated with
	 * the items on the stack <em>in reverse order.</em> Generate code to work
	 * through the list backwards, creating an accumulator on top of the stack.
	 * The order is critical because, for strings, the total must be the
	 * left-to-right concatenation of the string values.
	 * 
	 * @param work the compilation unit
	 * @return Status indicating that the compilation succeeded without error.
	 * @throws JBasicException if an error in the count or type of arguments
	 * is found.
	 */
	public Status compile(final CompileContext work) throws JBasicException {

		if (work.argumentCount < 1)
			throw new JBasicException(Status.INSFARGS);
		
		if (work.argumentCount == 1)
			return new Status();

		/*
		 * If the argument list is constant, we can actually just generate
		 * a constant result.  To find the correct result, this code scoops
		 * up the arguments and calculates the sum of the items.
		 */
		
		if( work.constantArguments & work.argPosition > 0) {
			
			/*
			 * Get a list of the arguments that are in the generated
			 * code stream.
			 */
			Value [] r = this.getArguments(work);
			
			/*
			 * Find out what the most complex type is; that will be
			 * the type we coerce all arguments to.
			 */
			int maxType = Value.UNDEFINED;
			for( Value v : r )
				if( v.getType() > maxType )
					maxType = v.getType();
			
			if( maxType == Value.RECORD)
				return new Status(Status.UNKFUNC);
			
			/*
			 * Now convert each argument in the list to the correct type.
			 */
			for( Value v : r)
				v.coerce( maxType);
			
			/*
			 * Calculate the sum, by adding each of the values together.  We
			 * use the first element in the array as the accumulator.
			 */
			for( int idx = 1; idx < r.length; idx++ )
				r[0] = OpADD.addValue(r[idx], r[0]);
			
			/*
			 * Delete the bytecodes in the stream that were used to define
			 * the argument list, and instead generate the code for the result
			 * value.
			 */
			this.deleteArguments(work);
			work.byteCode.add(r[0]);
			return new Status();
		}
		
		/*
		 * The bytecode stream was not a constant, so we'll emit _ADD operation and
		 * let them get resolved later at runtime.
		 */
		for (int i = 1; i < work.argumentCount; i++)
			work.byteCode.add(ByteCode._ADD);
		
		return new Status();
	}

}

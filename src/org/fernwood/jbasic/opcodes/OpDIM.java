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
 * @author cole
 * 
 */
public class OpDIM extends AbstractOpcode {

	/**
	 * <b><code>_DIM <em>type</em></code><br><br></b>
	 * Create an array of a given type, where the count is the top stack item.
	 * The integer operand is the data type for each element that is created in
	 * the array.
	 * 
	 * <p><br>
	 * <b>Explicit Arguments:</b><br>
	 * <list>
	 * <li>&nbsp;&nbsp;<code>I</code> - data type, one of Value.INTEGER, Value.STRING, etc.</li>
	 * </list>
	 * <p><br>
	 * <b>Implicit Arguments:</b><br>
	 * <list>
	 * <li>&nbsp;&nbsp;<code>stack[tos]</code> - count of elements to create in array</li>
	 * <br><br>
	 * </list>
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * Get the data type that will be stored in each element of the
		 * multi-dimensional array we are creating. 
		 */
		final int dataType = env.pop().getInteger();
		
		/*
		 * Get the number of array dimensions to populate.
		 */
		int dimCount = env.instruction.integerOperand;
	
		/*
		 * Get the size of the first dimension.
		 */
		int size = env.pop().getInteger();
		
		/*
		 * Let's make sure these are reasonable.
		 */
		if( dimCount < 1 )
			throw new JBasicException(Status.ARRAYBOUNDS, Integer.toString(dimCount));
		if( size < 1 )
			throw new JBasicException(Status.ARRAYBOUNDS, Integer.toString(size));
			
		/*
		 * There must always be at least one dimension, so create a new value
		 * of the correct type and filled to the correct size.  The size will
		 * be zero if it is just a scalar create.
		 */
		Value array = makeDimension(new Value(dataType, null), size);
		
		/*
		 * If there are additional dimensions, fill them using the previously
		 * partially populated array with each successive dimension's size as
		 * taken from the stack.
		 */
		while( dimCount > 1 ) {
			dimCount--;
			array = makeDimension( array, env.pop().getInteger() );
		}
		
		/*
		 * Put the constructed object back on the stack.
		 */
		env.push(array);
		return;
	}

	/**
	 * Create a dimension of a multidimensional array.  
	 * @param fillValue The value that is to be stored in each element of the newly
	 * created array.
	 * @param count The size of the dimension that is to be created
	 * @return The populated object.
	 */
	private Value makeDimension( Value fillValue, int count ) {
		Value dimension = new Value(Value.ARRAY, null);
		for( int ix = 1; ix <= count; ix++ )
			dimension.setElement(fillValue.copy(), ix);
		return dimension;
	}
}

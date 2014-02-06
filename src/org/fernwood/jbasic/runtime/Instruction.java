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

/**
 * An object that contains a single executable ByteCode. 
 * <p>
 * The object contains an identifier describing the specific operation to 
 * perform, as well as any arguments to this specific instance of the 
 * instruction to the ByteCode. 
 * <p>
 * For each instruction, there can be an integer, double, or string
 * argument.  An instruction may have zero or one of each type of
 * argument.  There is also a flag indicating if the 
 * string or integer operand is "valid", which means that it was present 
 * in the expression of the ByteCode. This is used for some bytecodes to 
 * indicate when an optional value is given.
 * <p>
 * Additionally, each instruction knows how many times it has been
 * executed, which is used in profiling.
 * 
 * @author tom
 * 
 */
public class Instruction {

	/**
	 * The floating point constant associated with this instruction, if any.
	 */
	public double doubleOperand;

	/**
	 * A flag indicating if this instruction has a double operand. If false,
	 * then the double operand is considered missing.
	 */
	public boolean doubleValid;

	/**
	 * The integer constant associated with this instruction, if any. This is
	 * also used to store boolean values as a 1 or 0.
	 */
	public int integerOperand;

	/**
	 * A flag indicating if this instruction has an integer operand. If false,
	 * then the integer operand is considered missing.
	 */
	public boolean integerValid;

	/**
	 * The operation code for this instruction, which tells what this
	 * instruction does.
	 */
	public int opCode;

	/**
	 * The string constant associated with this instruction, if any.
	 */
	public String stringOperand;

	/**
	 * A flag indicating if this instruction has a string operand. If false,
	 * then the string operand is considered missing.
	 */

	public boolean stringValid;

	/**
	 * Profiling counter for execution of this specific instruction.
	 */

	public int counter;

	/**
	 * Is this instruction the target of a branch?  This is set during link
	 * phases and is not used at runtime.
	 */
	public boolean branchTarget;
	
	/**
	 * This is a developer's trap. This should not be called, <em>ever</em>. 
	 * <p>
	 * If it is, then it means that a new OpCode was created that wasn't 
	 * correctly formed, and therefore did not subclass the instruction 
	 * correctly. If we're called, it means the developer of JBasic made 
	 * an error...
	 */
	public Instruction() {
		System.out.println(
				"Ouch! generic instruction constructor should never be called!");
	}

	/**
	 * Initialize an instruction with a single opcode
	 * 
	 * @param o
	 *            Opcode to execute
	 */
	public Instruction(final int o) {
		opCode = o;
		integerOperand = 0;
		integerValid = false;
		stringValid = false;
		doubleValid = false;
		stringOperand = null;
	}

	/**
	 * Initialize an instruction with an opcode and a floating point parameter.
	 * 
	 * @param o
	 *            Opcode to execute
	 * @param d
	 *            The double precision floating point value to store in the
	 *            instruction.
	 */
	public Instruction(final int o, final double d) {
		opCode = o;
		doubleOperand = d;
		doubleValid = true;
		integerValid = false;
		stringValid = false;
	}

	/**
	 * Initialize an instruction with an opcode and numeric argument. The
	 * argument can be either additional informaiton such as the test value
	 * required from a _STRCMP or it can be a label number, etc.
	 * 
	 * @param o
	 *            Opcode to execute
	 * @param b
	 *            Numeric argument
	 */
	public Instruction(final int o, final int b) {
		opCode = o;
		integerOperand = b;
		stringOperand = null;
		integerValid = true;
		doubleValid = false;
	}

	/**
	 * Initialize an instruction with an opcode, a numeric value, and a string
	 * value. An example of this is <code>_LOAD 7 "X"</code> which loads a
	 * value from variable "X" and coerces it to a formatted string
	 * representation (7).
	 * 
	 * @param o
	 *            The opcode of the instruction
	 * @param b
	 *            The integer parameter of the instruction
	 * @param s
	 *            The string parameter of the instruction
	 */
	public Instruction(final int o, final int b, final String s) {
		opCode = o;
		integerOperand = b;
		stringOperand = s + "";
		integerValid = true;
		stringValid = true;
		doubleValid = false;
	}

	/**
	 * Initialize an instruction with an opcode and text argument. The argument
	 * can be a string constant to load on the stack, a text label to go to,
	 * etc.
	 * 
	 * @param o
	 *            Opcode to execute
	 * @param s
	 *            Text argument
	 */
	public Instruction(final int o, final String s) {
		opCode = o;
		integerOperand = 0;
		stringOperand = s + "";
		stringValid = true;
		integerValid = false;
		doubleValid = false;
	}

	public String toString() {
		return ByteCode.disassembleInstruction(-1, this);
	}
	
	/**
	 * Does the current instruction equal a given instruction?
	 * @param t2 the instruction to compare with the current instruction
	 * 
	 * @return true if the instructions are semantically equal, even if they 
	 * are in fact different objects.
	 */
	public boolean equals(Instruction t2) {
		if( this.opCode != t2.opCode )
			return false;
		
		if( this.stringValid != t2.stringValid)
			return false;
		if( this.stringValid )
			if( !this.stringOperand.equals(t2.stringOperand))
				return false;

		if( this.integerValid != t2.integerValid)
			return false;
		if( this.integerValid)
			if( this.integerOperand != t2.integerOperand)
				return false;

		if( this.doubleValid != t2.doubleValid)
			return false;
		if( this.doubleValid)
			if( this.doubleOperand != t2.doubleOperand)
				return false;

		return true;
	}
}

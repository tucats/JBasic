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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.compiler.Linkage;
import org.fernwood.jbasic.compiler.StringPool;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.opcodes.AbstractOpcode;
import org.fernwood.jbasic.opcodes.InstructionContext;
import org.fernwood.jbasic.opcodes.OpDEBUG;
import org.fernwood.jbasic.statements.Statement;
import org.fernwood.jbasic.value.Value;

/**
 * The ByteCode class supports the generation and execution of ByteCodes, which
 * are arrays of Instruction objects which represent the executable code for a
 * JBasic statement.
 * <p>
 * The ByteCode object has the following major functions:
 * <p>
 * <list>
 * <li>Define the ByteCode opcode values, like _AND or _EXEC for use in
 * anything doing code generation.
 * <li>Add instructions to a bytecode stream.
 * <li>Support runtime call/return stack handling.
 * <li>Host the label map created by the Linker
 * <li>Disassemble instructions for the debugger
 * <li>Run a bytecode stream by using the dispatcher to invoke OpCode objects
 * representing the instructions in the bytecode stream.
 * <li>Handle the runtime data stack. <list>
 * <p>
 * <br>
 * In general, the use of the ByteCode object takes place in two separate phases. 
 * During compilation, the bytecode is created and instructions are added to the
 * bytecode stream.  These can be added directly, or by compiling an expression
 * using the Expression object which appends instructions to a given bytecode 
 * stream.<p>
 * After compilation, the bytecode can be executed at any time.  To execute the
 * bytecode, you must supply a symbol table which is used to resolve symbols at
 * runtime.  The symbol table is typically the most-local symbol table in the
 * execution scope (i.e. the currently CALLed program, etc.) and links to the
 * symbol tables of parents and the global tables.  When you execute a stream,
 * you can optionally fetch a result from it if it contains only an expression.
 * <br>
 * <p><br>
 * <code>
 *          ByteCode bc = new ByteCode( session );<br>
 *             ...<br>
 *          bc.add( <em>instruction information</em>);<br>
 *          bc.add( <em>instruction information</em>);<br>
 *             ...
 * </code>
 * <p><br>
 * <code>
 * Status sts = bc.run( <em>symbols</em> );<br>
 * Value v = bc.getResult();<br>
 * </code>
 * <br>
 * <p>
 *             
 *          
 * @author Tom Cole
 * 
 */
public class ByteCode {

	/**
	 * Runtime context for a bytecode stream is stored here.
	 */

	private JBasic session;

	/**
	 * Flag indicating if the _END should pop the result from the stack if there
	 * is one and move it to ARG$RESULT. This is done for function call programs
	 * only.
	 */
	boolean popReturn;

	/**
	 * Set the popReturn flag indicating if the _END should pop the result from
	 * the stack if there is one and move it to ARG$RESULT. This is done for
	 * function call programs only.
	 * 
	 * @param flag
	 *            True if the _END should pop the result and move it to the
	 *            caller's local storage.
	 */
	public void popReturn(final boolean flag) {
		popReturn = flag;
	}

	/**
	 * Dispatch vector for objects that handle execution of instructions.
	 */

	static AbstractOpcode[] dispatchVector = null;

	/**
	 * Used with the _ENTRY bytecode to indicate a PROGRAM
	 */
	public static int ENTRY_PROGRAM = 1;
	
	/**
	 * Used with the _ENTRY bytecode to indicate a VERB
	 */
	public static int ENTRY_VERB = 2;
	
	/**
	 * Used with the _ENTRY bytecode to indicate a FUNCTION
	 */
	public static int ENTRY_FUNCTION = 3;


	/**
	 * Used with the _ENTRY bytecode to indicate a SUB internal routine
	 */
	public static final int ENTRY_SUB = 4;

	
	/**
	 * This value is added to the value of any ByteCode that is expected to
	 * hold a branch address in it's integer operand.  This flag is used
	 * to handle branch address relocation when linking or during instruction
	 * deletion from a bytecode stream.
	 * 
	 * Any time a new ByteCode is created that uses the integer operand as
	 * a bytecode address, always add this!
	 */
	public static final int _BRANCH_FLAG = 1000;
	
	/**
	 * <code>_ADD</code> <br>
	 * <br>
	 * Pop two items from the stack, add them, and push the result
	 */
	public static final int _ADD = 15;

	/**
	 * <code>_AND</code> <br>
	 * <br>
	 * Pop two items from the stack, AND, push. If the items are strings,
	 * concatenate.
	 */

	public static final int _AND = 38;

	/**
	 * <code>
	 * _ARG n "SYMBOL"
	 * </code> <br>
	 * <br>
	 * Fetch argument n from the current scope's argument list and store it in
	 * local variable "SYMBOL".
	 */
	public static final int _ARG = 48;

	/**
	 * <code>
	 * _ARGDEFFAULT n "SYMBOL"
	 * </code><br>
	 * <br>
	 * Pop a default value from the top of the stack. If argument n exists in
	 * the current scope's argument list, store it in "SYMBOL" Otherwise store
	 * the default value in "SYMBOL"
	 */

	public static final int _ARGDEF = 49;

	/**
	 * <code>
	 * _BR n
	 * </code> <br>
	 * <br>
	 * Transfer control to instruction n in the current bytecode stream. If n is
	 * not present, then end the current bytecode stream.
	 */
	public static final int _BR = 52 + _BRANCH_FLAG;

	/**
	 * <code>
	 * _CALLF n ["NAME"]
	 * </code> <br>
	 * <br>
	 * Pop n items from the stack and store them in a new call frame argument
	 * list. If "NAME" is given, then call that function by name. Otherwise, pop
	 * another string value from the stack and call using that value as the
	 * name.
	 */
	public static final int _CALLF = 27;

	/**
	 * <code>
	 * _CALLP n ["NAME"]
	 * </code> <br>
	 * <br>
	 * Pop n items from the stack and store them in a new call frame argument
	 * list. If "NAME" is given, then call that program by name. Otherwise, pop
	 * another string value from the stack and call using that value as the
	 * name.
	 * 
	 * @see ArgumentList
	 * @see Program
	 */

	public static final int _CALLP = 46;

	/**
	 * <code>
	 * _CLOSE
	 * </code> <br>
	 * <br>
	 * Close the open file matching the identifier on top of the stack.
	 */
	public static final int _CLOSE = 35;

	/**
	 * <code>
	 * _CVT n
	 * </code> <br>
	 * <br>
	 * Convert the top item on the stack to an item of type n, such as 1 for
	 * INTEGER or 2 for STRING, or 7 for FORMATTED_VALUE. The types match the
	 * Value type constants.
	 * 
	 * @see JBasicFile
	 */
	public static final int _CVT = 43;

	/**
	 * <code>
	 * _CONCAT 
	 * </code> <br>
	 * <br>
	 * Pops two strings from the stack, concatenates them, and pushes the
	 * resulting string back. This is semantically equivalent to _ADD.
	 */
	public static final int _CONCAT = 19;

	/**
	 * <code>
	 * _ARRAY n
	 * </code> <br>
	 * <br>
	 * Pop n items from the stack, and create an arrayValue Value on the stop of
	 * the stack. The arrayValue is populated with the items from the stack, in
	 * reverse order (the top item is the last item in the arrayValue, the
	 * bottom item is the first item in the arrayValue).
	 */
	public static final int _ARRAY = 5;

	/**
	 * <code>
	 * _BOOL n
	 * </code> <br>
	 * <br>
	 * Push a constant boolean Value on the stack. The value of n is an integer
	 * that is converted to a boolean value; 0 = false and any other value =
	 * true.
	 */
	public static final int _BOOL = 10;

	/**
	 * <code>
	 * _CHAR n
	 * </code> <br>
	 * <br>
	 * Push a string on the stack that is a single character, represented by the
	 * numeric value n as a Unicode character. For example,
	 * <code>_CHAR 10</code> pushes a string with a newline character on the
	 * stack.
	 */
	public static final int _CHAR = 51;

	/**
	 * <code>
	 * _DOUBLE [i] d
	 * </code> <br>
	 * <br>
	 * Pushes a double precision floating point value on the stack.  
	 * <p>
	 * If the double
	 * constant is not present, then the integer value is examined for special
	 * codes.  These are:<p>
	 * <list>
	 * <li>0 = 0.0,</li><li>1 = Inf,</li><li>2 = -Inf,</li><li>3 = NaN.</li></list>
	 */
	public static final int _DOUBLE = 8;

	/**
	 * <code>
	 * _INTEGER n
	 * </code> <br>
	 * <br>
	 * Pushes an integer value on the stack.
	 */
	public static final int _INTEGER = 7;

	/**
	 * <code>
	 * _STRING "str"
	 * </code> <br>
	 * <br>
	 * Pushes a string constant on the stack.
	 */
	public static final int _STRING = 9;

	/**
	 * <code>
	 * _DIV
	 * </code> <br>
	 * <br>
	 * Pop two items from the stack, and divide the first by the second. The
	 * result is put back on the stack. The division is done using an operation
	 * suitable to the data type (integer, float, etc.).
	 */
	public static final int _DIV = 18;

	/**
	 * <code>
	 * _DUP
	 * </code> <br>
	 * <br>
	 * Duplicate the top stack item.
	 */
	public static final int _DUP = 54;

	/**
	 * <code>
	 * _END
	 * </code> <br>
	 * <br>
	 * End execute of the ByteCode stream. Causes the run() method to terminate.
	 */
	public static final int _END = 41;

	/**
	 * <code>
	 * _ENTRY t "name"</code> <br>
	 * <br>
	 * Defines the entry point for a program unit. The value t is the type of
	 * program unit. <p>
	 * <list><li>1=PROGRAM,</li><li>2=FUNCTION,</li><li>3=VERB.</li></list><p>
	 * The "name" is the name of
	 * the program unit. 
	 */
	public static final int _ENTRY = 47;

	/**
	 * <code>_EQ</code><br>
	 * <br>
	 * Pop the top two items on the stack and test for equality of their values.
	 * Push a boolean back on the stack that indicates whether they were equal.
	 */
	public static final int _EQ = 23 + _BRANCH_FLAG;

	/**
	 * <code>_EXEC</code><br>
	 * <br>
	 * Pop a string from the stack and execute the string as a JBasic statement.
	 * The statement will be compiled and executed immediately if possible, or
	 * it will be interpreted.
	 */

	public static final int _EXEC = 42;

	/**
	 * <code>_GE</code><br>
	 * <br>
	 * Pop the top two items on the stack. Push a boolean back on the stack that
	 * indicates whether the top item is greater than or equal to the second
	 * item.
	 */
	public static final int _GE = 21;

	/**
	 * <code>_JSB inst | "label"</code><br>
	 * <br>
	 * If present, the label is used as the destination of the GOSUB statement,
	 * which is a local subroutine call within the current program. Execution
	 * continues at the destination address. If the label is not provided, then
	 * the instruction number is given as the integer argument. This instruction
	 * number is the instruction address within the current bytecode stream.
	 */

	public static final int _JSB = 28 + _BRANCH_FLAG;

	/**
	 * <code>_JSBIND</code><br>
	 * <br>
	 * Pop a string from the top of the stack and use it as the destination of a
	 * GOSUB option, which is a local subroutine call. If the current bytecode
	 * is a linked program, the labelMap is used to lookup the destination
	 * instruction address, else the label us used as a statement label to
	 * search for the next statement's bytecode stream to branch to.
	 */

	public static final int _JSBIND = 45;

	/**
	 * <code>_JMP inst | "label"</code><br>
	 * <br>
	 * If present, the label is used as the destination of the GOTO statement,
	 * which is a local transfer of control. Execution continues at the
	 * destination address. If the label is not provided, then the instruction
	 * number is given as the integer argument. This instruction number is the
	 * instruction address within the current bytecode stream.
	 */

	public static final int _JMP = 4 + _BRANCH_FLAG;

	/**
	 * <code>_JMPIND</code><br>
	 * <br>
	 * Pop a string from the top of the stack and use it as the destination of a
	 * GOTO option, which is a local transfer of control operation. If the
	 * current bytecode is a linked program, the labelMap is used to lookup the
	 * destination instruction address, else the label us used as a statement
	 * label to search for the next statement's bytecode stream to branch to.
	 */
	public static final int _JMPIND = 3;

	/**
	 * <code>_GT</code><br>
	 * <br>
	 * Pop the top two items on the stack. Push a boolean back on the stack that
	 * indicates whether the top item is greater than the second item.
	 */
	public static final int _GT = 22;

	/**
	 * <code>_INDEX "symbol"</code><br>
	 * <br>
	 * The string argument is the name of an array. The top of stack is used as
	 * an integer index into the array, and the value at that location in the
	 * array is pushed back on the stack.
	 */

	public static final int _INDEX = 36;

	/**
	 * <code>_LE</code><br>
	 * <br>
	 * Pop the top two items on the stack. Push a boolean back on the stack that
	 * indicates whether the top item is less than or equal to the second item.
	 */
	public static final int _LE = 25;

	/**
	 * <code>_LENGTH</code><br>
	 * <br>
	 * Pop the top item from the stack, and replace it with an integer
	 * describing the length. For numbers, the number is formatted and the
	 * length of the resulting string is given. For strings, the length is
	 * given. For arrays, the number of elements in the arrayValue is given.
	 */
	public static final int _LENGTH = 60;

	/**
	 * <code>_LOAD  [<em>type</em>], "symbol"</code><br>
	 * <br>
	 * The symbol name is used to lookup a value starting in the local symbol
	 * table and searching up the execution stack. If the <em>type</em> value
	 * is given, it represents a Value type that the result is coerced to before
	 * it is pushed on the stack as the result of the instruction.
	 */
	public static final int _LOAD = 11;

	/**
	 * <code>_LT</code><br>
	 * <br>
	 * Pop the top two items on the stack. Push a boolean back on the stack that
	 * indicates whether the top item is less than the second item.
	 */

	public static final int _LT = 26;

	/**
	 * <code>_MOD</code><br>
	 * <br>
	 * The top of the stack is popped as a modulus value. The next item on the
	 * stack is popped, and an integer modulo operation performed using the
	 * modulus value. The integer result is pushed back on the stack.
	 */

	public static final int _MOD = 40;

	/**
	 * <code>_MULT</code><br>
	 * <br>
	 * Two numeric values are popped from the top of the stack. They are
	 * promoted to the highest-precision common type, so an integer and a double
	 * are converted to two doubles, etc. The resulting values are multipled
	 * together, and the product is pushed back on the stack.
	 */

	public static final int _MULT = 17;

	/**
	 * <code>_NE</code><br>
	 * <br>
	 * Pop the top two items on the stack. Push a boolean back on the stack that
	 * indicates whether the top item is not equal to the second item.
	 */

	public static final int _NE = 24 + _BRANCH_FLAG;

	/**
	 * <code>_NEGATE</code><br>
	 * <br>
	 * The top stack item is popped from the stack, and arithmetically negated.
	 * So an integer 5 becomes -5, and a double -3.2 becomes 3.2. A boolean is
	 * converted to an integer and negated as an integer type. The result is
	 * pushed back on the stack.
	 */
	public static final int _NEGATE = 6;

	/**
	 * <code>_NOT</code><br>
	 * <br>
	 * The top of stack is popped and a boolean NOT operation performed. If the
	 * value is a number, then if zero it becomes 1 and if non-zero it becomes
	 * zero. The result is pushed back on the stack.
	 */
	public static final int _NOT = 39;

	/**
	 * <code>_OPEN <em>mode</em>, "identifier"</code><br>
	 * <br>
	 * The top of the stack is popped and is used as a filename string to define
	 * the file to open. The integer operand <em>mode</em> is the file mode 
	 * (JBasicFile.INPUT, JBasicFile.OUTPUT, etc.). If successful, the file 
	 * identification record is stored in the local variable 
	 * <em>identifier</em>.
	 * 
	 * @see JBasicFile
	 */
	public static final int _OPEN = 34;

	/**
	 * <code>_COLUMN</code><br>
	 * <br>
	 * The top two stack items are popped and treated as the column width and
	 * max column count. The next item on the stack is popped as the file
	 * identification record,and is used to locate the file to set the COLUMN()
	 * attribute on. This is usually generated after an _OPENFILE.
	 */
	public static final int _COLUMN = 32;

	/**
	 * <code>_KILL ["name"]</code><br>
	 * <br>
	 * IF the <em>name</em> is given, then it the filename to delete.
	 * Otherwise the top of stack is popped, and must contain the identification
	 * record for the open file to delete. In either case, the physical file
	 * indicated is deleted, and if the file was opened it is closed.
	 * 
	 * @see JBasicFile
	 */
	public static final int _KILL = 33;

	/**
	 * <code>_OR</code><br>
	 * <br>
	 * Pop top two stack items and coerce them to be booleans. Push the boolean
	 * "or" result.
	 */
	public static final int _OR = 37;

	/**
	 * <code>_OUT [<em>file-flag</em>]</code><br>
	 * <br>
	 * Pop the top stack item and output it. If the <em>file-flag</em> is
	 * present and non-zero, also pop the identifier indicating which file to
	 * write to.
	 */
	public static final int _OUT = 29;

	/**
	 * <code>_OUTNL [<em>file-flag</em>]</code><br>
	 * <br>
	 * Output a newline character. If the <em>file-flag</em> is present and
	 * non-zero, also pop the identifier indicating which file to write to.
	 */

	public static final int _OUTNL = 31;

	/**
	 * <code>_OUTTAB [<em>file-flag</em>]</code><br>
	 * <br>
	 * Output a tab character. If the <em>file-flag</em> is present and
	 * non-zero, also pop the identifier indicating which file to write to.
	 */

	public static final int _OUTTAB = 30;

	/**
	 * <code>_QUIT</code><br>
	 * <br>
	 * Terminate the JBasic session.
	 */

	public static final int _QUIT = 53;

	/**
	 * <code>_PROT</code><br>
	 * <br>
	 * Pop the name of a program from the stack as a string, and set it's
	 * protected attribute.
	 */
	public static final int _PROT = 55;

	/**
	 * <code>_RET [<em>return-flag</em>]</code><br>
	 * <br>
	 * Return from an execution context. If <em>return-flag</em> is present
	 * and non-zero, pop the top stack item and return it as the result of the
	 * function or procedure call.
	 */

	public static final int _RET = 1;

	/**
	 * <code>_SIGNAL</code><br>
	 * <br>
	 * Pop string from the top of the stack and use it to identify generate a
	 * signal code
	 */
	public static final int _SIGNAL = 50;

	/**
	 * <code>_STOR ["name"]</code><br>
	 * <br>
	 * Store the top of the stack in the named value in the local context.
	 * If the name is not given, the name is also popped from the stack 
	 * as a string value.
	 */
	public static final int _STOR = 12;

	/**
	 * Store in an array element
	 */
	public static final int _STORA = 13;

	/**
	 * Store in a program line
	 */
	public static final int _STORP = 14;

	/**
	 * Declare the text of a statement, whose bytecode follows
	 */
	public static final int _STMT = 56;

	/**
	 * Builtin string compare function
	 */
	public static final int _STRCMP = 2;

	/**
	 * Subtract top two stack items, push result back on stack
	 */
	public static final int _SUB = 16;

	/**
	 * Swap the top two stack items
	 */
	public static final int _SWAP = 44;

	/**
	 * Pop the top stack item, and branch if it is a zero value.
	 */
	public static final int _BRZ = 20 + _BRANCH_FLAG;

	/**
	 * Load a field from a record
	 */
	public static final int _LOADR = 57;

	/**
	 * Store a field value in a record
	 */
	public static final int _STORR = 58;

	/**
	 * Create a RECORD object from field names and values pushed on the stack.
	 */
	public static final int _RECORD = 59;

	/**
	 * Load a register value onto the stack
	 */
	public static final int _LOADREG = 61;

	/**
	 * Change the sign of the top stack item
	 */
	public static final int _SIGN = 62;

	/**
	 * Store the top stack item in a register
	 */
	public static final int _STORREG = 63;

	/**
	 * <code>_FOR "index" </code><br>
	 * <br>
	 * 
	 * Variable <em>index</em> is used as an index for a FOR-NEXT loop. The
	 * top stack item is the increment, then second stack item is the end value,
	 * and third stack item is the start value.
	 * 
	 * The statement <code>FOR I = 1 TO 10</code> would compile to:
	 * <p>
	 * <br>
	 * <code>
	 * 	_INTEGER 1<br>
	 *	_INTEGER 10<br>
	 *	_INTEGER 1<br>
	 *	_FOR "I"<br><br>
	 *</code>
	 * 
	 * There will be a matching _NEXT that identifies the same index variable.
	 * 
	 * @see LoopControlBlock
	 */
	public static final int _FOR = 64 + _BRANCH_FLAG;

	/**
	 * Terminus of a FOR-NEXT loop; causes the evaluation of the index, and a
	 * backwards branch if the endpoint has not been reached yet.
	 */
	public static final int _NEXT = 65 + _BRANCH_FLAG;

	/**
	 * No-op that defines a branch location
	 */
	public static final int _LABEL = 66;

	/**
	 * Perform a LINE INPUT operation, and leave the input buffer string on the
	 * stack.
	 */
	public static final int _LINE = 67;

	/**
	 * Pop the top item which must be an array, and sort it.
	 */
	public static final int _SORT = 68;

	/**
	 * No operation performed, skip to the next instruction
	 */
	public static final int _NOOP = 69;

	/**
	 * Define start of a DO loop
	 */
	public static final int _DO = 70;

	/**
	 * <code> _LOOP [integer-type]</code><br>
	 * <br>
	 * Define the end of a DO loop.  The integer operand tells what kind
	 * of loop it is used for.  The value 1 means a DO..UNTIL loop, and
	 * the value 2 means a DO..WHILE loop.
	 */
	public static final int _LOOP = 71;

	/**
	 * Allocate storage in an array
	 */
	public static final int _ALLOC = 72;

	/**
	 * Process an INPUT statement item
	 */
	public static final int _INPUT = 73;

	/**
	 * Declare an error condition
	 */
	public static final int _ERROR = 74;

	/**
	 * Resolve a method invocation, using superclass traversal
	 */
	public static final int _METHOD = 75;

	/**
	 * Resolve an object dereference, using parent object traversal
	 */
	public static final int _OBJECT = 76;

	/**
	 * Call a method
	 */
	public static final int _CALLM = 77;

	/**
	 * Load a reference (not a copy) to a value on the stack. Used when the
	 * object is to be directly manipulated.
	 */
	public static final int _LOADREF = 78;

	/**
	 * Generate a unique ID value and put it on the stack.
	 */
	public static final int _UID = 79;

	/**
	 * Declare the start of a bytecode segment that defines a DATA element
	 */
	public static final int _DATA = 80;

	/**
	 * Read the next DATA element
	 */
	public static final int _READ = 81;

	/**
	 * Reset the current program's "next DATA element" pointer to the first DATA
	 * element.
	 */
	public static final int _REW = 82;

	/**
	 * <code>_EOD</code> <br>
	 * <br>
	 * If the next _READ will cause an implicit _REW to start reading at the
	 * first DATA statement, then this pushes true on the stack. If the next
	 * _READ will not start again at the first DATA because there is more data
	 * yet to be read, it returns FALSE. If there are no DATA statements in the
	 * program, it returns TRUE.
	 */
	public static final int _EOD = 83;

	/**
	 * <code>_STORINT n, "name"</code> <br>
	 * <br>
	 * The integer value expressed by 'n' is stored in the named variable. The
	 * stack is not changed.
	 */
	public static final int _STORINT = 84;

	/**
	 * <code>_STORDBL d, "name"</code> <br>
	 * <br>
	 * The double floating value expressed by 'd' is stored in the named
	 * variable. The stack is not changed.
	 */
	public static final int _STORDBL = 85;

	/**
	 * <code>_STORBOOL n, "name"</code> <br>
	 * <br>
	 * The boolean value expressed by 'n' which must be 0 or 1 is stored in the
	 * named variable. The stack is not changed.
	 */
	public static final int _STORBOOL = 86;

	/**
	 * Add an integer constant to the top item on the stack.
	 */
	public static final int _ADDI = 87;

	/**
	 * Subtrace an integer constant from the top item on the stack.
	 */
	public static final int _SUBI = 88;

	/**
	 * <code>_MULTI  n</code> <br>
	 * <br>
	 * The top of the stack is multiplied by the integer value 'n', and the
	 * result put back on the stack. This is a short-hand way of doing an
	 * _INTEGER followed by a _MULT instruction.
	 */
	public static final int _MULTI = 89;

	/**
	 * <code>_DIVI  n</code> <br>
	 * <br>
	 * The top of the stack is divided by the integer value 'n', and the result
	 * put back on the stack. This is a short-hand way of doing an _INTEGER
	 * followed by a _DIV instruction.
	 */
	public static final int _DIVI = 90;

	/**
	 * <code>_RESULT "name"</code> <br>
	 * <br>
	 * The result of the last program call is moved into the named symbol
	 * storage.
	 */
	public static final int _RESULT = 91;

	/**
	 * <code>_CLEAR "name"</code> <br>
	 * <br>
	 * Clear the symbol of the given name; the symbol is removed from the local
	 * symbol table.
	 */
	public static final int _CLEAR = 92;

	/**
	 * <code>_USING count</code> <br>
	 * <br>
	 * Pop the format string from the stack, and then pop 'count' values from
	 * the stack, which are formatted using the format string. The result is
	 * pushed back on the stack as a single string.
	 */
	public static final int _USING = 93;

	/**
	 * <code>_DIM type</code> <br>
	 * <br>
	 * Pop an integer count from the stack, and create an array of that many
	 * elements. Each element is initialized with a value of the given type, at
	 * it's default value setting. This array is left on the stack.
	 */
	public static final int _DIM = 94;

	/**
	 * <code>_VALUE type</code> <br>
	 * <br>
	 * Create a new value on the stack of the given type, initialized to
	 * whatever default is appropraite for the type. Used by the DIM statement.
	 */
	public static final int _VALUE = 95;

	/**
	 * <code>_GET</code> <br>
	 * <br>
	 * Use the top of the stack as an array of records that describe what field
	 * definitions to read. Second on stack is the expression that defines the
	 * open file to do the read from.
	 */
	public static final int _GET = 96;

	/**
	 * <code>_PUT</code> <br>
	 * <br>
	 * Use the top of the stack as an array of records that describe what field
	 * definitions to write. Second on stack is the expression that defines the
	 * open file to do the write to.
	 */
	public static final int _PUT = 97;

	/**
	 * <code>_SEEK</code> <br>
	 * <br>
	 * Use the integer on the top of stack to position the BINARY file
	 * identified by second-on-stack.
	 */
	public static final int _SEEK = 98;

	/**
	 * <code>_EXP</code> <br>
	 * <br>
	 * Pop top two values from stack, push result of S0 raised to S1 back on the
	 * stack.
	 */

	public static final int _EXP = 99;

	/**
	 * <code>_SIZEOF <em>type</em></code> <br>
	 * <br>
	 * Calculate the size of the top item on the stack. The type code is zero
	 * (or missing) for a scalar _SIZEOF operation, or 1 for a RECORD size 
	 * calculation such as used in a SEEK statement.
	 */
	public static final int _SIZEOF = 100;

	/**
	 * <code>_NL [<em>file-flag</em>]</code><br>
	 * <br>
	 * Print a newline. Equivalent to a PRINT statement with no data elements.
	 * The file-flag, if present and non-zero, means that a file id is popped
	 * from the stack to direct the newline to. Otherwise, the newline is sent
	 * to the console.
	 */

	public static final int _NL = 101;

	/**
	 * <code>_FORX "index"</code><br>
	 * <br>
	 * Identical to the _FOR opcode, except that it assumes a start value of 1
	 * and an increment of 1. This is the most common kind of loop so we can
	 * optimize it down to the FORX instruction in some cases.
	 */
	public static final int _FORX = 102 + _BRANCH_FLAG;

	/**
	 * <code>_INCR n, "value"</code><br>
	 * <br>
	 * Increment the stored integer variable "value" by the integer 'n'. Used by
	 * the optimizer to replace sequences like:
	 * <p>
	 * <code>
	 * _LOAD "X"<br>
	 * _ADDI 1<br>
	 * _STOR "X"<br>
	 * </code>
	 * <p>
	 * with:
	 * <p>
	 * <code>
	 * _INCR 1, "X"
	 * </code>
	 */
	public static final int _INCR = 103;

	/**
	 * <code>_TYPES n</code><br>
	 * <br>
	 * Set the strong typing flag in the current symbol table. When n is true
	 * (non-zero) the flag is set, which means that if a symbol has a
	 * pre-existing type then all values stored in it are coerced into that
	 * type. When false (zero), the symbol's type is changed to match the value
	 * being stored in it. Strong typing is more expensive in terms of runtime,
	 * but needed to honor some dialects of BASIC. This instruction is emitted
	 * as part of a program definition.
	 */

	public static final int _TYPES = 104;

	/**
	 * <code>_CALLT n [<em>"name"</em>]</code><br>
	 * <br>
	 * 
	 * Calls a procedure identically to CALLP, but runs it on a new
	 * JBasicThread.
	 */
	public static final int _CALLT = 105;

	/**
	 * <code>_SYS 0</code><br>
	 * <br>
	 * Pop string from top-of-stack and execute it is a child process of the
	 * current process. Implements SYSTEM command. The integer argument must be
	 * present and must be zero. Future expansion may allow for another value
	 * indicating that the spawn should be asynchronous.
	 */
	public static final int _SYS = 107;

	/**
	 * <code>_TRACE m</code><br>
	 * <br>
	 * Turn the statement-level trace mode on (m=1) or off(m=2). This sets the
	 * SYS$TRACE_STATEMENTS global flag on or off, which is used in _STMT
	 * opcodes to trace execution.
	 */
	public static final int _TRACE = 108;

	/**
	 * <code>_DEBUG m</code><br>
	 * <br>
	 * Turn the debug flag on or off for the current bytecode stream. Used to
	 * support RUN DEBUG and DEBUG statements.
	 */
	public static final int _DEBUG = 109;

	/**
	 * <code>_DEFMSG [f] [code]</code><br>
	 * <br>
	 * Define a message in the current runtime. If the flag 'f' is set to 1 then
	 * the language value is on the stack, else it has already been concatenated
	 * to the code. If the code is present in the instruction then it is used,
	 * else it is also popped from the stack. Finally, the text of the message
	 * is popped from the stack.
	 * <p>
	 * The messages are stored in session-specific message handling
	 * data structures.
	 */
	public static final int _DEFMSG = 110;

	/**
	 * <code>_THREAD subcmd [, "name"]</code><br>
	 * <br>
	 * Execute THREAD subfunctions, defined by the integer argument.
	 * <p>
	 * <list>
	 * <li> 0 - stop a thread
	 * <li> 1 - start a thread
	 * <li> 3 - list thread status
	 * <li> 4 - release terminated threads </list>
	 * <p>
	 * For code 0, the thread name is either the string argument of the
	 * instruction, or on the stack.
	 * <p>
	 * For code 1, if the string argument is present, the thread identifier is
	 * stored in a local variable with the given name.
	 */
	public static final int _THREAD = 111;

	/**
	 * <code>_LOCK ["name"]</code><br>
	 * <br>
	 * Lock a named LOCK resource, shared among all threads in the given
	 * process. If the lock is unavailable, the current thread waits. If the
	 * lock does not exist, it is created.
	 * <p>
	 * If the string argument is present, it contains the lock name. Otherwise
	 * the lock name is a string popped from the stack.
	 */
	public static final int _LOCK = 112;

	/**
	 * <code>_UNLOCK ["name"]</code><br>
	 * <br>
	 * Unlock/release a named LOCK resource, shared among all threads in the
	 * given process. If the lock was not owned by the current thread, nothing
	 * happens. If the lock does not exist, it is created.
	 * <p>
	 * If the string argument is present, it contains the lock name. Otherwise
	 * the lock name is a string popped from the stack.
	 */
	public static final int _UNLOCK = 113;

	/**
	 * <code>_SUBSTR n</code><br>
	 * <br>
	 * Perform a SUBSTR() builtin operation. The number n must be 2 or 3, and
	 * indicates how many parameters are on the stack. If only two parameters
	 * are given, the end of the range is assumed to be the end of the string.
	 */
	public static final int _SUBSTR = 114;

	/**
	 * <code>_OVER count</code><br>
	 * <br>
	 * Extract an element on the stack and make it be the top stack item.
	 * Similar to the FORTH 'over' operator, but this lets you specify an
	 * arbitrary count of elements to skip down. The count is 1-based, so OVER 1
	 * does no work (top item is removed and replaced on the stack), OVER 2 is
	 * the same as SWAP, etc. OVER 3 and larger is where the instruction has
	 * usefulness.
	 */
	public static final int _OVER = 115;
	
	/**
	 * <code>_EOF</code><br>
	 * <code>_EOF <em>n</em></code><br>
	 * <code>_EOF <em>"fid"</em></code><br>
	 * <br>
	 * Test for end-of-file.  The file can be on the top of the stack (the default),
	 * expressed as an integer file # value, or expressed as a symbol name in the
	 * instruction, which is loaded from the active symbol table.
	 */
	public static final int _EOF = 116;

	/**
	 * <code>_NOTEOF</code><br>
	 * <code>_NOTEOF <em>n</em></code><br>
	 * <code>_NOTEOF <em>"fid"</em></code><br>
	 * <br>
	 * Test for NOT end-of-file.  The file can be on the top of the stack (the default),
	 * expressed as an integer file # value, or expressed as a symbol name in the
	 * instruction, which is loaded from the active symbol table.
	 */
	public static final int _NOTEOF = 117;

	/**
	 * <code>_GOTO <em>linenumber</em></code><br>
	 * <br>
	 * Jump to a statement by line number.  Searches the current program for
	 * a line number and branches to that statement bytecode.
	 */
	public static final int _GOTO = 118;
	
	/**
	 * <code>_GOSUB <em>linenumber</em></code><br>
	 * <br>
	 * Jump to a subroutine by line number.  Searches the current program for
	 * a line number and branches to that statement bytecode, saving the 
	 * current address on the GOSUB/RETURN linkage stack.  The optimizer will
	 * convert this to a JSB if all goes well.
	 */
	public static final int _GOSUB = 119;
	
	/**
	 * <code>_CHAIN</code><br>
	 * <br>
	 * Pop a string from the stack and use it as the name of a program to
	 * run. Similar to the _EXECP operation, except that a new symbol
	 * table is not created; the current one is scrubbed so all symbols
	 * that aren't considered COMMON are cleared out and the current
	 * symbol table is re-used.
	 */
	public static final int _CHAIN = 120;
	
	/**
	 * <code>_COMMON <em>"name"</em></code><br>
	 * <br>
	 * Marks the COMMON attribute in the named variable, 
	 * so it will be preserved across CHAIN operations.
	 */
	public static final int _COMMON = 121;
	
	/**
	 * <code>_OF</code><br>
	 * <br>
	 * Pop container object and contained object off of stack and add a
	 * OBJECT$PARENT link in the contained object to the container.  Both
	 * stack arguments must be objects or there is an error.
	 */
	public static final int _OF = 122;

	/**
	 * <code>_RIGHT</code><br>
	 * <br>
	 * Pop a count and a string from the stack.  Push back the 
	 * right-most 'count' characters of the string.  If the count
	 * is less than one, the result is an empty string.  If the
	 * string length is less than count, the result is the entire
	 * string.
	 */
	public static final int _RIGHT = 123;

	/**
	 * <code>_LEFT</code><br>
	 * <br>
	 * Pop a count and a string from the stack.  Push back the 
	 * left-most 'count' characters of the string.  If the count
	 * is less than one, the result is an empty string.  If the
	 * string length is less than count, the result is the entire
	 * string.
	 */

	public static final int _LEFT = 124;
	
	/**
	 * <code>_SAVE <em>mode</em></code><br>
	 * <br>
	 * 
	 * The mode integer determines what kind of SAVE operation is
	 * being performed.
	 * 
	 * <list>
	 * <li>1 - pop a filename string from the stack and save the current
	 *     program to that file.
	 * <li>   
	 * 2 - pop a filename string from the stack and save the workspace
	 *     under that name.
	 * <li>    
	 * 3 - save the workspace under the default name.</list><br>
	 * 
	 */
	public static final int _SAVE = 125;
	
	/**
	 * <code>
	 * _CALLFL n "NAME"
	 * </code> <br>
	 * <br>
	 * Pop n items from the stack and store them in a new call frame argument
	 * list. Then call a LOCAL FUNCTION of the given name.  This function must
	 * be defined in the local program function space.  Note that this 
	 * instruction is usually converted from _CALLF to _CALLFL when the runtime
	 * sees that it is a local invocation and can be done more efficiently.
	 */
	
	public static final int _CALLFL = 126;
	
	/**
	 * <code>
	 * _DEFFN count "NAME"
	 * </code><br>
	 * <br>
	 * The following "count" bytecodes are used to define the statement
	 * function to be called NAME.  At link time, this information is used
	 * to collect up the byte code sequence and bind it to the current
	 * program's local function list.  At runtime, this just skips over 
	 * "count" instructions to continue execution at the next statement.
	 */
	public static final int _DEFFN = 127;
	
	/**
	 * <code>
	 * _FOREACH "index"
	 * </code><br>
	 * <br>
	 * Takes an array off the stack and creates a temporary array variable
	 * to store it in.  The "index" variable gets each member of the list in
	 * turn as the body of the loop is executed.
	 */
	public static final int _FOREACH = 128 + _BRANCH_FLAG;
	
	/**
	 * <code>
	 * _RAND <em>flag</em></code><br>
	 * <br>
	 * Generates a new pseudo-random number or re-seeds the sequence.  The
	 * flag value tells what operation to perform:<br>
	 * <list>
	 * <li>0 - generate a new integer value and push it on the stack.</li>
	 * <li>1 - use the system timer to re-seed the generator</li>
	 * <li>2 - use the integer value from the stack to re-seed the generator.
	 * </list>
	 */
	
	public static final int _RAND = 129;
	
	/**
	 * <code>
	 * _STORALL <em>"array-name"</em></code><br>
	 * <br>
	 * Pops the top stack item as the fill value, and the next stack item
	 * as the count, and fills the named array with as many copies of the
	 * fill item as given.
	 */

	public static final int _STORALL = 130;

	/**
	 * <code>_NEEDP <em>prompt-flag</em>
	 * </code></br><br>
	 * 
	 * This sets the needPrompt flag in the current session.  This means
	 * that a sequence has run which requires a prompt even when we are
	 * in NOPROMPT mode.  Examples include commands that list output or
	 * show program contents.  The normal parameter is '1' meaning that
	 * a prompt is needed, but in special cases a '0' could also be used
	 * to suppress a previously-requested prompt.  The prompt flag is 
	 * turned off again after each prompt until the next explicit or
	 * implicit request to turn it back on.
	 */
	public static final int _NEEDP = 131;
	
	/**
	 * <code>_SBOX</code><br><br>
	 * This instruction prefixes a sequence that should be considered
	 * invalid if in the SANDBOX mode.  This mode describes protected
	 * operations not suitable for multiuser or n-tier applications.  If
	 * sandbox mode is enabled, this instruction throws an error; if 
	 * it is not enabled then no error occurs and the sequence of 
	 * byte code instructions that follow it can be executed safely.
	 */
	public static final int _SBOX = 132;
	
	/**
	 * <code>_BRNZ <em>addr</em></code><br><br>
	 * This instruction is the mirror of _BRZ; if the top of stack
	 * is non-zero then the branch is taken to the given address.
	 */
	public static final int _BRNZ = 133 + _BRANCH_FLAG;
	
	/**
	 * <code>_ARGC <em>count</em></code><br><br>
	 * Throw an error if the current invocation does not contain the
	 * given number of arguments.
	 */
	public static final int _ARGC = 134;
	
	/**
	 * <code>_TIME <em>mode</em>,<em>"NAME"</em></code><br><br>
	 * Manage timed execution.  If mode is zero, then capture the
	 * time data and store in the named record.  If the mode is 1,
	 * then re-capture the time data, and print the differences.
	 */
	public static final int _TIME = 135;
	
	/**
	 * <code>_INPUTXML <em>fileMode</em></code><br><br>
	 * Input as much text as needed to complete an XML value, which 
	 * is returned as the result.  IF the <em>fileMode</em> flag is 
	 * present and non-zero then it means that there is a file 
	 * designation on the stack to be used to get the input from. 
	 * Otherwise, the console is used to read the XML value.
	 */
	public static  final int _INPUTXML = 136;

	/**
	 * <code>_LOADFREF <em>"file-id"</em></code><br><br>
	 * Load a file identifier on the stack.  This is functionally the
	 * same as _LOADREF except that the error messaging is specific 
	 * to file identifiers, and type coercion isn't permitted.
	 */
	public static final int _LOADFREF = 137;

	/**
	 * <code>_SET</code><br><br>
	 * Takes the top stack item, and stores it in the Value that is
	 * stored at stack(2), which must be a symbolic value.  This is used
	 * to copy a value into a reference, without creating a new value
	 * object.
	 */
	public static final int _SET = 138;

	/**
	 * <code>_LOCREF <em>"symbol"</em></code><br><br>
	 * Create a local reference to the symbol.   IF the symbol exists
	 * in the local symbol table, then load it on the stack.  If it does
	 * not exist, then it is created as an integer 0 and loaded on the
	 * stack.
	 */
	public static final int _LOCREF = 139;

	/**
	 * <code>_LOCMEM</code<br><br>
	 * Load or create member. Try to load a member reference from a 
	 * record. If the member does not exist, create it in the record 
	 * as an integer 0.  This is used in LValues that create member names.
	 */
	public static final int _LOCMEM = 140;

	/**
	 * <code>_LOCIDX</code><br><br>
	 * Access an index location, and create it if it does not exist. 
	 * If the source is a record, create the field with the given 
	 * name.  If the source is an array, make sure the array is at
	 * least as big as the given index.
	 */
	public static final int _LOCIDX = 141;

	/**
	 * <code>_ASM</code><br><br>
	 * The top value on the stack is converted to a string and assembled
	 * into an instruction stream that is executed immediately.
	 */
	public static final int _ASM = 142;

	/**
	 * <code>_REFSTR <em>code</em></code><br><br>
	 * Enable or disable capture of a reference string, which is the
	 * resolved expression that accesses a Value. For example, if the
	 * expression is <code>X[I+2]</code> and the value of <code>I</code>
	 *  is 5, then the 
	 * resulting reference string is <code>X[7]</code>.
	 * <p>
	 * If the code is zero, then any previous reference string is 
	 * discarded and a new capture begins.
	 * <p>
	 * If the code is one, then the capture string is pushed on the
	 * stack and then cleared in the bytecode object, indicating no
	 * additional capture is needed.
	 */
	public static final int _REFSTR = 143;

	/**
	 * <code>_CONSTANT <em>count</em>, <em>"NAME"</em></code><br><br>
	 * Defines that the next <em>count</em> instructions define a constant
	 * that should be stored under the given symbol <em>NAME</em>.  The
	 * linker locates these and moves them to the front of the code
	 * stream and replaces the _CONSTANT with a store to the given named
	 * variable.
	 */
	public static final int _CONSTANT = 144;

	/**
	 * <code>_LOGGING</code><br><br>
	 * Pop an integer from the stack and set the logging mechanism to
	 * that level.  1 = Errors, 2 = Warnings, 3 = Info messages.  
	 */
	public static final int _LOGGING = 145;

	/**
	 * <code>_FIELD</code><br><br>
	 * Pop the file reference and a record definition from the stack,
	 * and bind the record definition to the file reference.
	 */
	public static final int _FIELD = 146;

	/**
	 * <code>_IF <em>mode</em></code><br><br>
	 * 
	 * Marker used to generate multi-line if-then-else statements.  The
	 * opcode is itself never executed, but is replaced in the generated
	 * code at link-time by appropriate branch instructions.  The mode
	 * integer operand tells what element of the IF-ELSE-ENDIF group
	 * we are at.<p>
	 * <table>
	 * <tr><td>1</td><td>End of IF conditional test generation.</td></tr>
	 * <tr><td>2</td><td>End of THEN true block</td></tr>
	 * <tr><td>3</td><td>End of ELSE false block</td></tr></table>
	 * 
	 */
	public static final int _IF = 147;
	
	/**
	 * <code>_BRLOOP <em>dest</em> ["F" | "B"]</code><br><br>
	 * 
	 * Branch control from within a loop - this is used to support the END LOOP
	 * statement, to branch outside the body of a loop, for example.  The inner-
	 * most loop control block is discarded before the branch occurs.<p>
	 * The string values "F" and "B" indicate a branch forward or backwards
	 * during code generation, which are patched up by the linker and the 
	 * string operand removed from the instruction.
	 */
	public static final int _BRLOOP = 148 + _BRANCH_FLAG;

	/**
	 * <code>_LOADFILE <em>if-flag</em></code><br><br>
	 * 
	 * Pop a file name value from the stack and load it as a new program in memory,
	 * and make it the current program.  If the <em>if-flag</em> is 1, then do not
	 * do the load if there is a named program like it already in memory.
	 */
	public static final int _LOADFILE = 149;

	
	/**
	 * <code>_SERVER <em>selector</em></code><br><br>
	 * 
	 * Perform server management functions based on the selector code.
	 */
	public static int _SERVER = 150;

	
	/**
	 * <code>_PACKAGE</code><br><br>
	 * Pop a string from the stack and add it to the list of packages searched
	 * to resolve class names for user-defined statements and functions, and
	 * for NEW() operations on partial class names.
	 */
	public static final int _PACKAGE = 151;

	/**
	 * <code>_DROP</code><br><br>
	 * Discard the top stack item.
	 */
	public static final int _DROP = 152;

	/**
	 * <code>_SETPGM [<em>"name"</em>]</code><br><br>
	 * Use optional operand or top of stack to set current program name.
	 */
	public static final int _SETPGM = 153;
	
	/**
	 * <code>_MIN</code><br><br>
	 * Pop two items from the stack, and put back the smaller of the
	 * two items.
	 */
	public static final int _MIN = 154;
	
	/**
	 * <code>_MAX</code><br><br>
	 * Pop two items from the stack, and put back the larger of the
	 * two items.
	 */
	public static final int _MAX = 155;

	/**
	 * <code>_WHERE <em>count</em><br><br>
	 * Pop an array from the top of the stack.  Use the 'count'
	 * following bytecodes to form a temporary function.
	 *   The resulting elements
	 * that have a <code>true</code> function result are stored in the
	 * resulting array pushed back on the stack.
	 */
	public static final int _WHERE = 156;

	/**
	 * <code>_TABLE <em>count</em></code><br><br>
	 * Pop <em>count</em> strings off the stack and build
	 * an array whose first row consists of these names. This
	 * starts a TABLE which is a special case of an array where
	 * the first row has metadata.
	 */
	public static final int _TABLE = 157;

	/**
	 * <code>_INPUTROW <em>file-flag</em> "table-name"</code><br><br>
	 * Read a row of input data into the existing table <em>table-name</em>. 
	 * If the table does not exist, then an error is thrown.  The row data
	 * is read from a file identifier on the stack if <em>file-flag</em> is
	 * non-zero, or from the console.
	 * <p>
	 * The operation reads as many values as needed to satisfy the row
	 * count and data types of the given table.  Type conversions are done
	 * automatically.  The row is always added to the end of the table.
	 * The input data does not contain any additional syntax other than 
	 * normal INPUT data; this really just determines how many items
	 * to read from the input stream based on the TABLE characteristics.
	 */
	public static final int _INPUTROW = 158;

	/**
	 * <code> _ROWPROMPT <em>"table-name"</em></code><br><br>
	 * Generate a string that represents the default row prompt for the
	 * given table.  The prompt enumerates the column names in order and
	 * separated by commas, followed by a question mark. This is used
	 * with the INPUT ROW(table) statement when no explicit prompt
	 * string is given and the input is from the console.
	 */
	public static final int _ROWPROMPT = 159;

	/**
	 * <code>_SLEEP <em>secs</em></code><br><br>
	 * Sleep for the given number of seconds, expressed as a
	 * double or integer. If the argument is not given, it is
	 * popped from the stack.
	 */
	public static final int _SLEEP = 160;
	
	/**
	 * <code>_CONSOLE <em>code</em> [ <em>fref</em>]</code><br><br>
	 * Set the console mode to a specific file ref for the default
	 * console output on the current thread.  The code defines what
	 * is done. 0 = reset console to default, 1 set input console,
	 * 2 set output console.
	 */
	public static int _CONSOLE = 161;

	/**
	 * <code>_STRPOOL <em>code</em> | <em>"string"</em></code><br><br>
	 * Create a pool entry or reference one.  If the code value is
	 * given but no string, then the pooled string at the given location
	 * is stored on the stack, replacing the function of _STRING. If
	 * the string is present, then it is stored in the pool using the
	 * given code value.  If no code value is given then it is assigned
	 * the next available slot number.
	 * <br>
	 * See the function StringPool("String") for more information.
	 */
	public static int _STRPOOL = 162;
	
	/**
	 * <code>_TYPECHK <em>"type spec"</em></code><br><br>
	 * Compare two stack items to see if they match type with a
	 * deep compare, or use the string to construct a value used
	 * with the top of stack item.
	 * <p>
	 * The result is pushed as a boolean back on the stack.
	 */
	public static final int _TYPECHK = 163;

	/**
	 * <code>_PROTOTYPE <em>"type spec"</em></code><br><br>
	 * Use the top of stack (or, if present, the string operand)
	 *  to construct a value with the given type specification.
	 */

	public static final int _PROTOTYPE = 164;

	/**
	 * <code>_SETSCOPE <em>table-code</em></code><br><br>
	 * Set the scope of the current symbol table.  The <em>table-code</em>
	 * value is -1 for GLOBAL, -2 for ROOT, or a positive value indicating 
	 * how far up the chain of parents to reset the scope.  This can be done
	 * only once.
	 */
	public static final int _SETSCOPE = 165;

	/**
	 * <code>_SETDYNVAR <em>code</em></code><br><br>
	 * Set the flag that indicates if dynamic variable creation is enabled
	 * (flag=1) or disabled (flag=0).  When dynamic variable creation is
	 * enabled, any variable referenced in an lvalue (left side of an 
	 * assignment) will be created if it does not exist, and assigned the
	 * type of the value stored in it.  When the flag is disabled, a
	 * variable must be declared explicitly (such as with an INTEGER or DIM
	 * statement) before it can be written to.
	 */
	public static final int _SETDYNVAR = 166;

	/**
	 * <code>_DCLVAR <em>type-code</em>, <em>"variable-name"</em></code><br><br>
	 * Declare a variable.  This causes a variable in the local table to
	 * be created using the given name and type code.  This is required when
	 * the _SETDYNVAR flag is set to 0, forcing explicit variable declaration.
	 * If the type-code is a negative number, then the initial value is
	 * assumed to be on the stack and is stored in the value using the
	 * positive type code value.
	 */
	public static final int _DCLVAR = 167;
	
	/**
	 * Duplicate a reference to the top stack item. This doesn't make a new
	 * copy of the data item, it only creates a second reference to the same
	 * item. This is for cases like _LOADREF "A", _LOADREF "A" where both
	 * are loads of a reference; this is replaced with _LOADREF "A", _DUPREF
	 * so the symbol table lookup doesn't have to happen a second time.
	 */
	public static final int _DUPREF = 168;

	/**
	 * Similar to _ADD but writes the source argument directly to the
	 * target which MUST be a TABLE. The source can be a table or an
	 * array representing a single row.
	 */
	public static final int _INSERT = 169;

	/**
	 * Pop the top stack item, and determine if it's type matches
	 * the integer operand.  If it does not, then use the string
	 * operand to signal an error.  If no string operand, just
	 * signal INVTYPE.
	 */
	public static final int _ASSERTTYPE = 170;

	/**
	 * Pop two TABLE values from the stack and execute a join
	 * based on the match-by criteria in the JOIN codegen block.
	 * Automatic variables include LEFT and RIGHT as the row
	 * names, and also the actual table names if they are present,
	 * plus any unambiguous member names.
	 */
	public static final int _JOIN = 171;

	/**
	 * Test or set the attribute of a RECORD to be a table
	 * catalog. If the integer operand is zero, then set the
	 * record on top of the stack to reference a catalog. If
	 * the integer is 1, test the top of stack to see if it is
	 * a catalog, and throw an error if not.
	 */
	public static final int _CATALOG = 172;

	/**
	 * For a DECIMAL value, set the SCALE factor for the
	 * value, by name.
	 */
	public static final int _SCALE = 173;
	
	/**
	 * Convert ARRAY on top of stack to list of discrete values.
	 */
	public static final int _DECOMP = 174;
	
	
	/**
	 * The instance of the debugger to attach to this bytecode stream, if any.
	 */
	public JBasicDebugger debugger;

	/**
	 * The vector of "compiled" opcodes that will be executed.  Note that this
	 * is implemented as an ArrayList, not a Vector, so it is not thread-safe.
	 * A byteCode array should not be compiled by two or more threads simultaneously.
	 * <p>
	 * <strong>There is currently no code to explicitly prevent threads competing
	 * for use of this data structure!</strong>
	 */
	public ArrayList<Instruction> byteCode;

	/**
	 * An arrayValue of registers used to hold temporary values.
	 */

	public RegisterArray registers;

	/**
	 * The runtime stack used to hold values. ByteCode is essentially an RPN
	 * notation language. Data items are pushed on a stack to make them
	 * available to instructions. Instructions then pop zero or more items from
	 * the stack to get data items to operate on, as required by each
	 * instruction. The stack is last-in, first-out.
	 * 
	 * This was originally implemented as a Vector, but since stacks are always
	 * private to the threads running them, ArrayList was used instead because
	 * it is not synchronized. (This runs about 15% faster.)
	 * 
	 */
	public ArrayList<Value> dataStack;

	/**
	 * Return the size of the stack.
	 * 
	 * @return An integer indicating the size of the runtime data stack,or zero
	 *         if one has not been created yet.
	 */
	public int stackSize() {
		if( dataStack == null )
			return 0;
		return dataStack.size();
	}

	/**
	 * Return a specific element from the stack by ordinal position, rather than
	 * just the "top item". This is used when reading lists pushed on the stack
	 * in reverse order, such as function arguments. The stack is not modified
	 * by this operation.
	 * 
	 * @param i
	 *            The 0-based ordinal position on the stack
	 * @return The Value object at that location on the stack.
	 */
	public Value getStackElement(final int i) {
		if (dataStack == null)
			return null;
		return dataStack.get(i);
	}

	/**
	 * Get the top of the active expression stack. This is used when a bytecode
	 * stream is used to calculate an expression, and the result of the
	 * expression has been left on the top of the stack.
	 * 
	 * @return The top item is popped from the stack, and returned to the
	 *         caller. If there is no top item on the stack, a null is returned.
	 */
	public Value getResult() {
		Value v = null;
		final int top = dataStack.size();
		if (top > 0) {
			v = dataStack.remove(top - 1);
		}
		return v;
	}

	/**
	 * Flag indicating if the ByteCode stream is currently running. The run()
	 * method uses this to control the execution loop, and it is set to false
	 * when an _END instruction is encountered.
	 */
	public boolean fRunning;

	/**
	 * This flag indicates if the byteCode is a fully linked program versus a
	 * single statement. This affects how flow-of-control happens
	 * (intra-statement or intra-bytecode).
	 */
	public boolean fLinked;

	/**
	 * The current instruction being executed by the run() method, in the
	 * arrayValue of byteCode instructions.
	 */
	public int programCounter;

	/**
	 * A reference to the containing statement. This is required for branch
	 * management, etc. to find the program that goes with this statement.
	 */

	public Statement statement;

	/**
	 * A stored Status value that reflects the ending state of the bytecode
	 * stream when the statement ended. This is examined after the run() method
	 * is called to determine the results of the statement execution.
	 */
	public Status status;

	/**
	 * This is a cross-reference map (of linkage objects) that are used to match
	 * statement labels to byteCode addresses. This is created when a byteCode
	 * stream is linked (fLinked=true) and used at link time for label 
	 * resolution.  It is also used at runtime for GOTO USING() statements that
	 * have an indirect label expression.
	 */
	public TreeMap<String, Linkage> labelMap;

	/**
	 * This is a string pool used to handle string constants in protected code
	 * that must pass through the assembler. This is not used otherwise.
	 */
	StringPool pool;

	/**
	 * This is the name associated with the bytecode stream.
	 */
	private String name;

	/**
	 * This is the last line number found in a STMT opcode.  This is used to
	 * report errors at runtime by locating the offending statement.
	 */
	public int lastLineNumber;

	/**
	 * If this string buffer is non-null, then reference operations (_LOADREF,
	 * _LOADR, etc.) are converted into text representations and put in this
	 * buffer by the opcodes.  This supports PRINT E= notation.  This is 
	 * normally null indicating no capture is active.  The caller uses the
	 * refPrimary() and refSecondary() methods to store information in this
	 * buffer, and uses getCaptureBuffer() to get the buffer information.
	 */
	private StringBuffer referenceCaptureBuffer;

	/**
	 * This is the initial stack size for the runtime stack.  This stack is 
	 * created each time an execution context is started, and manages the
	 * runtime data stack used for expression handling, etc.  This number
	 * should be low enough that we don't waste memory, but large enough
	 * that the likelihood of the stack growing beyond this is small, since
	 * doing so results in copying the entire data stack.
	 */
	
	private int initialStackSize = 32;

	/**
	 * This is the type of data for a RETURN Statement.  If no type is
	 * given, this should be Value.UNDEFINED
	 */
	public int returnType;
	
	/**
	 * This flag is set by the linker or optimizer if an _ERROR bytecode
	 * exists.  This is used to decide if it is necessary to create an
	 * ON statement handler entry when this bytecode is run.
	 */
	public boolean fHasErrorHandler;

	/**
	 * This is the storage structure for the pooled string area for the
	 * current byte code stream.  This will be null if string pooling was
	 * not enabled, and is not used until the code stream is linked.
	 */
	
	public StringPool stringPool;

	/**
	 * Flag used to indicate if symbols can be dynamically constructed when they are 
	 * referenced as lvalues but do not yet exist.  The default BASIC behavior is that
	 * any variable can be created just by writing to it.  You can turn this off by
	 * clearning this flag (it's on by default).
	 */
	public boolean fDynamicSymbolCreation;

	/**
	 * Flag used to indicate if the symbol table(s) used for this codestream are to
	 * be scoped locally or allowed to participate in a multiply-scoped table set.
	 */
	public boolean fLocallyScoped;
	
	/**
	 * Constructor for making a new byteCode object, which contains executable
	 * code streams representing the work of a single statement or an entire
	 * program. ByteCode objects contain an array of Instruction objects
	 * defining the work to be done to execute the compiled code.
	 * <p>
	 * A new ByteCode object must have instructions added to it (via the
	 * <code>add()</code> method) before it can be executed.
	 * <p>
	 * In general, the <code>compile()</code> method of each Statement object
	 * is called to generate the instruction streams that go in a ByteCode
	 * object.
	 * 
	 * @param jb
	 *            The JBasic object containing this session.
	 * @see Instruction
	 * @see Statement
	 * 
	 */

	public ByteCode(final JBasic jb) {
		setSession(jb);
		initializeByteCode(null);
	}

	/**
	 * Constructor for making a new byteCode object, which contains executable
	 * code streams representing the work of a single statement or an entire
	 * program. ByteCode objects contain an array of Instruction objects
	 * defining the work to be done to execute the compiled code.
	 * <p>
	 * A new ByteCode object must have instructions added to it (via the
	 * <code>add()</code> method) before it can be executed.
	 * <p>
	 * In general, the <code>compile()</code> method of each Statement object
	 * is called to generate the instruction streams that go in a ByteCode
	 * object.
	 * 
	 * @param jb
	 *            The JBasic object containing this session.
	 * @param enclosingStatement
	 *            Most ByteCode is associated with a statement. This parameter
	 *            identifies the Statement object that contains it. This is used
	 *            to link the bytecode back to the calling statement and it's
	 *            enclosing program, which is needed for flow-of-control operations.
	 * @see Instruction
	 * @see Statement
	 * 
	 */

	public ByteCode(final JBasic jb, final Statement enclosingStatement) {
		setSession(jb);
		initializeByteCode(enclosingStatement);
	}

	/**
	 * Initializer called from constructors for making a new byteCode object. A
	 * ByteCode object contains executable code streams representing the work
	 * of a single statement or an entire program. ByteCode objects contain an
	 * array of Instruction objects defining the work to be done to execute the
	 * compiled code.
	 * <p>
	 * A new ByteCode object must have instructions added to it (via the
	 * <code>add()</code> method) before it can be executed.
	 * <p>
	 * In general, the <code>compile()</code> method of each Statement object
	 * is called to generate the instruction streams that go in a ByteCode
	 * object.
	 * 
	 * @param enclosingStatement
	 *            Most ByteCode is associated with a statement. This parameter
	 *            identifies the Statement object that contains it. This is used
	 *            to link the bytecode back to the calling statement and it's
	 *            enclosing program, which is needed for flow of control between
	 *            statements.
	 * @see Instruction
	 * @see Statement
	 * 
	 */

	private void initializeByteCode(final Statement enclosingStatement) {
		byteCode = new ArrayList<Instruction>();
		fDynamicSymbolCreation = true;
		if (enclosingStatement != null) {
			statement = enclosingStatement;
			int ident = enclosingStatement.lineNumber;
			if (ident < 1)
				ident = enclosingStatement.statementID;

			boolean fText = false;
			if( getSession() != null )
				fText = getSession().getBoolean("SYS$STATEMENT_TEXT");
			if( fText) {
				String fmtLine = "";
				if (enclosingStatement.statementLabel != null)
					fmtLine = enclosingStatement.statementLabel + ": ";
				fmtLine = fmtLine + enclosingStatement.statementText;
				add(_STMT, ident, fmtLine);
			}
			else
				add(_STMT, ident );
		}
		returnType = Value.UNDEFINED;
		popReturn = false;
		registers = new RegisterArray(100);
	}

	/**
	 * Format a ByteCode object as a printable string. This is used mostly in
	 * the Eclipse debugger to display an object. It formats the object
	 * characteristics and state as a string.
	 * 
	 * @return A string containing a description of the object, including it's
	 *         linked and running state, number of instructions, and current
	 *         program counter.
	 */
	public String toString() {
		String result = "Bytecode";
		if (byteCode != null)
			result = result + ", " + byteCode.size() + " instructions";
		if (fLinked)
			result = result + ", linked";
		if (fRunning)
			result = result + ", running";
		if (programCounter > 0)
			result = result + ", PC=" + programCounter;
		if (statement != null)
			result = result + ", statement=" + statement;
		return result;
	}

	/**
	 * Get an instruction from the executable stream at a given address. The
	 * instructions are stored in a generic Vector, and this gets the element
	 * from the vector and casts it as an Instruction type.
	 * 
	 * @param i
	 *            The zero-based offset in the instruction array that we are
	 *            accessing.
	 * @return The instruction object stored at that location in the execution
	 *         vector. Since this is a reference to the instruction,
	 *         modifications to the instruction object returned will be
	 *         reflected in future execution of the stream.
	 */
	public Instruction getInstruction(final int i) {
		return byteCode.get(i);
	}

	/**
	 * Store an instruction in the execution vector for the current ByteCode
	 * object.
	 * 
	 * @param i
	 *            The Instruction object to store.
	 * @param n
	 *            The zero-based location in which to store the object. If the
	 *            address represents an already-existing instruction location,
	 *            the instruction at that location is replaced with the
	 *            parameter 'i'. If the location is one location past the end of
	 *            the vector, then the vector is extended by one instruction. It
	 *            is an error to pass an address more than one location larger
	 *            than the current instruction vector size.
	 */
	public void setInstruction(final Instruction i, final int n) {
		if ((n < 0) | (n > byteCode.size() + 1))
			return;
		if (n > byteCode.size())
			byteCode.add(i);
		else
			byteCode.set(n, i);
	}

	/**
	 * Add an already-constructed instruction to the ByteCode stream.
	 * 
	 * @param i
	 *            The Instruction to be added to the stream.
	 * @return The position in the byte code stream where this instruction was
	 *         stored.
	 */
	public int add(final Instruction i) {
		byteCode.add(i);
		return byteCode.size() - 1;
	}

	/**
	 * Add an instruction with no operands to the bytecode stream.
	 * 
	 * @param opcode
	 *            The instruction to add.
	 * @return The byteCode address of the instruction just added.
	 */
	public int add(final int opcode) {
		byteCode.add(new Instruction(opcode));
		return byteCode.size() - 1;
	}

	/**
	 * Add an instruction with a double operand to the bytecode stream.
	 * 
	 * @param opcode
	 *            The instruction to add.
	 * @param d
	 *            The double value to store as the instruction operand
	 * @return The byteCode address of the instruction just added.
	 */
	public int add(final int opcode, final double d) {
		byteCode.add(new Instruction(opcode, d));
		return byteCode.size() - 1;
	}

	/**
	 * Add an instruction with an integer operand to the bytecode stream.
	 * 
	 * @param opcode
	 *            The instruction to add.
	 * @param integer
	 *            The integer value to store as the instruction operand
	 * @return The byteCode address of the instruction just added.
	 */
	public int add(final int opcode, final int integer) {
		byteCode.add(new Instruction(opcode, integer));
		return byteCode.size() - 1;
	}

	/**
	 * Add an instruction with integer and string operands to the bytecode
	 * stream.
	 * 
	 * @param opcode
	 *            The instruction to add.
	 * @param count
	 *            The integer value to store as the instruction operand
	 * @param label
	 *            The string value to store as the instruction operand
	 * @return The byteCode address of the instruction just added.
	 */

	public int add(final int opcode, final int count, final String label) {
		byteCode.add(new Instruction(opcode, count, label));
		return byteCode.size() - 1;
	}

	/**
	 * Add an instruction with an string operand to the bytecode stream.
	 * 
	 * @param opcode
	 *            The instruction to add.
	 * @param label
	 *            The string value to store as the instruction operand. This is
	 *            usually a statement label or a symbol label.
	 * @return The byteCode address of the instruction just added.
	 */

	public int add(final int opcode, final String label) {
		byteCode.add(new Instruction(opcode, label));
		return byteCode.size() - 1;
	}

	/**
	 * Given a Value, generate code that expresses the value in the current
	 * bytecode object, using the various constant and object constructors.
	 * @param v The Value to be stored in the current bytecode
	 * @return the number of instructions currently in the bytecode array.
	 */
	public int add(Value v ) {
		
		int count = 0;
		switch( v.getType()) {
		
		case Value.INTEGER:
			add(ByteCode._INTEGER, v.getInteger());
			break;
			
		case Value.DOUBLE:
			add(ByteCode._DOUBLE, v.getDouble());
			break;
			
		case Value.BOOLEAN:
			add(ByteCode._BOOL, v.getInteger());
			break;
			
		case Value.STRING:
			add(ByteCode._STRING, v.getString());
			break;
			
		case Value.ARRAY:
			count = v.size();
			for( int idx = 1; idx <= count; idx++ )
				add(v.getElement(idx));
			add(ByteCode._ARRAY, count);
			break;
			
		case Value.RECORD:
			ArrayList<String> keys = v.recordFieldNames();
			count = keys.size();
			for( int idx = 0; idx < count; idx++ ) {
				String key = keys.get(idx);
				add(ByteCode._STRING, key);
				add(v.getElement(key));
			}
			add(ByteCode._RECORD, count);
		}
		return byteCode.size() - 1;
	}
	
	/**
	 * Generate a temporary name, that is guaranteed to be unique for the life
	 * of the current JBasic session. This is used to create temporary variable
	 * names in instruction streams, for example.
	 * 
	 * @return A string containing the temporary name.
	 */

	public static String tempName() {
		return "__TEMP" + Integer.toString(JBasic.getUniqueID());
	}

	/**
	 * Add an _END to the byteCode stream if it doesn't already have one.
	 * 
	 * @return true if an instruction had to be added.
	 */
	public boolean end() {
		if (hasEND())
			return false;
		byteCode.add(new Instruction(_END));
		return true;
	}

	/**
	 * Test to see if the ByteCode stream has an _END operation at the end of
	 * the stream, or if there is a final instruction that does not require the
	 * addition of an _END operation, such as an instruction that branches to
	 * another statement.
	 * 
	 * @return true if _END is already the last instruction or one is not
	 *         needed.
	 */
	boolean hasEND() {

		if (byteCode == null)
			return false;

		final int last = byteCode.size();
		if (last == 0)
			return false;

		final Instruction i =  byteCode.get(last - 1);

		if (i.opCode == _END)
			return true;
		if (i.opCode == _BR)
			return true;
		if (i.opCode == _RET)
			return true;
		if (i.opCode == _QUIT)
			return true;
		if (i.opCode == _JMP)
			return true;
		if (i.opCode == _JMPIND)
			return true;
		return false;
	}

	/**
	 * Concatenate a byteCode stream on to the end of the current byteCode
	 * object.
	 * 
	 * @param src
	 *            The byteCode object whose instructions will be added to the
	 *            end of the current object.
	 * @return This method always returns Status.SUCCESS.
	 */
	public Status concat(final ByteCode src) {

		if (src == null)
			return new Status(Status.NOBYTECODE);

		int base = size();
		
		/*
		 * If the last instruction of the stream we are concatenating TO is
		 * an _END, then remove it - otherwise, the concatenated code will
		 * never be executed.
		 */
		if( base > 0 ) {
			Instruction last = byteCode.get(base-1);
			if( last.opCode == ByteCode._END)
				this.remove(--base);
		}
		
		final int count = src.byteCode.size();
		boolean isEnd = false;
		for (int i = 0; i < count; i++) {
			final Instruction in = src.byteCode.get(i);
			if (in.opCode == ByteCode._END) 
				in.opCode = ByteCode._NOOP;
			
			if (((in.opCode == ByteCode._BRZ) |
				 (in.opCode == ByteCode._BR) |
				 (in.opCode == ByteCode._BRNZ))
					&& !in.integerValid) {
				in.integerOperand = count;
				in.integerValid = true;
			}

			/* If this is a branch, offset the branch destination */
			if ((in.opCode > ByteCode._BRANCH_FLAG) && in.integerValid)
				in.integerOperand = in.integerOperand + base;

			byteCode.add(in);
			if( isEnd )
				break;
		}

		return new Status(Status.SUCCESS);
	}

	/**
	 * Disassemble the instructions for the current ByteCode stream.
	 * 
	 */
	public void disassemble() {

		if( getSession() == null ) {
			System.out.println("Unable to disassemble bytecode stream; no session");
			return;
		}
		final int instructionCount = byteCode.size();

		boolean profiling = false;
		profiling = getSession().getBoolean("SYS$PROFILING");

		if (instructionCount == 0  ) {
			getSession().stdout.println("No bytecodes");
			return;
		}
		final String linked = fLinked ? " linked" : "";

		getSession().stdout.println("       Bytecode, " + instructionCount + linked
				+ " instruction" + ((instructionCount == 1) ? "" : "s"));

		for (int address = 0; address < instructionCount; address++) {

			final Instruction in = byteCode.get(address);

			/*
			 * If we are asked to add profiling data to the output, generate
			 * that now.
			 */
			if (profiling) {
				String pdata;
				if (in.counter > 0) {
					pdata = Utility.pad(Integer.toString(in.counter) + "#", 6);
				} else
					pdata = Utility.spaces(6);
				getSession().stdout.print(pdata);
			}

			/*
			 * Output the disassembled instruction at this address.
			 */
			getSession().stdout.println("       " + disassembleInstruction(address, in));

		}
		/*
		 * For diagnostic purposes, dump out the label map if it exists. This is
		 * the map that is used to resolve named labels in the program to
		 * bytecode addresses, and is created by the linker.
		 */

		if (labelMap != null)
			if (labelMap.size() > 0) {
				getSession().stdout.println("Label map:");
				for (final Iterator i = labelMap.values().iterator(); i
						.hasNext();) {
					final Linkage lx = (Linkage) i.next();
					String sx = Utility.pad(Integer.toString(lx.byteAddress), -5);
					getSession().stdout.println("    " + sx + " " + lx.label);
				}
				getSession().stdout.println();
			}

	}

	/**
	 * Disassemble a single ByteCode instruction, and display the formatted
	 * output on the console. This is used both from the disassemble() method
	 * that dumps an entire ByteCode arrayValue, and also from the tracing
	 * function in the ByteCode execution loop.
	 * 
	 * @param address
	 *            Integer containing the address of the instruction to
	 *            disassemble
	 * @param in
	 *            Instruction to disassemble
	 * @return A string containing the formatted output
	 */
	public static String disassembleInstruction(final int address,
			final Instruction in) {

		String addressText = "";
		if (address >= 0) {
			addressText = Integer.toString(address);
			while (addressText.length() < 5)
				addressText = "0" + addressText;
		}

		String opcodeText = Utility.pad(AbstractOpcode.getName(in.opCode), -20);
		if( !in.branchTarget )
			opcodeText = opcodeText.toLowerCase();
		
		/*
		 * Format the numeric argument, if it exists
		 */
		String opcodeArgumentText;
		if (in.integerValid)
			opcodeArgumentText = Integer.toString(in.integerOperand);
		else if (in.doubleValid)
			opcodeArgumentText = Double.toString(in.doubleOperand);
		else
			opcodeArgumentText = "";

		opcodeArgumentText = Utility.pad(opcodeArgumentText, -10);

		/*
		 * Format the string argument, if it exists
		 */

		String stringArgumentText;
		if (in.stringValid)
			stringArgumentText = "\"" + in.stringOperand + "\"";
		else
			stringArgumentText = "";

		StringBuffer result = new StringBuffer();
		result.append(addressText);
		result.append(":  ");
		result.append(opcodeText);
		result.append(' ');
		result.append(opcodeArgumentText);
		result.append(' ');
		result.append(stringArgumentText);
		return result.toString();
	}



	/**
	 * Assemble a textual representation of a bytecode and add it to the current
	 * byteCode structure.
	 * 
	 * @param buffer
	 *            The text to assemble. The text can have comments delimited by
	 *            "//" which are ignored. If no text is in the buffer, then no
	 *            assembly is done and no error returned. Otherwise, the string
	 *            must contain a valid statement of the form:<br>
	 *            <br>
	 *            <code>opcode [integer] ["string"]</code> <br>
	 *            <br>
	 *            The opcode is required. The integer and string are optional,
	 *            and depend on the nature of the opcode. The string must be
	 *            enclosed in double quotation marks. The integer can be signed.
	 * @return Status indicating if the buffer contained a valid ByteCode
	 *         instruction. If there was an error, it is reported as a syntax
	 *         error with additional text in the argument describing the
	 *         specifics of the syntax error.
	 */
	public Status assemble(final String buffer) {
		status = new Status(Status.SUCCESS);
		final Tokenizer t = new Tokenizer(buffer, JBasic.compoundStatementSeparator);

		while (!t.testNextToken(Tokenizer.END_OF_STRING)) {

			String opCodeName = t.nextToken();

			if (opCodeName.equals(".")) {
				opCodeName = t.nextToken();

				/*
				 * .STRING "string", id
				 * 
				 * Adds a string to the string pool. This is used during
				 * assemble of .CODE statements to fetch the string value.
				 */

				if (opCodeName.equalsIgnoreCase("STRING")) {

					if (pool == null)
						pool = new StringPool();

					String str = null;
					if( t.assumeNextSpecial("[")) {
						StringBuffer chars = new StringBuffer();
						while( !t.assumeNextSpecial("]")) {
							chars.append( Character.toString((char)Integer.parseInt(t.nextToken())));
							t.assumeNextSpecial(",");
						}
						str = chars.toString();
					}
					else
						str = t.nextToken();
					
					t.assumeNextToken(",");
					final int id = Integer.parseInt(t.nextToken());

					pool.addString(str, id);

				}
				/*
				 * .MAP "label", v
				 * 
				 * Where label is a string label, and v is a bytecode address
				 * used to create a linkage stored in the labelMap structure.
				 */
				if (opCodeName.equalsIgnoreCase("MAP")) {
					final Linkage lnk = new Linkage();
					lnk.label = t.nextToken().toUpperCase(); /* Label name */
					t.assumeNextToken(",");
					lnk.byteAddress = Integer.parseInt(t.nextToken());
					if (labelMap == null)
						labelMap= new TreeMap<String, Linkage>();
					labelMap.put(lnk.label, lnk);
					return status;
				}

				/*
				 * .CODE opcode, iv [i], dv [d], sv [s] [, opcode...]
				 * 
				 * Where iv, dv, and sv are integer flags. If they are non-zero,
				 * then the i, d, or s value is present and is also parsed as
				 * the integer, double, or string operand.
				 * 
				 * More than one opcode can be expressed on a line.
				 */
				if (opCodeName.equalsIgnoreCase("CODE")) {

					while (true) {
						final Instruction ic = new Instruction(Integer
								.parseInt(t.nextToken()));
						t.assumeNextToken(",");
						int iv = Integer.parseInt(t.nextToken());
						if (iv != 0) {
							ic.integerValid = true;
							ic.integerOperand = Integer.parseInt(t.nextToken());
						} else
							ic.integerValid = false;

						t.assumeNextToken(",");
						iv = Integer.parseInt(t.nextToken());
						if (iv != 0) {
							ic.doubleValid = true;
							ic.doubleOperand = Double
									.parseDouble(t.nextToken());
						} else
							ic.doubleValid = false;

						t.assumeNextToken(",");
						iv = Integer.parseInt(t.nextToken());
						if (iv != 0) {
							ic.stringValid = true;
							ic.stringOperand = pool.getString(Integer
									.parseInt(t.nextToken()));
						} else
							ic.stringValid = false;

						byteCode.add(ic);
						if (t.testNextToken(Tokenizer.END_OF_STRING))
							break;
						t.assumeNextToken(",");
					}

					return status;
				}
			}
			if (!t.isType(Tokenizer.IDENTIFIER))
				return status = new Status(Status.ASMEXPOPCODE);

			final int opCode = AbstractOpcode.getCode(opCodeName);
			if (opCode == 0)
				return status = new Status(Status.ASMINVOPCODE, opCodeName);

			final Instruction i = new Instruction(opCode);

			boolean negate = false;
			if (t.assumeNextToken("-"))
				negate = true;

			if (t.testNextToken(Tokenizer.INTEGER)) {
				t.nextToken();
				i.integerOperand = Integer.parseInt(t.getSpelling());
				if (negate)
					i.integerOperand = -(i.integerOperand);
				i.integerValid = true;
			}

			if (t.testNextToken(Tokenizer.DOUBLE)) {
				t.nextToken();
				i.doubleOperand = Double.parseDouble(t.getSpelling());
				if (negate)
					i.doubleOperand = -(i.doubleOperand);
				i.doubleValid = true;
			}

			if (t.testNextToken(Tokenizer.STRING)) {
				t.nextToken();
				i.stringOperand = t.getSpelling() + "";
				i.stringValid = true;
			}

			byteCode.add(i);

			if (!t.assumeNextSpecial(","))
				break;

		}

		if (status.success() & !t.testNextToken(Tokenizer.END_OF_STRING))
			status = new Status(Status.EXTRA, t.getRemainder());

		return status;
	}

	/**
	 * Remove a bytecode from the bytecode vector, and update any addresses
	 * affected by the deletion.
	 * 
	 * @param addr
	 *            The address of the instruction to remove from the vector.
	 */
	public void remove(final int addr) {

		/*
		 * Step one. Remove the actual byte-code
		 */
		byteCode.remove(addr);

		/*
		 * Step two. Find any branch address instruction operands that now must
		 * be offset by one, because they point to a location after the address
		 * we just deleted.
		 */
		
		final int len = byteCode.size();
		for (int n = 0; n < len; n++) {

			final Instruction i = byteCode.get(n);
			if ((i.opCode > _BRANCH_FLAG) && i.integerValid && (i.integerOperand > addr)) {
					i.integerOperand--;
				}
		}

		/*
		 * Step three.  Update the label map to see if any labels now need
		 * to be shifted down.  This must be kept accurate or indirect
		 * branches may not work correctly after optimization.
		 */
		if( this.labelMap != null ) {

			Iterator i = this.labelMap.keySet().iterator();
			while (i.hasNext()) {
				String key = (String) i.next();
				Linkage l = labelMap.get(key);
				if (l.byteAddress > addr)
					l.byteAddress--;
			}
		}
	}

	/**
	 * Add a bytecode to the bytecode vector, and update any addresses
	 * affected by the insertion.
	 * 
	 * @param addr
	 *            The address of the vector at which to insert a new 
	 *            instruction
	 * @param newInstruction
	 * 			The Instruction object to be inserted at the location 'addr'.
	 */
	public void insert(final int addr, Instruction newInstruction ) {

		/*
		 * Step one. Insert the actual instruction
		 */
		
		byteCode.add(addr, newInstruction);

		/*
		 * Step two. Find any branch address instruction operands that now must
		 * be offset by one, because they point to a location after the address
		 * we just inserted.
		 */
		final int len = byteCode.size();
		for (int n = 0; n < len; n++) {

			final Instruction i = byteCode.get(n);
			if ((i.opCode > ByteCode._BRANCH_FLAG) && i.integerValid && (i.integerOperand > addr)) {
					i.integerOperand++;
				}
		}
		/*
		 * Step three.  Update the label map to see if any labels now need
		 * to be shifted up.  This must be kept accurate or indirect
		 * branches may not work right after optimization, etc.
		 */
		if( this.labelMap != null ) {
			
		Iterator i = this.labelMap.keySet().iterator();
			while (i.hasNext()) {
				Object n = i.next();
				Linkage l = labelMap.get(n);
				if (l.byteAddress >= addr)
					l.byteAddress++;
			}
		}

	}


	/**
	 * Return the size (in instructions) of the bytecode for this ByteCode
	 * object.
	 * 
	 * @return Integer number of instructions, or zero if no bytecode has been
	 *         generated.
	 */
	public int size() {
		if (byteCode == null)
			return 0;
		return byteCode.size();
	}

	/**
	 * Return the address of the most recently-generated instruction. This is
	 * identical to the return value from the add() method that stored the
	 * instruction.
	 * 
	 * @return The zero-based address of the most recently-generated
	 *         instruction.
	 */
	int mark() {
		return byteCode.size() - 1;
	}

	/**
	 * Update an instruction at a given location with the address of our current
	 * instruction. This is used to back-patch forward references in _BRZ and
	 * _BR instructions.
	 * 
	 * @param addr
	 *            The address of the instruction to be patched to reference the
	 *            current location. Note that "current location" here means the
	 *            next instruction that will be generated, not the last one that
	 *            was generated.
	 */
	public void patch(final int addr) {
		final Instruction i = byteCode.get(addr);
		i.integerOperand = byteCode.size();
		i.integerValid = true;
	}

	/**
	 * Pop the topmost value from the stack. The stack pointer is adjusted to
	 * remove this item from the stack.
	 * 
	 * @return The top Value object.
	 * @throws JBasicException
	 *             indicates a stack underflow
	 */
	public Value pop() throws JBasicException /* throws JBasicException */{
		final int top = dataStack.size();
		if (top < 1) {
			status = new Status(Status.UNDERFLOW);
			throw new JBasicException(Status.UNDERFLOW);
		}
		/* The stack is zero based */
		Value v = dataStack.remove(top - 1);
		return v ; 
	}


	/**
	 * Push an already-typed Value object on the stack.
	 * 
	 * @param d
	 *            The Value to add to the top of the stack.
	 */
	public void push(final Value d) {		
		dataStack.add(d);
	}

	/**
	 * Push an integer on the stack. Creates a Value object from the integer,
	 * and pushes it.
	 * 
	 * @param i
	 *            The integer to add to the top of the stack.
	 */
	public void push(final int i) {
		push(new Value(i));
	}

	/**
	 * Push a double on the stack. Creates a Value object from the double, and
	 * pushes it.
	 * 
	 * @param d
	 *            The double to add to the top of the stack.
	 */
	public void push(final double d) {
		push(new Value(d));
	}

	/**
	 * Push a string on the stack. Creates a Value object from the string, and
	 * pushes it.
	 * 
	 * @param s
	 *            The string to add to the top of the stack.
	 */
	public void push(final String s) {
		push(new Value(s));
	}

	/**
	 * Push a boolean on the stack. Creates a Value object from the boolean, and
	 * pushes it.
	 * 
	 * @param b
	 *            The boolean to add to the top of the stack.
	 */
	public void push(final boolean b) {
		push(new Value(b));
	}

	/**
	 * Mark whether this bytecode stream is to be debugged or not. If it is, an
	 * instance of a debugger is created. If it is not to be debugged, then the
	 * debugger is destroyed.
	 * 
	 * @param fDebug true if the debugger is to be enabled, else false.
	 */
	public void debug(final boolean fDebug) {
		if (fDebug && getSession() != null ) {
			if (debugger == null)
				debugger = new JBasicDebugger(getSession().stdin(), getSession().stdout);
			else
				debugger.stepCounter = 1;
		} else
			debugger = null;
	}

	/**
	 * Attach a debugger to this code stream.
	 * 
	 * @param dbg
	 *            The debugger instance to associate with the code stream, or
	 *            null if no debugger is to be attached.
	 */
	public void setDebugger(final JBasicDebugger dbg) {
		debugger = dbg;
	}

	/**
	 * Execute stored Bytecode. The current ByteCode object is run, using the
	 * provided symbol table anchor for symbol name resolution. The current
	 * program must also be correctly set for some operations to work.
	 * 
	 * @param s
	 *            The symbol table used to manage symbol resolution
	 * @param start 
	 * 			  The staring line number to begin execution at, or zero
	 * 			  if the program starts at the beginning (the default case).
	 * 			  If this is a negative number, then ABS(start) is used as
	 * 			  the initial program counter as opposed to line number.
	 * 
	 * @return Returns a Status object describing the success of the execution.
	 */

	public Status run(final SymbolTable s, int start) {

		programCounter = 0;
		fRunning = true;
		lastLineNumber = 0;
		JBasic currentSession = getSession();
		boolean savedTraceState = ( s == null? false : s.getBoolean("SYS$TRACE_STATEMENTS"));
		stringPool = new StringPool();
		
		/*
		 * The stack rarely exceeds 32 elements.  Pre-allocate 
		 * the space so that we eliminate early vector doubling
		 * (the actual default initial size depends on the
		 * underlying implementation.)
		 */
		dataStack = new ArrayList<Value>(initialStackSize);
		
		final int maxPC = byteCode.size();
		boolean fByteCodeTrace = s == null ? false : s.getBoolean("SYS$TRACE_BYTECODE");
		boolean fStatementTrace = s == null ? false : s.getBoolean("SYS$TRACE_STATEMENTS");
		boolean fProtected = false;
		if( statement != null )
			if( statement.program != null )
				if( statement.program.isProtected()) {
					fProtected = true;
					//if((fByteCodeTrace || fStatementTrace) && currentSession != null )
					//	currentSession.stdout.println("<<<Tracing disabled in protected program " 
					//			+ statement.program.getName() + ">>>");

					fByteCodeTrace = false;
					fStatementTrace = false;
				}
		/*
		 * If the program we're being asked to execute requests strong typing,
		 * mark our symbol table accordingly. The default is to use the system
		 * state if there is no program state available.
		 */
		
		boolean fStatic =  s == null ? false : s.getBoolean("SYS$STATIC_TYPES");
		if( s != null )
			s.setStrongTyping(fStatic);

		if (statement != null) {
			if (statement.program != null) {
				if( s != null )
					s.setStrongTyping(statement.program.fStaticTyping);
				name = statement.program.getName();
			}
		}
		
		if (maxPC == 0)
			return new Status(Status.SUCCESS);

		if( currentSession == null )
			fByteCodeTrace = false;
		
		if (fByteCodeTrace ) {
			String statementText = "";
			if (statement != null)
				statementText = "[" + (statement.statementID + 1) + "] "
						+ statement.statementText;
			if( currentSession != null)
				currentSession.stdout.println("ByteCode: " + statementText);
		}

		if (ByteCode.dispatchVector == null)
			dispatchVector = AbstractOpcode.initialize();

		/*
		 * If this code segment has an error handler, then we need to
		 * create a new slot in the ON statement handler stack.
		 */
		int mark = -1;
		
		if ( fHasErrorHandler && currentSession != null )
			mark = currentSession.onStatementStack.push("Frame for "
					+ statement.statementText);

		InstructionContext env = new InstructionContext(currentSession, 
				this, null, s, fStatementTrace);
		
		/*
		 * If we were given a starting line number, find that now and make it
		 * the starting point in the program.    The CHAIN statement is an
		 * example which optionally allows the specification of a starting line 
		 * number.
		 * 
		 * If no line number was given (the value is zero) then we just start 
		 * at the beginning, which is the normal default case.
		 */
	
		Instruction i = null;

		if( start > 0 ) {
			boolean foundStart = false;

			for( programCounter = 0; programCounter < maxPC; programCounter++ ) {
				i = byteCode.get(programCounter);
				if( i.opCode == ByteCode._STMT && i.integerOperand == start ) {
					foundStart = true;
					break;
				}
			}
			if(!foundStart)
				return status = new Status(Status.LINENUM, start);
		}
		
		/*
		 * If the starting value is a negative number, it's an actual bytecode
		 * address (like from a _CALLP to an internal SUBroutine) and we should
		 * just start there.
		 */
		else
			if( start < 0 )
				programCounter = -start;
		/*
		 * Run the program in a loop until there is an interrupt, an error,
		 * or we run past the end of the bytecode array.
		 */
		while (fRunning) {

			if (programCounter >= maxPC)
				break;


			/*
			 * CHECK FOR INTERRUPT FROM THE USER. This isn't thread-safe,
			 * stacked up attentions might be lost since access to abort isn't
			 * serialized yet....
			 */
			
			boolean fAbort = JBasic.interruptSignalled;
			if( currentSession != null )
				fAbort = fAbort | currentSession.isAbortSignalled();
			if (fAbort) {
				if( JBasic.interruptSignalled )
					JBasic.interruptSignalled = false;
				if( currentSession != null )
					currentSession.setAbort(false);
				status = new Status(Status.INTERRUPT);
			}

			/*
			 * We're not interrupted, so execute an instruction.
			 */
			else {
				
				/*
				 * Get the instruction from the byteCode array.
				 */
				
				i = byteCode.get(programCounter++);
				
				if( i.opCode == _TRACE && i.integerOperand > 0 && !fProtected ) {
					if(( i.integerOperand % 2 ) == 1 )
						fByteCodeTrace = true;
					if(( i.integerOperand / 2 ) == 1 )
						fStatementTrace = true;
				}				
				/*
				 * If we are tracing byteCodes then we've got some extra
				 * work to do to format the disassembly and the top of stack.
				 */
				if (fByteCodeTrace ) {

					String pc = Integer.toString(programCounter - 1);
					while (pc.length() < 5)
						pc = "0" + pc;

					if( currentSession != null ) {
						currentSession.stdout.print("ByteCode " + pc + ": ");
						OpDEBUG.printStackObject(currentSession.stdout, this);
						currentSession.stdout.println("         "
							+ ByteCode.disassembleInstruction(
									programCounter - 1, i));

						currentSession.stdout.println();
					}
				}
				if( i.opCode == _TRACE && i.integerOperand == 0 )
					fByteCodeTrace = false;
				
				/*
				 * Let's execute the instruction.  This is in a try-catch block
				 * to intercept errors thrown from the runtime, usually stack
				 * underflow operations caused by bad code generation.
				 */
				try {

					env.setInstruction( i );
					if( currentSession != null )
						currentSession.instructionsExecuted++;
					status = null;
					
					int opCode = i.opCode;
					if( opCode > ByteCode._BRANCH_FLAG)
						opCode = opCode - ByteCode._BRANCH_FLAG;
					
					if (dispatchVector[opCode] != null) {
						final AbstractOpcode op = dispatchVector[opCode];
						i.counter++;
						op.execute(env);
					}
					else
					if (opCode == ByteCode._NOOP)
						status = null;
					else
						status = new Status(Status.UNIMPBYTECODE, AbstractOpcode
							.getName(opCode));

				} catch (ArithmeticException e) {
					status = new Status(Status.MATH, e.getMessage());
				} catch (final JBasicException e) {
					status = e.getStatus();
				} catch (final Exception e) {
					status = new Status(Status.FAULT, e.toString());
				}
			}

			/*
			 * The default case is that status is still null, which 
			 * means success - loop to the next instruction.
			 */
			if (status == null)
				continue;

			/*
			 * If the statement causes a flow-of-control change, quit executing
			 * tokens.
			 */
			if (status.equals(Status.RETURN) |
				status.equals("*END") | 
				status.equals("*STEP"))
					break;

			/*
			 * And of course if there was an error, can we handle it ourselves?
			 * If we're a linked bytecode stream, then we do this ourselves.
			 * Otherwise it's handled by the interpreter level that called us,
			 * and we just pass it on up.
			 */
			
			if (status.failed()) {
				if( this.lastLineNumber > 0 )
					status.setWhere( this.name, this.lastLineNumber);
				
				if( s != null )
					try {
						s.insert("SYS$STATUS", new Value(status));
					} catch (JBasicException e) {
						return e.getStatus();
					}
				if (fLinked) {
					
					String code = status.getCode();
					String label = null;
					if( currentSession != null ) {
						label = currentSession.onStatementStack.fetch(code);

						if (label == null) {
							code = "*";
							label = currentSession.onStatementStack.fetch(code);
						}
					}
					if (label != null) {
						
						/*
						 * We have a destination label.  Before we can do
						 * anything else, scrub the stack... the error 
						 * may have occurred mid-instruction and left 
						 * anything on the stack; the _STMT handler on 
						 * the destination may get testy about it if we
						 * don't give them a clean runtime stack.
						 */
						
						while(stackSize() > 0 ) {
							try {
								pop();
							} catch (JBasicException e) {
								break;
							}
						}
						/*
						 * We'll need a location to jump to based on the label.
						 */
						Linkage whereTo = null;
						
						/*
						 * Is this the label for an implied GOSUB operation?
						 * This was stored in the label data when the ON
						 * statement was parsed.
						 */
						
						if( label.charAt(0) == '>') {
							
							final ScopeControlBlock scope = new ScopeControlBlock();
							whereTo = labelMap.get(label.substring(1));
							scope.targetStatement = whereTo.byteAddress;

							scope.scopeType = ScopeControlBlock.GOSUB;
							
							/*
							 * We can't just return to the next instruction, because
							 * it could be in the middle of a statement.  Find the
							 * next statement boundary.
							 */
							
							Instruction skipInst = getInstruction(programCounter);
							if( skipInst.opCode != ByteCode._STMT)
								while( programCounter < this.size()) {
									skipInst = getInstruction(programCounter);
									if( skipInst.opCode == ByteCode._STMT)
										break;
									programCounter++;
							}
							else
								programCounter = programCounter + 1;
							
							scope.returnStatement = programCounter-1;
							
							scope.activeProgram = statement.program;

							if (statement.program.gosubStack == null)
								statement.program.gosubStack = new ArrayList<ScopeControlBlock>();

							statement.program.gosubStack.add(scope);
							programCounter = scope.targetStatement;
							status = new Status();
							continue;
						}
						
						/*
						 * Must be a label for a simple GOTO so branch there.
						 */
						whereTo = labelMap.get(label);
						if (whereTo != null) {
							programCounter = whereTo.byteAddress;
							status = new Status();
							continue;
						}

						/*
						 * Never found the label, so error out.
						 */
						new Status(Status.NOSUCHLABEL, label).print(currentSession);
						break;
					}
				}
				break;
			}
		}

		/*
		 * Because a null status is passed around for efficiency to denote the
		 * default successful state of a ByteCode, it might be null now. Let's
		 * go ahead and make it real.
		 */

		if (status == null)
			status = new Status();

		/*
		 * Release the execution stack for this element, if it was set. Also, if
		 * we're executing linked code, then a RETURN means exit from this
		 * executable code, not the caller - mask the RETURN status in this
		 * case.
		 */

		if( mark >= 0 && currentSession != null )
			currentSession.onStatementStack.pop(mark);
		if (status.equals(Status.RETURN))
			status = new Status();
		
		boolean newTraceState = s == null ? false : s.getBoolean("SYS$TRACE_STATEMENTS");
		if( newTraceState != savedTraceState && s != null ) {
			Value v = s.findGlobalTable().localReference("SYS$TRACE_STATEMENTS");
			v.setBoolean(savedTraceState);
		}
		return status;
	}


	/**
	 * Dump the current ByteCode stream (either human-readable or encoded) to a
	 * file referenced previously opened.  This is used to save programs to
	 * a Workspace file, which can be a blend of protected and unprotected
	 * programs.
	 * 
	 * @param outputFile
	 *            the previously-opened file
	 */

	public void saveProtectedBytecode(final JBFOutput outputFile) {
		final int ixlen = byteCode.size();
		int ix;

		StringBuffer pbuffer = null;
		final StringPool pool = new StringPool();
		Instruction inst;

		/*
		 * Scan the bytecode and output all the strings.  These are
		 * stored in a local pool as well, so we can reference them by
		 * index into the pool.  This way, each unique string is referenced
		 * by only a single .STRING id.
		 */

		for (ix = 0; ix < ixlen; ix++) {
			inst = getInstruction(ix);
			if (inst.stringValid) {
				int id = pool.getStringID(inst.stringOperand);
				if (id == 0) {
					id = pool.addString(inst.stringOperand);
					outputFile.println(": .STRING " + charArray(inst.stringOperand)
							+ ", " + id);
				}
			}
		}


		/*
		 * Scan over the instructions and store them away.
		 */
		for (ix = 0; ix < ixlen; ix++) {
			inst = getInstruction(ix);


			if (pbuffer == null)
				pbuffer = new StringBuffer(": .CODE ");
			else
				pbuffer.append(", ");

			StringBuffer is = new StringBuffer(Integer.toString(inst.opCode));
			is.append(", ");
			is.append((inst.integerValid ? '1' : '0'));
			if (inst.integerValid) {
				is.append(" ");
				is.append(Integer.toString(inst.integerOperand));
			}
			is.append(", ");
			is.append(inst.doubleValid ? '1' : '0');
			if (inst.doubleValid) {
				is.append(" ");
				is.append(Double.toString(inst.doubleOperand));
			}
			is.append(" ");
			is.append(inst.stringValid ? '1' : '0');
			if (inst.stringValid) {
				is.append(' ');
				is.append(pool.getStringID(inst.stringOperand));
			}
			pbuffer.append(is);
			is = null;
			if (pbuffer.length() > 60) {
				outputFile.println(pbuffer.toString());
				pbuffer = null;
			}

		}

		if (pbuffer != null)
			outputFile.println(pbuffer.toString());

		/*
		 * If there is a label map for this program, write that information
		 * as well.
		 */
		if (labelMap != null)
			for (final Iterator i = labelMap.values().iterator(); i.hasNext();) {
				final Linkage l = (Linkage) i.next();
				outputFile.println(": .MAP \"" + l.label + "\", "
						+ l.byteAddress);
			}

	}

	private String charArray(String stringOperand) {
		StringBuffer r = new StringBuffer("[");
		
		for( int ix = 0; ix < stringOperand.length(); ix++ ) {
			int i = stringOperand.charAt(ix);
			if( ix > 0 )
				r.append(',');
			r.append(Integer.toString(i));
		}
		r.append(']');
		return r.toString();
	}

	/**
	 * Accessor function to access the popReturn attribute, which indicates if a
	 * RETURN statement should move the top of the stack into the caller's local
	 * storage into a ARG$RESULT variable.
	 * 
	 * @return True if the popReturn attribute is set for the current bytecode
	 *         stream.
	 */
	public boolean getPopReturn() {
		return popReturn;
	}

	/**
	 * Given a bytecode address in the current program, determine if there is a
	 * label for that bytecode address that should be used in displaying a
	 * statement.
	 * 
	 * @param byteCodeAddress
	 *            the 0-based address of the current instruction.
	 * @return A string containing the label, or null if there is no label.
	 */
	public String findLabel(final int byteCodeAddress) {
		if (labelMap != null)
			if (labelMap.size() > 0)
				for (final Iterator i = labelMap.values().iterator(); i
						.hasNext();) {
					final Linkage lx = (Linkage) i.next();
					if (lx.byteAddress == byteCodeAddress)
						return lx.label;
				}
		return null;
	}
	
	/**
	 * Given a label, return byte bytecode address in the compiled and linked
	 * program of that label.  This will be the _STMT opcode for the next
	 * statement following the label.
	 * @param label the label string (must be uppercase)
	 * @return the integer byteCode location, or 0 if there is no such
	 * matching label.
	 */
	public int findLabel(final String label ) {
		if( labelMap == null )
			return 0;
		
		final Linkage lx = labelMap.get(label);
		if( lx == null )
			return 0;
		
		return lx.byteAddress;
	}
	
	/**
	 * Return the container object for this bytecode stream.
	 * @return a JBasic object.
	 */
	public JBasic getEnvironment() {
		return getSession();
	}

	/**
	 * @return The name assigned to this bytecode stream.  This is used
	 * in constant pooling, for example.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Set the name of teh current bytecode stream.
	 * @param bytecodeName the name to set.
	 */
	public void setName(String bytecodeName ) {
		name = bytecodeName;
	}

	/**
	 * Discard a given number of elements from the stack.
	 * @param argc the number of arguments to discard.  This
	 * value must be a positive integer.
	 */
	public void discard(int argc) {
		
		if( argc < 0 )
			return;
		
		int top = dataStack.size();
		for( int ix = 0; ix < argc; ix++ )
			dataStack.remove(--top);	
	}
	
	/**
	 * Return a pointer to a bytecode stream for a locally-defined function.
	 * @param functionName The name of the function in UPPERCASE.
	 * @return The bytecode object that contains the function definition, or
	 * null if there is no such local function available.
	 */
	public ByteCode findLocalFunction( String functionName ) {
		
		if( this.statement == null )
			return null;
		if( this.statement.program == null )
			return null;
		
		return this.statement.program.findLocalFunction(functionName);
	}

	/**
	 * Assume that the current ByteCode object is a function, call it
	 * using the function arguments provided in the ArgumentList object.
	 * @param session The container JBasic session for this session
	 * @param functionName the String name of the function to call.
	 * @param args The argument list structure containing the function
	 * call arguments.  This can be null, which means there are no
	 * arguments.
	 * @param symbols the current symbol table.  This can be null,
	 * in which case the call is made with only a local-to-the-call
	 * temporary symbol table.
	 * @return a Value containing the result of the function call.
	 * @throws JBasicException EXPRETVAL if the called byte code
	 * stream does not have a RETURN statement that returns a value
	 * to the caller.
	 */
	public Value call(JBasic session, String functionName, ArgumentList args,
			SymbolTable symbols) throws JBasicException {
		
		/*
		 * The argument list must be broken apart again to set up
		 * the implicit "CALL" operation that is implied by the
		 * function invocation.
		 * 
		 * This means that each argument is given a slot in the new
		 * function's symbol table in the array $ARGS.  The calling
		 * program can reference the argument list via this array at
		 * any time, in addition to referencing the arguments via
		 * explicitly named parameters.
		 */

		final SymbolTable newTable = new SymbolTable(session, "Local to "
				+ functionName, symbols);
		
		final Value argArray = new Value(Value.ARRAY, null);
		
		/*
		 * Note that you can call with a null argument list, which means
		 * no arguments processed.
		 */
		
		if( args != null )
			for (int argcount = 0; argcount < args.size(); argcount++)
				argArray.addElement(args.element(argcount));
		newTable.insertLocal("$ARGS", argArray);
		
		/*
		 * Define additional local variables that tell the function
		 * about itself, how it got called, and the name of the
		 * calling program or function.
		 */
		newTable.insertLocal("$MODE", new Value("FUNCTION"));
		newTable.insertLocal("$THIS", new Value(functionName));
		String parentName = "Console";
		
		Program cp = session.programs.getCurrent();
		if (cp != null) {
			if (cp.isActive())
				parentName = cp.getName();
		}
		newTable.insertLocal("$PARENT", new Value(parentName));

		/*
		 * Now run the program with the new symbol table and the
		 * supplied debugger object (if any).  After we return,
		 * we clear out the running status so it does not confuse
		 * someone looking at the bytecode, such as a DISASM command.
		 */
		Status sts = this.run(newTable, 0);
		this.fRunning = false;
		this.programCounter = 0;
		
		/*
		 * If there was a result, get it now.  If no result, then
		 * this wasn't a valid CALL operation.
		 */
		
		Value d = newTable.findReference("ARG$RESULT", false);
		if( d == null ) {
			throw new JBasicException(Status.EXPRETVAL);
		}

		
		/*
		 * If the result of the function call was an error, then
		 * let's print it now (and trigger any ON-unit handlers).
		 * If the status was successful, the method does nothing.
		 */
		sts.printError(session);
		return d;
	}

	/**
	 * Return the text of a statement given it's statement ID, which
	 * can be the statement ID number or a line number.
	 * @param statementID if a negative integer, then abs(statementID) 
	 * is the ordinal statement number (i.e. -4 is the fourth statement).
	 * If a positive integer, then this is a line number from the 
	 * source code.
	 * @return a string containing the statement text if available, or
	 * the string "<no statement text available>".
	 */
	public String getStatementText(int statementID) {
		
		String stmtText = "<no statement text available>";
		
		if( this.statement == null )
			return stmtText;
		
		Program program = this.statement.program;
		Statement s = null;
		
		if( program == null ) {
			if( this.statement.statementText == null )
				return stmtText;
			return this.statement.statementText;
		}
		
		if( statementID < 0 )
			s = program.getStatement(-statementID);
			else
				s = program.findLineNumber(statementID);
		if( s == null )
			return stmtText;
		if( s.statementText == null )
			return stmtText;
		return s.statementText;
	}
	
	/**
	 * Return an array object that contains the current contents of the runtime stack.
	 * This is typically used by a debugger that wishes to examine or display the
	 * runtime stack.  The actual runtime stack itself is not disturbed.  Note that
	 * the resulting ARRAY Value contains the actual stack Values, not copies, so the
	 * contents of the result of this function should not be modified unless the
	 * user intends to change the runtime stack.
	 * @return a Value containing an array.
	 */
	public Value getStack() {
	
		Value result = new Value(Value.ARRAY, null);
		
		if( this.dataStack != null ) {
			for( int i = 0; i < dataStack.size(); i++ ) 
				result.addElement(dataStack.get(i));
		}
		return result;
	}
	
	/**
	 * Return the current runtime stack as a string-formatted array.
	 * @return string containing formatted copy of runtime stack information.
	 */
	public String stackToString() {
		return getStack().toString();
	}

	/**
	 * Add a reference capture string to the current reference operation,
	 * if there is one in effect.
	 * @param stringOperand the variable reference to add to the capture buffer.
	 */
	public void refSecondary(String stringOperand) {
		if( referenceCaptureBuffer != null )
			referenceCaptureBuffer.append(stringOperand);
		
	}
	/**
	 * Add a reference capture string to the current reference operation,
	 * if there is one in effect.  If, however, there is already a primary
	 * reference, this this is not added.  This prevents the problem of
	 * a reference like <code>X[N+1]</code> generating a primary reference
	 * for <code>N</code>.  So if there is already information in the
	 * capture buffer, we are not really a primary reference and no work
	 * is done.
	 * @param stringOperand the variable reference to add to the capture buffer.
	 * @param override if true, the variable reference is stored in the buffer
	 * even if there is already a name there.
	 */

	public void refPrimary(String stringOperand, boolean override) {
		if( referenceCaptureBuffer != null ) {
			if( override )
				referenceCaptureBuffer = new StringBuffer(stringOperand);
			else
				if( referenceCaptureBuffer.length() == 0 )
					referenceCaptureBuffer.append(stringOperand);
		}
	}
	
	/**
	 * Return the reference capture buffer if there is one.
	 * @return The current buffer if there is one, else null.
	 */
	public String getCaptureBuffer() {
		if( referenceCaptureBuffer == null )
			return null;
		return referenceCaptureBuffer.toString();
	}

	/**
	 * Set the state of the capture buffer.  If enabled, the buffer is
	 * set as an empty buffer.  If disabled, the buffer is destroyed.
	 * @param enable true if we are to capture data in a new empty
	 * string buffer.
	 */
	public void setCaptureBuffer(boolean enable ) {
		if( enable )
			referenceCaptureBuffer = new StringBuffer();
		else
			referenceCaptureBuffer = null;
	}

	/**
	 * Indicate if there is an active capture buffer, containing
	 * the text description of a set of value references.
	 * @return true if there is an active capture buffer
	 */
	public boolean hasCaptureBuffer() {
		return (referenceCaptureBuffer != null);
	}

	/**
	 * Set the session for the current ByteCode object
	 * @param jbenv the active session
	 */
	public void setSession(JBasic jbenv) {
		this.session = jbenv;
	}

	/**
	 * Get the active session for this bytecode
	 * @return null if no session associated.
	 */
	public JBasic getSession() {
		return session;
	}

	/**
	 * Given a line number, search the code for the matching _STMT and return
	 * the address of the following instruction.
	 * @param lineNumber a line number in the program.
	 * @return the bytecode address of that statement, or zero if the line 
	 * number does not exist.
	 */
	public int findLineNumber(int lineNumber) {
		int len = byteCode.size();
		for( int ix = 0; ix < len; ix++ ) {
			Instruction i = getInstruction(ix);
			if( i.opCode == ByteCode._STMT && i.integerValid && i.integerOperand == lineNumber )
				return ix;
		}
		return 0;
	}
}

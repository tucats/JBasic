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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.value.Value;

/**
 * Define a Loop Control Block object. 
 * 
 * This class defines the control block for handling loops
 * in the JBasic language. Loops can be FOR-NEXT loops, or they can be DO-WHILE
 * loops. In either case, the loop control object knows how to process a "next
 * iteration" of the loop.
 * 
 * @author Tom Cole
 */

public class LoopControlBlock {

	/**
	 * The name of the index variable, if any. In a FOR-NEXT loop, this is the
	 * name of the index variable. In a DO-WHILE there is no index variable and
	 * this is null.
	 */
	public String indexVariableName;

	/**
	 * The name of the container that holds the values for a FOR EACH operation.
	 */
	public String eachList;
	
	/**
	 * Type of this loop, such as LOOP_FOR or LOOP_DO. See the list of
	 * enumerated LOOP types for a description of each type.
	 */
	public int loopType;

	/**
	 * The terminating value of a FOR-NEXT loop.  Normally, this is always
	 * greater than the initial value, except when the increment is a 
	 * negative number.
	 */
	public Value end;

	/**
	 * The increment of a FOR-NEXT loop. By default the increment is the value
	 * 1.0, but the increment can be explicitly set using a BY clause. The
	 * increment can be a negative number, in which case the end should be less
	 * than the initial value.
	 */
	public Value increment;

	/**
	 * The statement number of the top of the loop. This is the index into the
	 * vector of statements in the current program where the loop returns when
	 * the exit condition is not satisfied. If the loop exit condition is met,
	 * then the loop falls through to the next statement after the loop.
	 */
	public int statementID;

	/**
	 * The offset address in the bytecode for this statement where the _FOR
	 * instruction was emitted, when this is a compiled statement. We use this
	 * to back-patch the FOR statement's address for the NEXT.
	 */
	int offset;

	/**
	 * The symbol table used to resolve the exit condition expression.
	 */
	SymbolTable symbols;

	/**
	 * Current position in the FOR EACH list, if used.
	 */
	public int eachCounter;

	/**
	 * Size of the FOREACH list when there is one.
	 */
	public int eachLength;

	/**
	 * ByteCode address of the _LOOP instruction+1, which is
	 * the "exit point" when an END LOOP instruction is found.
	 */
	public int exitAddress;

	/**
	 * ByteCode address of the _DO instruction, which is
	 * where the CONTINUE LOOP instruction branches.
	 */
	public int startAddress;

	/**
	 * Loop type for a FOR-NEXT loop.
	 */
	static public final int LOOP_FOR = 1;

	/**
	 * Loop type for a WHILE loop, not currently implemented.
	 */
	static public final int LOOP_WHILE = 2;

	/**
	 * Loop type for a DO-WHILE loop.
	 */
	static public final int LOOP_DO = 3;

	/**
	 * Loop type for a FOR EACH loop.
	 */
	static public final int LOOP_FOREACH = 4;
	
	/**
	 * Create a loop control block of a specific type. This constructor also
	 * binds the symbol table name to the loop block for future symbol
	 * management (such as incrementing an index variable in a FOR-NEXT loop).
	 * 
	 * @param theLoopType
	 *            Type of loop, such as LOOP_FOR
	 * @param localSymbols
	 *            Symbol table used to evaluate exit conditions, etc.
	 */
	public LoopControlBlock(final int theLoopType, final SymbolTable localSymbols) {
		indexVariableName = null;
		loopType = theLoopType;
		end = null;
		increment = null;
		statementID = -1;
		symbols = localSymbols;
		eachList = null;
	}

	/**
	 * @return generated name of a string variable for the list of
	 * elements in the FOR EACH loop.
	 */
	public String createEachList() {
		return eachList = "_EACH_" + JBasic.getUniqueID();
	}
	
	/**
	 * Evaluate a loop to determine if it's done or not. The loop type controls
	 * the internal evaluation method. The return is an indicator that the loop
	 * has completed execution; the caller uses this fact to decide how to
	 * branch the loop.
	 * 
	 * @return True if the loop as satisfied it's exit condition
	 * @throws JBasicException an unknown symbol, function call error, or a
	 * math error such as divide-by-zero occurred.
	 */
	public boolean evaluate() throws JBasicException {

		boolean done = false;

		/*
		 * Depending on which kind of loop it is, do the right evaluation
		 */

		switch (loopType) {

		/*
		 * FOR EACH <list> DO
		 */
		case LOOP_FOREACH:
			
			/*
			 * Step to the next element in the FOR EACH list.
			 */
			eachCounter++;
			if( eachCounter > eachLength) {
				symbols.deleteAlways(this.eachList);
				return true;
			}
			/*
			 * Get the next element value.  If we are at the end of the
			 * list, we can delete the temporary array created to hold
			 * the list.
			 */
			Value list = symbols.localReference(this.eachList);
			if( eachCounter > list.size()) {
				symbols.deleteAlways(this.eachList);
				return true;
			}
			
			/*
			 * Set the index variable (named by iName) to the value of 
			 * the new item.
			 */
			symbols.insert(indexVariableName, list.getElement(eachCounter));
			return false;
			
		/*
		 * FOR .. NEXT.
		 * 
		 * A FOR-NEXT loop. In this case, iName is the name of the
		 * variable used to hold the value to be indexed.
		 */
		case LOOP_FOR:

			/*
			 * Get the index, and do the math...
			 */
			final Value indexVariable = symbols.reference(indexVariableName);

			Expression.coerceTypes(indexVariable, increment);
			int incrementSign = 0;

			/*
			 * Depending on the data type, do the right kind of increment.  We do this
			 * in a type-specific way so we don't get into trouble with precision 
			 * errors unnecessarily when the index is really just an integer anyway.
			 */
			switch (indexVariable.getType()) {
			case Value.INTEGER:
				indexVariable.setInteger(indexVariable.getInteger()
						+ increment.getInteger());
				incrementSign = (increment.getInteger() < 0) ? -1 : 1;
				break;

			case Value.DOUBLE:
				indexVariable.setDouble(indexVariable.getDouble()
						+ increment.getDouble());
				incrementSign = (increment.getDouble() < 0.0) ? -1 : 1;
				break;

			default:
				return true;

			}

			/*
			 * Note that since indexVariable is the actual value reference, the
			 * compare operation will coerce it's type to match end if needed.
			 * This will happen on the first iteration through the loop, and
			 * will result in the index variable ending up as the same type of
			 * the END value if the END value was of greater precision. That is,
			 * an integer index with a double precision END value will become a
			 * double precision variable the first time the loop iterates.
			 */
			final int cmp = indexVariable.compare(end);

			if (incrementSign < 0)
				done = (cmp < 0);
			else
				done = (cmp > 0);

			break;

		default:
			break;
		}

		return done;
	}

	/**
	 * Method to test if a given loop block is a FOR-NEXT loop for the given
	 * variable. Tests to see if the loop block is a FOR-NEXT block, and then
	 * compares the index variable name. This is used when scanning a stack of
	 * loop blocks to see if the current one belongs to the NEXT variable just
	 * parsed, for example.
	 * 
	 * @param var
	 *            The name of the index variable being tested.
	 * @return true if this loop block corresponds to the variable name given
	 */
	public boolean equalsFor(final String var) {
		if ((loopType == LOOP_FOR || loopType == LOOP_FOREACH) & indexVariableName.equals(var))
			return true;
		return false;

	}
}
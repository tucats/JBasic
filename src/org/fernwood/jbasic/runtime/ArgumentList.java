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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.value.Value;

/**
 * Function argument list. This data structure holds an argument list used by
 * functions. An argument list is really just an ArrayList that can grow
 * dynamically, and a pointer to the encapsulating session object.
 * <p>
 * You can insert elements into an argument list by adding them to the end of
 * the list. You can also fetch an arbitrary element from the argument list. If
 * you request an argument that is not there, you get a null element back.
 * <p>
 * You can also extract an element by type, which causes a type coercion to
 * occur. This allows a function that knows it needs a double value to make sure
 * the argument is a double implicitly. You can also get the type of the
 * argument out of the list without fetching the value if you wish.
 * 
 * @author Tom Cole
 * @version version 1.0 Jun 21, 2004
 * 
 */
public class ArgumentList {

	/**
	 * The containing session in which the function call is made.  This data is
	 * used by some functions to access global state information, so it is passed
	 * to the function via the ArgumentList block.
	 */
	public JBasic session;

	/**
	 * The arrayValue of elements that make up the arguments for a function
	 * call.
	 */

	private ArrayList<Value> argumentItems;

	/**
	 * Constructor that builds an empty argument list.
	 * 
	 * @param jb
	 *            The JBasic object that contains the current session. This is
	 *            passed to functions that might need to access session state.
	 */
	public ArgumentList(final JBasic jb) {
		session = jb;
		argumentItems = new ArrayList<Value>();
	}

	/**
	 * Get the size of the argument list.
	 * 
	 * @return Integer count of items in the encapsulated argument vector.
	 */

	public int size() {
		if (argumentItems == null)
			return 0;
		return argumentItems.size();
	}

	/**
	 * Check to see if the argument list matches the requirements of a given
	 * function. This validation includes minimum and maximum argument counts,
	 * and optional type checking of arguments.
	 * <p>
	 * For example,
	 * <p>
	 * <code>validate( 1, 3, new int[] { Value.INTEGER, Value.STRING } )</code>
	 * <p>
	 * This requires between 1 and three arguments. The first two must be an
	 * <code>INTEGER</code> and a <code>STRING</code>. No type checking is
	 * done on the third item because it has no matching entry in the list.
	 * <code>Value.UNDEFINED</code> could have been used to represent this as
	 * well.
	 * 
	 * @param minimum
	 *            The minimum number of arguments that must have been passed in
	 *            the argument list.
	 * @param maximum
	 *            The maximum number of arguments that must have been passed in
	 *            the argument list. If -1 is given instead, it means there is
	 *            no maximum.
	 * @param typeMap
	 *            An integer arrayValue of argument types. If the map is
	 *            present, it requires that the arguments given must match the
	 *            types in the list (which are <code>Value.STRING</code>,
	 *            etc.) types. If an element is <code>DataType.UNDEFINED</code>
	 *            then that argument is not type-checked.
	 * @throws JBasicException if a parameter or math error occurs
	 */
	public void validate(final int minimum, final int maximum,
			final int[] typeMap) throws JBasicException {

		/*
		 * If we require a minimum number of arguments and there aren't that
		 * many given, then fail.
		 */

		if (minimum > size()) {
			throw new JBasicException(Status.INSFARGS);
		}

		/*
		 * If we are over the maximum allowed, fail.
		 */
		if ((maximum != -1) && (maximum < size())) {
			throw new JBasicException(Status.TOOMANYARGS);
		}

		/*
		 * If a type map is given, then let's validate each argument against it.
		 */

		if (typeMap != null) {
			int len = typeMap.length;
			if (len > size())
				len = size();

			for (int item = 0; item < len; item++) {
				if (typeMap[item] == Value.UNDEFINED)
					continue;
				final Value e = argumentItems.get(item);
				if (!e.isType(typeMap[item])) {
					throw new JBasicException(Status.ARGTYPE);
				}
			}
		}
		return;
	}

	/**
	 * Insert a new item into the vector, and update the count.
	 * 
	 * @param e
	 *            Any Value, which is added to the vector. The vector is
	 *            zero-based, so the first element will be at location 0.
	 */
	public void insert(final Value e) {
		if (argumentItems == null)
			argumentItems = new ArrayList<Value>();
		argumentItems.add(e);
	}

	/**
	 * Fetch a Value from the argument list by index. The Value returned is the
	 * same one passed in originally, with no type coercion.
	 * 
	 * @param j
	 *            The zero-based position of the argument to return. So
	 *            <code>element(0)</code> is the first argument,
	 *            <code>element(1)</code> is the second, and so on.
	 * @return A Value. If the index is outside the range of actual items in the
	 *         list, a null pointer is returned.
	 */
	public Value element(final int j) {
		if ((j < 0) | (j > size() - 1))
			return null;

		final Value e = argumentItems.get(j);
		return e;
	}

	/**
	 * Fetch a String from the argument list by index. The String returned is
	 * the result of type coercion of the Value originally put in the argument
	 * list.
	 * 
	 * @param j
	 *            The zero-based position of the argument to return. So
	 *            <code>stringElement(0)</code> is the first argument,
	 *            <code>stringElement(1)</code> is the second, and so on.
	 * @return A String value representing the argument. If the index is outside
	 *         the range of actual items in the list, a null pointer is
	 *         returned.
	 */
	public String stringElement(final int j) {
		if ((j < 0) | (j > size() - 1))
			return null;

		final Value e = argumentItems.get(j);
		return e.getString();
	}

	/**
	 * Fetch a floating point value from the argument list by index. The double
	 * returned is the result of type coercion of the Value originally put in
	 * the argument list.
	 * 
	 * @param j
	 *            The zero-based position of the argument to return. So
	 *            <code>doubleElement(0)</code> is the first argument,
	 *            <code>doubleElement(1)</code> is the second, and so on.
	 * @return A double value representing the argument. If the index is outside
	 *         the range of actual items in the list, then 0.0 is returned.
	 */
	public double doubleElement(final int j) {
		if ((j < 0) | (j > size() - 1))
			return 0.0;

		final Value e = argumentItems.get(j);
		return e.getDouble();
	}

	/**
	 * Fetch an integer from the argument list by index. The integer returned is
	 * the result of type coercion of the Value originally put in the argument
	 * list.
	 * 
	 * @param j
	 *            The zero-based position of the argument to return. So
	 *            <code>intElement(0)</code> is the first argument,
	 *            <code>intElement(1)</code> is the second, and so on.
	 * @return A long value representing the argument. If the index is outside
	 *         the range of actual items in the list, a 0 is returned.
	 */

	public int intElement(final int j) {
		if ((j < 0) | (j > size() - 1))
			return 0;

		final Value e = argumentItems.get(j);
		return e.getInteger();
	}

	/**
	 * Fetch a boolean value from the argument list by index. The boolean
	 * returned is the result of type coercion of the Value originally put in
	 * the argument list.
	 * 
	 * @param j
	 *            The zero-based position of the argument to return. So
	 *            <code>booleanElement(0)</code> is the first argument,
	 *            <code>booleanElement(1)</code> is the second, and so on.
	 * @return A boolean value representing the argument. If the index is
	 *         outside the range of actual items in the list, false is returned.
	 */

	public boolean booleanElement(final int j) {
		if ((j < 0) | (j > size() - 1))
			return false;

		final Value e = argumentItems.get(j);
		return e.getBoolean();
	}

	/**
	 * Fetch the type of a value from the argument list by index. The type
	 * returned is the type of the Value originally put in the argument list.
	 * 
	 * @param j
	 *            The zero-based position of the argument to return. So
	 *            <code>type(0)</code> is the first argument,
	 *            <code>type(1)</code> is the second, and so on.
	 * @return A integer representing the argument, from the set of Value
	 *         enumerated data types. If the index is outside the range of
	 *         actual items in the list, <code>Value.UNDEFINED</code> is
	 *         returned.
	 */

	public int type(final int j) {
		if ((j < 0) | (j > size() - 1))
			return Value.UNDEFINED;
		final Value e = argumentItems.get(j);
		return e.getType();
	}

}

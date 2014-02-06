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
package org.fernwood.jbasic.compiler;

import java.util.ArrayList;

/**
 * Class for managing a string pool, which keeps a single copy of string
 * constant objects that can be referenced by a unique integer ID number.
 * <p>
 * This is used when storing protected programs in a workspace. All the strings
 * in the program are stored in a string pool, so each unique string value gets
 * the same id number. This ID number is used to reference the string in the
 * encoded bytecode streams. During the loading of such a protected program, the
 * StringPool map is used to reconstruct string arguments to bytecodes.
 * 
 * @author cole
 * 
 */
public class StringPool {

	private ArrayList<String> pool;

	/**
	 * This is the constructor for a StringPool, which is used to manage pools
	 * of string constants. The constructor ensures that the underlying vector
	 * has been initialized.
	 */
	public StringPool() {
		pool = new ArrayList<String>(10);
	}

	/**
	 * Return a string value from the string pool.
	 * 
	 * @param stringID
	 *            The string id returned from a previous addString call.
	 * @return A String value that is the constant previously stored, or a null
	 *         pointer if the index is invalid.
	 */
	public String getString(final int stringID) {
		if ((stringID > pool.size()) | (stringID < 1))
			return null;
		return pool.get(stringID - 1);
	}

	/**
	 * Return the ID from a previously stored string in the pool.
	 * 
	 * @param string
	 *            The string to look up.
	 * @return The integer index value of the previously stored string, or zero
	 *         if the string is not in the pool.
	 */
	public int getStringID(final String string) {

		final int ixlen = pool.size();
		for (int ix = 0; ix < ixlen; ix++)
			if (string.equals(pool.get(ix)))
				return ix + 1;
		return 0;
	}

	/**
	 * Add a string to the pool.
	 * 
	 * @param stringToAdd
	 *            The string constant to add to the pool.
	 * @return The string ID number. If the string was previously already in the
	 *         pool, then the pre-existing ID is returned, else a new ID is
	 *         returned if the string is added to the pool.
	 */
	public int addString(final String stringToAdd) {

		final int ix = getStringID(stringToAdd);
		if (ix > 0)
			return ix;

		pool.add(stringToAdd);
		return pool.size();
	}

	/**
	 * Add a string to a pool, with a specific ID number to be used to identify
	 * it.
	 * 
	 * @param stringToAdd
	 *            The string to add.
	 * @param stringID
	 *            The ID number to bind to the string.
	 */
	public void addString(final String stringToAdd, final int stringID) {

		/*
		 * Make sure there are enough slots in the vector to allow
		 * us to set this id element.  This accounts for elements
		 * that are added out-of-order.
		 */
		while( stringID >= pool.size())
			pool.add(null);
		
		/*
		 * Now set the given ID in the array.  ID's are 1-based.
		 */
		pool.set(stringID - 1, stringToAdd);
	}

}

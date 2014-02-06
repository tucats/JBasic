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
 * Created on May 9, 2008 by tom
 *
 */
package org.fernwood.jbasic.runtime;

import org.fernwood.jbasic.value.Value;

/**
 * Manage a basic Quicksort of an array of values.  The array must have been
 * created by the caller, and is <b>not</b> the same as a value array.
 */
public class Quicksort {

	/**
	 * Do a generic quicksort on the values in the array list 'a'
	 * @param theArray a Java array containing the values to be sorted.
	 */
	public static void quicksort(Value[] theArray) {
		shuffle(theArray);
		quicksort(theArray, 0, theArray.length - 1);
	}

	/**
	 * Sort a subset of the array list 'a'
	 * @param theArray a Java array containing the values to be sorted
	 * @param left first element in the array to process
	 * @param right last element in the array to process
	 */
	public static void quicksort(Value[] theArray, int left, int right) {
		if (right <= left)
			return;

		int pivotPoint = partition(theArray, left, right);
		quicksort(theArray, left, pivotPoint - 1);
		quicksort(theArray, pivotPoint + 1, right);
	}

	/**
	 * Search for a good place to break the array segment up by locating
	 * a new partition point.  When exchanging the values can be done to
	 * maintain sort order, do so.  When we get to a new segment of the
	 * array, report a partition point.
	 * 
	 * @param theArray a Java array containing the values to be sorted
	 * @param left the first element to scan
	 * @param right the last element to scan
	 * @return index into array that represents next partition boundary
	 */
	private static int partition(Value[] theArray, int left, int right) {
		int i = left - 1;
		int j = right;
		while (true) {

			/*
			 * Scan to see if everything to the left is smaller than us.
			 */
			while (isLessThan(theArray[++i], theArray[right]))
				;

			/*
			 * Scan to see if everything to the right is greater than us.
			 */
			while (isLessThan(theArray[right], theArray[--j]))
				if (j == left)
					break;

			if (i >= j)
				break;
			swapArrayElements(theArray, i, j);
		}
		swapArrayElements(theArray, i, right);
		return i;
	}

	/**
	 * Compare two values, and report if x < y. Uses the generic Value
	 * compare operation which returns -1, 0, or 1 depending on the
	 * relationship of the values.
	 * @param x first value to compare
	 * @param y second value to compare
	 * @return true if x is less than y
	 */
	private static boolean isLessThan(Value x, Value y) {
		try {
		return (x.compare(y) < 0);
		} catch (JBasicException e ) {
			return false;
		}
	}

	/**
	 * Exchange two elements of the Value array.  When done, a[i] will
	 * contain the value previously at a[j], and vice versa.
	 * @param theArray a Java array containing the values to be swapped
	 * @param i first position to be swapped.
	 * @param j second position to be swapped.
	 */
	private static void swapArrayElements(Value[] theArray, int i, int j) {
		Value swap = theArray[i];
		theArray[i] = theArray[j];
		theArray[j] = swap;
	}

	/**
	 * Scramble the order of the elements in the array by striding through
	 * it and swapping each element with one between that position and the
	 * end of the array.  A more random distribution results in a faster
	 * Quicksort.
	 * @param theArray
	 */
	private static void shuffle(Value[] theArray) {
		int count = theArray.length;
		for (int i = 0; i < count; i++) {
			int randomCell = i + (int) (Math.random() * (count - i));
			swapArrayElements(theArray, i, randomCell);
		}
	}
}

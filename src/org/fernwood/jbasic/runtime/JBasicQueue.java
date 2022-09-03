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

import java.util.Queue;

/**
 * This class represents the basic mechanisms of the in-memory file type of 
 * QUEUE, used to created FIFO message queues between JBasic threads.  It is
 * a wrapper around the Queue data type which provides synchronized queues.
 * <p>
 * This implementation knows that only strings are pushed and pulled from
 * the queue, and also tracks use counts for concurrent users of the queue.
 * 
 * @author cole
 * 
 */
public class JBasicQueue {

	/**
	 * The name of the queue, which allows it to be used by multiple
	 * threads all accessing the QUEUE by name.
	 */
	String name;

	/**
	 * The synchronized data structure that manages the actual FIFO
	 * queue.
	 */
	Queue<String> queue;

	/**
	 * The number of threads (really opens) that have an interest in
	 * this queue at any one time.
	 */
	int useCount;

	/**
	 * Instantiate an instance of the queue and give it a name.
	 * @param theName the name of the queue, from the OPEN QUEUE statement.
	 */
	JBasicQueue(final String theName) {
		name = theName;
		useCount = 0;
	}
	
	/**
	 * Set the number of concurrent OPEN operations have are
	 * made on this queue.  The QUEUE persists as long as this count
	 * is greater than zero.  The parameter is a delta that is added
	 * to the active queue count.
	 * @param delta The number of active users who are added or removed
	 * form teh use count (positive values reflect OPEN operations, and
	 * negative values reflect CLOSE operations).
	 * @return The udpated count of active users of the queue.
	 */

	int usage(final int delta) {
		useCount = useCount + delta;
		return useCount;
	}

	/**
	 * Write a string to the queue.
	 * @param o The string to store in the FIFO queue.
	 */
	 void put(String o) {
		queue.add(o);
	}
	 
	 /**
	  * Get a string from the queue. The string is removed from the queue
	  * by this operation. 
	  * @return The string value at the front of the queue.  If the queue 
	  * is currently empty, then return a null value.
	  */

	String get() {
		return queue.remove();
	}

	/**
	 * Find out how many threads have an active interest in this queue
	 * 
	 * @return the count of threads that have this queue open. This number will
	 *         always be greater than zero, because if it went to zero then the
	 *         queue would be deleted.
	 */
	public int useCount() {
		return useCount;
	}

	/**
	 * Format the object as a readable string.  This is used in the SHOW QUEUES
	 * command, as well as the Eclipse debugger. The queue name, empty state, 
	 * and active reference count are reported.  Thread safety prevents
	 * reporting on the current size of the queue.
	 * @return string description of the queue object.  
	 */
	public String toString() {
		final boolean empty = queue.isEmpty();
		final String s = "{QUEUE: \"" + name + "\", EMPTY: " + empty
				+ ",  REFCOUNT: " + useCount + "}";
		return s;
	}
}

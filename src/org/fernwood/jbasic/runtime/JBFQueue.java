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
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * Subclass of JBasicFile. This handles FIFI QUEUE files only. Supported methods
 * are open(), read(), readValue(), and close();
 * <p>
 * The operations that can be performed on an QUEUE file are OPEN, CLOSE, PRINT,
 * and LINE INPUT. The input operation supported by this program reads a single
 * line at a time from the file, and passes this data to the runtime operations
 * like _INPUT which parse the buffer to return values to variables.
 * 
 * @author cole
 * 
 */
public class JBFQueue extends JBasicFile {

	/**
	 * The FIFO data structure that manages the queue and serializes access to it.
	 */
	JBasicQueue queue;

	/**
	 * Create a new JBasicFile for a QUEUE data type.
	 * @param jb the JBasic session that hosts the open file.
	 */
	public JBFQueue(final JBasic jb) {
		super(jb);
		jbenv = jb;
		mode = JBasicFile.MODE_QUEUE;
	}

	/**
	 * Open the input file, using a provided file name. This operation must be
	 * performed before the file can be used for any I/O operation.
	 * 
	 * @param fn
	 *            The external physical file name, stored as a string in a
	 *            Value. Can also be "%TERMINAL%" (or whatever JBasic.CONSOLE_NAME
	 *            is defined as) which is a reserved name meaning
	 *            the Java system console.
	 * @throws JBasicException   if an I/O error occurs or the data structure
	 * was a duplicate name
	 */
	public void open(final Value fn, final SymbolTable symbols) throws JBasicException {

		fname = fn.getString().toUpperCase();

		if( fname.equalsIgnoreCase(JBasic.CONSOLE_NAME)) {
			throw new JBasicException(Status.FILECONSOLE);
		}

		/*
		 * See if this queue already exists, and we can just attach to it. If it
		 * doesn't exist, set the flag that says to create it in a thread-safe
		 * way.
		 */

		queue = findQueue(fname, true);

		register();
		lastStatus = new Status(Status.SUCCESS);
	}

	/**
	 * Read a line from the front of the input queue, and remove the
	 * item from the queue.
	 * 
	 * @return A string containing the last string read, or a null pointer if
	 * the queue is empty.
	 */
	public String read() {

		String buffer = queue.get();

		lastStatus = new Status(buffer == null ? Status.EOF : Status.SUCCESS);
		return buffer;
	}

	/**
	 * Write a string to the FIFO queue.
	 * 
	 * @param s
	 *            The string to write to the output file.
	 */

	public synchronized void print(final String s) {

		queue.put(s);
		lastStatus = new Status(Status.SUCCESS);
	}

	/**
	 * Write a string followed by a newline to the output file. Because a queue
	 * has no carriage control, a print() and a println() are identical.
	 * 
	 * @param s
	 *            The string to print to the output buffer.
	 */
	public void println(final String s) {
		print(s);
	}

	/**
	 * Test to see if this file is at end-of-file. If there is an active
	 * read-ahead buffer, then the file is not at EOF. (A read-ahead buffer is
	 * caused by a previous call to EOF or an INPUT statement that is reading
	 * multiple variables). If there is no read-ahead buffer, then the method
	 * attempts to read a line into the read-ahead buffer. IF this fails, then
	 * we are at end-of-file. If it succeeds, it marks the read-ahead buffer as
	 * valid and returns true.
	 * 
	 * @return Returns true if there is no more data to be read from the file.
	 *         Returns false if there is more data to be read.
	 */
	public boolean eof() {
		return queue.queue.isEmpty();
	}

	public void close() {

		releaseQueue();
		lastStatus = new Status();
		super.close();
	}

	/**
	 * Create a new QUEUE and return it.  This is guaranteed to be serialized so
	 * multiple threads attempting to create the same queue name will not
	 * collide.
	 * 
	 * @param name
	 *            the name of the queue, which <em>must</em> be uppercase.
	 * @return the newly created Queue
	 */
	private synchronized JBasicQueue newInstance(final String name) {
		final JBasicQueue q = new JBasicQueue(name.toUpperCase());
		return q;
	}

	/**
	 * Locate a queue by name. The queue may be optionally created if needed.
	 * 
	 * @param queueName
	 *            The name of the queue to locate. This <em>must</em> be in
	 *            uppercase.
	 *            <p>
	 *            Calling this routine successfully marks the queue as having
	 *            an (additional) user. If you successfully call findQueue, 
	 *            you <b>must</b>
	 *            call <code>releaseQueue</code> on the same queue to avoid
	 *            causing resources to be "leaked."
	 * @param createIf
	 *            if true, the queue will be created if it does not exist. If
	 *            false, then if the requested queue does not exist then a null
	 *            is returned.
	 * @return the queue that was found, or a new queue with the given name.
	 */
	private synchronized JBasicQueue findQueue(final String queueName,
			final boolean createIf) {
		JBasicQueue q = JBasic.queueList.get(queueName);
		if (createIf & (q == null))
			q = newInstance(queueName);
		
		/*
		 * If we were able to locate/create a queue, then increase the
		 * usage count.  IF the usage count becomes 1, then it implies we
		 * are the only holder of the queue and it was created just now.
		 * In this case, register the queue on the global QUEUE list.
		 */
		if( q != null ) {
			final int u = q.usage(+1);
			if (u == 1)
				JBasic.queueList.put(queueName, q);
		}
		return q;
	}

	/**
	 * Release a queue. If the number of threads interested in the queue becomes
	 * zero, the queue is also deleted.
	 * 
	 * @param queueName
	 *            the name of the queue to release. This name <em>must</em> be
	 *            in uppercase.
	 */
	synchronized void releaseQueue() {
		final int c = queue.usage(-1);
		if (c == 0) {
			JBasic.queueList.remove(queue.name);
			queue = null;
		}
	}
}

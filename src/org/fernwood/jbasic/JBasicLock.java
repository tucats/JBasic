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
 * Created on Mar 20, 2009 by tom
 *
 */
package org.fernwood.jbasic;

import java.util.concurrent.locks.ReentrantLock;

/**
 * This is a subclass a ReentrantLock object, that adds the ability to
 * bind the the owner of the lock (a JBasic session thread) to the lock.
 * @author tom
 * @version version 1.0 Mar 20, 2009
 *
 */
public class JBasicLock extends ReentrantLock {
	
	/**
	 * version UUID for serialization.
	 */
	private static final long serialVersionUID = -1463041315442744632L;

	/**
	 * The Java thread id of the thread that created this lock.  Only the owning
	 * thread can delete a lock.
	 */
	String	owner;
	
	
	/**
	 * This flag indicates that a lock has been scheduled for deletion, but 
	 * can't yet be deleted because there is someone holding the lock.  No
	 * new holders are allowed to connect to the lock, but we let the current
	 * holder release it before it is destroyed.
	 * 
	 * The initial state is "True" indicating this lock is un-owned.
	 */
	public boolean zombie = true;
	

	/**
	 * Disown the lock, which means the lock has no owner.
	 */
	public void disown() {
		zombie = true;
		owner = LockManager.NO_OWNER;
	}
	

	/**
	 * If the lock isn't owned by anyone (it is in a "zombie" state), then
	 * claim it for my session. This is used when a thread attempts to
	 * manipulate a lock that was owned by a thread that has ended, and 
	 * the ownership is transferred to the next thread that touches the
	 * lock.
	 * 
	 * @param mySession the JBasic session of the thread that will become
	 * the new owner if the lock is in zombie state.  If not a zombie lock,
	 * then no change in ownership occurs.
	 * @return true if the lock is now owned by the current thread.
	 */
	public boolean claimOwnership(JBasic mySession) {
		if( zombie ) {
			zombie = false;
			owner = mySession.getString("SYS$INSTANCE_NAME");
			return true;
		}
		return owner.equals(mySession.getString("SYS$INSTANCE_NAME"));
	}
}
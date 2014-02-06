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
 * Created on Aug 21, 2008 by tom
 *
 */
package org.fernwood.jbasic;

import java.util.Iterator;
import java.util.TreeMap;

import org.fernwood.jbasic.value.Value;

/**
 * This class handles global locks for JBasic, coordinated between any and all threads.
 * These locks are "named" so they can be used within the JBasic language for intra-thread
 * synchronization.
 * 
 * @author tom
 * @version version 1.0 Aug 21, 2008
 *
 */
public class LockManager {


	static final String NO_OWNER = "<none>";
	
	/**
	 * This maintains a list of the LOCK objects that are shared among threads
	 * in the current session's execution tree.
	 */
	private static TreeMap<String, JBasicLock> lockTable;

	/**
	 * Given the name of a shared lock, attempt to acquire ownership of the
	 * lock. If the lock is currently held by someone else, then wait until the
	 * lock is available.
	 * @param session The session taking out the lock, and which will
	 * claim ownership of the lock if it is currently un-owned.
	 * 
	 * @param lockName
	 *            The name of the lock, shared among all threads in the process.
	 * @return A Status() value indicating that the lock has been successfully
	 *         acquired.
	 */
	public static Status lock(JBasic session, final String lockName) {

		if (lockTable == null)
			lockTable = new TreeMap<String,JBasicLock>();

		final String name = lockName.toUpperCase();
		JBasicLock lock = null;
		synchronized (lockTable) {
			lock = lockTable.get(name);
			if (lock == null) {
				lock = new JBasicLock();
				lock.claimOwnership(session);
				lockTable.put(name, lock);
			}
			
			/* If the lock's original owner died, re-assign it to the current thread */
			else
				lock.claimOwnership(session);
		}

		try {
			lock.lockInterruptibly();
		} catch (InterruptedException e) {
			return new Status(Status.INTERRUPT);
		}
		
		/*
		 * Try one last time to own the lock if no one does; since it may have
		 * only gone to zombie state by a thread termination which let us
		 * grab the lock.
		 */
		lock.claimOwnership(session);

		return new Status();
	}

	/**
	 * Given the name of a shared lock previously locked, release ownership of
	 * the lock. If the lock is currently being waited upon by by someone else,
	 * that thread may begin executing immediately. If you attempt to release a
	 * lock that does not yet exist, it is created but not owned by any thread.
	 * @param session The session that will own the lock if it is currently
	 * not owned by any session.
	 * 
	 * @param lockName
	 *            The name of the lock, shared among all threads in the process.
	 * @param allFlag if true, then the lock is unlocked as many times as needed
	 * to release all holds on the lock by this thread.  If false, then a single
	 * unlock operation is attempted and the lock must be held by the thread or an
	 * error occurs.
	 * @return A Status() value indicating that the lock has been successfully
	 *         released.
	 */

	public static Status release(JBasic session, final String lockName, boolean allFlag) {

		final String name = lockName.toUpperCase();
		if (lockTable == null) 
			lockTable = new TreeMap<String, JBasicLock>();
			
		JBasicLock lock = null;
		synchronized (lockTable) {
			lock = lockTable.get(name);
			if (lock == null) {
				lock = new JBasicLock();
				lock.claimOwnership(session);
				lockTable.put(name, lock);
			}
			else
				lock.claimOwnership(session);
			
		}
		
		try {
			if( allFlag ) 
				while( lock.isHeldByCurrentThread())
					lock.unlock();
			else
				lock.unlock();
		} catch( IllegalMonitorStateException e ) {
			return new Status(Status.INVLOCK, name);
		}
		
		
		return new Status();
	}

	/**
	 * Return a list of the named locks currently known to the Lock Manager.
	 * @return a Value containing an array of strings.
	 */
	public static Value list() {
		
		Value array = new Value(Value.ARRAY, null);
		
		if( lockTable != null ) {
			Iterator i = lockTable.keySet().iterator();
			while( i.hasNext()) {
				String name = (String) i.next();
				JBasicLock lock = lockTable.get(name);
				
				Value entry = new Value(Value.RECORD, null);
				entry.setElement(new Value(name),"NAME");
				entry.setElement(new Value(lock.isLocked()), "ISLOCKED");
				
				entry.setElement(new Value(lock.getHoldCount()), "HOLDCOUNT");
				entry.setElement(new Value(lock.getQueueLength()), "WAITCOUNT");
				entry.setElement(new Value(lock.isHeldByCurrentThread()), "ISMINE");
				entry.setElement(new Value(lock.zombie), "ISZOMBIE");
				entry.setElement(new Value(lock.owner), "OWNER");
				
				array.addElement(entry);
			}
		}
		return array;
	}
	
	/**
	 * Release locks held by this thread, and delete locks owned by this thread.
	 * @param session The session that will be used to determine if the lock
	 * was owned by the session and should be put into "unowned" state.
	 * @return a Value containing an array of strings.
	 */
	public static int releaseAll(JBasic session) {
		int count = 0;

		if( lockTable != null ) {
			synchronized (lockTable) {
				Iterator i = lockTable.keySet().iterator();
				while( i.hasNext()) {

					String lockName = (String) i.next();
					JBasicLock lock = lockTable.get(lockName);

					while( lock.isHeldByCurrentThread()) {
						lock.unlock();
						count++;
					}

					/*
					 * IF we are also the owner of this lock
					 */
					if( lock.owner.equals(session.getString("SYS$INSTANCE_NAME"))) {
						lock.disown();
					}
				}

			}
		}
		return count;
	}



	/**
	 * Determine if a given lock is held by the current thread.
	 * @param session The session that will own the lock if it is
	 * currently unowned.
	 * @param lockName the name of the lock to test
	 * @return true if the current thread could successfully release the lock
	 * (regardless if it is currently locked or not).
	 */
	public static boolean isMine(JBasic session, String lockName) {
		final String name = lockName.toUpperCase();
		if (lockTable == null) 
			lockTable = new TreeMap<String, JBasicLock>();
			
		JBasicLock lock = null;
		synchronized (lockTable) {
			lock = lockTable.get(name);
			if (lock == null) {
				lock = new JBasicLock();
				lock.claimOwnership(session);
				lockTable.put(name, lock);
			}
		}
		return lock.isHeldByCurrentThread();
		
	}

	/**
	 * Release (if possible) and delete a lock from the lock table.  Supports
	 * the CLEAR LOCK command.
	 * @param session The session that will own the lock if it is
	 * currently unowned.
	 * @param lockName The name of the lock to clear
	 * @return Status indicating if it went okay.  INVLOCK is returned if the
	 * lock is not found or not owned by the current thread.
	 */
	public static Status clear(JBasic session, String lockName) {
		
		JBasicLock lock = null;
		if( lockTable == null )
			return new Status(Status.INVLOCK, lockName );
		
		synchronized (lockTable) {
			lock = lockTable.get(lockName);
			if (lock == null) {
				return new Status(Status.INVLOCK, lockName );
			}
			try {
				
				/*
				 * If the original owner died, assign it to us.
				 * Otherwise, make sure we are the owner.
				 */
				
				lock.claimOwnership(session);
				
				if( !lock.owner.equals(session.getString("SYS$INSTANCE_NAME")))
					return new Status(Status.INVLOCK, lockName);
				
				while( lock.isHeldByCurrentThread())
					lock.unlock();
				if( lock.hasQueuedThreads()) {
					lock.disown();
				}
			} catch( IllegalMonitorStateException e ) {
				return new Status(Status.INVLOCK, lockName );
			}
			if( !lock.zombie)
				lockTable.remove(lockName);
		}

		return new Status();
	}

	/**
	 * Create a new lock and make it be owned by the current session.  The
	 * lock isn't locked.
	 * @param session The current session, which will own the lock
	 * @param lockName The name of the lock.
	 * @return true if the lock was created, or if it already existed
	 * but the session now owns it.  False if the lock already exists
	 * and is owned by someone else.
	 */
	public static boolean create(JBasic session, String lockName) {
		if (lockTable == null)
			lockTable = new TreeMap<String,JBasicLock>();

		final String name = lockName.toUpperCase();
		JBasicLock lock = null;
		synchronized (lockTable) {
			lock = lockTable.get(name);
			if (lock == null) {
				lock = new JBasicLock();
				lock.claimOwnership(session);
				lockTable.put(name, lock);
				return true;
			}
			
			/* If the lock's original owner died, re-assign it to the current thread */
			
			return lock.claimOwnership(session);
		}
		
	}
}

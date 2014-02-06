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
 * Created on Nov 1, 2007 by tom
 *
 */
package org.fernwood.jbasic;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.fernwood.jbasic.runtime.SimpleCipher;
import org.fernwood.jbasic.statements.SortStatement;
import org.fernwood.jbasic.value.Value;

/**
 * The data known about a user in MULTIUSER mode. This information is
 * attached to remote sessions, and is also the fundamental object 
 * managed by the UserManager.
 * 
 * @author tom
 * @version version 1.0 Nov 1, 2007
 * 
 */
public class User {

	/**
	 * The username of this user.  This must be normalized to upper 
	 * case in all calls.  The name cannot contain blanks or control 
	 * characters.
	 */
	private String name;

	/**
	 * This is the hashed password string value.  This value is never
	 * stored in plain text except for the brief time when it is 
	 * entered by the user and then hashed. The password itself can 
	 * be any case-sensitive string of letters, numbers, or punctuation.
	 * The password cannot contain blanks or control characters.
	 */
	private String password;

	/**
	 * This is the name of the user's workspace file.  Currently this
	 * is stored in the user's home directory.
	 */
	private String workspace;

	/**
	 * This is the user's home directory.  This is not the same as a
	 * Unix home directory; it is instead the directory that 
	 * constrains all file system access for the remote user.  To the
	 * remote user, this directory is the root level of the visible 
	 * file system; this prevents the user from being able to see other
	 * directories or damage other parts of the file system.
	 */
	private String home;

	/**
	 * This is a descriptive string for the user, and is not currently
	 * used for anything.  It can hold the user's organizational or 
	 * institutional identity, for example, such as a class designation
	 * in a school.
	 */
	private String account;

	/**
	 * This is a descriptive string, and typically is used to hold the
	 * user's full (real) name.
	 */
	private String fullname;

	/**
	 * This is a map that contains String objects, each of which 
	 * represents a permission.  Permission names are always normalized
	 * to upper case. The permission is considered granted if there
	 * is a named object in the user's HashMap for that permission.
	 */
	private HashMap<String,String> permissions;
	
	/**
	 * This contains the "master" list of permissions assigned.  A
	 * permission cannot be added to the above permissions list unless
	 * it is in this list as well. The settings granted to a user
	 * from the user database define the maximum permissions settings
	 * the user can have.
	 */
	
	private HashMap<String,String> permissions_mask;

	/**
	 * This is a formatted string containing the date and time of 
	 * the last time the user logged into this server instance.  
	 * When the server is first started, this field contains the 
	 * word "NEVER" for all users. This value is not currently 
	 * persisted in the user database on disk.
	 */
	private String lastLogin;

	/**
	 * This counts the number of times this user has logged in to 
	 * the server.  It is incremented on each successful login
	 * operation, even if multiple logins overlap.
	 */
	private int loginCount;
	
	/**
	 * This is a flag that indicates that the ALL privilege has been 
	 * granted to the user.  This is a short-cut to prevent 
	 * unnecessary privilege checks for root users.
	 */
	private boolean all;

	/**
	 * Pointer to the encryption object that handles the password 
	 * hashing.
	 */
	private SimpleCipher crypto;

	/**
	 * When a copy is made of a User object, this points back to the
	 * orginal. This way, when we update the password (for example)
	 * we can always find the original object we were made from that
	 * will need to be updated.  This will be null if the object is
	 * not a copy.
	 */
	private User root;

	/**
	 * Create a user object with the minimum info known about a user.
	 * 
	 * @param newUserName the username to create
	 * @param cryptographyManager
	 *            an instance of the Cipher manager for encoding 
	 *            this password.
	 * @param newPassword the plain-text password to assign.
	 */
	public User(String newUserName, SimpleCipher cryptographyManager,
			String newPassword) {
		name = newUserName;

		if (cryptographyManager == null)
			password = null;
		else {
			crypto = cryptographyManager;
			password = newPassword;
			if( password != null )
				while( password.length() > 1 && password.charAt(0) == '*')
					password = password.substring(1);
		}
		permissions = new HashMap<String,String>();
		permissions_mask = null;
		
		all = false;
		lastLogin = "NEVER";
	}

	/**
	 * Set the default home directory path for this user. The name 
	 * is converted to a host-specific canonical form so it can be 
	 * used by the routines that convert user paths to system paths 
	 * and back again.
	 * 
	 * @param path
	 *            the path specification where all user file and 
	 *            directory IO is sand-boxed.
	 */
	public void setHome(String path) {
		
		try {
			File f = new File(JBasic.userManager.getSession().getNamespace().logicalToPhysical(path));
			home = f.getCanonicalPath();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Set or clear a permission state for the given user.
	 * 
	 * @param name
	 *            the name of the permission to grante/revoke
	 * @param state
	 *            a boolean indicating if the permission exists 
	 *            or not.
	 */
	public void setPermission(String name, boolean state) {

		String permName = name.toUpperCase();
		
		/*
		 * "ALL" is a special case, and results in either an empty
		 * permissions list or one that contains only "ALL".
		 */
		if (permName.equals("ALL")) {
			all = state;
			/* Remove the existing permissions list completely */
			permissions = new HashMap<String, String>();
			
			/* If ALL is set, then put all permissions in the list */
			if( all ) {
				for( String validName : Permissions.validNames )
					putPermission(validName);
			}
			return;
		}
		
		if (state) {
			putPermission(permName);
		} else {
			permissions.remove(permName);
			if (permissions.size() < Permissions.validNames.length)
				all = false;
		}
	}

	/**
	 * Store a permission in the user permission list. If the mask (the master
	 * list of permitted permissions) exists, verify that this permission is
	 * allowed to be on the list.  If there is no mask list, then any permission
	 * can be set.
	 * 
	 * @param validName the name of the permission to set
	 */
	public void putPermission(String validName) {
		
		if( validName.startsWith("!"))
			validName = validName.substring(1);
		else
			if( permissions_mask != null ) 
				if( permissions_mask.get(validName) == null)
					return;
		
		permissions.put(validName, validName);
	}

	/**
	 * Set the permissions mask to match the current list of permissions.  This
	 * is done once after a user is loaded from the database, for example.
	 */
	public void setPermissionMask() {
		permissions_mask = new HashMap<String,String>();
		permissions_mask.putAll(permissions);
	}
	/**
	 * Determine if the current user has a given permission or not.
	 * 
	 * @param name
	 *            The name of the permission.
	 * @return true if the permission is granted, else false.
	 */
	public boolean hasPermission(String name) {
		if (all)
			return true;
		return (permissions.get(name.toUpperCase()) != null);
	}

	/**
	 * Set the user's password value.  If the password string is 
	 * null or contains a "*" then the password is removed, and the 
	 * account has no required password.
	 * <p>
	 * If the password string starts with a "*" character then the
	 * password is already encoded using the secret and should be 
	 * stored as-is.  This is how data is loaded from the database 
	 * file, for example.
	 * <p>
	 * If the password is of non-zero length and does not start with 
	 * an "*" then it must be encrypted.  This is how data is read 
	 * from the user for SET PASSWORD for example.
	 * 
	 * @param newPassword the new password
	 * @param secret the encryption secret, typically established 
	 * by default.
	 */
	public void setPassword(String newPassword, String secret) {

		
		if (newPassword == null)
			password = null;
		else if (newPassword.length() == 0)
			password = null;
		else if (newPassword.equals("*"))
			password = null;
		else {
			if (crypto == null)
				crypto = new SimpleCipher(JBasic.userManager.getSession());
			if( newPassword.charAt(0) == '*')
				password = newPassword.substring(1);
			else
				password = crypto.encryptedString(newPassword, secret);
		}
		
		/*
		 * If we are a clone, do the set password on the original
		 * as well.
		 */
		if( this.root != null ) {
			this.root.setPassword( newPassword, secret);
		}

	}

	/**
	 * Does the current user have a password set?
	 * 
	 * @return true if there is a non-null password field
	 */
	public boolean hasPassword() {
		return (this.password != null);
	}

	/**
	 * Set the workspace for the user.
	 * 
	 * @param string the name of the workspace file
	 */
	public void setWorkspace(String string) {
		this.workspace = string;
	}

	/**
	 * Set the account for the user
	 * 
	 * @param string the name of the account
	 */
	public void setAccount(String string) {
		this.account = string;

	}

	/**
	 * Set the user's full name
	 * 
	 * @param string the full name
	 */
	public void setFullName(String string) {
		this.fullname = string;
	}

	/**
	 * Get this user's user name
	 * 
	 * @return the user name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the full name of this user
	 * 
	 * @return a string
	 */
	public String getFullName() {
		return fullname;
	}

	/**
	 * Get the account name for this user
	 * 
	 * @return a string
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * Get the name of the work space file for this user
	 * 
	 * @return a string
	 */
	public String getWorkspace() {
		return workspace;
	}

	/**
	 * Get the current home directory in the native file system 
	 * for this user.
	 * 
	 * @return a string
	 */
	public String getHome() {
		if( home == null )
			return "";
		return home;
	}

	/**
	 * Set the user's last login time to the current time.
	 */
	public synchronized void setLogin() {
		lastLogin = new Date().toString();
		loginCount++;
	}

	/**
	 * When is the last time this user logged in?
	 * 
	 * @return a string containing the text of the login time or 
	 * the word "NEVER"
	 */
	public String getLogin() {
		if (lastLogin == null)
			return "NEVER";
		return lastLogin;
	}

	/**
	 * Get the number of times this user has successfully logged 
	 * in to the server?
	 * @return a positive integer which may be zero.
	 */
	public int getLoginCount() {
		return loginCount;
	}
	
	/**
	 * Return the encrypted password string.
	 * 
	 * @return string containing the encrypted password of the user.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Return an iterator that can be used to step through the 
	 * names of the permissions that the current user holds.
	 * 
	 * @return an Iterator for the key set of permissions data.
	 */
	public Iterator getPermissionsIterator() {
		return permissions.keySet().iterator();
	}

	/**
	 * Make a new copy of this instance to bind to a session.
	 * @return a new instance of the User object.
	 */
	public User copy() {
		
		User newUser = new User(this.name, null, null);
		newUser.crypto = this.crypto;
		newUser.password = this.password;
		newUser.account = this.account;
		newUser.all = this.all;
		newUser.fullname = this.fullname;
		newUser.home = this.home;
		newUser.lastLogin = this.lastLogin;
		newUser.permissions = new HashMap<String,String>();
		newUser.permissions.putAll(this.permissions);
		newUser.workspace = this.workspace;
		newUser.loginCount = this.loginCount;

		newUser.root = this;
		
		return newUser;
	}

	/**
	 * Determine if the current user is actively logged in as to a 
	 * multi-user session.
	 * 
	 * @return boolean if the current user is logged in via the User Manager.
	 */
	public boolean isActive() {
		
		return JBasic.userManager.isActive(this.name);
	}

	/**
	 * Set the initial login count for this user object.
	 * @param integer the login count
	 */
	public void setLoginCount(int integer) {
		loginCount = integer;
	}

	/**
	 * Return an array of names of permissions associated with this user.
	 * @return array of names
	 */
	public Value getPermissions() {
		Value v  = new Value(Value.ARRAY, null);
		Iterator i = getPermissionsIterator();
		while( i.hasNext())
			v.addElement(new Value((String) i.next()));
		SortStatement.sortArray(v);
		return v;
	}
}

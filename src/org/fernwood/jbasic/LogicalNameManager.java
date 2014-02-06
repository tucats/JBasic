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
 * Created on Nov 26, 2007 by cole
 *
 */
package org.fernwood.jbasic;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import org.fernwood.jbasic.runtime.JBasicException;

/**
 * @author cole
 * @version version 1.0 Nov 26, 2007
 *
 */
public class LogicalNameManager {

	/**
	 * This list contains the well-known root points in the file system, such
	 * as USER:: which means the user's home directory, or TEMP:: which means
	 * the system temp space, as well as any shared libraries that are available.
	 */
	public TreeMap<String,String> list;

	/**
	 * Create an instance of the logical name manager.  This is normally a
	 * singleton object, owned by the root session.
	 */
	public LogicalNameManager() {
		list = new TreeMap<String,String>();
		
		/*
		 * By default, add the basic two logical names.  We're making some
		 * assumptions here...
		 */
		
		String fileSep = System.getProperty("file.separator");
		if( fileSep == null )
			fileSep = "/";
		String tempDir = System.getProperty("java.io.tmpdir");
		if( tempDir == null ) {
			if( fileSep.equals("/"))
				tempDir = "/tmp";
			else
				tempDir = "\temp";
		}
		
		addLogicalName("TMP", tempDir + fileSep + "jbasic" + fileSep + "temp" + fileSep + "public");
		addLogicalName("_TMP", tempDir + fileSep + "jbasic" + fileSep + "temp");
		addLogicalName("_WS", tempDir + fileSep + "jbasic" + fileSep + "workspaces");
		
	}

	/**
	 * Add a logical name to the list of logical name mappings.  If the name
	 * already exists, then the new path supersedes the previous name mapping.
	 * @param logicalName The logical name string, which is case insensitive.
	 * @param physicalPath The physical path mapped to the name.
	 */
	public void addLogicalName( String logicalName, String physicalPath ) {
		
		String path = physicalPath;
		String sep = System.getProperty("file.separator");
		String resolvedPath = sep;

		if( path.endsWith(sep))
			sep = "";
		File f = new File(path + sep);
		
		try {
			resolvedPath = f.getCanonicalPath();
		} catch (IOException e) {
			System.out.println("Failure to get path for " + path + sep );
		}
		
		synchronized(this) {
			list.put( logicalName.toUpperCase(), resolvedPath);
		}
	}
	
	/**
	 * Given a logical name, return the matching physical path.  If there is 
	 * no matching path, then return a null.
	 * @param logicalName the case-insensitive logical name.
	 * @return the physical path string, or null if the logical name does
	 * not exist.
	 */
	public String getPhysicalPath( String logicalName ) {
		String path = null;
		synchronized(this) {
			 path = list.get(logicalName.toUpperCase());
		}
		return path;
	}
	
	/**
	 * Given a path name, determine if the name contains a logical name.
	 * If it does not, then the path is returned as-is.  If there is a logical
	 * name in the path, then it is replaced in the file name by the mapped
	 * physical name.
	 * <p>
	 * Logical names take the form NAME:: where the name is case-insensitive
	 * but is followed by two colons, and then the rest of the path name. This
	 * is intended to reduce the possibility of a collision with real native
	 * file system path names.
	 * @param path a file system path that may have a logical name prefix.
	 * @return a file system path with any logical name resolved.
	 * @throws JBasicException the logical name is unknown
	 */
	public String logicalToPhysical( String path ) throws JBasicException {
		
		/*
		 * See if there are the tell-tale colons in the name.  If not,
		 * then we return the path name unchanged.
		 */
		
		int ix = path.indexOf("::");
		if( ix < 0 )
			return path;
		
		/*
		 * Get the logical name from the path, and see if there is a
		 * matching physical name. If there is no mapping (null returned
		 * from the list look-up) then return the path unchanged, which
		 * will probably result in an error when the path is used, so the
		 * caller will know they have a problem at that point...
		 */
		String logicalName = path.substring(0,ix);
		String physicalName = getPhysicalPath(logicalName);
		if( physicalName == null )
			throw new JBasicException(Status.UNKLN, logicalName);
		
		/*
		 * Get the rest of the path name after the logical name, as well
		 * as the native file system separator. If the physical name mapping
		 * or the remainder after the path already have a separator, then we
		 * don't need another one.
		 */
		String remainder = path.substring(ix+2);
		String sep = System.getProperty("file.separator");
		if( physicalName.endsWith(sep) || remainder.startsWith(sep))
			sep = "";
		
		/*
		 * Return the physical name mapping of the logical, a separator
		 * (if needed) and the remainder of the path name to the caller.
		 */
		return physicalName + sep + remainder;
	}

	/**
	 * Given a physical path name, determine if it should be mapped back to a
	 * logical name location.
	 * @param filePath a String containing teh full file path to convert.
	 * @return string containing the logical representation of the given
	 * physical string, by applying logical names as needed.
	 */
	public String physicalToLogical(String filePath) {

		Iterator i = list.keySet().iterator();
		while( i.hasNext()) {
			String key = (String) i.next();
			String path = list.get(key);
			if( filePath.startsWith(path)) {
				int pos = path.length();
				return key + "::" + filePath.substring(pos+1);
			}
		}
		return filePath;
	}
	
}

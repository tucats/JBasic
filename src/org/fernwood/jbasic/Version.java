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
package org.fernwood.jbasic;

import java.net.URL;
import java.util.Date;

/**
 * Object whose purpose is to look in the resource path to find out the date of
 * the JBasic class file. This is considered the "build date" of the program.
 * This is concatenated with the version number string statically set in the
 * JBasic module itself.
 * 
 * @author cole
 */
public class Version {

	/**
	 * Get the actual build date from the class file by using the class loader
	 * and resource mechanisms. Return the default version string with this
	 * concatenated. If an error occurs, report "unknown version" string.
	 * 
	 * @return String containing compound version string
	 */
	public String getVersion() {
		
		/*
		 * This is part of the string we add to the version number, and also
		 * the value we look for in the existing version to see if it's already
		 * been taken care of.
		 */
		final String datePrefix = " (Built ";
		
		/*
		 * If the version string already has the date on it, then we're done.
		 * This is because the initializer for JBasic has run at least once,
		 * and yet the version number is static.  We don't want to keep appending
		 * the date onto the buffer over and over again.
		 */
		
		if( JBasic.version.lastIndexOf(datePrefix) > 0 )
			return JBasic.version;
		
		/*
		 * No date there yet, so we'll build one.
		 */
		StringBuffer vstring = new StringBuffer(JBasic.version);
		vstring.append(datePrefix);

		/*
		 * The openConnection() method may throw an error if it's not able
		 * to access the JBasic class.  This is probably a really bad error,
		 * but for now let's just mask it with a dummy date string.
		 */
		try {
			final URL versionUrl = getClass().getClassLoader().getResource(
			"org/fernwood/jbasic/JBasic.class");
			// update the title of the window based on the version
			vstring.append(new Date(versionUrl.openConnection().getLastModified()));
		} catch (final Exception e) {
			vstring.append("on unknown date");
		}
		
		/*
		 * Wrap it up with a nice bow.
		 */
		vstring.append(")");
		return vstring.toString();
	}
}
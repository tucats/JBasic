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
 * Created on Aug 20, 2008 by tom
 *
 */
package org.fernwood.jbasic;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class manages programs for a JBasic session.  This includes the registry of available
 * programs, adding and removing programs, and iterating over them.
 * 
 * @author tom
 * @version version 1.0 Aug 20, 2008
 *
 */
public class ProgramManager {

	/**
	 * The list of stored programs. These are Program objects, stored by program
	 * name. A program must be loaded into this list by a <code>LOAD</code>
	 * statement before it can be RUN. They key for the map is the name of the
	 * program, normalized to upper-case.
	 */
	private Map<String, Program> storedPrograms;

	/**
	 * This is the current (last) program. This pointer is null when no program
	 * has been loaded or run. When a program is loaded or <code>RUN</code> by
	 * explicit name, it becomes the "last program" and subsequent
	 * <code>RUN</code> commands with no name will re-run this program.
	 */
	private Program currentProgram;

	/**
	 * Create an instance of the program manager
	 */
	
	ProgramManager() {
		storedPrograms = new TreeMap<String,Program>();
		
	}
	/**
	 * Given the name of a stored program, fetch the matching program object
	 * from the registry of stored programs for this session object.
	 * 
	 * @param name
	 *            A String containing the name. The name is not case-sensitive.
	 * @return A Program object representing the stored program. If the name was
	 *         not valid, null is returned.
	 */
	public Program find(final String name) {
		if (storedPrograms == null)
			storedPrograms = new TreeMap<String,Program>();
		return storedPrograms.get(name.toUpperCase());
	}

	/**
	 * Remove a stored program from the registry.
	 * 
	 * @param name
	 *            The name of the program to remove. Case is not significant.
	 * @return The Program object just removed from the registry.
	 */
	public Program remove(final String name) {
		if (storedPrograms == null)
			storedPrograms = new TreeMap<String,Program>();
		return storedPrograms.remove(name.toUpperCase());
	}

	/**
	 * Add a program object to the database of stored programs for this JBasic
	 * object. If the storedPrograms public TreeMap has not been initialized, it
	 * is created. The object is the stored away.
	 * 
	 * @param name
	 *            The name of the program object. This is always converted to an
	 *            uppercase name.
	 * @param p
	 *            The program object to register.
	 */
	public void add(final String name, final Program p) {
		if (storedPrograms == null)
			storedPrograms = new TreeMap<String,Program>();
		storedPrograms.put(name.toUpperCase(), p);
	}

	/**
	 * Get an iterator that lets the caller step over the list of stored
	 * programs in this JBasic object. This iterator is used for functions that
	 * wish to enumerate programs, such as the SHOW PROGRAMS command or the
	 * PROGRAMS() function.
	 * 
	 * @return The opaque iterator object for the stored program TreeMap
	 */
	public Iterator iterator() {
		return storedPrograms.values().iterator();
	}

	/**
	 * Return the number of programs stored in the current object.
	 * 
	 * @return The 1-based count of stored program objects.
	 */
	public int count() {
		return storedPrograms.size();
	}
	
	/**
	 * Return a flag indicating if there are unsaved programs in the stored
	 * program space. This is used to warn the user before a QUIT operation, for
	 * example.
	 * 
	 * @return true if there are one or more unsaved programs
	 */
	public boolean unsaved() {
		for (final Iterator i = storedPrograms.values().iterator(); i.hasNext();) {
			final Program m = (Program) i.next();
			if (m.isModified())
				return true;
		}
		return false;
	}
	/**
	 * Set the current program.
	 * @param newPgm the Program to establish as the current program.
	 */
	public void setCurrent(Program newPgm) {
		currentProgram = newPgm;
	}

	/**
	 * Return the current program object being run, if any
	 * @return the program object, or null if none is current.
	 */
	public Program getCurrent() {
		return currentProgram;
	}

}

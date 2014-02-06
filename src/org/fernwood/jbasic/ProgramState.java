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
 * Created on Aug 28, 2007 by cole
 *
 */
package org.fernwood.jbasic;

import java.util.ArrayList;

import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.LoopManager;
import org.fernwood.jbasic.runtime.RegisterArray;
import org.fernwood.jbasic.runtime.ScopeControlBlock;
import org.fernwood.jbasic.value.Value;

/**
 * This object can hold the transient execution state of a program object. This
 * is used when recursion is possible, to capture runtime program state before
 * allowing a recursive call, and to restore it when done.
 * <p>
 * It should be noted that this information is stored here from fields found in
 * both the Program and ByteCode classes.
 * 
 * @author cole
 * @version version 1.0 Aug 28, 2007
 * 
 */
public class ProgramState {

	ArrayList<ScopeControlBlock> gosubStack;

	int next;

	int dataElementPosition;

	boolean atEOD;

	int programCounter;

	RegisterArray registers;

	ArrayList<Value> datastack;

	boolean valid;

	private LoopManager loopStack;

	/**
	 * Create a program state object using data from an existing Program
	 * 
	 * @param p
	 *            the Program object whose state is to be saved in this object.
	 */
	public ProgramState(Program p) {

		/*
		 * First, remember if the current program is running or not.
		 */
		valid = p.isActive();
		
		/*
		 * If it is already running, then we must remember it's state.
		 */
		if (valid) {
			ByteCode bc = p.getExecutable();
			this.gosubStack = p.gosubStack;
			this.next = p.next;
			this.dataElementPosition = p.dataElementPosition;
			this.atEOD = p.fAtEOD;
			this.loopStack = p.loopManager;
			
			if (bc != null) {
				this.datastack = bc.dataStack;
				this.programCounter = bc.programCounter;
				this.registers = bc.registers;
			}
		}
	}

	/**
	 * Restore the program state of a given Program using the data stored in
	 * this object.
	 * 
	 * @param p
	 *            The Program object to restore.
	 */
	public void restoreState(Program p) {
		
		/*
		 * If the program was running at the time we were asked to save
		 * it's state, then put the state back.  If it was not running,
		 * we do nothing.
		 */
		if (valid) {
			p.gosubStack = this.gosubStack;
			p.next = this.next;
			p.dataElementPosition = this.dataElementPosition;
			p.fAtEOD = this.atEOD;
			p.loopManager = this.loopStack;
			
			ByteCode bc = p.getExecutable();
			if (bc != null) {
				bc.programCounter = this.programCounter;
				bc.registers = this.registers;
				bc.dataStack = this.datastack;
			}
		}
		else {
			p.gosubStack = null;
			p.next = 0;
			p.dataElementPosition = 0;
			p.fAtEOD = true;
			ByteCode bc = p.getExecutable();
			if (bc != null) {
				bc.programCounter = 0;
				bc.registers = null;
				bc.dataStack = null;
			}
			
		}
	}
}

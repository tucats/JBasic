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
 * Created on Mar 24, 2009 by tom
 *
 */
package org.fernwood.jbasic.runtime;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.value.Value;

/**
 * This is an extension of the ByteCode class, and is used to hold the short
 * segments of bytecode for each DATA element in a program.  This adds a
 * cached result value which is the result of running the program.  This 
 * is allowed since the DATA statement elements must be constant and therefore
 * have one and only one possible value.
 * 
 * @author tom
 * @version version 1.0 Mar 24, 2009
 *
 */
public class DataByteCode extends ByteCode {

	/**
	 * The cached result of the DATA item expressed
	 * by this byte code object.  When non-null, this
	 * value is returned as the DATA item instead of
	 * re-running the code stream.
	 */
	public Value cachedResult;
	
	/**
	 * Constructor for a DataByteCode; it adds initialization of
	 * the cached result set to null. 
	 * @param jb the parent JBasic session
	 */
	public DataByteCode(JBasic jb) {
		super(jb);
		cachedResult = null;
	}

}

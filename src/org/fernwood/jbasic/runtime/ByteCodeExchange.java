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
 * Created on May 28, 2008 by cole
 *
 */
package org.fernwood.jbasic.runtime;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.ArrayList;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Linkage;
import org.fernwood.jbasic.value.Value;

/**
 * ByteCodeExchange objects contain a representation of a ByteCode object
 * abstracted into a Value which contains a record with the bytecode
 * data, label map, etc.
 * @author cole
 * @version version 1.0 May 28, 2008
 *
 */
public class ByteCodeExchange {

	private Value result;
	private Value pool;
	private Value codes;

	/**
	 * Create a new ByteCodeExchange object
	 */
	public ByteCodeExchange() {
		result = new Value(Value.RECORD, null);
		pool = new Value(Value.ARRAY, null);
		codes = new Value(Value.ARRAY, null);
	}


	/**
	 * Given a bytecode stream, encode it as a value that could be stored,
	 * passed on a network, etc.
	 * @param code the bytestream to encode
	 * @return a value containing a representation of the bytecode.
	 */
	public Value encodeAsValue(ByteCode code) {

		/*
		 * First, scan over the byte code array looking for non-integer
		 * arguments we need to put in the pool.
		 */

		result.setElement(new Value(true), "ISBYTECODE");
		String name = null;
		if (code.statement != null)
			if (code.statement.program != null)
				name = code.statement.program.getName();
		if (name == null)
			name = "NEW_" + JBasic.getUniqueID();
		result.setElement(new Value(name), "NAME");

		for (int ix = 0; ix < code.size(); ix++) {
			Instruction i = code.getInstruction(ix);

			int dx = 0;
			int sx = 0;

			if (i.doubleValid)
				dx = addDouble(i.doubleOperand);
			if (i.stringValid & i.opCode != ByteCode._STMT)
				sx = addString(i.stringOperand);

			int op = i.opCode * 10;
			int mask = 0;
			if (i.integerValid)
				mask = mask + 1;
			if (i.doubleValid)
				mask = mask + 2;

			/* Special case, never include the _STMT string text */
			if ( i.opCode != ByteCode._STMT & i.stringValid)
				mask = mask + 4;

			codes.addElement(new Value(op + mask));
			if (i.integerValid)
				codes.addElement(new Value(i.integerOperand));
			if (i.doubleValid)
				codes.addElement(new Value(dx));
			/* Special case, never include the _STMT string text */
			if (i.opCode != ByteCode._STMT & i.stringValid)
				codes.addElement(new Value(sx));
		}

		result.setElement(pool, "POOL");
		result.setElement(codes, "CODE");

		/*
		 * Also must store away the label map, if there is one.
		 */

		Value linkmap = new Value(Value.RECORD, null);
		if( code.labelMap != null ) {
			Iterator mx = code.labelMap.keySet().iterator();
			while (mx.hasNext()) {
				String label = (String) mx.next();
				Linkage link = code.labelMap.get(label);
				linkmap.setElement(new Value(link.byteAddress), label);
			}
		}

		result.setElement(linkmap, "MAP");

		return result;
	}

	/**
	 * Given a value that contains a bytecode definition, convert
	 * it back to a usable bytecode.
	 * @param session the parent session to bind this bytecode to.
	 * @param v the Value containing the encoded stream
	 * @return the ByteCode object stored in a session previously
	 * created by the encodeAsValue() method.
	 */
	public ByteCode getByteCode(JBasic session, Value v) {

		ByteCode bc = new ByteCode(session);
		bc.fLinked = true;

		Value pool = v.getElement("POOL");
		if (pool == null)
			return null;

		Value code = v.getElement("CODE");
		if (code == null)
			return null;

		/*
		 * Scan over the instructions and pull out each value.
		 */

		for (int ix = 1; ix <= code.size(); ix++) {
			int item = code.getInteger(ix);
			int mask = item % 10;
			Instruction i = new Instruction(ByteCode._NOOP);
			i.opCode = item / 10;

			i.integerValid = (mask % 2) == 1;
			mask = mask / 2;

			i.doubleValid = (mask % 2) == 1;
			mask = mask / 2;

			i.stringValid = (mask % 2) == 1;
			mask = mask / 2;

			if (i.integerValid)
				i.integerOperand = code.getInteger(++ix);

			if (i.doubleValid) {
				int dx = code.getInteger(++ix);
				i.doubleOperand = pool.getDouble(dx);
			}

			if (i.stringValid) {
				int sx = code.getInteger(++ix);
				i.stringOperand = pool.getString(sx);
			}

			bc.add(i);
		}
		return bc;
	}

	/**
	 * Return the tree map that defines the linkages for this byte code.
	 * @param v
	 * @return
	 */
	TreeMap<String,Linkage> getMap(Value v) {

		TreeMap<String, Linkage>  map = new TreeMap<String,Linkage>();

		Value linkMap = v.getElement("MAP");
		if (linkMap == null)
			return null;

		/*
		 * Get the list of labels store in the link map value.
		 */
		ArrayList<String> keys = linkMap.recordFieldNames();

		/*
		 * Stepping over this list of labels, create a new link map
		 * (TreeMap class) object.  This involves the label name and
		 * a Linkage object stored in the map.
		 */

		for (int ix = 0; ix < keys.size(); ix++) {

			String labelName = keys.get(ix);
			int byteCodeAddr = linkMap.getElement(labelName).getInteger();

			Linkage link = new Linkage(labelName, byteCodeAddr);
			map.put(labelName, link);
		}
		return map;
	}

	private int addString(String s) {
		Value v = new Value(s);
		for (int ix = 1; ix <= pool.size(); ix++)
			if (pool.getElement(ix).match(v))
				return ix;
		return pool.addElement(v);
	}

	private int addDouble(double d) {
		Value v = new Value(d);
		for (int ix = 1; ix <= pool.size(); ix++)
			if (pool.getElement(ix).match(v))
				return ix;
		return pool.addElement(v);
	}

	/**
	 * @param session The current active JBasic session
	 * @param v the value containing the program description to create.
	 * @return Status indicating if the program was successfully
	 * created.
	 */
	public Status createProgram(JBasic session, Value v) {

		String programName = null;

		Value name = v.getElement("NAME");
		if (name == null)
			programName = "NEW_" + JBasic.getUniqueID();
		else
			programName = name.getString();

		try {
			ByteCode bc = getByteCode(session, v);
			bc.labelMap = getMap(v);

			Program p = new Program(session, programName);
			p.register();
			p.setByteCode(bc);
			p.link(true);
			p.clearModifiedState();			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Status();
	}
}

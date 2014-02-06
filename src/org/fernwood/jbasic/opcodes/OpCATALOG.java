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
package org.fernwood.jbasic.opcodes;

import java.util.Iterator;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * Test a value to verify it is of a given type.
 * @author cole
 * 
 */
public class OpCATALOG extends AbstractOpcode {

	/**
	 * Opcode argument: is the named object a valid catalog?
	 */
	public static final int IS_VALID = 0;
	
	/**
	 * Opcode argument: mark the record object as a valid catalog
	 */
	public static final int SET_VALID = 1;
	
	/**
	 * Opcode argument: set the file name of the catalog to the same name as the catalog itself.
	 */
	public static final int SET_NAME_SELF = 2;
	
	/**
	 * Opcode argument: set the filename to the argument on the stack.
	 */
	public static final int SET_NAME = 3;
	
	/**
	 * Opcode argument: throw an error if the catalog does not have a filename
	 */
	public static final int IS_NAMED = 4;
	
	/**
	 * Opcode argument: if the catalog does not have a name yet, use the string on the stack.
	 */
	public static final int SET_NAME_IF = 5;

	/**
	 * Opcode argument: clear the dirty bit for all tables in the catalog
	 */
	public static final int CLEAR_DIRTY_FLAG = 6;
	
	/**
	 * Opcode argument: get the dirty flag for the catalog, leave on stack.
	 */
	public static final int GET_DIRTY_FLAG = 7;

	/**
	 * Opcode argument: set the autosave flag for teh catalog
	 */
	public static final int SET_AUTO_FLAG = 8;
	
	/**
	 * This is the name of the boolean member written into a catalog record
	 * to indicate that it is a CATALOG.  This is used to validate that a
	 * valid catalog is being referenced in a SQL statement, and also
	 * determines how it is formatted for printing.
	 */
	public static final String CATALOG_FLAG = "__CATALOG_VALID";
	
	/**
	 * This is the name of the string member of a catalog record that
	 * contains the name of the file where the catalog is persisted.
	 */
	public static final String CATALOG_NAME = "__CATALOG_NAME";

	/**
	 * Name of the BOOLEAN member of the catalog record that indicates
	 * if the catalog is to automatically be saved on a QUIT.
	 */
	public static final String CATALOG_AUTO = "__CATALOG_AUTOSAVE";
	
	
	/**
	 *  <b><code>_ISCATALOG  <em>code</em>, [<em>"member"</em>]</code><br><br></b>
	 * Test the top of the stack to see if it is a RECORD that is designed
	 * to be a CATALOG.  If the member name is included, verify that the
	 * member also exists.  IF not, throw an error.<p><br>
	 * If the code is 1, then set the catalog attribute on the record on
	 * top of the stack.
	 * 
	 * <p><br>
	 * <b>Implicit Arguments:</b><br>
	 * <list>
	 * <li>&nbsp;&nbsp;<code>stack[tos]</code> - RECORD to test</l1>
	 * </list><br><br>
	 *
	 * @param env The instruction context.
	 * @throws JBasicException indicating a stack over- or under-flow.
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		if( !env.instruction.integerValid)
			throw new JBasicException(Status.INVOPARG, "missing integer");
		
		Value sourceValue = null;
		String catalog = null;
		
		if( !env.instruction.stringValid) {
			sourceValue = env.pop();
			catalog = "<no-name>";
		}
		else {
			catalog  = env.instruction.stringOperand;
			sourceValue = env.localSymbols.findReference(catalog, false);
		}

		if( sourceValue == null || sourceValue.getType() != Value.RECORD)
			throw new JBasicException(Status.INVCATALOG, catalog);
		
		int theType = env.instruction.integerOperand;
		
		/**
		 * If the record exists at a table other than root, then move it to the
		 * root table now, assuming we are setting the CATALOG attribute 
		 */
		if( theType > 0 ) {
			SymbolTable t = env.localSymbols.findTableContaining(catalog);
			if( !t.fRootTable) {
				t.delete(catalog);
				JBasic.rootTable.insertSynchronized(catalog, sourceValue);
			}
		}
		Iterator<String> i = null;
		
		switch( theType ) {
		
		case IS_VALID:
			
			if( sourceValue.getElement(CATALOG_FLAG) == null )
				throw new JBasicException(Status.INVCATALOG);
			return;	
				
		case SET_VALID:
			sourceValue.setElement( new Value(true), CATALOG_FLAG);
			return;
		
		case SET_NAME_SELF:
			sourceValue.setElement( new Value(catalog), CATALOG_NAME);
			return;
		
		case SET_NAME:
			sourceValue.setElement( env.pop(), CATALOG_NAME);
			return;
			
		case IS_NAMED:
			if( sourceValue.getElement(CATALOG_NAME) == null )
				throw new JBasicException(Status.EXPNAME);
			return;	
		
		case SET_NAME_IF:
			String newName = env.pop().getString();
			if( sourceValue.getElement(CATALOG_NAME) == null )
				sourceValue.setElement(new Value(newName), CATALOG_NAME);
			return;	

			/* Scan over the catalog and clear the dirty bit for all tables */
		case CLEAR_DIRTY_FLAG:
			if( sourceValue.getElement(CATALOG_FLAG) == null )
				throw new JBasicException(Status.INVCATALOG);
			
			i = sourceValue.getIterator();
			while( i.hasNext()) {
				Value v = sourceValue.getElement(i.next());
				if( v instanceof RecordStreamValue ) {
					RecordStreamValue r = (RecordStreamValue) v;
					r.dirty(false);
				}
			}
			return;
		
		case GET_DIRTY_FLAG:
			env.push(isDirty(sourceValue));
			return;
			
		case SET_AUTO_FLAG:
			sourceValue.setElement(new Value(env.pop().getBoolean()), CATALOG_AUTO);
			return;
			
		default:
			throw new JBasicException(Status.INVOPARG, theType);
		}
		
	}
	
	/**
	 * Mark the catalog object as dirty; that is, the catalog definition
	 * or the tables within have been modified since the last LOAD or SAVE
	 * operation.
	 * @param sourceValue the value containing the CATALOG object
	 * @return true if the catalog needs to be saved
	 * @throws JBasicException if there is an error accessing the
	 * CATALOG contents; i.e. a mal-formed catalog
	 */
	public static boolean isDirty( Value sourceValue ) throws JBasicException {
		boolean dirty = false;
		if( sourceValue.getElement(CATALOG_FLAG) == null )
			throw new JBasicException(Status.INVCATALOG);
		Iterator<String> i = sourceValue.getIterator();
		while( i.hasNext()) {
			String name = i.next();
			Value v = sourceValue.getElement(name);
			
			if( v != null && v instanceof RecordStreamValue ) {
				RecordStreamValue r = (RecordStreamValue) v;
				if( r.isDirty()) 
					dirty = true;
			}
		}
	
		return dirty;
	}
}

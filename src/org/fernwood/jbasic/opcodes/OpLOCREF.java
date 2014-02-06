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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpLOCREF extends AbstractOpcode {

	/**
	 * Load or Create a reference to an item on the stack. If the named symbol
	 * exists in the local table, then it is stored on the stack as-is. If the
	 * symbol exists above us on the stack, then a copy is created in the local
	 * symbol table. If it does not exist at all, a new INTEGER 0 is created in
	 * the local symbol table.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * Locate the value using the symbol name given.
		 */

		final String symbolName = env.instruction.stringOperand;
		Value value = null;
		env.codeStream.refPrimary(symbolName, false);
		
		/*
		 * By default, the reference is always created in the local table. 
		 * However, a scope definition may be found in the integer operand
		 * that might change the behavior.  Values mean:
		 * 
		 * -1  The global table
		 * -2  The root table
		 *  0  The current table (Default)
		 *  1  The parent table
		 *  n  nth table up parent chain.
		 */
		SymbolTable targetTable = env.localSymbols;
		int scope = env.instruction.integerOperand;
		
		if( scope < 0 ) {
			targetTable = env.session.globals();
			if( scope == -2 )
				while( targetTable.parentTable != null )
					targetTable = targetTable.parentTable;
		}
		else
		if( scope > 0 )
			for( int scopeCount = 0; scopeCount < scope; scopeCount++)
				if( targetTable.parentTable != null )
					targetTable = targetTable.parentTable;
			
		/*
		 * See if it exists locally. If so, we're done.
		 */
		value = targetTable.localReference(symbolName);
		
		if (value == null) {

			if( !env.codeStream.fDynamicSymbolCreation)
				throw new JBasicException(Status.UNKVAR, symbolName);
			/*
			 * See if it exists anywhere in the symbol table tree. If so,
			 * make a copy of it in the local table.
			 */

			try {
				value = targetTable.reference(symbolName);
				targetTable.insert(symbolName, value.copy());
				value = targetTable.reference(symbolName);
			} catch (JBasicException e) {
				value = null;
			}

			/*
			 * No such symbol anywhere, just manufacture one.
			 */
			if (value == null) {
				value = new Value(0);
				targetTable.insert(symbolName, value);
			}
		}

		if( value.fReadonly) 
			throw new JBasicException(Status.READONLY, symbolName);
		env.push(value);
	}

}

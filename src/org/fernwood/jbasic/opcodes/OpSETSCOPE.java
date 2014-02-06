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

/**
 * @author cole
 * 
 */
public class OpSETSCOPE extends AbstractOpcode {

	/**
	 * Modify the current running context's scope to remove inheritance.  This changes
	 * the current symbol table's parent to always be the global table, so no other
	 * tables in outer-most scope will be used to resolve symbols.  This operation is
	 * typically done as part of the prologue of a program (near the _ENTRY operation)
	 * and cannot be undone for the life of the symbol table.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

				
		/*
		 * Figure out which table we are to bind our parent to. Values mean:
		 * 
		 * -1  The global table
		 * -2  The root table
		 *  0  The current table (illegal operation)
		 *  1  The parent table (the default state of a table)
		 *  n  nth table up parent chain.
		 */
		SymbolTable targetTable = env.localSymbols.originalParentTable;
		int scope = env.instruction.integerOperand;
		if( scope < -2)
			throw new JBasicException(Status.INVOPARG, scope);
		
		/*
		 * Set the flag that tells us if non-standard scoping is in use. This is used
		 * in CALL operations to take the current scope out of the chain for the called
		 * routines.
		 */
		env.codeStream.fLocallyScoped = (scope != 1);
		
		if( scope == 0 )
			targetTable = null;
		else
		if( scope < 0 ) {
			targetTable = env.session.globals();
			if( scope == -2 )
				while( targetTable.parentTable != null )
					targetTable = targetTable.parentTable;
		}
		else
		if( scope > 0 )
			for( int scopeCount = 1; scopeCount < scope; scopeCount++)
				if( targetTable.parentTable != null )
					targetTable = targetTable.parentTable;

		env.localSymbols.parentTable = targetTable;
	}

}

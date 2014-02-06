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
package org.fernwood.jbasic.statements;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.MockSQLStatement;

/**
 * SQL Statement
 * 
 * @author tom
 * @version 1.2 December 2010
 */

class SqlStatement extends Statement {


	/**
	 * Compile the parts of the SQL statement that can be successfully compiled.
	 */
	
	public Status compile( final Tokenizer tokens ) {
		
		MockSQLStatement msql = new MockSQLStatement(this.session);
		byteCode = new ByteCode(session, this);
		
		status = msql.prepare(tokens);
		
		if( status.equals(Status.NOCOMPILE)) {
			return status;
		}
		if( status.failed())
			return status;

		byteCode.concat(msql.generatedCode);
		
		if( msql.getStatementType() == MockSQLStatement.STMT_SELECT)
			byteCode.add(ByteCode._OUTNL, 0 );
		
		return new Status();
	}

}

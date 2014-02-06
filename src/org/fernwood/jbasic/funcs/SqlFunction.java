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
package org.fernwood.jbasic.funcs;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.MockSQLStatement;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;


/**
 * <b>SQL()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Evaluate a SQL statement and return result as a Table</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>tbl = SQL( string )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Table</td></tr>
 * </table>
 * <p>
 * Executes the argument as a SQL statement and returns the result.  It is an error if there
 * is no result set.
 * @author cole
 *
 */

public class SqlFunction extends JBasicFunction {
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  There was an error in the type or count of
	 * function arguments, or there was an error in the SQL statement syntax
	 * given.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {
		arglist.validate(1, 1, new int[] {Value.STRING});

		MockSQLStatement msql = new MockSQLStatement(arglist.session);
		
		Status sts = msql.prepare(arglist.stringElement(0));
		if( sts.failed())
			throw new JBasicException(sts);
		if( msql.getStatementType() != MockSQLStatement.STMT_SELECT)
			throw new JBasicException(Status.FAULT, "unsupported SQL() statement type, " + msql.getStatementType());
		
		sts = msql.execute(symbols);
		if( sts.failed())
			throw new JBasicException(sts);
		
		int defaultResultSetSize = -1;
		
		Value result = msql.fetch(defaultResultSetSize);
		if( result == null )
			throw new JBasicException(Status.FAULT, "no SQL result returned");
		return result;
	}
		

}

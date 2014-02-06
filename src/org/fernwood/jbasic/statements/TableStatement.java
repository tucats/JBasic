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

import java.util.Vector;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.value.Value;

/**
 * TIME statement handler. Accepts a command, and runs it while timing the
 * execution latency. A temporary hidden symbol is created by the operation
 * that holds a record with details used to track execution statistics. This
 * is a uniquely-generated name that is used only for a specific compilation
 * of the TIME statement, allowing for nested TIME statements should that
 * be needed.
 * 
 * @author cole
 * @version 1.0 August 12, 2004
 * 
 */

class TableStatement extends Statement {

	
	/**
	 * Compile the TIME statement
	 */
	@SuppressWarnings("unchecked") 
	public Status compile(final Tokenizer tokens) {

		Vector<String> nameList = new Vector();
		
		byteCode = new ByteCode(session, this);
		
		if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
			return status = new Status(Status.EXPNAME);
		
		String tableName = tokens.nextToken();
		
		
		if( !tokens.assumeNextToken("AS"))
			return status = new Status(Status.EXPAS);
		
		while( true ) {
			
			if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
				return status = new Status(Status.EXPTYPE, tokens.nextToken());
			
			String type = tokens.nextToken();
			if( Value.nameToType(type) == Value.UNDEFINED)
				return status = new Status(Status.EXPTYPE, type);
	
			if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
				return status = new Status(Status.EXPNAME, tokens.nextToken());
			
			String name = tokens.nextToken() + "@" + type;
			nameList.add(name);

			if( tokens.endOfStatement())
				break;
			if( !tokens.assumeNextSpecial(","))
				return status = new Status(Status.SYNEXPTOK, ",");
			
		}
		
		int count = nameList.size();
		for( int idx = count; idx > 0; idx-- ) 
			byteCode.add(ByteCode._STRING, nameList.get(idx-1));
		byteCode.add(ByteCode._TABLE, nameList.size());
		byteCode.add(ByteCode._STOR, tableName);

		
		return new Status();
	}
}
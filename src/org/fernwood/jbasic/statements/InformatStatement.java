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
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * RETURN statement handler.
 * 
 * @author cole
 * 
 */

class InformatStatement extends Statement {

	/**
	 * Compile 'INFORMAT' statement. Processes a token stream, and compiles it
	 * into a byte-code stream associated with the statement object. The first
	 * token in the input stream has already been removed, since it was the
	 * "verb" that told us what kind of statement object to create.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the source to
	 *            compile.
	 * @return A Status value that indicates if the compilation was successful.
	 *         Compile errors are almost always syntax errors in the input
	 *         stream. When a compile error is returned, the byte-code stream is
	 *         invalid.
	 */

	public Status compile(final Tokenizer tokens) {

		byteCode = new ByteCode(session, this);
		
		/*
		 * Get the LValue that we're going to store this format in.
		 */
		LValue target = new LValue(session, false);
		status = target.compileLValue(byteCode, tokens);
		if( status.failed())
			return status;
		
		/*
		 * Eat optional separator tokens
		 */
		
		tokens.assumeNextToken(new String[] {"AS", "="});
		
		/*
		 * Loop over each format definition.
		 */
		int count = 0;
		while( true ) {
			
			/*
			 * If we're at the end of the string, all well and good.
			 * 
			 */
			if( tokens.endOfStatement())
				break;
			
			/*
			 * If there's not an identifier, then boo boo.
			 */
			if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
				break;
			
			/*
			 * Get the name of the identifier, which is the FORMAT name.
			 */
			String name = tokens.nextToken();
			byteCode.add(ByteCode._STRING, "NAME");
			byteCode.add(ByteCode._STRING, name );
			/*
			 * See if there is additional parameter info.
			 */
			
			int fieldCount = 1;
			if( tokens.assumeNextSpecial("(")) {
				name = name + "(";
				if( tokens.assumeNextSpecial("*")) {
					byteCode.add(ByteCode._STRING, "LEN");
					byteCode.add(ByteCode._INTEGER, -1);
					fieldCount++;
				}
				else
					if( tokens.testNextToken(Tokenizer.INTEGER)){
						byteCode.add(ByteCode._STRING, "LEN");
						byteCode.add(ByteCode._INTEGER, 
								Integer.parseInt(tokens.nextToken()));
						fieldCount += 1;
					}
					else
						if( tokens.testNextToken(Tokenizer.STRING)) {
							String string = tokens.nextToken();
							byteCode.add(ByteCode._STRING, "STRING");
							byteCode.add(ByteCode._STRING, string );
							byteCode.add(ByteCode._STRING, "LEN");
							byteCode.add(ByteCode._INTEGER, string.length());
							fieldCount += 2;
						}
						else
							return new Status(Status.INVFMT, "length");
				if( tokens.assumeNextSpecial(",")) {
					if( tokens.testNextToken(Tokenizer.INTEGER)) {
						byteCode.add(ByteCode._STRING, "SCALE");
						byteCode.add(ByteCode._INTEGER, 
								Integer.parseInt(tokens.nextToken()));
						fieldCount += 1;
					}
					else
						return new Status(Status.INVFMT, "scale");
				}
				if(!tokens.assumeNextSpecial(")"))
					return new Status(Status.PAREN);
			}
			
			/*
			 * Emit the format item as a record constant.
			 */
			count++;
			byteCode.add(ByteCode._RECORD, fieldCount);
			
			/*
			 * Eat a comma separator if there is one, and loop again.
			 */
			tokens.assumeNextSpecial(",");
		}
		
		byteCode.add(ByteCode._ARRAY, count );
		target.compileStore();
		
		return new Status();

	}
}
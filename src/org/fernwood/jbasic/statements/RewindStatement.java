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
import org.fernwood.jbasic.compiler.FileParse;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;

/**
 * The REWIND statement. This resets the next DATA element pointer back to the
 * start of the program. It can also be used with REWIND FILE to reset the next
 * byte in an open file back to the start of the file.
 * 
 * @author tom
 * @version version 1.0 Aug 16, 2006
 * 
 */
public class RewindStatement extends Statement {

	public Status compile(final Tokenizer tokens) {
		byteCode = new ByteCode(session, this);

		/*
		 * See if it is the case of REWIND FILE <fd>
		 */

		final FileParse f = new FileParse(tokens, true);
		if (f.success()) {
			f.generate(byteCode);
			byteCode.add(ByteCode._INTEGER, 0);
			byteCode.add(ByteCode._SEEK, 1);  /* 1 means ignore FIELD spec if any */
			return new Status();
		}

		/*
		 * Nope, a simple DATA rewind. This might be a line number constant or it
		 * might be a label constant, or it might be a REWIND with no value that
		 * just means rewind to the beginning.
		 */
				
		if( tokens.testNextToken(Tokenizer.INTEGER)) {
			int lineNumber = Integer.parseInt(tokens.nextToken());
			try {
				addLineNumberPosition(tokens.getPosition() - 1);
			}
			catch(JBasicException e ) {
				return e.getStatus();
			}
			byteCode.add(ByteCode._REW, lineNumber );
		}
		else
		if( tokens.testNextToken(Tokenizer.IDENTIFIER)) {
			byteCode.add(ByteCode._REW, tokens.nextToken());
		}
		else
			byteCode.add(ByteCode._REW);
		return new Status();

	}
}

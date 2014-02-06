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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.FileParse;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.opcodes.OpCLEAR;
import org.fernwood.jbasic.opcodes.OpSYS;
import org.fernwood.jbasic.opcodes.OpTHREAD;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * CLEAR verb, which has subverbs for clearing specific things.
 * 
 * <br>
 * <list>
 * <li><code>CLEAR PROGRAM name</code>- Clear a program from memory.
 * <li><code>CLEAR FUNCTION name</code>- Clear a user-written function from
 * memory.
 * <li><code>CLEAR VERB name</code>- Clear a user-written verb from memory.
 * <li><code>CLEAR TEST name</code>- Clear a unit test program from memory.
 * <li><code>CLEAR SYMBOL name</code>- Clear a symbol from the most-local table
 * <li><code>CLEAR FIELD name</code>- Clear a FIELD definition from memory.
 * <li><code>CLEAR THREADS</code> - Clear threads that have already completed.
 * <li><code>CLEAR MESSAGE name </code> - Clear a MESSAGE text mapping
 * <li><code>CLEAR MESSAGES</code> - Clear all MESSAGE mappings
 * </list> <br>
 * <p>
 * <br>
 * Note that in all the above cases, <code>"name"</code> can be expressed
 * either as an identifier containing the name, or with a
 * <code>USING(expression)</code> construct that is a string expression that
 * contains the name.
 * <p>
 * <br>
 * <p>
 * 
 * @author cole
 * 
 */

class ClearStatement extends Statement {

	/**
	 * Compile 'clear' statement.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 * @return a Status value indicating if the compile was successful.
	 */
	public Status compile(final Tokenizer tokens) {

		byteCode = new ByteCode(session, this);
		int type = -1;
		String name = null;

		/*
		 * See what kind of operation it is. The codes are:
		 * 
		 * 0 - symbol, respecting readonly flags 
		 * 1 - symbol, ignore readonly flags
		 * 2 - fully qualified program object
		 * 
		 * Additionally, a CLEAR THREADS command disposes of threads that
		 * are no longer running, but is implemented via the _THREAD code
		 * rather than the _CLEAR code.
		 */

		if( tokens.assumeNextToken("EVENTS")) {
			byteCode.add(ByteCode._SYS, OpSYS.SYS_CLEAR_EVENTS);
			return new Status();
		}
		
		if (tokens.assumeNextToken("THREADS")) {
			/* Implemented by a sub command of the _THREAD byte code */
			byteCode.add(ByteCode._THREAD, OpTHREAD.RELEASE_THREADS);
			return new Status();
		} 

		if( tokens.assumeNextToken("SCREEN")) {
			byteCode.add(ByteCode._SYS, OpSYS.SYS_CLEAR_SCREEN);
			return new Status();
		}
		
		/*
		 * CLEAR MESSAGES dumps the active message database.
		 */
		if( tokens.assumeNextToken("MESSAGES")) {
			byteCode.add(ByteCode._DEFMSG, 5);
			return new Status();
		}
		
		/*
		 * CLEAR OPTIMIZER zeroes out the optimizer stats
		 */
		if( tokens.assumeNextToken("OPTIMIZER")) {
			byteCode.add(ByteCode._SYS, OpSYS.SYS_CLEARSTAT_OPT);
			return new Status();
		}
		/*
		 * Figure out if there is a prefix type (PROGRAM, TEST, etc.)
		 * that tells us this is a clear of a program in memory.  Generate
		 * the correct name and type code 2 for a program object.
		 */
		else if (tokens.assumeNextToken("PROGRAM")) {
			type = OpCLEAR.CLEAR_PROGRAM; /* CLEAR <program object> */
			name = JBasic.PROGRAM + tokens.nextToken();
		} else if (tokens.assumeNextToken("FUNCTION")) {
			type = OpCLEAR.CLEAR_PROGRAM; /* CLEAR <program object> */
			name = JBasic.FUNCTION + tokens.nextToken();
		} else if (tokens.assumeNextToken("TEST")) {
			type = OpCLEAR.CLEAR_PROGRAM; /* CLEAR <program object> */
			name = JBasic.TEST + tokens.nextToken();
		} else if (tokens.assumeNextToken("VERB")) {
			type = OpCLEAR.CLEAR_PROGRAM; /* CLEAR <program object> */
			name = JBasic.VERB + tokens.nextToken();
		} else if (tokens.assumeNextToken("SYMBOL")) {
			type = OpCLEAR.CLEAR_SYMBOL; /* Clear symbol */
			name = tokens.nextToken();
		} else if (tokens.assumeNextToken("MESSAGE")) {
			type = OpCLEAR.CLEAR_MESSAGE; /* Clear message code */
			name = tokens.nextToken();
			if( tokens.assumeNextSpecial("*")) {
				name = name + "*";
			}
		} else if (tokens.assumeNextToken("FIELD")) {
			FileParse fp = new FileParse( tokens, true);
			status = fp.getStatus();
			if( status.failed())
				return status;
			fp.generate(byteCode);
			byteCode.add(ByteCode._FIELD, 1);
			return new Status();
		} else if (tokens.assumeNextToken("LOCK")) {
			name = tokens.nextToken();
			type = OpCLEAR.CLEAR_LOCK;
		} else

			/*
			 * See if it's just CLEAR name, which means that SYMBOL should be
			 * assumed
			 */

			if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {
				type = OpCLEAR.CLEAR_SYMBOL;
				name = tokens.nextToken();
				if( tokens.assumeNextSpecial(".")) {
					if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
						return new Status(Status.EXPMEMBER);
					byteCode.add(ByteCode._LOADREF, name);
					name = tokens.nextToken();
					type = OpCLEAR.CLEAR_MEMBER;
				}
			} else {
				if( tokens.endOfStatement())
					return status = new Status(Status.EXPVARS);
				return status = new Status(Status.VERB, "CLEAR "
						+ tokens.nextToken());
			}

		/*
		 * At this point, we must have a name or it's an error.  Additionally,
		 * that name must be an IDENTIFIER type or it's an error.
		 */
		if (name == null)
			return status = new Status(Status.EXPVARS);

		if (type == -1 /* && tokens.getType() != Tokenizer.IDENTIFIER */)
			return status = new Status(Status.VERB, "CLEAR " + name);

		/*
		 * Finally, generate the code to do the appropriate type of _CLEAR
		 * operation on the given name, and we're done.
		 */

		byteCode.add(ByteCode._CLEAR, type, name);

		/*
		 * There may be a list of these
		 */
		int count = 0;
		while( tokens.assumeNextSpecial(",")) {

			if( !tokens.testNextToken(Tokenizer.IDENTIFIER)) {
				if( count == 0 )
					tokens.restoreToken();
				break;
			}
			String objName = tokens.nextToken();
			if( tokens.assumeNextSpecial("*"))
				objName = objName + "*";
			else
				if( type == OpCLEAR.CLEAR_MEMBER && tokens.assumeNextSpecial(".")) {
					byteCode.add(ByteCode._LOADREF, objName);
					objName = tokens.nextToken();
				}
			count++;
			byteCode.add(ByteCode._CLEAR, type, objName );
		}

		return new Status();

	}

}

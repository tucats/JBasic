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
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.FileParse;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.opcodes.OpCLEAR;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * Open a file for input or output. The user must provide a filename, open mode,
 * and an identifier which is used to store information about the file for
 * referencing in future statements line PRINT or INPUT, and to CLOSE the file.
 * <p>
 * <code>
 * OPEN FILE <em>name-expression</em> FOR [ INPUT | OUTPUT ] AS <em>identifier</em>
 * </code>
 * <p>
 * The <em>name-expression</em> is a string expression that represents the
 * physical file name of the file to be opened. A special reserved name of
 * "%console" means to open the standard console file of stdin/stdout.
 * <p>
 * The FOR clause tells what mode to put the file in. INPUT means that only
 * INPUT and LINE INPUT statements will be used on the file. OUTPUT means that
 * only PRINT statements will be used on the file. A file cannot be opened for
 * both INPUT and OUTPUT at the same time.
 * <p>
 * The AS clause has a symbol name that is used to hold the file identification
 * information. The symbol is created in the local symbol table. This is the
 * identifier that is used in the FILE clause of the PRINT, INPUT, CLOSE, etc.
 * statements. The variable contains a RECORD value, which holds the information
 * from the OPEN statement as well as a sequence number that uniquely identifies
 * the specific instance of an open file. For example,
 * <p>
 * <code>
 * OPEN FILE "x.dat" FOR OUTPUT AS FOO
 * 
 * PRINT FOO
 * 
 *   { FILENAME:"x.dat", IDENT:"FOO", MODE:"OUTPUT", SEQNO:23 }
 * 
 *<code>
 *<p>
 *Note that the PRINT operation does not print to the file, but prints the file identifier
 *record information.
 *<p>
 *
 * A variation of the OPEN statement is allowed which supports emulation of 
 * the "GW-BASIC syntax".  This has the form:
 * 
 * <p>
 * <code>
 *    OPEN "filename", "mode", integer
 * </code>
 * <p>
 * 
 * The mode is a string expression that must resolve to "INPUT, "OUTPUT", or 
 * "APPEND".  The integer is the file number used to access the file using
 * the "#" syntax.
 * <p>
 * @see JBasicFile
 * @see SymbolTable
 * @see Value
 * 
 * @author tom
 * @version version 1.1 December 17, 2008
 *
 */

class OpenStatement extends Statement {

	/**
	 * Compile 'OPEN' statement. Processes a token stream, and compiles it into
	 * a byte-code stream associated with the statement object. The first token
	 * in the input stream has already been removed, since it was the "verb"
	 * that told us what kind of statement object to create.
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
		final Expression exp = new Expression(session);
		boolean hasName = false;
		int mode = JBasicFile.MODE_UNDEFINED;
		String varname = null;
		boolean hasColumn = false;
		boolean hasUserInfo = false;
		boolean hasQuery = false;
		boolean indirect = false;
		

		/*
		 * There are two basic syntaxes allowed.  These are the full
		 * syntax for JBasic, and a GW-BASIC compatibility mode.  If
		 * we are in GW-BASIC mode, then check for the alternate syntax.
		 */
		
		
		int savedTokenPosition = tokens.getPosition();

		/*
		 * In this mode, the tokens must be :
		 * 		<string expression>
		 * 		<comma>
		 * 		<string expression>
		 * 		<comma>
		 * 		<integer>
		 * 
		 * If we fail on any of this, we give up and assume that
		 * it's not a GWBASIC format command and try the "regular"
		 * syntax.
		 */

		ByteCode tempBC = new ByteCode(session);
		Expression tempExp = new Expression( session );

		/*
		 * Parse file name.
		 */
		tempExp.compile(tempBC, tokens);
		if( tempExp.status.success()) {

			/*
			 * Store file name.  Require a comma
			 */
			tempBC.add(ByteCode._CVT, Value.STRING);
			if( tokens.assumeNextSpecial(",")) {

				/*
				 * Parse file mode.
				 */
				tempExp.compile(tempBC, tokens);
				if( tempExp.status.success()) {

					/*
					 * Store file mode.  Require a comma
					 */
					tempBC.add(ByteCode._CVT, Value.STRING);
					if( tokens.assumeNextSpecial(",")) {

						/*
						 * Parse file number
						 */
						tempExp.compile(tempBC, tokens);
						if( tempExp.status.success()) {

							/*
							 * Store the file number
							 */
							tempBC.add(ByteCode._CVT, Value.INTEGER);

							/*
							 * Generate the open code, then discard
							 * the temporary values.
							 */

							tempBC.add(ByteCode._OPEN);
							byteCode.concat(tempBC);
							return status = new Status();
						}
					}
				}

			}
		}
		tokens.setPosition(savedTokenPosition);


		
		ByteCode query = null;
		ByteCode width = null;
		ByteCode columns = null;
		ByteCode filename = null;

		while (true) {

			int oldMode = mode;
			
			if( tokens.endOfStatement())
				break;
			String next = tokens.nextToken().toUpperCase();

			if (tokens.getType() == Tokenizer.END_OF_STRING)
				break;

			if (next.equals("QUERY")) {
				if( hasQuery )
					return new Status(Status.DUPCLAUSE, "QUERY");
				query = new ByteCode(session);
				exp.compile(query, tokens);
				if (exp.status.failed())
					return exp.status;
				query.add(ByteCode._CVT, Value.STRING);
				hasQuery = true;
			} 
			
			else if (next.equals("USER")) {
				exp.compile(byteCode, tokens);
				if (exp.status.failed())
					return exp.status;
				byteCode.add(ByteCode._CVT, Value.STRING);
				byteCode.add(ByteCode._STOR, "__USERNAME");
				hasUserInfo = true;
			} 
			
			else if (next.equals("PASSWORD")) {
				exp.compile(byteCode, tokens);
				if (exp.status.failed())
					return exp.status;
				byteCode.add(ByteCode._CVT, Value.STRING);
				byteCode.add(ByteCode._STOR, "__PASSWORD");
				hasUserInfo = true;
			} 
			
			else if (next.equals("COLUMNS")) {

				if( hasColumn)
					return new Status(Status.DUPCLAUSE, "COLUMN");
				/*
				 * Parse COLUMNS( width, count )
				 * 
				 * where width is the width of formatted columns, and count is
				 * the number of columns in a single line of output.
				 */
				hasColumn = true;

				final Status bogusStatus = new Status(Status.FILECOL);
				if (!tokens.assumeNextToken("("))
					return bogusStatus;
				width = new ByteCode(session);
				exp.compile(width, tokens);
				if (exp.status.failed())
					return exp.status;

				if (!tokens.assumeNextToken(","))
					return bogusStatus;
				columns = new ByteCode(session);
				exp.compile(columns, tokens);
				if (exp.status.failed())
					return exp.status;

				if (!tokens.assumeNextToken(")"))
					return bogusStatus;

			} 
			
			else if (next.equals("AS")) {
				if( varname != null )
					return new Status(Status.DUPCLAUSE, "AS");
				final FileParse f = new FileParse(tokens, false);
				varname = f.getFID();
				indirect = f.getIndirect();
				if( varname == null )
					return f.getStatus();
				
			} 
			else if (next.equals("USING")) {
				boolean hasParens = false;
				if( varname != null )
					return new Status(Status.DUPCLAUSE, "AS");
				if( tokens.assumeNextSpecial("("))
					hasParens = true;
				final FileParse f = new FileParse(tokens, false);
				varname = f.getFID();
				indirect = true;
				if( varname == null )
					return f.getStatus();
				if( hasParens)
					if( !tokens.assumeNextSpecial(")"))
						return new Status(Status.PAREN);
				
			} 

			
			else if (next.equals("INPUT"))
				mode = JBasicFile.MODE_INPUT;
			else if (next.equals("OUTPUT"))
				mode = JBasicFile.MODE_OUTPUT;
			else if (next.equals("DATABASE"))
				mode = JBasicFile.MODE_DATABASE;
			else if (next.equals("JDBC"))
				mode = JBasicFile.MODE_DATABASE;
			else if (next.equals("QUEUE"))
				mode = JBasicFile.MODE_QUEUE;
			else if (next.equals("BINARY"))
				mode = JBasicFile.MODE_BINARY;
			else if (next.equals("PIPE"))
				mode = JBasicFile.MODE_PIPE;
			else if(( next.equals("CLIENT") || next.equals("SERVER")) && 
					tokens.peek(0).equals("SOCKET")) {
				if( hasName )
					return new Status(Status.DUPCLAUSE, "FILE");
				filename = new ByteCode(this.session);
				filename.add(ByteCode._STRING, next);
				filename.add(ByteCode._CONCAT, "/");
				next = tokens.nextToken().toUpperCase();
				exp.compile(filename, tokens);
				filename.add(ByteCode._CONCAT);
				if (exp.status.failed())
					return exp.status;
				hasName = true;
				mode = JBasicFile.MODE_SOCKET;
			}
			else if (next.equalsIgnoreCase("FOR")) {
				final String kind = tokens.nextToken().toUpperCase();
				if (kind.equalsIgnoreCase("INPUT"))
					mode = JBasicFile.MODE_INPUT;
				else if (kind.equals("OUTPUT"))
					mode = JBasicFile.MODE_OUTPUT;
				else if (kind.equals("APPEND"))
					mode = JBasicFile.MODE_APPEND;
				else if (kind.equals("BINARY"))
					mode = JBasicFile.MODE_BINARY;
				else if (kind.equals("PIPE"))
					mode = JBasicFile.MODE_PIPE;
				else if (kind.equals("DATABASE"))
					mode = JBasicFile.MODE_DATABASE;
				else if (kind.equals("JDBC"))
					mode = JBasicFile.MODE_DATABASE;
				else
					return new Status(Status.FILESYNTAX, 
							new Status(Status.INVFMODE, kind));

			} 
			
			else if (next.equalsIgnoreCase("FILE")) {
				if( hasName )
					return new Status(Status.DUPCLAUSE, "FILE");
				filename = new ByteCode(this.session);
				exp.compile(filename, tokens);
				if (exp.status.failed())
					return exp.status;
				hasName = true;
			} 
			
			else {
				/* If we already got a name then this is an error... catch it later */
				if( hasName )
					break;
				/*
				 * Try it without the FILE keyword to see if it's a valid
				 * expression
				 */
				tokens.restoreToken();
				filename = new ByteCode(this.session);
				exp.compile(filename,tokens);
				if (exp.status.failed())
					return exp.status;
				hasName = true;
			}
			
			if( mode != oldMode & oldMode != JBasicFile.UNDEFINED)
				return new Status(Status.DUPCLAUSE, "file mode");
		}

		if (hasUserInfo & (mode != JBasicFile.MODE_DATABASE))
			return new Status(Status.FILESYNTAX,
					"USER or PASSWORD only valid on DATABASE files");

		if (hasQuery & (mode != JBasicFile.MODE_DATABASE))
			return new Status(Status.FILESYNTAX,
					"QUERY only valid on DATABASE files");

		if (hasColumn & (mode != JBasicFile.MODE_OUTPUT)
				& (mode != JBasicFile.MODE_APPEND))
			return new Status(Status.FILECOL);

		if (varname == null)
			return new Status(Status.EXPCLAUSE, "AS file-identifier");

		if (mode == JBasicFile.UNDEFINED)
			return new Status(Status.EXPCLAUSE, "FOR file-mode");

		if (!hasName)
			return new Status(Status.EXPCLAUSE, "FILE file-name");

		/*
		 * Generate code to open the file. Now is time to write out the 
		 * code for the filename expression, followed by the _OPEN
		 * opcode.
		 */

		byteCode.concat(filename);
		
		if( indirect ) {
			byteCode.add(ByteCode._LOADFREF, 3, varname );
			byteCode.add(ByteCode._OPEN, mode);
		}
		else
			byteCode.add(ByteCode._OPEN, mode, varname);

		/*
		 * If there was a COLUMN(...) clause then write out the expressions
		 * for those values and generate the bytecode.
		 */
		if (hasColumn) {
			byteCode.concat(width);
			byteCode.concat(columns);
			byteCode.add(ByteCode._COLUMN, varname);
		}

		/*
		 * If there was a username or password then it was passed via these
		 * reserved variable names, which we can now clear away again.
		 */
		if (hasUserInfo) {
			byteCode.add(ByteCode._CLEAR, OpCLEAR.CLEAR_SYMBOL_ALWAYS, "__USERNAME");
			byteCode.add(ByteCode._CLEAR, OpCLEAR.CLEAR_SYMBOL_ALWAYS, "__PASSWORD");
		}

		/*
		 * If there was a query expression, time to generate a PRINT operation
		 * to the file of the given expression.
		 */
		if (hasQuery) {
			byteCode.add(ByteCode._LOADFREF, varname);
			byteCode.concat(query);
			byteCode.add(ByteCode._OUTNL, 1);
		}
		return status = new Status();
	}
}
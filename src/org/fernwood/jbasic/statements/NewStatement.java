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

import java.util.Date;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCodeExchange;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class NewStatement extends Statement {

	static int nextName = 0;

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		String programName = null;
		String prefix = "";
		String kind = "PROGRAM";
		String rootName = "";
		int baseLineNumber = 0;
		
		/*
		 * One possible invocation has the next token being a starting
		 * line number of the first new statement to be added.  This means
		 * that any line numbers we store away to begin with (the auto-comment
		 * header, etc.) must be less than this number.
		 */
		
		if( tokens.testNextToken(Tokenizer.INTEGER)) {
			baseLineNumber = Integer.parseInt(tokens.nextToken());
			if( baseLineNumber > 10 )
				baseLineNumber = 1;
			else
				if( baseLineNumber > 100)
					baseLineNumber = 10;
				else
					if( baseLineNumber > 1000)
						baseLineNumber = 100;
		}
		/*
		 * The user can create a new program using an existing variable that
		 * contains a structured definition of the program, such as created
		 * via the PROGRAM() function.
		 * 
		 * NEW USING(expression)
		 * 
		 * Where the expression must be a valid RECORD object that defines
		 * the program.
		 */
		
		if( tokens.assumeNextToken("USING")) {
			Expression exp = new Expression(this.session);
			Value programValue = exp.evaluate(tokens, symbols);
			if( programValue == null )
				return exp.status;
			
			if( programValue.getType() != Value.RECORD )
				return new Status(Status.INVPGMOBJ, 
						new Status(Status.EXPREC));

			Value v = programValue.getElement("NAME");
			if( v == null )
				return new Status(Status.INVPGMOBJ, 
						new Status(Status.EXPMEMBER, "NAME"));
			
			programName = v.getString();
			if (session.programs.find(programName) != null)
				return new Status(Status.NOTNEW, programName);

			/*
			 * See if this is an encoded program (bytecode)
			 */
			
			v = programValue.getElement("ISBYTECODE");
			if( v != null )
				if( v.getBoolean()) {
				ByteCodeExchange bc = new ByteCodeExchange();
				return bc.createProgram(session, programValue);
			}
			/*
			 * Okay, then it must be a stored program object.
			 */
			
			Program p = new Program(session, programName);
			v = programValue.getElement("LINES");
			if( v == null ||  v.getType() != Value.ARRAY )
				return new Status(Status.INVPGMOBJ, 
						new Status(Status.EXPMEMBER, "LINES"));

			p.register();

			int count = v.size();
			for( int ix = 1; ix <= count; ix++ ) {
				p.add(v.getString(ix));
			}
			
			session.programs.setCurrent(p);
			session.setCurrentProgramName(programName);
			
			p.clearModifiedState();
			v = programValue.getElement("USER");
			boolean isUserObject = true;
			if( v != null )
				if( v.getBoolean())
				isUserObject = false;
			p.setSystemObject(isUserObject);
			return p.link(false);
		}
		
		
		/*
		 * The user can explicitly name the program two ways.
		 * 
		 * NEW VERB FOO
		 * 
		 * or
		 * 
		 * NEW VERB$FOO
		 * 
		 * And we have to handle both cases. So start by seeing if there is a
		 * program kind token at the start that we need to pick off. Create a
		 * suitable prefix designation to form the full name.
		 */
		if (tokens.assumeNextToken("VERB")) {
			prefix = JBasic.VERB;
			kind = "VERB";
		} else if (tokens.assumeNextToken("FUNCTION")) {
			prefix = JBasic.FUNCTION;
			kind = "FUNCTION";
		} else if (tokens.assumeNextToken("TEST")) {
			prefix = JBasic.TEST;
			kind = "TEST";
		}

		/*
		 * If there is a name given, then use it. Otherwise, generate a new
		 * unique name by using an integer increment, and validating that no
		 * such name already exists. This is required so each time that JBasic
		 * is run, the NEW command doesn't generate the same name over and over.
		 * 
		 * If we have a name already, then verify that it's not already taken.
		 */
		if (tokens.testNextToken(Tokenizer.IDENTIFIER)) {
			rootName = tokens.nextToken();
			programName = prefix + rootName;
			if (session.programs.find(programName) != null)
				return new Status(Status.NOTNEW, programName);
		} 
		else if( !tokens.endOfStatement()) {
			return new Status(Status.EXPPGM);
		}
		else
			while (true) {
				rootName = JBasic.NEWPREFIX
						+ Integer.toString(NewStatement.nextName++);
				programName = prefix + rootName;
				if (session.programs.find(programName) == null) {
					session.stdout.println("Creating " + kind + " " + rootName);
					break;
				}
			}

		/*
		 * Make a new program object, and place it in the registry of stored
		 * programs. The register operation also makes it the current active
		 * program.
		 */

		final Program newProgram = new Program(session, programName);
		session.programs.setCurrent(newProgram);
		newProgram.register();
		boolean autoComment = session.getBoolean("SYS$AUTOCOMMENT");

		/*
		 * Make sure a prompt is printed after this statement even if in 
		 * NOPROMPTMODE.
		 */
		this.session.setNeedPrompt(true);
		
		/*
		 * There may be additional tokens that are part of the declaration
		 * statement.  If so, grab them now so they can be part of the
		 * first statement. When that statement gets compiled below, they
		 * will be checked for valid syntax, etc. so we don't really care
		 * here.
		 */
		String suffix = tokens.getBuffer();
		tokens.flush();
		
		/*
		 * The program must start with a PROGRAM statement or similar
		 * declaration. If the user gave it a name we can use, then let's choose
		 * the type. Otherwise, assume PROGRAM.
		 */


		if( programName != null ) {
			if (programName.startsWith(JBasic.VERB)) {
				kind = "VERB";
				programName = programName.substring(JBasic.VERB.length());
			} else if (programName.startsWith(JBasic.FUNCTION)) {
				kind = "FUNCTION";
				programName = programName.substring(JBasic.FUNCTION.length());
				if( suffix.length() < 2)
					suffix = "()" + suffix;
			} else if (programName.startsWith(JBasic.TEST)) {
				kind = "TEST";
				programName = programName.substring(JBasic.TEST.length());
			}
		}
		final Date rightNow = new Date(System.currentTimeMillis());

		if (autoComment) {
			status = newProgram.add("1 " + kind + " " + programName + " " + suffix);
			if( status.failed())
				return status;
			
			newProgram.add("2 // Version: 1.0");
			newProgram.add("3 // Date:    " + rightNow.toString());
			newProgram.add("4 // Author:  " + System.getProperty("user.name"));
		} else
			newProgram.add("1 " + kind + " " + programName + suffix);

		if (kind.equals("FUNCTION"))
			newProgram.add("10000 return 0");
		newProgram.add("10001 end");
		
		session.setCurrentProgramName(newProgram.getName());
		if( baseLineNumber == 0 )
			newProgram.renumber(1000, 10);
		
		return status = new Status(Status.SUCCESS);
	}
}

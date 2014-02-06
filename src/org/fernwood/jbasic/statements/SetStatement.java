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
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.LValue;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.opcodes.OpCLEAR;
import org.fernwood.jbasic.opcodes.OpSYS;
import org.fernwood.jbasic.opcodes.OpTHREAD;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * SET statement. This parses a variety of option values and uses the tokens
 * or specified values to set various system variables.
 * 
 * @author tom
 * @version 1.0 Oct 18, 2004
 */

class SetStatement extends Statement {

	private static final int OPT_SET = 1;

	private static final int OPT_SET_TRUE = 2;

	private static final int OPT_SET_FALSE = 3;

	private static final int OPT_SET_STRING = 4;
	
	private static final int OPT_SET_INT = 5;
	
	private static final int OPT_SET_ROOT = 6;
	
	private static final int OPT_CLR_ROOT = 7;

	private static final int OPT_SERVER_ON = 8;
	
	private static final int OPT_SERVER_OFF = 9;

	private static final int OPT_SET_PACK = 10;

	private static final int OPT_CLR_PACK = 11;
	
	private static final int OPT_SET_VALUE = 12;
	
	private static final int OPT_CLR_VALUE = 13;
	

	class SetOption {
		String name;
		int action;
		String symbol;
		String typeMap;
		
		SetOption(final String newName, final int newAction,
				final String newSymbol) {
			name = newName;
			action = newAction;
			symbol = newSymbol;
		}
		SetOption(final String newName, final int newAction,
				final String newSymbol, final String newType) {
			name = newName;
			action = newAction;
			symbol = newSymbol;
			typeMap = newType;
		}

	}

	/*
	 * Define the table of SET options, what they do, and the global variable that they
	 * can affect. Note the optional fourth argument, which is a description of the required
	 * data type when OPT_SET_VALUE is used.
	 */
	private final SetOption setOptions[] = {
			new SetOption("OPTIMIZE",		OPT_SET,		"SYS$OPTIMIZE"),
			new SetOption("NOOPTIMIZE",		OPT_SET_FALSE,	"SYS$OPTIMIZE"),
			new SetOption("OPTASM",			OPT_SET,		"SYS$OPT_ASM"),
			new SetOption("NOOPTASM",		OPT_SET_FALSE,	"SYS$OPT_ASM"),
			new SetOption("OPTDEADCODE",	OPT_SET,		"SYS$OPT_DEADCODE"),
			new SetOption("NOOPTDEADCODE",	OPT_SET_FALSE,	"SYS$OPT_DEADCODE"),
			new SetOption("OPTLOOPS",       OPT_SET,        "SYS$LOOP_OPT"),
			new SetOption("NOOPTLOOPS",     OPT_SET_FALSE,  "SYS$LOOP_OPT"),
			new SetOption("OPTSTRUCTS",		OPT_SET,		"SYS$STRUCTURE_POOLING"),
			new SetOption("NOOPTSTRUCTS",	OPT_SET_FALSE,	"SYS$STRUCTURE_POOLING"),
			new SetOption("OPTDEBUG",		OPT_SET,		"SYS$DEBUG_OPT"),
			new SetOption("NOOPTDEBUG",		OPT_SET_FALSE,	"SYS$DEBUG_OPT"),
			new SetOption("TOKENIZE",		OPT_SET,		"SYS$RETOKENIZE"),
			new SetOption("NOTOKENIZE",		OPT_SET_FALSE,	"SYS$RETOKENIZE"),
			new SetOption("STATIC_TYPES", 	OPT_SET,		"SYS$STATIC_TYPES"),
			new SetOption("DYNAMIC_TYPES",	OPT_SET_FALSE,	"SYS$STATIC_TYPES"),
			new SetOption("NOSTATIC_TYPES", OPT_SET_FALSE,	"SYS$STATIC_TYPES"),
			new SetOption("STATEMENT_TEXT", OPT_SET,		"SYS$STATEMENT_TEXT"),
			new SetOption("NOSTATEMENT_TEXT", OPT_SET_FALSE,"SYS$STATEMENT_TEXT"),
			new SetOption("AUTOCOMMENT",	OPT_SET,		"SYS$AUTOCOMMENT"),
			new SetOption("NOAUTOCOMMENT",	OPT_SET_FALSE,	"SYS$AUTOCOMMENT"),
			new SetOption("AUTORENUMBER",	OPT_SET,		"SYS$AUTORENUMBER"),
			new SetOption("NOAUTORENUMBER",	OPT_SET_FALSE,	"SYS$AUTORENUMBER"),
			new SetOption("LABELWIDTH", 	OPT_SET_INT,	"SYS$LABELWIDTH"),
			new SetOption("TRACE",			OPT_SET, 		"SYS$TRACE_BYTECODE"),
			new SetOption("NOTRACE",		OPT_SET_FALSE,	"SYS$TRACE_BYTECODE"),
			new SetOption("TIMEGC",			OPT_SET,		"SYS$TIME_GC"),
			new SetOption("NOTIMEGC",		OPT_SET_FALSE,	"SYS$TIME_GC"),
			new SetOption("ROOT_USER",		OPT_SET_ROOT,	"SYS$ROOTUSER"),
			new SetOption("NOROOT_USER",	OPT_CLR_ROOT,	"SYS$ROOTUSER"),
			new SetOption("FUNCTIONERRORS", OPT_SET_TRUE, "SYS$SIGNAL_FUNCTION_ERRORS"),
			new SetOption("NOFUNCTIONERRORS", OPT_SET_FALSE, "SYS$SIGNAL_FUNCTION_ERRORS"),
			new SetOption("PROMPTMODE",		OPT_SET,		"SYS$CMDPROMPT"),
			new SetOption("NOPROMPTMODE",	OPT_SET_FALSE,	"SYS$CMDPROMPT"),
			new SetOption("SANDBOX",        OPT_SET_ROOT,	"SYS$SANDBOX"),
			new SetOption("MULTIUSER",		OPT_SERVER_ON,	"SYS$MULTIUSER"),
			new SetOption("NOMULTIUSER",	OPT_SERVER_OFF,	"SYS$MULTIUSER"),
			new SetOption("SQL",			OPT_SET,		"SYS$SQL_COMMANDS"),
			new SetOption("NOSQL",			OPT_SET_FALSE,	"SYS$SQL_COMMANDS"),
			new SetOption("SQLDISASM",		OPT_SET,		"SYS$SQL_DISASM"),
			new SetOption("NOSQLDISASM",	OPT_SET_FALSE,	"SYS$SQL_DISASM"),
			new SetOption("SQLOPT",			OPT_SET,		"SYS$SQL_OPT"),
			new SetOption("NOSQLOPT",		OPT_SET_FALSE,	"SYS$SQL_OPT"),
			new SetOption("LANGUAGE",		OPT_SET_VALUE,	"SYS$LANGUAGE", 	"string"),
			new SetOption("PACKAGE",		OPT_SET_PACK, 	JBasic.PACKAGES),
			new SetOption("NOPACKAGE",		OPT_CLR_PACK, 	JBasic.PACKAGES),
			new SetOption("MACRO",			OPT_SET_VALUE,	"SYS$MACRO_QUOTES", "[string,string]"),
			new SetOption("NOMACRO",		OPT_CLR_VALUE,	"SYS$MACRO_QUOTES"),
			new SetOption("PROMPT",			OPT_SET_VALUE,	"SYS$PROMPT", 		"string"),
			new SetOption("INPUTPROMPT",	OPT_SET_VALUE,	"SYS$INPUT_PROMPT",	"string"),
			new SetOption("DEBUGPROMPT",	OPT_SET_VALUE,	"SYS$DEBUG_PROMPT",	"string")
			};

	/**
	 * Return the current debug status string, formatted for output in a SHOW
	 * DEBUG statement.
	 * 
	 * @param session
	 *            The JBasic object that contains the current session.
	 * @param indent
	 *            A string that prefixes each line of output. This is typically
	 *            a number of spaces used to indent the output.
	 * @return The formatted string containing multiple output lines separated
	 *         by embedded newline characters.
	 */
	public static String getDebugString(final JBasic session, final String indent) {
		final SetStatement s = new SetStatement();
		s.session = session;
		return s.getPrivateDebugString(indent);
	}

	private String getPrivateDebugString(final String prefix) {
		int count = 0;
		int indent = 0;

		/*
		 * If a prefix is given, we put this at the start of the result and
		 * indent the options by the same length, so it makes a nice columnar
		 * output, like:
		 * 
		 * OPTIONS: PROMPT="> ", TOKENIZE, BUILTINS
		 * 
		 * If no prefix is given, then we don't indent or add returns.
		 * 
		 */
		String result = "";
		if (prefix != null) {
			indent = prefix.length();
			result = prefix;
		}
		final int len = setOptions.length;
		String indentPad = "";
		while (indentPad.length() < indent)
			indentPad = indentPad + " ";

		for (int ix = 0; ix < len; ix++) {
			if (setOptions[ix].action == OPT_SET_FALSE)
				continue;
			if (setOptions[ix].action == OPT_CLR_ROOT )
				continue;
			if (setOptions[ix].action == OPT_SET_ROOT )
				continue;
			if( setOptions[ix].action == OPT_SERVER_OFF )
				continue;
			if( setOptions[ix].action == OPT_CLR_PACK )
				continue;
			if( setOptions[ix].action == OPT_CLR_VALUE )
				continue;
			

			count++;

			Value e;
			try {
				e = session.globals().reference(
						setOptions[ix].symbol);
			} catch (JBasicException e1) {
				e = new Value(false);
			}
			if (e == null)
				continue;

			if (count > 1) {
				result = result + ", ";
				if (prefix != null)
					result = result + JBasic.newLine + indentPad;
			}

			if (e.isType(Value.BOOLEAN)) {
				if (e.getBoolean() == false)
					result = result + "NO";
				result = result + setOptions[ix].name;
			} else
				result = result + setOptions[ix].name + "="
						+ Value.toString(e, true);
		}
		return result;
	}

	/**
	 * Set a named DEBUG flag to be true or false based on the contents of a
	 * Value object.
	 * 
	 * @param name
	 *            The name of the option, which must be in uppercase.
	 * @param valueOnStack
	 *            Has a value already been compiled on the stack?
	 * @return Status indicator, which indicates if the name was valid or not.
	 */
	private Status setDebugOption(final String name, boolean valueOnStack) {

		final int len = setOptions.length;

		for (int ix = 0; ix < len; ix++)
			if (name.equals(setOptions[ix].name)) {

				switch (setOptions[ix].action) {
				
				case OPT_SET_PACK:
					if( !valueOnStack)
						return new Status(Status.INVSET);
					
					byteCode.add(ByteCode._PACKAGE, 0);
					break;
				case OPT_CLR_PACK:
					byteCode.add(ByteCode._PACKAGE, valueOnStack? 1 : 2);
					break;
					
				case OPT_SERVER_ON:
				case OPT_SERVER_OFF:
					if( valueOnStack ) {
						return new Status(Status.INVSET);
					}
					byteCode.add(ByteCode._CLEAR, OpCLEAR.CLEAR_SYMBOL_ALWAYS, setOptions[ix].symbol);
					if( !valueOnStack )
						byteCode.add(ByteCode._BOOL, 
							(setOptions[ix].action == OPT_SERVER_ON) ? 1 : 0);
					else
						byteCode.add(ByteCode._CVT, Value.BOOLEAN);
					
					byteCode.add(ByteCode._STOR, -1, setOptions[ix].symbol);
					byteCode.add(ByteCode._PROT, -1, setOptions[ix].symbol);
					
					/*
					 * Issue the command that starts or stops the multi-user
					 * server mode.
					 */
					
					boolean isStart = (setOptions[ix].action == OPT_SERVER_ON);
					byteCode.add(ByteCode._THREAD,
							isStart? OpTHREAD.START_SERVER : OpTHREAD.STOP_SERVER);
					break;


				case OPT_CLR_VALUE:
					if( valueOnStack ) {
						return new Status(Status.INVSET);
					}
					byteCode.add(ByteCode._CLEAR, OpCLEAR.CLEAR_SYMBOL_ALWAYS, setOptions[ix].symbol );
					break;
					
				case OPT_SET_VALUE:
					if(!valueOnStack ) {
						return new Status(Status.INVSET);
					}
					if( setOptions[ix].typeMap != null ) {
						byteCode.add(ByteCode._TYPECHK, 1, setOptions[ix].typeMap);
					}
					byteCode.add(ByteCode._STOR, setOptions[ix].symbol);
					break;
							

				case OPT_SET_ROOT:
				case OPT_CLR_ROOT:
					if( !valueOnStack ) {
						byteCode.add(ByteCode._INTEGER, 0);
					}
					byteCode.add(ByteCode._SYS, OpSYS.SYS_ROOT);
					break;
				case OPT_SET:
					if (!valueOnStack)
						byteCode.add(ByteCode._BOOL, 1 );
					else
						byteCode.add(ByteCode._CVT, Value.BOOLEAN);
					byteCode.add(ByteCode._STOR, setOptions[ix].symbol);
					break;
				case OPT_SET_TRUE:
					byteCode.add(ByteCode._BOOL, 1 );
					byteCode.add(ByteCode._STOR, setOptions[ix].symbol);
					break;
				case OPT_SET_FALSE:
					byteCode.add(ByteCode._BOOL, 0 );
					byteCode.add(ByteCode._STOR, setOptions[ix].symbol);
					break;
				case OPT_SET_INT:
					if( !valueOnStack) 
						return new Status(Status.EXPRESSION, new Status(Status.EXPVALUE));
					byteCode.add(ByteCode._CVT, Value.INTEGER);
					byteCode.add(ByteCode._STOR, setOptions[ix].symbol);
					break;
				case OPT_SET_STRING:
					if( !valueOnStack )
						byteCode.add(ByteCode._STRING, "");
					else
						byteCode.add(ByteCode._CVT, Value.STRING);
					byteCode.add(ByteCode._STOR, setOptions[ix].symbol);
					break;

				default:
					return new Status(Status.FAULT,
							"invalid option semantic code");

				}
				return new Status();
			}
		return new Status(Status.UNKVAR, name);
	}

	/**
	 * Compile 'set' statement.  This really translates into setting on or
	 * more system variables to specific states.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status compile(final Tokenizer tokens) {

		/*
		 * The SET statement is followed by a list of keywords, possibly
		 * separated by commas.
		 */

		this.byteCode = new ByteCode(session, this);
		String name = null;
		Status sts = null;
		boolean valueOnStack = false;
		int count = 0;
		
		int mark = tokens.getPosition();
		
		/*
		 * See if this is the SET <lvalue> TO <expression> version
		 */
		
		LValue lv = new LValue(session, false);
		sts = lv.compileLValue(byteCode, tokens);
		if( sts.success()) {
			if( tokens.assumeNextToken("TO")) {
				Expression e = new Expression(session);
				sts = e.compile(byteCode, tokens);
				if( sts.success()) {
					sts = lv.compileStore();
					if( sts.success())
						return sts;
				}
			}
		}
		/*
		 * Nope, it's an option setting. Clean up any partially generated
		 * byteCode from the previous fork and start reading option lists.
		 */
		byteCode = new ByteCode(session);
		tokens.setPosition(mark);
		
		while (true) {

			if (!tokens.testNextToken(Tokenizer.IDENTIFIER))
				break;
			count++;
			name = tokens.nextToken();
			
			if( name.equals("PERMISSION")) {
				if( !tokens.assumeNextToken("="))
					return new Status(Status.ASSIGNMENT);
				if( tokens.assumeNextToken("(")) {
					while(true) {
						if( tokens.assumeNextToken(")"))
							break;
						if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
							return new Status(Status.INVPERM);
				
						String perm = tokens.nextToken();
						if( perm.equals("NONE")) 
							perm = "NOALL";
						else
							if( !perm.equals("ALL") && !Permissions.valid(perm))
								if( !perm.substring(0,2).equals("NO") || !Permissions.valid(perm.substring(2)))
									return new Status(Status.INVPERM, perm);
						
						byteCode.add(ByteCode._SYS, OpSYS.SYS_SETPERM, perm);
						if( tokens.assumeNextToken(")"))
							break;
						if( tokens.endOfStatement())
							return new Status(Status.PAREN);
						if( !tokens.assumeNextToken(","))
							continue;
					}
				}
				else {
					if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
						return new Status(Status.INVPERM);
					String perm = tokens.nextToken();
					if( perm.equals("NONE")) 
						perm = "NOALL";
					else 
						if( !perm.equals("ALL") && !Permissions.valid(perm))
							if( !perm.substring(0,2).equals("NO") || !Permissions.valid(perm.substring(2)))
								return new Status(Status.INVPERM, perm);
				
					byteCode.add(ByteCode._SYS, OpSYS.SYS_SETPERM, perm);
				}
				
				if( tokens.testNextToken(",")) {
					tokens.nextToken();
					continue;
				}
				break;
			}
			valueOnStack = false;
			if (tokens.assumeNextToken("=")) {
				final Expression exp = new Expression(session);
				exp.compile(byteCode, tokens);
				if (exp.status.failed())
					return exp.status;
				valueOnStack = true;
			}

			if( name.equals("FSM_MOUNT")) {
				byteCode.add(ByteCode._SYS, OpSYS.SYS_FSM_MOUNT);
				return new Status();
			}
			if( name.equals("NEW_FORMATTER")) {
				byteCode.add(ByteCode._SYS,  OpSYS.SYS_FORMATTER_ON);
				return new Status();
			}
			if( name.equals("NONEW_FORMATTER")) {
				byteCode.add(ByteCode._SYS, OpSYS.SYS_FORMATTER_OFF);
				return new Status();
			}
			
			if( name.equals("STRING_POOLING")) {
				byteCode.add(ByteCode._SYS, 
						(valueOnStack? OpSYS.SYS_STRING_POOL_SIZE : 
							OpSYS.SYS_STRING_POOL_ON));
				return new Status();
			}

			if( name.equals("NOSTRING_POOLING")) {
				byteCode.add(ByteCode._SYS, OpSYS.SYS_STRING_POOL_OFF);
				return new Status();
			}

			if( name.equals("LOGGING")) {
				if( !valueOnStack )
					byteCode.add(ByteCode._INTEGER, 3);
				byteCode.add(ByteCode._LOGGING);
				return new Status();
			}
			if( name.equals("NEEDPROMPT") & !valueOnStack) {
				byteCode.add(ByteCode._NEEDP, 1);
				return new Status();
			}
			/*
			 * SET SERVER followed by specific sub-verbs is translated into an
			 * invocation of the SERVER extension.
			 */
			if (name.equals("SERVER") & !valueOnStack) {
				String[] subVerbs = new String[] { "ADD", "DELETE", "GRANT", "DEFINE",
						"REVOKE", "START", "STOP", "QUIT", "LOAD", "SAVE", "MODIFY" };
				for (String subVerb : subVerbs )
					if (tokens.testNextToken(subVerb)) {
						byteCode.add(ByteCode._STRING, "SERVER "
								+ tokens.getBuffer());
						byteCode.add(ByteCode._EXEC);
						tokens.flush();
						return new Status();
					}
				return new Status(Status.VERB, tokens.nextToken());
			}
			
			if( name.equals("PERMISSION")) {
				if( !valueOnStack) {
					final Expression exp = new Expression(session);
					exp.compile(byteCode, tokens);
					if (exp.status.failed())
						return exp.status;
					valueOnStack = true;
				}
				byteCode.add(ByteCode._SYS, OpSYS.SYS_SETPERM);
				sts = new Status();
			}
			/*
			 * Password is implemented as a special THREAD option and not handled
			 * like other standard options either.
			 */
			else
			if( name.equals("PASSWORD")) {
				if( !valueOnStack) {
					final Expression exp = new Expression(session);
					exp.compile(byteCode, tokens);
					if (exp.status.failed())
						return exp.status;
					valueOnStack = true;
				}
				byteCode.add(ByteCode._THREAD, OpTHREAD.SET_PASSWORD);
				sts = new Status();
			}
			else
				sts = setDebugOption(name, valueOnStack);
			
			if (sts.failed())
				return sts;

			if (tokens.endOfStatement())
				break;
			
			if (!tokens.assumeNextToken(","))
				return new Status(Status.INVSET);
		}
		
		if( count == 0 )
			return new Status(Status.INVSET);
		
		byteCode.add(ByteCode._NEEDP, 1 );

		return new Status();
	}
}

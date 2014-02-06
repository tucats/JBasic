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
package org.fernwood.jbasic;

import org.fernwood.jbasic.value.Value;

/**
 * Status object. This is used to pass information between objects and classes
 * in the jbasic environment. The status block is used to convey information
 * about the success of an operation, the specific condition it represents, and
 * any required additional data (substitution string) used to fully explain the
 * error.
 * <p>
 * The class also includes a method for producing a formatted message, that
 * processes the substitution data if any is present and returns a displayable
 * string.
 * 
 * @author Tom Cole
 * @version version 1.0 Jun 24, 2004
 * 
 */
public class Status {


	/**
	 * Flag indicating if a given status block has been printed already. This is
	 * useful to prevent a status object passed back up a call chain from being
	 * printed multiple times.
	 * 
	 */
	private boolean fAlreadyBeenPrinted;

	/**
	 * Flag indicating if this status block represents a SUCCESS versus a
	 * FAILURE state. This is internally known because the text code used to
	 * identify the status has a "*" as it's first character. However, that
	 * check is done only when the object is created or a status code assigned,
	 * and we later just check this boolean for efficiency.
	 * 
	 */
	private boolean fSuccess;

	/**
	 * The string mnemonic message code for this instance of a Status object.
	 */
	private String messageCode;

	/**
	 * This holds the string substitution parameter, if there is one. Not all
	 * messages have a string substitution, so this can be null. This will be
	 * inserted into the formatted message output wherever the "[]" appears in
	 * the message text.
	 */
	private String messageParameter;

	/**
	 * This hold the line number where the error was thrown, if known.
	 */
	private int lineNumber;

	/**
	 * This holds the name of the program that triggered the fault.
	 */
	private String program;

	/**
	 * For cascading errors, this contains the nested status code.
	 */
	private Status nestedStatus;

	/*
	 * Note that successful signal values start with an "*" to make them easy to
	 * test for failure, etc. They also cannot be signaled by a user program,
	 * since the signal must be an identifier.
	 */

	/**
	 * Message: No compilation support for <null> statement
	 */
	public final static String NOCOMPILE = "*NOCOMPILE";

	/**
	 * Message: Quit JBasic
	 */
	public final static String QUIT = "*QUIT";

	/**
	 * Message: Return from program or function
	 */
	public final static String RETURN = "*RETURN";

	/**
	 * Message: JBasicStatement added to active program
	 */
	public final static String STMTADDED = "*STMTADDED";

	/**
	 * Message: No error
	 */
	public final static String SUCCESS = "*SUCCESS";

	/**
	 * Message: QUIT canceled
	 */
	public final static String UNSAVED = "*UNSAVED";

	/**
	 * Message: Invalid operation on active user %s
	 */
	public static final String ACTUSER = "ACTUSER";

	/**
	 * Message: Argument list error
	 */
	public final static String ARGERR = "ARGERR";

	/**
	 * Message: Argument not given for <null>
	 */
	public final static String ARGNOTGIVEN = "ARGNOTGIVEN";

	/**
	 * Message: Argument of wrong type
	 */
	public final static String ARGTYPE = "ARGTYPE";

	/**
	 * Message: Invalid array syntax
	 */
	public final static String ARRAY = "ARRAY";

	/**
	 * Message: Array index out of bounds
	 */
	public final static String ARRAYBOUNDS = "ARRAYBOUNDS";

	/**
	 * Message: Expected OPCODE not found
	 */
	public final static String ASMEXPOPCODE = "ASMEXPOPCODE";

	/**
	 * Message: Invalid or unknown OPCODE '<null>'
	 */
	public final static String ASMINVOPCODE = "ASMINVOPCODE";

	/**
	 * Message: Missing '=' operator
	 */
	public final static String ASSIGNMENT = "ASSIGNMENT";

	/**
	 * Message: Bad data type name "%s"
	 */
	
	public final static String BADTYPE = "BADTYPE";
	
	/**
	 * Message: Call depth limit exceeded
	 */
	public static final String CALLDEPTH = "CALLDEPTH";
	
	/**
	 * Message: Language dialect does not allow %s
	 */
	public static final String DIALECT = "DIALECT";

	/**
	 * Message: Syntax error, duplicate [] clause specified.
	 */
	public static final String DUPCLAUSE = "DUPCLAUSE";

	/**
	 * Message: Duplicate label %s
	 */
	public static final String DUPLABEL = "DUPLABEL";

	/**
	 * Message: Invalid or unexpected ELSE statement
	 */
	public final static String ELSE = "ELSE";

	/**
	 * Message: End of file
	 */

	public final static String EOF = "EOF";

	/**
	 * Message: Error:
	 */
	public final static String ERROR = "ERROR";

	/**
	 * Message: Unexpected shell command result <null>
	 */
	public final static String EXEC = "EXEC";

	/**
	 * Message: Expected array name not found
	 */
	public final static String EXPARRAY = "EXPARRAY";

	/**
	 * Message: Expected keyword AS not found
	 */
	public static final String EXPAS = "EXPAS";

	/**
	 * Message: Expected destination of CALL not found
	 */
	
	public final static String EXPCALL = "EXPCALL";
	
	/**
	 * Message: Syntax error, expected [] clause not found.
	 */
	public static final String EXPCLAUSE = "EXPCLAUSE";

	/**
	 * Message: Expected file name not found.
	 */
	public final static String EXPFNAME = "EXPFNAME";
	
	/**
	 * Message: Expected index variable not found
	 */
	public final static String EXPINDEX = "EXPINDEX";

	/**
	 * Message: Expected label not found
	 */
	public final static String EXPLABEL = "EXPLABEL";

	/**
	 * Message: Expected language code not found
	 */
	
	public final static String EXPLANG = "EXPLANG";
	
	/**
	 * Message: Expected MESSAGE code not found
	 */
	public final static String EXPMESSAGE = "EXPMESSAGE";

	/**
	 * Message: Expected program name not found
	 */
	public final static String EXPPGM = "EXPPGM";
	
	/**
	 * Message: Missing or invalid expression
	 */
	public final static String EXPRESSION = "EXPRESSION";

	/**
	 * Message: Calling program expects RETURN value but called program does not
	 * return one
	 */
	public final static String EXPRETVAL = "EXPRETVAL";

	/**
	 * Message: Invalid expression syntax, <null>
	 */
	public final static String EXPSYNTAX = "EXPSYNTAX";

	/**
	 * Message: Extra text after statement end: '<null>'
	 */
	public final static String EXTRA = "EXTRA";

	/**
	 * Message: Java exception, <null>
	 */
	public final static String FAULT = "FAULT";

	/**
	 * Message: Error on file operation, <null>
	 */
	public final static String FILE = "FILE";

	/**
	 * Message: Invalid use of COLUMNS()
	 */
	public final static String FILECOL = "FILECOL";

	/**
	 * Message: Missing comma after FILE clause
	 */
	public final static String FILECOMMA = "FILECOMMA";

	/**
	 * Message: Invalid operation on console device
	 */
	public final static String FILECONSOLE = "FILECONSOLE";

	/**
	 * Message: File identifier %s already in use
	 */
	public final static String FILEINUSE = "FILEINUSE";

	/**
	 * Message: invalid file mode for open
	 */
	public final static String FILEMODE = "FILEMODE";

	/**
	 * Message: File <null> not found
	 */
	public final static String FILENF = "FILENF";

	/**
	 * Message: File %s not open for BINARY I/O
	 */
	public static final String FILENOTBIN = "FILENOTBIN";

	/**
	 * Message: FILE clause syntax error <null>
	 */
	public final static String FILESYNTAX = "FILESYNTAX";

	/**
	 * Message: System files cannot be closed by user programs
	 */
	public static final String FILESYS = "FILESYS";

	/**
	 * Message: File <null> not found or cannot be deleted
	 */
	public final static String FNDEL = "FNDEL";

	/**
	 * Message: File <null> not open for input or output
	 */
	public final static String FNOPEN = "FNOPEN";

	/**
	 * Message: File <null> not open for input
	 */
	public final static String FNOPENINPUT = "FNOPENINPUT";

	/**
	 * Message: File <null> not open for output
	 */
	public final static String FNOPENOUTPUT = "FNOPENOUTPUT";

	/**
	 * Message: FOR loop increment cannot be zero
	 */
	public final static String FORINCR = "FORINCR";

	/**
	 * Message: Mismatched FOR-NEXT index variable <null>
	 */
	public final static String FORINDEX = "FORINDEX";

	/**
	 * Message: Format string error <string>
	 */
	public final static String FORMATERR = "FORMATERR";
	

	/**
	 * Message: RETURN without GOSUB
	 */
	public final static String GOSUB = "GOSUB";

	/**
	 * Message: Missing or invalid arrayValue index expression
	 */
	public final static String IDXEXP = "IDXEXP";

	/**
	 * Message: Invalid arrayValue reference syntax
	 */
	public final static String IDXSYNTAX = "IDXSYNTAX";

	/**
	 * Message: Missing or invalid conditional expression
	 */
	public final static String IF = "IF";

	/**
	 * Message: file <null> not opened for input
	 */
	public final static String INFILE = "INFILE";

	/**
	 * Message: Error in INPUT statement, <null>
	 */
	public final static String INPUTERR = "INPUTERR";

	/**
	 * Message: Insufficient arguments given
	 */
	public final static String INSFARGS = "INSFARGS";

	/**
	 * Message: In statement:
	 */
	public final static String INSTATEMENT = "INSTATEMENT";

	/**
	 * Message: Interrupted by user
	 */
	public final static String INTERRUPT = "INTERRUPT";

	/**
	 * Message: Invalid LET
	 */
	public final static String INVALIDLET = "INVALIDLET";

	/**
	 * Message: Invalid COMPILE command <null>
	 */
	public final static String INVCOMPILE = "INVCOMPILE";

	/**
	 * Message: Invalid DIM AS syntax or invalid type name
	 */
	public final static String INVDIM = "INVDIM";

	/**
	 * Message: Invalid EXECUTE expression
	 */
	public final static String INVEXEC = "INVEXEC";

	/**
	 * Message: Invalid FILE ID
	 */
	public final static String INVFID = "INVFID";

	/**
	 * Message: Invalid format specification %s
	 */
	public static final String INVFMT = "INVFMT";

	/**
	 * Message: Invalid FOR statement
	 */
	public final static String INVFOR = "INVFOR";

	/**
	 * Message: Invalid use of label %s in a command
	 */
	public final static String INVLABEL = "INVLABEL";

	/**
	 * Message: Invalid line number %s
	 */
	
	public final static String INVLINE = "INVLINE";
	
	/**
	 * Message: Invalid name "%s"
	 */
	public final static String INVNAME = "INVNAME";
	
	/**
	 * Message: <null> does not exist or is an invalid object
	 */
	public final static String INVOBJECT = "INVOBJECT";

	/**
	 * Message: ON statement not valid outside running program.
	 */
	public final static String INVONSTACK = "INVONSTACK";

	/**
	 * Message: Invalid file system path %s
	 */
	public static final String INVPATH = "INVPATH";

	/**
	 * Message: Invalid MATCHES pattern string: <null>
	 */
	public final static String INVPATTERN = "INVPATTERN";
	
	/**
	 * Invalid PERMISSION name %s
	 */
	public static final String INVPERM = "INVPERM";

	/**
	 * Message: Invalid PROGRAM statement
	 */
	public final static String INVPGM = "INVPGM";
	
	/**
	 * Message: Invalid PROGRAM object, %s
	 */
	public static final String INVPGMOBJ = "INVPGMOBJ";

	/**
	 * Message: Invalid PRINT syntax
	 */
	public final static String INVPRINT = "INVPRINT";

	/**
	 * Message: Invalid PRINT, missing comma expected after USING clause.
	 */
	public final static String INVPRINTUSING = "INVPRINTUSING";

	/**
	 * Message: Invalid file record definition, <null>
	 */
	public final static String INVRECDEF = "INVRECDEF";

	/**
	 * Message: Invalid RECORD constant
	 */
	public final static String INVRECORD = "INVRECORD";

	/**
	 * Message: Invalid use of RECORD type
	 */
	public static final String INVRECUSE = "INVRECUSE";

	/**
	 * Message: Invalid RENUMBER starting line number %s
	 */
	
	public final static String INVRENSTART = "INVRENSTART";
	
	/**
	 * Message: Invalid RENUMBER increment %s
	 */
	
	public final static String INVRENINC = "INVRENINC";
	
	/**
	 * Message: Invalid RETURNS clause
	 */
	public final static String INVRET = "INVRET";
	
	/**
	 * Message: Invalid scope usage for %s
	 */
	public static final String INVSCOPE = "INVSCOPE";

	/**
	 * Message: Invalid SET statement syntax or option
	 */
	public final static String INVSET = "INVSET";

	/**
	 * Message: Invalid SHOW option
	 */
	public final static String INVSHOW = "INVSHOW";

	/**
	 * Message: Invalid SORT BY syntax
	 */
	public final static String INVSORT = "INVSORT";

	/**
	 * Message: Array contains invalid type
	 */
	public final static String INVTYPE = "INVTYPE";

	/**
	 * Message: Invalid or missing UNTIL expression
	 */
	public final static String INVUNTIL = "INVUNTIL";

	/**
	 * Message: Invalid USING() syntax
	 */
	public final static String INVUSING = "INVUSING";

	/**
	 * Message: Unexpected FILE I/O error, <null>
	 */
	public final static String IOERROR = "IOERROR";

	/**
	 * Message: JDBC error, %s
	 */

	public final static String JDBC = "JDBC";

	/**
	 * Message: Invalid or unexpected keyword <text>
	 */
	public final static String KEYWORD = "KEYWORD";
	
	/**
	 * Message: Unsupported language code "%s"
	 */
	
	public static final String LANGUAGE = "LANGUAGE";

	/**
	 * Message: Line number %d not found
	 */
	public final static String LINENUM = "LINENUM";
	
	/**
	 * Message: Cannot link program. <null>
	 */
	public final static String LINKERR = "LINKERR";

	/**
	 * Message: Please log in to JBasic
	 */
	public final static String _LOGIN = "_LOGIN";
	
	/**
	 * Message: Invalid assignment target '<null>'
	 */
	public final static String LVALUE = "LVALUE";

	/**
	 * Message: Arithmetic error, %s
	 */
	public final static String MATH = "MATH";
	
	/**
	 * Message: This statement can only be used in a program, not as a command
	 */
	public final static String NOACTIVEPGM = "NOACTIVEPGM";

	/**
	 * Message: No ByteCode generated
	 */
	public final static String NOBYTECODE = "NOBYTECODE";

	/**
	 * Message: Compound statement not allowed after %s command
	 */
	public static final String NOCOMPOUND = "NOCOMPOUND";

	/**
	 * Message: Unable to delete readonly or system variable <null>
	 */
	public final static String NODELVAR = "NODELVAR";
	
	/**
	 * Message: No debugger active
	 */
	public static final String NODBG = "NODBG";

	/**
	 * Message: no lines to delete.
	 */
	public static final String NODELLINE = "NODELLINE";

	/**
	 * Message: Mismatched DO..%s
	 */
	public final static String NODO = "NODO";

	/**
	 * Message: No command given
	 */
	public final static String NOEXE = "NOEXE";

	/**
	 * Message: NEXT without FOR
	 */
	public final static String NOFOR = "NOFOR";

	/**
	 * Message: %s not permitted in this expression context
	 */
	public static final String NOFUNCS = "NOFUNCS";

	/**
	 * Message: Line numbers such as <string> cannot be used as labels in JBASIC.
	 */
	public final static String NOLINELBLS = "NOLINELBLS";
	
	/**
	 * Message: Member <null> does not exist
	 */
	public final static String NOMEMBER = "NOMEMBER";

	/**
	 * Message: Invalid or missing MESSAGE code
	 */
	public final static String NOMSG = "NOMSG";

	/**
	 * Message: Cannot use SET PASSWORD except as remote user
	 */
	public static final String NOPWD = "NOPWD";

	/**
	 * Message: Cannot RENUMBER because of invalid line number references
	 */
	public final static String NORENUM = "NORENUM";
	
	/**
	 * Message: File reference %s invalid or not open
	 */
	public static final String NOSUCHFID = "NOSUCHFID";

	/**
	 * Message: Label <null> not found
	 */
	public final static String NOSUCHLABEL = "NOSUCHLABEL";

	/**
	 * Message: No record member %s found.
	 */
	public static final String NOSUCHMEMBER = "NOSUCHMEMBER";

	/**
	 * Message: Method <null> cannot be found in CLASS hierarchy
	 */
	public final static String NOSUCHMETHOD = "NOSUCHMETHOD";

	/**
	 * Message: Symbol table %s not found.
	 */
	public static final String NOTABLE = "NOTABLE";

	/**
	 * Message: Variable '<null>' is not an array variable
	 */
	public final static String NOTARRAY = "NOTARRAY";

	/**
	 * Message: Not a BINARY file
	 */

	public final static String NOTBINARY = "NOTBINARY";

	/**
	 * Message: Note:
	 */
	public final static String NOTE = "NOTE";

	/**
	 * Message: The name <null> is already in use
	 */
	public final static String NOTNEW = "NOTNEW";

	/**
	 * Message: Variable <null> is not a recordValue
	 */
	public final static String NOTRECORD = "NOTRECORD";

	/**
	 * Message: No user <name>
	 */
	public final static String NOUSER = "NOUSER";
	
	/**
	 * Message: Mismatched or missing parenthesis
	 */
	public final static String PAREN = "PAREN";
	
	/**
	 * Message: PROGRAM statement not first line of code in file '<null>'
	 */
	public final static String PGMNOTFIRST = "PGMNOTFIRST";

	/**
	 * Message: LIST requires a PROGRAM or ARRAY name
	 */
	public final static String PGMORARRAY = "PGMORARRAY";

	/**
	 * Message: Program <null> cannot be modified, it is READONLY or linked
	 */
	public final static String PGMREADONLY = "PGMREADONLY";

	/**
	 * Message: Program <null> not found
	 */
	public final static String PROGRAM = "PROGRAM";

	/**
	 * Message: Program <null> is a protected program
	 */
	public final static String PROTECTED = "PROTECTED";

	/**
	 * Message: Variable <null> is read-only
	 */
	public final static String READONLY = "READONLY";

	/**
	 * Message: Invalid or missing starting line number or label
	 */
	public final static String RUNFROM = "RUNFROM";

	/**
	 * Message: Security setttings prevent operation
	 */
	public final static String SANDBOX = "SANDBOX";
	
	/**
	 * Message: Server administration error, <text>
	 */
	
	public final static String SERVER = "SERVER";
	
	/**
	 * Message: Syntax error, <null>
	 */
	public final static String SYNTAX = "SYNTAX";
	
	/**
	 * Message: Syntax error, expected token %s not found
	 */
	
	public final static String SYNEXPTOK = "SYNEXPTOK";

	/**
	 * Message: Syntax error, invalid token %s found
	 */
	
	public final static String SYNINVTOK = "SYNINVTOK";
	
	/**
	 * Message: SYSTEM status code <parm>
	 */
	public final static String SYSTEM = "*SYSTEM";
	
	/**
	 * Message: Invalid or unexpected THEN
	 */
	public final static String THEN = "THEN";

	/**
	 * Message: Error in THREAD command, %s
	 */
	public final static String THREAD = "THREAD";

	/**
	 * Message: CALL cannot return a value from a thread
	 */
	public final static String THREADRET = "THREADRET";
	
	/**
	 * Message: Too many arguments given
	 */
	public final static String TOOMANYARGS = "TOOMANYARGS";

	/**
	 * Message: Array element type mismatch
	 */
	public final static String TYPEMISMATCH = "TYPEMISMATCH";

	/**
	 * Message: Internal arithmetic stack underflow
	 */
	public final static String UNDERFLOW = "UNDERFLOW";

	/**
	 * Message: Unknown function <null>()
	 */
	public final static String UNKFUNC = "UNKFUNC";

	/**
	 * Message: Unknown THREAD %s
	 */
	public static final String UNKTHREAD = "UNKTHREAD";


	/**
	 * Message: Internal error; unimplemented byte code <null>
	 */
	public final static String UNIMPBYTECODE = "UNIMPBYTECODE";

	/**
	 * Message: Unknown breakpoint %s
	 */
	public static final String UNKBPT = "UNKBPT";
	
	/**
	 * Message: Unknown logical name %s
	 */
	public static final String UNKLN = "UNKLN";

	/**
	 * Message: Unknown or invalid name '<null>'
	 */
	public final static String UNKVAR = "UNKVAR";

	/**
	 * Message: Unrecognized verb <null>
	 */
	public final static String VERB = "VERB";

	/**
	 * Message: File not opened for %s mode
	 */
	public static final String WRONGMODE = "WRONGMODE";

	/**
	 * Message: Invalid XML syntax, %s
	 */
	public static final String XML = "XML";

	/**
	 * Message: Thread [] not found
	 */
	public static final String THREADNR = "NOTHREAD";

	/**
	 * Message: Duplicate field name [] specified
	 */
	public static final String DUPFIELD = "DUPFIELD";

	/**
	 * Message: Expected constant value not found
	 */
	public static final String EXPCONST = "EXPCONST";

	/**
	 * Message: Invalid program line number specification
	 */
	public static final String INVPGMLIN = "INVPGMLIN";

	/**
	 * Message: Unexpected token "[]"
	 */
	public static final String UNEXPTOK = "UNEXPTOK";

	/**
	 * Message: expected SIZE value not found
	 */
	public static final String EXPSIZE = "EXPSIZE";

	/**
	 * Message: invalid data type []
	 */
	public static final String INVDATTYP = "INVDATTYP";

	/**
	 * Message: expected variables not given
	 */
	public static final String EXPVARS = "EXPVARS";

	/**
	 * Message: Expected RECORD data value not found
	 */
	public static final String EXPREC = "EXPREC";

	/**
	 * Message: Expected record field [] not found
	 */
	public static final String EXPMEMBER = "EXPMEMBER";

	/**
	 * Message: Invalid file mode []
	 */
	public static final String INVFMODE = "INVFMODE";

	/**
	 * Message: Expected value not found
	 */
	public static final String EXPVALUE = "EXPVALUE";

	/**
	 * Message: unexpected function runtime exception, []
	 */
	public static final String FUNCFAULT = "FUNCFAULT";

	/**
	 * Message: Invalid INTEGER size []
	 */
	public static final String INTSIZE = "INTSIZE";

	/**
	 * Message: Invalid FLOAT size []
	 */
	public static final String FLTSIZE = "FLTSIZE";

	/**
	 * Message: "STOP statement executed; use RESUME to continue program execution."
	 */
	public static final String _STOPMSG = "_STOPMSG";

	/**
	 * Message: Type HELP for more information on how to use JBasic
	 */
	public final static String _GETHELP = "_GETHELP";

	/**
	 * Message: Ignoring unknown command line option 
	 */
	public static final String _UNKOPT = "_UNKOPT";

	/**
	 * Message:  No $MAIN program found.
	 */
	public static final String _NOMAIN = "_NOMAIN";

	/**
	 * Message: Loaded workspace
	 */
	public static final String _LOADEDWS = "_LOADEDWS";

	/**
	 * Message: Missing or mismatched brackets
	 */
	public static final String BRACKETS = "BRACKETS";

	/**
	 * Message: Uncompiled statement []
	 */
	public static final String UNCOMPILED = "UNCOMPILED";

	/**
	 * Message: Invalid date/time value []
	 */
	public static final String DATETIME = "DATETIME";

	/**
	 * Message: Invalid register array size []
	 */
	public static final String REGARRSIZ = "REGARRSIZ";

	/**
	 * Message: Invalid register number []
	 */
	public static final String REGNUM = "REGNUM";

	/**
	 * Message: Bad size value []
	 */
	public static final String BADSIZE = "BADSIZE";

	/**
	 * Message: "_REFSTR 1 without _REFSTR 0"
	 */
	public static final String REFSTR = "REFSTR";

	/**
	 * Message: "Invalid opcode argument []"
	 */
	public static final String INVOPARG = "INVOPARG";

	/**
	 * Message: Mismatched IF-ELSE-END IF at line []
	 */
	public static final String IFERR = "IFERR";

	/**
	 * Message: No active DO..LOOP or FOR..NEXT loop.
	 */
	public static final String NOLOOP = "NOLOOP";

	/**
	 * Message: Mismatched DO..LOOP statements
	 */
	public static final String MISMATDO = "MISMATDO";

	/**
	 * Message: Mismatched FOR..NEXT statements
	 */
	public static final String MISMATFOR = "MISMATFOR";

	/**
	 * Message: Server already runnings
	 */
	public static final String RUNNING = "RUNNING";

	/**
	 * Message: Invalid attempt to [] a Java object
	 */
	public static final String INVOBJOP = "INVOBJOP";

	/**
	 * Message: Invalid Java object field []
	 */
	public static final String INVOBJFLD = "INVOBJFLD";

	/**
	 * Message: Invalid attempt to convert to [] type
	 */
	public static final String INVCVT = "INVCVT";

	/**
	 * Message: Invalid or unknown object method []
	 */
	public static final String INVOBJMETH = "INVOBJMETH";

	/**
	 * Message: Unexpected object exception []
	 */
	public static final String OBJEXCEPT = "OBJEXCEPT";

	/**
	 * Message: Invalid class object []
	 */
	public static final String INVCLASS = "INVCLASS";

	/**
	 * Message: Invalid variable length specification for [] statement
	 */
	public static final String INVVARLEN = "INVVARLEN";

	/**
	 * Message: Expected TYPE name, found []
	 */
	public static final String EXPTYPE = "EXPTYPE";

	/**
	 * Message: Expected FOR index variable not given
	 */
	public static final String EXPNEXT = "EXPNEXT";

	/**
	 * Message: Invalid operation when running with debugger
	 */
	public static final String INVDBGOP = "INVDBGOP";

	/**
	 * Message: End of DATA
	 */
	public static final String EOD = "EOD";

	/**
	 * Message: There is no current program
	 */
	public static final String NOPGM = "NOPGM";

	/**
	 * Message: Invalid LOCK []
	 */
	public static final String INVLOCK = "INVLOCK";

	/**
	 * Message: Expected name not found
	 */
	public static final String EXPNAME = "EXPNAME";

	/**
	 * Message: Invalid or missing record syntax
	 */
	public static final String INVRECSYN = "INVRECSYN";

	/**
	 * Message: There are unsaved programs.  You must use the SAVE or the
	 *          SAVE WORKSPACE command to permanently store them to disk.
     *          Are you sure you want to QUIT [y/n] ?
	 *
	 */
	public static final String _SAVEPROMPT = "_SAVEPROMPT";

	/**
	 * Message: Invalid use of reserved word []
	 */
	public static final String RESERVED = "RESERVED";

	/**
	 * Message: At row []
	 */
	public static final String ATROW = "ATROW";

	/**
	 * Message: At line []
	 */
	public static final String ATLINE = "ATLINE";

	/**
	 * Message: There is no function code to execute for []
	 */
	public static final String NOFUNRUN = "NOFUNRUN";

	/**
	 * Message: Expected file ID not found.
	 */
	public static final String EXPFID = "EXPFID";

	/**
	 * Message: Bad URL "[]"
	 */
	public static final String BADURL = "BADURL";
	
	/**
	 * Message: Invalid array of records
	 */
	public static final String INVRECARRAY = "INVRECARRAY";

	/**
	 * Message: Expected value of type [] not found.
	 */
	public static final String WRONGTYPE = "WRONGTYPE";

	/**
	 * Message: Invalid SOCKET mode or name []
	 */
	public static final String SOCKETMODE = "SOCKETMODE";

	/**
	 * Message: Socket error, []
	 */
	public static final String SOCKET = "SOCKET";

	/**
	 * Message: Invalid use of ALL or HIDDEN keyword
	 */
	public static final String INVALL = "INVALL";

	/**
	 * Message: Invalid count []
	 */
	public static final String INVCOUNT = "INVCOUNT";

	/**
	 * Message: duplicate variable definition []
	 */
	public static final String DUPVAR = "DUPVAR";

	/**
	 * Message: invalid implicit conversion to []
	 */
	public static final String IMPCVT = "IMPCVT";

	/**
	 * Message: JBasic method invocations illegal in STATIC or LOCAL_SCOPE programs
	 */
	public static final String METHSCOPE = "METHSCOPE";

	/**
	 * Message: Successfully performed [] optimizations
	 */
	public static final String OPTIMIZED = "*OPTIMIZED";

	/**
	 * Message: Error in SQL statement
	 */
	public static final String SQL = "SQL";
	
	/**
	 * Message Inalid or missing keyword []
	 */
	public static final String SQLWORD = "SQLWORD";
	
	/**
	 * Message: Invalid or missing table []
	 */
	public static final String SQLTABLE = "SQLTABLE";
	
	/**
	 * Message: Unsupported feature
	 */
	public static final String SQLUNSUP = "SQLUNSUP";

	/**
	 * Message: Invalid or unknown column []
	 */
	public static final String SQLCOL = "SQLCOL";

	/**
	 * Message: Attempt to execute or fetch before successfull prepare
	 */
	public static final String SQLPREP = "SQLPREP";
	
	/**
	 * Message: Name [] already in use
	 */
	
	public static final String SQLDUPNAME = "SQLDUPNAME";
	
	/**
	 * Message: Missing comma
	 */
	public static final String COMMA = "COMMA";

	/**
	 * Message: Table name [] already in use
	 */
	public static final String SQLDUPTABLE = "SQLDUPTABLE";
	
	/**
	 * Message: Field name [] already in use
	 */
	public static final String SQLDUPFIELD = "SQLDUPFIELD";

	/**
	 * Message: Invalid TABLE []
	 */
	public static final String INVTABLE = "INVTABLE";

	/**
	 * Message: Invalid CATALOG []
	 */
	public static final String INVCATALOG = "INVCATALOG";

	/**
	 * Message: Mismatched quotes
	 */
	public static final String QUOTE = "QUOTE";

	/**
	 * Message: Error in tokenizer buffer positioning
	 */
	public static final String TOKBUFFER = "TOKBUFFER";
	
	/**
	 * Access function for string substitution parameter.
	 * 
	 * @param s
	 *            The string to be stored as the substitution value for this
	 *            message.
	 */

	public void setMessageParameter(final String s) {
		messageParameter = s;
	}


	/**
	 * Accessor function to return the current status block's substitution
	 * string value.
	 * 
	 * @return String containing text of substitution string, or null if there
	 *         is no substitution string value.
	 */
	public String getMessageParameter() {
		return messageParameter;
	}

	/**
	 * Return the nested Status() value for cascading error handling. 
	 * @return null if there is no nested (cascading) status
	 */
	public Status getNestedStatus() {
		return nestedStatus;
	}
	
	/**
	 * Access function to retrieve the message code for the given status
	 * block.
	 * 
	 * @return String containing the message code value.
	 */

	public String getCode() {
		return messageCode;
	}

	/**
	 * Test to see if the message code is a particular value. This hides from
	 * the user the issue of what kind of item the code is. We do a string
	 * comparison. This lets the user compare using the .equals() method without
	 * the default object pointer comparison becoming active.ing
	 * 
	 * @param s
	 *            The status to compare against.
	 * @return Flag indicating if the current Status is the same as 's'
	 */
	public boolean equals(final String s) {
		return messageCode.equalsIgnoreCase(s);
	}

	


	/**
	 * Constructor for a known status. Create a status object with a given
	 * string status value. The message code is stored in the object, and the
	 * object is marked as not printed.
	 * 
	 * @param i
	 *            The status value to assign.
	 */
	public Status(final String i) {
		fSuccess = ( i.charAt(0) == '*');
		messageCode = i;
		messageParameter = null;
	}

	/**
	 * Constructor for unknown status. Creates a status object with an assumed
	 * status of SUCCESS.
	 * 
	 */
	public Status() {
		fSuccess = true;
		messageCode = Status.SUCCESS;
		messageParameter = null;
	}

	/**
	 * Constructor for a known status with a string substitution. The
	 * substitution string is stored in the object for later use in reporting
	 * errors.
	 * 
	 * @param i
	 *            The error code
	 * @param s
	 *            The substitution string
	 */
	public Status(final String i, final String s) {
		fSuccess = ( i.charAt(0) == '*');
		messageCode = i;
		messageParameter = s;
	}

	/**
	 * Constructor for a known status with an integer substitution. The
	 * substitution value is stored in the object for later use in reporting
	 * errors.
	 * 
	 * @param i
	 *            The error code
	 * @param n
	 *            The substitution integer
	 */

	public Status(final String i, final int n) {
		fSuccess = ( i.charAt(0) == '*');
		messageCode = i;
		messageParameter = Integer.toString(n);
	}

	/**
	 * Create a Status object, with a nested Status object as the parameter.
	 * @param i The code for the primary message
	 * @param s The nested message linked to this primary message
	 */
	public Status(final String i, Status s ) {
		fSuccess = ( i.charAt(0) == '*');
		messageCode = i;
		nestedStatus = s;
	}
	
	/**
	 * Create a Status object, with a parameter and a nested Status object
	 * @param i The code for the primary message
	 * @param p The string parameter for the primary message
	 * @param s The nested message linked to this primary message
	 */

	public Status(final String i, String p, Status s ) {
		fSuccess = ( i.charAt(0) == '*');
		messageCode = i;
		messageParameter = p;
		nestedStatus = s;
	}
	
	/**
	 * Create a Status object, with a RECORD as the parameter, which must
	 * be a representation of a Status object with a CODE and PARM member.
	 * @param record a Value containing the description of the information
	 * to be converted to a Status object
	 */
	public Status(Value record ) {
		if( record.getType() == Value.RECORD ) {
			messageCode = record.getElement("CODE").getString();
			Value v = record.getElement("PARM");
			if( v != null ) {
				if( v.getType() == Value.RECORD) 
					nestedStatus = new Status(v);
				else
					messageParameter = v.getString();
			}
			v = record.getElement("SUBSTATUS");
			if( v != null ) {
				nestedStatus = new Status(v);
			}
		}
	}
	
	/**
	 * Pseudo-factor that generates a new Status that contains the
	 * current Status object, wrapped in a new Code value.
	 * @param code The code to associate with the new Status object
	 * @return a new Status object that contains the current one.
	 */
	public Status nest(String code) {
		return new Status(code, this);
	}
	
	/**
	 * Return a boolean indicating if the status code is an error nor not. Codes
	 * that are considered non-failures start with an "*" character. This was
	 * processed when the status object was created, because it is more
	 * efficient to do the string test once, and save the result. So we just
	 * return the result.
	 * 
	 * @return Boolean flag indicating if the Status block reflects a successful
	 *         operation or not.
	 */

	public boolean success() {
		return fSuccess;
	}

	/**
	 * Return a boolean indicating if the status code is an error nor not.
	 * 
	 * @return Boolean flag indicating if the Status block reflects a failed
	 *         operation.
	 */

	public boolean failed() {
		return !fSuccess;
	}


	/**
	 * Return the message text from the status object. Fetches the message text
	 * that corresponds to the status code. These have been previously loaded
	 * by a MESSAGE statement, and typically are initialized in MESSAGES.JBASIC
	 * which is executed as part of initialization.
	 * <p>
	 * The message code is derived from the current Status object, and then is
	 * explicitly qualified by the currently active language code, which is
	 * stored in SYS$LANGUAGE.  The resulting language code (EN=English, 
	 * FR=French, ES=Spanish, etc.) is appended to the message key to lookup 
	 * the value in the message table.  If the given language does not exist, 
	 * the lookup is repeated using (EN) as the code, on the assumption that 
	 * there will probably be an English version of every message
	 * code even if there isn't necessarily one in each localization.  
	 * 
	 * @param session
	 *            The JBasic object that contains this session
	 * @return String containing the text message matching the status code
	 */
	public String getMessage(final JBasic session) {

		if( session.messageManager == null )
			return messageCode;
		
		/*
		 * Use the language code to lookup the message text based on the
		 * code and the language as a composite key.
		 */
		String language = session.getString("SYS$LANGUAGE");
		final String langKey = "(" + language.toUpperCase() + ")";
		String msgKey = messageCode + langKey;
		Message messageEnvelope = session.messageManager.get(msgKey);

		/*
		 * If the message was not found given the user's default language
		 * code, look it up again using EN for English as the default code;
		 * all messages are likely to have an English translation.
		 */
		if (messageEnvelope == null) {
			msgKey = messageCode + "(EN)";
			messageEnvelope = session.messageManager.get(msgKey);
		}

		/*
		 * If the result is still nothing, then this isn't a message for which
		 * there is an existing text translation, and we use the message code
		 * as the message text.  However, if there was a text translation, get
		 * it out of the message object now.
		 */
		String messageText;
		if (messageEnvelope == null) 
			messageText = messageCode + ( messageParameter == null? "" : ", " + messageParameter);
		else 
			messageText = messageEnvelope.mText;

		/*
		 * Is there a substitution operator in the message text?  If not, then
		 * we can just return what we have right now.
		 */
		int subPosition = messageText.indexOf("[]");
		if (subPosition >= 0 ) {

			/*
			 * There's a substitution operator so let's construct a new string
			 * containing the full text of the message.
			 */

			StringBuffer newString = new StringBuffer();

			/*
			 * If there is text before the substitution operator, include it 
			 */
			if( subPosition > 0 )
				newString.append(messageText.substring(0, subPosition));

			/*
			 * Include the substitution parameter.  If it's null, then put in
			 * a reasonable substitution string value.
			 */

			newString.append(messageParameter == null ? "" : messageParameter);

			/* 
			 * If there is text after the operator, include it 
			 */

			if( subPosition < messageText.length()+2)
				newString.append(messageText.substring(subPosition+2));

			/* 
			 * Convert the buffer back to a conventional string. 
			 */
			messageText = newString.toString();
		}

		if( nestedStatus != null ) {
			StringBuffer nestedMessage = new StringBuffer(messageText);
			nestedMessage.append(JBasic.newLine);
			nestedMessage.append("- ");
			nestedMessage.append(nestedStatus.getMessage(session));
			messageText = nestedMessage.toString();
		}
		
		return messageText;
	}



	/**
	 * Method to print the status value. This creates the correct prefix for the
	 * message (Error or Note, etc.) It also suppresses printing the message if
	 * it has already been printed.
	 * 
	 * @param session
	 *            The JBasic runtime environment handle
	 */

	public void print(final JBasic session) {
		if (!fAlreadyBeenPrinted) {
			
			StringBuffer prefix = new StringBuffer();
			fAlreadyBeenPrinted = true;
			
			if (failed())
				prefix.append(new Status(Status.ERROR).getMessage(session));
			else
				prefix.append(new Status(Status.NOTE).getMessage(session));
			prefix.append(": ");
			
			if( program != null && !program.startsWith(JBasic.VERB) ) {
				prefix.append("In program ");
				prefix.append(program);
				if( lineNumber > 0 ) {
					prefix.append(" at line ");
					prefix.append(Integer.toString(lineNumber));
				}
				prefix.append("; ");
			}
			session.stdout.println(prefix + getMessage(session));
		}
	}

	/**
	 * Print the text of the message to the console IFF the Status object
	 * represents an error.
	 * 
	 * @param session
	 *            The JBasic runtime environment handle
	 * @return true if an error message was printed by this call.
	 */
	public boolean printError(final JBasic session) {
		boolean retVal = false;
		if (failed()) {
			print(session);
			retVal = true;
		}
		fAlreadyBeenPrinted = true;
		return retVal;
	}


	/**
	 * Support function for printing Status objects without control
	 * of JBasic, such as in external programs or in a debugger.
	 * @return a String representation of the object
	 */
	public String toString() {
		StringBuffer msg = new StringBuffer();
		msg.append("Status(" + messageCode);
		if( messageParameter != null )
			msg.append( ", \"" + this.messageParameter + "\"");
		if( this.program != null )
			msg.append( ", in program " + program);
		if( this.lineNumber > 0 )
			msg.append( ", at line " + lineNumber);
		msg.append( ")");
		return msg.toString();
	}
	


	/**
	 * Return a flag indicating if this message has been printed at least once
	 * or not - used by code that is "percolating" an error and doesn't want to
	 * print it more than once.
	 * 
	 * @return true if a previous print() or printError() has been called on
	 *         this Status object.
	 */
	public boolean printed() {
		return fAlreadyBeenPrinted;
	}

	/**
	 * Set the location where the exception was triggered; if it is known at runtime.
	 * @param name The name of the executing program
	 * @param lastLineNumber The last line number executed.
	 */
	public void setWhere(String name, int lastLineNumber) {
		this.lineNumber = lastLineNumber;
		this.program = name;
		
	}

	/**
	 * Get the program name associated with the error.
	 * @return the name of the program that triggered the runtime error, else null.
	 */
	public String getProgram() {
		return program;
	}
	
	/**
	 * Return the line number at which the error occurred, else zero if unknown.
	 * @return line number of the error
	 */
	public int getLine() {
		return lineNumber;
	}

	/**
	 * Set the flag indicating if the status has been printed already.  This is
	 * primarily used when a JBasicException is converted back to a Status
	 * object, and the flag must be set correctly.
	 * @param alreadyPrinted true if the status is to be marked as already
	 * printed.
	 */
	public void setPrinted(boolean alreadyPrinted) {
		fAlreadyBeenPrinted = alreadyPrinted;
	}

	/**
	 * Set the nested Status value for the given Status object.
	 * @param status The object to embed in the current object.
	 */
	public void setNestedStatus(Status status) {
		nestedStatus = status;
	}
}
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
import org.fernwood.jbasic.compiler.CompileContext;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.statements.SortStatement;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * This is the container class for all intrinsic functions. They are grouped as
 * subclasses of this class in part to support the <code><b>SHOW FUNCTIONS</b></code>
 *  command which
 * must be able to enumerate over the function list.
 * 
 * @author cole
 * 
 */
public class JBasicFunction {

	static String[] functionNameList = new String[] { 
			"ABS",	 			/* Absolute value */
			"ARCCOS",	 		/* Arc cosine */
			"ARCSIN", 			/* Arc sine */
			"ARCTAN",			/* Arc tangent */
			"ARRAY", 			/* Cast arguments as an array */
			"ASCII", 			/* Convert character to ASCII */
			"BASENAME", 		/* base name of file path */
			"BINARY", 			/* Encode numeric argument in RADIX(2) */
			"BOOLEAN", 			/* Cast argument to boolean */
			"BYNAME",			/* Parse named values from a buffer */
			"BYTECODE",			/* Return bytecode data for a program */
			"CALL",				/* Call a function indirectly by name */
			"CEILING",			/* Return next highest integer value */ 
			"CHARACTER", 		/* Convert integer to character */
			"CIPHER",			/* Encrypt a string */
			"CLASS",			/* Create a Java class by name */
			"CLASSOF",			/* Return the Java class of the parameter */
			"COMPILE",			/* Compile a statement to bytecode */
			"COS", 				/* Cosine */
			"CSV",				/* Format comma-separated value strings */
			"DATE", 			/* Convert float to text date string */
			"DECIMAL", 			/* Convert string of a radix into decimal integer */
			"DECIPHER",			/* Convert encrypted string back to original string */
			"DOUBLE", 			/* Convert argument to double float */
			"EOD", 				/* Test if READ is at end of DATA statements */
			"EOF", 				/* Test if file is at end-of-file */
			"EXISTS",			/* Test to see if file exists */
			"EXP", 				/* log exponent */
			"EXPRESSION", 		/* Calculate string as expression, return result */
			"EXTENSION",		/* Extension of file name */
			"FILEPARSE", 		/* Break file name into parts */
			"FILES", 			/* Return list of files from file system */
			"FILETYPE", 		/* Describe file system attributes of a file */
			"FILETYPES", 		/* Describe file type info for files in a directory */
			"FLOOR", 			/* Lowest integer value */
			"FORMAT", 			/* Convert value based on format */
			"GETPOS", 			/* Get current position of binary file */
			"HEXADECIMAL", 		/* Convert value to/from hex */
			"INTEGER",			/* Convert argument to INTEGER */
			"ISOBJECT", 		/* Determine if argument is an Object */
			"LEFT", 			/* Left-most characters of a string */
			"LENGTH", 			/* Length of string or array */
			"LOADED",			/* Determine if a program is loaded in memory */
			"LOCATE", 			/* Search for substring in argument */
			"LOWERCASE", 		/* Convert argument to lower case string */
			"MATCHES", 			/* Pattern matching */
			"MAX", 				/* Maximum value from argument list */
			"MEMBER", 			/* Extract a single member from a record */
			"MEMBERS",			/* List of member names of a record */
			"MEMORY", 			/* Available runtime memory */
			"MESSAGE", 			/* Text of a given message in current language */
			"MESSAGES", 		/* List of all message names */
			"METHODS",			/* List of methods known to Java object */
			"MIN", 				/* Smallest value from argument list */
			"MKDIR",            /* Create a directory *
			"MOD", 				/* Modulo remainder */
			"NAN", 				/* Returns true if argument is a NaN */
			"NEW",				/* Create new instance of a Class */
			"NUMBER", 			/* Convert string to a numeric data type */
			"OCTAL", 			/* Convert argument to/from octal */
			"OBJECT", 			/* Descriptive info about an Object */
			"OPENFILES", 		/* List of identifiers of currently open files */
			"PARSE", 			/* Return nth element of a tokenized string */
			"PASSWORD", 		/* One-way hash of a password */
			"PATHNAME",			/* Pathname part of a file name */
			"PERMISSION", 		/* Indicate if user has a permission */
			"PROGRAM", 			/* Descriptive info about a program */
			"PROPERTIES", 		/* List of Java property names */
			"PROPERTY", 		/* Value of a Java property */
			"QUOTE", 			/* Enclose argument in quotes as string */
			"RADIX", 			/* Convert number to/from any radix */
			"RANDOM", 			/* Return random number */
			"RANDOMLIST", 		/* Generate random list of integers */
			"RECORD",
			"REPEAT", 			/* Create repeated string value */
			"REPLACE", 			/* Replace text in a string */
			"RIGHT", 			/* Right characters of string */
			"ROUND", 			/* Round floating point number to nearest integer */
			"SECONDS", 			/* Seconds since JBasic started */
			"SIN",				/* Sine */
			"SIZEOF", 			/* Size of argument in bytes int BINARY file */
			"SORT",  			/* Sort array or arguments */
			"SPELL", 			/* Convert number to text words */
			"SQRT", 			/* Square root */
			"STRING", 			/* Convert argument to string data type*/
			"SUBSTR",			/* Substring */
			"SUBSTRING",		/* Substring */
			"SUM", 				/* Sum of numeric arguments */
			"SYMBOL",			/* Descriptive info about a symbol */
			"SYMBOLS", 			/* List of symbols in a table */
			"TABLES", 			/* List of symbol table names*/
			"TAN", 				/* Tangent */
			"THREAD", 			/* Info about a named thread */
			"THREADS",  		/* Array of names of active threads */
			"TIMECODE", 		/* Generate current time code */
			"TOKENIZE",  		/* Break string into token structures */
			"TOTAL", 			/* Calculate sum of arguments */
			"TRIM", 			/* Trim leading and trailing whitespace */
			"TYPE", 			/* Return type of argument as string */
			"TYPECHK",			/* Test a value against a type descriptor */
			"UNIQUENAME", 		/* Return a session-wide unique identifier */
			"UNIQUENUMBER",		/* Return a session-wide unique number */
			"UPPERCASE", 		/* Convert argument to uppercase string */
			"URL",				/* Parse a URL */
			"XML", 				/* Convert a value to an XML string */
			"XMLCOMMENT", 		/* Generate an XML comment item */
			"XMLPARSE"			/* Parse an XML string for a value */
			};

	/**
	 * Return a list of the function names available in the language.  This
	 * version of the operation does this using a static list of names.  See
	 * the method classList() for an attempt to find this list by inspection
	 * which is currently not working.
	 * @return a Value containing an array of strings with the names of the
	 * builtin functions.
	 */
	public static Value functionList() {


		Value v = new Value(Value.ARRAY, null);

		for (int ix = 0; ix < functionNameList.length; ix++) {
			v.setElement(new Value(functionNameList[ix]), ix + 1);
		}

		SortStatement.sortArray(v);
		
		return v;
	}

	/**
	 * Runtime execution of the function via _CALLF.  This is not a valid
	 * function, but really a superclass.  If they do call the run() method
	 * because of a function invocation, just throw an error.
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException an attempt to use the word JBASIC as a function
	 * name results in an UNKFUNC error.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		throw new JBasicException(Status.UNKFUNC, "JBASIC");
		
	}
	
	/**
	 * Stub, this will never be called directly.  This is used by subclasses to declare
	 * the compile() method that compiles a function invocation.  If a function cannot
	 * be compiled (but is just called at runtime) then this returns the UNKFUNC status.
	 * @param work the block describing the function name and arguments.
	 * @return this always returns the UNKFUNC status.
	 * @throws JBasicException Attempt to call the superclass for the compile()
	 * method for a function.
	 */
	public Status compile(CompileContext work) throws JBasicException  {
		if( work == null )
			throw new JBasicException(Status.FAULT, "missing work unit");
		return new Status(Status.UNKFUNC, work.name);
	}

	/**
	 * Delete the arguments from a compiler work unit.  This assumes that
	 * the function compiler has done something with the arguments such 
	 * that they no longer need to be in the bytecode data.  This works
	 * for virtually any section of code that defines arguments, based
	 * on the argPosition field that defines where the argument list code
	 * generation begins.
	 * @param work the CompileContext that defines the compilation unit.
	 * 
	 */
	public void deleteArguments(CompileContext work ) {
		int bc = work.byteCode.size() - work.argPosition;
		for( int idx = 0; idx < bc; idx++ )
			work.byteCode.remove(work.argPosition);
	}
	
	/**
	 * For a compilation work unit, determine what the constant argument
	 * data is that is defined in the generated code, and extract the
	 * argument data into an array of Values.<p>
	 * This will not work if performed on a work unit with non-constant
	 * arguments.
	 * @param work the CompileContext that defines the current compilation work.
	 * @return an array of Value objects for each of the constant arguments.
	 * 
	 * @throws JBasicException Attempt to fetch arguments from a codestream
	 * that are non-constant and cannot be derived at compile time.
	 */
	public Value [] getArguments(CompileContext work) throws JBasicException {
		
		if( !work.constantArguments )
			throw new JBasicException(Status.FAULT, "read non-constant arguments from bytecode");
		
		/*
		 * Create a new bytecode area to use temporarily.
		 */
		ByteCode temp = new ByteCode(work.byteCode.getSession(), null);
		
		/*
		 * Copy all the previously generated bytecode that sets up the
		 * constant arguments into this new bytecode area.  Count them
		 * while we're at it for easy use later.
		 */
		int byteCount = work.byteCode.size() - work.argPosition;
		for( int idx = 0; idx < byteCount; idx++ ) {
			temp.add(work.byteCode.getInstruction(work.argPosition + idx));
		}
		
		/*
		 * Run the summation operation code now.  If it failed, then error
		 * out.  Otherwise, get the resulting constant summation value.
		 */
		Status s = temp.run(work.byteCode.getSession().globals(), 0);
		if( s.failed())
			throw new JBasicException(s);
		
		Value result[] = new Value[work.argumentCount];
		
		for( int idx = 0; idx < work.argumentCount;idx ++ ) 
			result[(work.argumentCount - idx) - 1] = temp.getResult();
		
		return result;

	}

	/**
	 * Indicate if a keyword is a known function name. These are treated
	 * as reserved words by the tokenizer for formatting purposes.
	 * @param k the name to test
	 * @return true if the name is on the list of pre-defined functions.
	 */
	static public boolean isFunctionName( final String k ) {
		boolean result = false;
		for( int idx = 0; idx < functionNameList.length; idx++)
			if( k.equalsIgnoreCase(functionNameList[idx]))
				return true;
		return result;
		
	}
}

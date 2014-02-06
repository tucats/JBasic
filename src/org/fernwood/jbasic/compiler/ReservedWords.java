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
 * Created on Apr 17, 2007 by tom
 *
 */
package org.fernwood.jbasic.compiler;

/**
 * A static class that handles reserved word checking.  This is used for
 * verifying that a label on a statement is not a reserved word, which would
 * cause no end of problems.
 * 
 * @author tom
 * @version version 1.1  June 2007
 *
 */
public class ReservedWords {

	/* Words that are verbs */
	
	static String verbList[] = { 
		
		/* VERBS */
		"ASM", "BREAK", "CALL", "CHAIN", "CLASS", "CLEAR", "CLOSE", "COMMON",
		"COMPILE", "DATA", "DEBUG", "DELETE", "DEFFN", "DIM", "DO", "ELSE", 
		"END", "EXECUTE", "FOR", "FUNCTION", "GET", "GOSUB", "GOTO", "IF", 
		"INPUT", "KILL", "LET", "LINE", "LINK", "LIST", "LOAD", "LOCK", 
		"MESSAGE", "MID$", "NEW", "NEXT", "OLD", "ON", "OPEN", "PRINT", 
		"PROGRAM", "PROTECT", "PUT", "QUIT", "RANDOMIZE", "READ", "RECORD", 
		"REM", "RENUMBER", "RESUME", "RETURN", "REWIND", "RUN", "SAVE", "SEEK", 
		"SET", "SHELL", "SHOW", "SIGNAL", "SLEEP", "SORT", "STEP", "STOP", 
		"SUB", "SYSTEM", "TABLE", "THEN",  "THREAD", "TIME", "TRACE", "UNLINK", "UNLOCK", 
		"UNTIL", "VERB", "WHILE", "WINDOW", "INFORMAT",
		"ARRAY", "INTEGER", "STRING", "DOUBLE", "BOOLEAN", "FIELD", 
		"ADD", "SUBTRACT", "MULTIPLY", "DIVIDE" };

	/* Additional keywords in the language syntax */

	static String wordList[] = {
	
		"ALL", "AS", "BY", "FILE", "RETURNS", "SUPERCLASS", "TO", "USING",
		"AND", "OR", "NOT", "MAX", "MIN", "FROM", "QUEUE", "PIPE", "OUTPUT",
		"DATABASE", "SKIP", "WHERE"
	 };
	
	/**
	 * Determine if a given keyword is on the reserved word list.
	 * @param keyword The keyword to test, which must already be uppercased.
	 * @return true if the keyword is considered a reserved word in the 
	 * language.
	 */
	public static boolean isReserved(String keyword) {
		if( isVerb( keyword ))
			return true;
		
		int count = wordList.length;
		for( int ix = 0; ix < count; ix++ )
			if( wordList[ix].equals(keyword))
				return true;
		return false;
	}
	
	/**
	 * Determine if a given keyword is on the reserved word list.
	 * @param keyword The keyword to test, which must already be uppercased.
	 * @return true if the keyword is considered a reserved word in the 
	 * language.
	 */
	public static boolean isVerb(String keyword) {
		int count = verbList.length;
		for( int ix = 0; ix < count; ix++ )
			if( verbList[ix].equals(keyword))
				return true;
		return false;
	}
	
	
}

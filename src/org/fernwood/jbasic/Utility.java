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
 * Created on Jun 23, 2007 by tom
 *
 */
package org.fernwood.jbasic;

import java.util.Hashtable;
import java.util.Iterator;

import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * This class manages various utility functions for JBasic, mostly as static
 * functions.
 * 
 * @author Tom Cole
 */
public class Utility {

	/**
	 * Static data structures.
	 */

	static Hashtable typeCache;
	
	
	/**
	 * Given a string and a session, resolve any macro references in the string.
	 * The macro references are identified by "macro quote" characters which are
	 * found in the global symbol <code>SYS$MACRO_QUOTES</code>.  This must be a string 
	 * containing four characters; the first two are the lead-in characters and
	 * the last two are the lead-out characters.
	 * <p>
	 * The routine does no substitution operations if either there is no active
	 * session (so no substitutions string table is available) or if the quote
	 * character definition is empty.
	 * <p>
	 * The user can get information about the macro substitution operations done
	 * (and any error messages) by setting the logging level to 3 (DEBUG).
	 * <p>
	 * 
	 * @param env the JBasic session where macro variables can be resolved
	 * @param line the text that is being processed.
	 * @return the new string with all substitutions completed.
	 */
	public static String resolveMacros(JBasic env, final String line) {
		
		String buffer;
		String originalLine = line;
		StringBuffer workingBuffer = new StringBuffer();
		StringBuffer substitutionBuffer = new StringBuffer();
		boolean inExpression = false;
		boolean didSubstitute = false;
		int subError = 0;

		/*
		 * Get the quote characters.  If this array of strings is 
		 * missing or mal-formed, then we assume it is meant to 
		 * disable quoting.
		 * 
		 * Therefore, to disable MACRO substitution entirely, just
		 * set the string SYS$MACRO_QUOTES="".
		 * 
		 */
		Value mQuotes = env.globals().localReference("SYS$MACRO_QUOTES");
		if( mQuotes == null || mQuotes.getType() != Value.ARRAY || mQuotes.size() != 2 )
			return line;

		/*
		 * Break out the two character pairs for the quotes.
		 */

		String quoteStart = mQuotes.getString(1);
		String quoteEnd = mQuotes.getString(2);

		/*
		 * Scan over the string...
		 */
		for( int i = 0; i < line.length(); i++ ) {

			/*
			 * If this is the start of a substitution operation, we will find the
			 * quote start characters.
			 */

			if( i < line.length()-(quoteStart.length()-1)) {
				String t = line.substring(i,i+quoteStart.length());
				if( t.equals(quoteStart)) {
					inExpression = true;
					didSubstitute = true;
					i = i + quoteStart.length()-1;
					continue;
				}
			}

			/*
			 * If this is the end of the substitution operation, we'll find the
			 * quote end characters.  Time to tokenize the expression string
			 * and do an expression evaluation on it.  The result is copied
			 * to the output buffer if no error is found.
			 */
			 
			if( i < line.length()-(quoteEnd.length()-1)) {
				String t = line.substring(i,i+quoteEnd.length());
				if( t.equals(quoteEnd)) {
					inExpression = false;
					Expression exp = new Expression(env);
					Tokenizer tok = new Tokenizer(substitutionBuffer.toString());
					Value v = exp.evaluate(tok,env.macroTable);
					if( v != null )
						workingBuffer.append(v.getString());
					else {
						subError++;
						JBasic.log.debug("MACRO ERROR(" + exp.status.getMessage(env) + ")");
					}
					substitutionBuffer = new StringBuffer();
					i = i + quoteEnd.length()-1;
					continue;
				}
			}
			/*
			 * If we are in the middle of a sub expression, copy the characters to 
			 * the expression buffer.
			 */
			 if( inExpression )
				substitutionBuffer.append(line.charAt(i));
			/*
			 * Of if we are not in a substitution operation, just copy the input
			 * character to the output string buffer.
			 */
			else
				workingBuffer.append(line.charAt(i));
		}

		/*
		 * If we got done with the string and we had an outstanding exprssion
		 * result buffer, copy it to the output. This usually means a dangling
		 * end quote mark.
		 */
		if( substitutionBuffer.length() > 0 )
			workingBuffer.append(substitutionBuffer);

		/*
		 * If we had a dangling substitution operator, then we need to create
		 * a nice error message about it, using the current macro quote chars.
		 */
		if( inExpression ) {
			StringBuffer t = new StringBuffer();
			t.append(quoteStart);
			t.append("macro");
			t.append(quoteEnd);
			JBasic.log.debug("MACRO ERROR(mismatched " + t + " operators)");
			subError++;
			t=null;
		}

		/*
		 * Turn the string buffer back into a string to return to the caller.
		 * If we did any substitutions and logging is enabled, take the time
		 * to format a nice message indicating what the resolution looks like
		 * and if there were any errors.
		 */
		buffer = workingBuffer.toString();
		if( didSubstitute && JBasic.log.isLoggingLevel(3))
			JBasic.log.debug("MACRO SOURCE(" + originalLine + ") RESOLVED TO("
					+ buffer + 
					(subError > 0? ") WITH " + Integer.toString(subError) + "" +
							" ERROR" + 
							(subError == 1? "":"S"): ")"));

		return buffer;
	}

	/**
	 * Pad a string to the given length, with either left or right justification.
	 * @param source The input string to pad.
	 * @param length The resulting length.  A positive number means the
	 * padding is added to the right side of the string, a negative number
	 * means the padding is added to the left side of the string.
	 * @return a String value is the padding on the correct side.  IF the
	 * input string is already greater than the abs(length) then the string
	 * is returned unchanged.
	 */
	public static String pad( String source, int length ) {

		/*
		 * See if the requested length is less than the source length
		 * itself. If so, we are done; pad() won't truncate a string.
		 */
		int padSize = Math.abs(length) - source.length();
		if( padSize <= 0 )
			return source;

		/*
		 * Depending on whether the spaces go on the left or 
		 * right, return the source with an appropriate number of
		 * spaces on either the left or right side.
		 */

		if( length < 0 )
			return Utility.spaces(padSize) + source;

		return source + Utility.spaces(padSize);
	}

	/**
	 * Return a string of blanks of the given length.
	 * @param length The length of the string to return, which must be
	 * greater than zero.
	 * @return A String containing 'length' spaces.
	 */
	public static String spaces(int length) {

		StringBuffer result = new StringBuffer("                                ");
		if( length < 30 )
			return result.substring(0, length);

		for( int ix = result.length(); ix < length; ix++ )
			result.append(' ');

		return result.toString();
	}

	/**
	 * @param seconds an Int indicating the number of seconds that is to
	 * be formatted
	 * @return a String containing the formatted string value.
	 */
	public static String formatElapsedTime( int seconds ) {

		String sPlural = ( seconds == 1 ? "" : "s");

		if( seconds < 60 )
			return Integer.toString(seconds) + " second" + sPlural;

		int s = (seconds % 60);
		sPlural = ( seconds == 1 ? "" : "s");
		int m = seconds / 60;
		String mPlural = ( m == 1 ? "" : "s");

		return Integer.toString( m ) + " minute" + mPlural +
		( s > 0 ? (", " + Integer.toString(s) + " second" + sPlural) : "" );	

	}

	/*
	 * Various file name services...
	 */

	/**
	 * Given a filename, add an extension if needed to the filename.
	 * @param fn the file name string
	 * @return the filename with an extension added if needed.
	 */
	public static String normalize( String fn ) {
		if( Utility.hasExtension(fn))
			return fn;
		return fn + JBasic.DEFAULTEXTENSION;
	}
	/**
	 * Return the extension part of a full or partial path name.
	 * @param fn the file name string
	 * @return the extension (minus the leading ".") or an empty
	 * string if there is no extension.
	 */
	public static String extension( String fn ) {

		String separator = System.getProperty("file.separator");
		int sepLen = separator.length();

		int pos = fn.length()-sepLen;

		for( ; pos >= 0; pos-- ) {
			String testChar = fn.substring(pos,pos+sepLen);
			if( testChar.equals(separator) || testChar.equals(":"))
				return "";
			if( testChar.equals("."))
				return fn.substring(pos);
		}

		return "";
	}
	/**
	 * Given a full or partial path name, locate the "base name" from
	 * the name.  This means removing all absolute or relative path 
	 * name prefixes and any extension suffix.
	 * @param fn the String filename to scan
	 * @return the base name element of the string.  If the path name
	 * does not include a basename element (for example, consists only of a
	 * directory specification) then an empty string is returned.
	 */
	public static String baseName( String fn ) {

		/*
		 * Start at the end of the string and search for a path
		 * separator character.
		 */

		String separator = System.getProperty("file.separator");

		int sepLen = separator.length();
		int startPos = fn.length() - sepLen;
		if( startPos < 0 )
			return "";

		int pos;
		for( pos = startPos; pos >= 0; pos--) {
			String testChar = fn.substring(pos, pos+sepLen);
			if( testChar.equals(separator))
				break;
			if( testChar.equals(":"))
				break;
		}

		if( pos == startPos )
			return "";

		/*
		 * Make a new copy of the part of the string that has only
		 * the filename.  If we never found a separator, adjust to 
		 * the start of the string. If we did find a separator, then
		 * don't include it in the new filename string.
		 */

		if( pos < 0 )
			pos = 0;
		else
			pos += 1;

		String fileName = fn.substring(pos);

		/*
		 * Strip off the last part of the filename (the "extension")
		 * if there is one, identified by the "dot".
		 */

		for( pos = fileName.length()-1; pos >= 0; pos--)
			if( fileName.charAt(pos) == '.')
				break;
		if( pos < 0)
			pos = fileName.length();

		return fileName.substring(0,pos);
	}

	/**
	 * Given a full or partial path name, locate the pathname from
	 * the name. 
	 * @param fn the String filename to scan
	 * @return the path name element of the string.
	 */
	public static String pathName( String fn ) {

		/*
		 * Start at the end of the string and search for a path
		 * separator character.
		 */

		String separator = System.getProperty("file.separator");

		int sepLen = separator.length();
		int startPos = fn.length() - sepLen;

		int pos;
		for( pos = startPos; pos >= 0; pos--) {
			String testChar = fn.substring(pos, pos+sepLen);
			if( testChar.equals(separator))
				return fn.substring(0,pos+sepLen);
			if( testChar.equals(":"))
				return fn.substring(0,pos+1);
		}

		return "";

	}
	/**
	 * Determine if a given file name already includes an extension
	 * in the filename part.
	 * @param fn the file name to check
	 * @return true if there is already an extension
	 */
	public static boolean hasExtension( String fn ) {

		String separator = System.getProperty("file.separator");
		int sepLen = separator.length();

		int pos = fn.length() - sepLen;

		for( ; pos >= 0; pos-- ) {
			String testChar = fn.substring(pos,pos+sepLen);
			if( testChar.equals(separator))
				return false;
			if( testChar.equals("."))
				return true;
		}

		return false;
	}

	/**
	 * Wrap a string in quotation marks.
	 * @param s the string value to be quoted
	 * @return the value in quotation marks.
	 */
	public static String quote(String s) {
		return "\"" + s + "\"";
	}

	/**
	 * Convert a string to mixed case; i.e. capitalized first letter
	 * and lower-case remainder.  For example,
	 * <p>
	 * <table  border="1">
	 * <tr ><td align="center"><strong>&nbsp;Source&nbsp;</strong></td>
	 * 	<td align="center"><strong>&nbsp;Result&nbsp;</strong></td></tr>
	 * <tr><td align="center">tom</td><td align="center">Tom</td><tr>
	 * <tr><td align="center">TOM</td><td align="center">Tom</td><tr>
	 * <tr><td align="center">tOm</td><td align="center">Tom</td><tr>
	 * </table>
	 * @param s the string to convert
	 * @return a new String in mixed case.  If the argument was null or an
	 * empty string, an empty string is returned.
	 */
	public static String mixedCase( String s ) {
		StringBuffer r = new StringBuffer();
		if( s != null && s.length() > 0 ) {
			r.append(s.substring(0,1).toUpperCase());
			if( s.length() > 1 )
				r.append(s.substring(1).toLowerCase());
		}
		return r.toString();
	}
	
	
	/**
	 * Given a values and a declaration of a constant, determine if the
	 * value matches the types of the constant, including
	 * a deep traversal of nested array elements or record members.
	 * <p>
	 * 
	 * This can be used for complex type validation, such as determining if
	 * a value is really an array of two strings by comparing the value to a
	 * constant declaration of ["",""] or something similar.
	 * @param session the session, required for expression evaluation.
	 * @param base the base value to compare
	 * @param descriptor a string describing the value to compare to the base
	 * @return true if all types match, else false.
	 */
	public static boolean validateTypes( JBasic session, Value base, String descriptor ) {
		
		return validateTypes(base, protoType(session, descriptor));
	}

	/**
	 * Given a type descriptor string, create an empty data object of the
	 * given type.  This can be used to create a template object, or to
	 * create an object for use with the validateTypes() operation.
	 * 
	 * @param session the containing session, needed for type compilation
	 * @param descriptor a string containing the descriptor data.
	 * @return the Value containing the protype data.
	 */
	@SuppressWarnings("unchecked") 
	public static Value protoType(JBasic session, String descriptor) {

		/*
		 * If the type cache has never been created, go ahead and do so
		 * now.  The type cache is used to keep a copy of previously
		 * compiled descriptors.
		 */
		if (typeCache == null)
			typeCache = new Hashtable();

		/*
		 * See if we've compiled this string before.  If so, we can 
		 * use the previous value.
		 */
		Value result = (Value) typeCache.get(descriptor);
		
		/*
		 * Not seen this one before, so go to the trouble of setting up
		 * the tokenizer, expression handler, and the symbol table for the
		 * data types.
		 */
		if (result == null) {
			Tokenizer t = new Tokenizer(descriptor);
			Expression e = new Expression(session);
			SymbolTable s = new SymbolTable(session, "Data Types", null);
			try {
				s.insert("STRING", "");
				s.insert("BOOLEAN", false);
				s.insert("DOUBLE", 0.0);
				s.insert("INTEGER", 0);
				s.insert("NUMBER", Double.NEGATIVE_INFINITY);
				s.insert("ANY", Double.NaN);
			} catch (JBasicException e1) {
				System.out.println(e1);
			}

			/*
			 * Compile the expression, resulting in a simple template
			 * object.
			 */
			result = e.evaluate(t, s);
			
			/*
			 * Assuming the compilation was successful, save the
			 * result for later use.
			 */
			if (result != null)
				typeCache.put(descriptor, result);
		}
		
		return result;

	}
	
	
	/**
	 * Given two values, determine if they are of the same type, including
	 * a deep traversal of nested array elements or record members.
	 * <p>
	 * 
	 * This can be used for complex type validation, such as determining if
	 * a value is really an array of two strings by comparing the value to a
	 * constant declaration of ["",""] or something similar.
	 * @param base the base value to compare
	 * @param compare the value to compare to the base
	 * @return true if all types match, else false.
	 */
	public static boolean validateTypes( Value base, Value compare ) {

		if( base == null || compare == null )
			return false;
		
		int baseType = base.getType();
		int compareType = compare.getType();
		
		/*
		 * Special case, if the compare map is a double but the value is really
		 * a NaN, then we treat this as "ANY" and declare it a match.
		 */
		if( compareType == Value.DOUBLE && Double.isNaN(compare.getDouble()))
			return true;
		
		/*
		 * Special case, if the compare map is negative infinity, then match
		 * any numeric value (integer or double)
		 */
		
		if( compareType == Value.DOUBLE && Double.isInfinite(compare.getDouble())
				&& (baseType == Value.INTEGER || baseType == Value.DOUBLE))
			return true;
		
		/*
		 * Otherwise, the types must match.
		 */
		if( baseType != compareType)
			return false;
		
		switch( baseType ) {
		
		case Value.ARRAY:
			
			if( base.size() != compare.size())
				return false;
			
			for( int idx = 1; idx <= base.size(); idx++)
				if( !validateTypes(base.getElement(idx), compare.getElement(idx)))
					return false;
			break;
		
		case Value.RECORD:
			if( base.size() != compare.size())
				return false;
			Iterator i = base.getIterator();
			while( i.hasNext()) {
				String key = (String) i.next();
				if( !validateTypes(base.getElement(key), compare.getElement(key)))
					return false;
			}
			break;
		}
		
		return true;
	}
	

}

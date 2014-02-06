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
package org.fernwood.jbasic.compiler;

import java.util.ArrayList;

import org.fernwood.jbasic.Status;



/**
 * Simple tokenizer. <p><br>
 * This uses the very simple tokenization rules needed by the
 * language processor. This is slightly more complex than the rules used
 * by the built-in token tools in the java.lang package.
 * <p>
 * This allows tokens to be restored to the stream (unparsed) and also
 * has some special case flags like handling unary negation which is
 * semantically only meaningful during certain kinds of expression parsing.
 * <p>
 * The general strategy is to create a tokenizer object, and give it a string to
 * tokenize. This string can be presented as part of the object constructor, or
 * by a separate loadBuffer() method. When text is loaded into the tokenizer,
 * the lexer immediately breaks it into semantically meaningful parts, and
 * stores them in a queue.
 * <p>
 * Subsequent calls to the tokenizer to get a token or restore a token
 * move forward and backward in the queue. The restoreToken() operation
 * that supplies a new token will replace the current token in the queue (this
 * is used to convert the "#" in the OPEN #X statement to be a "FILE", as in
 * OPEN FILE X, for example).
 * <p>
 * Calls to the accessor functions like getSpelling() return
 * information about the current position in the token queue.
 * 
 * @author Tom Cole
 * @version version 1.2 Jun 21, 2011 [Cleaned up]
 * 
 */

public class Tokenizer {

	/*
	 * These are used to gather statistics about tokenization for tuning.
	 */
	
	/**
	 * A count of the number of tokens processed by the lexical scanner.
	 */
	public static int globalTokenCount = 0;
	
	/**
	 * The number of token buffers processed
	 */
	public static int globalBufferCount = 0;
	
	/**
	 * The largest token buffer size processed.
	 */
	public static int globalMaxCount = 0;

	/**
	 * Define a single token in the tokenizer queue. This object stores what is
	 * known about each discrete text item in a line to be parsed. For the most
	 * part, this object type is used as a structure definition, and has only a
	 * basic constructor and debugging toString() method.
	 * 
	 * @author cole
	 * 
	 */
	class Token {

		/**
		 * The spelling of the token; i.e. the textual representation of the
		 * token from the input source.
		 */
		String spelling;

		/**
		 * The type of the token, such as Tokenizer.IDENTIFIER or
		 * Tokenizer.STRING.
		 */
		int type;

		/**
		 * The position (zero-based) in the original buffer where this token was
		 * found. This can be used for error messaging, and also to be able to
		 * return the correct remainder of the buffer for cases when
		 * tokenization is used to only partially decompose a string.
		 */
		int position;

		/**
		 * General constructor for a Token object. The caller must supply the
		 * object data in the constructor.
		 * 
		 * @param tokenSpelling
		 *            The spelling of the token.
		 * @param tokenType
		 *            The token type (i.e. Tokenizer.INTEGER, etc.)
		 * @param tokenPosition
		 *            The position in the input buffer where this token was
		 *            found, zero-based.
		 */
		public Token(final String tokenSpelling, final int tokenType, final int tokenPosition) {
			spelling = tokenSpelling;
			type = tokenType;
			position = tokenPosition;
		}

		/**
		 * Debugging method used by the Eclipse debugger to format an object so
		 * it is readable.
		 */
		@Override
		public String toString() {
			return "Token(`" + spelling + "`, " + type + "@"+ position + ") ";
		}
	}

	/**
	 * The queue that holds the list of Token objects for the current buffer
	 * being tokenized.
	 */
	private ArrayList<Token> queue;

	/**
	 * The position of the current token that is about to be parsed. Before any
	 * tokenization, this has a value of zero.
	 */
	private int queuePosition;

	/**
	 * The string buffer we are processing. This buffer is modified as the
	 * tokenizer consumes tokens and returns them to the caller. The original
	 * string that is parsed is not preserved.
	 * 
	 * 
	 */
	private String buffer;

	/**
	 * The size of the original text string stored in buffer.
	 */
	private int buffSize;

	/**
	 * The current character position in the buffer during lexing.
	 */
	private int pos;

	/**
	 * The rest of the string (if any) after a comment is found.
	 */
	private String remainder;

	/**
	 * The length of the last token parsed.
	 */
	private int length;

	/**
	 * The text of the last token parsed. If the token was a string, then the
	 * spelling does not include the quotation marks.
	 */
	private String spelling;

	/**
	 * The token type of the last token parsed.
	 */
	private int type;

	/**
	 * Flag indicating if an ELSE can be expected as a reserved word in the
	 * token stream. Set during IF THEN ELSE processing only.
	 */
	public boolean fReserveElse;

	/**
	 * Flag indicating if the tokenizer is active. An active tokenizer has a
	 * known state for last token, etc. An inactive tokenizer has been created
	 * but not given a buffer to parse, or has exhausted it's text buffer.
	 */

	boolean fActiveParse;

	/**
	 * Flag indicating if debugging messages are to be printed as tokenization
	 * actions occur.
	 */

	boolean fDebugTokenizer;

	/**
	 * Flag indicating if a unary sign is allowed on the next token. This is
	 * used during processing of expressions to allow things like
	 * <code> X = Y + -Z</code> to parse correctly. This is only set when an
	 * expression atom is being parsed (a constant, identifier, or ()
	 * subexpression.
	 */

	boolean fUnarySign;

	private boolean fErrorPrinted;

	/**
	 * This token identifies multiple statements within a single line of
	 * text for language expressions that use such a thing.
	 */
	private String compoundStatementSeparator;

	/**
	 * Status indicating if there was a tokenization error of any kind.
	 */
	private Status status;

	/**
	 * Indicates that a token is an identifier (a name)
	 */
	public static final int IDENTIFIER = 101;

	/**
	 * Indicates that the token is an unsigned integer value
	 */
	public static final int INTEGER = 102;

	/**
	 * Indicates that the token is a special character or sequence of
	 * characters, such as "." or ">="
	 */
	public static final int SPECIAL = 103;

	/**
	 * Indicates that the token is a quoted string, though the quotes have been
	 * removed from the token spelling.
	 */
	public static final int STRING = 104;

	/**
	 * Indicates that the token represents a floating-point number
	 * representation that includes a decimal-point.
	 */
	public static final int DOUBLE = 105;

	/**
	 * Indicates that the token represents a DECIMAL number.
	 */
	public static final int DECIMAL = 106;
	
	/**
	 * Indicates that there is no token, because the end of the input text
	 * buffer was found.
	 */
	public static final int END_OF_STRING = 199;

	/**
	 * Indicates an unknown token type - this is used as a trap value for when
	 * testing falls through known cases.
	 */
	public static final int UNKNOWN = 198;
	
	/**
	 * Internal flag used to determine how token buffers are formatted.  If
	 * false, the old (fluffy and space-filled) version is used; if true
	 * then the newer version that uses a minimum number of spaces is used.
	 * This can be controlled with the <code>SET [NO]NEW_FORMATTER</code>
	 * command.
	 */
	public static boolean USE_NEW_FORMATTER = false;


	/**
	 * Character set of alphabetic characters, used in IDENTIFIER tokens
	 */
	public static final String alphabeticCharacterSet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/**
	 * Character set of digits, which are used in INTEGER values.
	 */
	public static final String numericCharacterSet = "0123456789";

	/**
	 * Character set of alphanumeric (letters and numbers) used in IDENTIFIERS
	 * after the first character.
	 */
	public static final String alphaNumericCharacterSet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_$#!";

	/**
	 * The character used to mark the location of an error.  The token position
	 * Information is used to determine where to print this marker.  The default
	 * is a carat (pointer) character.
	 */
	private static final char ERROR_MARKER_CHARACTER = '^';;

	/**
	 * When formatting a token buffer, do we make reserved names have a different
	 * case than user-provided names?
	 */
	private static boolean USE_RESERVED_CASE = true;

	/**
	 * Construct a tokenizer and initialize it with a string buffer. The string
	 * is tokenized as part of this call and a token array created that can be
	 * processed using the other methods of the Tokenizer object.
	 * 
	 * @param sourceBuffer
	 *            A text string to be tokenized.
	 * @param separator The token that represents the statement separator, if any.
	 */
	public Tokenizer(final String sourceBuffer, String separator) {
		setCompoundStatementSeparator(separator);
		initializeTokenizer(sourceBuffer);
	}

	/**
	 * Construct a tokenizer and initialize it with a string buffer. The string
	 * is tokenized as part of this call and a token array created that can be
	 * processed using the other methods of the Tokenizer object.
	 * 
	 * @param sourceBuffer
	 *            A text string to be tokenized.
	 */
	public Tokenizer(final String sourceBuffer ) {
		setCompoundStatementSeparator(":");
		initializeTokenizer(sourceBuffer);
	}
	
	/**
	 * Common routine to initialize the tokenizer regardless of the
	 * parameterization of the constructor.  Initializes all the 
	 * values of the tokenizer (except the statement separator, which
	 * is handled in the constructors) and then breaks up the buffer
	 * into individual lexical tokens.
	 * 
	 * @param sourceBuffer the source buffer to tokenize.
	 */
	private void initializeTokenizer( String sourceBuffer ) {
		fActiveParse = true;
		if (sourceBuffer == null) {
			buffer = "";
			buffSize = 0;
		} else {
			buffer = sourceBuffer.trim();
			buffSize = buffer.length();
		}
		queue = new ArrayList<Token>();
		queuePosition = 0;

		lex();
	}

	/**
	 * Set the default separator character for compound statements.  This
	 * defaults to a ":" character but can be overridden explicitly here.
	 * @param token the token used to identify a statement separator.
	 */
	public void setCompoundStatementSeparator( String token ) {
		compoundStatementSeparator = ( token == null ? ":" : token);
	}
	/**
	 * Restore the last parsed token to the token stream. This is the null case
	 * where a string isn't given to restore; it just uses the last parsed
	 * string stored in the token structure as the text to restore.
	 * 
	 */
	public void restoreToken() {
		if( queuePosition > 0 )
			queuePosition--;
		else
			status = new Status(Status.TOKBUFFER);
	}

	/**
	 * Restore a string value to the token stream. The string is prepended to
	 * the front of the existing buffer. The last parsed value (the "spelling")
	 * is reset so we don't try to use it again. This operation is used when we
	 * have parsed something that we can't use and must put back. You can only
	 * do this once; i.e. the queue of tokens to restore is only one deep.
	 * 
	 * @param newTok
	 *            the token string value to be restored to the token stream.
	 * @param kind
	 *            The kind of token being restored (Tokenizer.INTEGER, etc.)
	 */

	public void restoreToken(final String newTok, final int kind) {

		if( queuePosition == 0 ) {
			status = new Status(Status.TOKBUFFER);
			return;
		}
		
		/*
		 * Back up the pointer to the previous slot in the token vector.
		 */
		queuePosition--;
		
		/*
		 * Get a reference to the token object at that position.  Set
		 * the spelling and type to match the new parameters, so the next
		 * attempt to access this token will return the new spelling/type.
		 */
		final Token t = queue.get(queuePosition);
		t.spelling = newTok;
		t.type = kind;

	}

	/**
	 * Get the current string buffer being parsed.
	 * 
	 * @return A reference to the current string buffer
	 */
	public String getBuffer() {

		/*
		 * If the buffer we parsed is empty, return an empty string.
		 */
		if (buffer.length() == 0)
			return "";

		/*
		 * Get the token at the next available (unread) position. We only 
		 * need this to find out what position in the original buffer at 
		 * which it was found.
		 */
		final Token t = queue.get(queuePosition);
		
		/*
		 * Get the remainder of the string from that position forward.
		 * This has the effect of returning the "next" token identified by
		 * queuePosition and all text that follows it.
		 */
		return buffer.substring(t.position);
	}

	/**
	 * Accessor function to get the spelling of the current token.
	 * 
	 * @return A String containing the current spelling, or null if there is no
	 *         active token.
	 */
	public String getSpelling() {
		return spelling;
	}

	/**
	 * Accessor function to get the remainder of the string that was not parsed
	 * in the token queue. This normally is the text of any comment on the line.
	 * 
	 * @return A String containing unparsed text from the input buffer.
	 */
	public String getRemainder() {
		return remainder;
	}

	/**
	 * Accessor function to get the type of the current (most recently parsed)
	 * token.
	 * 
	 * @return An int representing the type; one of Tokenizer.IDENTIFIER,
	 *         Tokenizer.INTEGER, etc.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Test to see if the most recently parsed token matches the provided type.
	 * 
	 * @param testType
	 *            The type to check for, such as Tokenizer.SPECIAL or
	 *            Tokenizer.STRING
	 * @return True if the most recently parsed token matches the given type.
	 */
	public boolean isType(final int testType) {
		return (type == testType);
	}

	/**
	 * Scan a string to see if the next token is a double numeric value. This is
	 * used as a general routine for fetching numeric values since it can also
	 * handle integers. This returns the portion of the string that was parsed
	 * as a valid double.
	 * 
	 * @param buff
	 *            The string buffer to scan for a floating point value
	 * @param usign
	 *            Is the value required to be unsigned?
	 * @return String representation of what was parsed from the input buff.
	 */
	String scanForDouble(final String input, final boolean usign) {
		
		int sign = 0;
		boolean decimal = false;
		boolean digits = false;
		boolean hasExp = false;
		boolean hasExpSign = false;
		boolean hasExpdigits = false;
		
		/*
		 * If we fine a valid numeric value, we'll return the text of it
		 * in this buffer.
		 */
		StringBuffer output = new StringBuffer();

		/*
		 * Loop as long as there are unprocessed characters in the input
		 * buffer we're asked to scan.
		 */
		int pos = 0;
		while (pos < input.length()) {

			/*
			 * Get the next character from the input string.
			 */
			char ch = input.charAt(pos++);
			
			/*
			 * If we've come to the end of the token, then either
			 * return the digits we have if they are valid, or return
			 * an empty string indicating we never found a valid number.
			 */
			if (Character.isWhitespace(ch)) {
				if (digits)
					return output.toString();
				return "";
			}

			/*
			 * Is this the exponent indicator?  If we already have seen
			 * one, this is just another "E" and we return what we have.
			 * Otherwise, note that we've seen the exponent and put an "E"
			 * in the output buffer.
			 */
			if( digits && (ch == 'e' || ch =='E')) {
				if( hasExp )
					return output.toString();
				hasExp = true;
				output.append("E");
				continue;
			}
			
			/*
			 * Is this a sign?  If we have an exponent and exponent
			 * digits already, this is just another character and we
			 * return the buffer we already have.  If we already have
			 * an exponent sign then its not a valid number anyway.
			 * 
			 * If we don't have exponent yet and no digits, we record
			 * the sign.  If we've already seen digits, this sign isn't 
			 * part of our number and we return what we already have.
			 * 
			 * Messy, isn't it?
			 */
			if (ch == '-' || ch == '+') {
				if( hasExp ) {
					if( hasExpdigits )
						return output.toString();

					if( hasExpSign && hasExpdigits ) 
						return output.toString();
					if( hasExpSign )
						return "";
					hasExpSign = true;
					output.append(ch);
					continue;
				}
				if ((sign == 0) && usign) {
					sign = ( ch == '-')?  -1 : 1;
					output.append(ch);
					continue;
				}

				if (digits)
					return output.toString();

				return "";

			}

			/*
			 * Decimal point?  If we're already parsing exponent characters
			 * then we've gone beyond the token and we return just what we
			 * have so far.  
			 * 
			 * If we haven't seen a decimal point yet, process it as part
			 * of the number.  If we never got an explicit sign for the
			 * mantissa, then we assume positive at this point.
			 */

			if (ch == '.') {
				
				if( pos < input.length())
					if( input.charAt(pos) == '.')
						return output.toString();
				
				if( hasExp )
					return output.toString();
				
				if (!decimal) {
					decimal = true;
					output.append(ch);
					if (sign == 0)
						sign = 1;
					continue;
				}
				if (digits)
					return output.toString();
				return "";

			}

			/*
			 * Is it a digit?  Add it to our string and note that we've
			 * seen at least one digit.  If we've also passed the "e"
			 * for the exponent, indicate that we've seen exponent digits.
			 * And if we never got a sign before now for the mantissa,
			 * assume "+".
			 */
			if ("0123456789".indexOf(ch) > -1) {
				output.append(ch);
				digits = true;
				if( hasExp )
					hasExpdigits = true;
				if (sign == 0)
					sign = 1;
				continue;
			}
			break;
		}

		/*
		 * After we're done with the string, check to see if we saw
		 * an "E" for exponent but no exponent digits.  If so, then 
		 * the "E" must not be part of the number token; return up to
		 * that position in the buffer only.
		 */
		if( hasExp && !hasExpdigits) {
			int ix = output.indexOf("E");
			return output.substring(0,ix-1);
		}
		
		/*
		 * If we got some digits at some point, then this is a valid
		 * numeric token and return what we got.
		 */
		if (digits)
			return output.toString();
		
		/*
		 * No digits ever seen, so return empty string indicating nothing
		 * to see here, move along...
		 */

		return "";

	}

	/**
	 * Test to see if the token just parsed is really an identifier token. This
	 * is most commonly used as a sanity check for syntax errors during
	 * compilation.
	 * 
	 * @return true if the token is an identifier, false if it is not.
	 */
	public boolean isIdentifier() {
		return type == Tokenizer.IDENTIFIER;
	}

	/**
	 * Test next token for string value. Peeks ahead at the next token to see
	 * what it is, and compares with a given string value.
	 * 
	 * @param expectedToken
	 *            The expected token value
	 * @return If the next token is the expected token, return true
	 */
	public boolean testNextToken(final String expectedToken) {

		boolean resultStatus;
		String nextOne;

		/*
		 * Get the next token from the token queue.  If we shot past
		 * the end of the string (current token type is end-of-string)
		 * then indicate we didn't see the caller's token-of-interest.
		 */
		nextOne = nextToken();
		if (type == Tokenizer.END_OF_STRING)
			return false;

		/*
		 * If the next token is a quoted string (as opposed to an identifier)
		 * then it can never match - we don't allow this so a "PRINT" and 
		 * PRINT are not mistaken for the same token.  IF this is the case,
		 * put the token back and indicate that no action was taken.
		 */
		if (type == Tokenizer.STRING) {
			this.restoreToken();
			return false;
		}
		
		/*
		 * Test the next token against the test string for equality. This
		 * is done in a case-insensitive way, so testing for the token 
		 * 'print' is the same as 'Print' and 'PRINT'.
		 */
		if (nextOne.equalsIgnoreCase(expectedToken))
			resultStatus = true;
		else
			resultStatus = false;
		
		/*
		 * The test routine does not actually consume a token, only tests
		 * the state of the next token. So put the one we just parsed "back"
		 * on the token queue.  This really just backs up the token pointer
		 * in the queue.
		 */
		this.restoreToken();
		
		/*
		 * Return the test for token value as the result.
		 */
		return resultStatus;
	}

	/**
	 * Test next token for string value. Peeks ahead at the next token to see
	 * what it is, and compares with a given string value. If it does match, the
	 * token is also discarded. If the token does not match, the token buffer is
	 * unchanged.
	 * 
	 * @param expectedToken
	 *            The expected token value
	 * @return If the next token is the expected token, return true
	 */
	public boolean assumeNextToken(final String expectedToken) {

		String nextOne;

		/*
		 * Get the next token value in the token queue.  If it was
		 * the end of the buffer then we return false; there is no
		 * work to be done.
		 */
		nextOne = nextToken();
		if (type == Tokenizer.END_OF_STRING)
			return false;

		/*
		 * If the token is a quoted string, then put it back and
		 * return no-match... you cannot match a quoted string with
		 * this routine, so PRINT and "PRINT" cannot be mistaken for
		 * each other.
		 */
		if (type == Tokenizer.STRING) {
			this.restoreToken();
			return false;
		}

		/*
		 * If the token matches the token-of-interest, return true.
		 * Note that when true, the token has been "consumed" from
		 * the token queue.
		 */
		if (nextOne.equalsIgnoreCase(expectedToken))
			return true;

		/*
		 * There was no match, so put the token back and let the caller
		 * know we didnt' see the token-of-interest.
		 */
		this.restoreToken();
		return false;
	}

	/**
	 * Test next token to see if it is on a list of possible string values. 
	 * Peeks ahead at the next token to see
	 * what it is, and compares with a given string value. If it does match, the
	 * token is also discarded. If the token does not match, the token buffer is
	 * unchanged.
	 * 
	 * @param expectedTokens
	 *            A list of possible expected token values
	 * @return If the next token is on the list of expected tokens, returns true
	 */
	public boolean assumeNextToken(final String[] expectedTokens) {

		String nextOne;

		/*
		 * Get the next token value in the token queue.  If it was
		 * the end of the buffer then we return false; there is no
		 * work to be done.
		 */

		nextOne = nextToken();
		if (type == Tokenizer.END_OF_STRING)
			return false;

		/*
		 * If the token is a quoted string, then put it back and
		 * return no-match... you cannot match a quoted string with
		 * this routine, so PRINT and "PRINT" cannot be mistaken for
		 * each other.
		 */

		if (type == Tokenizer.STRING) {
			this.restoreToken();
			return false;
		}

		/*
		 * Loop over the array of tokens-of-interest.  For each one,
		 * see if the current token matches the array element.  If it
		 * does match, return true.  This results in consuming the
		 * current token as well.
		 */

		for (String testToken : expectedTokens )
			if (nextOne.equals(testToken))
				return true;

		/*
		 * If nothing in the list of interesting tokens was a match,
		 * then put the token we parsed back - this just backs up the
		 * queue pointer.  Then tell the caller we didn't find anything.
		 */
		this.restoreToken();
		return false;
	}

	/**
	 * Determine if the most recently parsed token matches a given SPECIAL
	 * character string. This is a test for both the exact characters, and the
	 * type of the token, all rolled into a single convenient method.
	 * 
	 * @param expectedToken
	 *            A string representation of the token, such as "<>"
	 * @return True if the most-recently-parsed token matches the SPECIAL
	 *         character string passed as expectedToken.
	 */
	public boolean isSpecial(final String expectedToken) {
		if ((type == Tokenizer.SPECIAL) && spelling.equals(expectedToken))
			return true;

		return false;
	}

	/**
	 * Test next token for a specific symbol value. Peeks ahead at the next
	 * token to see what it is, and compares with a given string value. If it
	 * does match, the token is also discarded. The token must be a symbol (as
	 * opposed to a quoted string containing the same value. If the token does
	 * not match, the token buffer is unchanged.
	 * 
	 * @param expectedToken
	 *            The expected token value
	 * @return If the next token is the expected token, return true
	 */
	public boolean assumeNextSpecial(final String expectedToken) {

		String nextOne;

		/*
		 * Get the next token.  If we hit end-of-string, no additional
		 * work is done and we indicate no-match.
		 */
		nextOne = nextToken();
		if (type == Tokenizer.END_OF_STRING)
			return false;
		
		/*
		 * If this token isn't a special character token, then put it
		 * back in the token queue and return no-match.
		 */
		if (type != Tokenizer.SPECIAL) {
			this.restoreToken();
			return false;
		}

		/*
		 * If the token text matches the token-of-interest then return
		 * a match.  This also results in the token being "consumed" by
		 * the calls.
		 */
		if (nextOne.equals(expectedToken))
			return true;

		/*
		 * No match, so put the token back on the queue and tell the caller
		 * we didn't see it.
		 */
		this.restoreToken();
		return false;
	}
	
	/**
	 * Test next token for to see if it is in a list of specific special characters. 
	 * Peeks ahead at the next
	 * token to see what it is, and compares with a given string value. If it
	 * does match, the token is also discarded. The token must be a symbol (as
	 * opposed to a quoted string containing the same value. If the token does
	 * not match, the token buffer is unchanged.
	 * 
	 * @param expectedTokens
	 *            The list of possible expected token values
	 * @return If the next token is the expected token, return true
	 */
	public boolean assumeNextSpecial(final String[] expectedTokens) {

		String nextOne;
		/*
		 * Get the next token.  If we hit end-of-string, no additional
		 * work is done and we indicate no-match.
		 */

		nextOne = nextToken();
		if (type == Tokenizer.END_OF_STRING)
			return false;

		/*
		 * If this token isn't a special character token, then put it
		 * back in the token queue and return no-match.
		 */
		if (type != Tokenizer.SPECIAL) {
			this.restoreToken();
			return false;
		}
		
		/*
		 * Loop over the array of tokens-of-interest.  For each one,
		 * see if the current token matches the array element.  If it
		 * does match, return true.  This results in consuming the
		 * current token as well.
		 */

		for( String testToken : expectedTokens)
			if (nextOne.equals(testToken))
				return true;

		/*
		 * The token didn't match anything in the array of interesting
		 * tokens, so put it back in the queue and tell the caller no
		 * match was found.
		 */
		this.restoreToken();
		return false;
	}

	/**
	 * Test next token for token type. Peeks ahead at the next token to see what
	 * it is, and compares with a given string value.
	 * 
	 * @param expectedType
	 *            The expected token type
	 * @return If the next token is the expected type, return true
	 */
	public boolean testNextToken(final int expectedType) {

		boolean matchedType;
		
		/*
		 * Advance to the next token
		 */
		nextToken();
		
		/*
		 * See if the type of the token matches the expected type.
		 */
		
		matchedType = (type == expectedType);
		
		/*
		 * This is a non-destructive call to the token queue, so put
		 * the token back (actually just backs up the queue pointer).
		 * The return the state of the comparison test.
		 */
		this.restoreToken();
		return matchedType;
	}

	/**
	 * Return the next token in the token array. The "current" token values are
	 * updated to reflect the next token.
	 * 
	 * @return A String containing the spelling of the token. Null is returned
	 *         if the token was END_OF_STRING.
	 */
	public String nextToken() {

		/*
		 * If all the tokens have been consumed (queue pointer is past
		 * the actual size of the queue) then return the END_OF_STRING
		 * indicators.
		 */
		if (queuePosition >= queue.size()) {
			spelling = "";
			type = Tokenizer.END_OF_STRING;
			fActiveParse = false;
			return null;
		}
		
		/*
		 * Get the token at the current queue position, and advance 
		 * the queue pointer for the next call.  The spelling and type
		 * information are moved from the token element into the state
		 * information for the Tokenizer itself, indicating the state
		 * of the token.
		 */
		final Token t = queue.get(queuePosition++);
		spelling = t.spelling;
		type = t.type;

		/*
		 * The return value is the actual spelling of the token.  The
		 * caller can check the type independently.
		 */
		return spelling;

	}

	/**
	 * Peek ahead to see what the nth token ahead in the queue is. This does not
	 * change the current token or the token queue in any way.
	 * 
	 * @param count
	 *            The zero-based position in the queue to extract a token from,
	 *            based on the current position. A value of 0 returns the next
	 *            token.
	 * @return The string spelling of the token. If the count would move past
	 *         the token buffer, then an empty string is returned.
	 */
	public String peek(final int count) {
		
		/*
		 * If this token position is beyond the end of the token queue
		 * (i.e. no more tokens in the string) or before the start of
		 * the queue, then return an empty string.
		 */
		
		int peekPosition = queuePosition + count;
		
		if (peekPosition < 0 || peekPosition >= queue.size()) {
			status = new Status(Status.TOKBUFFER);
			return "";
		}
		
		/*
		 * Otherwise, get the token value at the relative position in the
		 * queue from our current location, and return the spelling of the
		 * token.
		 */
		return queue.get(peekPosition).spelling;

	}

	/**
	 * Scan the buffer associated with this tokenizer, and break it into
	 * discrete textual elements. This scans the source and creates the ArrayList
	 * named 'queue' that holds each token found. This operation is done once
	 * each time a new text buffer is loaded in the tokenizer. Subsequent calls
	 * to nextToken(), etc. simply scan through the token queue created by this
	 * routine.
	 * 
	 * @return The number of Token objects stored in the tokenizer's queue
	 */
	private int lex() {

		/*
		 * Initialize a new ArrayList which will contain the individual tokens
		 * that are processed from the input buffer string. There are typically
		 * ten or fewer tokens on a given program line.
		 */
		queue = new ArrayList<Token>(10); 

		/* 
		 * Initialize the status and various pointers and counters.
		 */
		status = null; /* Same as success */
		int count = 0;
		Token t = null;
		pos = 0;
		buffSize = buffer.length();
		remainder = "";

		while (true) {

			/*
			 * Skip leading blanks and other whitespace characters.
			 */

			while (pos < buffSize) {
				if (!Character.isWhitespace(buffer.charAt(pos)))
					break;
				pos++;
			}

			/*
			 * Remember where we start, and parse a single token from the 
			 * input string.
			 */
			
			final int tokenPos = pos;
			lexNextToken();

			/*
			 * Allocate a new token object, and fill it in.
			 */
			t = new Token(spelling, type, tokenPos);

			/*
			 * Add it to the token queue, and see if we're done.
			 */
			queue.add(t);
			count++;
			if (t.type == Tokenizer.END_OF_STRING)
				break;
		}
		
		/*
		 * Reset the state of the tokenizer to begin processing tokens
		 * at the first position in the queue, and that parsing is active
		 * (i.e. end-of-string not reached yet).  Finally, let the caller
		 * know how many tokens we ended up with.
		 */
		queuePosition = 0;
		fActiveParse = true;

		globalTokenCount += count;
		globalBufferCount ++;
		if( count > globalMaxCount )
			globalMaxCount = count;
			
		return count;
	}

	/**
	 * Scan the input buffer for the next token. This is where the lexing rules
	 * are embodied (that is, that ">=" is a single token, the nature of an
	 * identifier, etc.) The current token data expressed in Tokenizer object is
	 * updated to reflect the state of the next token, such as the spelling and
	 * type.
	 * <p>
	 * 
	 * As a rule, this routine is only called from lex() to build the token
	 * queue
	 * 
	 * @return A string representing the token parsed. Implicitly also sets the
	 *         current token data in the Tokenizer object.
	 */
	public String lexNextToken() {

		String num;
		String ch;
		boolean is_alpha, is_num, is_string, more_data;
		int locallen;

		String bch;
		String next_set = "";

		/*
		 * END-OF-STRING
		 */

		if (pos >= buffSize) {
			type = END_OF_STRING;
			fActiveParse = false;
			spelling = "";
			remainder = "";
			length = 0;
			return spelling;
		}

		/*
		 * DOUBLE
		 */
		num = scanForDouble(buffer.substring(pos), fUnarySign);
		if (num.length() > 0) {
			spelling = num;
			type = DOUBLE;

			
			try {
				final int i = Integer.parseInt(num);
				final double d = Double.parseDouble(num);
				if (i == d)
					type = INTEGER;
			} catch (final NumberFormatException e) {
				/* Ignore it */
			}

			/*
			 * If it wasn't an integer but doesn't contain a fraction or exponent
			 * then we can interpret it as a BigDecimal (DECIMAL) type.
			 */
			if( type != INTEGER && num.indexOf('.') == -1 && num.toLowerCase().indexOf('e') == -1) 
				type = DECIMAL;
			
			
			length = num.length();
			pos += length;

			return spelling;
		}

		/*
		 * ARBITRARY SEQUENCE OF CHARACTERS
		 */
		ch = buffer.substring(pos, pos + 1);

		/*
		 * Determine the type of the token by looking at the first character.
		 * Alphabetic means identifier, digit means an integer, quote means a
		 * string, and anything else means sequence of special characters.
		 */
		is_alpha = ((alphabeticCharacterSet.indexOf(ch) > -1) | ch.equals("_") | ch
				.equals("$"));
		is_num = (numericCharacterSet.indexOf(ch) > -1);
		is_string = ch.equals("\"");

		/*
		 * Based on the type, figure out how we'll know when we come to the end
		 * of the token.
		 */
		if (is_string) {
			type = STRING;
			next_set = "\"";
		} else if (is_alpha) {
			type = IDENTIFIER;
			next_set = alphaNumericCharacterSet;
		} else if (is_num) {
			type = INTEGER;
			next_set = numericCharacterSet;
		} else {

			/*
			 * For special characters, this is a brute-force check against a
			 * list of well known tokens. Most special tokens are one character
			 * long.
			 */
			locallen = 1;

			/*
			 * Search for two-character special tokens.
			 */

			if (pos + 2 <= buffSize) {

				/*
				 * List of two-character tokens
				 */
				final String twoCharTokens[] = new String[] {
						"++", /* post increment */
						"--", /* post decrement */
						"!=", /* not equal to */
						"<>", /* not equal to */
						">=", /* greater than or equal */
						"<=", /* less than or equal */
						"==", /* equal */
						"||", /* concatenation */
						"//", /* comment separator */
						"->", /* method call */
						"<-", /* assignment */
						".."  /* array range specification */
				};

				bch = buffer.substring(pos, pos + 2);

				/*
				 * Check the list of two-byte tokens to see if any matches this token
				 */
				for (String tx : twoCharTokens)
					if (tx.equals(bch)) {
						locallen = 2;
						break;
					}

			}

			/*
			 * Search for three-character special tokens.  Currently there
			 * is only one, the ellipses ("...") token.
			 */
			if (pos + 3 <= buffSize) {
				bch = buffer.substring(pos, pos + 3);
				if (bch.equals("..."))
					locallen = 3;
			}

			/*
			 * Based on the length, extract the appropriate number of characters
			 * from the input buffer.
			 */
			spelling = buffer.substring(pos, pos + locallen);
			type = SPECIAL;
			length = spelling.length();

			/*
			 * Last special case; if it's the comment marker then pretend EOS
			 */

			if (spelling.equals("//")) {
				remainder = buffer.substring(pos);
				spelling = "";
				fActiveParse = false;
				length = 0;
				pos = 0;
				type = END_OF_STRING;
			}
			pos = pos + locallen;

			return spelling;
		}

		/*
		 * Okay, it wasn't a special character sequence, so skip through the
		 * token based on the type and the map of characters that are to be
		 * included, until we get to the end of the token.
		 */
		
		StringBuffer tok = new StringBuffer();
		more_data = true;
		if (type != STRING)
			tok.append(ch);
		int token_start = pos;
		pos++;
		boolean lastSlash = false;
		while (more_data)
			/* IF we skip past the end of the buffer we're done */
			if (pos >= buffer.length()) {
				more_data = false;
				if( type == STRING ) {
					/* We came to the end of the string without finding
					 * a closing quote, so it must not really be a string.
					 */
					status = new Status(Status.QUOTE);
					pos = token_start+1;
					type = SPECIAL;
					spelling = "\"";
					return spelling;
				}
			}
			else {

				ch = buffer.substring(pos, pos + 1);

				/*
				 * If its a STRING and quote, we're done unless the quote was
				 * "escaped" by a preceding backslash.
				 */
				if (type == STRING) {
					if (!lastSlash & ch.equals("\"")) {
						more_data = false;
						pos++;
					} else {
						pos++;
						tok.append(ch);
						lastSlash = ch.equals("\\");
					}
				} else {

					/*
					 * Otherwise, just see if it's in the set of characters we
					 * keep scanning over (identifiers, integers, etc.)
					 */
					more_data = next_set.indexOf(ch) > -1;
					if (more_data) {
						pos++;
						tok.append(ch);
					}
				}
			}

		/*
		 * If the token is not a string, then coerce it to uppercase.
		 */
		if (type == STRING)
			spelling = tok.toString();
		else
			spelling = tok.toString().toUpperCase();
		length = tok.length();

		return spelling;

	}

	/**
	 * Assigns a string to be the new buffer processed by a tokenizer object.
	 * The tokenizer object can be re-used and a new buffer given to it as the
	 * buffer is exhausted (returning an EOS token code on parse).
	 * 
	 * This discards the previous token queue (if any) and re-lexes the string.
	 * 
	 * @param s
	 *            The string to use for subsequent tokenization operations.
	 */
	public void loadBuffer(final String s) {
		buffer = s.trim();
		buffSize = buffer.length();
		fActiveParse = true;
		spelling = "";
		remainder = "";
		type = UNKNOWN;
		lex();
	}

	/**
	 * Advance the current token pointers past all text in the string. The
	 * current token becomes END_OF_STRING.
	 */
	public void flush() {

		remainder = "";
		fActiveParse = true;
		spelling = "";
		type = END_OF_STRING;
		if (queue == null)
			queuePosition = 1;
		else
			queuePosition = queue.size() + 1;
	}

	/**
	 * Debugging tool that describes the tokenizer. This is really only used by
	 * the Eclipse debugger.
	 * @return a string representation of the Tokenizer including the lexical 
	 * queue.
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("Tokenizer ");
		
		if (spelling != null) {
			result.append(", spelling=\"");
			result.append(spelling);
			result.append("\"");
			result.append(", type=");
			result.append(Integer.toString(type));
		}
		
		if (buffer != null) {
			result.append(", buffer=\"");
			result.append(buffer);
			result.append("\"");
		}
		if (queue == null)
			result.append(", no token queue");
		else {
			result.append(", ");
			result.append(Integer.toString(queue.size()));
			result.append(" tokens: ");
			final int n = queue.size();
			for (int i = 0; i < n; i++) {
				final Token t = queue.get(i);
				if (i == queuePosition)
					result.append('@');
				result.append('\'');
				result.append(t.spelling);
				result.append("' ");
			}
		}
		return result.toString();
	}

	/**
	 * Return the status object for the tokenizer.  Currently, the only
	 * errors detected at parsing time are mismatched quotes and buffer
	 * positioning errors by the caller. Usually the value
	 * will be SUCCESS if no error has occurred, QUOTE if there have been
	 * mismatched quotes, or TOKBUFFER if a token buffer positioning error
	 * has been made by the caller.<p>
	 * 
	 * The status is generally only set when the buffer is lexed at 
	 * constructor or buffer load times. The status is not reset until 
	 * a new buffer is loaded. 
	 * @return the last status of the tokenizer. 
	 */
	public Status status() {
		if( status == null )
			status = new Status();
		return status;
	}
	
	/**
	 * Return a string that contains two lines of text - the input source line,
	 * and a line with a pointer positioned at the offending location in the
	 * input.
	 * 
	 * @return String that can be printed (left justified) to describe the
	 *         location of the current token where an error is detected.
	 */
	public String error() {
		if( this.fErrorPrinted ) 
			return null;
		fErrorPrinted = true;
		return errorBuffer() + "\n" + errorPointer();
	}

	/**
	 * Return the buffer containing an error to be formatted. Currently, this
	 * just returns the buffer but might later have additional formatting
	 * information added.
	 * 
	 * @return String containing a representation of the original command line
	 *         tokenized.
	 */
	public String errorBuffer() {
		return buffer;
	}

	/**
	 * Return a String containing an "error pointer" that indicates where in the
	 * buffer that the error occurred.
	 * 
	 * @return A string that can be printed <em>after</em> the error buffer to
	 *         point out where an error occurred in the input text.
	 */
	public String errorPointer() {
		StringBuffer pointerString = new StringBuffer();
		
		int spaces;

		/*
		 * Figure out which token we're on, and get it's character position.
		 */
		if (queuePosition < queue.size()) {
			final Token t = queue.get(queuePosition);
			spaces = t.position;
		}
		else
			spaces = buffSize;
			
		/*
		 * Dashes to the desired position and then a marker.
		 */
		for (int i = 0; i < spaces; i++)
			pointerString.append('-');
		
		pointerString.append(ERROR_MARKER_CHARACTER);	
		
		return pointerString.toString();
		
	}

	/**
	 * @return the pointer of the current token position.
	 */
	public int getPosition() {
		return this.queuePosition;
	}
	
	/**
	 * Store a token in the token buffer, overwriting whatever token was
	 * previously at that position.  If the token position given is
	 * invalid then no update is made.
	 * 
	 * @param position The zero-based position in the queue that we are
	 * setting a value for.
	 * @param type The token type (Tokenizer.INTEGER, Tokenizer.STRING, etc)
	 * @param spelling The text of the token.
	 */
	public void setToken( int position, int type, String spelling ) {
		if( position < 0 || position >= queue.size()) {
			status = new Status(Status.TOKBUFFER);
			return;
		}
		Token t = this.queue.get(position);
		t.spelling = spelling;
		t.type = type;
	}

	/**
	 * Get the token at the given position in the token buffer.
	 * @param position the token number to fetch.
	 * @return String containing the token spelling at the given position
	 */
	public String getToken(int position) {
		if( position < 0 || position >= queue.size()) {
			status = new Status(Status.TOKBUFFER);
			return null;
		}
		Token t = this.queue.get(position);
		return t.spelling;
	}
	
	
	/**
	 * Reconstruct the string buffer using the current token queue
	 * contents; this is used after modifying token(s) via restore or
	 * set operations, and a new string buffer is to be returned.  This
	 * returns the string, and also makes it the new contents of this
	 * tokenizer's buffer (i.e. this is a destructive operation if you
	 * needed to keep a copy of the original text buffer.
	 * @return The formatted string
	 */
	public String reTokenize() {

		
		/*
		 * Reset the tokenizer to the start of the queue, without
		 * re-lexing anything.
		 */
		fActiveParse = true;
		spelling = "";
		remainder = "";
		type = UNKNOWN;
		queuePosition = 0;
		
		if( USE_NEW_FORMATTER ) {

			StringBuffer localBuffer = new StringBuffer();

			String lastSpelling = "";
			int lastType = Tokenizer.UNKNOWN;
			boolean firstIdentifier = true;
			int count = 0;
			
			while(!testNextToken(Tokenizer.END_OF_STRING)) {
				String t = this.nextToken();
				int thisType = getType();
				count++;
				
				/*
				 * Do we make reserved words different case than user words?
				 */
				if( USE_RESERVED_CASE && thisType == Tokenizer.IDENTIFIER  
						/* && (ReservedWords.isReserved(t) || JBasicFunction.isFunctionName(t) ) */ )
							t = t.toLowerCase();
				
				/*
				 * If this is the first identifier on the line and it's not MID$
				 * then we put a blank after it.  And if it's not the first token
				 * in the line, put a blank before it as well (for line numbers
				 * and labels).
				 * 
				 * The MID$ token is a special case because it's a pseudo-
				 * function and the following () should not be separated
				 * by a space.
				 */
				if( firstIdentifier && thisType == Tokenizer.IDENTIFIER && !t.equalsIgnoreCase("MID$")) {
					if( count > 1 )
						localBuffer.append(' ');
					localBuffer.append(t);
					localBuffer.append(' ');
					firstIdentifier = false;
					/* We already added the blank, so record it as such */
					/* so subsequent identifiers don't get double blanks. */
					lastSpelling = " "; 
					lastType = Tokenizer.UNKNOWN;
					continue;
				}
				
				/*
				 * If it was the MID$() pseudo function name, then turn off the
				 * test for first identifier so the rest of the line spaces
				 * as normal.
				 */
				if( firstIdentifier && thisType == Tokenizer.IDENTIFIER && t.equalsIgnoreCase("MID$"))
					firstIdentifier = false;
				
				/*
				 * If the next token is a string, we must re-enclose it in
				 * quotes.  Also, if the previous token was not a special
				 * character, put a blank before the quoted string.
				 */
				if( thisType == Tokenizer.STRING) {
					if( lastType != Tokenizer.SPECIAL)
						localBuffer.append(' ');
					lastType = Tokenizer.STRING;
					localBuffer.append('"');
					localBuffer.append(t);
					localBuffer.append('"');
					lastSpelling = "";
					continue;
				}

				/*
				 * Next we have a handful of cases were we need to add an extra
				 * blank into the buffer "by rule" given special case needs.
				 */
				boolean neSpace = false;
				if( lastType == Tokenizer.IDENTIFIER && thisType == Tokenizer.SPECIAL && t.equals("!=")) {
					localBuffer.append(' ');
					neSpace = true;
				}
				else
				/* 
				 * Tokens of "-" followed by "-" must be kept separate so they
				 * don't parse as "--" on subsequent lexical scans.
				 */
				if( lastType == Tokenizer.SPECIAL && lastSpelling.equals("-") && thisType == Tokenizer.SPECIAL && t.equals("-"))
					localBuffer.append(' ');
				else
					/* 
					 * Tokens of "+" followed by "+" must be kept separate so they
					 * don't parse as "+" on subsequent lexical scans.
					 */
				if( lastType == Tokenizer.SPECIAL && lastSpelling.equals("+") && thisType == Tokenizer.SPECIAL && t.equals("+"))
					localBuffer.append(' ');
				else
					/*
					 * An identifier followed by a special character or "#" or "!"
					 * as parts of an identifier string must have a space.
					 */
				if( lastType == Tokenizer.IDENTIFIER && thisType == Tokenizer.SPECIAL && ( t.equals("!") || t.equals("#")))
					localBuffer.append(' ');
				else
				/* 
				 * While not needed for parsing, it's much more readable if whatever
				 * follows a string has a space unless it's a special character.
				 */
				if( lastType == Tokenizer.STRING && thisType != Tokenizer.SPECIAL)
					localBuffer.append(' ');
				else
				/*
				 * Let's put a space after closing brackets and identifiers.
				 */
				if( lastSpelling != null && lastSpelling.equals("]") && thisType == Tokenizer.IDENTIFIER)
					localBuffer.append(' ');
				else
				/*
				 * Let's put a space after closing parenthesis and identifiers
				 */
				if( lastSpelling != null && lastSpelling.equals(")") && thisType == Tokenizer.IDENTIFIER)
					localBuffer.append(' ');
				else
				/*
				 * If the last character of the previous token and the first character
				 * of the new token combined could be mistaken for a single
				 * identifier, then put a space between them.
				 */
				if( lastSpelling != null && lastSpelling.length() > 0 ) {
					char last = lastSpelling.charAt(lastSpelling.length()-1);
					char next = t.charAt(0);
					if( Character.isJavaIdentifierPart(last) && Character.isJavaIdentifierPart(next))
						localBuffer.append(' ');
				}

				localBuffer.append(t);
				if( neSpace && thisType == Tokenizer.SPECIAL && t.equals("!="))
					localBuffer.append(' ');
				lastSpelling = t;
				lastType = thisType;
				
				/*
				 * If we just passed a DO or THEN clause, reset the "first identifier" flag
				 * which lets us do better spacing after initial verbs.  This allows things
				 * like IF X THEN RETURN [] to preserve the space after RETURN.
				 */
				if( thisType == Tokenizer.IDENTIFIER &&
						(t.equals("DO") || t.equals("THEN")))
					firstIdentifier = true;
			}
			localBuffer.append(this.getRemainder());
			this.loadBuffer(localBuffer.toString());
			return localBuffer.toString();
		} 
		
		/*
		 * We will reconstruct the buffer by scanning over the lexer queue.
		 */
		String localBuffer = "";
		
		int lastTokenType = Tokenizer.UNKNOWN;
		while (!testNextToken(Tokenizer.END_OF_STRING)) {
			String item = nextToken();
			int thisTokenType = getType();

			if (!item.equals(".") && !item.equals(",") && !item.equals(";")
					&& !item.equals(":") && !item.equals("->")
					&& (thisTokenType == Tokenizer.SPECIAL)) {

				if ((lastTokenType == Tokenizer.IDENTIFIER)
						&& ((item.equals("(")) || item.equals("["))) {
					if (localBuffer.charAt(localBuffer.length() - 1) == ' ')
						localBuffer = localBuffer.substring(0, localBuffer.length() - 2);
				} else
					localBuffer = localBuffer + " ";

				thisTokenType = Tokenizer.UNKNOWN;
			} else if ((lastTokenType == Tokenizer.SPECIAL)
					|| (thisTokenType == Tokenizer.SPECIAL)) {
				if (item.equals(",") || item.equals(";") || item.equals(":"))
					item = item + " ";
			} else
				localBuffer = localBuffer + " ";

			if (getType() == Tokenizer.STRING)
				localBuffer = localBuffer + "\"" + item + "\"";
			else
				localBuffer = localBuffer + item;
			lastTokenType = thisTokenType;
		}

		/*
		 * Get rest of string (comment) if any
		 */
		localBuffer = localBuffer + getRemainder();
		loadBuffer(localBuffer);
		
		return localBuffer;
		
	}

	/**
	 * Set the token position for the current tokenizer (typically this is
	 * the value retrieved from getToken()).
	 * @param savedTokenPosition a previous token posiiton
	 */
	public void setPosition(int savedTokenPosition) {
		if( savedTokenPosition < 0 || savedTokenPosition >= queue.size()) {
			status = new Status(Status.TOKBUFFER);
			return;
		}
		this.queuePosition = savedTokenPosition;		
	}

	/**
	 * Return the character position in the internal buffer where the current token
	 * was found. This can be used for error reporting.
	 * @return the 1-based character position.
	 */
	public int getCharacterPosition() {
		Token t = this.queue.get(this.queuePosition);
		return t.position+1;
	}
	
	/**
	 * Format a section of the token buffer.  The starting and ending positions
	 * are passed in as parameters, and are typically fetched by the caller via
	 * getPosition() during parsing.  This routine can then "lift" a section
	 * of the tokenized code from the token buffer and return it as a formatted
	 * string.  This is used in the PRINT <expr> = notation, for example.
	 * @param start the starting character position to process
	 * @param end the ending character position to process
	 * @return the formatted string value.
	 */

	public String format(int start, int end) {

		StringBuffer tokenBuffer = new StringBuffer();
		
		for( int ix = start; ix < end; ix++ ) {
			Token t = queue.get(ix);
			if( t.type == Tokenizer.STRING) {
				tokenBuffer.append('"');
				tokenBuffer.append( normalize(spelling));
				tokenBuffer.append('"');
			}
			else
				tokenBuffer.append(t.spelling);

			tokenBuffer.append(' ');
		}
		
		Tokenizer temp = new Tokenizer(tokenBuffer.toString());
		
		return temp.reTokenize();
	}

	/**
	 * Return the number of tokens in the token buffer.  This is the maximum
	 * position that a setPosition can specify, for example.
	 * @return an integer count of the number of tokens in the token buffer.
	 */
	public int size() {
		if( queue == null )
			return 0;
		
		return queue.size();
	}

	/**
	 * Determine if there are more tokens in this statement.  The statement
	 * can be ended by an END-OF-STATEMENT token, or by a compound statement
	 * separator (":" by default).
	 * @return true if there are no more tokens in this statement, even if
	 * there are more on the line.
	 */
	public boolean endOfStatement() {
		if( this.testNextToken(Tokenizer.END_OF_STRING))
			return true;
		if( this.testNextToken(compoundStatementSeparator))
			return true;
		
		return false;
	}

	/**
	 * Move the token pointer position a relative distance forwards
	 * or backwards.  There is no error checking.
	 * @param i the number of positions to move.  Forward motion is based
	 * on positive values; backwards motion on negative values.  A value 
	 * of zero does not change the position at all.
	 */
	public void movePosition(int i) {
		queuePosition += i;
		if( queuePosition < 0 ) {
			queuePosition = 0;
			status = new Status(Status.TOKBUFFER);
		}
		else 
			if( queuePosition > queue.size()) {
				queuePosition = queue.size();
				status = new Status(Status.TOKBUFFER);
			}
	}
	
	/**
	 * Given a string that contains possible control characters, convert
	 * them to escaped format if needed.
	 * @param sv the String value that is to be normalized
	 * @return A string with control characters converted to escape character
	 *         representation. Returns null if the value is not a STRING.
	 */

	public String normalize( String sv) {

		StringBuffer v = new StringBuffer();

		final int n = sv.length();
		for (int i = 0; i < n; i++) {

			final char ch = sv.charAt(i);

			if (ch == '\\')
				v.append( "\\\\");

			else if (ch == '\n')
				v.append("\\n");

			else if (ch == '\t')
				v.append("\\t");

			else if (ch == '\r')
				v.append("\\r");

			else if (ch == '"')
				v.append("\\\"");

			else
				v.append(ch);
		}

		return v.toString();
	}

}
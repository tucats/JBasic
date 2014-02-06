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
 * Created on May 31, 2007 by tom
 *
 */
package org.fernwood.jbasic.informats;

import java.util.ArrayList;
import java.util.TreeMap;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * The InputProcessor is a way of specifying a list of one or more Informats 
 * that are used to process a buffer of characters.  The Informats can be used
 * to convert characters into data (such as an INTEGER) or to change position 
 * in the buffer (such as SKIP).
 * 
 * This is currently implemented using the INPUT( fmt-array, buffer), where the
 * first parameter is an array of strings containing the format commands and
 * the buffer is a string containing data to be formatted.
 * 
 * Currently supported informats:
 * 
 * INTEGER(x)		Text representation of an integer value in a field x
 * 					bytes long.  Value must be right-justified
 * 
 * SKIP("string")	Skip until a match for the string is found.
 * 
 * SKIP(*)			Skip ahead to the next non-whitespace.
 * 
 * SKIP(x)			Skip ahead x characters in the buffer.
 * 
 * @author tom
 * @version version 1.0 May 31, 2007
 *
 */
public class InputProcessor {

	
	/**
	 * This object tracks the relationship between a verb name such as "INTEGER"
	 * and the underlying Informat object that implements it. This is used as a
	 * lookup table in the InputProcessor.Command class.
	 * @author cole
	 * @version version 1.0 Jun 6, 2007
	 *
	 */
	class InFormatMap {
		String verb;
		Informat fmt;
	}
	
	static TreeMap formatVerbList;
	
	/**
	 * This class encapsulates each format command operation that is to be
	 * done on an input buffer.  The format command is "compiled" and converted
	 * to an object type and additional information for length and scale, and
	 * stored in this object.  A list of these objects comprises the formatting
	 * operations for a given InputBuffer.
	 * @author cole
	 * @version version 1.0 Jun 6, 2007
	 *
	 */
	class Command {
		/**
		 * Indicates that a command input field is of arbitrary and varying
		 * length, as opposed to a fixed field width which is expressed as
		 * a positive integer.
		 */
		public static final int VARYING = -1;
		String verb;
		Informat method;
		int	length;
		int	scale;
		String string;
		Status status;

		Command( String text ) {
			status = compile(text);
		}
		
		Command( Value fmtRecord ) {
			status = compile(fmtRecord);
		}
		
		public String toString() {
			StringBuffer text = new StringBuffer(verb + "(");
			if( length == Command.VARYING)
				text.append("*");
			else
				if( string != null )
					text.append("\"" + string + "\"");
				else
					text.append(length);
			if( scale > 0 )
				text.append("," + Integer.toString(scale));
			text.append(")");
			return text.toString();
		}
		
		
		Status compile( Value record ) {
			status = new  Status(Status.INVRECORD);
			if( record.getType() != Value.RECORD) 
				return status;
			
			Value v = record.getElement("NAME");
			if( v == null )
				return status;
			
			verb = v.getString().toUpperCase();
			Informat fmt = (Informat) InputProcessor.formatVerbList.get(verb);
			if( fmt != null )
				method = fmt;
			else
				method = null;

			v = record.getElement("LEN");
			if( v != null )
				length = v.getInteger();
			
			v = record.getElement("SCALE");
			if( v != null)
				scale = v.getInteger();
			
			v = record.getElement("STRING");
			if( v != null)
				string = v.getString();
			
			return status = new Status();
		}
		
		
		/*
		 * Compile a string of text into a set of format actions, which
		 * are stored in the local object.
		 */
		Status compile( String text ) {
			status = new Status();
			Tokenizer tokens = new Tokenizer(text);
			verb = tokens.nextToken().toUpperCase();
			
			Informat fmt = (Informat) InputProcessor.formatVerbList.get(verb);
			if( fmt != null )
				method = fmt;
			else
				method = null;
			
			if( tokens.assumeNextSpecial("(")) {
				
				if( tokens.testNextToken(Tokenizer.STRING)) {
					string = tokens.nextToken();
					length = string.length();
				}
				else
				if( tokens.assumeNextSpecial("*"))
					length = Command.VARYING;
				else
					length = Integer.parseInt(tokens.nextToken());

				if( tokens.assumeNextSpecial(".")) {
					if( tokens.assumeNextSpecial("*"))
						scale = Command.VARYING;
					else
						scale = Integer.parseInt(tokens.nextToken());
				}
				if( !tokens.assumeNextSpecial(")"))
						status = new Status(Status.PAREN);
			}
			
			if( !tokens.testNextToken(Tokenizer.END_OF_STRING))
				status = new Status(Status.INVFMT, tokens.getBuffer());
			return status;
		}
	}
	
	ArrayList<Command> formatCommands;
	String buffer;
	int currentPos;
	int currentFormat;
	boolean fActive;

	/**
	 * The status of the last operation performed using the Input Processor.
	 * This can be used after getting a null value back from the input
	 * processing operation, for example, to learn what was wrong with the
	 * input text.  It can also be used to get more information about why
	 * a format specification could not be compiled, for example.
	 */
	public Status status;

	/**
	 * Create a new instance of an input processor.  This object accepts
	 * a definition of an input format specification, which it compiles
	 * into an internal representation.  This can be used to process string
	 * buffers and extra Value objects from the string based on the format 
	 * specification.
	 */
	public InputProcessor() {
		formatCommands = new ArrayList<Command>();
		if( InputProcessor.formatVerbList == null)
			InputProcessor.formatVerbList = initializeFormatVerbList();
		setBuffer(null);	
	}
	
	/**
	 * Create a new instance of an input processor.  This object accepts
	 * a definition of an input format specification, which it compiles
	 * into an internal representation.  This can be used to process string
	 * buffers and extra Value objects from the string based on the format 
	 * specification.
	 * @param b The initial string buffer to use for input processing.
	 */

	InputProcessor( String b ) {
		formatCommands = new ArrayList<Command>();
		if( InputProcessor.formatVerbList == null)
			InputProcessor.formatVerbList = initializeFormatVerbList();
		setBuffer(b);
	}
	
	/**
	 * Set the input buffer to be used with the compiled format to read
	 * values.
	 * @param b The string buffer to use for input processing.
	 */
	public void setBuffer( String b ) {
		buffer = b;
		currentPos = 0;
		currentFormat = 0;
		status = new Status();
	}
	
	TreeMap initializeFormatVerbList() {
		
		TreeMap<String,Informat> list = new TreeMap<String,Informat>();
		
		list.put("INTEGER", new IntegerFormat());
		list.put("SKIP",	new SkipFormat());
		list.put("HEX",		new HexFormat());
		list.put("BINARY",	new BinaryFormat());
		list.put("CHAR",	new CharFormat());
		return list;
		
	}
	
	/**
	 * Add a format command to the input processor.  Each format command
	 * represents an input operation (read an integer, skip bytes, etc.).
	 * These are accumulated by compiling the text description into an
	 * internal representation.  There must be at least one input format
	 * added to the input processor to be able to read values from an input
	 * string.
	 * 
	 * @param f The text of an input format operation, such as "INTEGER(8)"
	 * or "SKIP(3)"
	 * @return Status indicating if the format was valid.
	 */
	public Status addInputFormat( String f ) {
		
		Command cmd = new Command(f);
		if( cmd.status.success())
			formatCommands.add(cmd);
		return cmd.status;
	}
	
	/**
	 * Add a format command to the input processor.  Each format command
	 * represents an input operation (read an integer, skip bytes, etc.).
	 * These are accumulated by compiling the text description into an
	 * internal representation.  There must be at least one input format
	 * added to the input processor to be able to read values from an input
	 * string.
	 * 
	 * @param f The text of an input format operation, such as "INTEGER(8)"
	 * or "SKIP(3)"
	 * @return Status indicating if the format was valid.
	 */
	public Status addInputFormat( Value f ) {
		Command cmd = new Command(f);
		if( cmd.status.success())
			formatCommands.add(cmd);
		return cmd.status;
	}

	/**
	 * Use the previously compiled format specifications to fetch the next
	 * input value from the input buffer string.  IF null is returned, use
	 * the "status" field to determine the nature of the error encountered.
	 * @return The next Value found in the input buffer.  If there is no 
	 * more data in the input buffer or the next item is not valid, then a 
	 * null is returned.
	 */
	public Value getNextValue() {
		
		Value v = null;
		
		if( currentFormat >= formatCommands.size()) {
			status = new Status(Status.EOF);
			return null;
		}
		
		fActive = true;
		while (fActive) {
			Command cmd = formatCommands.get(currentFormat++);
			// System.out.println("CALLING FORMAT " + cmd );
			v = cmd.method.run(this, cmd);
		}
		return v;
		
	}
	
	/**
	 * Return the current position in the input buffer.  This is used to reset the
	 * position if an input operation fails or has to "look ahead" to determine if
	 * a valid value is to be found.
	 * @return a zero-based integer position.  This can be passed back to reset()
	 * to set the input pointer back to a saved position.
	 */
	int mark() {
		return this.currentPos;
	}
	
	void reset( int m ) {
		this.currentPos = m;
	}
	
	char nextChar() {
		if( currentPos >= buffer.length())
			return 0;
		
		return this.buffer.charAt(currentPos++);
	}
	
	/**
	 * Back up one character in the input buffer.
	 */
	void backup() {
		if( currentPos > 0)
			currentPos--;
	}
	
	static int findCharacter( char ch, String match) {
		for( int ix = 0; ix < match.length(); ix++)
			if( match.charAt(ix) == ch)
				return ix+1;
		return 0;
			
	}
}


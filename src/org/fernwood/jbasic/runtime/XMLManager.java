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
 * Created on May 14, 2008 by tom
 *
 */
package org.fernwood.jbasic.runtime;

import java.util.ArrayList;
import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.funcs.CryptstrFunction;
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * Simple generator and parser routines for XML that contains a value.  
 * The XMLManager can be used
 * to convert a Value into an XML string, or to parse an XML string and return
 * the Value encoded in the XML.  If an error occurs during parsing, there is
 * a status variable that can be consulted.
 * @author tom
 * @version version 1.1 Sep 7, 2009 Add optimization for empty arrays and 
 * records
 *
 */
public class XMLManager {

	/**
	 * Status of the last operation performed; this can be queried
	 * after a call if it returns a null value.
	 */
	private Status status;
	
	/**
	 * The key string used to encode an encrypted XML value.  The 
	 * default value will be set if none is provided, but encrypted
	 * XML can be created and decoded using a private key value if
	 * this is set later using the setPassword() method.
	 */
	private String encodedKey;
	
	/**
	 * The local buffer for holding the XML string as it is parsed.
	 */
	private String xmlString;
	
	/**
	 * This instance of the tokenizer is used to handle parsing the
	 * XML value.
	 */
	private Tokenizer tokens;

	/**
	 * This is the controlling session for this instance of the XML
	 * Manager.
	 */
	private JBasic theSession;

	/**
	 * This is the name of the root XML node for a stored Value.
	 */
	static public final String defaultRootValue = "JBasicValue";

	/**
	 * Get the Status of the last operation done with this object.
	 * @return a Status object.
	 */
	public Status getStatus() {
		if( status == null )
			status = new Status();
		return status;
	}
	
	/**
	 * Create a new XMLManager object.
	 * @param session the JBasic session this is being handled in, which is used
	 * to get state information from the session global variables.
	 * @param xmlString the String containing the XML code to parse.
	 */
	public XMLManager( JBasic session, String xmlString) {
		super();
		tokens = new Tokenizer(xmlString );
		theSession = session;
		encodedKey = "poptarts";
	}

	/**
	 * Create a new XMLManager object.  No initial XML string is provided;
	 * use the parseXML(string) to parse code with a supplied XML data
	 * representation.
	 * @param session the JBasic session this is being handled in, which is used
	 * to get state information from the session global variables.
	 */
	public XMLManager( JBasic session ) {
		super();
		tokens = new Tokenizer("");
		theSession = session;
		encodedKey = "poptarts";
	}

	/**
	 * If this XML manager is used to encode data, you can set the password used as part of the
	 * underlying encryption.  If you don't set it, a default value is used to allow encoded
	 * and decoded XML to match.
	 * @param s the plain-text string to use to decode encrypted XML
	 */
	public void setPassword(String s ) {
		encodedKey = s;
	}
	
	/**
	 * Set a new buffer to parse XML from.  The string is tokenized immediately
	 * but is't parsed until the parseXML() call starts moving through the
	 * token buffer.  This means errors won't be detected until the parseXML
	 * call.
	 * @param xml the raw XML string to parse.
	 */
	public void setString( String xml ) {
		xmlString = xml;
		tokens.loadBuffer(xmlString);
	}

	/**
	 * Parse an XML string and return a value.  This uses the default XML
	 * root tag to locate the value to parse.
	 * 
	 * @return a Value object containing the data in the XML string, or null if there
	 * was an error.  If there was an error, you can consult the status variable in the
	 * class.
	 */
	public Value parseXML( ) {
		return parseXML(defaultRootValue);
	}

	/**
	 * Parse the current XML string in the XMLParser object.  A custom
	 * XML root tag is provided and is used to search the string for a
	 * Value to parse.
	 * @param rootValue the root value to search for in the XML string
	 * @return Value contained in XML
	 */
	public Value parseXML( String rootValue ) {
		
		status = new Status(Status.XML, "no valid value found in string");
		Value v = null;
		Value result = null;
		String root = null;
		if( rootValue == null )
			rootValue = defaultRootValue;

		int count = 0;
		while( true ) {

			/*
			 * If we're done with the string, bail out.
			 */
			if( tokens.testNextToken(Tokenizer.END_OF_STRING))
				break;

			/*
			 * The next (root) item in the string should be <JBASICVALUE>, so
			 * skip through the string a token-at-a-time until you find the
			 * tag.
			 */

			if( !tokens.assumeNextSpecial("<")) {
				tokens.nextToken();
				continue;
			}

			root = tokens.nextToken();
			if( !root.equalsIgnoreCase(rootValue)){
				tokens.nextToken();
				continue;
			}

			if( !tokens.assumeNextSpecial(">")){
				tokens.nextToken();
				continue;
			}


			/*
			 * Now parse an item.  This can be a single value, or a compound
			 * value.  The result is returned as a Value object we can send
			 * back to the caller.
			 */

			v = parseValue();
			if( v == null )
				return null;

			/*
			 * There needs to be a closing </JBASICVALUE>
			 */

			status = new Status(Status.XML, "missing/invalid closing tag"
					+ " at " + tokens.getCharacterPosition());

			if( !tokens.assumeNextSpecial("<"))
				return null;
			if( !tokens.assumeNextSpecial("/"))
				return null;

			if( !tokens.assumeNextToken(root))
				return null;

			if( !tokens.assumeNextSpecial(">"))
				return null;

			/*
			 * Got a good value, put it in the result.  IF this is the 
			 * first(only) result then just return it.  If this is the 
			 * second value we found, convert the result into an array 
			 * and add the previous and current elements to the array.  
			 * 
			 * Finally, if this is our third-or-more value, just add it 
			 * to the array we're already building.
			 */
			count++;

			if( count == 1 ) {
				result = v;
			}
			else if( count == 2 ) {
				Value t = result;
				result = new Value(Value.ARRAY, null);
				result.addElement(t);
				result.addElement(v);
			}
			else
				if( result != null )
					result.addElement(v);

			status = new Status();

		}
		/*
		 * All done, return the Value(s) that we parsed.
		 */
		return result;

	}

	/**
	 * Parse a single Value from the XML stream.  Can be called recursively.
	 * @return null if there was an error parsing the Value object.
	 */
	Value parseValue() {

		/*
		 * The first object must be a type code
		 */
		status = new Status(Status.XML, "invalid Value item"
				+ " at " + tokens.getCharacterPosition());
		if( !tokens.assumeNextSpecial("<"))
			return null;

		if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
			return null;

		String typeName = tokens.nextToken();
		int count = 0;
		Value v = null;

		/*
		 * See if there are any attributes.  Currently, COUNT and
		 * DATA are the only allowed attributes.
		 */
		String textData = null;
		String nameData = null;
		String typeData = null;
		String pw = null;
		
		while( tokens.testNextToken(Tokenizer.IDENTIFIER)) {
			String attr = tokens.nextToken();

			if( attr.equals("KEY")) {
				if( !tokens.assumeNextSpecial("="))
					return null;
				pw = encodedKey + tokens.nextToken();

			}
			else
			if( attr.equals("NAME")) {
				if( !tokens.assumeNextSpecial("="))
					return null;
				nameData = tokens.nextToken();

			}
			else
				if( attr.equals("TYPE")) {
					if( !tokens.assumeNextSpecial("="))
						return null;
					typeData = tokens.nextToken();

				}
				else
			if( attr.equals("DATA")) {
				if( !tokens.assumeNextSpecial("="))
					return null;
				textData = tokens.nextToken();

			}
			else if( attr.equals("D")) {
				if( !tokens.assumeNextSpecial("="))
					return null;
				textData = tokens.nextToken();

			}
			else if( attr.equals("TEXT")) {
				if( !tokens.assumeNextSpecial("="))
					return null;
				textData = tokens.nextToken();

			}
			else if( attr.equals("COUNT")) {
				if( !tokens.assumeNextSpecial("="))
					return null;
				String countString = tokens.nextToken();
				count = Integer.parseInt(countString);
			}
			else 
				return null;
		}

		/*
		 * If the type is METADATA then we're doing some special
		 * parsing for TABLE data.
		 */
		
		if( typeName.equals("METADATA")) {
			typeName = "ARRAY";
		}
		
		/*
		 * If the type is ARRAY or RECORD, there was no data, and
		 * we have a closure on the tag, then assume it was an
		 * empty ARRAY or RECORD definition.
		 */
		if( typeName.equals("ARRAY") || typeName.equals("RECORD")) {
			int savedPos = tokens.getPosition();
			if( tokens.assumeNextSpecial("/"))
				if( tokens.assumeNextSpecial(">")) {
					return new Value(Value.nameToType(typeName), null);
				}
			tokens.setPosition(savedPos);
		}
		/*
		 * See if there was data, which also allows a short-form tag
		 */
		if( textData != null ) {
			if(!tokens.assumeNextSpecial("/"))
				return null;
		}
		/*
		 * Must be a closing ">" on the type code.
		 */
		if( !tokens.assumeNextSpecial(">"))
			return null;

		if( typeName.equals("ENCODED")) {
			
			/*
			 * Build a buffer that contains the encoded data, which
			 * looks to the tokenizer like a bunch of identifiers.
			 * Stop when the end of the <ENCODED> tag is found.
			 */
			StringBuffer raw = new StringBuffer();
			while( tokens.testNextToken(Tokenizer.IDENTIFIER))
				raw.append(tokens.nextToken());
			if( !tokens.assumeNextSpecial("<"))
				return null;
			if( !tokens.assumeNextSpecial("/"))
				return null;
			if( !tokens.assumeNextToken("ENCODED"))
				return null;
			if( !tokens.assumeNextSpecial(">"))
				return null;
			
			/*
			 * Decode the string of text (alphabetic encoding) into
			 * a Value that contains an array of integers.  The array
			 * of integers is the input/output of the Cipher function.
			 */
			Value a = CryptstrFunction.stringToArray(raw.toString());
			
			/*
			 * Create an instance of the SimpleCipher helper class,
			 * and set the password for the instance.
			 */
			SimpleCipher cipher = new SimpleCipher(this.theSession);
			cipher.setPassword(pw);
			
			/*
			 * Create a new temporary sub-instance of the XML manager
			 * that we'll use to create a temp XML buffer of the
			 * decrypted XML expression.
			 */
			XMLManager x = new XMLManager(this.theSession);
			
			/*
			 * Stuff the lead tag into the buffer, followed by the
			 * raw XML decrypted using the cipher manager, and the
			 * ending tag.  This results in a new XML string
			 * value, which can be parsed using the temporary
			 * instance of a new XML Manager.
			 */
			StringBuffer rawXML = new StringBuffer("<JBasicValue>");
			rawXML.append(cipher.decrypt(a).getString());
			rawXML.append("</JBasicValue>");
			x.setString(rawXML.toString());
			return x.parseXML();
		}

		/*
		 * It wasn't an encoded XML string, so based on the type, 
		 * parse the item text correctly.
		 */
		status = new Status(Status.XML, "invalid " + typeName + " value given"
				+ " at " + tokens.getCharacterPosition());
		
		/*
		 * If it is the pseudo-type of COLUMN then the result is parsing
		 * the name and type as a composite string value.
		 */
		if( typeName.equals("COLUMN")) {
			v = new Value(nameData + "@" + typeData);
			return v;
		}
		
		/*
		 * Convert the type name to a type selector.  If it isn't
		 * recognized then throw an error.
		 */
		int type = Value.nameToType(typeName);
		if( type == Value.UNDEFINED) {
			status = new Status(Status.XML, "unknown Value type " + typeName
					+ " at " + tokens.getCharacterPosition());return null;
		}
		
		
		int sign = 1;

		/*
		 * If the text was stored directly in the tag, then we can do
		 * this quickly and simply by just converting the text using
		 * the type value and the coerce() method.  The resulting
		 * value is our return value.
		 */
		if( textData != null ) {
			v = new Value(textData);
			if( type == 0 )
				return null;
			try {
				v.coerce(type);
				if( type == Value.STRING)
					v = new Value(v.denormalize());

			} catch (JBasicException e) {
				return null;
			}
			return v;
		}

		/*
		 * Not a shorthand tag, so parse the value from the input
		 * buffer, delimited by the end tag.
		 */
		switch( type ) {

		case Value.BOOLEAN:
			if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
				return null;
			v = new Value(tokens.nextToken().equals("TRUE"));
			break;

		case Value.INTEGER:
			if( tokens.assumeNextSpecial("-"))
				sign = -1;
			if( !tokens.testNextToken(Tokenizer.INTEGER))
				return null;
			v = new Value(sign * Integer.parseInt(tokens.nextToken()));
			break;

		case Value.DOUBLE:
			if( tokens.assumeNextSpecial("-"))
				sign = -1;
			if( !tokens.testNextToken(Tokenizer.DOUBLE) &
					!tokens.testNextToken(Tokenizer.DOUBLE))
				return null;
			try {
				v = new Value(sign *  Double.parseDouble(tokens.nextToken()));
			} catch(NumberFormatException e) {
				v = new Value(Double.NaN);
			}
			break;

		case Value.STRING:
			if( !tokens.testNextToken(Tokenizer.STRING))
				return null;
			v = new Value(tokens.nextToken());
			v = new Value(v.denormalize());
			break;

		case Value.TABLE:
			/* Get the array of metadata elements */
			Value array = parseValue();
			
			RecordStreamValue r = new RecordStreamValue(array.getElement(1));
			for( int rowindex = 2; rowindex <= array.size(); rowindex++ ) {
				r.addElement(array.getElement(rowindex));
			}
			v = r;
			break;
			
		case Value.ARRAY:
			v = new Value(Value.ARRAY, null);

			boolean unknownArraySize = false;
			if( count == 0 ) {
				count = 32768;
				unknownArraySize = true;
			}

			for( int i = 0; i < count; i++ ) {

				if( unknownArraySize ) {
					int m = tokens.getPosition();

					if( tokens.assumeNextSpecial("<")) 

						if( tokens.assumeNextSpecial("/")) {
							boolean isEnd = false;
							if( tokens.assumeNextToken("ARRAY"))
								isEnd = true;
							else
								if( tokens.assumeNextToken("METADATA"))
									isEnd = true;
							
							if( isEnd )
								if( tokens.assumeNextSpecial(">")) {
									status = new Status();
									return v;
								}
							return null;
						}
					tokens.setPosition(m);
				}
				Value arrayElement = parseValue();
				if( arrayElement == null)
					return null;
				v.addElementAsIs(arrayElement);
			}
			break;

		case Value.RECORD:

			/*
			 * Create an empty RECORD object.
			 */
			v = new Value(Value.RECORD, null );

			/*
			 * For as many keys as are in the record definition, parse each <KEY>..</KEY>
			 * pair and capture the values in the record using the NAME attribute of each
			 * KEY.
			 */
			if( count == 0 )
				count = 1000;

			for( int i = 0; i < count; i++ ) {

				/*
				 * Parse the <key> item
				 */

				status = new Status(Status.XML, "invalid RECORD key definition"
						+ " at " + tokens.getCharacterPosition());
				if( !tokens.assumeNextSpecial("<"))
					return null;

				String key = tokens.nextToken();
				if( key.equals("/")) {
					if( tokens.assumeNextToken("RECORD"))
						if( tokens.assumeNextSpecial(">")) {
							status = new Status();
							return v;
						}
					return null;
				}

				if( !tokens.assumeNextSpecial(">"))
					return null;

				/*
				 * Get the actual value of the record field.
				 */
				Value recordElement = parseValue();
				if( recordElement == null )
					return null;

				/*
				 * Check for closing </KEY>
				 */
				status = new Status(Status.XML, "mising/invalid RECORD closing tag definition"
						+ " at " + tokens.getCharacterPosition());
				if( !tokens.assumeNextSpecial("<"))
					return null;
				if( !tokens.assumeNextSpecial("/"))
					return null;
				if( !tokens.assumeNextToken(key))
					return null;
				if( !tokens.assumeNextSpecial(">"))
					return null;

				/*
				 * Store the value parsed using the name parsed in the record variable.
				 */
				v.setElement(recordElement, key);
			}
			break;

		default:
			status = new Status(Status.XML, "unknown Value type " + typeName
					+ " at " + tokens.getCharacterPosition());
		return null;
		}
		
		/*
		 * After that, we must see the closing type key.
		 */
		status = new Status(Status.XML, "missing/invalid closing " + typeName + " tag at "
				+ tokens.getCharacterPosition());

		if( !tokens.assumeNextSpecial("<"))
			return null;
		if( !tokens.assumeNextSpecial("/"))
			return null;

		if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
			return null;
		if( !tokens.nextToken().equalsIgnoreCase(typeName))
			return null;
		if( !tokens.assumeNextSpecial(">"))
			return null;

		/*
		 * All done, return the value.
		 */
		status = new Status();
		return v;
	}


	/**
	 * Format a Value as an XML string. The assumping is that
	 * the value is formatted with spacing and indentations for
	 * maximum readability.
	 * 
	 * @param rootName the  root tag to assign to the resulting XML
	 * @param value the Value to be converted to XML
	 * @return the String value containing a properly formatted XML
	 * specification of the current Value object.
	 */
	public String toXML(String rootName, Value value) {
		return toXML(rootName, value, 1, 0);
	}

	/**
	 * Format a Value as an XML string.
	 * @param rootName the  root tag to assign to the resulting XML
	 * @param value the Value to be formatted
	 * @param formatted if zero, no formatting is done.  If set to 1, formatting (spacing
	 * for indentation and line breaks) is included.  If set to 2, then the contents are
	 * encoded.
	 * @return String containing XML representation
	 */
	public String toXML(String rootName, Value value, int formatted) {
		return toXML(rootName, value, formatted, 0 );
	}
	/**
	 * Create an XML string that describes the value.
	 * @param rootName the  root tag to assign to the resulting XML
	 * @param value the value to be converted to XML
	 * @param formatted a boolean indicating if formatting is required
	 * @param depth the nested depth of this item, or zero top-level Value.
	 * @return String formatted as XML
	 */
	public String toXML( String rootName, Value value, int formatted, int depth ) {

		final int indent = 2;
		if( rootName == null )
			rootName = defaultRootValue;

		StringBuffer pad = new StringBuffer();
		char separator = ' ';
		if( formatted > 0 ) {
			for( int i = 0; i < depth*indent; i++ )
				pad.append(' ');
			separator = '\n';
		}

		StringBuffer result = new StringBuffer(pad.toString());
		if( depth == 0 ) {
			result.append('<');
			result.append(rootName);
			result.append('>');
			result.append(separator);
			
			if( formatted >= 2 ) {
				int keyNum = (int) (Math.random()*12071959);
				String key = encodedKey + Integer.toString(keyNum);
				String rawXML = toXML(rootName, value, 0, 1);
				SimpleCipher cipher = new SimpleCipher(theSession);
				cipher.setPassword(key);
				result.append("  <encoded key=\"" + keyNum + "\">\n    ");
				Value c = cipher.encrypt(new Value(rawXML));
				
				Value encodedData = CryptstrFunction.arrayToString(c);
				String s = encodedData.getString();
				int len = s.length();
				int p = 0;
				boolean hasReturn = false;

				while( p < len ) {
					result.append(s.charAt(p++));
					if( p % 40 == 0) {
						result.append("\n    ");
						hasReturn = true;
					}
					else
						hasReturn = false;
				}
				if( !hasReturn )
					result.append("\n");
				result.append("  </encoded>\n");
			}
			else 
				result.append(toXML(rootName, value, formatted, 1));
			result.append("</");
			result.append(rootName);
			result.append('>');
			return result.toString();
		}
		switch( value.getType()) {

		case Value.INTEGER:
			result.append("<Integer d=\"");
			result.append(Integer.toString(value.getInteger()));
			result.append("\"/>");
			result.append(separator);
			break;

		case Value.BOOLEAN:
			result.append("<Boolean d=\"");
			result.append(value.getBoolean()? "true" : "false");
			result.append("\"/>");
			result.append(separator);
			break;

		case Value.DOUBLE:
			double d = value.getDouble();
			final String ds = Double.toString(d);
			
			/* If the value can be represented exactly as a string,
			 * format it as such.
			 */
			if( Double.parseDouble(ds) == d  ) {
				result.append("<Double d=\"");
				result.append(ds);
				result.append("\"/>");
				result.append(separator);
			}
			
			/* Otherwise, make it a HexString which is less readable
			 * by humans but more exact.  Either can be parsed later
			 * from the XML data.
			 */
			else {
				result.append("<Double d=\"");
				result.append(Double.toHexString(d));
				result.append("\"/>");
				result.append(separator);
			}
			
			break;

		case Value.STRING:
			result.append("<String d=");
			result.append(Value.toString(value, true));
			result.append("/>");
			result.append(separator);
			break;

		case Value.ARRAY:
			int arraySize = value.size();
			if( arraySize == 0 ) {
				result.append("<Array/>");
				result.append(separator);
				break;
			}
			result.append("<Array>");
			result.append(separator);
			for( int i = 1; i <= arraySize; i++ ) {
				Value v = value.getElement(i);
				result.append(toXML(rootName, v, formatted, depth+1));
			}
			result.append(pad);
			result.append("</Array>");
			result.append(separator);
			break;

		case Value.TABLE:
			
			RecordStreamValue table = (RecordStreamValue) value;
			StringBuffer pad3 = new StringBuffer(pad.toString());
			if( formatted == 1 )
				for( int i = 0; i < indent; i++ )
					pad3.append(' ');
			StringBuffer pad4 = new StringBuffer(pad3.toString());
			if( formatted == 1 )
				for( int i = 0; i < indent; i++ )
					pad4.append(' ');
			StringBuffer pad5 = new StringBuffer(pad4.toString());
			if( formatted == 1 )
				for( int i = 0; i < indent; i++ )
					pad5.append(' ');
			result.append("<Table>");
			result.append(separator);
			result.append(pad3);
			result.append("<Array>");
			result.append(separator);
			Value metadata = table.columnNames();
			result.append(pad4);
			result.append("<Metadata>");
			result.append(separator);
			for( int c = 1; c <= metadata.size(); c++ ) {
				String item = metadata.getString(c).toUpperCase();
				result.append(pad5);
				result.append("<Column name=\"");
				result.append(item.substring(0, item.indexOf('@')));
				result.append("\" type=\"");
				String typeName = item.substring(item.indexOf('@')+1);
				result.append(Utility.mixedCase(typeName));
				result.append("\">");
				result.append(separator);
			}
			result.append(pad4);
			result.append("</Metadata>");
			result.append(separator);

			//result.append(toXML(rootName, table.columnNames(), formatted, depth+2));
			
			for( int i = 1; i <= table.size(); i++ ) {
				Value v = table.getElementAsArray(i);
				//result.append(separator);
				result.append(toXML(rootName, v, formatted, depth+2));
			}
			result.append(pad3);
			result.append("</Array>");
			result.append(separator);
			result.append(pad);
			result.append("</Table>");
			result.append(separator);

			
			break;
			
		case Value.RECORD:
			
			if( value.size() == 0 ) {
				result.append("<Record/>");
				result.append(separator);
				break;
			}
			StringBuffer pad2 = new StringBuffer(pad.toString());
			for( int i = 0; i < indent; i++ )
				pad2.append(' ');

			result.append("<Record>"); 
			result.append(separator);
			ArrayList keys = value.recordFieldNames();
			for( int i = 0; i < keys.size(); i++ ) {
				String key = (String) keys.get(i);
				Value v = value.getElement(key);
				result.append(pad2);
				result.append("<");
				result.append(key);
				result.append(">");
				result.append(separator);
				result.append(toXML(rootName, v, formatted, depth+2));
				result.append(pad2);
				result.append("</");
				result.append(key);
				result.append(">");
				result.append(separator);
			}
			result.append(pad);
			result.append("</Record>");
			result.append(separator);
			break;

		}

		return result.toString();
	}


	/**
	 * Read as much as required from an input file to construct an XML string.
	 * <p>
	 * This is implemented as a finite-state-machine that processes
	 * syntactically valid XML strings to find all the nested tags,
	 * elements, and data and find the end of the outer-most tag set.
	 * <p>
	 * The file pointer is advanced past the end of the valid XML
	 * string, and the result is a string object that contains all
	 * the XML text that was read in from the file.
	 * @param session The controlling session, used for messaging
	 * @param inputFile The input file to be read (may be a console or a file)
	 * @param debug If true, the debug messages are generated to track the
	 * execution of the finite state machine that parses the XML
	 * @return a String containing the XML collected from the file.
	 * @throws JBasicException if an end-of-file occurs
	 */
	public String readXML(JBasic session, JBFInput inputFile, boolean debug)
							throws JBasicException {

		/*
		 * Gather up string data such that we have a valid XML item to 
		 * parse from the input stream.  We do this with a very simple
		 * FSM that handles detecting when we've found the start and
		 * end tags, and captured what goes between them.
		 */

		StringBuffer xml = new StringBuffer();
		String rawData = null;
		Tokenizer tokens = new Tokenizer("");

		final int S_OPENTAG = 0;
		final int S_TAGNAME = 1;
		final int S_CLOSETAG = 2;
		final int S_ATTRIBUTE = 3;
		final int S_EQUALS = 4;
		final int S_VALUE = 5;
		final int S_INCOMMENT = 6;
		final int M_FINDTAG = 10;
		final int M_ENDTAG = 11;
		final int M_DONE = 12;

		int state = S_OPENTAG;
		int mode = M_FINDTAG;

		boolean capture = false;
		boolean foundRoot = false;
		boolean first = true;
		int lastType = Tokenizer.END_OF_STRING;
		String rootTag = null;

		while (true) {

			/*
			 * If we've exhausted all the tokens so far, get more
			 */
			if (tokens.testNextToken(Tokenizer.END_OF_STRING)) {
				if (!first & inputFile.isConsole())
					session.stdout.print("XML? ");

				rawData = inputFile.read();
				first = false;
				if (rawData == null) {
					throw new JBasicException(Status.EOF);
				}
				tokens.loadBuffer(rawData);
			}

			/*
			 * If the user enters .END and we're reading from a console, break the
			 * processing now. 
			 */
			String t = tokens.nextToken();
			if (t.equals(".") & tokens.peek(0).equals("END"))
				if( inputFile.isConsole())
					break;

			/*
			 * If the debug flag was set for us, dump the current state of the 
			 * FSM before processing each token.
			 */
			if (debug) {
				session.stdout.print("DEBUG XML: ");
				session.stdout.print("state=" + state);
				session.stdout.print(" mode=" + mode);
				session.stdout.print(" foundroot=" + foundRoot);
				session.stdout.print(" token=" + t);
				session.stdout.println(" type=" + tokens.getType());
			}

			switch (state) {

			/*
			 * Get an opening tag character
			 */
			case S_OPENTAG:
				if (tokens.getType() != Tokenizer.SPECIAL)
					break;
				if (!t.equals("<"))
					break;
				lastType = Tokenizer.SPECIAL;

				if( tokens.assumeNextSpecial("?")) {
					xml.append("<");
					t = "?";
					mode = M_FINDTAG;
					state = S_INCOMMENT;
					break;
				}
				if (tokens.assumeNextSpecial("/")) {
					xml.append("<"); /* Have to do this because just */
					t = "/"; /* consumed the "/" token */
					lastType = Tokenizer.SPECIAL;
					mode = M_ENDTAG;
				} else
					mode = M_FINDTAG;

				state = S_TAGNAME;
				break;

			case S_INCOMMENT:
				if( tokens.getType() != Tokenizer.SPECIAL)
					break;
				if( !t.equals("?"))
					break;
				lastType = Tokenizer.SPECIAL;
				if( tokens.assumeNextSpecial(">")) {
					xml.append("?");
					t = ">";
					mode = M_FINDTAG;
					state = S_OPENTAG;
					break;
				}
				break;
				/*
				 * Get the tag name (first token inside a <> tag string)
				 */
			case S_TAGNAME:
				if (tokens.getType() != Tokenizer.IDENTIFIER) {
					state = S_OPENTAG;
					mode = M_FINDTAG;
					break;
				}

				if (rootTag == null)
					rootTag = t;

				if (rootTag.equalsIgnoreCase(t)) {
					foundRoot = true;
					if (!capture) {
						capture = true;
					}
					state = S_CLOSETAG;
				} else {
					state = S_OPENTAG;
					foundRoot = false;
					mode = M_FINDTAG;
				}
				break;

			case S_EQUALS:
				if (tokens.getType() == Tokenizer.SPECIAL & t.equals("="))
					state = S_VALUE;
				else
					state = S_OPENTAG;
				break;

			case S_VALUE:
				if (tokens.getType() == Tokenizer.STRING)
					state = S_CLOSETAG;
				else
					state = S_OPENTAG;
				break;

			case S_ATTRIBUTE:

				/* Might just be end of tag, only process if attribute is
				 * here.  Else just fall through to next state for looking
				 * at close tag.
				 */
				if (!(tokens.getType() == Tokenizer.SPECIAL & t.equals(">"))) {
					if (tokens.getType() == Tokenizer.IDENTIFIER)
						/* It's an attribute name */
						state = S_EQUALS;
					else
						/* Not an attribute, looks like bogus XML.  Try to
						 * find another tag.
						 */
						state = S_OPENTAG;
					break;
				}
			case S_CLOSETAG:
				if (tokens.getType() != Tokenizer.SPECIAL)
					break;
				if (!t.equals(">")) {
					state = S_OPENTAG;
					break;
				}
				state = S_OPENTAG;

				if (foundRoot & (mode == M_ENDTAG))
					mode = M_DONE;
				break;

			}

			/*
			 * Write the token to the output string that we accumulate the
			 * parsed XML code in.  A special case is a string which needs
			 * to be re-quoted and have escape characters converted to 
			 * printable format.
			 */
			if (tokens.getType() == Tokenizer.STRING) {
				xml.append('"');
				xml.append(t);
				xml.append('"');
				lastType = Tokenizer.STRING;
			} else {
				int thisType = tokens.getType();
				if (lastType == Tokenizer.IDENTIFIER
						& (thisType == Tokenizer.INTEGER
								|| thisType == Tokenizer.IDENTIFIER
								|| t.equals("$") || t.equals("_")))
					xml.append(' ');
				if (lastType == Tokenizer.STRING
						& thisType == Tokenizer.IDENTIFIER)
					xml.append(' ');
				lastType = thisType;
				xml.append(t);
			}
			/*
			 * IF we found everything we need (a complete matching root tag)
			 * then we don't parse any more.
			 */
			if (mode == M_DONE)
				break;
		}

		if (debug)
			session.stdout.println("DEBUG XML: completed scan");

		/*
		 * We have to find the next token in the file.  We do this by reading 
		 * lines from the file until we have a non-empty line.  This will
		 * satisfy the requirement that we can detect EOF() after an XML
		 * read correctly even if there are one or more blank lines after
		 * the last XML item.
		 */

		String remainder = tokens.getBuffer().trim();

		if (inputFile.isConsole()) {
			if( remainder.length() > 0 )
				inputFile.setReadAheadBuffer(remainder);
		} else {
			if (remainder.length() == 0) {
				while (true) {
					rawData = inputFile.read();
					if (rawData == null) {
						remainder = null;
						break;
					}
					if (rawData.trim().length() > 0) {
						remainder = rawData;
						break;
					}
				}
			}

			inputFile.setReadAheadBuffer(remainder);
		}
		return xml.toString();
	}
}

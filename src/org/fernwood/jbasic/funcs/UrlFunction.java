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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>URL()</b> JBasic Function
 * <p>
 * <table>
 * <tr>
 * <td><b>Description:</b></td>
 * <td>Parse a URL.</td>
 * </tr>
 * <tr>
 * <td><b>Invocation:</b></td>
 * <td><code>r = URL( <em>url-string</em> )</code></td>
 * </tr>
 * <tr>
 * <td><b>Returns:</b></td>
 * <td>Record</td>
 * </tr>
 * </table>
 * <p>
 * Create a RECORD object that contains information known about the given URL
 * string
 * <p>
 * Items returned in the record include:
 * <p>
 * <table>
 * <tr><td><b>Item</b></td><td><b>Description</b></td></tr>
 * <tr><td>HOST</td><td>The host name</td></tr>
 * <tr><td>PATH</td><td>Path info following the host name</td></tr>
 * <tr><td>PROTOCOL</td><td>The protocol (HTTP, FTP, etc.)</td></tr>
 * <tr><td>QUERY</td><td>The query parsed as a record of arrays</td></tr>
 * <tr><td>QUERYSTRING</td><td>The original query string</td></tr>
 * <tr><td>USERINFO</td><td>User info preceding the host name</td></tr>
 * </table>
 * @author cole
 * 
 */
public class UrlFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist
	 *            the function argument list and count already popped from the
	 *            runtime data stack
	 * @param symbols
	 *            the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  An error in the count or type of argument
	 * occurred, or the URL is incorrectly formed.
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols)
	throws JBasicException {
		arglist.validate(1, 2, new int[] { Value.STRING });

		URL url = null;
		URL context = null;

		/*
		 * See if there is a context URL we must first process.
		 */
		if( arglist.size() == 2 ) try {
			context = new URL(arglist.stringElement(0));
			url = new URL(context, arglist.stringElement(1));
		} catch (MalformedURLException e) {
			throw new JBasicException(Status.SYNTAX, new Status(Status.BADURL,
					e.getMessage()));
		}
		else
			/*
			 * No context URL first, so just process the URL as the primary
			 * URL string.
			 */
			try {
				url = new URL(arglist.stringElement(0));
			} catch (MalformedURLException e) {
				throw new JBasicException(Status.SYNTAX, new Status(Status.BADURL,
						e.getMessage()));
			}

			/*
			 * The result of this is a RECORD describing all the fields of the
			 * URL as individual members.
			 */
			Value result = new Value(Value.RECORD, null);

			String s = null;
			String none = "";

			/*
			 * Protocol; i.e. HTTP or FTP
			 */
			s = url.getProtocol();
			if (s == null)
				s = none;
			result.setElement(s, "PROTOCOL");

			/*
			 * Host name; i.e. apple.com or websrv01.cnn.com
			 */
			s = url.getHost();
			if (s == null)
				s = none;
			result.setElement(s, "HOST");

			/*
			 * Path; i.e. /content/index.jsp or /index.html
			 */
			s = url.getPath();
			if (s == null)
				s = none;
			result.setElement(s, "PATH");

			/*
			 * User info; i.e. tomcole or admin:password
			 */
			s = url.getUserInfo();
			if (s == null)
				s = none;
			result.setElement(s, "USERINFO");

			/*
			 * The query arguments following the path; i.e. "range=5&bob=Y"
			 */
			s = url.getQuery();
			if (s == null)
				s = none;
			result.setElement(s, "QUERYSTRING");

			/*
			 * The individual query elements as a sub-record containing an
			 * array for each value (the keys can be given more than once
			 * so there must be arrays of values.  For example,
			 * 
			 *     ?index=5&format=Y&key=HELLO&key=BYE&quote=%22
			 *     
			 * { FORMAT: [ "Y" ], KEY: [ "HELLO", "BYE" ], QUOTE: "\"" }
			 * 
			 * Note that the hex notation for a character is processed here.
			 * Also, a "+" standing alone is treated as whitespace.
			 */

			result.setElement( parseQueryString(s), "QUERY");

			return result;
	}

	/**
	 * Parse a string containing URL query elements and return a record
	 * describing them.
	 * <p>
	 * The individual query elements as a sub-record containing an array for
	 * each value (the keys can be given more than once so there must be arrays
	 * of values. For example,
	 * <p>
	 * <code>
	 * ?index=5&format=Y&key=HELLO&key=BYE&quote=%22</code>
	 * <p>
	 * 
	 * when processed will result in the record value
	 * <p>
	 * <code>{ FORMAT: [ "Y" ], KEY: [ "HELLO", "BYE" ], QUOTE: "\"" }</code>
	 * <p>
	 * 
	 * Note that the hex notation for a character is processed here. Also, a "+"
	 * standing alone is treated as whitespace.
	 * 
	 * <p>
	 * This code adapted from examples posted to forums.sun.com Java developer
	 * forum, thread 272970
	 * @param queryString the String containing the query to parse
	 * @return a RECORD value containing the results
	 * @throws JBasicException if there is a syntax error
	 */
	private Value parseQueryString(String queryString) throws JBasicException {

		if (queryString == null)
			throw new JBasicException(Status.ARGNOTGIVEN, "query");

		Value v = new Value(Value.RECORD, null);
		Value valArray = new Value(Value.ARRAY, null);

		StringBuffer sb = new StringBuffer();
		String key;

		for (StringTokenizer st = new StringTokenizer(queryString, "&"); st
		.hasMoreTokens(); v.setElement(valArray, key.toUpperCase())) {
			String pair = st.nextToken();
			int pos = pair.indexOf('=');
			if (pos == -1)
				throw new JBasicException(Status.ARGERR);
			key = parseName(pair.substring(0, pos), sb);
			String val = parseName(pair.substring(pos + 1, pair.length()), sb);

			if (v.getElement(key) != null) {
				valArray.addElement(new Value(val));
			} else {
				valArray = new Value(Value.ARRAY, null);
				valArray.addElement(new Value(val));
			}
		}

		return v;
	}

	/**
	 * Parse a name item from the query string. This can be the keyword or
	 * its value.  In either case, a plus-sign ("+") character can be used
	 * to represent a white-space, and a hex notation ("%<em>nn</em>") can be used
	 * to indicate a hex character for <em>nn</em>.
	 * <p>
	 * This code adapted from examples posted to forums.sun.com Java developer
	 * forum, thread 272970
	 * @param queryString The string to convert
	 * @param workingBuffer a StringBuffer provided by the caller so it is
	 * allocated only once.
	 * @return a String containing the processed value
	 * @throws JBasicException if an invalid hex character representation is given
	 */
	private String parseName(String queryString, StringBuffer workingBuffer) throws JBasicException {
		workingBuffer.setLength(0);
		for (int i = 0; i < queryString.length(); i++) {
			char c = queryString.charAt(i);
			switch (c) {
			case 43: // '+'
				workingBuffer.append(' ');
				break;

			case 37: // '%'
				try {
					workingBuffer.append((char) Integer.parseInt(
							queryString.substring(i + 1, i + 3), 16));
					i += 2;
					break;
				} catch (NumberFormatException e) {
					throw new JBasicException(Status.ARGERR);
				} catch (StringIndexOutOfBoundsException e) {
					String rest = queryString.substring(i);
					workingBuffer.append(rest);
					if (rest.length() == 2)
						i++;
				}
				break;

			default:
				workingBuffer.append(c);
				break;
			}
		}

		return workingBuffer.toString();
	}
}
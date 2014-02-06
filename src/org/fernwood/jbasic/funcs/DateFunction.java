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

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>DATE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Format a date value.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = DATE( [ d [,<em>format-string</em>]] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 
 * Format the given date_value as a date string. If the date value is a
 * double-precision value, it contains a date/time value that is formatted
 * as a string and returned.  The default formatting is used unless a
 * format string is given as the second parameter.  IF the first value is
 * omitted or is zero, then the current date/time is formatted.
 * <p>
 * If the first argument is a string, then it is a string buffer containing
 * a date time representation that is parsed according to the pattern given
 * in the second parameter.  If the parse cannot be done successfully due
 * to an invalid input value then a JBasic error is signaled.
 * <p>
 * If the format pattern is invalid, then an ARGERR error is signaled as well.
 * The pattern is passed to the java.text.SimpleDateFormat class and its
 * rules for the pattern apply..
 * @see java.text.SimpleDateFormat
 * 
 * @author cole
 *
 */
public class DateFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException if an argument or execution error occurs
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(0, 2, new int [] { Value.UNDEFINED, Value.STRING});
		String dateString = null;
		double dateTimeValue = 0.0;
		Date rightNow = new Date();
		boolean toText = true;

		/*
		 * If there is at least one argument, get the date/time value in the
		 * first position.  This is either a string (meaning text-to-date
		 * conversion) or a number (meaning date-to-text conversion).
		 */
		if (arglist.size() > 0) {

			Value dateValue = arglist.element(0);
			if( dateValue.getType() == Value.STRING ) {
				toText = false;
				dateString = dateValue.getString().trim();
				if( dateString.toUpperCase().equals("TODAY") & arglist.size() == 1)
					return new Value(rightNow.getTime());
			}
			else {
				dateTimeValue = arglist.doubleElement(0);
				if( Math.abs(dateTimeValue) > 0.01)
					rightNow.setTime((long) arglist.doubleElement(0));
			}
		}

		/*
		 * If there is a second argument, then it contains the pattern
		 * value.  If it is not given explicitly, use the default value.
		 */
		String formatPattern = null;
		if( arglist.size() > 1 )
			formatPattern = arglist.stringElement(1);
		else
			formatPattern = "EEE, d MMM yyyy HH:mm:ss Z";

		/*
		 * Special case.  If we're converting to text, but the format is
		 * actually an asterisk, then we really generate a record with
		 * all the date components expressed as fields.  This makes it
		 * easier to process in a program that needs specific parts of 
		 * the date information.  This could be done as easily by using
		 * a list of format types in iterations with the date() function
		 * to extract each part, but this is generally easier.
		 */
		if( toText & (formatPattern.equals("*") || formatPattern.length() == 0)) {
			Calendar now = Calendar.getInstance();
			now.setTime(rightNow);
			
			Value result = new Value(Value.RECORD, null);
			
			result.setElement(new Value(now.get(Calendar.AM_PM)), "AM_PM");
			result.setElement(new Value(now.get(Calendar.MONTH)), "MONTH");
			result.setElement(new Value(now.get(Calendar.DATE)), "DATE");
			result.setElement(new Value(now.get(Calendar.DAY_OF_MONTH)), "DAY_OF_MONTH");
			result.setElement(new Value(now.get(Calendar.DAY_OF_WEEK)), "DAY_OF_WEEK");
			result.setElement(new Value(now.get(Calendar.DAY_OF_WEEK_IN_MONTH)), "DAY_OF_WEEK_IN_MONTH");
			result.setElement(new Value(now.get(Calendar.DAY_OF_YEAR)), "DAY_OF_YEAR");
			result.setElement(new Value(now.get(Calendar.DST_OFFSET)), "DST_OFFSET");
			result.setElement(new Value(now.get(Calendar.ERA)), "ERA");
			result.setElement(new Value(now.get(Calendar.HOUR)), "HOUR");
			result.setElement(new Value(now.get(Calendar.HOUR_OF_DAY)), "HOUR_OF_DAY");
			result.setElement(new Value(now.get(Calendar.MILLISECOND)), "MILLISECOND");
			result.setElement(new Value(now.get(Calendar.MINUTE)), "MINUTE");
			result.setElement(new Value(now.get(Calendar.MONTH)), "MONTH");
			result.setElement(new Value(now.get(Calendar.SECOND)), "SECOND");
			result.setElement(new Value(now.get(Calendar.WEEK_OF_MONTH)), "WEEK_OF_MONTH");
			result.setElement(new Value(now.get(Calendar.WEEK_OF_YEAR)), "WEEK_OF_YEAR");
			result.setElement(new Value(now.get(Calendar.YEAR)), "YEAR");
			result.setElement(new Value(now.get(Calendar.ZONE_OFFSET)), "ZONE_OFFSET");

			return result;
		}
		/*
		 * Process the format string by applying it to the format object.
		 * This will throw an error if the format string is invalid.
		 */
		SimpleDateFormat fmt = new SimpleDateFormat();
		try {
			fmt.applyPattern(formatPattern);
		}
		catch (Exception e ) {
			throw new JBasicException(Status.FORMATERR, "invalid format string, " + 
					e.getMessage());
		}
		
		/*
		 * If we are converting to text, do that now. There aren't any errors for the
		 * formatting here.
		 */
		if( toText) {
			dateString = fmt.format(rightNow);
			return new Value(dateString);
		}
		
		/*
		 * No, we are parsing a string.  Create a parse position object to track
		 * where we get to in the string, and try to parse the value.
		 */
		ParsePosition pos = new ParsePosition(0);
		rightNow = fmt.parse(dateString, pos);
		
		/*
		 * If there was an error, the error position will be set and we can report
		 * this as where the problem was.
		 */
		if( pos.getErrorIndex() >= 0)
			throw new JBasicException(Status.SYNTAX,
					new Status(Status.DATETIME, dateString.substring(pos.getErrorIndex())));
		
		/*
		 * If there is unprocessed text at the end of the string then this is an
		 * error indicating bogus format as well.
		 */
		if( pos.getIndex() < dateString.length())
			throw new JBasicException(Status.EXTRA, dateString.substring(pos.getIndex()));
		
		/*
		 * Else return the dateTime value as a double.
		 */
		return new Value(rightNow.getTime());
	}

}

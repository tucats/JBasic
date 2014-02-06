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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;

/**
 * SLEEP statement handler. Causes the current program(thread) to pause
 * execution for a given number of seconds (expressed as a floating
 * point value).
 * 
 * @author cole
 * @version 1.0 March, 2009
 * 
 */

class SleepStatement extends Statement {

	/**
	 * Compile 'sleep' statement.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 * @return A Status value indicating if the THREAD statement was compiled
	 *         successfully.
	 */

	public Status compile(final Tokenizer tokens) {

		byteCode = new ByteCode(session, this);

		Expression exp = new Expression(session);
		status = exp.compile(byteCode, tokens);
		if (status.failed())
			return status;
		
		
		/*
		 * There may be an optional unit specification that is
		 * an implicit multiplier.  If the unit is greater than
		 * one or less than one, there is an explicit unit. If
		 * the result is exactly 1 then the unit is already in
		 * seconds.
		 */
		
		double multiplier = parseTimeUnit(tokens);
		
		if( multiplier > 1.0 )
			byteCode.add(ByteCode._MULTI, (int) multiplier);
		if( multiplier <  1.0 ) {
			byteCode.add(ByteCode._DOUBLE, multiplier);
			byteCode.add(ByteCode._MULT);
		}
		
		/*
		 * Generate the code to do the actual sleep operation.
		 */
		byteCode.add(ByteCode._SLEEP);
		return status = new Status();

	}

	/**
	 * Parse and optional time unit ("SECONDS", "DAY") and return the
	 * proper multiplier that converts seconds into the requested unit.
	 * If no unit is given, then seconds is assumed and the result is
	 * always 1.0.
	 * 
	 * @param tokens the Tokenizer stream to parse
	 * @return a double containing the multiplier needed to convert 
	 * seconds in the given unit.
	 */
	private double parseTimeUnit(final Tokenizer tokens) {

		class UnitMap {
			double factor;
			String[] unitName;
			
			UnitMap( double d, String[] seconds ) {
				factor = d;
				unitName = seconds;
			}
		};
		
		final UnitMap map[] = { new UnitMap(0.001,	new String [] { "MILLISECONDS", "MILLISECOND", "MS", "MILLI"}),
								new UnitMap(1.0, 	new String [] {"SECONDS", "SECOND", "SEC", "SECS"}),
								new UnitMap(60,		new String [] {"MINUTES", "MINUTE", "MINS", "MIN"}),
								new UnitMap(3600,	new String [] {"HOURS", "HOUR"}),
								new UnitMap(86400,  new String [] {"DAYS", "DAY"}),
								new UnitMap(604800, new String [] {"WEEKS", "WEEK"})
		};
		
		double multiplier = 1.0;
		
		for( UnitMap mapElement : map) {
			if( tokens.assumeNextToken(mapElement.unitName))
				multiplier = mapElement.factor;
		}
		
		return multiplier;
	}

}
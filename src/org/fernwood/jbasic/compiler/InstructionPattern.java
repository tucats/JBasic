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
 * COPYRIGHT 2003-2007 BY TOM COLE, TOMCOLE@USERS.SF.NET
 *
 * Created on Apr 19, 2011 by tom
 *
 */
package org.fernwood.jbasic.compiler;

import java.util.HashMap;
import java.util.Iterator;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.opcodes.AbstractOpcode;
import org.fernwood.jbasic.runtime.Instruction;

/**
 * This class represents a pattern for a single instruction.  For each
 * field of an instruction (opcode, integer, double, and string arguments)
 * this indicates how each field is tested.  
 * 
 * @author cole
 * @version version 1.0 Feb 21, 2011
 *
 */
class InstructionPattern {

	final static int MATCH_ANY = 0;
	final static int MATCH_EXACT = 1;
	final static int MATCH_RANGE = 2;
	final static int MATCH_MISSING = 3;
	final static int MATCH_NOT = 4;

	String encoding;
	
	int action[];
	int actionCount;

	int opcodeMatch, opcodeMin, opcodeMax;
	int intMatch, intMin, intMax;
	int doubleMatch;
	double doubleMin, doubleMax;
	int stringMatch;
	String stringMin, stringMax;
	Status status;

	/**
	 * Define a single instruction pattern by string specification
	 * 
	 *    <opcode>,i,d,s
	 * 
	 * where i, d, and s are:
	 * 
	 * "-"         parameter must be missing
	 * "*"         any value accepted
	 * <value>     The actual value required
	 * value:value The range of values required
	 * 
	 * @param spec The specification of the instruction pattern
	 * @param dictionary The dictionary of pre-defined action names
	 */
	public InstructionPattern( String spec, HashMap<String,Integer> dictionary) {
		Tokenizer t = new Tokenizer(spec);
		actionCount = 0;
		action = new int[5];
		encoding = spec;
		status = new Status();
		
		String opCode = t.nextToken();
		opcodeMin = opcodeMax = AbstractOpcode.getCode(opCode);
		opcodeMatch = MATCH_EXACT;

		/*
		 * There are two models, one which is heavily positional and one that allows
		 * for parameters in any order.  See what we're doing here.  The variable 
		 * format allows for the parameters to be identified with a prefix.
		 */
		intMatch = MATCH_MISSING;
		doubleMatch = MATCH_MISSING;
		stringMatch = MATCH_MISSING;
		StringBuffer fs = new StringBuffer();

		if( t.endOfStatement() || t.testNextToken("I") || t.testNextToken("D") || t.testNextToken("S") || t.testNextToken("@")) {
			
			while( true ) {
				if( t.endOfStatement() || t.testNextToken("@"))
					break;
				String code = t.nextToken();
				int match = MATCH_MISSING;
				String min = null;
				String max = null;

				if( !t.assumeNextSpecial("(")) {
					status = new Status(Status.PAREN);
					break;
				}
				if( t.assumeNextSpecial("-"))
					match = MATCH_MISSING;
				else
					if( t.assumeNextSpecial("*"))
						match= MATCH_ANY;
					else
						if( t.assumeNextSpecial("!")) {
							match = MATCH_NOT;
							min = t.nextToken();
						}
						else {
							min = t.nextToken();
							if( t.assumeNextSpecial(":")) {
								match = MATCH_RANGE;
								min = t.nextToken();
							}
							else {
								match = MATCH_EXACT;
								max = min;
							}
						}
				
				if( !t.assumeNextSpecial(")")) {
					status = new Status(Status.PAREN);
					break;
				}
				if( code.equals("I")) {
					intMin = min == null ? 0 : Integer.parseInt(min);
					intMax = max == null ? 0 : Integer.parseInt(max);
					intMatch = match;
				}
				else if( code.equals("D")) {
					doubleMin = min == null ? 0.0 : Double.parseDouble(min);
					doubleMax = max == null ? 0.0 : Double.parseDouble(max);
					doubleMatch = match;
				}
				else if( code.equals("S")) {
					stringMin = min;
					stringMax = max;
					stringMatch = match;
				}
			}
			
			/*
			 * All done, reconstruct the string as appropriate
			 */
			
			fs.append(opCode);
			fs.append(' ');
			switch(intMatch) {
			case MATCH_MISSING:
				fs.append("-,");
				break;
			case MATCH_ANY:
				fs.append("*,");
				break;
			case MATCH_NOT:
				fs.append("!");
				fs.append(Integer.toString(intMin));
				break;
			case MATCH_EXACT:
				fs.append(Integer.toString(intMin));
				break;
			case MATCH_RANGE:
				fs.append(Integer.toString(intMin));
				fs.append(":");
				fs.append(Integer.toString(intMax));
				break;
			}
			switch(doubleMatch) {
			case MATCH_MISSING:
				fs.append("-,");
				break;
			case MATCH_ANY:
				fs.append("*,");
				break;
			case MATCH_NOT:
				fs.append("!");
				fs.append(Double.toString(doubleMin));
				break;
			case MATCH_EXACT:
				fs.append(Double.toString(doubleMin));
				break;
			case MATCH_RANGE:
				fs.append(Double.toString(doubleMin));
				fs.append(":");
				fs.append(Double.toString(doubleMax));
				break;
			}
			switch(stringMatch) {
			case MATCH_MISSING:
				fs.append("- ");
				break;
			case MATCH_ANY:
				fs.append("* ");
				break;
			case MATCH_NOT:
				fs.append("!\"");
				fs.append(stringMin);
				fs.append("\"");
				break;
			case MATCH_EXACT:
				fs.append("\"");
				fs.append(stringMin);
				fs.append("\"");
				break;
			case MATCH_RANGE:
				fs.append("\"");
				fs.append(stringMin);
				fs.append("\":\"");
				fs.append(stringMax);
				fs.append("\"");
				break;
			}
		} else {

		/*
		 * Nope, use the positional values, where the first one must be the integer,
		 * the second the double, and the third the string.
		 */
		/*
		 * Integer value
		 */
		if( t.assumeNextSpecial("-"))
			intMatch = MATCH_MISSING;
		else
			if( t.assumeNextSpecial("*"))
				intMatch = MATCH_ANY;
			else
				if( t.assumeNextSpecial("!")) {
					intMax = Integer.parseInt(t.nextToken());
					intMin = intMax;
					intMatch = MATCH_NOT;
				}
				else {
					intMin = Integer.parseInt(t.nextToken());
					if( t.assumeNextSpecial(":")) {
						intMax = Integer.parseInt( t.nextToken());
						intMatch = MATCH_RANGE;
					} else {
						intMax = intMin;
						intMatch = MATCH_EXACT;
					}
				}

		/*
		 * Double value
		 */
		t.assumeNextSpecial(",");

		if( t.assumeNextSpecial("-"))
			doubleMatch = MATCH_MISSING;
		else
			if( t.assumeNextSpecial("*"))
				doubleMatch = MATCH_ANY;
			else
				if( t.assumeNextSpecial("!")) {
					doubleMax = Double.parseDouble(t.nextToken());
					doubleMin = doubleMax;
					doubleMatch = MATCH_NOT;
				}
				else {
					doubleMin = Double.parseDouble(t.nextToken());
					if( t.assumeNextSpecial(":")) {
						doubleMax = Double.parseDouble(t.nextToken());
						doubleMatch = MATCH_RANGE;
					} else {
						doubleMax = intMin;
						doubleMatch = MATCH_EXACT;
					}
				}

		/*
		 * String value
		 */
		t.assumeNextSpecial(",");
		if( t.assumeNextSpecial("-"))
			stringMatch = MATCH_MISSING;
		else
			if( t.assumeNextSpecial("*"))
				stringMatch = MATCH_ANY;
			else
				if( t.assumeNextSpecial("!")) {
					stringMin = t.nextToken();
					stringMax = stringMin;
					stringMatch = MATCH_NOT;
				}
				else {
					stringMin = t.nextToken();
					if( t.assumeNextSpecial(":")) {
						stringMax = t.nextToken();
						stringMatch = MATCH_RANGE;
					} else {
						stringMax = stringMin;
						stringMatch = MATCH_EXACT;
					}
				}
		}
		while( t.assumeNextSpecial("@")) {
			fs.append(" @");
			if( dictionary != null && t.testNextToken(Tokenizer.IDENTIFIER)) {
				String key = t.nextToken();
				Integer i = dictionary.get(key);
				fs.append(key);
				int act = 0;
				if( i == null)
					status = new Status(Status.FAULT, "unknown optimizer action, " + key);
				else
					act = i.intValue();
				if( act == 0 )
					status = new Status(Status.FAULT, "unknown optimizer action " + key );
				
				action[actionCount++] = act;
			}
			else
				try {
					String a = t.nextToken();
					fs.append(a);
					action[actionCount++] = Integer.parseInt(a);
				} catch( Exception e ) {
					status = new Status(Status.FAULT, "error parsing action, " + e);
					action[actionCount] = 0;
				}
		}
		
		//if( fs.length() > 0 )
		//	System.out.println("I: " + this.toString());

	}

	/**
	 * Convert the current InstructionPattern to a display string.
	 * @return string representation of the current pattern and its actions.
	 */
	public String toString() {

		StringBuffer s = new StringBuffer();
		s.append(AbstractOpcode.getName(opcodeMin));
		while( s.length() < 10)
			s.append(' ');

		switch(intMatch) {
		case MATCH_ANY:
			s.append(" I(*)");
			break;
		case MATCH_EXACT:
			s.append(" I(" + Integer.toString(intMin) +")");
			break;
		case MATCH_NOT:
			s.append(" I(!" + Integer.toString(intMin)+")");
			break;
		case MATCH_RANGE:
			s.append(" I(" + Integer.toString(intMin) + ":" + Integer.toString(intMax) + ")");
			break;
		}


		switch(doubleMatch) {
		case MATCH_ANY:
			s.append(" D(*)");
			break;
		case MATCH_EXACT:
			s.append(" D(" + Double.toString(doubleMin) +")");
			break;
		case MATCH_NOT:
			s.append(" D(!" + Double.toString(doubleMin)+")");
			break;
		case MATCH_RANGE:
			s.append(" D(" + Double.toString(doubleMin) + ":" + Double.toString(doubleMax) + ")");
			break;
		}

		switch(stringMatch) {
		case MATCH_ANY:
			s.append(" S(*)");
			break;
		case MATCH_EXACT:
			s.append(" S(\"" + stringMin +"\")");
			break;
		case MATCH_NOT:
			s.append(" S(!\"" + stringMin +"\")");
			break;
		case MATCH_RANGE:
			s.append(" S\"" + stringMin + "\":\"" + stringMax + "\")");
			break;
		}

		while( s.length() < 24)
			s.append(' ');

		for( int ax = 0; ax < this.actionCount; ax++ ) {
			s.append(" @");
			s.append(actionName(this.action[ax]));
		}

		while( s.length() < 50)
			s.append(' ');

		return s.toString();

	}
	
	/**
	 * Given an action code, get the name for the code if it's in the
	 * action dictionary, else just format it as an integer.
	 * @param i
	 * @return
	 */
	private String actionName(int i) {
		
		Iterator ax = PatternOptimizer.actionDictionary.keySet().iterator();
		
		while( ax.hasNext()) {
			String key = (String) ax.next();
			Integer an = PatternOptimizer.actionDictionary.get(key);
			if( an.intValue() == i)
				return key;
		}
		return Integer.toString(i);
	}

	/**
	 * Determine if a given instruction matches this specific instruction
	 * pattern.
	 * @param i
	 * @return
	 */
	boolean match( Instruction i ) {

		/* 
		 * Test the opcode
		 */
		switch( opcodeMatch) {
		case MATCH_NOT:
			if( i.opCode == opcodeMin)
				return false;

		case MATCH_EXACT:
			if( i.opCode != opcodeMin)
				return false;
			break;
		case MATCH_RANGE:
			if( i.opCode < opcodeMin || i.opCode > opcodeMax)
				return false;
			break;
		}

		/*
		 * Test the integer value
		 */

		switch( intMatch ) {
		case MATCH_NOT:
			if( i.integerValid && i.integerOperand == intMin)
				return false;
			break;
		case MATCH_EXACT:
			if( !i.integerValid || i.integerOperand != intMin)
				return false;
			break;
		case MATCH_MISSING:
			if( i.integerValid)
				return false;
			break;
		case MATCH_RANGE:
			if( !i.integerValid || (i.integerOperand < intMin || i.integerOperand > intMax))
				return false;
			break;
		case MATCH_ANY:
			if( !i.integerValid )
				return false;
		}

		/*
		 * Test the double value
		 */

		switch( doubleMatch ) {
		case MATCH_NOT:
			if( i.doubleValid && i.doubleOperand == doubleMin)
				return false;
			break;
		case MATCH_EXACT:
			if( !i.doubleValid || i.doubleOperand != doubleMin)
				return false;
			break;
		case MATCH_MISSING:
			if( i.doubleValid)
				return false;
			break;
		case MATCH_RANGE:
			if( !i.doubleValid || (i.doubleOperand < doubleMin || i.doubleOperand > doubleMax))
				return false;
			break;
		case MATCH_ANY:
			if( !i.doubleValid )
				return false;
		}

		/*
		 * Test the string value
		 */

		switch( stringMatch ) {
		case MATCH_NOT:
			if( i.stringValid && i.stringOperand.equals(stringMin))
				return false;
			break;
		case MATCH_EXACT:
			if( !i.stringValid || !i.stringOperand.equals(stringMin))
				return false;
			break;
		case MATCH_MISSING:
			if( i.stringValid)
				return false;
			break;
		case MATCH_RANGE:
			if( !i.stringValid || (i.stringOperand.compareTo(stringMin) < 0 || i.stringOperand.compareTo(stringMax)>0))
				return false;
			break;
		case MATCH_ANY:
			if( !i.stringValid )
				return false;
		}

		return true;
	}
}

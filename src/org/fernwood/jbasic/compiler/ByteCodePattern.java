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

import java.util.ArrayList;
import java.util.HashMap;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.value.Value;


/**
 * This class is a list of patterns that can be compared to a sequence of instructions.
 * @author cole
 * @version version 1.0 Feb 21, 2011
 *
 */
class ByteCodePattern {

	boolean fLinked;
	ArrayList<InstructionPattern> pattern;
	ArrayList<InstructionPattern> replacement;

	HashMap<String, Integer> actionDictionary;
	
	String stringData[];
	int intData[];
	double doubleData[];
	String name;
	
	int matchCount;

	public String patternString() {
		StringBuffer s = new StringBuffer("[");
		for( int ix = 0; ix < pattern.size(); ix++ ) {
			if( ix > 0)
				s.append(", ");
			s.append('"');
			s.append(pattern.get(ix));
			s.append('"');
		}
		s.append("]");
		return s.toString();
	}
	
	public String replaceString() {
		StringBuffer s = new StringBuffer("[");
		for( int ix = 0; ix < replacement.size(); ix++ ) {
			if( ix > 0)
				s.append(", ");
			s.append('"');
			s.append(replacement.get(ix));
			s.append('"');
		}
		s.append("]");
		return s.toString();
	}
	
	ByteCodePattern( HashMap<String,Integer> dict) {
		pattern = new ArrayList<InstructionPattern>();
		replacement = new ArrayList<InstructionPattern>();
		name = null;
		actionDictionary = dict;
		stringData = new String[10];
		intData = new int[10];
		doubleData = new double[10];
	}

	/**
	 * Add a pattern to the list of instructions we are searching for to
	 * do a peephole replacement.
	 * @param spec
	 */
	Status addPattern( String spec ) {
		InstructionPattern ip = new InstructionPattern(spec, actionDictionary);
		pattern.add(ip);
		return ip.status;
	}

	/**
	 * Add a replacement instruction.
	 * @param spec
	 */
	Status addReplacement( String spec ) {
		InstructionPattern ip = new InstructionPattern(spec, actionDictionary);
		if( ip.status.failed())
			return ip.status;
		
		for( int ax = 0; ax < ip.actionCount; ax++ ) 
			if( ip.action[ax] < 400 && (ip.action[ax] / 100 != PatternOptimizer.ACTION_RCL)) {
				ip.action[ax] = 0;
				return new Status(Status.FAULT, "invalid replacement action code " + ip.action[ax]);
			}
		replacement.add(ip);
		return ip.status;
	}

	/**
	 * Get the replacement bytecode for this pattern.  If there is an error in the
	 * instruction stream (division by zero, etc.) then return null.
	 * @param baseAddress the base address this code will be used in, which is needed
	 * for branch adjustements.
	 * @return
	 */
	ByteCode getByteCode(int baseAddress) {
		ByteCode bc = new ByteCode(null);
		for( int ix = 0; ix < replacement.size(); ix++ ) {
			Instruction i = getReplacement(ix, baseAddress);
			if( i == null )
				return null;
			bc.add(i);
		}
		return bc;
	}
	
	/**
	 * Get a replacement instruction from the optimizer pattern, executing
	 * any of the required actions for this instruction in the pattern
	 * @param idx The index of the instruction to fetch
	 * @param baseAddress The base address of this optimization, which is needed
	 * to determine if branch target adjustments are needed.
	 * @return
	 */
	Instruction getReplacement( int idx, int baseAddress ) {
		InstructionPattern ip = replacement.get(idx);
		if( ip == null )
			return null;
		
		/*
		 * Start by setting what we already know from the pattern.
		 */
		Instruction i = new Instruction( ip.opcodeMin);
		if( ip.intMatch == InstructionPattern.MATCH_EXACT ) {
			i.integerValid = true;
			i.integerOperand = ip.intMin;
		}
		else if (ip.intMatch == InstructionPattern.MATCH_MISSING) {
			i.integerValid = false;
			i.integerOperand = 0;
		}

		if( ip.doubleMatch == InstructionPattern.MATCH_EXACT ) {
			i.doubleValid = true;
			i.doubleOperand = ip.doubleMin;
		}
		else if (ip.doubleMatch == InstructionPattern.MATCH_MISSING) {
			i.doubleValid = false;
			i.doubleOperand = 0.0;
		}
		
		if( ip.stringMatch == InstructionPattern.MATCH_EXACT ) {
			i.stringValid = true;
			i.stringOperand = ip.stringMin;
		}
		else if (ip.stringMatch == InstructionPattern.MATCH_MISSING) {
			i.stringValid = false;
			i.stringOperand = null;
		}
		
		/*
		 * Now scan over the actions to see if any affect any parameters.  These
		 * can be order-sensitive (typically with respect to the special actions)
		 */

		for( int ax = 0; ax < ip.actionCount; ax++) {
			int action = ip.action[ax];
			int mode = action / 100;          /* action 1 = copy, 2= test, 3=rcl */
			int type = (action % 100 ) / 10;  /* type   1=integer, 2=double, 3=string */
			int slot = action %10;            /* slot   0-9 */
			if( action == 0 )
				System.out.println("FAULT: zero action found!");
			if( mode >= 4 ) {
				
				switch( action ) {
				
				/* Special actions */
				
				case 401:	/* MULT12 */
					intData[2] = intData[1] * intData[2];
					doubleData[2] = doubleData[1] * doubleData[2];
					break;
					
				case 402:  /* ADD12 */
					intData[2] = intData[1] + intData[2];
					doubleData[2] = doubleData[1] + doubleData[2];
					break;
				
				case 403:  /* SUB12 */
					intData[2] = intData[2] - intData[1];
					doubleData[2] = doubleData[2] - doubleData[1];
					break;
					
				case 404:  /* DIVI12 */			
					try {
						intData[2] = intData[2] / intData[1];
					} catch (ArithmeticException e ) {
						return null; /* Indicate error prevents action */
					}
					break;
				case 416:  /* DIVD12 */			
					try {
						doubleData[2] = doubleData[2] / doubleData[1];
					} catch (ArithmeticException e ) {
						return null; /* Indicate error prevents action */
					}
					break;
					
				case 405:  /* CAT12 */
					stringData[2] = stringData[1] + stringData[2];
					break;
				case 406:  /* NEG0 */
					intData[0] = -intData[0];
					break;
				case 407:  /* NEG1 */
					intData[1] = -intData[1];
					break;
				case 408: 	/* NEGD0 */
					doubleData[0] = -doubleData[0];
					break;
				case 409:	/* OFFSET1 */
					intData[1] = intData[1] - (pattern.size() - replacement.size());
					break;
				case 410: 	/* LENGTH1 */
					intData[1] = stringData[1].length();
					break;
				case 411:	/* NOT1 */
					intData[1] = intData[1] == 0 ? 1: 0;
					break;
				case 412:	/* INTSTR0 */
					stringData[0] = Integer.toString(intData[0]);
					break;
				case 413:	/* DBLSTR0 */
					stringData[0] = Value.toString(new Value(doubleData[0]),false);
					break;
				case 414:   /* INTDBL1 */
					doubleData[1] = intData[1];
					break;
				case 415: 	/* INTDBL2 */
					doubleData[2] = intData[2];
					break;
				}
			}
			else switch( type ) {
			case PatternOptimizer.ACTION_INTEGER:
				i.integerValid = true;
				i.integerOperand = intData[slot];
				break;
			case PatternOptimizer.ACTION_DOUBLE:
				i.doubleValid = true;
				i.doubleOperand = doubleData[slot];
				break;
			case PatternOptimizer.ACTION_STRING:
				i.stringValid = true;
				i.stringOperand = stringData[slot];
				break;
			}
		}

		/*
		 * Finally, if this is a BRANCH instruction and there is
		 * an integer value, it must be offset by any change in the
		 * pattern size to the replacement size.
		 */
		if( i.opCode > ByteCode._BRANCH_FLAG && i.integerValid && i.integerOperand >= baseAddress)
			i.integerOperand = i.integerOperand - (pattern.size() - replacement.size());

		/*
		 * All done, return the instruction we created
		 */
		return i;
	}

	/**
	 * Given a ByteCode array and a position, determine if the
	 * instruction stream at that position matches the current
	 * pattern of one or more instructions.
	 * @param bc
	 * @param pos
	 * @return
	 */
	boolean match( ByteCode bc, int pos ) {

		if( pattern.size() + pos > bc.size())
			return false;
		for( int ix = 0; ix < stringData.length; ix++ ) {
			stringData[ix] = null;
			intData[ix] = 0;
			doubleData[ix] = Double.NaN;
		}

		for( int ix = 0; ix < pattern.size(); ix++ ) {
			Instruction i = bc.getInstruction(pos+ix);
			InstructionPattern ip = pattern.get(ix);
			if(!ip.match(i))
				return false;
			for( int ax = 0; ax<ip.action.length; ax++) {

				int action = ip.action[ax];

				if( action > 0 ) {

					if( action < 400) {
						int mode = action / 100;          /* action 1 = copy, 2= test, 3=rcl */
						int type = (action % 100 ) / 10;  /* type   1=integer, 2=double, 3=string */
						int slot = action %10;            /* slot   0-9 */

						if( mode == PatternOptimizer.ACTION_RCL ) /* no action for matching */
							continue;
						
						if( mode == PatternOptimizer.ACTION_STORE )
							switch( type ) {
							case 1:	
								intData[slot] = i.integerOperand;
								break;
							case 2:
								doubleData[slot] = i.doubleOperand;
								break;
							case 3:
								stringData[slot] = i.stringOperand;
								break;
							}
						else switch( type ) {
						case PatternOptimizer.ACTION_INTEGER:
							if( intData[slot] != i.integerOperand)
								return false;
							break;
						case PatternOptimizer.ACTION_DOUBLE:
							if( doubleData[slot] != i.doubleOperand)
								return false;
							break;
						case PatternOptimizer.ACTION_STRING:
							if( stringData[slot] == null || !stringData[slot].equals(i.stringOperand))
								return false;
							break;
						case PatternOptimizer.ACTION_NEXT:
							if( pos+ix+1 != intData[slot])
								return false;
							break;
						case PatternOptimizer.ACTION_CURRENT:
							if( pos+ix != intData[slot])
								return false;
							break;
						}
					} else switch( action ) {
					/* Execute any specific actions with codes >=400 */
					case 405:
						System.out.println("APPEND TEST!");
						
					default: 
						System.out.println("Action " + action);
						break;
						
					}
				}
			}
		}
		matchCount++;
		return true;
	}
}


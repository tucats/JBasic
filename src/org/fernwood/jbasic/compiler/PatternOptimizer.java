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
 * Created on Feb 21, 2011 by cole
 *
 */
package org.fernwood.jbasic.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.value.Value;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The PatternOptimizer does a peephole scan of a bytecode stream looking
 * for pattern matches which it can replaced with more efficient patterns.
 * @author cole
 * @version version 1.0 Feb 21, 2011
 *
 */
public class PatternOptimizer {

	final static int ACTION_STORE = 1;
	final static int ACTION_TEST = 2;
	final static int ACTION_RCL = 3;
	final static int ACTION_INTEGER = 1;
	final static int ACTION_DOUBLE = 2;
	final static int ACTION_STRING = 3;
	final static int ACTION_NEXT = 4;
	final static int ACTION_CURRENT = 5;
	
	static ArrayList<ByteCodePattern> optimizations;
	static HashMap<String,Integer> actionDictionary;

	/**
	 * Size of the largest pattern processed.  This is used to
	 * back up the pattern pointer when a replacement occurs
	 * since after a change, a previous bytecode stream could
	 * potentially now be a new pattern.
	 */
	public int maxPatternSize = 0;
	
	/**
	 * Status indicator for initialization operations.
	 */
	
	public Status status;
	
	/**
	 * The session that is used to locate the SYS$DEBUG_OPT
	 * flag to manage debugging data.
	 */
	private JBasic session;
	
	/**
	 * Flag indicating if we attempt to remove dead code from
	 * an optimization stream.
	 */
	public boolean deadCodeRemoval = true;
	
	/**
	 * Create a new instance of the optimizer, which mostly involves ensuring
	 * that the master optimization data has been loaded.
	 * @param parent the session that will "own" this instance of the 
	 * pattern optimizer. This is used to retrieve the debugging
	 * flag when doing an optimization
	 */
	public PatternOptimizer( JBasic parent ) {

		status = new Status();
		session = parent;
		
		/*
		 * If the first time, load up the list of optimizations.
		 */
		if( optimizations == null ) {
			
			optimizations = new ArrayList<ByteCodePattern>();
			
			/*
			 * First, load up the action dictionary list.
			 */
			actionDictionary = new HashMap<String,Integer>();
			for( int i = 0; i < 10; i++ ) {
				actionDictionary.put("SETINT" + i, ACTION_STORE*100 + ACTION_INTEGER*10 + i);
				actionDictionary.put("SETDBL" + i, ACTION_STORE*100 + ACTION_DOUBLE*10 + i);
				actionDictionary.put("SETSTR" + i, ACTION_STORE*100 + ACTION_STRING*10 + i);
				actionDictionary.put("TESTINT" + i, ACTION_TEST*100 + ACTION_INTEGER*10 + i);
				actionDictionary.put("TESTDBL" + i, ACTION_TEST*100 + ACTION_DOUBLE*10 + i);
				actionDictionary.put("TESTSTR" + i, ACTION_TEST*100 + ACTION_STRING*10 + i);
				actionDictionary.put("TESTNXT" + i, ACTION_TEST*100 + ACTION_NEXT * 10 + i);
				actionDictionary.put("RCLINT" + i, ACTION_RCL*100 + ACTION_INTEGER*10 + i );
				actionDictionary.put("RCLDBL" + i, ACTION_RCL*100 + ACTION_DOUBLE*10 + i);
				actionDictionary.put("RCLSTR" + i, ACTION_RCL*100 + ACTION_STRING*10 + i );
			}
			
			actionDictionary.put("MULT12", 	401);
			actionDictionary.put("ADD12", 	402);
			actionDictionary.put("SUB12", 	403);
			actionDictionary.put("DIVI12", 	404);
			actionDictionary.put("CAT12", 	405);
			actionDictionary.put("NEG0", 	406);
			actionDictionary.put("NEG1",	407);
			actionDictionary.put("NEGD0",	408);
			actionDictionary.put("OFFSET1", 409);
			actionDictionary.put("LENGTH1", 410);
			actionDictionary.put("NOT1", 	411);
			actionDictionary.put("INTSTR0", 412);
			actionDictionary.put("DBLSTR0", 413);
			actionDictionary.put("INTDBL1", 414);
			actionDictionary.put("INTDBL2", 415);
			actionDictionary.put("DIVD12",	416);
			
			
			/*
			 * Now, define the optimizations. This is loaded from an XML
			 * file that resides in the jar or the local directory by
			 * default.
			 */
			
			status = loadOptimizationFile("!OptDict.xml");
		}
	}
	
	/**
	 * Given a file path, attempt to laod the optimization dictionary
	 * from the file.  The name can
	 * start with "@" to indicate a resource in the jar file, or it can
	 * start with "!" to indicate that both the resource and file system
	 * should be checked.  If neither character is present then the name
	 * indicates a path in the file system.
	 * @param fname The name of the file to read
	 * @return a Status indicating if the file was found, and if the
	 * XML contents are correctly formed.
	 */
	public Status loadOptimizationFile( String fname ) {

		InputStream is = null;

		try {

			if( fname.charAt(0) == '!') {
				is = JBasic.class.getResourceAsStream("/" + fname.substring(1));
				if( is == null ) {
					File f = new File(fname.substring(1));
					try {
					is = new FileInputStream(f);
					} catch (Exception e ) {
						is = null;
					}
				}
			}

			if( is == null ) {

				if( fname.charAt(0) == '@' ) {
					is = JBasic.class.getResourceAsStream(fname.substring(1));
				}
				else {
					File f = new File(fname);
					is = new FileInputStream(f);
				}
			}
		} catch (FileNotFoundException e) {
			return new Status(Status.FILENF, fname);
		}
		if( is != null )
			return loadOptimizationXML(is);

		return new Status(Status.FILENF, fname);
	}

		/**
		 * Given an input stream containing an XML dictionary of optimizations,
		 * load the definitions and build the optimizations list.
		 * the data.
		 * @param is a InputStream, typically created from a string or as a file
		 * @return a Status indicating if the parse went correctly.
		 */
		private Status loadOptimizationXML( InputStream is ) {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			Status sts = null;

			try {
				DocumentBuilder dBuilder = null;

				/*
				 * Parse the data we were given as an input stream and build a DOM
				 * in memory.
				 */
				dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(is);

				/*
				 * Traverse the tree looking for <Message> elements>
				 */

				NodeList nodes = doc.getElementsByTagName("Opt");


				/*
				 * Loop over the list to process each one.
				 */
				int sequence = 0;
				
				for( int ix = 0; ix < nodes.getLength(); ix++ ) {
					Node node = nodes.item(ix);
					sequence++;
					ByteCodePattern bcp = new ByteCodePattern(actionDictionary);
					NamedNodeMap attrs = node.getAttributes();
					if( attrs != null ) {
						Node name = attrs.getNamedItem("name");
						if( name != null )
							bcp.name = name.getNodeValue();
						name = attrs.getNamedItem("linked");
						if( name != null )
							bcp.fLinked = name.getNodeValue().equalsIgnoreCase("true");
						
					}
					if( bcp.name == null )
						bcp.name = "optimization-" + Integer.toString(sequence);
					
					NodeList msgs = node.getChildNodes();
					if( msgs == null )
						return new Status(Status.EXPMESSAGE);
					for( int in = 0; in < msgs.getLength(); in++ ) {
						Node msg = msgs.item(in);
						int t = msg.getNodeType();
						String lang = null;
						String text = null;

						if( t == Node.ELEMENT_NODE) {
							lang = msg.getNodeName();
							msg = msg.getFirstChild();
							text = msg.getNodeValue();

							text = new Value(text).denormalize();
							
							if( lang.equals("Pattern")) {
								sts = bcp.addPattern(text);
								if( sts.failed())
									return sts;
							}
							else
								if( lang.equals("Replace")) {
									sts = bcp.addReplacement(text);
									if( sts.failed())
										return sts;
								}
								else
									return new Status(Status.XML, "expected Pattern or Replace, found " + lang);
						}

					}
					if( maxPatternSize < bcp.pattern.size())
						maxPatternSize = bcp.pattern.size();
					optimizations.add(bcp);
				}
				/*
				 * Force some cleanup.
				 */
				doc = null;
				dBuilder = null;

			} catch (Exception e) {
				return new Status(Status.XML, e.toString());
			}
			return new Status();
		}

	

	/**
	 * Optimize a bytecode stream using the pattern-based optimization table.
	 * @param bc the Bytecode stream to optimize
	 * @return Status indicating successful optimization
	 */
	public synchronized Status optimize( ByteCode bc ) {

		if( session != null )
			deadCodeRemoval = session.getBoolean("SYS$OPT_DEADCODE");
		else
			deadCodeRemoval = false;
		
		boolean optASM = false;
		if( session != null )
			optASM = session.getBoolean("SYS$OPT_ASM");
		
		int count = 0;
		boolean debugging = false;
		if( session != null )
			debugging = session.getBoolean("SYS$DEBUG_OPT");
		
		int blockBase = -1;
		int blockSize = 0;
		
		/*
		 * Scan over the code looking for branch targets that are tagged in the
		 * instructions - we need this info later to determine the eligibility of
		 * instructions for optimizations that change the number of instructions in
		 * a pattern.
		 */
		
		/* 
		 * Step one, clear the flag on the instructions, except those that have labels
		 * in which case we assume the label is there as a target and mark it as such.
		 */
		for( int pc = 0; pc < bc.size(); pc++ ) {
			Instruction i = bc.getInstruction(pc);
			i.branchTarget = false;
			if( bc.findLabel(pc) != null )
				i.branchTarget = true;
		}
		
		/*
		 * Step two, scan the instructions to find branch targets and mark them 
		 * accordingly.
		 */
		
		for( int pc = 0; pc < bc.size(); pc++ ) {
			Instruction i = bc.getInstruction(pc);
			if( i.opCode > ByteCode._BRANCH_FLAG && i.integerValid && i.integerOperand < bc.size()) {
				bc.getInstruction(i.integerOperand).branchTarget = true;
			}
		}
		
		/*
		 * Now scan the bytecode looking to see if we have some optimizations to do.
		 */
		
		int currentStatement = -1;
		
		for( int pc = 0; pc < bc.size(); pc++ ) {
			
			/*
			 * We never optimize ASM blocks since they were written by the
			 * user.
			 */
			
			Instruction iASM = bc.getInstruction(pc);
			if( !optASM && iASM.opCode == ByteCode._ASM && iASM.integerValid) {
				pc += iASM.integerOperand;
				continue;
			}
			/*
			 * Have we moved outside the a block we were processing?
			 * IF so, update the block size and be done with it.
			 */
			
			if((blockBase >=0) && (pc > blockBase + blockSize)) {
				Instruction i = bc.getInstruction(blockBase);
				i.integerOperand = blockSize;
				blockBase = -1;
				blockSize = 0;
			}
			/*
			 * If we are starting a new block, capture that info...
			 */
			
			int opCode = bc.getInstruction(pc).opCode;

			if( opCode == ByteCode._DATA || opCode == ByteCode._WHERE || 
					opCode == ByteCode._JOIN || 
					( opCode == ByteCode._ASM && iASM.integerValid )){
				blockBase = pc;
				blockSize = bc.getInstruction(pc).integerOperand;
				continue;
			}
			
			/*
			 * Check each optimization against the current pointer in
			 * the bytecode data.
			 */
			
			boolean canOpt = false;
			for( int optx = 0; optx < optimizations.size(); optx++ ) {
				ByteCodePattern opt = optimizations.get(optx);
				
				/*
				 * If the current bytecode is linked, we only apply
				 * optimizations that are intended for linked code.
				 */
				if( bc.fLinked & !opt.fLinked)
					continue;
				
				/*
				 * Is an instruction within this pattern a target for
				 * a branch? If so, we don't do the optimization
				 * because it could easily blow up the optimization.
				 * We only care about *within* the pattern, if the
				 * branch is to the first instruction we are okay
				 * with that...
				 */
				if( isBranchTarget( bc, pc+1, opt.pattern.size()-1))
					continue;
				
				if( bc.getInstruction(pc).opCode == ByteCode._STMT)
					currentStatement = bc.getInstruction(pc).integerOperand;
				
				/*
				 * if( debugging )
				 *   JBasic.log.println("OPTIMIZER: Scanning " + bc.getInstruction(pc).toString());
				 */
				
				canOpt = opt.match(bc, pc);
				if( canOpt ) {
					
					if( debugging ) {
						if( opt.name != null )
							JBasic.log.println("OPTIMIZER: Pattern " + opt.name + " #" + currentStatement);
						else {
							JBasic.log.println("OPTIMIZER: At statement " + currentStatement);
							JBasic.log.println("OPTIMIZER: Match   " + opt.patternString());
							JBasic.log.println("OPTIMIZER: Replace " + opt.replaceString());
						}
						for( int i = 0; i < opt.pattern.size(); i++) {
							JBasic.log.println("OPTIMIZER: << " + 
								bc.getInstruction(pc+i));
						}
					}
					
					/*
					 * Before we being the process of deleting and inserting instructions,
					 * let's make sure that the instructions to replace with will work.
					 * For example,divisions by zero or other actions in stream could
					 * cause us to abort the optimization.
					 */
					ByteCode newCode = opt.getByteCode(pc);
					if( newCode == null ) {
						if( debugging )
							JBasic.log.println("OPTIMIZER: Replacement action failure prevents optimization");
						continue;
					}
					
					/*
					 * If the first instruction of the old code was a branch target, then 
					 * we've got to make sure that bit is set in the new instruction as well
					 * so dead code removal doesn't take it out incorrectly.
					 */
					
					boolean wasBranchTarget = bc.getInstruction(pc).branchTarget;
					
					/*
					 * Now, delete any extra instructions in the old pattern.  If
					 * we are in a block (_WHERE or _DATA, for example) also update
					 * the size of the block accordingly.
					 */
					
					int deleteCount = opt.pattern.size() - opt.replacement.size();
					if( blockBase >= 0 )
						blockSize = blockSize - deleteCount;
					for( int i = 0; i < deleteCount; i++)
						bc.remove(pc);

					/*
					 * The, copy the new instructions right over
					 * the top of the remaining old instructions.
					 * If the output is larger than the pattern
					 * (which almost never happens) then do some
					 * inserts now.
					 */
					
					for( int i = 0; i < opt.replacement.size(); i++ ) {
						Instruction inst = newCode.getInstruction(i);
						if( debugging )
							JBasic.log.println("OPTIMIZER: >> " + inst);;

						if( i > opt.pattern.size())
							bc.insert(pc+i, inst);
						else
							bc.setInstruction(inst, pc+i);
					}
					
					/*
					 * Finally, reset the branch target flag on the first
					 * new instruction (which might not even be in the old
					 * code block if it was a simple delete)
					 */
					if( wasBranchTarget )
						bc.getInstruction(pc).branchTarget = true;

					count++;
					break;
				}
			}
			/*
			 * If we found an optimization, back up a little bit (based on
			 * the largest possible pattern) and start again.
			 */
			if( canOpt ) {
				pc = pc - (maxPatternSize+1);
				if( blockBase >= 0 && pc < blockBase )
					pc = blockBase + 1;
				if( pc < 0 )
					pc = 0;
			}
		}
		
		/*
		 * After all that, if we have dead code we can clean it up.
		 */
		
		if( deadCodeRemoval && !bc.fLinked) {
			boolean deadCode = false;
			int lastBranchAddr = -1;
			
			for( int pc = 0; pc < bc.size(); pc++ ) {
				Instruction i = bc.getInstruction(pc);

				/*
				 * IF this is a _stmt that isn't a branch target
				 * and the next statement is also _STMT, then 
				 * delete the current one.
				 */
				
				if( !i.branchTarget && i.opCode == ByteCode._STMT && pc < bc.size()-1 
						&& bc.getInstruction(pc+1).opCode == ByteCode._STMT) {
					bc.remove(pc);
					pc--;
					continue;
				}
				/*
				 * If this is a data block, skip over it.
				 */
				
				if( i.opCode == ByteCode._DATA || i.opCode == ByteCode._WHERE  || i.opCode == ByteCode._JOIN){
					pc = pc + i.integerOperand;
					continue;
				}
				
				/*
				 * If this instruction is a branch target, get out of
				 * dead code mode and keep going.
				 */
				if( i.branchTarget ) {
					if( lastBranchAddr == pc-1 && bc.getInstruction(lastBranchAddr).integerOperand == pc ) {
						bc.remove(pc-1);
						// i.branchTarget = false;
						pc--;
					}
					deadCode = false;
					continue;
				}
				
				/*
				 * If we are in dead code mode, throw away the instruction and
				 * keep on trucking
				 */
				if( deadCode ) {
					bc.remove(pc);
					pc--;
					continue;
				}

				/*
				 * Special case - we're not in dead code mode, but we have a branch
				 * to the subsequent instruction - toss it away.  Also, if we are
				 * at a hard branch to another instruction, re-start search for
				 * dead code.
				 */
				if( i.opCode == ByteCode._BR && i.integerValid ) {

					deadCode = true;
					lastBranchAddr = pc;
					continue;
				}
			}
		}
		
		/*
		 * Let the caller know we're done and how many patterns were found
		 */
		return new Status(Status.OPTIMIZED, count);
	}
	
	/**
	 * Determine if a given range of instructions in the bytecode
	 * contains an instruction that is a branch target. 
	 * @param bc the ByteCode area to check
	 * @param pc the starting location in the ByteCode to check
	 * @param size the number of instructions to check
	 * @return true if the given range contains the target of a branch
	 * instruction.
	 */
	private boolean isBranchTarget(ByteCode bc, int pc, int size) {
		
		if( size <= 0 )
			return false;
		
		int max = bc.size();
		
		if( max > pc+size )
			max = pc+size;
		
		for( int ix = pc; ix < max; ix++ ) {			
			Instruction i = bc.getInstruction(ix);
			if( i.branchTarget  /* && !( i.opCode == ByteCode._STMT || i.opCode == ByteCode._NOOP) */)					
					return true;
		}
		return false;
	}

	/**
	 * Given a value that contains a record with PATTERN and REPLACE string
	 * arrays, create a new optimization definition.
	 * @param opt a Value containing a the pattern to use.
	 * @return a Status indicating if the pattern was properly formed
	 */
	public Status add( Value opt ) {
		
		if( opt.getType() != Value.RECORD)
			return new Status(Status.EXPREC);
		
		Value patterns = opt.getElement("PATTERN");
		Value replace = opt.getElement("REPLACE");
		
		if( patterns == null )
			return new Status(Status.NOSUCHMEMBER, "PATTERN");
		if( replace == null )
			return new Status(Status.NOSUCHMEMBER, "REPLACE");
		
		if( patterns.getType() != Value.ARRAY )
			return new Status(Status.TYPEMISMATCH, "PATTERN");
		if( replace.getType() != Value.ARRAY )
			return new Status(Status.TYPEMISMATCH, "REPLACE");
		
		ByteCodePattern bcp = new ByteCodePattern(actionDictionary);
		for( int ix = 0; ix < patterns.size(); ix++ )
			bcp.addPattern(patterns.getString(ix+1));
		for( int ix = 0; ix < replace.size(); ix++ )
			bcp.addReplacement(replace.getString(ix+1));
		
		optimizations.add(bcp);

		return new Status();
	}
	
	/**
	 * Reset the statistics counters
	 */
	public void clearStatistics() {
		for( int ix = 0; ix < optimizations.size(); ix++ ) {
			ByteCodePattern bcp = optimizations.get(ix);
			bcp.matchCount = 0;
		}
	}
	
	/**
	 * Display a single optimization pattern on the console
	 * @param opt the name of the optimization pattern to display.
	 */
	public void dumpOne( String opt ) {
		
		for( int ix = 0; ix < optimizations.size(); ix++ ) {
			ByteCodePattern bcp = optimizations.get(ix);
			if( !bcp.name.equalsIgnoreCase(opt))
				continue;
			session.stdout.println("Pattern:");
			for( int i = 0; i < bcp.pattern.size(); i++ )
				session.stdout.println("  " + bcp.pattern.get(i).toString());
			
			if( bcp.replacement.size() == 0 )
				session.stdout.println("Deleted");
			else {
				session.stdout.println("Replacement:");
				for( int i = 0; i < bcp.replacement.size(); i++ )
					session.stdout.println("  " + bcp.replacement.get(i).toString());
			}
			return;
		}
		session.stdout.println("No optimization pattern \"" + opt + "\"");
	}
	
	/**
	 * Dump the frequency statistics.
	 */
	public void statistics( ) {
		
		int count = 0;
		int optCount = 0;
		for( int ix = 0; ix < optimizations.size(); ix++ ) {
			ByteCodePattern bcp = optimizations.get(ix);
			if( bcp.matchCount <= 0)
				continue;

			if( optCount == 0 ) {
				optCount++;
				session.stdout.println("Optimization          Count");
				session.stdout.println("------------          -----");
			}
			
			StringBuffer m = new StringBuffer(bcp.name);
			for( int px = m.length(); px < 22; px++ )
				m.append(' ');
			
			count += bcp.matchCount;
			m.append(Utility.pad(Integer.toString(bcp.matchCount),-5));
			session.stdout.println(m.toString());
		}
		if( optCount > 0 )
			session.stdout.println();
		
		session.stdout.println("Total optimization patterns   = " + optimizations.size());
		session.stdout.println("Total optimizations performed = " + count);
	}
	
	
	/**
	 * Dump the current optimizer data to a file name
	 * @param fname A string describing the name of the file to create.
	 * @return a Status indicating if the write was successful.
	 */
	public Status dump( String fname ) {
		
		String fileName = fname;
		
		if( !fname.endsWith(".xml"))
			fileName = fname + ".xml";
		
		JBasicFile out = JBasicFile.newInstance(session, JBasicFile.MODE_OUTPUT);
		try {
			out.open(new Value(fileName), null);
		} catch (JBasicException e) {
			return e.getStatus();
		}
		
		Date rightNow = new Date();
		
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); 
		out.println("<!-- Pattern Matching Optimizer Dictionary -->");
		out.println("<!-- Saved " + rightNow.toString() + " -->");
		
		out.println("<Optimizations>");
		int count=0;
		for( int ix = 0; ix < optimizations.size(); ix++ ) {
			ByteCodePattern bcp = optimizations.get(ix);
			//out.println("  <!-- Match count " + bcp.matchCount + " -->");
			StringBuffer nodeText = new StringBuffer("  <Opt");
			if( bcp.name != null ) {
				nodeText.append(" name=\"");
				nodeText.append(bcp.name);
				nodeText.append("\"");
			}
			if( bcp.fLinked)
				nodeText.append(" linked=\"true\"");
			
			nodeText.append(">");
			out.println( nodeText.toString());
			
			for( int i = 0; i < bcp.pattern.size(); i++ )
				out.println("    <Pattern>" + bcp.pattern.get(i) /*.encoding*/ + "</Pattern>");
			
			for( int i = 0; i < bcp.replacement.size(); i++ )
				out.println("    <Replace>" + bcp.replacement.get(i)/* .encoding*/  + "</Replace>");
			out.println("  </Opt>");
			count++;
		}
		out.println("</Optimizations>");
		out.close();
		session.stdout.println("Wrote " + count + " optimization patterns to file \"" + fileName + "\"");
		
		return new Status();
	}
}

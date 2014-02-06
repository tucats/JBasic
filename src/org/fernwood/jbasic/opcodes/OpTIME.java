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
package org.fernwood.jbasic.opcodes;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpTIME extends AbstractOpcode {

	/**
	 * This defines a "big loop" for the TIME command; if the count is greater
	 * than this value then a FORX..NEXT loop is created.  If the count is less
	 * than this, then the target statement is generated repeatedly.
	 */
	public static final int BIG_LOOP = 100;

	/**
	 * Swap top two stack items. Often used to concatenate objects scanned
	 * left-to-right in their natural order; i.e. swap/add sequences.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * Based on the mode in the instruction, capture or print data.
		 */
		String name = "$TIME_DATA";
		Value timeData = null;
		boolean doGC = false;
		final int mode = env.instruction.integerOperand;
		
		/*
		 * If the mode is zero, this is the baseline operation.
		 */
		if( mode == 0 ) {
			
			/*
			 * To get a more repeatable and clean benchmark, let's force garbage
			 * collection at the start of this operation (before we take a
			 * timestamp!) so multiple runs of the same benchmark will have
			 * comparable results. We'll let the user disable this with the
			 * SYS$TIME_DISABLE_GC flag if they wish.
			 */

			doGC = env.session.getBoolean("SYS$TIME_GC");
			if (doGC)
				System.gc();

			/*
			 * Set up variables used to track what we do for this instruction.  
			 */
			
			timeData = new Value(Value.RECORD, null);
			timeData.setElement(new Value(env.session.statementsExecuted), "STATEMENTS");
			timeData.setElement(new Value(env.session.instructionsExecuted), "INSTRUCTIONS");
			timeData.setElement(new Value(System.currentTimeMillis()), "TIMESTAMP");
			timeData.setElement(new Value(doGC), "GC");

			/*
			 * Store the data in the variable name we were given.
			 */
			
			if( env.instruction.stringValid)
				name = env.instruction.stringOperand;
						
			env.localSymbols.insert(name, timeData);
			env.localSymbols.markReadOnly(name);
			return;
		}
		
		/*
		 * Otherwise the mode is non-zero and reflects the number of times the code
		 * was looped.  This is normally "1" for a simple TIME command, but may be
		 * a larger integer if iterations were requested.
		 * 
		 * Get the time data we'd previous stored away.  Use the name in the 
		 * instruction if one is given, else assume the default name.
		 */
			if( env.instruction.stringValid)
				name = env.instruction.stringOperand;
			double scale = mode;

			timeData = env.localSymbols.localReference(name);
			if( timeData == null ) 
				throw new JBasicException(Status.FAULT, "no time data found for " + name );
			if( timeData.getType() != Value.RECORD ) 
				throw new JBasicException(Status.FAULT, "invalid time data found for " + name );

			
			/*
			 * Get the variables used to track what we do for this instruction.
			 */
			final long startCount = timeData.getElement("STATEMENTS").getInteger();
			final int startICount = timeData.getElement("INSTRUCTIONS").getInteger();
			final long startTime = (long) timeData.getElement("TIMESTAMP").getDouble();
			doGC = timeData.getElement("GC").getBoolean();
			
			/*
			 * Print out the count, duration, and statements/second ratings.  
			 * We decrement the instruction counter to discount
			 * the actual _TIME opcode itself.
			 */
			long count = env.session.statementsExecuted - startCount ;
			int iCount = env.session.instructionsExecuted - startICount - 1;
			
			/*
			 * If there was a loop generated, remove the count of the implicit
			 * FOR-NEXT loop instructions generated.
			 */
			if( mode > BIG_LOOP )
				iCount = iCount - 3;  
			
			double duration = (System.currentTimeMillis() - startTime);
			duration = duration / 1000.0;
			
			/*
			 * Scale everything now.
			 */
			
			count = count / mode;
			iCount = iCount / mode;
			double tDuration = duration;
			duration = duration / scale;
			
			/*
			 * Format the pleasing output.
			 */
			
			if( duration > 1000)
				duration = Math.floor(duration);
			
			String sDuration = Double.toString(duration);
			if( sDuration.length() > 6)
				sDuration = sDuration.substring(0, 6);
			
			final String plural = (count == 1) ? "" : "s";
			env.session.stdout.println("Executed " + count + " statement" + plural + ", "
					+ iCount + " instructions " + " in " + sDuration + " seconds,");
			if( mode > 1 )
				env.session.stdout.println("   for " + mode + 
						" iterations and total elapsed time of " + tDuration +" seconds,");
			
			double speed = (count / duration);
			
			if( duration > 0.0 ) {
				env.session.stdout.print("   or " + (int) speed + " statements/second, ");
				speed = (iCount / duration) ;
				env.session.stdout.println((int) speed + " instructions/second,");
			}
			
			double ips = (double) iCount / (double) count;
			String st;
			st = Value.format(new Value(ips), "###.#").trim();
			
			if( count > 1 )
				env.session.stdout.println("   and averaging " + st   + " instructions/statement.");
			if( !doGC )
				env.session.stdout.println("   Initial garbage collection was disabled.");
			env.session.setNeedPrompt(true);
			
			/*
			 * Delete the temporary symbol table created to hold the timing data
			 */
			
			env.localSymbols.deleteAlways(name);
			return;

	}

}

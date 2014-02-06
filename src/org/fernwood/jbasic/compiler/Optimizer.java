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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.value.Value;

/**
 * Optimizer object. This object contains methods used to perform various kinds
 * of  optimizations of a bytecode stream that are not easily expressed via the
 * basic peephole optimizer in PatternOptimizer. Note that the optimize() method 
 * of this class also calls the PatternOptimizer so both optimization forms are
 * handled by the one method call.
 * <p>
 * The general usage pattern is to create a byte code stream via the various
 * compiler options, and then call the optimizer to run over that bytecode.
 * The bytecode is examined iteratively to seek specific patterns that can 
 * be improved.
 * The optimizer iterates over the entire bytecode stream until no additional 
 * patterns are seen.  This iteration process means that successive passes may 
 * reduce multiple patterns to simpler patterns which are in turn optimized again.
 * <p>
 * For JBasic programs themselves, optimization can be disabled by the user 
 * with the SET OPTIMZE command, which controls whether the program management 
 * code calls the optimizer as a result of linking or not.
 * <p>
 * <em>It is essential that no optimizations cross statement boundaries without
 * also modifying the label map handling.  Currently, removal of unwanted bytecodes
 * does not result in a change in the label maps, because those are built after 
 * optimizations are done per-statement... subsequent whole-byte-code optimizations
 * could change those. You've been warned...</em>
 * 
 * @author tom
 * @version version 1.3 Jan 7, 2008
 * 
 */
public class Optimizer {

	/**
	 * The bytecode object must be at least this many instructions to
	 * justify performing any optimizations at all.
	 */
	
	private static final int OPT_SIZE_THRESHHOLD = 4;

	
	/**
	 * Indicates if the _STRPOOL opcode is used by the optimizer to pool _STRING
	 * constants. This would be a theoretical improvement only in cases where
	 * long strings are used heavily in a program and therefore cause churn in
	 * the memory map when there are redundant copies in memory.  For now, I'm
	 * leaving this disabled.  
	 * 
	 * Note the following value as well which indicates how many times the same
	 * string must be seen to warrant pooling it at all.
	 */
	public static boolean OPT_STRING_POOL = false;

	/**
	 * How many occurrences of the _STRING need to exist to justify creating the
	 * compile time string pool for this constant?
	 */
	public static int STRING_POOL_THRESHHOLD = 3;
	
	/**
	 * Count of optimizations performed.
	 */
	private int count;


	int getOptimizationCount() {
		return count;
	}

	/**
	 * Optimize a bytecode string. This is currently a short list of essentially
	 * peep-hole optimizations. The optimizer works by executing each
	 * optimization iteratively over the bytecode string until no more
	 * optimizations can be performed. The bytecode arrayValue is updated in
	 * place as a result of the optimizer.
	 * 
	 * @param bc
	 *            The bytecode stream to be optimized.
	 * @return Number of optimizations found and implemented.
	 */

	public int optimize(final ByteCode bc) {
		count = 0;
		
		if (bc.size() < OPT_SIZE_THRESHHOLD )
			return 0;
		
		/*
		 * Iterate over the remaining optimizations until we don't find any
		 * more optimizations to perform.  We iterate because each template-
		 * or peephole-based optimization might reveal a subsequent pattern
		 * that could also be optimized.
		 */
		int oldCount = 0;

		while (true) {

	
			/*
			 * Branches with labels (_BRZ "bob") are converted to branch with
			 * actual bytecode address (_BRZ 153).
			 */
			count = optResolveBranches(bc, count);

			/*
			 * A constant followed by a CVT operation can be changed to just
			 * define the constant of the correct type and remove the CVT.
			 */
			count = optConstantCVT(bc, count);

			/*
			 * Branch optimizations
			 */
			count = optBranches(bc, count);
						
			/*
			 * Convert _STRING reference to string pool
			 */
			
			count = optStringPool(bc,count);
			
			/*
			 * _END that follows a hard branch is unreachable and can be
			 * deleted.
			 */
			optDeadEnd(bc);

			/*
			 * Run the pattern-matching optimizer if we have a controlling session
			 * for this code stream.
			 */
			if( bc.getSession() != null ) {
				Status optStatus = bc.getSession().pmOptimizer.optimize(bc);
				if( optStatus.getCode().equals(Status.OPTIMIZED)) {
					count = count + Integer.parseInt(optStatus.getMessageParameter());
				}
			}


			if (count == oldCount)
				break;
			oldCount = count;
		}
		return count;
	}

	/**
	 * Specific self-relative branches can be removed such as a branch to the
	 * following instruction or over a _STMT operation.  These cannot be
	 * expressed in the PatternOptimizer (it has no sense of instruction
	 * location in it's pattern specifications) so they are handled here.
	 * @param bc
	 * @param startingCount
	 * @return
	 */
	int optBranches( final ByteCode bc, int startingCount ) {
		int count = startingCount;

		/*
		 * Scan over the bytecode stream, looking for chances to
		 * optimize some specific branch cases.
		 */
		for( int n = 0; n < bc.size()-2; n++ ) {
			
			Instruction i1 = bc.getInstruction(n);
			
			/*
			 * If this is a branch to the next instruction, just delete it.
			 */
			if((i1.opCode == ByteCode._BR) && (i1.integerOperand == n+1)) {
				bc.remove(n);
				continue;
			}
			if( n >= bc.size()-2)
				continue;;
			
			
			Instruction i2 = bc.getInstruction(n+1);
			Instruction i3 = bc.getInstruction(n+2);

			/*
			 * Two "value" instructions that load
			 * constants or a variable, followed by a SWAP, can be reversed
			 * and the swap removed.
			 */
			if ((i3.opCode == ByteCode._SWAP) && isValue(i1) && isValue(i2)) {
				bc.setInstruction(i2, n);
				bc.setInstruction(i1, n + 1);
				bc.remove(n + 2);
				count++;
				continue;

			}

			/*
			 * _BR[N]Z  X, _STMT, _BR Y, X: ==>  _BR[!N]Z Y
			 */

			if((( i1.opCode == ByteCode._BRNZ || i1.opCode == ByteCode._BRZ)) &&
				( i2.opCode == ByteCode._STMT ) &&
				( i3.opCode == ByteCode._BR) &&
				( i1.integerOperand == (n + 3))) {

				if( i1.opCode == ByteCode._BRNZ )
					i1.opCode = ByteCode._BRZ;
				else
					i1.opCode = ByteCode._BRNZ;
				i1.integerOperand = i3.integerOperand;
				bc.remove(n+1);
				bc.remove(n+1);
				count++;
				continue;
			}

		}	
		return count;
	}

	/**
	 * @param bc
	 */
	private void optDeadEnd(final ByteCode bc) {
		int n;
		int len;
		/*
		 * flow-of-control followed by _END (sometimes from above optimization)
		 * can be removed as long as the second _END isn't addressed by anyone.
		 */

		len = bc.size();
		for (n = 0; n < len - 1; n++) {
			final Instruction i1 = bc.getInstruction(n);
			if ((i1.opCode == ByteCode._END) || (i1.opCode == ByteCode._BR)
					| (i1.opCode == ByteCode._JMP)
					| (i1.opCode == ByteCode._RET)
					| (i1.opCode == ByteCode._JSB)
					| (i1.opCode == ByteCode._JMPIND)
					| (i1.opCode == ByteCode._JSBIND)) {

				final Instruction i2 = bc.getInstruction(n + 1);
				if (i2.opCode == ByteCode._END) {

					/* See if anyone reference location n+1 */
					boolean found = false;
					for (int j = 0; j < len; j++) {
						final Instruction i3 = bc.getInstruction(j);
						if (((i3.opCode == ByteCode._BR) ||
								(i3.opCode == ByteCode._BRZ)||
								(i3.opCode == ByteCode._BRNZ))
								&& i3.integerValid
								&& (i3.integerOperand == (n + 1)))
							found = true;
					}

					if (!found) {
						bc.remove(n + 1);
						break;
					}
				}
			}
		}
	}

	/**
	 * Scan the bytecode and collect _STRING declarations and combine them into a
	 * _STRPOOL block.  The code is modified to execute the pool initialization one
	 * time at the start of the code, and subsequent references to _STRING constants
	 * are converted to _STRPOOL references to the matching pool entry.
	 * @param bc
	 * @param newCount
	 * @return
	 */
	@SuppressWarnings("unchecked") 
	int optStringPool(final ByteCode bc, int newCount ) {

		/*
		 * If the optimization is disabled, do no work.  Also, this can only be
		 * done on linked programs, so if this byte code stream isn't linked then
		 * do no work.
		 */
		if (!OPT_STRING_POOL || !bc.fLinked )
			return count;

		class StringPoolCount {
			int count;
		};
		
		HashMap h = new HashMap();
		
		/*
		 * Step one, scan over the code and find _STRING instances, and count
		 * the number of times each constant is used.
		 */
		
		for( int n = 0; n < bc.size(); n++ ) {
			Instruction i = bc.getInstruction(n);
			if( i.opCode != ByteCode._STRING)
				continue;
			if( !i.stringValid)
				continue;
			
			StringPoolCount spc = (StringPoolCount) h.get(i.stringOperand);
			if( spc == null ) {
				spc = new StringPoolCount();
				spc.count = 0;
				h.put(i.stringOperand, spc);
			}
			spc.count = spc.count + 1;	
		}
		
		Vector<String> poolList = new Vector<String>();
		
		for( Iterator i = h.keySet().iterator(); i.hasNext();) {
			String k = (String) i.next();
			StringPoolCount spc = (StringPoolCount) h.get(k);
			if( spc.count >= STRING_POOL_THRESHHOLD)
				poolList.add(k);
		}
		h = null;
		
		/*
		 * Step two, scan over the data to see which strings should actually be
		 * converted.
		 */
		int count = newCount;
		StringPool tempPool = null;
		ByteCode initCode = null;
		int entryLocation = -1;
		int poolSize = 0;
		
		for( int n = 0; n < bc.size(); n++ ) {
			Instruction i = bc.getInstruction(n);
			if( i.opCode == ByteCode._STRPOOL)
				return count;
			
			/*
			 * Don't do this in DATA or WHERE code streams which are treated differently
			 */
			if( i.opCode == ByteCode._DATA || i.opCode == ByteCode._WHERE || i.opCode == ByteCode._JOIN ) {
				n = n + i.integerOperand;
				continue;
			}
			/*
			 * If we find the _ENTRY location, then remember it since this is where
			 * we will later insert a subroutine call to the initialization code. If
			 * we have already found at least one entry, then this is a SUBroutine
			 * and we can't do the optimization from here because the scope of the
			 * string pool is limited to the main program only.
			 */
			if( i.opCode == ByteCode._ENTRY) {
				if( entryLocation < 1) {
					entryLocation = n;
					continue;
				}
				if( entryLocation > 0)
					break;
			}
		
			if( i.opCode != ByteCode._STRING)
				continue;
			if( i.integerValid)
				continue;
			
			/*
			 * See if this one is in the list of candidate string constants?
			 */
			
			if( !poolList.contains(i.stringOperand))
				continue;
			
			/*
			 * We're going to create a pool entry. Make sure the pool and the
			 * code for initialization exist.
			 */
			if( tempPool == null ) {
				tempPool = new StringPool();
				initCode = new ByteCode(bc.getEnvironment());
			}
			
			int idx = tempPool.addString(i.stringOperand);
			if( idx > poolSize && initCode != null) {
				initCode.add(ByteCode._STRPOOL, idx, i.stringOperand);
				poolSize = idx;
			}
			i.integerValid = true;
			i.integerOperand = idx;
			i.stringValid = false;
			i.stringOperand = null;
			i.opCode = ByteCode._STRPOOL;
			count++;
		}
		
		if( count > newCount ) {
			bc.add(ByteCode._RET, 0);
			
			Instruction i = new Instruction(ByteCode._BR, bc.size());
			bc.insert(entryLocation+1, i);
			
			bc.concat(initCode);
			
			bc.add(ByteCode._BR, entryLocation+2);
		}
		
		return count;
	}


	/**
	 * @param bc
	 *            The ByteCode instruction stream to be optimized.
	 * @param newCount
	 *            The count of optimizations performed so far.
	 * @return Count of optimizations performed after this phase.
	 */
	private int optResolveBranches(final ByteCode bc, int newCount) {
		int n;
		int len;
		int count = newCount;
		
		/*
		 * A _BR with a string parameter is a named label branch. Let's find
		 * where the label is and replace the label with a byteCode address.
		 * Also, any _BR with no destination is really an _END
		 */
		len = bc.size();
		for (n = 0; n < len; n++) {

			final Instruction i1 = bc.getInstruction(n);

			/*
			 * See if it's a CALLF that should be converted to a 
			 * local call.
			 */
			if( i1.opCode == ByteCode._CALLF && i1.stringValid ) {
				if( bc.findLocalFunction(i1.stringOperand) != null ) {
					i1.opCode = ByteCode._CALLFL;
					count++;
					continue;
				}
			}

			/*
			 * Conversely see if it's a local call that is no longer
			 * valid and needs to be encoded as a general function call.
			 */

			if( i1.opCode == ByteCode._CALLFL ) {
				if( bc.findLocalFunction(i1.stringOperand) == null ) {
					i1.opCode = ByteCode._CALLF;
					count++;
				}
			}

			/*
			 * Fix up the BRANCH
			 */
			if ((i1.opCode == ByteCode._BR) && i1.stringValid)
				for (int n2 = 0; n2 < len; n2++) {
					final Instruction lbl = bc.getInstruction(n2);
					if (lbl.opCode == ByteCode._LABEL)
						if (lbl.stringOperand.equals(i1.stringOperand)) {
							i1.integerOperand = n2 + 1;
							i1.integerValid = true;
							i1.stringOperand = null;
							i1.stringValid = false;
							count++;
							break;
						}
				}
			else if ((i1.opCode == ByteCode._BR) && !i1.integerValid
					&& !i1.stringValid) {
				i1.opCode = ByteCode._END;
				count++;
				continue;
			} else if ((i1.opCode == ByteCode._BR) && i1.integerValid
					&& (i1.integerOperand == n + 1)) {
				bc.remove(n);
				len = len - 1;
				count++;
				continue;
			}

		}
		return count;
	}


	/**
	 * Various optimizations when constants have type conversions.  Most of these 
	 * can be handled at optimization time to remove the actual conversion, and
	 * just substitute a suitable initial constant value.
	 * @param bc
	 *            The ByteCode instruction stream to be optimized.
	 * @param newCount
	 *            The count of optimizations performed so far.
	 * @return Count of optimizations performed after this phase.
	 */
	private int optConstantCVT(final ByteCode bc, int newCount) {
		int n;
		int len;
		int oldCount;
		int count = newCount;
		/*
		 * _CONST* followed by _CVT to a numeric type can be replaced with the
		 * more correct constant load.
		 */

		oldCount = count;
		while (true) {
			len = bc.size();
			for (n = 0; n < len - 1; n++) {
				final Instruction i1 = bc.getInstruction(n);
				final Instruction i2 = bc.getInstruction(n + 1);

				/* Skip sections of DATA expression definitions */
				if( i1.opCode == ByteCode._DATA || i1.opCode == ByteCode._WHERE) {
					n = n + i1.integerOperand;
					continue;
				}
				
				if( i2.opCode != ByteCode._CVT)
					continue;

				if( i1.opCode == ByteCode._INTEGER | 
					i1.opCode == ByteCode._DOUBLE  |
					i1.opCode == ByteCode._BOOL | 
					i1.opCode == ByteCode._STRING ) {
					
					int op = i1.opCode;
					boolean modified = false;
					switch( i2.integerOperand) {
					case Value.INTEGER:
						
						i1.opCode = ByteCode._INTEGER;
						i1.integerValid = true;
						i1.stringValid = false;
						i1.doubleValid = false;
						
						switch( op ) {
						case ByteCode._INTEGER:
							modified = true;
							break;
						case ByteCode._DOUBLE:
							i1.integerOperand = (int) i1.doubleOperand;
							modified=true;
							break;

						case ByteCode._BOOL:
							modified=true;
							break;
						case ByteCode._STRING:
							i1.integerOperand = Integer.parseInt(i1.stringOperand);
							i1.stringOperand = null;
							modified = true;
							break;
						}
						break;
						
					case Value.DOUBLE:
						i1.opCode = ByteCode._DOUBLE;
						i1.integerValid = false;
						i1.stringValid = false;
						i1.doubleValid = true;
						
						switch( op ) {
						case ByteCode._INTEGER:
							i1.doubleOperand = i1.integerOperand;
							modified = true;
							break;
						case ByteCode._DOUBLE:
							modified=true;
							break;

						case ByteCode._BOOL:
							i1.doubleOperand = i1.integerOperand;
							break;
						case ByteCode._STRING:
							try {
								i1.doubleOperand = Double.parseDouble(i1.stringOperand);
							} catch (NumberFormatException e ) {
								i1.doubleOperand = Double.NaN;
							}
							i1.stringOperand = null;
							modified = true;
							break;
						}
						break;

					case Value.BOOLEAN:
						i1.opCode = ByteCode._BOOL;
						i1.integerValid = true;
						i1.stringValid = false;
						i1.doubleValid = false;
						
						switch( op ) {
						case ByteCode._INTEGER:
							i1.integerOperand = i1.integerOperand == 0 ? 0 : 1;
							modified = true;
							break;
						case ByteCode._DOUBLE:
							i1.integerOperand = i1.doubleOperand == 0.0 ? 0 : 1;
							modified=true;
							break;

						case ByteCode._BOOL:
							modified= true;
							break;
						case ByteCode._STRING:
							if( i1.stringOperand.equalsIgnoreCase("true") |
									i1.stringOperand.equalsIgnoreCase("t") |
									i1.stringOperand.equalsIgnoreCase("y") |
									i1.stringOperand.equalsIgnoreCase("yes"))
								i1.integerOperand = 1;
							else
								i1.integerOperand = 0;
							i1.stringOperand = null;
								
							modified = true;
							break;
						}
						break;

						
					case Value.STRING:
						i1.opCode = ByteCode._STRING;
						i1.integerValid = false;
						i1.stringValid = true;
						i1.doubleValid = false;
						
						switch( op ) {
						case ByteCode._INTEGER:
							i1.stringOperand = Integer.toString(i1.integerOperand);
							modified = true;
							break;
						case ByteCode._DOUBLE:
							i1.stringOperand = Double.toString(i1.doubleOperand);
							modified=true;
							break;

						case ByteCode._BOOL:
							i1.stringOperand = i1.integerOperand == 0 ? "false" : "true";
							modified = true;
							break;
						case ByteCode._STRING:
							modified = true;
							break;
						}
						break;


					}
					if( modified ) {
						bc.remove(n+1);
						len--;
						count++;
						continue;
					}
				}
				if (((i1.opCode == ByteCode._INTEGER)
						|| (i1.opCode == ByteCode._BOOL) || (i1.opCode == ByteCode._DOUBLE))
						&& ((i2.integerOperand == Value.STRING) || (i2.integerOperand == Value.FORMATTED_STRING))) {

					Value constFormat = null;
					switch (i1.opCode) {
					case ByteCode._INTEGER:
						constFormat = new Value(i1.integerOperand);
						break;
					case ByteCode._BOOL:
						constFormat = new Value(i1.integerOperand != 0);
						break;
					case ByteCode._DOUBLE:
						constFormat = new Value(i1.doubleOperand);
						break;

					}

					i1.opCode = ByteCode._STRING;
					i1.doubleValid = false;
					i1.integerValid = false;
					i1.stringValid = true;
					if( constFormat != null )
						i1.stringOperand = constFormat.getString();
					else
						i1.stringOperand = null;
					
					bc.remove(n + 1);
					len = len - 1;
					count++;
				}
			}
			if (oldCount == count)
				break;

			oldCount = count;
		}
		return count;
	}


	/**
	 * Is the given instruction one that causes a constant
	 * value to be loaded on the stack?
	 * @param i the Instruction to examine.
	 * @return true if the instruction is one that loads a constant
	 * scalar value on the stack.
	 */
	private boolean isValue(final Instruction i) {

		final int vops[] = new int[] {  ByteCode._INTEGER, 
										ByteCode._BOOL,
										ByteCode._DOUBLE, 
										ByteCode._STRING, 
										ByteCode._LOAD,
										ByteCode._LOADREF 
									  };

		for (int valueOp : vops )
			if (i.opCode == valueOp)
				return true;

		return false;

	}


	/**
	 * Scan the bytecode array for constant array or record declarations
	 * in loops, and pull them out of the loops.  These constant declarations
	 * are pretty expensive if they are for more than an empty array or 
	 * record, and should be created once and stored in temporary storage
	 * in the symbol table, and referenced as they are used in the program.
	 * <p>
	 * This module scans the code for _ARRAY and _RECORD operations where
	 * the contributing stack elements are all constants, and moves them
	 * to the start of the program prologue, with a _STOR to put them in
	 * the local symbol table.  The structured constant operations are then
	 * converted to a simple _LOADREF for the temporary value.
	 * <p>
	 * This operation is not done for empty arrays or records (which are
	 * faster to create than the symbol table lookup for the pre-constructed
	 * value) and it is not done for constants that are not in a FOR-NEXT
	 * loop, since there is no benefit to moving a constant used only once;
	 * in fact it incurs an extra STORE/LOAD step with no value.
	 * <p>
	 * Additionally, attempts to reference the identical constant in more
	 * than one place are pooled; that is, they all reference a single instance
	 * of the constant value.
	 * @param bc The Bytecode object to optimize
	 * @param count The current count of optimizations performed
	 * @return the new count of optimizations; this is count + number of
	 * constant structure definitions that are moved to the prolog.
	 */
	int optStructuredConstants( ByteCode bc, int count ) {

		/*
		 * If the program isn't linked, this won't do any good.
		 */
		if( !bc.fLinked )
			return count;

		if( !bc.getEnvironment().getBoolean("SYS$STRUCTURE_POOLING"))
			return count;

		HashMap<String,ByteCode> pool = new HashMap<String,ByteCode>();

		int nestedLoopCount = 0;

		for( int ix = 0; ix < bc.size(); ix++ ) {

			Instruction i1 = bc.getInstruction(ix);
			Instruction i2 = null;

			/* Skip sections of DATA expression definitions */
			if( i1.opCode == ByteCode._DATA || i1.opCode == ByteCode._WHERE) {
				ix = ix + i1.integerOperand;
				continue;
			}

			int elementCount = i1.integerOperand;
			int opcode = i1.opCode;

			/*
			 * Don't optimize empty records and arrays, they are faster
			 * to just build on the fly.
			 */
			if( elementCount == 0 )
				continue;

			/*
			 * The payoff for these really is in loops.  So let's track when
			 * we are in the body of a loop or not.  FOR and FORX increments
			 * the loop count, and NEXT decrements it.  Finally, if we're
			 * not in the body of a loop, just keep on scanning.
			 */
			if( opcode == ByteCode._FOR || opcode == ByteCode._FORX
					|| opcode == ByteCode._FOREACH) {
				nestedLoopCount++;
				continue;
			}

			if( opcode == ByteCode._NEXT) {
				nestedLoopCount--;
				continue;
			}

			if( nestedLoopCount < 1 )
				continue;

			/*
			 * We are in the body of a loop, see if this is a structure
			 * constant indicator or not.
			 */
			if(opcode == ByteCode._ARRAY || opcode == ByteCode._RECORD ) {

				/*
				 * Determine how many instructions make up the constant
				 * definition.  RECORDS are doubled because each item
				 * consumes two constants on the stack.
				 */
				if( opcode == ByteCode._RECORD )
					elementCount = elementCount * 2;

				/*
				 * Scan backwards looking at the instructions that make
				 * up the definition.  If they are only constant values,
				 * this is a candidate for pooling.  If any other instructions
				 * (usually LOAD operations, but sometimes expressions) are
				 * on the stack then this isn't a constant, and not suitable
				 * for pooling.
				 */
				boolean constantsOnly = true;
				int nx;
				for( nx = 0; nx < elementCount; nx++ ) {
					i2 = bc.getInstruction( ix-(nx+1));
					if( i2.opCode == ByteCode._INTEGER ||
							i2.opCode == ByteCode._BOOL ||
							i2.opCode == ByteCode._STRING ||
							i2.opCode == ByteCode._DOUBLE )
						continue;
					constantsOnly = false;
				}

				/*
				 * If the ARRAY or RECORD definition is comprised only of
				 * constant values, then we can extract the initializer into
				 * a separate storage area.
				 */
				if( constantsOnly ) {

					boolean alreadyGenerated = false;
					ByteCode tempBC = new ByteCode(bc.getEnvironment());
					for( nx = 0; nx <= elementCount; nx++ )
						tempBC.add( bc.getInstruction(ix-elementCount+nx));

					/*
					 * See if we already have an instance of this in our list.
					 */
					String generatedName = null;
					for( Iterator j = pool.values().iterator(); j.hasNext();) {
						ByteCode oldBC = (ByteCode) j.next();

						if( oldBC.size() != tempBC.size()+ 1)
							continue;

						int k;
						boolean match = true;
						for( k = 0; k < tempBC.size(); k++ ) {
							Instruction t1 = oldBC.getInstruction(k);
							Instruction t2 = tempBC.getInstruction(k);

							if( !t1.equals(t2)) {
								match = false;
								break;
							}
						}
						if( match ) {
							generatedName = oldBC.getName();
							break;
						}
					}

					if( generatedName != null ) {
						alreadyGenerated = true;
					} else {
						generatedName = ByteCode.tempName();
						pool.put(generatedName, tempBC);
						tempBC.add( ByteCode._DCLVAR, Value.RECORD, generatedName);
						tempBC.add( ByteCode._STOR, generatedName);
						tempBC.setName(generatedName);
					}
					for( nx = 0; nx < elementCount; nx++ )
						bc.remove(ix-elementCount);


					Instruction newRef = new Instruction(ByteCode._LOADREF, generatedName );
					bc.setInstruction(newRef, ix-elementCount);
					i2 = bc.getInstruction(ix - elementCount);
					i2.opCode = ByteCode._LOADREF;
					i2.stringValid = true;
					i2.stringOperand = generatedName;
					i2.integerValid = false;
					i2.integerOperand = 0;

					if( !alreadyGenerated ) {
						int poolPosition = 1;
						Instruction i3 = bc.getInstruction(poolPosition);
						if( i3.opCode == ByteCode._ENTRY)
							poolPosition = 2;

						for( nx = 0; nx < tempBC.size(); nx++ ) {
							bc.insert(poolPosition, tempBC.getInstruction((tempBC.size()-nx)-1));
						}
					}
				}
			}
		}
		return count;
	}

	/**
	 * Remove all _STMT instructions from a bytecode stream.  This makes the
	 * bytecode stream UNDEBUGGABLE, but improved performance.
	 * @param bc The bytecode stream to process
	 * @return The number of STMT operators removed.
	 */
	public int stripSTMT(ByteCode bc) {
		int count = 0;

		for( int ix = 0; ix < bc.size(); ix++ ) {
			Instruction i = bc.getInstruction(ix);
			if( i.opCode == ByteCode._STMT) {
				bc.remove(ix);
				count++;
			}
		}
		return count;
	}

}

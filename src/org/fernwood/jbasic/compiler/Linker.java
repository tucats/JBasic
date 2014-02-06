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

import java.util.Iterator;
import java.util.TreeMap;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.DataByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.statements.Statement;
import org.fernwood.jbasic.value.Value;

/**
 * This class implements the linker. When a program is compiled, each statement
 * generates a (usually short) sequence of bytecodes stored with the individual
 * statement objects in the program. Before a program can be executed, these
 * individual segments are combined into one large bytecode stream representing
 * the entire program. This is the "link" operation.
 * <p>
 * The Linker object has the following primary jobs:
 * <p>
 * <list>
 * <li>Physically concatenate the individual statement bytecode streams into a
 * master bytecode stream.
 * <li>Handle label references in instructions, by converting them to bytecode
 * addresses instead.
 * <li>Patch up branch operations in multi-statement constructs like FOR..NEXT
 * and DO..LOOP blocks, and handle loop flow-of-control like CONTINUE or 
 * END LOOP statements.
 * <li>Invokes the optimizer to improve the resulting code quality.
 * <li>Scan the program for DATA statement objects that are built as a map for
 * use by the READ statement in the language. 
 * <li>Review line number and label references in instructions like _REW to
 * be sure they actually exist.</list>
 * <p><Br><p>
 * 
 * @author tom
 * 
 */
public class Linker {

	/**
	 * Compile all statements in a program. This is used as part of the "unlink"
	 * operation, which discards the aggregated bytecode and re-generates it for
	 * each statement. This must be done when a program is modified by the user,
	 * and therefore must ultimately be re-linked.
	 * 
	 * @param pgm
	 *            The program that is to have each statement re-compiled
	 * @return A Status object indicating if there was a compile error or not.
	 */
	public static Status compile( final Program pgm) {

		if (pgm.isProtected())
			return new Status(Status.PROTECTED, pgm.getName());
		
		/*
		 * Step over the code and compile each statement that is
		 * not currently already in a compile state.
		 */
		Status status = new Status();
		final int pgmLen = pgm.statementCount();
		final Tokenizer t = new Tokenizer(null, JBasic.compoundStatementSeparator);
		
		for (int i = 0; i < pgmLen; i++) {
			
			/*
			 * Get each statement in the program in turn.
			 */
			final Statement stmt = pgm.getStatement(i);
			
			/*
			 * If the statement does not currently have bytecode
			 * associated with it, and was previously not seen as
			 * an empty statement (such as a comment), then load up
			 * the tokenizer with the statement text, and pass the
			 * tokenizer to the statement compile method.
			 * 
			 * Note that this is required because we don't actually
			 * save the tokenizer data for a statement, only the
			 * source string and the compiled code.  So recompilation
			 * requires re-tokenization.
			 * 
			 * Note that often there will be no need to do the compile
			 * at all; the statement is already compiled.  This method
			 * is called to ensure that a program is compiled after a
			 * new statement is added to a program, for example... the
			 * existing statements are already compiled, and the new
			 * statement is compiled directly on input. However, after
			 * an unlink() operation, a recompile is done on every statement
			 * to restore the per-statement byte codes.
			 */
			if ((stmt.byteCode == null) && !stmt.fEmptyStatement) {
				t.loadBuffer(stmt.statementText);
				status = stmt.compile(t);
			}
						
			/*
			 * If the compile of any single statement failed, then
			 * print it as an error and return the status.
			 */
			if (status.failed()) {
				pgm.session().stdout.println(t.error());
				status.setWhere(pgm.getName(), stmt.lineNumber);
				return status;
			}
		}
		return new Status();

	}

	/**
	 * Unlink (remove the aggregated bytecode) for the given program. This is
	 * executed directly by the UNLINK command, and also is done when a
	 * statement is changed requiring that the program be re-linked before it
	 * can be run.
	 * <p>
	 * The current linked code is deleted, and the marker that says that the
	 * scan for DATA statements has been done is cleared. Then the individual
	 * statements in the program are recompiled.
	 * 
	 * @param pgm
	 *            The program to unlink
	 * @return A status indicator that tells if the program was protected (and
	 *         therefore cannot be unlinked) or if a compile error occurred on
	 *         any given statement.
	 */
	public static Status unlink(final Program pgm) {

		if (pgm.isProtected())
			return new Status(Status.PROTECTED, pgm.getName());

		/*
		 * If it is already unlinked, we have no work to do here.
		 */
		if( !pgm.hasExecutable())
			return new Status();
		
		/*
		 * First, blow away the stored linked program code. This is the code
		 * stored in the first statement, and also linked from the main program
		 * object itself.
		 */
		pgm.clearExecutable();
		pgm.clearLocalFunctions();

		/*
		 * Because we're not in a linked state, we don't know if we have DATA
		 * statements or not, since program editing may have invalidated this
		 * flag setting. So clear it for now, and we'll reset it during the link
		 * operation.
		 */

		pgm.fHasData = false;

		/*
		 * Now, recompile each statement. This isn't needed from a runtime point
		 * of view (program editing causes each statement to be compiled as it's
		 * entered) but since a LINK destroys the per-statement code, this puts
		 * it back for debug viewing.
		 */

		return compile(pgm);
	}

	/**
	 * Link the given program. This aggregates the bytecode for each individual
	 * statement and also resolves/relocates all branches and statement label
	 * references as needed. Optionally it may invoke an optimization phase as
	 * well.
	 * <p>
	 * If a program is already linked, this method returns without doing any
	 * work; i.e. a program object is linked once. If the program statements are
	 * changed, then the unlink method must be called to undo the linkage
	 * operation.
	 * 
	 * @param pgm
	 *            The program to link
	 * @param strip Strip the program of STMT bytecodes?
	 * @return A status code indicating if there was an error in linking the
	 *         program.
	 */
	public static Status link(final Program pgm, boolean strip) {

		
		/*
		 * If we are already linked, do nothing else.
		 */

		if (pgm.hasExecutable())
			return new Status();

		/*
		 * Create new empty executable, linked to the first statement of the
		 * program.
		 */
		pgm.initExecutable();
		pgm.initDataElements();

		/*
		 * Step one. See if there are any statements in the program that weren't
		 * compilable. If so, we can't link. While we're at it, let's pick up
		 * the label info.
		 */

		int pgmLen = pgm.statementCount();
		int stmtIndex;
		Statement stmt;

		TreeMap<String, Linkage> labels = new TreeMap<String, Linkage>();
		ByteCode bc = pgm.getExecutable();

		for (stmtIndex = 0; stmtIndex < pgmLen; stmtIndex++) {

			stmt = pgm.getStatement(stmtIndex);
			
			/*
			 * If there's a statement label, scoop it up in the label map
			 * along with the current bytecode address.  Also, check each
			 * label to make sure it is used only once in the program.
			 */
			if (stmt.statementLabel != null) {
				final Linkage l = new Linkage(stmtIndex, bc.size(), stmt.statementLabel);
				if( labels.get(l.label) != null ) {
					labels = null;
					return new Status(Status.DUPLABEL, l.label);
				}
				labels.put(l.label, l);
			}

			/*
			 * If the statement has no executable statement associated with 
			 * it (a comment line, or a line with only a label on it, for
			 * example) then move on.
			 */
			if (stmt.fEmptyStatement)
				continue;

			/*
			 * If this statement is not empty but has no bytecode, then it
			 * is likely there is a compile error.  Make sure by trying to
			 * compile it again using the program text and a temporary
			 * tokenizer.  This can happen when a load operation brings in
			 * a program that mostly compiles but cannot link; we need to
			 * attempt to recompile each line to figure out which one is
			 * causing the failure, so it doesn't just report that the first
			 * statement (with no linked bytecode) is in error.
			 */
			if (stmt.byteCode == null) {
				stmt.compile(new Tokenizer(stmt.statementText, JBasic.compoundStatementSeparator));
				if( stmt.status.equals(Status.NOCOMPILE)) {
					stmt.byteCode = new ByteCode(pgm.session(), stmt);
					stmt.byteCode.add(ByteCode._STRING, stmt.statementText);
					stmt.byteCode.add(ByteCode._EXEC);
				}
			}
			/*
			 * If there's still no bytecode, then we can't link because there
			 * is a compile error on this statement.  Clear out the work we've
			 * done so far on the program and throw a link error.
			 */
			if (stmt.byteCode == null) {
				pgm.clearExecutable();
				labels = null;
				stmt.status = stmt.status.nest(Status.LINKERR);
				stmt.status.setWhere(pgm.getName(), stmt.lineNumber);
				return stmt.status;

			}

			/*
			 * If the statement has bytecode, then add it to the existing
			 * bytecode being accumulated as the linked program.
			 */
			bc.concat(stmt.byteCode);

		}

		/*
		 * Make sure there is a trailing END on the concatenated bytecode.
		 */
	
		bc.end();
		
		/*
		 * Store the label map in the bytecode now.  This lets the insert()
		 * and remove() operation take care of fixing linkages that get 
		 * changed.  As a result, this must be done before more editing
		 * or optimization occurs.
		 */
		bc.labelMap = labels;

		/*
		 * Step two.  Go through and find the _IF markers and
		 * convert them to real branch instructions.
		 */
		
		Status ifStatus = resolveIF( bc );
		if( ifStatus.failed()) {
			pgm.clearExecutable();
			labels = null;
			return ifStatus;
		}
		/*
		 * Step three, go back through and find all GOTO that are to labels, and
		 * convert them to _BR to a bytecode. Convert JMP instructions that 
		 * reference a statement ID to a bytecode BR operation.  Resolve GOSUB
		 * labels to be the correct statement bytecode address.
		 */

		pgmLen = bc.size();
		pgm.fHasData = false;
		int lastSTMT = 0;
		
		for (stmtIndex = 0; stmtIndex < pgmLen; stmtIndex++) {
			final Instruction i = bc.getInstruction(stmtIndex);
			if (i.opCode == ByteCode._DATA)
				pgm.fHasData = true;

			if( i.opCode == ByteCode._STMT)
				lastSTMT = i.integerOperand;
			
			/*
			 * If this is an _IF operator - after resolving them in the
			 * previous step, then it's a mismatched ELSE or END IF.
			 */
			
			if( i.opCode == ByteCode._IF ) {
				pgm.clearExecutable();
				labels = null;
				return new Status(Status.IFERR, lastSTMT);
			}
			/*
			 * If this is a GOTO or GOSUB with a line number, convert to
			 * a suitable JSB or BR instruction.
			 */
			boolean isGOSUB = (i.opCode == ByteCode._GOSUB);
			if((i.opCode == ByteCode._GOTO || isGOSUB) && i.integerValid) {
				int k = 0;
				int target = -1;
				int lineNumber = pgm.findExecutableLine( i.integerOperand);
				
				/*
				 * Now that we have the line number of the target (or the next
				 * active statement after the target) let's search for that
				 * target in the program code, and resolve it to the bytecode
				 * address for that statement.
				 */
				for( k = 0; k < bc.size(); k++) {
					Instruction inst = bc.getInstruction(k);
					if( inst.opCode != ByteCode._STMT)
						continue;
					
					if( inst.integerValid & inst.integerOperand == lineNumber ) {
						target = k + 1;
						i.opCode = isGOSUB? ByteCode._JSB : ByteCode._BR;
						i.integerValid = true;
						i.integerOperand = target;
						break;
					}
				}
				
				/*
				 * If we never found a target line, then throw an error about
				 * an unknown line number.
				 */
				if( target < 0 ) {
					pgm.clearExecutable();
					labels = null;
					return new Status(Status.LINENUM, Integer.toString(lineNumber), 
							new Status(Status.ATLINE, lastSTMT));
				}

			}
			
			/*
			 * JSB with a string label needs to be converted to a bytecode address.
			 */
			if (i.opCode == ByteCode._JSB && i.stringValid) {
				final Linkage l = labels.get(i.stringOperand);
				if (l == null) {
					pgm.clearExecutable();
					labels = null;
					return new Status(Status.LINKERR, 
							new Status(Status.NOSUCHLABEL, i.stringOperand,
									new Status(Status.ATLINE, lastSTMT)));
				}
				
				i.stringValid = false;
				i.stringOperand = null;
				i.integerOperand = l.byteAddress;
				i.integerValid = true;

			}
	
			if( i.opCode == ByteCode._REW & i.stringValid) {
				final Linkage l = labels.get(i.stringOperand);
				if( l == null )
					return new Status(Status.LINKERR, 
							new Status(Status.NOSUCHLABEL, i.stringOperand,
									new Status(Status.ATLINE, lastSTMT)));		
			}
			else
				if( i.opCode == ByteCode._REW & i.integerValid) {
					int target = 0;
					int lineNumber = i.integerOperand;
					for( int k = 0; k < bc.size(); k++) {
						Instruction inst = bc.getInstruction(k);
						if( inst.opCode != ByteCode._STMT)
							continue;
						
						if( inst.integerValid & inst.integerOperand == lineNumber ) {
							target = k + 1;
							break;
						}
					}
					
					/*
					 * If we never found a target line, then throw an error about
					 * an unknown line number.
					 */
					if( target <= 0 ) {
						pgm.clearExecutable();
						labels = null;
						return new Status(Status.LINKERR, 
								new Status(Status.LINENUM, Integer.toString(lineNumber), 
									new Status(Status.ATLINE, lastSTMT)));
					}

				}
			
			/*
			 * JMP with a string label needs to be converted to a BR to a bytecode
			 * address.
			 */
			if ((i.opCode == ByteCode._JMP || i.opCode == ByteCode._GOTO)
					&& i.stringValid) {
				final Linkage l = labels.get(i.stringOperand);
				if (l == null) {
					pgm.clearExecutable();
					labels = null;
					return new Status(Status.LINKERR, 
							new Status(Status.NOSUCHLABEL, i.stringOperand,
									new Status(Status.ATLINE, lastSTMT)));
				}
				i.opCode = ByteCode._BR;
				i.stringValid = false;
				i.stringOperand = null;
				i.integerOperand = l.byteAddress;
				i.integerValid = true;
			}
		}

		/*
		 * Step four, find the _FOR/_FORX operators and patch in the location
		 * of the NEXT operator that goes with it.
		 */

		for (stmtIndex = 0; stmtIndex < pgmLen; stmtIndex++) {
			final Instruction i = bc.getInstruction(stmtIndex);

			if ((i.opCode != ByteCode._FOR) && (i.opCode != ByteCode._FORX)
				&& (i.opCode != ByteCode._FOREACH))
				continue;

			for (int j = stmtIndex + 1; j < pgmLen; j++) {
				final Instruction n = bc.getInstruction(j);
				if (n.opCode != ByteCode._NEXT)
					continue;
				if (n.stringOperand.equals(i.stringOperand)) {
					i.integerOperand = j + 1;
					i.integerValid = true;
					n.integerOperand = stmtIndex + 1;
					n.integerValid = true;
					break;
				}
			}
		}

		/*
		 * Step five, repeat the operation for DO..LOOP pairs,
		 * to reset the starting DO to include an offset to the LOOP 
		 * instruction.  This is for the DO..WHILE case.  This causes the
		 * evaluation to be done once before the loop runs even one time.
		 * 
		 * NOTE NOTE NOTE the default was to not optimize the case for
		 * loops where the body is after the condition, because this
		 * is how JBasic was for years.  However, this seems dumb; the
		 * optimization should be done regardless of where the condition
		 * is in relation to the loop body.  The SYS$LOOP_OPT system
		 * variable controls this behavior. If you want both loop formats 
		 * to detect the case where the loop isn't to run at all, set 
		 * the global property SYS$LOOP_OPT.  [This is the default now]
		 */

		boolean topLoopOpt = false;
		if( pgm.session() !=null )
			topLoopOpt = pgm.session().getBoolean("SYS$LOOP_OPT");
	
		if( topLoopOpt ) {
			for( stmtIndex = 0; stmtIndex < pgmLen; stmtIndex++ ) {
				final Instruction i = bc.getInstruction(stmtIndex);
				if( i.opCode == ByteCode._LOOP && i.integerOperand > 0) {

					/*
					 * Back up to find the _STMT that starts this expression.
					 */

					int k;
					for( k = stmtIndex-1; k >= 0; k-- ) {
						final Instruction i1 = bc.getInstruction(k);
						if( i1.opCode == ByteCode._STMT)
							break;
					}
					/*
					 * Now back up and find the matching DO instruction that
					 * is the inner-most nested element (not already branch-resolved)
					 */
					for( int j = stmtIndex-1; j >= 0; j-- ) {
						final Instruction i2 = bc.getInstruction(j);
						if( i2.opCode == ByteCode._DO && i2.integerValid && i2.integerOperand == 0 ) {
							i2.integerOperand = k;
							break;
						}
					}
				}
			}
		}

		/*
		 * Step six, repeat the operation for DO WHILE/UNTIL .. LOOP pairs 
		 * where the condition was expressed at the top of the loop. Also
		 * handle out-of-loop branch operations (END LOOP and CONTINUE LOOP)
		 * operations.
		 */
		
		for( stmtIndex = 0; stmtIndex < pgmLen; stmtIndex++ ) {
			final Instruction i = bc.getInstruction(stmtIndex);
			
			/*
			 * If this is a CONTINUE LOOP or END LOOP instruction, replace it
			 * with a branch to the end or start of the loop body.
			 */
			int op = 0;
			if( i.opCode == ByteCode._BRLOOP) {
				int found = -1;
				boolean forward = (i.stringOperand.equals("F"));
				i.stringOperand = null;
				i.stringValid = false;
				
				if( forward ) {
					for( int idx = stmtIndex; idx < bc.size(); idx++ ) {
						op = bc.getInstruction(idx).opCode;
						if( op == ByteCode._LOOP || op == ByteCode._NEXT) {
							found = idx + 1;
							break;
						}
					}
				}
				else {
					for( int idx = stmtIndex; idx >= 0; idx-- ) {
						op = bc.getInstruction(idx).opCode;
						if( op == ByteCode._FOR | op == ByteCode._FORX | op == ByteCode._FOREACH) {
							
							/*
							 * If it's a FOR operation, then we really need to find the end of the
							 * loop and branch to the NEXT, which is where the loop evaluation
							 * really occurs.
							 */
							
							while( idx < bc.size()) {
								idx++;
								if( bc.getInstruction(idx).opCode == ByteCode._NEXT) {
									found = idx;
									break;
								}
							}
							break;
						}
						if( op == ByteCode._DO) {
							
							/* 
							 * Start of body has to include the conditional if
							 * present, so back up to the STMT marker.
							 */
							while( idx > 0) {
								idx --;
								if( bc.getInstruction(idx).opCode == ByteCode._STMT)
									break;
							}
							found = idx + 1;
							break;
						}
					}
				}
				
				/*
				 * If this was a CONTINUE in a FOR..NEXT loop, then the patch up has already
				 * occurred properly, and we're done.
				 */
				if( found == -2 )
					continue;
				
				/*
				 * If we didn't find the start or end of the loop body, then error.
				 */
				if( found < 0 ) {
					pgm.clearExecutable();
					labels = null;
					return new Status(Status.LINKERR, new Status(Status.MISMATDO));
				}
				
				/*
				 * Update the instruction with a suitable branch operation to represent
				 * the CONTINUE or END LOOP operation.  Normally this a _BR, but if this
				 * was a jump outside the loop, we use BRLOOP which also discards the
				 * pending loop block on the loop stack.
				 */
				bc.setInstruction(new Instruction(op == ByteCode._NEXT ? ByteCode._BRLOOP : ByteCode._BR, found), stmtIndex);
				continue;
			}
			
			if( i.opCode == ByteCode._LOOP && i.integerOperand == 0) {
			
				/*
				 * Now back up and find the matching DO instruction that
				 * is the inner-most nested element (not already branch-resolved)
				 */
				for( int j = stmtIndex-1; j >= 0; j-- ) {
					final Instruction i2 = bc.getInstruction(j);
					if( i2.opCode == ByteCode._DO && !i2.integerValid ) {
						

						/*
						 * Back up to find the _STMT that starts this expression.
						 */
						
						int k;
						for( k = j-1; k >= 0; k-- ) {
							final Instruction i1 = bc.getInstruction(k);
							if( i1.opCode == ByteCode._STMT)
								break;
						}
						
						/* 
						 * Reset the _LOOP to just be a branch back to the top of the
						 * condition test.
						 */
						
						i.opCode = ByteCode._BR;
						i.integerOperand = k;
						i.integerValid = true;
						
						i2.opCode = ByteCode._BRZ;
						i2.integerOperand = stmtIndex + 1;
						i2.integerValid = true;
						break;
					}
				}
			}
		}
	
		/*
		 * Step seven, compress out the NOOPs.  Also, detect if there are
		 * any _ERROR statements in this scan, since we'd need to know that
		 * in order to set up stacks at runtime.
		 */

		bc.fHasErrorHandler = false;
		for (stmtIndex = 0; stmtIndex < pgm.executableSize(); stmtIndex++) {

			final Instruction x = bc.getInstruction(stmtIndex);
			
			/*
			 * If this is an _ERROR handler then we need to note that 
			 * there are error handlers in this bytecode unit.
			 */
			
			if( x.opCode == ByteCode._ERROR ) {
				bc.fHasErrorHandler = true;
				continue;
			}

			if (x.opCode != ByteCode._NOOP)
				continue;
			bc.remove(stmtIndex);
			stmtIndex = stmtIndex - 1;
		}

		/*
		 * Step eight, locate any _CONSTANT blocks and move them to the front
		 * of the code stream.  Start by finding the first instruction after
		 * the prolog (_STMT and _ENTRY info)
		 */
		
		int codeBase = 0;
		
		if( bc.getInstruction(codeBase).opCode == ByteCode._STMT)
			codeBase++;
		if( bc.getInstruction(codeBase).opCode == ByteCode._ENTRY)
			codeBase++;
		
		SymbolTable constantPool = new SymbolTable(null, "Constant pool", null);
		
		for( stmtIndex = codeBase; stmtIndex < bc.size(); stmtIndex++ ) {
			Instruction x = bc.getInstruction(stmtIndex);
			
			/* Skip over all DATA constants */
			if( x.opCode == ByteCode._DATA) {
				stmtIndex = stmtIndex + x.integerOperand;
				continue;
			}
			/* If not a _CONSTANT block, we don't care */
			if( x.opCode != ByteCode._CONSTANT) 
				continue;
			
			/* Process a _CONSTANT block */
			int count = x.integerOperand;
			String name = x.stringOperand;
			bc.remove(stmtIndex); /* Remove the _CONSTANT itself */
			
			ByteCode constantBlock = new ByteCode(null);
			for( int ix = 0; ix < count; ix++) {
				constantBlock.add(bc.getInstruction(stmtIndex));
				bc.remove(stmtIndex);
			}
			
			/*
			 * Run the bytecode to capture the actual value.  See if we already
			 * know about this value.
			 */
						
			constantBlock.run(constantPool, 0);
			Value result = constantBlock.getResult();
			Iterator i = constantPool.table.keySet().iterator();
			boolean found = false;
			
			while( i.hasNext()) {
				String entryName = (String) i.next();
				Value test = constantPool.localReference(entryName);
				if( test.match(result)) {
					name = entryName;
					found = true;
					break;
				}
			}
			
			/*
			 * If we don't already have this constant, then save it in our pool.
			 * Also, move the instructions to the start of the program so they
			 * will be executed every time.
			 */
			if (!found) {
				Instruction cbInst = null;
				int constantType = Value.UNDEFINED;
				for (int ix = 0; ix < count; ix++) {
					cbInst = constantBlock.getInstruction(ix);
					bc.insert(codeBase + ix, cbInst);
				}
				
				/*
				 * Depending on what the last instruction was, it tells us what kind
				 * of constant this is (ARRAY or RECORD).
				 */
				if( cbInst != null ) {
					if( cbInst.opCode == ByteCode._ARRAY)
						constantType = Value.ARRAY;
					else
					if( cbInst.opCode == ByteCode._RECORD)
						constantType = Value.RECORD;
				}
				try {
					constantPool.insert(name, result);
				} catch (JBasicException e) {
					e.printStackTrace();
				}
				bc.insert(codeBase + count, new Instruction(ByteCode._DCLVAR, -constantType, name));
			}
			else {
				x = bc.getInstruction(stmtIndex);
				if( x.opCode == ByteCode._LOADREF)
					x.stringOperand = name;
			}
			stmtIndex--;
		
		}
		
		/*
		 * Step nine is a sanity check - there must not be mismatched DO and 
		 * LOOP instructions. While we're here, pick up the _ENTRY symbols 
		 * and add them to the symbol table; we need these for supporting 
		 * CALL's to local SUBroutines.
		 */
		
		int nestedDO = 0;
		int nestedFOR = 0;
		int lastStatement = 0;
		
		for( int idx = 0; idx < bc.size(); idx++ ) {
			final Instruction i = bc.getInstruction(idx);
			
			/*
			 * If it's an _ENTRY for a SUB routine, add it to the link map
			 */
		
			if( i.opCode == ByteCode._STMT)
				lastStatement = i.integerOperand;
			else
			if( i.opCode == ByteCode._ENTRY & i.integerOperand == ByteCode.ENTRY_SUB ) {
				
				Linkage sub = new Linkage();
				sub.byteAddress = idx;
				sub.id = lastStatement;
				sub.label = Linkage.ENTRY_PREFIX + i.stringOperand;
				if( labels.get(sub.label) != null ) {
					labels = null;
					return new Status(Status.DUPLABEL, sub.label);
				}
				labels.put(sub.label, sub);

			}
			else
			/*
			 * Handle tracking balance of DO..LOOP and FOR..NEXT
			 */
			if( i.opCode == ByteCode._DO)
				nestedDO++;
			else if( i.opCode == ByteCode._LOOP)
				nestedDO--;
			else if( i.opCode == ByteCode._FOREACH |
				i.opCode == ByteCode._FOR |
				i.opCode == ByteCode._FORX )
				nestedFOR ++;
			else if( i.opCode == ByteCode._NEXT)
				nestedFOR--;
		}
		
		if( nestedDO != 0 | nestedFOR != 0 ) {
			pgm.clearExecutable();
			labels = null;
		}
		if( nestedDO != 0 ) {
			return new Status(Status.LINKERR, new Status(Status.MISMATDO));
		}
		if( nestedFOR != 0 ) {
			return new Status(Status.LINKERR, new Status(Status.MISMATFOR));
		}
		
		
		/*
		 * Ensure that we have a current and correct mapping of the local
		 * function definitions.
		 */
		
		pgm.readyLocalFunctions();

		/*
		 * Mark this as a linked.
		 */
		bc.fLinked = true;

		/*
		 * Make the executed code all belong to the first statement. Clear the
		 * code for all other statements.
		 */

		pgmLen = pgm.statementCount();
		pgm.getStatement(0).byteCode = bc;

		for (stmtIndex = 1; stmtIndex < pgmLen; stmtIndex++)
			pgm.getStatement(stmtIndex).byteCode = null;

		/*
		 * If directed, remove _STMT and _DEBUG statements from the code stream,
		 * which supports protected programs.
		 */
		if( strip )
			strip(pgm);
		
		/*
		 * Now that the program is strung together in single code stream,
		 * make a final pass looking for branch optimizations.  After all 
		 * such optimizations are done, see if there are constant array
		 * or record declarations that should be pooled.
		 */
		
		Optimizer opt = new Optimizer();
		
		int count = 0;
		int lastCount = count-1;
		ByteCode linkedStream = pgm.getExecutable();
		boolean optFlag = true;
		if( pgm.session() != null )
			optFlag = pgm.session().getBoolean("SYS$OPTIMIZE");
		
		if( optFlag ) {
			while( count > lastCount ) {
				lastCount = count;
				count = opt.optBranches(linkedStream, count );
				count = opt.optStringPool(linkedStream, count);
			}

			opt.optStructuredConstants( linkedStream, count );

			Status status = pgm.session().pmOptimizer.optimize(linkedStream);
			if( status.failed())
				return status;
		}
		
		/*
		 * Last step - make sure any DATA statements have been collected and
		 * stored in the data vector.
		 */

		return buildData(pgm);

	}



	/**
	 * Given a bytecode stream, resolve any pending IF-THEN-ELSE branches nested in
	 * the stream.  This is done for both full programs that are being linked, and 
	 * for individual statements being compiled as expressions.
	 * @param bc the ByteCode stream to resolve IF..THEN..ELSE links for.
	 * @return a Status indicating if the operation was successfull.  Most common
	 * errors are mismatched statements or bad nesting.
	 */
	public static Status resolveIF(ByteCode bc) {
		
		
		int stmtIndex;
		for( stmtIndex = 0; stmtIndex < bc.size(); stmtIndex++ ) {
			
			Instruction i = bc.getInstruction(stmtIndex);
			if( i.opCode != ByteCode._IF | i.integerOperand != 1 )
				continue;
			
			/*
			 * Found the start.  Scan to the end of the code looking for
			 * matching IF 2 and IF 3 blocks.  Matching is based on nesting.
			 */
			
			int nest = 0;
			int elseLoc = 0;
			int endLoc = 0;
			int lastSTMT = 0;

			for( int idx = stmtIndex + 1; idx < bc.size(); idx++) {
				
				i = bc.getInstruction(idx);
				if( i.opCode == ByteCode._STMT ) {
					lastSTMT = i.integerOperand;
					continue;
				}
				if( i.opCode != ByteCode._IF)
					continue;
				
				if( i.integerOperand == 1 ) {
					nest++;
					continue;
				}
				
				if( i.integerOperand == 3 ) {
					if( nest == 0 ) {
						endLoc = idx;
						break;
					}
					nest--;
					if( nest < 0 ) {
						return new Status(Status.LINKERR,
								new Status(Status.IFERR, lastSTMT));
					}
					continue;
				}
				if( i.integerOperand == 2 & nest == 0 )
					elseLoc = idx;
				
			}
			if ( nest != 0 || endLoc == 0 ) {
				return new Status(Status.IFERR, lastSTMT);
			}
			/*
			 * If no ELSE clause, simple branch is all we need.  Note that
			 * we must create a new Instruction object to insert into this
			 * code because otherwise we're modifying one that came from the
			 * original per-statement bytecode... changing those _IF opcodes
			 * to branches as well.
			 */
			if( elseLoc == 0 ) {
				i = new Instruction(ByteCode._BRZ, endLoc + 1);
				bc.setInstruction(i, stmtIndex);
				bc.remove(endLoc);
				continue;
			}
			
			/*
			 * There's an ELSE clause, so a little more work.  Same rule
			 * as above; we generate new instructions here so we don't
			 * mess with the instructions from the individual statements.
			 */
			i = new Instruction(ByteCode._BRZ, elseLoc + 1);
			bc.setInstruction(i, stmtIndex);
			
			i = new Instruction(ByteCode._BR, endLoc + 1);
			bc.setInstruction(i, elseLoc);
			
			bc.remove(endLoc);
		
			
		}
		return new Status();
	}

	/**
	 * Strip a program of it's _STMT which makes it run faster but be
	 * it cannot be debugged.  There is some question as to whether
	 * a REWIND by label will work properly in a protected program 
	 * either.<p>
	 * 
	 * NOTE: THIS IS NOT RECOMMENDED.  Performance testing shows that the
	 * _STMT operations don't take very much time (stripping the HELP verb,
	 * for example, which runs about 2000 statements, shows only about .1%
	 * savings by stripping the _STMT's.
	 * 
	 * @param p the Program whose extraneous statements are to be removed.
	 */
	public static void strip( Program p ) {
		
		ByteCode bc = p.getExecutable();
		if( bc == null )
			return;
		
		/*
		 * First we must scan the bytecode to ensure there are instructions
		 * that require search for _STMT operators later.  Examples are
		 * GOTO USING and REWIND.
		 */
		
		boolean canStrip = true;
		for( int ix = 0; ix < bc.size(); ix++ ) {
			Instruction i = bc.getInstruction(ix);
			if(( i.opCode == ByteCode._REW & i.integerOperand > 0 ) ||
				 i.opCode == ByteCode._GOTO  ||
				 i.opCode == ByteCode._JMPIND ||
				 i.opCode == ByteCode._JSBIND ){
				canStrip = false;
				break;
			}
		}
		
		/*
		 * Now rescan removing the offending byte codes.
		 */
		for( int ix = 0; ix < bc.size(); ix++ ) {
			Instruction i = bc.getInstruction(ix);
			if( i.opCode == ByteCode._STMT ) {
				if( canStrip ) {
					bc.remove(ix);
				}
				else
					i.stringOperand = "";
				continue;
			}
			
			if( i.opCode == ByteCode._DEBUG) {
				bc.remove(ix);
				continue;
			}
		}

		/*
		 * The first statement is retained in a stripped program to hold
		 * extra context.  But we don't want it's program text to be visible,
		 * via something like the PROGRAM().LINES[] array.  So zap the text
		 * of the first statement.
		 */
		
		p.getStatement(0).clearText();
		
		/*
		 * We've monkeyed with the program quite a bit... if there are DATA
		 * statements let's recollect that information now.
		 */

		p.initDataElements();
		
		return;
	}
	/**
	 * Scan the current program for DATA statements, and collect the bytecode
	 * for each DATA element in an addressable map. This map is used later by
	 * READ statements to step through the various DATA declarations.
	 * <p>
	 * If there are no DATA statements in the program, then there is no work to
	 * do. Additionally, if the map has already been built then it does not need
	 * to be built again (this is undone by an unlink operation), and the "next
	 * DATA element" pointer is all that must be reset.
	 * <p>
	 * If a DATA scan is required, then each statement is examined to see if it
	 * is a DATA statement, and the bytecode streams for each element are
	 * gathered in a map stored in the program object.
	 * 
	 * @param p the Program whose DATA statements are to be collected for
	 * runtime execution.
	 * @return Status indicating if there was an error building the DATA list
	 *         (such as a program that has not been linked).
	 */
	public static Status buildData(final Program p) {

		/*
		 * If the current program has no DATA statements, then we have no work
		 * to do.
		 */

		if (!p.fHasData)
			return new Status();

		/*
		 * If the DATA structures have already been scanned, we don't need to do
		 * it again. Just reset the data pointer and we are done.
		 * 
		 * The DATA will need initialization if a program statement has been
		 * modified since the last time the data was initialized.
		 */
		if (!p.dataElementsNeedInitialization()) {
			p.rewindDataElements();
			return new Status();
		}

		p.initDataElements();

		final ByteCode bc = p.getExecutable();
		if (bc == null)
			return new Status(Status.LINKERR);

		final int ix = bc.size();
		int lineNumber = 0;
		
		for (int i = 0; i < ix; i++) {

			Instruction inst = bc.getInstruction(i);
			if( inst.opCode == ByteCode._STMT &&
					inst.integerOperand > lineNumber )
					lineNumber = inst.integerOperand;
			
			if (inst.opCode != ByteCode._DATA)
				continue;

			final int bcount = inst.integerOperand;
			final DataByteCode dataByteCode = new DataByteCode(p.session());
			for (int j = 0; j < bcount; j++) {
				inst = bc.getInstruction(i + j + 1);
				dataByteCode.add(inst);
			}
			dataByteCode.add(ByteCode._END);
			p.addDataElement(lineNumber, dataByteCode);
			i = i + bcount - 1;
		}

		return new Status();
	}
}

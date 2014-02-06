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
package org.fernwood.jbasic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.fernwood.jbasic.compiler.Linker;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.DataByteCode;
import org.fernwood.jbasic.runtime.Functions;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.runtime.JBasicDebugger;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.LoopManager;
import org.fernwood.jbasic.runtime.ScopeControlBlock;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.statements.Statement;
import org.fernwood.jbasic.value.Value;

/**
 * Class that defines a stored program. This contains a vector of statements,
 * and a pointer to the next statement to execute. The program also contains the
 * generated code for the linked program, and state information about the
 * program when it is run (such as whether dynamic typing is permitted, etc.)
 * <p>
 * Basic functions of a program include:
 * <p>
 * <list>
 * <li>Adding and removing lines of source to a program object
 * <li>Compiling and linking the program into an executable program object
 * <li>Invoking the program object as in a RUN command
 * <li>Support services such as building label maps and handling runtime object
 * management for GOSUB/RETURN </list>
 * <p>
 * 
 * @author cole
 * @version 1.0 July 6, 2004
 */

public class Program {

	/**
	 * The enclosing JBasic object for this execution session.
	 */
	private JBasic session;

	/**
	 * The name of the program object
	 */

	private String name;

	/**
	 * Get the name of the program object.
	 * 
	 * @return A string containing the name, or null if no name was ever set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * The name of the file it came from, or null if it is the same as the
	 * program object name.
	 */
	private String sourceFileName;

	/**
	 * The next element in the statement arrayValue to execute. This is
	 * essentially the "program counter" for the executing program, and can be
	 * reset on a branch statement as needed.
	 */
	public int next;

	/**
	 * Counts the number of times this program has been run since it was loaded
	 * into memory. Used for diagnostics and debugging of user programs.
	 */
	private int runCount;

	/**
	 * Get the run count for a program, used for profiling and SHOW PROGRAMS
	 * output.
	 * 
	 * @return Count of the number of times this program has been run.
	 */

	public int getRunCount() {
		return runCount;
	}

	/**
	 * A flag that says this program is registered, that is it has been given a
	 * named slot in the list of programs owned by the JBasic driver shell, and
	 * can be invoked by name via a RUN command.
	 */
	private boolean fRegistered;

	/**
	 * Flag that indicates if there are _DATA opcodes in this program. If so,
	 * then before each execution, the DATA pointers must be re-initialized.
	 */
	public boolean fHasData;

	/**
	 * Flag indicating if this program module uses/expects static typing. If
	 * true, then the name of a variable generally determines its type, but can
	 * be overridden by a DIM statement. If false, then dynamic type binding is
	 * used for this program, which means a symbol takes on the type of whatever
	 * value is stored in it, and can change over the life of the program.
	 */
	public boolean fStaticTyping;

	/**
	 * Flag indicates if the program/verb/function is owned by the system. This
	 * flag is set for all objects initially loaded from the library files. If a
	 * user program is created, or the user modifies a program, this flag is
	 * turned off.
	 */
	private boolean fSystemObject;

	/**
	 * Return a flag indicating if the program is owned by the JBasic runtime or
	 * was created by the user.
	 * 
	 * @return A boolean flag. True if the current program is a system object,
	 *         or false if created by a user.
	 */
	public boolean isSystemObject() {
		return fSystemObject;
	}

	/**
	 * Flag indicates if this object has been modified by the user since the
	 * last SAVE operation
	 */
	private boolean fModified;

	/**
	 * Determine if the program object been modified by the user.
	 * 
	 * If so, it is a candidate for a SAVE operation before QUIT.
	 * 
	 * @return True if the object has been modified and should be saved.
	 */
	public boolean isModified() {
		return fModified;
	}

	/**
	 * Clear the modification bit for a program. This is done by the loader
	 * after storing statements read from a file, so the program is considered
	 * pristine.
	 */
	public void clearModifiedState() {
		fModified = false;
	}

	/**
	 * This is the list of local functions that are only available to this
	 * program scope, such as functions created with the DEFFN statement.
	 */
	HashMap<String,ByteCode>localFunctions;
	
	/**
	 * The list of program statements, stored in a numerically indexed
	 * arrayValue. The element 'next' identifies which statement is executed
	 * next.
	 */
	private ArrayList<Statement> statements;

	/**
	 * This lists the DATA statement elements compiled in the program. Each
	 * element is a ByteCode object that will execute and produce the data
	 * element (often just a constant, but possibly a complex array or record,
	 * etc.
	 */

	private ArrayList<DataByteCode> dataElements;
	
	/**
	 * This defines the current (next to be read) position in the dataElements
	 * vector. This will be zero if there are no dataElements in the current
	 * program. This is advanced by each READ statement.
	 */
	int dataElementPosition = 0;

	/**
	 * This flag indicates if we have read the last active DATA element in the
	 * program. This is true if there are no DATA elements at all in the
	 * program, and is also set to true when dataElement reaches the end of the
	 * list of DATA values for the program. The EOD() function tests this flag,
	 * and the REWIND statement resets it.
	 */
	boolean fAtEOD = true;

	/**
	 * Test to see if the DATA elements in this program need to be pre-processed
	 * before execution of the program can happen. This will be used to scan the
	 * program for DATA items to construct the runtime map of DATA items.
	 * 
	 * @return True if the runtime DATA elements map needs to be initialized.
	 *         False if it has already been constructed and is valid (no
	 *         statements edited in the program since it was built, etc.).
	 */
	public boolean dataElementsNeedInitialization() {
		return dataElements == null;
	}

	/**
	 * Initialize the runtime DATA element map to a known state. This empties
	 * the current map (if there is one) and sets the state for READ to begin at
	 * the beginning of the map.
	 * 
	 */
	public void initDataElements() {
		dataElements = null;
		rewindDataElements();
		fAtEOD = true;
	}

	/**
	 * Rewind the DATA pointer so the first item will be read next, if there is
	 * one. Sets the atEOD flag to indicate if there are any DATA elements to be
	 * read.
	 * 
	 */
	public void rewindDataElements() {
		dataElementPosition = 0;
		if (dataElements == null) 
			fAtEOD = true;
		else
			fAtEOD = (dataElements.size() <= 0);
	}

	/**
	 * Rewind the DATA element(s) to a given label location in 
	 * the program.
	 * @param label the string label value.
	 * @throws JBasicException if the label is not found
	 */
	public void rewindDataElements( String label ) throws JBasicException {
		int lineNumber = this.getExecutable().findLabel(label);
		if( lineNumber == 0 )
			throw new JBasicException(Status.NOSUCHLABEL, label);
		if( lineNumber > 0 ) {
			Instruction i = this.getExecutable().getInstruction(lineNumber);
			if( i.opCode == ByteCode._STMT && i.integerOperand > 0 )
				rewindDataElements(i.integerOperand);
		}
	}
	
	/**
	 * Rewind to the DATA element(s) for a given line number
	 * @param lineNumber the target line number
	 * @throws JBasicException if the line number is not found
	 */
	public void rewindDataElements(int lineNumber) throws JBasicException {
		
		if( dataElements == null )
			throw new JBasicException(Status.LINENUM, lineNumber);
		rewindDataElements();

		/*
		 * Skip through the data elements trying to find the
		 * line number position; we will make this the new
		 * READ location.
		 */
		int count = dataElements.size();
		int ix;
		boolean found = false;
		for( ix = 0; ix < count; ix++ ) {
			
			/*
			 * Get the bytecode and look at the first instruction
			 */
			DataByteCode bc = dataElements.get(ix);
			Instruction i = bc.getInstruction(0);
			
			/*
			 * If it's a _STMT with a line number >= our target, we're done.
			 */
			if( i.opCode == ByteCode._STMT && i.integerOperand == lineNumber ) {
				dataElementPosition = ix;
				found = true;
				break;
			}
		}
		
		/*
		 * If we never found the line number, consider ourselves at the end
		 */
		if( !found ) {
			fAtEOD = true;
			throw new JBasicException(Status.LINENUM, lineNumber);
		}
	}
	
	/**
	 * Add a new entry in the DATA element pointer, given a pointer to a
	 * ByteCode object. The runtime map is created if it does not already exist.
	 * 
	 * @param lineNumber the line number this DATA element was read from, or
	 * zero if from an un-numbered program.
	 * @param bc the ByteCode stream that contains the definition of the
	 * data element.
	 */
	public void addDataElement(final int lineNumber, final DataByteCode bc) {
		if (dataElements == null)
			dataElements = new ArrayList<DataByteCode>();
		if( lineNumber > 0 )
			bc.insert(0, new Instruction(ByteCode._STMT, lineNumber, "<data element>"));
		dataElements.add(bc);
	}

	/**
	 * Check to see if another getNextDataElement() will return another data
	 * element, or if we will have read them all (and the next READ will start
	 * at the first DATA element again).
	 * 
	 * @return True if there is at least one more data element that can be READ
	 *         before restarting at the beginning of the data.
	 */
	public boolean endOfData() {
		return fAtEOD;
	}

	/**
	 * Reads the next DATA element. DATA elements are defined by DATA
	 * statements, and are lists of constant expressions that are processed at
	 * compile-time. These constants can be accessed by a READ statement which
	 * reads the "next" item into an lvalue. If there are no elements, then a
	 * NULL pointer is returned which is an error. If there are no more unread
	 * elements, we start again at the beginning.
	 * 
	 * @param s
	 *            The symbol table used to resolve non-constant values in DATA
	 *            statements. These are not recommended, but have partial
	 *            support currently.
	 * @return Returns the next Value in the DATA element list that was
	 *         constructed when the program was initialized for execution.
	 */
	public Value getNextDataElement(final SymbolTable s) {
		if (dataElements == null) {
			if( !fHasData) {
				fAtEOD = true;
				return null;
			}
			Linker.buildData(this);
			if( dataElements == null ) {
				fAtEOD = true;
				fHasData = false;
				return null;
			}
			rewindDataElements();
		}
		
		final int ix = dataElements.size();
		if (ix < 1) {
			fAtEOD = true;
			return null;
		}

		final DataByteCode bc = dataElements.get(dataElementPosition);
		Value result = null;
		if( bc.cachedResult != null )
			result = bc.cachedResult;
		else {
			bc.run(s, 0);
		
			if (bc.status.success()) {
				result = bc.getResult();
				bc.cachedResult = result;
			}
		}
		
		dataElementPosition++;
		if (dataElementPosition >= ix) {
			dataElementPosition = 0;
			fAtEOD = true;
		} else
			fAtEOD = false;
		return result;
	}

	/**
	 * Flag indicating if the program is running at the moment. For example,
	 * this is used in error message printing to see if the error in the
	 * statement was caused during a running program so the program name can be
	 * put in the message text.
	 */
	private boolean fProgramActive;

	/**
	 * Flag indicating if the current program is running. Note that it may be
	 * running but not be the current program, since it could have been a
	 * program that called the current program.
	 * 
	 * @return True if the current program has an active execution scope.
	 */
	public boolean isActive() {
		return (executable != null && fProgramActive);
	}


	/**
	 * This is the list of pending GOSUB/RETURN operations in the current
	 * program's context.
	 */

	public ArrayList<ScopeControlBlock> gosubStack;

	/**
	 * This is the byteCode for the program, once it has been linked.
	 */

	private ByteCode executable;

	/**
	 * Flag indicating if the file is protected, and cannot be listed or
	 * otherwise displayed as open source.
	 */
	private boolean fProtected;

	/**
	 * Flag indicating if this program is a stub used to execute immediate
	 * statements versus a persistent runnable program.
	 */
	public boolean fIsStub;

	/**
	 * This object handles nested loop control constructs
	 */
	public LoopManager loopManager;

	/**
	 * Constructor that creates a new program, given a name and an existing
	 * symbol table.
	 * 
	 * @param jb
	 *            The JBasic object containing this session.
	 * @param theName
	 *            The name of the program being created
	 */
	public Program(final JBasic jb, final String theName) {
		name = theName + "";
		next = 0;
		session = jb;
		runCount = 0;
		fRegistered = false;
		statements = new ArrayList<Statement>();
		loopManager = null;
		gosubStack = null;
		fSystemObject = session().isLoadingSystemObjects();
		fModified = false;
	}


	/**
	 * Format the program object as a printable string. This is mostly used by
	 * the Eclipse debugger to display program objects.
	 */
	public String toString() {
		StringBuffer result = new StringBuffer("[Program \"");
		result.append(name);
		result.append("\", ");
		if( fProtected)
			result.append("protected");
		else {
			result.append(statements.size());
			result.append(" statements");
		}
		if (executable != null)
			result.append(", linked");
		if (fModified)
			result.append(", modified");
		if (fProgramActive)
			result.append(", active");
		if (fSystemObject)
			result.append( ", system object");
		result.append("]");
		return result.toString();
	}

	/**
	 * Return the given statement from the current program, identified by its
	 * position in the program (its statementID). We also coerce the statementID
	 * to match at this point, since it is possible the program statement was
	 * stored away without knowing where it was.
	 * 
	 * @param i
	 *            The statementID (zero-based) of the statement to fetch
	 * @return The Statement object handle, or null if the statementID was
	 *         invalid.
	 */
	public Statement getStatement(final int i) {
		
		if (statements == null)
			return null;
		if ((i < 0) | (i >= statements.size()))
			return null;
		
		final Statement targetStatement = statements.get(i);
		targetStatement.statementID = i;
		return targetStatement;
	}

	/**
	 * Clear the current program object from the stored programs area. This
	 * removes it from the stored programs TreeMap, and also from the
	 * SYS$PROGRAMS string arrayValue.
	 * 
	 * @throws JBasicException  if the program is not actively registered or
	 * is protected from deletion.
	 */
	public void clear() throws JBasicException {

		final String pname = name;

		/*
		 * Since we're removing a program, we should invalidate the function
		 * name cache so we notice that this is no longer available as a
		 * program. Otherwise, deleting a user function that has been called
		 * at least once won't actually delete the function.
		 */
		
		Functions.flushCache(null);
		
		/*
		 * Attempt to remove it (which will also return the object that was
		 * removed). If that failed, then we have a bogus program name.
		 */
		final Program oldProgram = session().programs.remove(pname);
		if (oldProgram == null)
			throw new JBasicException(Status.PROGRAM, pname);

		/*
		 * If the program we just removed is also the current program, then we
		 * need to zap the current program. This means that if you CLEAR PROGRAM
		 * on the program you are editing, then <poof> it is gone.
		 */

		if (oldProgram == session().programs.getCurrent()) {
			session().programs.setCurrent(null);
			session().setCurrentProgramName("");
		}

		/*
		 * Must also remove this from the list of known program elements (used
		 * by the TEST command, among other things). We do this by creating a
		 * new arrayValue, since we have to re-index the keys.
		 */
		final Value parray = session().globals().reference(
				"SYS$PROGRAMS");

		/*
		 * Delete all the elements that match the one we're deleting.
		 */
		
		Value eName = new Value(pname);
		for (int i = 1; i <= parray.size(); i++) {
			Value item = parray.getElement(i);
			if (item == null)
				continue;
			if (item.match(eName)) {
				parray.removeArrayElement(i);
			}
		}
	}


	/**
	 * Rename this program. This changes the name of the program in the program
	 * object itself, the stored program TreeMap, and the SYS$PROGRAMS
	 * arrayValue.
	 * 
	 * @param newName
	 *            A String describing the new name of the program.
	 * @throws JBasicException 
	 */

	void rename(final String newName) throws JBasicException {

		if (newName == null)
			throw new JBasicException(Status.PROGRAM);

		final String oldName = name;
		
		/*
		 * Since we're changing names of stuff, let's flush the function name cache
		 * since we may be creating or deleting a user function with this operation.
		 */
		Functions.flushCache(null);
		
		/*
		 * Step one. Try to find us in the stored program arrayValue.
		 */

		if (name != null) {
			final Program p = session().programs.find(name);
			if (p != null)
				session().programs.remove(name);
		}

		session().programs.add(newName, this);

		/*
		 * Step two. Rename ourselves in the SYS$PROGRAMS arrayValue
		 */

		final Value array = session().globals().reference(
				"SYS$PROGRAMS");

		final int len = array.size();
		for (int ix = 1; ix <= len; ix++) {
			Value member = array.getElement(ix);
			member.coerce(Value.STRING);
			if (member.getString().equalsIgnoreCase(oldName)) {
				member = new Value(newName);
				array.setElementOverride(member, ix);
			}
		}

		/*
		 * Step three. Rename ourselves.
		 */

		name = newName;
		fSystemObject = false;
		fModified = true;

	}

	/**
	 * Insert a statement into the current program, with a given line number. If
	 * the statement is empty but there is a line number, then it indicates that
	 * the line is to be deleted. The line is inserted into the program's stored
	 * program TreeMap in line-number order.
	 * 
	 * @param lineNumber
	 *            A positive integer value indicating the user-supplied line
	 *            number at which to store the program. This is different than a
	 *            statementID which is the relative position in the program;
	 *            line numbers are arbitrary but ascending integer values.
	 * @param newStatement
	 *            A previously compiled statement. The statement must have been
	 *            processed by compile() in order to process the line number
	 *            correctly.
	 * @throws JBasicException if the line number does not exist or the
	 * current program is executing.
	 */
	public void insertStatement(final int lineNumber, final Statement newStatement) throws JBasicException {

		/*
		 * If we are running at this moment, can't do this...
		 */
		if( executable != null && executable.fRunning & executable.debugger != null )
			throw new JBasicException(Status.INVDBGOP);

		/*
		 * Set the object status. Normally this is false, marking all programs
		 * as NOT being system objects. It is set to true during the time that
		 * the Library load is occurring, when we would want to mark those
		 * programs as system programs.
		 */
		fSystemObject = session().isLoadingSystemObjects();
		fModified = true;

		if (executable != null)
			Linker.unlink(this);

		/*
		 * Determine if this is a really empty statement (that would be used to
		 * delete a line) or not. This has to not only check the statement text,
		 * but also account for a statement that has just a label on it.
		 */

		boolean fIsEmpty = newStatement.statementText.trim().length() == 0;
		if (newStatement.statementLabel != null)
			fIsEmpty = false;

		/*
		 * If there is bytecode and it starts with _STMT, then we can set the
		 * line number now.
		 */

		if (newStatement.byteCode != null) {
			final Instruction i = newStatement.byteCode.getInstruction(0);
			if (i.opCode == ByteCode._STMT) {
				i.integerOperand = lineNumber;
				i.integerValid = true;
			}
		}
		/*
		 * Now we must search the ArrayList of statements to see where this
		 * statement goes.
		 */
		final int len = statements.size();
		for (int i = 0; i < len; i++) {

			final Statement targetStatement = getStatement(i);

			/*
			 * If the line number matches something exactly, then we either
			 * replace the line or delete it outright.
			 */
			if (targetStatement.lineNumber == lineNumber) {

				/*
				 * If it's not a program declaration it can't go first.
				 */
				if (!newStatement.fDeclaration & (i == 0))
					throw new JBasicException(Status.PGMNOTFIRST, name);

				if (fIsEmpty)
					statements.remove(i);
				else {
					if (newStatement.fDeclaration) {
						if (i > 0)
							throw new JBasicException(Status.PGMNOTFIRST, name);
						rename(newStatement.declarationName);
					}
					newStatement.statementID = i;
					statements.set(i, newStatement);
				}
				return;
			}

			/*
			 * If we've found the place where we'd insert the statement, do that
			 * now.
			 */
			if (targetStatement.lineNumber > lineNumber) {

				/*
				 * If the statement is empty, don't bother to insert it.
				 */
				if (fIsEmpty)
					break;

				/*
				 * If it's not a program declaration it can't go first.
				 */
				if (!newStatement.fDeclaration && (i == 0))
					throw new JBasicException(Status.PGMNOTFIRST, name);

				newStatement.statementID = i;
				if (newStatement.fDeclaration) {
					if (i > 0)
						throw new JBasicException(Status.PGMNOTFIRST);
					if (newStatement.declarationName != null)
						rename(newStatement.declarationName);
				}
				statements.add(i, newStatement);

				/*
				 * Because we just inserted something, the statement ID values
				 * are wrong - fix them up from here forward.
				 */
				for (int ix = i; ix < len + 1; ix++) {
					final Statement tempStmt = getStatement(ix);
					tempStmt.statementID = ix;
					statements.set(ix, tempStmt);
				}

				return;
			}
		}
		newStatement.statementID = statements.size();
		if (newStatement.fDeclaration) {
			if (newStatement.statementID > 0)
				throw new JBasicException(Status.PGMNOTFIRST);
			if (newStatement.declarationName != null)
				rename(newStatement.declarationName);
		}
		statements.add(newStatement);

		return;

	}

	/**
	 * Add a statement text to a program. Creates a new statement object, and
	 * adds it to the current program. The statement object is loaded with the
	 * command line text.
	 * 
	 * @param statementText
	 *            The line of text to add to the program.
	 * 
	 * @return Status block describing success of the operation.
	 */

	public Status add(final String statementText) {
		final Status addStatus = new Status(Status.SUCCESS);
		
		/*
		 * Is the current program running, perhaps under a debugger?
		 * If so, we can't do this now.
		 */
		
		if(isActive())
			return new Status(Status.INVDBGOP);
		
		/*
		 * See if it's an assembler statement from a protected program; if so
		 * we'll add it to the executable instead of processing the lines.
		 */

		if (statementText.trim().startsWith(":")) {

			if (executable == null) {
				executable = new ByteCode(session());
				executable.statement = getStatement(0);
				if (executable.statement == null)
					executable.statement = new Statement(session());
				executable.statement.program = this;
				executable.statement.byteCode = executable;
				executable.fLinked = true;
				fProtected = true;
			}

			final String asmText = statementText.trim().substring(1);
			return executable.assemble(asmText);

		}
		final Statement newStatement = new Statement(session());
		fModified = true;
		newStatement.program = this;
		newStatement.store(statementText, this);

		/*
		 * If the statement has a line number, then the store operation above
		 * already stored the statement away in the program. If so, then we're
		 * done.
		 */

		if (newStatement.status.equals(Status.STMTADDED))
			return addStatus;

		final boolean compress = session().getBoolean("SYS$COMPRESS");

		/*
		 * If we are compressing blank/comment statements out, then toss this
		 * one away if it is really empty and has no label associated with it.
		 */

		if (compress & newStatement.fEmptyStatement & (newStatement.statementLabel == null))
			return new Status(Status.SUCCESS);

		/*
		 * If this has been previously linked, then toss that all away since
		 * it's no longer valid.
		 */
		if (executable != null)
			Linker.unlink(this);

		/*
		 * Set the statement ID, which we now can set based on the position in
		 * the file. Also, if we've generated bytecode and the first instruction
		 * is a _STMT, then set it's identifier field if it's zero.
		 */
		newStatement.statementID = statements.size();
		if (newStatement.byteCode != null) {
			final Instruction inst = newStatement.byteCode.getInstruction(0);
			if ((inst.opCode == ByteCode._STMT) & (inst.integerOperand == 0)) {
				inst.integerOperand = -newStatement.statementID;
				inst.integerValid = true;
			}
		}
		statements.add(newStatement);

		next = 0; /* Reset next statement on add operation */
		return addStatus;
	}

	/**
	 * Run a stored program. The program begins at the first statement and
	 * executes until an error occurs or there are no more statements.
	 * 
	 * @param symbols
	 *            The symbol table to be used for runtime symbol resolution
	 * @param start
	 *            The starting statement number where execution is to begin
	 *            (default is zero)
	 * @param debugger
	 *            The debugger object to use with this invocation, or null if no
	 *            debugger is active.
	 * @return Status block reflecting execution status
	 */
	
	public Status run(final SymbolTable symbols, final int start,
			final JBasicDebugger debugger) {
		
		if( this.session().onStatementStack.stackSize() >= JBasic.CALL_DEPTH_LIMIT-2)
			return new Status(Status.CALLDEPTH);
		
		ProgramState savedState = new ProgramState(this);
		Status status = runExecutable(symbols, start, debugger);
		savedState.restoreState(this);
		return status;
	}
	
	/*
	 * This is where the real work is done to execute a program, and is
	 * called from the run() method whose primary function is to support
	 * the save and restore of the program state as needed.
	 */
	private Status runExecutable(final SymbolTable symbols, final int start,
			final JBasicDebugger debugger) {

		if (!session().isRunning())
			return new Status(Status.QUIT);

		Status sts = new Status(Status.SUCCESS);
		boolean lastActive;
		final Program oldProgram = session().programs.getCurrent();

		session().programs.setCurrent(this);
		next = start;
		runCount++;
		lastActive = fProgramActive;
		fProgramActive = true;
		gosubStack = null;
		loopManager = new LoopManager();

		/*
		 * Create a new scope for on-error processing. For fDebugExpressions
		 * purposes, use the current program name to mark the stack location.
		 */

		final int scopeMark = session().onStatementStack.push(name);

		/*
		 * We must ensure that the program is linked before it can be run. The
		 * LINK phase does a buildData() for us, so we only do that if we didn't
		 * already have to link.
		 */

		if (executable == null)
			sts = relink(false);

		else
			/*
			 * Make sure that the DATA statements are collected, so that READ
			 * can scan through them properly. We do this at the "last minute"
			 * before running since the order of the statements is undefined
			 * until runtime.
			 */
			sts = Linker.buildData(this);

		/*
		 * Run the program! Note we only do this if the link and/or DATA scans
		 * went okay.
		 */

		if (sts.success()) {
			getExecutable().setDebugger(debugger);
			sts = runExecutable(symbols, start);
		}

		/*
		 * Discard any ON statements that still might exist on the stack for
		 * this on-unit.
		 */
		session().onStatementStack.pop(scopeMark);

		/*
		 * If there was an unresolved error, now is the time to print it out.
		 */
		if (sts.failed())
			sts.print(session());
		fProgramActive = lastActive;
		session().programs.setCurrent(oldProgram);
		loopManager = null;
		return sts;
	}

	private Status relink(boolean b) {
		Status sts = Linker.unlink(this);
		if( sts.failed())
			return sts;
		
		return link(b);
	}

	/**
	 * Return the statement index in the program arrayValue for the given label.
	 * Returns a -1 if the label is not found.
	 * 
	 * @param label
	 *            The string label name to search for in the program.
	 * @return the index into the program vector of the statement with the given
	 *         label.
	 * 
	 */
	public int findLabel(final String label) {
		final int length = statements.size();
		int ix;
		Statement stmt;

		final String normalizedLabel = label.trim().toUpperCase();

		for (ix = 0; ix < length; ix++) {

			stmt = getStatement(ix);

			/* Shouldn't ever happen, but just in case... */
			if (stmt == null)
				continue;

			/* No statement label on this one at all */
			if (stmt.statementLabel == null)
				continue;

			/* If it matches, then we're done! */
			if (stmt.statementLabel.equals(normalizedLabel))
				return ix;

		}

		return -1;

	}

	/**
	 * Add source code to the current program. The source is always appended to
	 * the end of the current program.
	 * 
	 * @param fileName
	 *            The name of the source file to append to the current program
	 * @return A Status block indicating if the operation was successful or not.
	 */
	Status addSource(final String fileName) {

		if( executable != null & executable.fRunning & executable.debugger != null )
			return new Status(Status.INVDBGOP);

		sourceFileName = fileName;
		try {
			final BufferedReader infile = new BufferedReader(new FileReader(
					sourceFileName));
			String line;

			while ((line = infile.readLine()) != null)
				add(line);
			infile.close();
		} catch (final IOException e) {
			return new Status(Status.INFILE, sourceFileName);
		}
		return new Status(Status.SUCCESS);

	}

	/**
	 * Method to register a program in the master program table. This is
	 * currently owned by the HashMap() owned by the JBasic shell. The program
	 * must already have been given a name to self-register in this way.
	 * <p>
	 * Note that the program only registers itself if it has not done so
	 * already, to prevent multiple name registration.
	 * <p>
	 * When a new program is registered, it can mean that a user program
	 * is expected to supersede a built-in function.  As such, we make
	 * sure that the function name cache does not have a reference to this
	 * program. 
	 */

	public void register() {

		name = name.toUpperCase();

		Functions.flushCache(name);
		if (!fRegistered) {

			/*
			 * If we haven't registered, first see if there is already a program
			 * by this name. If there is, then delete it so we don't risk having
			 * a duplicate reference.
			 */
			final Program oldVersion = session().programs.find(name);
			if (oldVersion != null)
				session().programs.remove(name);
			else {
				/*
				 * Never had this name before, so we need to add this into the
				 * list of program names we have. This is stored in a user-
				 * accessible arrayValue SYS$PROGRAMS. The setElementOverride() 
				 * call is needed because it's a read-only ARRAY value.
				 */
				Value programArray = session().globals().localReference("SYS$PROGRAMS");
				if( programArray == null ) {
					programArray = new Value(Value.ARRAY, "SYS$PROGRAMS");
					try {
						session().globals().insertReadOnly("SYS$PROGRAMS", programArray);
					} catch (JBasicException e) {
						session.stdout.println("FAULT: Failure to create SYS$PROGRAMS array");
						e.printStackTrace();
					}
				}
				programArray.setElementOverride(new Value(name), programArray.size() + 1);
			}

			session().programs.add(name, this);
			fRegistered = true;
		}
		session().programs.setCurrent(this);
	}

	/**
	 * Link the current program into a single byteStream. Must have successfully
	 * already done a compile() on the program or compiled each statement as it
	 * was entered.
	 * @param strip Flag indicating if STMTs are to be stripped from protected
	 * programs.
	 * 
	 * @return Status indicating if the link was successful. If so, a run() on
	 *         this program in the future will cause it to execute the composite
	 *         bytecode for maximum efficiency.
	 */
	public Status link(boolean strip) {
		
		if( executable != null && executable.fRunning & executable.debugger != null )
			return new Status(Status.INVDBGOP);

		Functions.flushCache(null);
		readyLocalFunctions();
		return Linker.link(this, strip);
	}

	/**
	 * Return the number of statements stored in this program object.
	 * 
	 * @return Integer indicating how many statements have already been stored
	 *         in the problem's statement vector.
	 */
	public int statementCount() {
		if (statements == null)
			return 0;
		return statements.size();
	}

	/**
	 * Store a statement at a given location in the statement vector.
	 * 
	 * @param i
	 *            The ordinal position (0-based) in the program to store the
	 *            statement.
	 * @param s
	 *            The statement object to store in the current program.
	 */
	public void setStatement(final int i, final Statement s) {
		statements.set(i, s);
	}

	/**
	 * Remove a statement from the vector, and move following statements forward
	 * by a statement position.
	 * 
	 * @param i
	 *            The zero-based index in the statement array to remove a
	 *            statement from.
	 * @return The statement that was removed is returned as the result.
	 * @throws JBasicException if attempted while the program is running
	 */
	public Statement removeStatement(final int i) throws JBasicException {
		
		if( executable != null && executable.fRunning )
			throw new JBasicException(Status.INVDBGOP);

		if( executable != null )
			Linker.unlink(this);
		
		return statements.remove(i);
	}

	/**
	 * Does the current program have a linked executable that can be run, versus
	 * calling the per-statement execution method?
	 * 
	 * @return True if runExecutable() is allowed for this object.
	 */
	public boolean hasExecutable() {
		if (executable == null)
			return false;
		if (!executable.fLinked)
			return false;
		return true;
	}

	/**
	 * Invoke the linked executable code for the program, if present.
	 * 
	 * @param symbols
	 *            The symbol table to use for this execution context.
	 * @param start
	 * 			  The starting line number to begin execution at, or zero.
	 * @return A status indicating if execution was successful. An error is
	 *         returned if there is a runtime error, or if there is no
	 *         executable to run.
	 */
	public Status runExecutable(final SymbolTable symbols, int start) {
		if (!hasExecutable())
			return new Status(Status.FAULT, "no executable bytecode to run");
		rewindDataElements();
		return executable.run(symbols, start);
	}

	/**
	 * Method to obliterate the executable code (if any) in the current program.
	 * This also eliminates any local statement definitions anchored off of the
	 * current program, which must be re-compiled.
	 */
	public void clearExecutable() {
		
		boolean hadCode = (executable != null);
		
		if (hadCode)
			executable.labelMap = null;
		executable = null;
		
	
		if (statementCount() > 0) {
			final Statement st = getStatement(0);
			if (st != null) {
				if (st.byteCode != null) {
					st.byteCode.labelMap = null;
					st.byteCode = null;
				}
			}
		}
		
		/*
		 * If there was code already (that is, this was previously linked)
		 * then go through each statement and blow them away as well.  We
		 * only do this if the program was previously linked, because the
		 * link and optimize phases can modify the individual Instruction
		 * objects in the bytecode for each statement (that was copied to 
		 * the linked code).  So for safety's sake, we're going to force
		 * a one-time compile of each statement the next time compile()
		 * is called.
		 */
		
		for( int i = 1; i < statementCount(); i++ ) {
			Statement st = getStatement(i);
			st.byteCode = null;
		}
	}
	
	/**
	 * Initialize a new executable area for linked code for this program object.
	 */
	public void initExecutable() {
		executable = new ByteCode(session(), null);
		executable.statement = getStatement(0);
	}

	/**
	 * Return the number of instructions stored in the byteCode.
	 * 
	 * @return The number of instructions in the executable byteCode stream, or
	 *         zero if there is no byte code stream linked with this program.
	 */
	public int executableSize() {
		if (executable == null)
			return 0;
		return executable.size();
	}

	/**
	 * Get the executable ByteCode for this program.
	 * 
	 * @return A ByteCode object, or null if the program is not linked.
	 */
	public ByteCode getExecutable() {
		return executable;
	}

	/**
	 * Is the current Program object protected?
	 * 
	 * @return True if the object is protect, and unavailable to be viewed or
	 *         modified by the user.
	 */
	public boolean isProtected() {

		return fProtected;
	}

	/**
	 * Set the protected characteristic for the object. This is an irrevokable
	 * action, because in addition to setting the flag, it deletes all program
	 * text and the statement text from _STMT operators, leaving only the linked
	 * byte code associated with the program.
	 */
	public void protect() {
		fProtected = true;
		
		/*
		 * Re-link the code with the strip flag set, which removes instructions
		 * that are not appropriate/useful in a protected program, such as
		 * _STMT or _DEBUG
		 */
		clearExecutable();
		Status status = link(true);
		if( status.failed()) {
			Linker.unlink(this);
			return;
		}
		/*
		 * Step over the individual statements and remove them since the code
		 * is now wholly contained in the program executable byte code stream.
		 */
		int ixlen = executableSize();
		int ix;

		ixlen = statementCount();
		for (ix = 1; ix < ixlen; ix++)
			statements.remove(1);
	}

	/**
	 * Set the system object attribute of the program object. System objects are
	 * not saved in user workspaces, etc. and user objects are saved in
	 * workspaces.
	 * 
	 * @param b true if this is marked as a system object.
	 */
	public void setSystemObject(final boolean b) {
		fSystemObject = b;
		if (b)
			fModified = false;

	}

	/**
	 * Renumber a program object's statement lines.  Modifies the line number
	 * references in IF..THEN and GOTO statements as needed.  This operation
	 * will fail if there are line number references in legacy statements that
	 * do not refer to existing statements; the renumber operation is not 
	 * performed in that case to avoid damaging the user source code.
	 * 
	 * @param startLineNumber
	 *            The line number to start the program
	 * @param increment
	 *            The increment for the line numbers
	 * @return status indicating if the operation as a success
	 */
	public Status renumber(final int startLineNumber, final int increment) {

		if( executable != null && executable.fRunning & executable.debugger != null )
			return new Status(Status.INVDBGOP);

		class LineNumberDescription {
			int statementID;
			int referencedLineNumber;
			int tokenPosition;
		};
		
		/*
		 * This is the list where we keep the map of line numbers and token
		 * positions of line numbers to handle re-editing the text where
		 * line number references are made.
		 */
		ArrayList<LineNumberDescription> lineNumberTable = new ArrayList<LineNumberDescription>();

		/*
		 * Go ahead and discard any linked code references and regenerate
		 * each statement's byte code.
		 */
		Linker.unlink(this);

		int nextLineNumber = startLineNumber;
		final int len = statementCount();
		int i, ix;
		
		/*
		 * Scan over the program and make a list of what statements
		 * have line numbers, and what the line numbers are.
		 */

		for (i = 0; i < len; i++) {
			
			/*
			 * Get the statement, tokenize it, and compile it to have a
			 * current and up-to-date line number array for the statement.
			 */
			final Statement s = getStatement(i);
			Tokenizer t = new Tokenizer(s.statementText);
			s.compile(t);
			
			/*
			 * See if there are line numbers in the statement text.  If so
			 * we have work to do to capture information about this statement.
			 */
			int tokenCount = s.lineNumberTokenCount();
			
			if( tokenCount > 0 ) {
				
				/*
				 * Scan over the list of line number positions in this one
				 * statement, and capture the information about them.
				 */
				for( int px = 1; px <= tokenCount; px++ ) {
					
					int posx = s.getLineNumberPosition(px);
					/*
					 * The buffer must be reloaded because some statement
					 * compile operations are destructive to the token buffer.
					 */
					t.loadBuffer(s.statementText);

					/*
					 * Store away the referenced line number, the statement 
					 * position, and the position of the line number token.
					 */
					LineNumberDescription ld = new LineNumberDescription();
					ld.referencedLineNumber = Integer.parseInt(t.getToken(posx));
					ld.statementID = i;
					ld.tokenPosition = posx;
					lineNumberTable.add(ld);
				}
			}
		}
		
		/*
		 * Because dangling line numbers will get hopelessly lost, make
		 * sure that every line number reference we have in the map does
		 * reference an existing line number.
		 */
		
		int errCount = 0;
		Statement programStatement = null;
		for( i = 0; i < lineNumberTable.size(); i++ ) {
			LineNumberDescription ld = lineNumberTable.get(i);
			boolean found = false;
			for( ix = 0; ix < this.statementCount(); ix++) {
				programStatement = getStatement(ix);
				if( programStatement.lineNumber == ld.referencedLineNumber) {
					found = true;
					break;
				}
			}
			if( !found ) {
				programStatement = getStatement(ld.statementID);
				this.session().stdout.println(programStatement.toString());
				errCount++;
			}
		}
		if( errCount > 0 )
			return new Status(Status.NORENUM);
		
		/*
		 * Now do the brute-force renumbering.
		 */
		for (i = 0; i < len; i++) {
			programStatement = getStatement(i);
			
			/*
			 * See if this line number was referenced in another statement.
			 */
			for( ix = 0; ix < lineNumberTable.size(); ix++ ) {
				LineNumberDescription ld = lineNumberTable.get(ix);
				if( programStatement.lineNumber == ld.referencedLineNumber) {
					/*
					 * Have to patch up the line number in the 
					 * referring statement.
					 */
					
					Statement refStmt = getStatement(ld.statementID);
					int pos = ld.tokenPosition;
					
					/*
					 * Create a tokenizer to do our work.  Prime it
					 * with the text from the statement.
					 */
					Tokenizer refTokens = new Tokenizer(refStmt.statementText);
					
					/*
					 * Use the tokenizer to force a token at a specific
					 * position to be the string value of the new line
					 * number.
					 */
					refTokens.setToken(pos, Tokenizer.INTEGER, 
							Integer.toString(nextLineNumber));
					
					/*
					 * Use the newly edited text buffer to recompile
					 * the statement with the new text of the line number.
					 */
					String newText = refTokens.reTokenize();
					refStmt.store(Integer.toString(refStmt.lineNumber)
							+ newText);
				}
			}
			
			/*
			 * Now we can reset the line number, and recompile this statement
			 */
			programStatement.lineNumber = nextLineNumber;
			programStatement.compile(new Tokenizer(programStatement.statementText));

			nextLineNumber = nextLineNumber + increment;
		}

		/*
		 * Scan over the end of the program, and remove any spurious
		 * empty statements at the end, which are often side-effects of
		 * loading source files from workspaces where blanks are put
		 * between programs.
		 */
		
		if( this.session().getBoolean("SYS$TRIMSOURCE")) 
			try {
				while( getStatement(statementCount()-1).fEmptyStatement)
					this.removeStatement(statementCount()-1);
			} catch (JBasicException e ) {
				return e.getStatus();
			}
		
		/*
		 * Now that we've renumbered, go ahead and regenerate a link.
		 */
		return new Status();  // link(false);
	}

	/**
	 * Parse the DEFINE(keyword [, keyword...]) syntax that can follow a
	 * PROGRAM, FUNCTION, or VERB.
	 * 
	 * @param bc
	 *            The bytecode stream being generated. Some pragma
	 *            codes will add additional control codes to the code stream.
	 * @param tokens
	 *            The token buffer of the line
	 * @return A status indicating if there was a parsing error.
	 */
	public Status handlePragmas(final ByteCode bc, final Tokenizer tokens) {
		fStaticTyping = session().getBoolean("SYS$STATIC_TYPES");

		if (fSystemObject)
			fStaticTyping = false;

		while( !tokens.endOfStatement()) {
			if( tokens.assumeNextToken("STATIC")) {
				bc.add(ByteCode._SETSCOPE, -1);
				fStaticTyping = true;
				bc.add(ByteCode._TYPES, 1);
				bc.add(ByteCode._SETDYNVAR, 0 );
				continue;
			}

			if( tokens.assumeNextToken("DYNAMIC")) {
				bc.add(ByteCode._SETSCOPE, -1);
				fStaticTyping = true;
				bc.add(ByteCode._TYPES, 1);
				bc.add(ByteCode._SETDYNVAR, 0 );
				continue;
			}

			if( tokens.assumeNextToken("RETURNS")) {
				boolean hasParen = false;
				if( tokens.assumeNextSpecial("("))
					hasParen = true;
				if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
					return new Status(Status.EXPTYPE, tokens.nextToken());
				String typeName = tokens.nextToken();
				int typeCode = Value.nameToType(typeName);
				if( typeCode == Value.UNDEFINED)
					return new Status(Status.EXPTYPE, typeName);
				bc.add(ByteCode._TYPES, typeCode);
				if( hasParen && !tokens.assumeNextSpecial(")"))
					return new Status(Status.PAREN);
				continue;
			}

			if (tokens.assumeNextToken(new String[] {"DEFINE", "PRAGMA"})) {
				tokens.assumeNextSpecial("(");
				while (true) {
					if (tokens.assumeNextSpecial(")"))
						break;
					if (tokens.assumeNextSpecial(","))
						continue;
					final String key = tokens.nextToken();

					if( key.equals("STATIC")) {
						bc.add(ByteCode._SETSCOPE, -1);
						fStaticTyping = true;
						bc.add(ByteCode._TYPES, 1);
						bc.add(ByteCode._SETDYNVAR, 0 );
					}
					else if( key.equals("DYNAMIC")) {
						bc.add(ByteCode._SETSCOPE, 1);
						fStaticTyping = false;
						bc.add(ByteCode._TYPES, 0);
						bc.add(ByteCode._SETDYNVAR, 1 );
					}
					else if (key.equals("SYSTEM_OBJECT"))
						fSystemObject = true;
					else if (key.equals("DYNAMIC_TYPES")) {
						fStaticTyping = false;
						bc.add(ByteCode._TYPES, 0);
					} else if (key.equals("STATIC_TYPES")) {
						fStaticTyping = true;
						bc.add(ByteCode._TYPES, 1);
					} else if (key.equals("PRIVATE")) {
						bc.add(ByteCode._SETSCOPE, -1);
					} else if (key.equals("SCOPED")) {
						bc.add(ByteCode._SETSCOPE, 1);
					} else if (key.equals("DYNAMIC_ALLOCATION")) {
						bc.add(ByteCode._SETDYNVAR, 1);
					} else if (key.equals("STATIC_ALLOCATION")) {
						bc.add(ByteCode._SETDYNVAR, 0);
					} else
						return new Status(Status.INVSET, key);
				}
				continue;
			}
			return new Status(Status.KEYWORD, tokens.nextToken());
		}
		return new Status();

	}

	/**
	 * Make a source copy of the current program. The new object is source-only
	 * and must be recompiled to run. This is usually used to copy programs in
	 * the current session to a new thread being created.
	 * 
	 * @return a new Program object that is a copy of the current object.
	 */
	public Program copy() {

		final Program p = new Program(session(), name);

		p.name = name;
		p.statements = new ArrayList<Statement>();

		for (int stmtNumber = 0; stmtNumber < statements.size(); stmtNumber++) {
			final Statement sourceStmt = statements.get(stmtNumber);
			final Statement newStatement = new Statement();
			newStatement.fEmptyStatement = sourceStmt.fEmptyStatement;
			newStatement.lineNumber = sourceStmt.lineNumber;
			newStatement.program = p;
			newStatement.statementID = sourceStmt.statementID;
			newStatement.statementLabel = sourceStmt.statementLabel;
			newStatement.statementText = sourceStmt.statementText;
			p.statements.add(newStatement);
		}
		return p;
	}

	/**
	 * Given a line number, return the statement with that line number.
	 * @param ln The line number (100, 110, 120, etc.) to search for.  The
	 * statement must have exactly that line number to be returned.  Note that
	 * this is different than getStatement(pos) which returns an ordinal 
	 * statement position (0, 1, 2, 3, etc.)
	 * @return The statement with the matching line number, or null if no
	 * statement was found with that line number.
	 */
	public Statement findLineNumber(int ln) {
		
		final int count = statementCount();
		for( int stmtNumber = 0; stmtNumber < count; stmtNumber++ ) {
			
			final Statement theStatement = getStatement(stmtNumber);
			
			/*
			 * If we have a match, return it.
			 */
			if( theStatement.lineNumber == ln)
				return theStatement;
			
			/*
			 * Because statements are stored in statement-number
			 * order, if we've passed the line number, then there
			 * isn't one and we should report that fact.
			 */
			if( theStatement.lineNumber > ln )
				return null;
		}
		
		/*
		 * If we got all the way to the end without a match, then we
		 * report no such line.
		 */
		return null;
	}

	/**
	 * Indicates if there are any local functions associated with this program.
	 * @return Returns true if there are one or more local functions.
	 */
	public boolean hasLocalFunctions() {
		if( localFunctions == null)
			return false;
		return (localFunctions.size() > 0 );
	}
	
	/**
	 * Return an iterator that can be used to locate all the functions in
	 * the list of local functions.  If there are no local functions, this
	 * returns null.
	 * @return An iterator that will return the KEY value of each local
	 * function.  Use the findLocalFunction() to get the Bytecode that goes
	 * with each key.
	 */
	public Iterator localFunctionIterator() {
		if( localFunctions == null)
			return null;
		return localFunctions.keySet().iterator();
	}
	
	/**
	 * Clear any stored local functions.
	 */
	public void clearLocalFunctions() {
		localFunctions = null;
	}
	/**
	 * Add a local function ByteCode stream to the current program object.
	 * @param name The name of the function, which must be upper-case.
	 * @param bc The byte code stream that expresses the function code.
	 */
	public void addLocalFunction( String name, ByteCode bc ) {
		if( localFunctions == null )
			localFunctions = new HashMap<String,ByteCode>();
		localFunctions.put(name, bc);
	}
	
	/**
	 * See if there is a bytecode stream stored as a local function
	 * in the program object.
	 * @param name The name of the function to locate, which must be 
	 * in uppercase.
	 * @return the ByteCode object that expresses the function result,
	 * or a null pointer if there is no such local function.
	 */
	public ByteCode findLocalFunction( String name ) {
		if( localFunctions == null)
			return null;
		ByteCode bc = localFunctions.get(name);
		return bc;
	}

	/**
	 * Ready the local function definitions in the current program for
	 * use by the runtime.
	 * @return a count of the number of functions prepared.
	 */
	public int readyLocalFunctions() {

		int count = 0;
		if( executable == null )
			return 0;
		
		/*
		 * If we've already done this, don't do it again.  Modifying
		 * the program (such as by adding a new statement) will zero
		 * this out for us so we only need to reload when the list is
		 * already empty.
		 */
		if( hasLocalFunctions())
			return 0;
		
		/*
		 * Gather up the DEFFN objects in the program and attach them to
		 * the current program, if there is one.  We need to do this first
		 * so optimizations to calls TO these defined functions work correctly.
		 */

		clearLocalFunctions();
		int ix;
		for( ix = 0; ix < executable.size(); ix++ ) {
			Instruction i = executable.getInstruction(ix);
			if( i.opCode != ByteCode._DEFFN)
				continue;
			ByteCode localFunc = new ByteCode(executable.getEnvironment(), executable.statement);
			int ifx;
			for( ifx = 1; ifx <= i.integerOperand; ifx++ )
				localFunc.add(executable.getInstruction(ix+ifx));
			localFunc.fLinked = true;
			addLocalFunction(i.stringOperand, localFunc);
			count++;
			ix = ix + ifx;
		}
		
		/*
		 * Now search for references to these newly collection up definitions
		 * in the program and make sure they are CALLFL instead of CALLF.
		 */
		
		for( ix = 0; ix < executable.size(); ix++ ) {
			Instruction i = executable.getInstruction(ix);
			if( i.opCode == ByteCode._CALLFL)
				i.opCode = ByteCode._CALLF;
			
			if( i.opCode == ByteCode._CALLF && i.stringValid ) {
				if(this.findLocalFunction(i.stringOperand) != null )
					i.opCode = ByteCode._CALLFL;
			}
		}
		
		return count;
	}

	/**
	 * Given a byte code structure, force it to be the protected representation
	 * of this program.  This replaces all statements in the program with 
	 * a single statement that references this new bytecode.
	 * @param bc The bytecode to store as the protected code.
	 */
	public void setByteCode(ByteCode bc) {

		/*
		 * The store (and compile) will have created a copy of the
		 * statement with the right subclass, etc.  So we need to
		 * retrieve the only (zeroth) statement and force it to
		 * hold a reference to the bytecode object.
		 */
		Statement targetStatement = null;
		if( statementCount() > 0 )
			targetStatement = getStatement(0);
		else
			targetStatement = new Statement(this.session());

		targetStatement.store("1 // Protected code created by NEW USING() statement", this);
		targetStatement.byteCode = bc;

		/*
		 * If there were no statements before, make this the first statement.
		 */
		if( statementCount() == 0 )
			this.setStatement(0, targetStatement);
		
		/*
		 * Otherwise, if there were multiple statements, discard all but the first
		 */
		else {
			try {
				while( statementCount() > 1 )
					this.removeStatement(1);
			} catch( JBasicException e ) {
				/* Ignore */
			}
		}


		/*
		 * Also need to make the program-wide reference to the executable
		 * point to the same bytecode.  The program is implicitly also 
		 * protected, and the executable contains a reference back to the
		 * statement.
		 */
		executable = bc;
		fProtected = true;
		executable.statement = targetStatement;
		
		/*
		 * We've got to find out if there are _DATA items here so the fHasData flag can
		 * be set correctly. This will cause the DATA items to be assembled at runtime
		 * into a DATA element map for use by the READ and REWIND statements.  If the
		 * flag is false, no map is created and these statements would signal an error.
		 */
		for( int idx = 0; idx < bc.size(); idx++ )
			if( bc.getInstruction(idx).opCode == ByteCode._DATA){
				this.fHasData = true;
				break;
			}
		initDataElements();

	}

	/**
	 * Determine if the current program has active/pending FOR..NEXT or DO..LOOP
	 * constructs. 
	 * @return true if there are active loops, or false if there are no active
	 * loops (or no loops ever created).
	 */
	public boolean hasLoops() {
		if( loopManager == null )
			return false;
		return (loopManager.loopStackSize()>0);
	}

	/**
	 * Override the current active session for the program object.
	 * @param session the session to set
	 */
	public void setSession(JBasic session) {
		this.session = session;
	}

	/**
	 * @return the session
	 */
	public JBasic session() {
		return session;
	}

	/**
	 * Reformat the text of the current program using the current tokenizer
	 * formatting rules.
	 */
	public void reFormat() {
		Tokenizer t = new Tokenizer(null, JBasic.compoundStatementSeparator);
		for( int idx = 0; idx < this.statementCount(); idx++ ) {
			Statement s = this.getStatement(idx);
			if( ! s.fEmptyStatement) {
				t.loadBuffer(s.statementText);
				t.reTokenize();
				s.statementText = t.getBuffer() + t.getRemainder();
			}
		}
	}
	
	/**
	 * Find the executable line we are transferring to.  The line number
	 * target may not be an executable statement, and may not even have
	 * a _STMT marker if it was an empty line.  So we must advance the
	 * line number to find the next executable statement.
	 * @param lineNumber The target line number
	 * @return the line number of the actual next executable statement;
	 * this will either be the lineNumber parameter or the line number
	 * of a statement that follows it.
	 */
	public int findExecutableLine(int lineNumber) {
		/*
		 * Find the branch target line number.  We are going to check
		 * to see if this is an empty statement; i.e. a GOTO to a line
		 * with a comment.  If so, we're going to skip ahead in the 
		 * program to find the next non-empty statement and reset the
		 * line number target to that non-empty statement line number.
		 */
		Statement branchTarget = this.findLineNumber(lineNumber);
		if( branchTarget != null ) {
			if( branchTarget.lineNumber == lineNumber ) {

				/*
				 * We have a statement that matches the target.  Is
				 * it an empty (comment) statement?
				 */
				if( branchTarget.fEmptyStatement ) {
					
					/*
					 * Starting with this statement position (the
					 * statement ID), search for a non-empty statement.
					 */
					int nk = branchTarget.statementID;
					for( ; nk < this.statementCount(); nk++ ) {
						branchTarget = this.getStatement(nk);
						
						/*
						 * When a non-empty statement is found,
						 * use it's line number as the target for
						 * the GOTO.
						 */
						if( !branchTarget.fEmptyStatement) {
							lineNumber = branchTarget.lineNumber;
							break;
						}
					}
				}
			}
		}
		return lineNumber;
	}
}

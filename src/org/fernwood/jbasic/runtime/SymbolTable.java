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
package org.fernwood.jbasic.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.value.Value;

/**
 * This class implements a basic symbol table. Each symbol name is a string, and
 * names are expected to be unique. Each symbol is associated with a Value,
 * which means the symbols are typed based on the individual data items. You can
 * get or set a symbol by explicit type (with coercion as needed) or you can
 * store and retrieve whole DataElements as needed.
 * <p>
 * Symbol table entries are always coerced to uppercase before being stored in
 * the hash table, so symbol names are essentially case-insensitive.
 * <p>
 * A special class of symbols are "connectors". These are symbols that must have
 * been created by the JBasic.initializeGlobals() method. However, when a symbol
 * with a prefix of SYS$$ is read from the global symbol table, the routine
 * updateConnector() is called to allow the system to set the variable to
 * whatever runtime state or variable is required.
 * 
 * @author Tom Cole
 * @version version 1.2 Jan 21, 2006
 * 
 */

public class SymbolTable {

	/**
	 * A pointer to the JBasic instance that contains this table. This allows
	 * symbol table references to determine if they are the global table, or to
	 * access connector data.
	 */
	public JBasic session;

	/**
	 * The data structure containing the DataElements that have been stored in
	 * this symbol table. The key for the map is the symbol's name, and the
	 * Value is stored as the object in the table.
	 */
	public HashMap<String, Value> table;

	/**
	 * The name of the symbol table, for diagnostic purposes. This is displayed
	 * in the SHOW SYMBOLS command, for example. It is typically either "GLOBAL"
	 * or "LOCAL TO xxx" where "xxx" is the name of the program that it is
	 * associated with.
	 */
	public String name;

	/**
	 * If this is a local symbol table, then it has a parent table. These are
	 * connected together when one program calls another to create cascading
	 * scope. The root of this list is the global symbol table, which has no
	 * parent and is created the JBasic object.
	 */
	public SymbolTable parentTable;

	/**
	 * Flag used to indicate if this THE global symbol table, where SYS$$*
	 * variable (runtime connectors) are stored.
	 */
	public boolean fGlobalTable;

	/**
	 * Flag used to indicate if all symbols in this table are read-only by
	 * default. Used by the "Constants" symbol table, for example. Causes the
	 * insert() method to set the readonly attribute by default.
	 */
	boolean fDefaultToReadOnly;

	/**
	 * Flag used to indicate if strong typing is in effect for this table. When
	 * this is true, attempts to insert a value into an already-existing symbol
	 * result in the value being converted to match the symbol.
	 * 
	 * The default is false, where the symbol just takes on whatever new type is
	 * given to the value.
	 */
	boolean fStrongTyping;

	/**
	 * Flag indicating if this is the root table.  The root table is the only
	 * table shared among threads, so it is the only table that requires
	 * synchronization primitives.
	 */
	public boolean fRootTable;

	/**
	 * This simple table is used to store names of symbols that are 
	 * to be considered in the COMMON block for this symbol table, 
	 * should a _CHAIN operation be performed.  If there are no
	 * symbols marked COMMON in this table, this object is null.
	 */
	ArrayList<String> commonBlock;
	
	/**
	 * This is a copy of the parent table as initially set up in the
	 * symbol table when it's created.  This is used by the _SETSCOPE
	 * opcode to (re-)set the scope of a table.  You can force the
	 * scope to the GLOBAL or ROOT tables, or to an abstract offset
	 * of the original parent (1 is the original parent, 2 is the
	 * grandparent, etc.).  Because this might be done more than
	 * once, we can't just change the parentTable or we can't ever
	 * get back to the original parent to re-scan the list.  So we
	 * keep a copy of the parent table here.
	 */

	public SymbolTable originalParentTable;
	

	/**
	 * Scoped constructor. Key element is to ensure that a HashTable is created
	 * and linked to the SymbolTable object. Additionally, the named symbol
	 * table that is the parent is stored in this object, so that scoped lookups
	 * can occur.
	 * 
	 * @param theSession
	 *            The JBasic object that contains the current session.
	 * @param theTableName
	 *            The name to give the table. This is used in SHOW SYMBOLS
	 *            commands, and also used in the SYMBOL() function to identify
	 *            specific symbol tables to fetch variables from.
	 * @param theParentTable
	 *            The parent of the symbol table to create.  Pass a null if this
	 *            is a temporary table or is the top-level table in a chain
	 *            such as the "Constants" table.
	 */
	public SymbolTable(final JBasic theSession, final String theTableName,
			final SymbolTable theParentTable) {
		session = theSession;
		parentTable = theParentTable;
		originalParentTable = parentTable;
		name = theTableName;
		fDefaultToReadOnly = false;

		/* If this is THE global table, make a note for later */
		fGlobalTable = theTableName.equalsIgnoreCase(JBasic.GLOBAL_TABLE_NAME);
		fRootTable = theTableName.equalsIgnoreCase(JBasic.ROOT_TABLE_NAME);

		if (table == null)
			table = new HashMap<String,Value>();
	}

	/**
	 * Debugging interface to format the current symbol table object for
	 * display. This is largely used in debugging using Eclipse and is not
	 * generally part of the JBasic user experience.
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("Symbol Table \"");
		result.append(name);
		result.append("\", ");
		result.append(table.size());
		result.append(" symbols");
		if (fGlobalTable)
			result.append(", global");
		else
			if(fRootTable)
				result.append(", root");
		if (parentTable != null) {
			result.append(", parent=\"");
			result.append(parentTable.name);
			result.append("\"");
		}
		return result.toString();
	}

	/**
	 * Method to set the read-only flag for the table. This is the default value
	 * copied to each symbol's "readonly" attribute. This is set for the
	 * Constants table, for example. It can also be changed to true to do "bulk
	 * loads" of readonly values, such as is done when setting up the global
	 * symbols.
	 * 
	 * @param flag
	 *            Default readonly state for symbols inserted into the table.
	 */
	public void setReadOnly(final boolean flag) {
		fDefaultToReadOnly = flag;
	}

	/**
	 * Set the "strong typing" flag. When set, inserting a value into a table
	 * which has a pre-existing instance of the variable causes the value to be
	 * coerced to match the type of the existing variable. This allows a program
	 * with strong typing to use the DIM statement to create variable's whose
	 * type may not be obvious from the name, but that type will still be
	 * honored.
	 * 
	 * @param flag true if strong typing is to be enabled.
	 */
	public void setStrongTyping(final boolean flag) {
		fStrongTyping = flag;
	}

	/**
	 * Determine if an element is marked read-only. The symbol is searched for
	 * in the current table and all it's parents.
	 * 
	 * @param name
	 *            The name of the symbol to search for. The name must be
	 *            normalized to uppercase.
	 * @return True if the symbol exists and is already marked readonly. False
	 *         if the symbol does not already exist, or exists but is not
	 *         readonly.
	 */
	public boolean isReadOnly(final String name) {
		final Value v = findReference(name, false);
		if( v == null )
			return false;
		return v.fReadonly;
		
	}

	/**
	 * Return the number of symbols in this symbol table.
	 * 
	 * @return Integer count. If the value is zero it may mean that the table is
	 *         incompletely initialized.
	 */
	public int size() {
		if (table == null)
			return 0;
		return table.size();
	}

	/**
	 * Insert an integer value into the symbol table.
	 * 
	 * @param symbolName
	 *            The name to associate with the data element.
	 * @param value
	 *            The integer value to store in the data element.
	 * @throws JBasicException if the variable is READONLY or strong data
	 * typing would result in an illegal type conversion.
	 */
	public void insert(final String symbolName, final int value) throws JBasicException {

		if (isReadOnly(symbolName))
			throw new JBasicException(Status.READONLY, symbolName);

		final Value s = new Value(value);

		if (fStrongTyping) {
			final Value tmp = table.get(symbolName);
			if (tmp != null)
				s.coerce(tmp.getType());
		}

		s.setName(symbolName);
		s.fReadonly = fDefaultToReadOnly;
		s.fSymbol = true;
		
		if (symbolName.startsWith("$"))
			s.fReadonly = true;

		if (symbolName.startsWith("SYS$"))
			session.globals().table.put(symbolName, s);
		else
			table.put(symbolName, s);
		return;
	}

	/**
	 * Insert a string value into the symbol table.
	 * 
	 * @param symbolName
	 *            The name to associate with the data element.
	 * @param stringValue
	 *            The string value to store in the data element.
	 * @throws JBasicException if the variable is READONLY or strong data
	 * typing would result in an illegal type conversion.
	 */
	public void insert(final String symbolName,
			final String stringValue) throws JBasicException {

		insert(symbolName, new Value(stringValue));
	}

	/**
	 * Insert a double value into the symbol table.
	 * 
	 * @param symbolName
	 *            The name to associate with the data element.
	 * @param doubleValue
	 *            The double value to store in the data element.
	 * @throws JBasicException if the variable is READONLY or strong data
	 * typing would result in an illegal type conversion.
	 */
	public void insert(final String symbolName, final double doubleValue) throws JBasicException {
		insert(symbolName, new Value(doubleValue));
	}


	/**
	 * Insert a boolean value into the symbol table.
	 * 
	 * @param symbolName
	 *            The name to associate with the data element.
	 * @param booleanValue
	 *            The boolean value to store in the data element.
	 * @throws JBasicException if the variable is READONLY or strong data
	 * typing would result in an illegal type conversion.
	 */

	public void insert(final String symbolName, boolean booleanValue) throws JBasicException {
		insert(symbolName, new Value(booleanValue));
		
	}

	/**
	 * Insert an already existing data element in the symbol table. This is
	 * typically the result of an expression evaluation where the type of the
	 * object is not explicitly known to the caller.
	 * 
	 * @param symbolName the uppercase name of the symbol to set to the value
	 * @param data the Value object to store in the named symbol
	 * @throws JBasicException if the variable is READONLY or strong data
	 * typing would result in an illegal type conversion.
	 */
	public void insert(final String symbolName, Value data)
			throws JBasicException {

		if (isReadOnly(symbolName))
			throw new JBasicException(Status.READONLY, symbolName);

		Value s = data;
		if (fStrongTyping) {
			final Value preExistingValue = table.get(symbolName);
			if (preExistingValue != null) {
				int targetType = preExistingValue.getType();
				if( s.getType() != targetType)
					throw new JBasicException(Status.IMPCVT, Value.typeToName(targetType));
			}
		}

		if (this.fDefaultToReadOnly)
			s.fReadonly = true;

		markSymbol(s);
		
		s.setName(symbolName);
		if (symbolName.startsWith("$"))
			s.fReadonly = true;
		if (symbolName.startsWith("SYS$"))
			session.globals().table.put(symbolName, s);
		else
			table.put(symbolName, s);

		return;
	}

	/**
	 * Mark all the data elements participating in a Value object
	 * as being related to a stored symbol.  This will cause a copy
	 * of them to be made as needed later.
	 * @param s
	 */
	@SuppressWarnings("unchecked") 
	private static void markSymbol(Value s) {
		s.fSymbol = true;
		if( s.getType() == Value.ARRAY) {
			int len = s.size();
			for( int idx = 1; idx <= len; idx++ )
				markSymbol(s.getElement(idx));
		}
		else
			if( s.getType() == Value.RECORD) {
				Iterator<String> i = s.getIterator();
				if( i != null)
					while( i.hasNext()) {
						String name = i.next();
						markSymbol(s.getElement(name));
					}
			}
	}

	/**
	 * Insert an already existing data element in the symbol table, and mark
	 * it READONLY.
	 * 
	 * @param symbolName the symbol name (must be upper-case)
	 * @param data the value to add to the table
	 * @throws JBasicException if the symbol already exists and is read-onlt
	 */
	public void insertReadOnly(final String symbolName, Value data) throws JBasicException {

		if (isReadOnly(symbolName))
			throw new JBasicException(Status.READONLY, symbolName);

		insert( symbolName, data);
		data.fReadonly = true;
		return ;
	}

	/**
	 * Insert an already existing data element in the symbol table. This is
	 * typically the result of an expression evaluation where the type of the
	 * object is not explicitly known to the caller.<br>
	 * <p>
	 * Note that this is the ONLY insert operation that is synchronized; if
	 * you insert into ROOT you must use this interface.
	 * 
	 * @param symbolName the symbol name (must be upper case)
	 * @param data the value to insert
	 * @throws JBasicException  if the value already exists and is marked
	 * read-only.
	 */
	public synchronized void insertSynchronized(final String symbolName, Value data) throws JBasicException {
		insert( symbolName, data);
	}


	/**
	 * Insert a value into the local table, regardless of what other symbols or
	 * values may exist in parent tables. This is required to support local
	 * symbols created as part of a function call.
	 * 
	 * @param name the name of the symbol, which must be uppercase
	 * @param data the Data item to be written to the symbol table.
	 * @throws JBasicException  if the symbol already exists and is read-only.
	 */
	public void insertLocal(final String name, final Value data) throws JBasicException {

		Value s = data;
		
		if (fStrongTyping) {
			final Value tmp = table.get(name);
			if (tmp != null) {
				s = data.copy();
				s.coerce(tmp.getType());
			}
		}

		if( name.charAt(0) == '$')
			s.fReadonly = true;
		s.fSymbol = true;
		s.setName(name);
		table.put(name, s);

		return;
	}

	/**
	 * Given any symbol table, return the global symbol table in
	 * it's chain. This is done by calling up through the symbol tables until
	 * the global table is found. 
	 * 
	 * @return The global symbol table, or null if there is no table marked
	 * GLOBAL in the symbol table chain.
	 */
	public SymbolTable findGlobalTable() {

		if (fGlobalTable)
			return this;

		if (parentTable != null)
			return parentTable.findGlobalTable();

		return null;

	}

	/**
	 * Locate a symbol table by name, starting with a given symbol table. The
	 * table can be located by it's exact table name (not very typically done)
	 * or by some reserved names, "LOCAL", "PARENT", "GLOBAL", and "CONSTANTS".
	 * 
	 * @param tableName
	 *            The name of the table to be searched for.
	 * @return A SymbolTable with the given name, or null if it was not found.
	 */
	public SymbolTable findTable(final String tableName) {

		/*
		 * Search to see if we can find an exact (case-insensitive) match to the
		 * starting table name, if one was given.
		 */

		for (SymbolTable foundTable = this; foundTable != null; foundTable = foundTable.parentTable)
			if (foundTable.name.equalsIgnoreCase(tableName))
				return foundTable;

		/*
		 * If no exact match, look for the "special" names
		 */
		if (tableName.equalsIgnoreCase("LOCAL"))
			return this;
		if (tableName.equalsIgnoreCase("PARENT"))
			return this.parentTable;
		if (tableName.equalsIgnoreCase("GLOBAL"))
			return findGlobalTable();
		if( tableName.equalsIgnoreCase("MACRO"))
			return session.macroTable;
		if (tableName.equalsIgnoreCase("CONSTANTS"))
			return findGlobalTable().parentTable;

		/*
		 * No such table, gosh darn it!
		 */
		return null;

	}

	/**
	 * Method to determine which symbol table a symbol name is found in. This is
	 * used to locate global symbols, and return the correct symbol table to use
	 * for insertion, etc. of the value.
	 * 
	 * @param symbolName
	 *            Name of the symbol to look for in the hierarchy of symbol
	 *            tables
	 * @return SymbolTable object that contains the symbol, or null if not
	 *         found.
	 */
	public SymbolTable findTableContaining(final String symbolName) {


		Value d;
		d = table.get(symbolName);
		if (d != null) {
			return this; // Found in our symbol table.
		}
		
		// Not in ours, but try up a level

		if (parentTable != null)
			return parentTable.findTableContaining(symbolName);

		// Or no more levels to look, so return null (not found)
		return null;
	}

	/**
	 * Get a reference to a named element. This is useful if you are planning on
	 * changing the value (as an lvalue, for example) and want the actual object
	 * in the symbol table. The more common use is to use the getElement()
	 * method that returns a copy of the object.
	 * 
	 * @param symbolName
	 *            The name of the symbol to look up
	 * 
	 * @return The Value object reference from the symbol table. If the symbol
	 *         does not exist in the current table or one of it's parents, then
	 *         a null is returned.
	 * @throws JBasicException  when the variable name is unknown
	 */

	public Value reference(final String symbolName) throws JBasicException {
		return reference(symbolName, true);
	}

	/**
	 * Get a reference to a named element. This is useful if you are planning on
	 * changing the value (as an lvalue, for example) and want the actual object
	 * in the symbol table. The more common use is to use the getElement()
	 * method that returns a copy of the object.
	 * 
	 * @param symbolName
	 *            The name of the symbol to look up.  This <em>must</em> already
	 *            have been normalized to uppercase by the parser or caller to maintain the
	 *            case-neutrality of JBasic.
	 * @param checkConnectors
	 *            A flag that indicates if connectors should be checked. This is
	 *            normally true, and is only set to false when the system is
	 *            actually updating connectors - which is required to prevent
	 *            recursion.
	 * 
	 * @return The Value object reference from the symbol table.
	 * @throws JBasicException when the variable name is unknown
	 */

	public Value reference(final String symbolName,
			final boolean checkConnectors) throws JBasicException {


		Value d = table.get(symbolName);
		
		if( d == null ) {
			if (parentTable != null)
				return parentTable.reference(symbolName);
			String name = symbolName;
			if( name.startsWith(JBasic.FILEPREFIX))
				name = "#" + name.substring(JBasic.FILEPREFIX.length());

			throw new JBasicException(Status.UNKVAR, name);
		}
		
		/* See if this is a connector that needs refreshing */
		if (checkConnectors && fGlobalTable)
			updateConnector(symbolName, d);

		return d;
	}


	/**
	 * Get a reference to a named element. This is useful if you are planning on
	 * changing the value (as an lvalue, for example) and want the actual object
	 * in the symbol table. The more common use is to use the getElement()
	 * method that returns a copy of the object.
	 * <p>
	 * A key difference between this and the reference() method is that this does
	 * not throw an exception, but instead returns a null pointer.  The caller must
	 * always check for this case, unlike reference() which just throws the error.
	 * <p>
	 * 
	 * @param symbolName
	 *            The name of the symbol to look up.  This <em>must</em> already
	 *            have been normalized to uppercase by the parser or caller to maintain the
	 *            case-neutrality of JBasic.
	 * @param checkConnectors
	 *            A flag that indicates if connectors should be checked. This is
	 *            normally true, and is only set to false when the system is
	 *            actually updating connectors - which is required to prevent
	 *            recursion.
	 * 
	 * @return The Value object reference from the symbol table.  If the symbol was
	 * not found, then a null pointer is returned.
	 */

	public Value findReference(final String symbolName,
			final boolean checkConnectors) {


		Value d = table.get(symbolName);
		
		if( d == null ) {
			if (parentTable != null)
				return parentTable.findReference(symbolName, checkConnectors);
			return null;
		}
		
		/* See if this is a connector that needs refreshing */
		if (checkConnectors && fGlobalTable)
			updateConnector(symbolName, d);

		return d;
	}

	/**
	 * Update the connector if the named variable is a connector variable (SYS$$
	 * prefix). This only happens for the system table, and is only done when
	 * the prefix matches and the name matches. Based on whatever the name is,
	 * the associated variable name is set with the runtime value. For example,
	 * SYS$$CACHEHIT gets set to JBasic.statementCacheHit.
	 * 
	 * @param normalizedName
	 *            The name of the symbol to update the connector for, if it
	 *            exists. The name must already be in all upper-case.
	 * 
	 * @param v The value being updated.  If this is non-null, it assumes
	 * that the caller has already acquired the value and just needs to be
	 * sure it has been updated.  If null, then it assumes that the value
	 * must be set into the current table first.
	 * 
	 * @return true if the connector variable was modified by this operation.
	 */
	public boolean updateConnector(final String normalizedName, Value v) {
		if (normalizedName.startsWith("SYS$$")) {

			/*
			 * Make a local copy, because if it's null we're going to write
			 * over it.
			 */
			Value theValue = v;

			/*
			 * If the value was not given to us already, see if it exists
			 * in the table.  If it doesn't exist in the table, create it
			 * and then get a pointer to the newly created value.
			 */

			if( theValue  == null ) {
				/* Look up the variable */
				theValue = this.localReference(normalizedName);

				/* If not found, we create one and then look it up again */
				if( theValue == null ) {
					return false;
				}
			}

			/*
			 * Handle "magic variables" that are runtime connectors.
			 */

			/*
			 * Attempts to look up a function in the cache
			 */
			if (normalizedName.equals("SYS$$FCACHE_TRIES")) {
				theValue.setInteger(Functions.functionCacheTries);
				return true;
			}

			/*
			 * Times the function run() method was already found
			 * in the cache.
			 */
			if (normalizedName.equals("SYS$$FCACHE_HITS")) {
				theValue.setInteger(Functions.functionCacheHits);
				return true;
			}
			/*
			 * Number of bytecode instructions executed
			 */
			if (normalizedName.equals("SYS$$INSTRUCTIONS_EXECUTED")) {
				theValue.setInteger(session.instructionsExecuted);
				return true;
			}


			/*
			 * Number of statements compiled (seen)
			 */
			if (normalizedName.equals("SYS$$STATEMENTS_COMPILED")) {
				theValue.setInteger(session.statementsCompiled);
				return false;
			}

			/*
			 * Number of times a statement was executed in bytecode form
			 */
			if (normalizedName.equals("SYS$$STATEMENTS_EXECUTED")) {
				theValue.setInteger(session.statementsByteCodeExecuted);
				return true;
			}

			/*
			 * Number of times a statement was executed in interpreted form.
			 */
			if (normalizedName.equals("SYS$$STATEMENTS_INTERPRETED")) {
				theValue.setInteger(session.statementsInterpreted);
				return true;
			}

			/*
			 * Number of statements executed in stored code.
			 */
			if (normalizedName.equals("SYS$$STATEMENTS")) {
				theValue.setInteger(session.statementsExecuted);
				return true;

			}
		}
		return false;
	}

	/**
	 * 
	 * Get a copy of the named element. Use this if you plan on manipulating the
	 * result value in some way; if you don't make a copy then you are changing
	 * the actual value of the symbol (usually not what you want).
	 * 
	 * @param symbolName
	 *            The name of the symbol to look up.  This <em>must</em> be in
	 *            uppercase.
	 * @return A copy of the data element in the symbol table
	 * @throws JBasicException indicating if the symbol was unknown.
	 */

	public Value value(final String symbolName) throws JBasicException {

		/*
		 * Try to locate the symbol value by the given name.
		 */
		final Value value = table.get(symbolName);

		/*
		 * If not found and there is a parent table above this one,
		 * try to ask that table to return the value.  Otherwise 
		 * just report that there is no such variable by returning
		 * a null.
		 */
		if (value == null) {
			if( parentTable != null)
				return parentTable.value(symbolName);
			//return null;
			throw new JBasicException(Status.UNKVAR, symbolName);
		}
		
		/*
		 * If this is the global symbol table, then it can contain
		 * "Connector" values which are JBasic-accessible symbols set
		 * to match internal-to-JBasic counters and other variables.
		 * 
		 * When someone requests a value from the global table, then
		 * we update the connector for the given name.
		 */
		if (fGlobalTable)
			updateConnector(symbolName, value);

		/*
		 * After making sure the value is up-to-date, return a copy of
		 * the value and we're done.
		 */
		return value.copy();
	}

	/**
	 * Accessor function to get an integer value from a symbol table element,
	 * with implicit type coercion if needed.
	 * 
	 * @param n
	 *            The name of the symbol to look up
	 * @return The symbol value converted to an integer type.
	 */
	public int getInteger(final String n) {

		Value s = findReference(n, true);
		if( s == null )
			return 0;
		return s.getInteger();
	}

	/**
	 * Accessor function to get an double value from a symbol table element,
	 * with implicit type coercion if needed.
	 * 
	 * @param n
	 *            The name of the symbol to look up
	 * @return The symbol value converted to an double type.
	 */

	public double getDouble(final String n) {

		Value v = findReference(n, false);
		if( v == null )
			return 0.0;
		return v.getDouble();

	}

	/**
	 * Accessor function to get a boolean value. Coercion occurs as needed.
	 * 
	 * @param n
	 *            Name of symbol to look up in table.
	 * @return Boolean value of symbol (after optional coercion)
	 */

	public boolean getBoolean(final String n) {

		Value v = findReference(n, false);
		if( v == null)
			return false;
		
		return v.getBoolean();
	}

	/**
	 * Accessor function to get an string value from a symbol table element,
	 * with implicit type coercion if needed.
	 * 
	 * @param n
	 *            The name of the symbol to look up
	 * @return The symbol value converted to an string type with appropriate
	 *         formatting.
	 */

	public String getString(final String n) {
		Value v = findReference(n,false);
		if( v == null )
			return null;
		return v.getString();
	}

	/**
	 * Accessor function to get the data type from a symbol table element.
	 * 
	 * @param n
	 *            The name of the symbol to look up
	 * @return The symbol's data type.
	 */

	public int getType(final String n) {

		/*
		 * Locate the named value, and get the Value object stored in
		 * the symbol table.  If there is such a value, return it's
		 * type field value.
		 */
		try {
			return reference(n).getType();
		} catch (JBasicException e) {
			return Value.UNDEFINED;
		}
	}

	/**
	 * Mark a symbol as read-only. A read-only symbol cannot be modified after it
	 * is created, and can only be deleted when the symbol table containing it
	 * is deleted.
	 * 
	 * @param symbolName
	 *            The name of the symbol, in uppercase. The symbol is located by
	 *            searching starting at the current table. Wherever it is found
	 *            (current table or a parent table), it is marked as read-only.
	 * @throws JBasicException if teh symbol is unknown
	 */
	public void markReadOnly(final String symbolName) throws JBasicException {

		final Value s = reference(symbolName);
		s.fReadonly = true;
	}

	/**
	 * Delete a symbol from the current table, by name. The variable must exist
	 * and not be marked read-only to succeed.
	 * 
	 * @param varName
	 *            A String containing the name of the variable to delete.
	 * @return A Status indicating if the delete was successful.
	 */
	public Status delete(String varName) {
		
		if( varName == null )
			return new Status();
		
		String vname = varName.toUpperCase();

		if (vname.startsWith("SYS$"))
			return new Status(Status.NODELVAR, vname);

		/*
		 * Look up the variable to see if it is readonly.
		 */

		if (isReadOnly(vname))
			return new Status(Status.NODELVAR, vname);

		/*
		 * Find the symbol table that contains it. If it is not found, then
		 * error.
		 */
		final SymbolTable parentTable = findTableContaining(vname);
		if (parentTable == null)
			return new Status(Status.UNKVAR, vname);

		/*
		 * Delete the item. If it wasn't found, then we have a weird internal
		 * consistency error.
		 */
		if (parentTable.table.remove(vname) == null)
			return new Status(Status.UNKVAR, vname);
		return new Status();
	}

	/**
	 * Delete a symbol from the current table, by name. The variable must exist
	 * to succeed. Unlike delete(), this will remove items from a system symbol
	 * table, or marked readonly. It is invoked by using the _CLEAR ByteCode
	 * with an integer code of 1.
	 * 
	 * @param varName
	 *            A String containing the name of the variable to delete.
	 * @return A Status indicating if the delete was successful.
	 */
	public Status deleteAlways(String varName) {
		
		/*
		 * The name of the variable we are to delete.  Because this
		 * can be a result of a runtime expression, we must explicitly
		 * convert the name to upper case before use.
		 */
		String vname = varName.toUpperCase();

		/*
		 * Find the symbol table that contains it. If it is not found, then
		 * error.
		 */
		final SymbolTable parentTable = findTableContaining(vname);
		if (parentTable == null)
			return new Status(Status.UNKVAR, vname);

		/*
		 * Delete the item. If it wasn't found, then we have a weird internal
		 * consistency error.
		 */
		if (parentTable.table.remove(vname) == null)
			return new Status(Status.UNKVAR, vname);
		
		return new Status();
	}

	/**
	 * Same as getElementReference but only searches the local symbol table.
	 * 
	 * @param normalizedName    Name of symbol to search for
	 * @return A value if it is found in the nearest-local table, or null if it
	 *         was not found.
	 */
	public Value localReference(final String normalizedName) {
		return table.get(normalizedName);
	}

	/**
	 * Mark a symbol name as being in the "COMMON" block for this symbol
	 * table.  This means that the symbol name will be retained during a
	 * CHAIN operation.
	 * 
	 * @param symbolName the name of the symbol to be retained in the COMMON
	 * block.
	 */
	public void setCommon(String symbolName) {

		/*
		 * If we don't have a common block list set up yet, do so now.
		 */
		if( commonBlock == null )
			commonBlock = new ArrayList<String>();

		else {
			/*
			 * Otherwise, see if we already have this name on the list.
			 */
			for( int ix = 0; ix < commonBlock.size(); ix++)
				if( commonBlock.get(ix).equals(symbolName))
					return;
		}
		/*
		 * Not already on the list, so add it to the end of the
		 * vector.
		 */
		commonBlock.add(symbolName);
	}

	
	/**
	 * Return an indicator if a given symbol name is to be considered part
	 * of the common block of the current program, for use in CHAIN statements.
	 * @param symbolName the name of the symbol, which must be upper case.
	 * @return true if the symbol had previously been marked as COMMON
	 */
	public boolean isCommon(String symbolName ) {
		if( commonBlock == null )
			return false;
		int ix = 0;
		for( ix = 0; ix < commonBlock.size(); ix++)
			if( commonBlock.get(ix).equals(symbolName))
				return true;
		
		return false;		
	}

	/**
	 * Dump a specific symbol table to the console. Since symbol tables are in
	 * fact HashMaps, they aren't in any particular order (and in fact, the order
	 * in the table changes over time to optimize access). However, the user has
	 * an expectation of order, so we want to display the table info in some
	 * kind of alphabetical order.
	 * 
	 * In order (hah!) to make this happen, we're going to temporarily copy the
	 * contents of the HashMap to a TreeMap and use that to do the ordered dump,
	 * using an iterator. The variables in the table are displayed, along with a
	 * formatted version of their values.
	 * 
	 * Note that this will not output anything for the Root session unless the
	 * program doing the output is the first session created, which owns the
	 * root session by default.
	 * @param outputSession  the session being used to output the data.  This
	 * has to be passed in from the caller so we output to the sessions
	 * running the dump command,which is not always the owner of the table
	 * (for example, the ROOT table is owned by the process root JBasic
	 * object, which means a dumptable() of it would come out on the console
	 * when executed by a thread, etc.
	 * 
	 * @param hidden flag indicating if hidden symbols are to be displayed
	 * @return A Status value indicating if the dump was successful.
	 */
	public Status dumpTable(JBasic outputSession, boolean hidden) {

		Value m = null;
		String rof = "";
		if( outputSession == null )
			return new Status();
		
		outputSession.stdout.println("Table: " + this.name);

		/*
		 * If this is the global table, then we first need to update the
		 * connectors. Because a connector update can cause the insertion of new
		 * data into the system table, we scan it first to do the connector
		 * updates, and then we run the table like normal to fetch the values.
		 */

		if (fGlobalTable)
			for (final Iterator i = this.table.keySet().iterator(); i.hasNext();) {
				String name = (String) i.next();
				m = table.get(name);
				updateConnector(name, null);
			}

		/*
		 * Copy everything from the "real" table to the temporary ordered table.
		 */
		TreeMap<String,Value> tempTable = new TreeMap<String,Value>();
		for (final Iterator i = table.keySet().iterator(); i.hasNext();) {
			String name = (String) i.next();
			m = table.get(name);
			if( name.startsWith("__")) {
				if( hidden )
					tempTable.put(name, m);
			} else
				tempTable.put(name, m);
		}

		/*
		 * Now scan the table and print out all the values. Because the symbol
		 * table is a TreeMap, we always get the stuff back in alphabetical
		 * order by variable name (the key).
		 */
		for (final Iterator i = tempTable.keySet().iterator(); i.hasNext();) {
			String name = (String) i.next();
			m = tempTable.get(name);

			rof = "";
			if (m.fReadonly)
				rof = "readonly";

			if(isCommon(name)) {
				if( !rof.equals(""))
					rof = rof + ", ";
				rof = rof + "common";
			}
			
			if( !rof.equals(""))
				rof = " (" + rof + ")";
			outputSession.stdout.println("   " + name + " = " + m.displayFormat()
					+ rof);
		}

		/*
		 * All done with the temporary table, let's explicitly free it up here.
		 */

		tempTable = null;

		return new Status(Status.SUCCESS);
	}

}
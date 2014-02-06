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
package org.fernwood.jbasic.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.opcodes.OpCATALOG;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import java.math.BigDecimal;


/**
 * This class describes "data elements" which are typed data objects used in
 * expressions and symbol table management. The object contains a type code and
 * fields for each possible data type. The object optionally may have a name as
 * well.
 * <p>
 * Value objects can have the following types: <list>
 * <li><code>INTEGER</code> &nbsp;&nbsp;&nbsp;a 32 bit integer
 * <li><code>STRING</code> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;A Unicode string
 * <li><code>DOUBLE</code> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;a 64-bit floating
 * point number
 * <li><code>BOOLEAN</code> &nbsp;&nbsp;&nbsp;a boolean value of 'true' or
 * 'false'
 * <li><code>ARRAY</code> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; an
 * arrayValue of Value objects
 * <li><code>RECORD</code> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;a HashMap of Value
 * objects, keyed by a name </list> <br>
 * <br>
 * The ARRAY type involves the use of a ArrayList data item indexed by an integer
 * value that is zero-based. A RECORD is a TreeMap with arbitrary names for the
 * sub-members, which are themselves Value objects. <br>
 * <br>
 * You can create constants in the language of any of these types. A number will
 * be an integer if it fits in the integer data type, else it will be a double.
 * The reserved values of 'true' and 'false' are created in the language to
 * represent boolean constants, and can be assigned to any variable. <br>
 * <br>
 * Array constants are defined by a list of items in brackets, such as <br>
 * <br>
 * <code>
 * [ "Tom", 44, "M" ]
 * </code><br>
 * <br>
 * You can even have nested arrays, such as <br>
 * <br>
 * <code>
 * [[ "Tom", 44 ], [ "Mary", 43 ]]
 * </code><br>
 * <br>
 * This creates an arrayValue of arrays, each element of which has it's own
 * type. In the above example, the first element in the nested arrayValue is a
 * string, and the second is an integer. See the documentation on the PRINT
 * statement for how you can print arrays to files to create records. <br>
 * <br>
 * Records similarly have a text syntax, delimited by braces:<br>
 * <br>
 * <code>
 *  { name: "Sue", age: 33 }
 *  </code><br>
 * <br>
 * This creates a recordValue with two members or fields. Each is identified by
 * a name/value pair, separated by a colon. As with ARRAY values, complex
 * nexting is possible, such as:<br>
 * <br>
 * <code>
 *  { name: "Bud", roles: { "admin", "accounting" }}
 *  </code><br>
 * <p>
 * <br>
 * 
 * @author Tom Cole
 * @version version 2.1 April, 2009 overload private Object for most types.
 * 
 */
public class Value {

	/**
	 * The name of the element if it is a variable.
	 * 
	 * 
	 */
	protected String name;

	/**
	 * Get the name of the value, if it has been previously named by storing it
	 * in a symbol table.
	 * 
	 * @return A string containing the name, or null if no name has been set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the value.
	 * 
	 * @param newName
	 *            A string containing the new name to store with the Value. This
	 *            is usually used for diagnostic purposes only.
	 */
	public void setName(final String newName) {
		name = newName;
	}

	/**
	 * The type of the data element, from the VALUE_* list. The element contains
	 * slots for all major types, but only one is used to contain the value at a
	 * given time, based on this type code.
	 */
	protected int type;

	/**
	 * Accessor function to fetch the data type of the given object.
	 * 
	 * @return An integer with the type code (Value.INTEGER, Value.STRING, etc.)
	 */
	public int getType() {
		return type;
	}

	/**
	 * Test to see if the current value is of a given type.
	 * 
	 * @param testType
	 *            An integer describing the type, such as Value.STRING or
	 *            Value.BOOLEAN.
	 * @return True if the current object is of the given type.
	 */

	public boolean isType(final int testType) {
		
		/* Special case for Value.NUMBER which is "any number" */
		if( testType == Value.NUMBER && 
				(type == Value.INTEGER ||
				 type == Value.DOUBLE ||
				 type == Value.DECIMAL))
			return true;
		if( testType == Value.ARRAY && type == Value.TABLE)
			return true;
		
		return type == testType;
	}

	/**
	 * Flag used to detect when a value has been modified.
	 */
	protected boolean updated;

	/**
	 * Get the update flag, and clear it.
	 * 
	 * @return true if this symbol has been updated since the last query.
	 */
	public boolean updated() {
		final boolean b = updated;
		updated = false;
		return b;
	}

	/**
	 * The actual Value object  This contains the String, ArrayList, or HashMap
	 * for complex objects.  For INTEGER, STRING, or BOOLEAN this is always
	 * null.
	 */

	protected Object value;

	/**
	 * When the type is INTEGER (the most common data type), store it
	 * as a separate integer here in the value so we don't keep creating
	 * Integer() objects to store in the 'value' above.  This is also used
	 * to store a boolean object in the integer value.
	 */
	private int integerValue;

	/**
	 * When the type is DOUBLE (the 2nd most common data type), store it
	 * as a separate double here in the value so we don't keep creating
	 * Double() objects to store in the 'value' above.
	 */

	private double doubleValue;

	/**
	 * Set the equivalent boolean value for this item, by storing it
	 * in the integer value.
	 * @param b if true, the integer value is set to 1, else it is set to 0.
	 */
	public void setBoolean( boolean b ) {
		value = null;
		integerValue = b? 1:0;
	}


	/**
	 * Get the array size of the current object if it is an array, or the
	 * number of key value pairs if it is a record.  For scalar objects, this
	 * always returns zero.
	 * 
	 * @return An int with the number of elements in the object.
	 */
	public int size() {

		if( isType(Value.ARRAY))
			return getArray().size();

		if( type == Value.RECORD)
			return getRecord().size();

		return 0;
	}

	/**
	 * Local accessor function that casts the overloaded 'value' Object as
	 * a Vector representing an array.
	 * @return
	 */
	@SuppressWarnings("unchecked") 
	protected ArrayList<Value> getArray() {
		return (ArrayList<Value>) value;
	}

	/**
	 * Stub function for getting an object class.  For a normal value, this
	 * returns a class indicator for the JBasic scalar data type when one
	 * is available, else null.
	 * @return the Class of the underlying scalar object, or null if the
	 * underlying JBasic object cannot be represented as a Java class, such
	 * as a RECORD datatype.
	 */
	public Class getObjectClass() {
		switch (getType()) {
		case Value.INTEGER:
			return int.class;
		case Value.STRING:
			return String.class;
		case Value.BOOLEAN:
			return boolean.class;
		case Value.DOUBLE:
			return double.class;
		}
		
		if( this.value == null )
			return null;
		
		return this.value.getClass();
	}

	/**
	 * You cannot call a method on a traditional Value, only an ObjectValue.  This will throw
	 * an error.
	 * @param methodName the name of the method to call
	 * @param argList the argument list of Values to pass as parameters
	 * @return a Value with the method result
	 * @throws JBasicException This routine always throws INVOBJOP because the object is not
	 * a wrapper for a Java object.
	 */
	public Value invokeMethod(String methodName, ArgumentList argList) throws JBasicException {

		throw new JBasicException(Status.INVOBJOP, "method invocation");
	}
	/**
	 * Get a field from the record in the current object.
	 * 
	 * @param name
	 *            A string describing the field name.
	 * @return A Value object that describes the field value.
	 */
	public Value getElement(final String name) {
		if (type != RECORD)
			return null;
		return getRecord().get(name);
	}

	/**
	 * Fetch a member from an array by integer index number
	 * 
	 * @param index
	 *            The 1-based arrayValue position that we are fetching data from
	 * @return A Value containing the contents of that element, or a null
	 *         pointer if the arrayValue index was invalid.
	 */
	public Value getElement(final int index) {
		Value d;
		if( !isType(Value.ARRAY ))
			return null;

		try {
			d = getArray().get(index - 1);
		} catch (final Exception e) {
			return null;
		}
		return d;

	}

	/**
	 * Put a value into a field in a record. The field is created if it does not
	 * already exist.
	 * 
	 * @param v
	 *            The value to store in the field.
	 * @param name
	 *            A string containing the uppercase field name.
	 *            @return true if the record was successfully updated, else false
	 *            if the value is not a RECORD or is READONLY.
	 */
	public boolean setElement(final Value v, final String name) {
		if( this.type != Value.RECORD )
			return false;
		if( this.fReadonly )
			return false;
		if( v == null)
			return false;
		updated = true;
		v.name = name;
		getRecord().put(name, v);
		return true;
	}

	/**
	 * Put a string into a field of a record.  The field is created if it does
	 * not already exist.
	 * @param s the String value to store in a record field
	 * @param name the name of the field (must already be in uppercase)
	 * @return true if the store was successful, else false if the current
	 * value is not a record.
	 */
	public boolean setElement(final String s, final String name ) {
		if( this.type != Value.RECORD )
			return false;
		if( this.fReadonly )
			return false;

		updated = true;
		Value v = new Value(s);
		v.name = name;
		getRecord().put(name, v);
		return true;
	}

	/**
	 * Put an integer into a field of a record.  The field is created if it does
	 * not already exist.
	 * @param i the int value to store in a record field
	 * @param name the name of the field (must already be in uppercase)
	 * @return true if the store was successful, else false if the current
	 * value is not a record.
	 */
	public boolean setElement(final int i, final String name ) {
		if( this.type != Value.RECORD )
			return false;
		if( this.fReadonly )
			return false;

		updated = true;
		Value v = new Value(i);
		v.name = name;
		getRecord().put(name, v);
		return true;
	}
	/**
	 * Put a boolean into a field of a record.  The field is created if it does
	 * not already exist.
	 * @param b the boolean value to store in a record field
	 * @param name the name of the field (must already be in uppercase)
	 * @return true if the store was successful, else false if the current
	 * value is not a record.
	 */
	public boolean setElement(final boolean b, final String name ) {
		if( this.type != Value.RECORD )
			return false;
		if( this.fReadonly )
			return false;

		updated = true;
		Value v = new Value(b);
		v.name = name;
		getRecord().put(name, v);
		return true;
	}


	/**
	 * Change a value in the array, even if the array is marked as read-only.
	 * This is used internally to modified system-level arrays such as
	 * SYS$PROGRAMS.
	 * 
	 * @param d
	 *            The value to store in the array.
	 * @param index
	 *            The 1-based index in the array to store the value in.
	 */
	public void setElementOverride(final Value d, final int index) {
		if( this.type != Value.ARRAY )
			return;

		final boolean saved_flag = fReadonly;
		updated = true;
		fReadonly = false;
		setElement(d, index);
		fReadonly = saved_flag;
		return;
	}

	/**
	 * Change a value in the array.
	 * 
	 * @param d
	 *            The value to store in the array.
	 * @param theIndex
	 *            The 1-based location in the array. This must be less than or
	 *            equal to the number of elements to change an existing value.
	 *            It may be one greater than the size of the array, to add a new
	 *            element at the end of an array.
	 */
	public void setElement(final Value d, final int theIndex) {
		if (fReadonly)
			return;
		if( theIndex < 1 )
			return;
		updated = true;

		/*
		 * Arrays are 1-based, but ArrayList are zero-based.
		 */

		final int index = theIndex - 1;

		/*
		 * If this is an element index that is beyond the current array size,
		 * go ahead and extend the array now.  Then set the specific element.
		 */

		ArrayList<Value> theArray = getArray();

		final int len = theArray.size();
		if (index >= len) {
			/* Preallocate the space in the vector */
			theArray.ensureCapacity(index+1);

			/* And then fill with integer zero */
			while (theArray.size() <= index)
				theArray.add(new Value(0));
		}
		theArray.set(index, d);

	}

	/**
	 * Add value to the end of the array.  The value can be another array, in
	 * which case the array elements are added to this array.
	 * 
	 * @param d
	 *            The value to store in the array.
	 * 
	 * @return the position in the array where the new element was stored; i.e.
	 * the new length of the array.  If the element cannot be added, the value
	 * returned is zero.  This could be because the array is read-only or is
	 * not even an array.
	 */
	public int addElement(final Value d) {
		if (fReadonly)
			return 0;

		if( this.type != Value.ARRAY )
			return 0;

		updated = true;

		if( d.type == Value.OBJECT || d.type == Value.UNDEFINED )
			return 0;

		if( d.type == Value.ARRAY ) {
			for( int j = 1; j <= d.size(); j++ ) {
				if( addElement(d.getElement(j)) == 0 )
					return 0;
			}
		}
		else {
			getArray().add(d);
		}
		return getArray().size();
	}

	/**
	 * Add all the record elements of another value to this one.
	 * @param value1 the record to be added to the current record.
	 * @throws JBasicException if the addend is a Java object
	 * instead of a Jbasic record
	 */
	public void addElementRecord(Value value1) throws JBasicException {
		if( type != Value.RECORD )
			return;
		if( value1.getType() != Value.RECORD)
			return;
		if( value1.isObject())
			throw new JBasicException(Status.INVOBJOP, "record add");

		getRecord().putAll(value1.getRecord());		
	}

	/**
	 * Indicator if the current Value is a wrapper for an actual
	 * Java object.
	 * @return false, since this data type can never contain a Java
	 * object.
	 */
	public boolean isObject() {
		return false;
	}

	/**
	 * Perform a "shallow" array concatenation.  That is, copy each
	 * element of the array as-is to the new array, and do not recursively
	 * copy elements to flatten out the array.
	 * @param value the value to add to the current array
	 */
	public void addElementShallow(Value value) {
		if (!isType(Value.ARRAY))
			return;

		if (value.isType(Value.ARRAY)) {
			int count = value.size();
			for (int ix = 1; ix <= count; ix++)
				this.getArray().add(value.getElement(ix));
		} else
			this.getArray().add(value);
	}

	/**
	 * Add an element to the array, without doing any additional flattening
	 * or modification of the element being added.  This is different from
	 * addElement() or addElementShallow() which will expand recursively or
	 * one level any array added to the current array.
	 * @param value The value that is added to the current array.
	 */
	public void addElementAsIs(Value value ) {
		if( type != Value.ARRAY )
			return;
		this.getArray().add(value);
	}
	/**
	 * This indicates if the data element is to be considered read-only. This is
	 * honored for all programming statements; this can be overridden in the
	 * JBasic code where needed.
	 * 
	 * 
	 */
	public boolean fReadonly;

	/**
	 * Flag that indicates if this symbol exists in a symbol table.  This
	 * flag is used to determine if a copy must be made before it is stored,
	 * to prevent it from changing a previous copy of the symbol.
	 */

	public boolean fSymbol;

	/**
	 * Flag to indicate if this variable is to be considered in a COMMON
	 * area in GWBASIC terminology; such variables are preserved across
	 * a CHAIN statement.  This is implemented during CHAIN by removing all
	 * variables that <i>do not</i> have the common flag from the symbol
	 * table before chaining to a new program.  This flag is normally false
	 * in all other cases.
	 */
	public boolean fCommon;

	/**
	 * Undefined data type, cannot be used in an expression operation.
	 */
	static public final int UNDEFINED = 0;

	/*
	 * The next three types must be in ascending numerical order, with the
	 * smallest value representing the type with the least precision
	 * requirement, and the largest number being the type with the greatest
	 * precision requirements.
	 */
	/**
	 * Boolean data type
	 */
	static public final int BOOLEAN = 10;

	/**
	 * Integer data type
	 */
	static public final int INTEGER = 11;

	/**
	 * Double precision floating point value type
	 */
	static public final int DOUBLE = 12;

	/**
	 * This is a special code used to identify an INTEGER *or* DOUBLE,
	 * typically in argument list checking. No actual Value will ever
	 * have this type.
	 */
	public static final int NUMBER = 13;

	/**
	 * Decimal data type for arbitrarily long mantissas
	 */
	
	public static final int DECIMAL = 14;
	
	/**
	 * String type.
	 */
	static public final int STRING = 20;

	/**
	 * Formatted string. This is the same as a string, except that when you
	 * coerce something else to a formatted string, the rules used are those for
	 * PRINT.
	 */
	static public final int FORMATTED_STRING = 21;

	/**
	 * Formatted string, except that string values themselves are quoted
	 * and normalized; i.e. control characters are converted to printable
	 * representations such as "\n" for new-line characters.
	 */
	static public final int QUOTED_FORMATTED_STRING = 22;

	/**
	 * Array type. An arrayValue type contains a list of DataElements, each of
	 * which can be it's own type.
	 */
	static public final int ARRAY = 30;

	/**
	 * Record type. The object contains a TreeMap that has a string name for
	 * each key, and a Value as the member. This is used to implement records
	 * with arbitrary fields, such as in the following example syntax: <br>
	 * <br>
	 * <code>x = { name: "Test", size: 33 }</code> <br>
	 * <code>print x.name</code> <br>
	 */
	static public final int RECORD = 31;

	/**
	 * Generic object. The data element is a reference to an arbitrary object.
	 */

	static public final int OBJECT = 32;

	/**
	 * This is the name of the record member that contains the object data
	 * when a record is used a an object.
	 */
	static public final String OBJECT_DATA = "_OBJECT$DATA";

	/**
	 * A RecordStreamValue which represents a result set, called a TABLE
	 * in the JBasic language.
	 */
	public static final int TABLE = 33;

	/**
	 * Same as STRING, but run through the denormalizer that processes escaped
	 * characters such as \n or \u0022.
	 */
	public static final int NORMALIZED_STRING = 23;


	/**
	 * Create a new Value of type STRING
	 * 
	 * @param s
	 *            The string value used to initialize the Value.
	 */
	public Value(final String s) {
		value = s;
		updated = true;
		type = STRING;
	}

	/**
	 * Create a enw Value of type DECIMAL
	 * @param d the DECIMAL item to initialize the value with.
	 */
	
	public Value( final BigDecimal d) {
		type = DECIMAL;
		this.value = d;
		updated = true;
	}
	/**
	 * Create a new Value of type DOUBLE
	 * 
	 * @param d
	 *            The double value used to initialize the Value.
	 */
	public Value(final double d) {
		doubleValue = d;
		updated = true;
		type = DOUBLE;
	}

	/**
	 * Create a new Value of type BOOLEAN
	 * 
	 * @param b
	 *            The boolean value used to initialize the Value.
	 */

	public Value(final boolean b) {
		value = null;
		integerValue = b? 1:0;
		updated = true;
		type = BOOLEAN;
	}

	/**
	 * Create a new Value of type INTEGER
	 * 
	 * @param i
	 *            The integer value used to initialize the Value.
	 */

	public Value(final int i) {
		integerValue = i;
		updated = true;
		type = INTEGER;
	}


	/**
	 * Create a new Value given an arbitrary type and name. The actual
	 * "contents" of the Value are initialized to whatever is the most
	 * appropriate state (empty arrayValue or recordValue, zero or false number
	 * value, etc.)
	 * 
	 * @param theType
	 *            the data type of the new Value to create
	 * @param theName
	 *            the name of the new data type, or null of not named.
	 */
	public Value(final int theType, final String theName) {
		type = theType;
		name = theName;
		updated = true;
		value = null;
		
		switch( theType ) {
		case DECIMAL:
			value = new BigDecimal(0);
			break;
		case ARRAY:
			value = new ArrayList<Value>();
			break;
		case RECORD:
			value = new HashMap<String,Value>();
			break;
		case BOOLEAN:
		case INTEGER:
			integerValue = 0;
			break;

		/* 
		 * NUMBER should never be used, but just in case, 
		 * make it a DOUBLE and fall though to the next case
		 */
		case NUMBER:
			type = Value.DOUBLE;
		case DOUBLE:
			doubleValue = 0.0;
			break;
		case STRING:
			value = "";
			break;
			
		}
	}

	/**
	 * Create a new value that contains a Status object, expressed in the standardized
	 * format as a RECORD with predefined fields for CODE, PARM, etc.
	 * @param status the Status object that is to be expressed as a Value
	 */
	public Value(Status status ) {

		this.type = Value.RECORD;
		value = new HashMap<String,Value>();
		updated = true;
		
		/*
		 * Store the code for the status in the record.
		 */
		String c = status.getCode();
		setElement(new Value(c), "CODE");

		/*
		 * If there is a nested status, store it away in the
		 * current object we're building.
		 */
		Status nestedStatus = status.getNestedStatus();
		if( nestedStatus != null )
			setElement(new Value(nestedStatus), "SUBSTATUS");
		
		/*
		 * If there is a parameter, store it.  If there is no
		 * parameter, store an empty string.
		 */
		String p = status.getMessageParameter();
		if( p == null )
			p = "";
		setElement(new Value(p), "PARM");
		
		/*
		 * Create a boolean market indicating if the current Status
		 * is a success or not.  This is queued by a code that starts
		 * with the "*" asterisk character.
		 */

		boolean isSuccess = (c.charAt(0) == '*');
		setElement(new Value(isSuccess), "SUCCESS");

		/*
		 * If there is a PROGRAM name, store it.  If not, store an
		 * empty string in the record.  Also, store the line which
		 * may be zero if there is no line number in the status.
		 */
		c = status.getProgram();
		if( c != null )
			setElement(new Value(c), "PROGRAM");
		else
			setElement(new Value(""), "PROGRAM");
		setElement(new Value(status.getLine()), "LINE");


	}

	/**
	 * Create a new array value, by populating it from an existing ArrayList of Strings.
	 * @param variableList an ArrayList<String> array whose values will become the individual
	 * array elements in the Value array.
	 */
	@SuppressWarnings("unchecked") 
	public Value(ArrayList<String> variableList) {

		type = Value.ARRAY;
		value = new ArrayList<Value>();
		for( int ix = 0; ix < variableList.size(); ix++ )
			((ArrayList<Value>) value).add(new Value(variableList.get(ix)));
	}

	/**
	 * Type-specific match function. Does the given object equal the current
	 * object, including both type and value?  Unlike compare() which will
	 * change the data types if needed, this method does not modify the
	 * data being compared in any way.
	 * 
	 * @param item
	 *            The Value to compare with the current object
	 * @return Boolean flag indicating they match in both type and value.
	 */
	public boolean match(final Value item) {

		if (type != item.type)
			return false;

		switch (type) {

		/*
		 * Records must have the same number of keys, and each key value
		 * must match in the current and matched record.
		 */
		case RECORD:
			if( this.size() != item.size())
				return false;

			/* Scan using the keys in this value */
			Iterator key = this.getRecord().keySet().iterator();
			while( key.hasNext()) {
				String keyString = (String)key.next();
				Value v1 = this.getElement(keyString);
				Value v2 = item.getElement(keyString);

				/* If matching record is missing this field, fail */
				if( v2 == null )
					return false;

				/* See if the resulting value matches or not */
				if( !v1.match(v2))
					return false;
			}
			/* All keys matched, so we succeed */
			return true;

			/*
			 * Arrays must be of the same length. We then recursively compare
			 * each array element.
			 */
		case ARRAY:
			if( this.size() != item.size())
				return false;
			for( int ix = 1; ix <= item.size(); ix++ ) {
				Value v1 = this.getElement(ix);
				Value v2 = item.getElement(ix);
				if( !v1.match(v2))
					return false;
			}
			return true;

		case BOOLEAN:
			return (getBoolean() == item.getBoolean());

		case DOUBLE:
			return (getDouble() == item.getDouble());

		case DECIMAL:
			return (getDecimal().equals(item.getDecimal()));
			
		case INTEGER:
			return (getInteger() == item.getInteger());

		case STRING:
			return getString().equals(item.getString());

		default:
			return false;
		}
	}

	/**
	 * Return a copy of the current object.
	 * 
	 * @return A deep copy of the current object.
	 */
	public Value copy() {
		return copy(false, 0);
	}

	/**
	 * Create a copy of the referenced object. This creates a new object and
	 * moves the contents to the new object, including copying any string value.
	 * 
	 * @param isObjectCopy
	 *            this parameter is currently unused.
	 * @param depth an integer index used to track recursive copy operations;
	 * the normal non-recursive value to pass is always zero.
	 * @return a new Value that is a copy of the referenced object.
	 */

	public Value copy(final boolean isObjectCopy, int depth) {

		final Value dest = new Value(type, null);
		dest.updated = true;
		dest.fSymbol = false;

		dest.value = value;
		dest.name = name;
		dest.integerValue = integerValue;
		dest.doubleValue = doubleValue;

		/*
		 * If this is a RECORD, then make a copy of the HashMap, recursively.
		 */
		if (type == RECORD) {
			dest.value = new HashMap<String,Value>();
			final Set k = this.getRecord().keySet();

			Iterator i = k.iterator();
			while( i.hasNext()) {
				String keyString = (String)i.next();
				Value dx = this.getElement(keyString);

				/*
				 * We should never hit this, but just in case we detect some kind of runaway
				 * recursion, let's bail out.
				 */
				if( depth > 100 )  {
					System.out.println("WARNING: Object copy recursion depth >100");
					return null;
				}

				dest.setElement(dx.copy(isObjectCopy, depth+1), keyString);
			}
			if (isObjectCopy)
				dest.removeObjectAttribute("METHODS");
		}

		/*
		 * If this is an arrayValue, make a new arrayValue and copy the data
		 * from the old to the new arrayValue.
		 */
		else
			if (isType(ARRAY)) {
				dest.value = new ArrayList<Value>();
				if (value != null) {
					final int len = getArray().size();
					if (len > 0)
						for (int i = 1; i <= len; i++) {
							final Value item = getElement(i);
							if (item != null)
								dest.setElement(item.copy(), i);
						}
				}
			}

		return dest;
	}

	/**
	 * Map the generic object to a RECORD type (implemented as a HashMap)
	 * @return
	 */
	@SuppressWarnings("unchecked") 
	private HashMap<String, Value> getRecord() {
		return (HashMap<String,Value>)value;
	}

	/**
	 * Store an integer in the current object.
	 * @param i the integer value to set 
	 */
	public void setInteger(int i) {
		integerValue = i;
		value = null;
	}

	/**
	 * Delete a member from an array, collapsing other members of the array.
	 * 
	 * @param idx
	 *            The 1-based address in the array to delete a member from.
	 * @return True if an item was removed; false if the index was out of range
	 *         for the array.
	 */
	public boolean removeArrayElement(final int idx) {
		updated = true;

		if (type != ARRAY)
			return false;

		ArrayList theArray = getArray();
		
		if ((idx < 1) || (idx > theArray.size()))
			return false;

		theArray.remove(idx - 1);

		return true;
	}

	/**
	 * Format a value for display to the user.
	 * 
	 * @param o
	 *            The value to format.
	 * @param formatString
	 *            The format string
	 * @return A string containing the formatted expression of the value.
	 */
	public static String format(final Value o, String formatString) {

		String f = formatString;
		final int vType = o.getType();
		if ((vType == Value.RECORD)	|| (vType == Value.ARRAY))
			if (f != null)
				if (!f.equals(""))
					return null;

		if (f == null)
			f = "";

		String objString = Value.toString(o, false);
		boolean negate = false;
		int i;
		String leftString = "";
		String rightString = "";

		boolean leadingZeroes = false;
		boolean parens = false;
		int leftLength = 0;
		int rightLength = 0;
		boolean foundDecimal = false;
		int dollarPosition = 0;
		boolean floatingDollarSign = false;

		final int len = f.length();
		String prefix = "";
		boolean inPrefix = true;

		for (i = 0; i < len; i++) {

			final String ch = f.substring(i, i + 1);

			if (ch.equals("#")) {
				if (foundDecimal)
					rightLength++;
				else
					leftLength++;
				inPrefix = false;
			} else if (ch.equals("0")) {
				leadingZeroes = true;
				leftLength++;
				inPrefix = false;
			} else if (ch.equals(".")) {
				foundDecimal = true;
				inPrefix = false;
			} else if (ch.equals("(") || ch.equals(")")) {
				parens = true;
				inPrefix = false;
			} else if (ch.equals("$")) {
				inPrefix = false;
				if (foundDecimal)
					rightLength++;
				else {
					if (dollarPosition > 0)
						floatingDollarSign = true;
					leftLength++;
					dollarPosition = i + 1;
				}
			} else if (!inPrefix)
				break;
			else
				prefix = prefix + ch;
		}

		if ((parens || leadingZeroes) && objString.startsWith("-")) {
			negate = true;
			objString = objString.substring(1);
		}

		i = objString.indexOf('.');
		if (i == -1) {
			leftString = objString;
			rightString = "";
		} else {
			leftString = objString.substring(0, i);
			rightString = objString.substring(i + 1);
		}

		while (leftString.length() < leftLength)
			if (leadingZeroes)
				leftString = "0" + leftString;
			else
				leftString = " " + leftString;

		if (!parens && negate && leadingZeroes)
			leftString = "-" + leftString.substring(1);

		while (rightString.length() < rightLength)
			rightString = rightString + "0";

		if (rightString.length() > rightLength)
			rightString = rightString.substring(0, rightLength);

		String buffer = prefix + leftString;
		if (foundDecimal)
			buffer = buffer + "." + rightString;

		if (parens)
			if (negate)
				buffer = "(" + buffer + ")";
			else
				buffer = " " + buffer + " ";

		if (dollarPosition > 0)
			if (floatingDollarSign)
				for (i = 0; i < dollarPosition; i++) {
					final String ch = buffer.substring(i, i + 1);
					if (!ch.equals(" ")) {
						buffer = buffer.substring(0, i - 1) + "$"
						+ buffer.substring(i);
						break;
					}

				}
			else
				buffer = buffer.substring(0, dollarPosition - 1) + "$"
				+ buffer.substring(dollarPosition);

		return buffer;

	}


	/**
	 * Get the current value expressed as a string using default formatting.
	 * 
	 * @return A string containing the value expressed as a string.
	 */
	public String getString() {
		switch (type) {
		case Value.INTEGER:
			return Integer.toString(integerValue);

		case Value.BOOLEAN:
			return Boolean.toString(getBoolean());

		case Value.DOUBLE:
			return Double.toString(doubleValue);

		case Value.STRING:
			return (String)value;

		case Value.ARRAY:
		case Value.RECORD:
			return Value.toString(this, false);

		case Value.DECIMAL:
			return ((BigDecimal)value).toString();
			
		case Value.TABLE:
			return toString();
			
		default:
			return null;
		}

	}

	/**
	 * Get the current value as an integer. If the value is not already an
	 * integer, a type conversion is attempted. The value itself is not modified
	 * by this operation.
	 * 
	 * @return The value expressed as an integer, or 0 if it could not be
	 *         converted to a valid integer.
	 */
	public int getInteger() {

		switch (type) {
		case Value.INTEGER:
			return integerValue;

		case Value.BOOLEAN:
			return getBoolean() ? 1 : 0 ;

		case Value.DOUBLE:
			return (int) getDouble();

		case Value.DECIMAL:
			return ((BigDecimal)value).intValue();
			
		case Value.STRING:
			return Integer.parseInt(getString());

		default:
			return 0;
		}

	}

	/**
	 * Get the current value as a boolean. If the value is not already a
	 * boolean, a type conversion is attempted. The value itself is not modified
	 * by this operation.
	 * 
	 * @return The value expressed as an boolean, or false if it could not be
	 *         converted to a valid boolean.
	 */
	public boolean getBoolean() {

		switch (type) {
		case Value.BOOLEAN:
		case Value.INTEGER:
			return (integerValue != 0);

		case Value.DOUBLE:
			return (getDouble() != 0.0);
		case Value.DECIMAL:
			return ((BigDecimal)value).intValue() != 0;

		case Value.STRING:
			if (getString().equalsIgnoreCase("TRUE"))
				return true;
			if (getString().equalsIgnoreCase("YES"))
				return true;
			if (getString().equals("1"))
				return true;
			return false;
		}
		return false;
	}


	/**
	 * Extract the big decimal value in the object, if there is one.
	 * @return the value of the DECIMAL object
	 */
	public BigDecimal getDecimal() {


		switch( type ) {
		case DECIMAL:
			return (BigDecimal) value;
		case INTEGER:
			return new BigDecimal( this.integerValue);
		case BOOLEAN:
			return new BigDecimal( this.integerValue == 1 ? 1 : 0);
		case STRING:
			return new BigDecimal((String) value);
		case DOUBLE:
			return new BigDecimal( this.doubleValue);
			default:
				return null;
		}
	}

	/**
	 * Get the current value as a double. If the value is not already a double,
	 * a type conversion is attempted. The value itself is not modified by this
	 * operation.
	 * 
	 * @return The value expressed as a double, or 0.0 if it could not be
	 *         converted to a valid double.
	 */

	public double getDouble() {
		switch (type) {
		case Value.INTEGER:
			return integerValue;

		case Value.DOUBLE:
			return doubleValue;

		case Value.BOOLEAN:
			return getBoolean() ? 1.0 : 0.0;
			
		case Value.DECIMAL:
			return ((BigDecimal)value).doubleValue();

		case Value.STRING:
			try {
				return Double.parseDouble(getString());
			} catch(NumberFormatException e ) {
				return Double.NaN;
			}

		}
		return 0.0;
	}
	/**
	 * Get an array element as a double value.
	 * @param i the 1-based index into the array
	 * @return the value in the array expressed as a double
	 */
	public double getDouble(int i) {
		if( type != Value.ARRAY)
			return Double.NaN;
		if( i < 1 || i > size())
			return Double.NaN;

		return getElement(i).getDouble();
	}

	/**
	 * Get an array element as an integer value.
	 * @param i the 1-based index into the array
	 * @return the value in the array expressed as a integer
	 */
	public int getInteger(int i) {
		if( type != Value.ARRAY)
			return 0;
		if( i < 1 || i > size())
			return 0;

		return getElement(i).getInteger();
	}

	/**
	 * Get an array element as an DECIMAL value.
	 * @param i the 1-based index into the array
	 * @return the value in the array expressed as a integer
	 */
	public BigDecimal getDecimal(int i) {
		if( type != Value.ARRAY)
			return new BigDecimal(0);
		if( i < 1 || i > size())
			return new BigDecimal(0);

		return getElement(i).getDecimal();
	}

	/**
	 * Get an array element as a string value.
	 * @param i the 1-based index into the array
	 * @return the value in the array expressed as a string
	 */
	public String getString(int i) {
		if( this.type != Value.ARRAY)
			return null;
		if( i < 1 || i > this.size())
			return null;
		
		return this.getElement(i).getString();
	}

	/**
	 * Get a string from a member of the current RECORD.
	 * @param name The name of the record member to retrieve and 
	 * convert to a string value.
	 * @return The item name expressed as a string, or null if the
	 * current Value is not a RECORD or the named element does not
	 * exist.
	 */
	public String getString(String name) {
		if( this.type != Value.RECORD )
			return null;

		Value v = this.getElement(name);
		if( v == null )
			return null;
		return v.getString();
	}


	/**
	 * Format a Value for printing. This uses the type of the object to
	 * determine a suitable textual representation of the value. For most types,
	 * it uses the default string formatter for the base type (double, integer,
	 * etc.).
	 * <p>
	 * The only extra work done here is for floating point values; the default
	 * Java formatter (as of JDK 1.4.2, at least) results in floating point
	 * values that have whole integer values still get a ".0" at the end. We'll
	 * strip that trailing ".0" off if it's there so a floating 33.0 and integer
	 * 33 are displayed the same, as "33".
	 * 
	 * @return printable string representation of the item
	 */

	public String toString() {
		return Value.toString(this, true);
	}

	/**
	 * Format a Value for printing. This uses the type of the object to
	 * determine a suitable textual representation of the value. For most types,
	 * it uses the default string formatter for the base type (double, integer,
	 * etc.).
	 * <p>
	 * The only extra work done here is for floating point values; the default
	 * Java formatter (as of JDK 1.4.2, at least) results in floating point
	 * values that have whole integer values still get a ".0" at the end. We'll
	 * strip that trailing ".0" off if it's there so a floating 33.0 and integer
	 * 33 are displayed the same, as "33".
	 * 
	 * @param v
	 *            the Value to format. The type of the data element will
	 *            determine the actual formatting operation.
	 * @param quote
	 *            Flag to indicate of quotes are to be put around strings. This
	 *            is cleared for print operations, for example, but is set when
	 *            formatting arrayValue contents.
	 * @return printable string representation of the item
	 */

	public static String toString(final Value v, final boolean quote) {

		String s = null;
		Value e = null;
		RecordStreamValue r = null;
		
		if (v == null)
			return "<null>";

		switch (v.type) {

		case DECIMAL:
			return ((BigDecimal) v.value).toString();
			
		case INTEGER:
			return Long.toString(v.getInteger());

		case STRING:
			if (quote)
				return "\"" + v.normalize() + "\"";
			return v.getString();

		case DOUBLE:
			String d_f = Double.toString(v.getDouble());

			// The default formatter for floating point values appends a ".0" to
			// whole numbers. Let's strip that off if it's the only thing at the
			// end of the formatted string for clean display.

			if (d_f.endsWith(".0"))
				d_f = d_f.substring(0, d_f.length() - 2);
			return d_f;

		case BOOLEAN:
			return v.getBoolean()? "true" : "false";

		case RECORD:
			s = "{";
			int count = 0;
			ArrayList keyList = v.recordFieldNames();
			boolean isCatalog = (v.getElement(OpCATALOG.CATALOG_FLAG) != null);
			if( isCatalog ) {
				s = "CATALOG\n";
			}
			for (int ix = 0; ix < keyList.size();ix++) {

				final String recordMember = (String) keyList.get(ix);
				if( recordMember.startsWith("_"))
					continue;
				e = v.getRecord().get(recordMember);
				if( isCatalog && e instanceof RecordStreamValue )
					r = (RecordStreamValue) e;
				else
					r = null;
				
				count++;
				if (count > 1)
					s = s + ", ";
				else
					s = s + " ";

				if( isCatalog ) {
					if( count > 1 )
						s = s + "\n ";

					if( e.getType() != Value.TABLE) {
						s = s + " VALUE " + recordMember + "(" + Value.toString(e, true);
					}
					else {
						s = s + (( r != null && r.isDirty()) ? "*" : " " ) + "TABLE " + recordMember + "(";

						RecordStreamValue table = (RecordStreamValue) e;
						Vector<String> names = table.columnNames;
						Vector<Integer> types = table.columnTypes;
						for( int cx = 0; cx < names.size(); cx++ ){
							if( cx > 0 )
								s = s + ", ";
							s = s + names.get(cx) + " ";
							int colType = types.get(cx);
							String tempType;
							if( colType == Value.STRING)
								tempType = "CHAR";
							else
								tempType = Value.typeToName(colType);
							s = s + tempType;
						}
					}
					s = s + ")";
				}
				else
					s = s + recordMember + ": "
					+ Value.toString(e, true);
			}
			if( !isCatalog )
				s = s + " }";
			return s;

		case TABLE:
		case ARRAY:
			s = "[";

			final int size = v.getArray().size();
			int i;
			for (i = 1; i <= size; i++) {
				e = v.getElement(i);
				if (i > 1)
					s = s + ", ";
				else
					s = s + " ";

				s = s + Value.toString(e, true);
			}
			s = s + " ]";
			return s;
		}
		return "<null>";
	}

	/**
	 * Return a vector containing the record field names in
	 * alphabetical order.
	 * @return a ArrayList containing the list of keys
	 */
	public ArrayList<String> recordFieldNames() {
		TreeMap<String,String> keySet = new TreeMap<String,String>();
		Iterator i = getRecord().keySet().iterator();
		while( i.hasNext()) {
			String k = (String) i.next();
			keySet.put(k, k);
		}
		ArrayList<String> v = new ArrayList<String>();
		i = keySet.keySet().iterator();
		while( i.hasNext()) {
			String k = (String) i.next();
			v.add(k);
		}
		keySet = null;
		i = null;
		return v;
	}

	/**
	 * Format a Value for display. This uses the type of the object to determine
	 * a suitable textual representation of the value. For most types, it uses
	 * the default string formatter for the base type (double, integer, etc.).
	 * Note that formatting for display means in a debugger or diagnostic mode,
	 * such that strings are printed with quotes, etc. This is distinct from the
	 * format() method which formats values for printing, so no quotes are
	 * included on strings, etc. Additionally, in this mode, floating point
	 * values are formatted with the default formatter, which appends a ".0"
	 * even to whole integer values, so the value's type is implicit in the
	 * display format.
	 * 
	 * @return printable string representation of the item
	 */

	public String displayFormat() {
		String s;

		switch (type) {

		case DECIMAL:
			return ((BigDecimal)value).toString();
			
		case INTEGER:
			return Long.toString(getInteger());

		case STRING:
			return "\"" + this.normalize() + "\"";

		case DOUBLE:
			return Double.toString(getDouble());

		case BOOLEAN:
			return getBoolean() ? "true" : "false";

		case RECORD:
			String recordText = Value.toString(this, false);
			if (recordText.length() > 60)
				recordText = recordText.substring(0, 60) + "...}";
			int mc = memberCount();
			if (mc == 1)
				s = "";
			else
				s = "s";
			return  "record[" + mc + " member" + s + "] = "
			+ recordText;

		case TABLE:
		case ARRAY:
			String arrayText = Value.toString(this, false);
			if (arrayText.length() > 60)
				arrayText = arrayText.substring(0, 60) + "...}";

			if (getArray().size() == 1)
				s = "";
			else
				s = "s";
			return "array[" + getArray().size() + " value" + s + "] = "
			+ arrayText;
		}
		return "<null>";
	}

	/**
	 * If the current Value is a string, and has carriage control characters in
	 * escaped text encoding form ("\n", etc.) this routine converts them back
	 * to actual carriage control characters. So when a string expression is
	 * entered by the user with a string constant like "\t", this can be
	 * converted to an actual tab character.
	 * 
	 * @return A string with the escaped characters converted to actual format
	 *         characters like tab and newline. If the current value is not a
	 *         STRING type, returns a null.
	 */

	public String denormalize() {

		if (type != Value.STRING)
			return null;

		final String s = getString();
		if( s == null )
			return "";

		StringBuffer v = new StringBuffer();

		final int n = s.length();
		for (int i = 0; i < n; i++) {
			char ch = s.charAt(i);
			if (ch != '\\') {
				v.append(ch);
				continue;
			}

			ch = s.charAt(++i);

			if( ch == 'u') {
				if(i+4 >= n) {
					v.append('\\');
					v.append('u');
					continue;
				}
				String unicodeString = s.substring(i+1, i+5);
				try {
					int unicodeChar = (Integer.parseInt(unicodeString, 16));
					v.append((char)unicodeChar);
				} catch (NumberFormatException e) {
					v.append('\\');
					v.append('u');
					v.append(unicodeString);
				}
				i = i + 4;
			}
			else if (ch == 'n')
				v.append('\n');
			else if (ch == 't')
				v.append('\t');
			else if (ch == 'r')
				v.append('\r');
			else if (ch == '"')
				v.append('\"');
			else if (ch == 'q')
				v.append('\"');
			else
				v.append(ch);
		}

		return v.toString();

	}

	/**
	 * If the current Value object is a string, and that has carriage control
	 * characters embedded in the string, this routine converts them to escaped
	 * format. So when a string value has a tab character in the string, it is
	 * converted to an escaped representation of \t.
	 * 
	 * @return A string with control characters converted to escape character
	 *         representation. Returns null if the value is not a STRING.
	 */

	public String normalize() {

		if (type != Value.STRING)
			return null;
		if( value == null )
			return "";

		StringBuffer v = new StringBuffer();
		String sv = getString();

		final int n = sv.length();
		for (int i = 0; i < n; i++) {

			final char ch = sv.charAt(i);

			if (ch == '\\')
				v.append( "\\\\");

			else if (ch == '\n')
				v.append("\\n");

			else if (ch == '\t')
				v.append("\\t");

			else if (ch == '\r')
				v.append("\\r");

			else if (ch == '"')
				v.append("\\\"");

			else
				v.append(ch);
		}

		return v.toString();
	}

	/**
	 * Create a new object that contains a copy of the old object, but
	 * with a new data type.
	 * @param type the type of the new object
	 * @return a newly created Value with the contents of the current 
	 * object, coerced to a new data type.
	 * @throws JBasicException if the data cannot be coerced to the given
	 * data type.
	 */
	public Value newAsType( int type ) throws JBasicException {

		Value v = this.copy();
		v.coerce(type);
		return v;
	}


	/**
	 * Coerce a data element in place to a specific type. For example, before
	 * you can do math on an object, it is converted to a double floating value
	 * (the normalized math type).
	 * 
	 * @param requestedNewType
	 *            the type code to which we are converting the object
	 * @return boolean indicating if type coercion was necessary
	 * @throws JBasicException if the conversion is invalid (such as BOOLEAN
	 * to RECORD)
	 */

	public boolean coerce(int requestedNewType) throws JBasicException {

		int newType = requestedNewType;
		if (type == newType)
			return false;
		
		switch (newType) {

		case RECORD:
			throw new JBasicException(Status.INVCVT, "RECORD");
		case ARRAY:

			final Value v = this.copy();

			value = new ArrayList<Value>();
			getArray().add(0, v);
			type = ARRAY;
			break;

		case FORMATTED_STRING:
			String formatBuffer;
			if( this.type == Value.TABLE )
				formatBuffer = this.toString();
			else
			formatBuffer = Value.toString(this, false);
			type = STRING;
			value = formatBuffer;
			newType = type; /* Have to change this so return works right */
			break;

		case DECIMAL:
			switch (type) {
			case BOOLEAN:
				value = new BigDecimal(getInteger() != 0 ? 1:0);
				break;
			case INTEGER:
				value = new BigDecimal(getInteger());
				break;
			case DOUBLE:
				value = BigDecimal.valueOf(getDouble());
				break;
			case STRING:
				value = new BigDecimal((String) value);
				break;
			default: 
				throw new JBasicException(Status.INVCVT, Value.typeToName(newType));
			}
			
			break;
			
		case BOOLEAN:

			switch (type) {
			case DECIMAL:
				setBoolean(getDecimal().intValue() != 0);
				break;
			case DOUBLE:
				setBoolean(getDouble() != 0.0);
				break;

			case INTEGER:
				setBoolean(getInteger() != 0);
				break;

			case STRING:
				int i;
				String sv = getString();
				if (sv.equalsIgnoreCase("TRUE"))
					i = 1;
				else if (sv.equalsIgnoreCase("T"))
					i = 1;
				else if (sv.equalsIgnoreCase("YES"))
					i = 1;
				else if (sv.equalsIgnoreCase("Y"))
					i = 1;
				else
					i = 0;
				setInteger(i);
				break;
			default: throw new JBasicException(Status.INVCVT, Value.typeToName(newType));
			}
			break;

		case INTEGER:
			switch (type) {
			case DECIMAL:
				setInteger(getDecimal().intValue());
				break;
			case BOOLEAN:
				setInteger(getBoolean() ? 1 : 0);
				break;
			case DOUBLE:
				setInteger((int) getDouble());
				break;
			case STRING:
				try {
					setInteger(Integer.parseInt(getString()));
				} catch (final Exception e) {
					setInteger(0);
				}
				break;
			default: throw new JBasicException(Status.INVCVT, Value.typeToName(newType));
			}
			break;

		case DOUBLE:
			switch (type) {
			case DECIMAL:
				setDouble(getDecimal().doubleValue());
				break;

			case BOOLEAN:
				setDouble( getBoolean()? 1.0 : 0.0 );
				break;
			case INTEGER:
				setDouble(getInteger());
				break;
			case STRING:
				try {
					setDouble(Double.parseDouble(getString()));
				} catch (final NumberFormatException e) {
					setDouble(Double.NaN);
				}
				break;
			default: throw new JBasicException(Status.INVCVT, Value.typeToName(newType));
			}
			break;

			/*
			 * Normalized string has special meaning for a string, otherwise, treat it just like a STRING
			 * by falling through...
			 */
		case NORMALIZED_STRING:
			if( type == STRING) {
				newType = STRING;
				value = denormalize();
				break;
			}
			/* Fall through to conventional string processing */
			
		case STRING:
			if (type == FORMATTED_STRING || type == NORMALIZED_STRING) {
				newType = STRING;
				break;
			}
			
			String s;
			switch (type) {
			case DECIMAL:
				value = ((BigDecimal)value).toString();
				break;
			case BOOLEAN:
				value = getBoolean()? "true" : "false";
				break;
			case INTEGER:
				value = Integer.toString(getInteger());
				break;
			case DOUBLE:

				String d_f = Double.toString(getDouble());

				// The default formatter for floating point values appends a
				// ".0" to
				// whole numbers. Let's strip that off if it's the only thing at
				// the
				// end of the formatted string for clean display.

				if (d_f.endsWith(".0"))
					d_f = d_f.substring(0, d_f.length() - 2);
				value = d_f;
				break;
			case RECORD:
				s = "{";
				int count = 0;
				ArrayList keyList = recordFieldNames();

				for (int ix = 0; ix < keyList.size();ix++) {

					final String recordMember = (String) keyList.get(ix);
					if( recordMember.startsWith("_"))
						continue;
					Value e = getRecord().get(recordMember);
					count++;
					if (count > 1)
						s = s + ", ";
					else
						s = s + " ";

					s = s + recordMember.toUpperCase() + ": "
					+ Value.toString(e, true);
				}
				s = s + " }";
				value = s;
				break;

			case TABLE:
			case ARRAY:
				s = "[";

				final int size = getArray().size();
				int i;
				for (i = 1; i <= size; i++) {
					Value e = getElement(i);
					if (i > 1)
						s = s + ", ";
					else
						s = s + " ";

					s = s + Value.toString(e, true);
				}
				s = s + " ]";
				value = s;
				break;
			}
		}

		// Type has to be changed and we're done.  Clear away the extra string formatting types
		
		type = newType;
		if( type == NORMALIZED_STRING || type == FORMATTED_STRING || type == QUOTED_FORMATTED_STRING)
			type = STRING;
		return true;
	}

	/**
	 * Set the double value of the object
	 * @param d the double to store in the value
	 */
	public void setDouble(double d) {
		value = null;
		doubleValue = d;
	}

	/**
	 * Override the equals() function that compares an item.  This lets us build Set() objects
	 * containing values.
	 * @param v the test value to compare to the current object
	 * @return true if the current item matches the test value
	 */
	public boolean equals( Value v ) {
		int result = -1;
		
		try {
			result = compare(v);
		}
		catch( JBasicException e ) {
			result = -1;
		}
		return result == 0;
	}
	
	/**
	 * Compare the current value to another value to determine which is
	 * less than the other, or if they are equal.
	 * 
	 * @param value2
	 *            The value to compare the current value to.
	 * @return An integer indicating the result of the comparison. -1 if the
	 *         current value is less, 0 if they are equal, or 1 if the
	 *         value parameter is less.
	 * @throws JBasicException If the values have irreconcilable types and 
	 * cannot be compared
	 */
	public int compare(final Value value2) throws JBasicException {

		/*
		 * Based on an agreed-upon mutual type, do the right kind of compare.
		 */

		int t;
		t = Expression.bestType(this, value2);

		switch (t) {

		case DECIMAL:
			return getDecimal().compareTo(value2.getDecimal());
			
		case BOOLEAN:
			if (getBoolean() == value2.getBoolean())
				return 0;
			return getBoolean()? 1 : -1;

		case DOUBLE:
			if( Double.isNaN(this.getDouble()) && Double.isNaN(value2.getDouble()))
				return 0;
			if (this.getDouble() == value2.getDouble())
				return 0;
			if (this.getDouble() > value2.getDouble())
				return 1;
			return -1;

		case INTEGER:
			return (this.getInteger() - value2.getInteger());

		case STRING:
			return this.getString().compareTo(value2.getString());

		case ARRAY:
		case TABLE:
			
			final int len1 = this.size();
			final int len2 = value2.size();

			if (len1 < len2)
				return -1;
			if (len1 > len2)
				return 1;

			for (int idx = 1; idx <= len1; idx++) {
				final Value v1 = this.getElement(idx);
				final Value v2 = value2.getElement(idx);

				if( v1.getType() == Value.RECORD && v2.getType() == Value.RECORD) {
					if( !v1.match(v2))
						return 98;
					continue;
				}
				
				final int compare = v1.compare(v2);
				if (compare != 0)
					return compare;
			}
			return 0;
		}
		return 99;

	}

	/**
	 * Determine the size of the current Value's binary data representation in
	 * bytes. This is used to calculate record size and positions for BINARY
	 * files.
	 * 
	 * @return An integer indicating the size of the binary value, or zero if
	 *         the current value cannot be written to a BINARY file.
	 */
	public int sizeOf() {
		int size = 0;

		switch (type) {

		case Value.BOOLEAN:
			size = 1;
			break;

		case Value.INTEGER:
			size = 4;
			break;

		case Value.DOUBLE:
			size = 8;
			break;

		case Value.DECIMAL:
			size = ((BigDecimal)value).toString().length();
			break;
			
		case Value.STRING:
			size = getString().length() * 2;
			break;

		case Value.RECORD:
		case Value.OBJECT:
			final Iterator i = getRecord().values().iterator();
			while (i.hasNext()) {
				final Value v = (Value) i.next();
				size = size + v.sizeOf();
			}
			break;

		case Value.ARRAY:
			final int ix = size();
			for (int n = 1; n <= ix; n++)
				size = size + getElement(n).sizeOf();

		}
		return size;
	}

	/**
	 * Get an object attribute from the current Value.
	 * @param name The name of the attribute
	 * @return The value of the attribute, or null if the attribute
	 * was not found or this is not an OBJECT type of Value.
	 */
	public Value getObjectAttribute( String name ) {

		if( this.type != Value.RECORD)
			return null;

		Value objectData = this.getElement(Value.OBJECT_DATA);
		if( objectData == null)
			return null;

		if( objectData.getType() != Value.RECORD)
			return null;

		return objectData.getElement(name);
	}

	/**
	 * Set an object attribute. This also forces the current value
	 * to be an object type if it is not already one.
	 * @param name The name of the object value field
	 * @param d The value to store in the object value field
	 * @return True if the object field could be set, or false if the
	 * current Value is not a record in the first place.
	 */
	public boolean setObjectAttribute( String name, Value d) {

		if( this.type != Value.RECORD) 
			return false;	

		Value objectData = this.getElement(Value.OBJECT_DATA);
		if( objectData == null ) {
			objectData = new Value(Value.RECORD, null);
			this.setElement(objectData, Value.OBJECT_DATA);
		}
		objectData.setElement(d, name);
		return true;
	}

	/**
	 * Remove an object attribute. This also forces the current value
	 * to be an object type if it is not already one.
	 * @param name The name of the object value field to remove
	 * @return True if the object field could be removed, or false if the
	 * current Value is not a record in the first place.
	 */
	public boolean removeObjectAttribute( String name) {

		if( this.type != Value.RECORD) 
			return false;	

		Value objectData = this.getElement(Value.OBJECT_DATA);
		if( objectData == null )
			return false;

		return (objectData.getRecord().remove(name) != null);
	}

	/**
	 * Remove a named element from a Record variable.  
	 * @param name The name of the field to remove - this MUST be in uppercase.
	 * @throws JBasicException if this Value is not a record or there is
	 * no such member
	 */
	public void removeElement( String name ) throws JBasicException {
		if( this.type != Value.RECORD )
			throw new JBasicException(Status.NOTRECORD);
		if( isObject())
			throw new JBasicException(Status.INVOBJOP, "remove element");
		if( this.getRecord().remove(name) == null )
			throw new JBasicException(Status.NOSUCHMEMBER, name);
	}

	/**
	 * Return the number of visible members of the record.  This is
	 * different than size() which is the actual count.  Visible members
	 * are those that do not have a "_" as the first character of the
	 * member name.
	 * @return count of visible members of the record
	 */
	public int memberCount() {
		if( type != Value.RECORD)
			return 0;

		if( value == null )
			return 0;
		int count = 0;
		Iterator i = getRecord().keySet().iterator();
		while( i.hasNext()) {
			String key = (String) i.next();
			if( key.startsWith("_"))
				continue;
			count++;
		}
		return count;
	}

	/**
	 * Convert a type name into a numeric code that can identify the type
	 * to conversion routines or the ByteCode interpreter.
	 * @param name The type name, which <em>must</em> be in uppercase already.
	 * @return an integer with the type code, or Value.UNDEFINED if the name
	 * was not a valid type name.
	 */
	public static int nameToType( String name ) {

		if( name.equals("INTEGER"))
			return Value.INTEGER;

		if( name.equals("STRING"))
			return Value.STRING;

		if( name.equals("BOOLEAN"))
			return Value.BOOLEAN;

		if( name.equals("ARRAY"))
			return Value.ARRAY;

		if( name.equals("DOUBLE"))
			return Value.DOUBLE;

		if( name.equals("RECORD"))
			return Value.RECORD;
		
		if( name.equals("DECIMAL"))
			return Value.DECIMAL;
		
		if( name.equals("TABLE"))
			return Value.TABLE;

		return Value.UNDEFINED;
	}

	/**
	 * Given an integer type specification, return the name of that data type.
	 * @param theType One of Value.INTEGER, Value.STRING, etc.
	 * @return The text name of that data type specification.
	 */
	public static String typeToName(int theType) {
		String typeName = "UNDEFINED";
		switch (theType) {
		case Value.DECIMAL:
			typeName = "DECIMAL";
			break;
		case Value.BOOLEAN:
			typeName = "BOOLEAN";
			break;
		case Value.ARRAY:
			typeName = "ARRAY";
			break;
		case Value.DOUBLE:
			typeName = "DOUBLE";
			break;
		case Value.INTEGER:
			typeName = "INTEGER";
			break;
		case Value.OBJECT:
			typeName = "OBJECT";
			break;
		case Value.RECORD:
			typeName = "RECORD";
			break;
		case Value.STRING:
			typeName = "STRING";
			break;
		case Value.TABLE:
			typeName = "TABLE";
			break;
		}
		return typeName;
	}

	/**
	 * Given a source object, copy it's contents into the current
	 * object, replacing the current object's data characteristics.
	 * @param src the source object to copy into the current object
	 */
	public void set(Value src) {

		this.type = src.type;
		this.updated = true;
		this.value = src.value;
		this.integerValue = src.integerValue;
		this.doubleValue = src.doubleValue;
	}


	/**
	 * Return the embedded object for this Value.  This will always be null
	 * for values other than ObjectValues
	 * @return the object embedded in this object, or null if it is not a
	 * Java object wrapper.
	 */
	public Object getObject() {
		
		return value;
	}

	/**
	 * Set the current value string
	 * @param s the String to store in the value
	 */
	public void setString(String s) {
		value = s;
		type = Value.STRING;
	}

	/**
	 * Get the iterator for keys in a RECORD object.
	 * @return a generic Iterator.
	 */
	public Iterator getIterator() {
		if( type != Value.RECORD )
			return null;
		return getRecord().keySet().iterator();
	}

	
	/**
	 * Get a row from the table as an array of values.  The default
	 * getElement() call returns a record with the column names as
	 * the record members.  This call instead returns a simple array
	 * of the row with no metadata.  This is used by sort and join
	 * operations that already understand the table metadata.
	 * @param rowNumber a 1-based integer describing which row in the
	 * table to read
	 * @return a Value.ARRAY containing each of the elements in order.
	 */
	public Value getElementAsArray( int rowNumber ) {
		
		Value result = new Value(Value.ARRAY, null );
		Value row = getElement(rowNumber);
		
		result.addElement(row);
		return result;
		
	}

	/**
	 * Method that searches the array for a matching value and
	 * returns an indicator if it exists... used to create Sets.
	 * @param item the Value to search for in the current array.
	 * @return true if the current value is an array and the
	 * item is found in the array, otherwise false.
	 */
	public boolean contains(Value item) {
		if( type != Value.ARRAY)
			return false;
		for( int ix = 1; ix <= size(); ix++ ) 
			if( getElement(ix).match(item))
				return true;
		return false;
	}

	/**
	 *  Store a DECIMAL value in the value.
	 *  @param d The value to store
	 */
	public void setDecimal(BigDecimal d) {
		value = d;
	}

}
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.ArrayList;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * This class describes a special subclass of Value, used to represent actual
 * Java objects.  Many of the methods are not supported and throw an error
 * indicating such.  A Java object looks a lot like a RECORD to the calling
 * code in terms of what it can do, but the members are actually Field elements
 * accessed using Java Reflection.
 * 
 * @author Tom Cole
 * @version version 1.1 March, 2009 Added better parameter type checking on methods
 * 
 */
public class ObjectValue extends Value {

	private static final boolean AGGRESSIVE_TYPE_BINDING = false;

	private static boolean USE_FULL_METHOD_NAMES = false;

	private static boolean USE_OBJECT_TOSTRING = true;

	/**
	 * The name of the element if it is a variable.
	 * 
	 * 
	 */
	private String name;

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
		if (newName == null)
			name = null;
		else
			name = newName/* .toUpperCase() */;
	}

	/**
	 * Accessor function to fetch the data type of the given object.
	 * 
	 * @return An integer with the type code (Value.INTEGER, Value.STRING, etc.)
	 */
	public int getType() {
		return Value.RECORD;
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
		return testType == Value.RECORD;
	}

	/**
	 * Flag used to detect when a value has been modified.
	 */
	private boolean updated;

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
	 * The actual Java object that this Value represents.
	 */
	private Object theObject;

	/**
	 * The actual Java object's class, used frequently enough to save here.
	 */
	private Class theClass;

	/**
	 * This is a private object used to represent the OBJECT$DATA information
	 * that JBasic expects for object classes.  If the caller tries to access
	 * a member with a leading "_" character, it is resolved using this Value
	 */

	private Value objectInfo;

	/**
	 * Get the number of public fields available in the RECORD representation
	 * of this object.
	 * 
	 * @return An int with the number of elements in the object.
	 */
	public int size() {
		return fieldMap.size();
	}



	/**
	 * Get a field from the record in the current object.
	 * 
	 * @param name
	 *            A string describing the field name.
	 * @return A Value object that describes the field value.
	 * object was invalid (typically non-existant)
	 */
	public Value getElement(final String name) {

		/*
		 * If it is the "fake" object info we keep in the wrapper on behalf of the user, then pass
		 * that back now.
		 */
		if( name.equals(Value.OBJECT_DATA))
			return objectInfo.getElement(Value.OBJECT_DATA);

		/*
		 * Otherwise, let's try to get an actual field.
		 */
		Field f = getFieldByName(name);
		if( f == null )
			return null;

		int fieldType = getFieldType(f);

		try {

			switch(fieldType ) {

			case Value.INTEGER:
				return new Value(f.getInt(theObject));
			case Value.BOOLEAN:
				return new Value(f.getBoolean(theObject));
			case Value.STRING:
				return new Value((String) f.get(theObject));
			case Value.DOUBLE:
				return new Value(f.getDouble(theObject));
			case Value.ARRAY:
				return objectToValue(f.get(theObject));
			case Value.OBJECT:
				return new ObjectValue(f.get(theObject));

			default:			
				return null;
			}		
		} catch (IllegalArgumentException e) {
			return null;
		}
		catch (IllegalAccessException e) {
			return null;
		} catch (JBasicException e) {
			return null;
		}
	}

	/**
	 * Given a Field object, map it to a Value type.
	 * @param f
	 * @return
	 */
	private int getFieldType(Field f) {
		Class fieldType = f.getType();
		String typeName = fieldType.getName();

		if(fieldType.isArray())
			return Value.ARRAY;
		if( typeName.equals("int")) 
			return Value.INTEGER;
		else if(typeName.equals("boolean"))
			return Value.BOOLEAN;
		else if(typeName.equals("java.lang.String"))
			return Value.STRING;
		else if(typeName.equals("double"))
			return Value.DOUBLE;

		return Value.OBJECT;

	}


	/**
	 * Given a Field object, map it to a Value type.
	 * @param testObject
	 * @return
	 */
	private int getObjectType(Object testObject) {
		Class fieldType = testObject.getClass();

		if( fieldType == Boolean.class)
			return Value.BOOLEAN;
		if( fieldType == Integer.class)
			return Value.INTEGER;
		if( fieldType == Double.class)
			return Value.DOUBLE;

		if(fieldType.isArray())
			return Value.ARRAY;

		String typeName = fieldType.getName();

		if( typeName.equals("int")) 
			return Value.INTEGER;
		else if(typeName.equals("boolean"))
			return Value.BOOLEAN;
		else if(typeName.equals("java.lang.String"))
			return Value.STRING;
		else if(typeName.equals("double"))
			return Value.DOUBLE;

		return Value.OBJECT;

	}

	/**
	 * Given a member name, locate the associated field name, and return
	 * the Field descriptor for it.
	 * @param name
	 * @return
	 */
	private Field getFieldByName(final String name) {
		return fieldMap.get(name);
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
		if (this.fReadonly)
			return false;
		updated = true;
		v.setName(name);

		Field f = getFieldByName(name);
		if (f == null)
			return false;

		int type = getFieldType(f);

		try {
			switch (type) {
			case Value.INTEGER:
				f.setInt(theObject, v.getInteger());
				return true;

			case Value.DOUBLE:
				f.setDouble(theObject, v.getDouble());
				return true;

			case Value.BOOLEAN:
				f.setBoolean(theObject, v.getBoolean());
				return true;

			case Value.STRING:
				f.set(theObject, v.getString());
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
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
		return setElement(new Value(s), name);
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
		return setElement(new Value(i), name);
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
		return setElement( new Value(b), name);
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
	 * This list contains each field in the object that we are allowed to
	 * access, by name. The name is normalized to uppercase.
	 */
	private HashMap<String,Field> fieldMap;

	/**
	 * Create a new ObjectValue representing a given Java object.
	 * 
	 * @param newObject the object to be stored in the new value.
	 * @throws JBasicException an invalid field name for a native Java
	 * object was given, or a type could not be bound to a native Java
	 * object class
	 */
	public ObjectValue(Object newObject) throws JBasicException {

		/*
		 * Explicitly invoke the superclass constructor
		 */
		super(Value.OBJECT, null);

		/*
		 * Store away the direct info about this object.
		 */

		theObject = newObject;
		theClass = theObject.getClass();

		updated = true;

		objectInfo = new Value(Value.RECORD, null);
		objectInfo.setObjectAttribute("CLASS", new Value(theClass.getName()));
		objectInfo.setObjectAttribute("ISCLASS", new Value(false));
		objectInfo.setObjectAttribute("ISJAVA", new Value(true));
		objectInfo.setObjectAttribute("ID", new Value(JBasic.getUniqueID()));

		/*
		 * Build the list of fields by name for this object.
		 */

		fieldMap = new HashMap<String,Field>();
		if (newObject != null) {
			Field[] fieldList = theClass.getFields();
			for (Field nextField : fieldList) {

				int mods = nextField.getModifiers();
				String fieldName = nextField.getName().toUpperCase();

				if( (mods & Modifier.STATIC) != 0 ) {
					fieldName = "_" + fieldName;
				}
				Field f = fieldMap.get(fieldName);
				if (f == null) {
					fieldMap.put(fieldName, nextField);
				} else {
					JBasic.log.info("Warning: ambiguous field name "
							+ fieldName + " ignored in Java object wrapper");
					throw new JBasicException(Status.INVOBJFLD, fieldName);
				}
			}
		}

	}

	/**
	 * Return a pointer to the object stored in this Value.
	 * @return the object contained in this value.
	 */
	public Object getObject() {
		return theObject;
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
	public boolean match(final ObjectValue item) {

		if( item.getType() != Value.RECORD)
			return false;

		return item.getObject() == this.theObject;
	}


	/**
	 * Get the current value expressed as a string using default formatting.
	 * 
	 * @return A string containing the value expressed as a string.
	 */
	public String getString() {
		return toString();

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

		Value v = this.getElement(name);
		if( v == null )
			return null;
		return v.getString();
	}

	/**
	 * Create a new object that contains a copy of the old object, but
	 * with a new data type.  The new data type can be a STRING type
	 * in which case the formatted value is returned.  Otherwise it must
	 * be a RECORD or an error is thrown.
	 * @param type the type of the new object
	 * @return a newly created Value with the contents of the current 
	 * object, coerced to a new data type.
	 * @throws JBasicException an INVOBJOP is thrown if the type to
	 * create is a numeric scalar. 
	 */
	public Value newAsType( int type ) throws JBasicException {
		if( type == Value.STRING || type == Value.FORMATTED_STRING)
			return new Value(this.toString());

		if( type == Value.RECORD )
			return this.copy();

		throw new JBasicException(Status.INVOBJOP, "coerce");

	}

	/**
	 * Coerce the current (java object) value into another type.
	 * The only value allowed is a coercion to a RECORD since
	 * that's already what the underlying data looks like.  Any
	 * attempt to convert this object to another type results in
	 * an error
	 * 
	 * @param type the Value type to convert to.
	 * @return FALSE if no conversion is required (i.e. it's already
	 * a record.
	 * @throws JBasicException An INVOBJOP is thrown for coercions
	 * to any type other than Value.RECORD.
	 */
	public boolean coerce(int type ) throws JBasicException {
		if( type == Value.RECORD)
			return false;

		throw new JBasicException(Status.INVOBJOP, "coerce");
	}

	/**
	 * Flag to indicate if the current object is a wrapper for
	 * a Java object
	 * @return true because this type is always a wrapper for a 
	 * java object.
	 */
	public boolean isObject() {
		return true;
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

		if( USE_OBJECT_TOSTRING) {
			return theObject.toString();
		}
		
		
		int t = this.getType();
		if( t == Value.UNDEFINED)
			return "<undefined>";

		if( theObject == null )
			return "<null>";

		StringBuffer s = new StringBuffer();

		s.append('{');
		Value e = null;

		int count = 0;
		ArrayList keyList = recordFieldNames();
		if( keyList == null )
			return "<none>";

		for (int ix = 0; ix < keyList.size(); ix++) {

			final String recordMember = (String) keyList.get(ix);
			if (recordMember.startsWith("_"))
				continue;
			e = getElement(recordMember);
			if( e == null )
				continue;

			count++;
			if (count > 1)
				s.append(", ");
			else
				s.append(" ");

			String valueString = null;
			if( e.getClass() == ObjectValue.class) {
				Object o = ((ObjectValue)e).getObject();

				valueString = ( o == null ) ? "<null>" : "OBJECT(" + o.getClass().getName() + ")";
			}
			else
				valueString = Value.toString(e, true);

			s.append(recordMember.toUpperCase());
			s.append( ": ");
			s.append(valueString);
		}
		s.append(" }");
		return s.toString();

	}

	/**
	 * Return a vector containing the record field names in
	 * alphabetical order.
	 * @return a ArrayList containing the list of String values
	 */
	public ArrayList<String> recordFieldNames() {

		/*
		 * Normally the names are kept in a HashMap which is far faster to
		 * manipulate than a TreeMap because it has no sort order requirements.
		 * But for this method, we need to return the names in sorted order.
		 * So I'm copying them into a TreeMap
		 */
		TreeMap<String,Field> fieldNameList = new TreeMap<String,Field>();

		if( theObject == null )
			return null;

		fieldNameList.putAll(fieldMap);
		fieldNameList.put(Value.OBJECT_DATA, null);

		/*
		 * Now pull them out of the tree in sorted order to put into the
		 * vector we return to the caller.  The part we care about is the
		 * keySet so just add that collection to the vector.
		 */
		ArrayList<String> v = new ArrayList<String>();
		v.addAll(fieldNameList.keySet());

		/*
		 * Free the storage for the TreeMap to speed garbage collection 
		 * and we're done.
		 */
		fieldNameList = null;
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

		String recordText = theObject.toString();
		if (recordText.length() > 60)
			recordText = recordText.substring(0, 60) + "...}";
		return "object[" + theClass.toString() + "] = "
		+ recordText;
	}



	/**
	 * Determine the size of the current Value's binary data representation in
	 * bytes. This is used to calculate record size and positions for BINARY
	 * files.
	 * 
	 * @return An integer indicating the size of the binary value, or zero if
	 *         the current value cannot be written to a BINARY file.  Because
	 *         this object is a wrapper for a native Java object, the size is
	 *         always zero.
	 */
	public int sizeOf() {
		return 0;
	}


	/**
	 * Make a copy of this object.  Required to support certain stack operations
	 * and the newAsType() method.
	 * @return a new instance of a Value wrapped around the same native Java object
	 * already contained in this wrapper. That is, the wrapper is copied but the
	 * underlying Java value is not.
	 */
	public ObjectValue copy() {
		try {
			return new ObjectValue(theObject);
		} catch (JBasicException e) {
			e.printStackTrace();
			return null;
		}
	}

	public ObjectValue copy(boolean b, int i ) {
		return copy();
	}

	/**
	 * Return the number of visible members of the record.  This is
	 * different than size() which is the actual count.  Visible members
	 * are those that do not have a "_" as the first character of the
	 * member name.
	 * @return count of visible members of the record
	 */
	public int memberCount() {
		return this.size();
	}

	/**
	 * Accessor function to get the class of the native Java object
	 * contained in this Value wrapper.
	 * @return a Class identifying the embedded object.
	 */
	public Class getObjectClass() {
		return theClass;
	}

	/**
	 * Given an argument list from a function-style invocation in JBasic,
	 * construct a valid method invocation of the current object.  The
	 * result is returned to the caller as a Value.
	 * @param theMethodName a string containing the method name to invoke
	 * @param argList the function-style argument list to pass to the method
	 * @return a Value that is the result (if any) of the method, or null if
	 * no return value is expected from the method.
	 * @throws JBasicException if the object is not known, the method
	 * parameters cannot be coerced to the right types, and for errors 
	 * generated by the underlying method as needed.
	 */
	@SuppressWarnings("unchecked") 
	public Value invokeMethod(String theMethodName, ArgumentList argList) throws JBasicException {

		/*
		 * Build a param list of class information based on the types of data given to 
		 * us in the argument list.
		 */
		int arraySize = argList.size();
		Class params[] = new Class[arraySize];
		Object args[] = new Object[arraySize];
		Boolean isObjectParm[] = new Boolean[arraySize];

		for( int ix = 0; ix < argList.size(); ix++ ) {
			Value v = argList.element(ix);
			Class argClass = v.getObjectClass();
			if( argClass == null ) 
				throw new JBasicException(Status.INVOBJOP, "use " + v.toString() + " with");
			params[ix] = argClass;

			if( v.isObject()) {
				args[ix] = ((ObjectValue)v).getObject();
				isObjectParm[ix] = true;
			}
			else {
				isObjectParm[ix] = false;
				switch( v.getType()) {

				case Value.INTEGER:
					args[ix] = new Integer(v.getInteger());
					break;
				case Value.DOUBLE:
					args[ix] = new Double(v.getDouble());
					break;
				case Value.BOOLEAN:
					args[ix] = new Boolean(v.getBoolean());
					break;
				case Value.STRING:
					isObjectParm[ix] = true;
					args[ix] = v.getString();
					break;
				case Value.RECORD:
					isObjectParm[ix] = true;
					args[ix] = v.getObject();
				}
			}
		}

		String javaMethodName = findMethod(theObject, theMethodName, params);
		if( javaMethodName == null )
			throw new JBasicException(Status.INVOBJMETH, methodSignature( theMethodName, params));


		/*
		 * First we try using the exact parameters give.  If that fails, try
		 * again converting the parameter signature entries of any objects
		 * to a general Java "Object" class.
		 */

		Method javaMethod = null;

		try {
			javaMethod = theClass.getMethod(javaMethodName, params);
		} catch (Exception e ) {
			for( int ix = 0; ix < arraySize; ix++ ) {
				if( isObjectParm[ix])
					params[ix] = Object.class;
			}
		}

		try {

			if( javaMethod == null )
				javaMethod = theClass.getMethod(javaMethodName, params);
			Object result = javaMethod.invoke(theObject, args);

			if( result == null )
				return null;

			Class returnType = javaMethod.getReturnType();

			/*
			 * If the expected return type is some type other than 
			 * what came back, cast it now.  If it is a primitive 
			 * type, do no cast.
			 */

			if( getObjectType(result) == Value.OBJECT)
				result = returnType.cast(result);
			return objectToValue(result);

		} catch( InvocationTargetException e ) {
			throw new JBasicException(Status.OBJEXCEPT, e.getTargetException().toString());	
		} catch (SecurityException e) {
			throw new JBasicException(Status.INVOBJOP, e.toString());	
		} catch (NoSuchMethodException e) {
			throw new JBasicException(Status.INVOBJMETH, e.getMessage());	
		} catch (IllegalArgumentException e) {
			throw new JBasicException(Status.INVOBJOP, "pass illegal argument type or value");	
		} catch (IllegalAccessException e) {
			throw new JBasicException(Status.INVOBJOP, e.toString());	

		}

	}

	/**
	 * Given a JBasic spelling of a method name and an array of class types, formulate a
	 * string representation that indicates the type of each parameter in the list.  This
	 * is used for reporting on errors in method calls from the JBasic code.
	 * @param theMethodName The method name, typically in upper-case
	 * @param params An array of class types that indicates what each parameter to the
	 * method is in the invoking JBasic code.
	 * @return the diagnostic message text
	 */
	public static String methodSignature(String theMethodName, Class[] params) {

		StringBuffer result = new StringBuffer(theMethodName);
		result.append("(");
		for( int ix = 0; ix < params.length; ix++ ) {
			if( ix > 0 )
				result.append(", ");
			if( params[ix] == null )
				result.append("null");
			else
				result.append(params[ix].toString());
		}
		result.append(")");
		return result.toString();
	}

	/**
	 * Given a Java object, convert it to a suitable JBasic Value.  If the
	 * object cannot be converted to a known JBasic type, return a new
	 * Java wrapper Value.
	 * @param theObject the Java object to be converted to a Value
	 * @return a Value that is either a native JBasic type or a wrapper object.
	 */
	private Value objectToValue(Object theObject) throws JBasicException {

		Class objectClass = theObject.getClass();
		Value returnValue = null;

		if( objectClass == Value.class)
			return (Value)theObject;
		
		if( objectClass.isArray()) {
			returnValue = new Value(Value.ARRAY, null);

			Object[] objectArray = (Object[]) theObject;
			for( Object obj : objectArray)
				returnValue.addElement(objectToValue(obj));
			return returnValue;
		}

		if( AGGRESSIVE_TYPE_BINDING ) {

			if( theObject instanceof Set) {
				returnValue = new Value(Value.ARRAY, null);
				for( Object o : (Set) theObject) {
					returnValue.addElement( objectToValue(o));
				}
				return returnValue;
			}
			if( theObject instanceof java.util.Collection) {
				returnValue = new Value(Value.ARRAY, null);
				Collection v = (Collection) theObject;
				for( Object member : v)
					returnValue.addElement(objectToValue(member));
						return returnValue;
			}
		}
		
		if( theObject instanceof java.util.Vector) {
			returnValue = new Value(Value.ARRAY, null);
			ArrayList v = (ArrayList) theObject;
			for( int ix = 0; ix < v.size(); ix++ )
				returnValue.addElement(objectToValue(v.get(ix)));
			return returnValue;
		}

		/*
		 * It <might> be a RECORD, but we need to check to see.
		 */
		boolean fail = false;
		if( objectClass == java.util.HashMap.class) {
			HashMap x = (HashMap) theObject;
			
			for( Object key : x.keySet()) {
				if(! (key instanceof String)) {
					fail = true;
					break;
				}
				Object value = x.get(key);
				if( value instanceof Value )
					continue;
				int t = getObjectType(value);
				if( t == Value.UNDEFINED || t== Value.OBJECT) {
					fail = true;
					break;
				}
			}
			if( !fail ) {
				Value newValue = new Value(Value.RECORD, null);
				newValue.value = theObject;
				return newValue;
			}
		}
		
		if( objectClass == Integer.class)
			return new Value(((Integer)theObject).intValue());

		if( objectClass == Boolean.class)
			return new Value(((Boolean)theObject).booleanValue());

		if( objectClass == Double.class)
			return new Value(((Double)theObject).doubleValue());

		if( objectClass == String.class)
			return new Value((String)theObject);

		return new ObjectValue(theObject);
	}

	/**
	 * Method names are passed to us in all-upper-case text, because JBasic
	 * normalizes all identifiers that way. So we're going to have to try to
	 * figure out what the method name is really supposed to be, by comparing
	 * it to the actual method names.  If we find one (and only one) method
	 * that matches in a case-insensitive compare, then we're golden. Otherwise
	 * we have to throw an error because we can't unambiguously detect the name
	 * if they are case-mangled such as theFIELDname and theFieldName which Java
	 * thinks are two names but we would see as only one.
	 * @param anObject a Java object we wish to locate a method for
	 * @param theMethodName The name of the method
	 * @param params an array of class designations describing each parameter
	 * @return string containing the matching method name, or null if no such method.
	 */
	@SuppressWarnings("unchecked") 
	private String findMethod(Object anObject, String theMethodName, Class[] params)  {

		Method[] methodList = anObject.getClass().getMethods();

		for( Method method : methodList ) {
			String testName = method.getName();
			if( testName.equalsIgnoreCase(theMethodName)) {

				/*
				 * Now we've got to see if the signature for this method matches our
				 * parameter list well enough to be used.
				 */

				Class[] testParams = method.getParameterTypes();
				if( testParams.length != params.length)
					continue;

				boolean matchingParameters = true;
				for( int i = 0; i < testParams.length; i++ ) {
					if( !testParams[i].isAssignableFrom(params[i])) {
						matchingParameters = false;
						break;
					}
				}

				/*
				 * If didn't make it to the end of the list with parameter class
				 * matches, this method isn't the one we want.
				 */
				if( !matchingParameters)
					continue;

				/*
				 * Good match. If we don't coerce full method names, then we're done.
				 */
				if( !USE_FULL_METHOD_NAMES ) 
					return testName;

				/*
				 * No, get the fully qualified method name string and parse out the
				 * actually full method path from that string - the last non-blank
				 * token before the opening parenthesis.
				 */

				String fullName = method.toString();
				int k = fullName.indexOf(' ');
				while( k > 0) {
					fullName = fullName.substring(k+1);
					k = fullName.indexOf(' ');
				}
				int j = fullName.indexOf('(');
				fullName = fullName.substring(0,j);
				return fullName;
			}
		}
		return null;
	}

	/**
	 * Given a partial class name and a pointer to the local symbol table, search
	 * for the fully-qualified class name.  First, see if the name is already a 
	 * complete class name; if so return it.  Otherwise, use the SYS$PACKAGES list
	 * to see if additional package qualifications added to the class name will
	 * yield a valid class name.
	 * @param className the full or partial name of the class
	 * @param symbols a symbol table where the SYS$PACKAGES array can be found.
	 * @return the fully qualified class name.
	 */
	public static String fullClassName(String className, SymbolTable symbols) {

		Value packageList = symbols.findReference(JBasic.PACKAGES, false);
		if( packageList == null )
			return className;
		if( packageList.getType() != Value.ARRAY)
			return className;

		String localName = className;
		boolean found = false;
		int index = 0;
		while( !found ) {

			try {
				Class.forName(localName);
				found = true;
			} catch (Exception e ) {
				found = false;
			}
			if( found )
				return localName;
			if( index > packageList.size())
				return null;
			
			localName = packageList.getString(++index) + "." + className;
		}
		return null;
	}
}
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
import java.util.Iterator;
import java.util.Vector;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.Utility;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.statements.SortStatement;


/**
 * This class describes a special subclass of Value, used to represent streams
 * of data.  
 * <p>
 * Currently a stream is an array of records.  However, this will ultimately be
 * changed to be an array of values with an associate key map, where each value
 * is an array for the elements of each row. At that point, the constructor will
 * need to validate the incoming array more clearly and also we'll need a convert
 * routine that puts the data back to an array-of-records as needed.
 * @author Tom Cole
 * @version version 1.1 March, 2009 Added better parameter type checking on methods
 * 
 */
public class RecordStreamValue extends Value {

	private static final boolean USE_FULL_FORMATTING = true;
	Vector<String> columnNames;
	Vector<Integer>	columnTypes;
	
	/**
	 * This flag indicates if the record stream value is dirty.  This
	 * is public at the moment so it can be set from generated code
	 * on a SAVE operation, and also after a successful LOAD.  This 
	 * should be changed to an accessor function at some point.
	 */
	private boolean dirty;
	
	/**
	 * Create a RecordStream Value object.  The parameter, if present, must
	 * be a Value.ARRAY containing the names and types of the columns to be
	 * created.
	 * @param d A value with the metadata.  The minimum data must be the
	 * column names in uppercase.  They can optionally be followed by "@" and
	 * the data type.  For example, AGE@INTEGER or NAME@STRING
	 */
	public RecordStreamValue(Value d ) {
		super(Value.ARRAY, null);
		type = Value.TABLE;
		dirty = true;
		columnNames = new Vector<String>();
		columnTypes = new Vector<Integer>();
		if( d == null )
			return;
		
		if( d.isType(Value.ARRAY)) {
			
			/*
			 * Test the first row to see if it is a list of strings,
			 * in which case this is a TABLE type.
			 */
			
			boolean isTable = true;
			for( int idx = 1; idx<= d.size(); idx++ ) {
				if( !d.getElement(idx).isType(Value.STRING)) {
					isTable = false;
					break;
				}
			}

			/*
			 * If it's a valid table, then copy the first row to the
			 * column name list, and add each additional row to the
			 * local data item.
			 */
			if( isTable ) {
				for( int idx = 1; idx<= d.size(); idx++ ) {
					String name = d.getString(idx).toUpperCase();
					int atPos = name.indexOf('@');
					int theType = Value.UNDEFINED;
					if( atPos > 0) {
						String type = name.substring(atPos+1);
						name = name.substring(0,atPos);
						theType = Value.nameToType(type);
					}
					/*
					 * Do we already have this?  If so, we update the
					 * one in place rather than adding a new one.
					 */
					
					boolean duplicate = false;
					for( int n = 0; n < columnNames.size(); n++ ) {
						if( columnNames.get(n).equals(name)) {
							columnTypes.set(n, theType);
							duplicate = true;
							break;
						}
					}
					
					if( !duplicate) {
						columnNames.add(name);
						columnTypes.add(theType);
					}
				}
			}
			
			return;
			
		}
	}
	
	/**
	 * Given a column name, return which column number it represents in the
	 * current RecordStream.
	 * 
	 * @param columnName the name of the column to look up.  Must already
	 * be UPPERCASE.
	 * @return the 1-based column number, or zero if no such column was found.
	 */
	public int getColumnNumber( String columnName ) {
		int columnNumber = 0;
		
		for( columnNumber = 1; columnNumber <= columnNames.size(); columnNumber++) {
			String key = columnNames.get(columnNumber-1);
			if( key.equals(columnName))
				return columnNumber;
		}
		return 0;
	}
	
	/**
	 * Get a single element from a TABLE, given the row number and column name.
	 * @param rowNumber The 1-based row number in the table.
	 * @param column the column name, which must already be UPPERCASE.
	 * @return the Value at that point in the table, or null of the
	 * row or name were invalid.
	 */
	public Value getElement( int rowNumber, String column ) {
		
		int columnNumber = getColumnNumber(column);
		Value row = getElement(rowNumber);
		if( row != null )
			return row.getElement(columnNumber);
		return null;
	}
	
	/**
	 * Get a single row from a TABLE, given the row number.  This supercedes
	 * the getElement() call in the Value class which would return a record
	 * when the table is an array of records - since a RecordStream looks like
	 * that to the outside world, a row is a RECORD.
	 * @param rowNumber The 1-based row number in the table.
	 * @return a Value containing a RECORD with all the elements
	 * of the row, tagged by their name.  If the row number is
	 * invalid, a null is returned.
	 */
	public Value getElement( int rowNumber ) {
		
		Value row = getArray().get(rowNumber-1);
		if( row == null )
			return null;

		Value r = new Value(Value.RECORD, null);
		for( int idx = 0; idx < columnNames.size(); idx++ ) {
			String key = columnNames.get(idx);
			Value element = row.getElement(idx+1);
			r.setElement(element, key);
		}
		return r;
	}
	

	/**
	 * Sort the stream based on a key value in the array.
	 * @param key the member name to sort on.
	 * @return a Status indicating if the sort was successful.  Failures
	 * occur for empty arrays or for arrays without the required key value.
	 */
	public Status sort(String key ) {
		int columnNumber = getColumnNumber(key);
		if( columnNumber < 1 )
			return new Status(Status.NOSUCHMEMBER, key);
		
		return SortStatement.sortArray(this, columnNumber);
	}
	/**
	 * Given two array locations, return a new Value.RECORD that is a merger
	 * of all members in both records in the contributing arrays.
	 * @param leftArray The first array to use as the base of the merge
	 * @param leftIndex The position in the left array to get the record
	 * @param rightArray The second array which is merged with the base
	 * @param rightIndex the position in the right array to get the record
	 * @return a Value.RECORD containing all fields from both contributing
	 * records.  If the field names match the right element overrides the
	 * value of the left element.
	 */
	@SuppressWarnings("unchecked") 
	public static Value streamMerge(Value leftArray, int leftIndex, Value rightArray,
			int rightIndex) {
		
		final Value left = leftArray.getElement(leftIndex);
		final Value right = rightArray.getElement(rightIndex);
		
		Value result = left.copy();
		
		Iterator<String> i = right.getIterator();
		while( i.hasNext()) {
			String key = i.next();
			result.setElement(right.getElement(key), key);
		}
		return result;
	}

	/**
	 * Given a starting position, find the record whose
	 * member (identified by a string key) matches an arbitrary value.
	 * @param position the starting position to being the search.
	 * @param key the name of the record member in each array element to check
	 * @param match the value to test for
	 * @return the position in the array where the match was found.  If no
	 * match is found, returns zero.
	 */
	public int streamFind(int position, String key, Value match) {
		
		int count = this.size();
		if( position > count )
			return 0;
		
		if( position < 1)
			position = 1;
		
		for( int idx = position; idx <= count; idx++ ) {
			Value element = this.getElement(idx);
			Value member = element.getElement(key);
			if( member.match(match))
				return idx;
		}
		return 0;
	}

	/**
	 * Validate that the stream is valid.  Currently this
	 * involves checking each element to see if it contains
	 * the key.
	 * @param key the member name that must be in each record
	 * @throws JBasicException the expected member was not found
	 */
	 public void streamValidate(String key) throws JBasicException {
				
		if( getColumnNumber(key) < 1 )
			throw new JBasicException(Status.EXPMEMBER, key);
		
	}
	 
	 public Value getElementAsArray(int i) {
		 return super.getElement(i);

	 }
	
	 /**
	  * IF you attempt to write a record to the RecordStream, we convert it
	  * to the underlying array row format, using the column names to pull
	  * the values from the row we need.
	  * <p>
	  * It is an error to write anything other than an ARRAY or RECORD
	  * to the table.
	  * @param d the value (an ARRAY or a RECORD) to be written to the stream.
	  * @param rowNumber the row to write the data.
	  */
	 public void setElement(Value d, int rowNumber) {
		 
		/*
		 * If it's an array, make sure each column is of the
		 * correct type.  If the array is not big enough to
		 * have all the columns, synthesize default values
		 * for the missing columns.  Extra columns are ignored.
		 */
		 
		 if( d.isType(Value.ARRAY)) {
			 for( int idx = 0; idx < columnNames.size(); idx++ ) {
				 try {
					 if( idx >= d.size())
						 d.addElement(new Value(columnTypes.get(idx), null));
					 else
						 d.getElement(idx+1).coerce(columnTypes.get(idx));
				} catch (JBasicException e) {
					e.printStackTrace();
				}
			 }
			 
			 /*
			  * Before we use the regular array insert, we need to be
			  * sure that we don't need to fill out the array.  The
			  * default setElement() will insert zeroes in unused array
			  * slots, but we need the unused slots to hold row arrays.
			  */
			 
			 if( rowNumber > size()+1) {
				 /*
				  * Construct a dummy row array
				  */
				 
				 Value row = new Value(Value.ARRAY, null);
				 for( int c = 0; c < columnNames.size(); c++ ) {
					 row.addElement(new Value(columnTypes.get(c), null));
				 }
				 
				 /*
				  * Insert as many copies as needed to pad out the
				  * table.  For the last one, just use the row we
				  * created above.
				  */
				 
				 for( int r = size()+1; r < rowNumber-1; r++ )
					 super.setElement(row.copy(), r);
				 super.setElement(row, rowNumber-1);
			 }
			 /*
			  * Now we can just insert the array
			  */
			 super.setElement(d,rowNumber);
		 }
		 else if( d.isType(Value.RECORD)) {
			 Value row = new Value(Value.ARRAY, null);
			 for( int idx = 0; idx < columnNames.size(); idx++) {
				 String key = columnNames.get(idx);
				 Value element = d.getElement(key);
				 if( element != null ) {
					 try {
						element.coerce(columnTypes.get(idx));
					} catch (JBasicException e) {
						JBasic.log.error("Fatal setElement error in a TABLE");
					}
					 row.addElementAsIs(element);
				 }
				 else
					 row.addElementAsIs(new Value(0));
			 }
			 super.setElement(row,rowNumber);
		 }
	 }
	
	/**
	 * Create a copy of the referenced object. This creates a new object and
	 * moves the contents to the new object, including copying any string value.
	 * 
	 * @return a new RecordStreamValue that is a copy of the referenced object.
	 */

	public Value copy() {

		RecordStreamValue dest = new RecordStreamValue(null);
		dest.updated = true;
		dest.fSymbol = false;
		dest.dirty = dirty;
		dest.name = name;
		dest.value = new ArrayList<Value>();
		dest.columnNames = columnNames;
		dest.columnTypes = columnTypes;
		
		if (value != null) {
			final int len = getArray().size();
			if (len > 0)
				for (int i = 1; i <= len; i++) {
					final Value item = getElement(i);
					if (item != null)
						dest.setElement(item, i);
				}
		}

		return dest;
	}
	/**
	 * Perform a "shallow" array concatenation.  That is, copy each
	 * element of the array as-is to the new array, and do not recursively
	 * copy elements to flatten out the array.
	 * @param value the value to add to the current array
	 */
	public int addElement(Value value) {

		if (value.isType(Value.ARRAY)) {
			
			if( rowSize() > 0 &&  value.size() != rowSize())
				return 0;
			getArray().add(value);
		} else if( value.isType(Value.RECORD)) {
			Value row = new Value(Value.ARRAY, null);
			for( int idx = 0; idx < columnNames.size(); idx++ ) {
				Value element = value.getElement(columnNames.get(idx)).copy();
				try {
					element.coerce(columnTypes.get(idx));
				} catch (JBasicException e) {
					e.printStackTrace();
				}
				row.addElement(element);
			}
			getArray().add(row);
			
		} else 
			return 0;
		
		return size();
	}

	/**
	 * Determine how wide the table width is.  This is the number of columns
	 * of the table.
	 * @return the number of elements in each row of the table, or zero if
	 * no metadata has been set up yet.
	 */
	public int rowSize() {
		if( columnNames == null )
			return 0;
		return columnNames.size();
	}
	
	public String toString() {

		StringBuffer result = new StringBuffer();

		if( USE_FULL_FORMATTING ) {

			int count = columnNames.size();
			int[] widths = new int[count];
			/*
			 * Start by figuring out the label widths
			 */
			for( int idx = 0; idx < count; idx++ ) 
				widths[idx] = columnNames.get(idx).length();
			
			/*
			 * Now see if any column data is wider and adjust the
			 * width accordingly.
			 */
			for( int idx = 1; idx <= getArray().size(); idx++ ) {
				Value row = getArray().get(idx-1);
				for( int col = 1; col <= row.size(); col++ ) {
					Value element = row.getElement(col);
					int w = element.getString().length();
					if( widths[col-1] < w)
						widths[col-1] = w;
				}
			}
			
			/*
			 * Put the column headers in the output buffer.
			 */
			
			for( int idx = 0; idx < count; idx++ ) {
				result.append(Utility.pad(columnNames.get(idx), widths[idx]));
				result.append(' ');
			}
			result.append('\n');
			for( int idx = 0; idx < count; idx++ ) {
				for( int c = 0; c < widths[idx]; c++ )
					result.append('-');
				result.append(' ');
			}
			result.append('\n');
			
			/*
			 * Put out each row.
			 */
			
			for( int idx = 1; idx <= getArray().size(); idx++ ) {
				Value row = getArray().get(idx-1);
				for( int col = 1; col <= row.size(); col++ ) {
					Value element = row.getElement(col);
					result.append(Utility.pad(element.getString(), widths[col-1]));
					result.append(' ');
				}
				if( idx < getArray().size())
					result.append('\n');
			}
			

		} else {
			result.append( dirty ? '*' : ' ');
			result.append("TABLE(");
			
			if( columnNames == null )
				result.append("undefined");
			else {
				for( int idx = 0; idx < columnNames.size(); idx++ ) {
					if( idx > 0 )
						result.append(", ");
					int type = columnTypes.get(idx);
					if( type != Value.UNDEFINED) {
						result.append(Value.typeToName(type));
						result.append(' ');
					}
					result.append(columnNames.get(idx));
				}
			}
			result.append( ") ");
			result.append(getArray().toString());
		}
		return result.toString();

	}
	

	/**
	 * Create a new object that contains a copy of the old object, but
	 * with a new data type.  For a RecordStreamValue, the only acceptable
	 * conversion is to a string.
	 * @param type the type of the new object
	 * @return a newly created Value with the contents of the current 
	 * object, coerced to a new data type.
	 * @throws JBasicException the type of argument is not a kind of string
	 */
	public Value newAsType( int type ) throws JBasicException {

		if( type == Value.STRING || type == Value.FORMATTED_STRING) {
			return new Value(this.toString());
		}
		throw new JBasicException(Status.INVCVT, Value.typeToName(type));
	}

	/**
	 * Return the column metadata as an array of strings.  This can be
	 * passed to a new RecordStreamValue constructor to make a matching
	 * table definition.
	 * @return a Value.ARRAY containing the names of the columns.
	 */
	public Value columnNames() {
		Value names = new Value(Value.ARRAY, null);
		
		for( int idx = 0; idx < columnNames.size(); idx++ ) {
			StringBuffer name = new StringBuffer(columnNames.get(idx));
			name.append('@');
			name.append(Value.typeToName(columnTypes.get(idx)));
			names.addElement(new Value(name.toString()));
		}
		return names;
	}

	/**
	 * Delete all the rows from a result set, but leave the metadata
	 * untouched.
	 */
	public void empty() {
		value = new ArrayList<Value>();
	}

	/**
	 * Add an existing table to the current table. The column
	 * metadata must exactly match.
	 * 
	 * @param sourceTable the reference to the input stream
	 * @return the size of the resulting table.  If -1 is returned,
	 * then it means the metadata didn't match.
	 */
	public int merge(RecordStreamValue sourceTable) {
		
		/*
		 * First, validate that every field in the target exists in the source
		 */
		
		for( int ix = 0; ix < columnNames.size(); ix++ ) {
			String name = columnNames.get(ix);
			if( sourceTable.getColumnNumber(name) != ix+1)
				return -1;
		}
		
		int rowNumber = size();
		
		for( int ix = 1; ix <= sourceTable.size(); ix++ ) {
			Value row = sourceTable.getElement(ix);
			this.setElement(row, ++rowNumber);
		}
		return rowNumber;
	}

	/**
	 * Set the dirty flag for a record stream indicating it needs to be 
	 * saved.
	 * @param f a boolean value
	 */
	public void dirty(boolean f) {
		dirty = f;
	}

	/**
	 * Determine if this table has been modified or not.
	 * @return true if the table has been modified since the
	 * last LOAD CATALOG or SAVE CATALOG operation.
	 */
	public boolean isDirty() {
		return dirty;
	}

}

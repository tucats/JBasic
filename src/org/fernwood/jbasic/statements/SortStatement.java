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
package org.fernwood.jbasic.statements;

import java.util.ArrayList;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Quicksort;
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * Sort statement. This accepts an arrayValue and sorts it. The arrayValue can
 * be identified by name, as in
 * 
 * SORT MYARRAY
 * 
 * or indirectly such as with a string variable containing the name, like
 * 
 * MYARRAYNAME = "MYARRAY" SORT USING(MYARRAYNAME)
 * 
 * The arrayValue is sorted in place, so contents of the arrayValue is changed
 * by this operation. You cannot sort a read-only arrayValue as a result of
 * this. The arrayValue cannot contain embedded arrays or programs, and all
 * members of the arrayValue must be either strings or numbers, but not both.
 * 
 * @author tom
 * @version 1.0 June 24, 2004
 */

public class SortStatement extends Statement {

	/**
	 * Sort an array of records. The Value array must contain only records, all
	 * of which must have the given member as a field. Additionally, the member
	 * field must contain only homogeneous scalar data types for all records in
	 * the array.
	 * 
	 * @param array
	 *            The Value containing an array of records.
	 * @param member
	 *            The name of the field in the record that will be used as a
	 *            sort key.
	 * @return Status indicating if the sort was successful. It can fail
	 *         because the value isn't a record, or not all records have the
	 *         named member, or the member field values are not all the same
	 *         scalar type.
	 */
	public static Status sortArray(final Value array, final String member) {

		if (array == null)
			return new Status(Status.EXPARRAY);

		/*
		 * The data element must be an arrayValue or this isn't going to be very
		 * interesting.
		 */

		if (!array.isType(Value.ARRAY))
			return new Status(Status.EXPARRAY);

		if( array.size() == 0 )
			return new Status(Status.NOSUCHMEMBER, member);
		/*
		 * Sort doesn't have the nerve to try to sort arrays of disparate types.
		 * So verify that the arrayValue types are all the same, and that none
		 * of them are non-scalar items.
		 */

		int i;
		final int length = array.size();
		int type = Value.UNDEFINED;

		for (i = 1; i <= length; i++) {

			final Value r = array.getElement(i);
			int thisType = r.getType();
			if (thisType != Value.RECORD)
				return new Status(Status.INVTYPE);

			final Value k = r.getElement(member);
			if (k == null)
				return new Status(Status.NOSUCHMEMBER, member, new Status(Status.ATROW, Integer.toString(i)));

			thisType = k.getType();

			/*
			 * For our purposes, treat all numeric types as if they were double,
			 * since that's how we'll handle sorting them.
			 */
			if ((thisType == Value.BOOLEAN) | (thisType == Value.INTEGER))
				thisType = Value.DOUBLE;

			/*
			 * If we haven't seen any data yet, just assume the type of the
			 * first element in the arrayValue.
			 * 
			 * Otherwise, make sure the types match up, and are not an illegal
			 * type such as an arrayValue or program.
			 */

			if (type == Value.UNDEFINED)
				type = thisType;
			else if (type != thisType)
				return new Status(Status.TYPEMISMATCH);
			if (thisType == Value.ARRAY)
				return new Status(Status.INVTYPE);

		}

		/*
		 * Now that we know it's suitably homogeneous and contains only records
		 * of compatible types, then let's do the sort. For now, we do a bubble
		 * sort.  Later we can adapt this to use a variant of the QuickSort if we
		 * find that we're sorting large arrays of records. The performance on the
		 * existing sort falls off badly as the number of elements approaches 1000.
		 */

		for (i = 1; i <= length; i++)
			for (int j = i; j <= length; j++) {

				/*
				 * Make local copies of the arrayValue elements. We need them to
				 * be local because we're going to coerce types and therefore
				 * modify the data for the purposes of arrayValue comparison.
				 */
				final Value srcRecord = array.getElement(i);
				final Value tgtRecord = array.getElement(j);

				Value src = srcRecord.getElement(member).copy();
				Value tgt = tgtRecord.getElement(member).copy();

				boolean swap = false;

				if (type != Value.STRING) {
					if (src.getDouble() > tgt.getDouble())
						swap = true;
				} else if (src.getString().compareTo(tgt.getString()) > 0)
					swap = true;
				if (swap) {

					/*
					 * If the sort order says swap 'em, get fresh (undamaged
					 * by the type coercion required for comparison) references
					 * and do the swap.
					 */
					src = array.getElement(i);
					tgt = array.getElement(j);
					array.setElement(tgt, i);
					array.setElement(src, j);
				}

			}

		return new Status(Status.SUCCESS);
	}

	/**
	 * Sort an array of records using a compound key of more than one column.
	 * This operation can only be performed on a Table value.
	 * <p>
	 * This is done by building a temporary array containing the key values 
	 * and the original row number as the last element of each row.  The
	 * resulting array is sorted, and then the sorted data is used to 
	 * reconstruct the full table again.
	 * @param table the Table object to sort
	 * @param keyList the list of column names to use to construct the key
	 * @return Status a status value indicating success or failure.
	 */
	
	public static Status sortArray( Value table, ArrayList<String> keyList) {
		
		/*
		 * Step one, validate that the value is really a table, and that
		 * all the key names are valid.
		 */
		
		RecordStreamValue v = (RecordStreamValue) table;
		int keyCount = keyList.size();
		
		if( table.getType() != Value.TABLE)
			return new Status(Status.INVTYPE);
		
		if( keyList.size() < 1)
			return new Status(Status.INVCOUNT, keyList.size());
		
		for( int ix = 0; ix < keyCount; ix++ ) 
			if(v.getColumnNumber(keyList.get(ix)) < 1)
				return new Status(Status.NOSUCHMEMBER, keyList.get(ix));
		
		/*
		 * Step two, build the array containing rows built from each
		 * of the column keys.
		 */
		Value keyArray = new Value(Value.ARRAY, null);
		
		for( int ix = 1; ix <= v.size(); ix++ ) {
			Value newRow = new Value(Value.ARRAY, null);
			Value oldRow = v.getElementAsArray(ix);
			
			for( int cx = 0; cx < keyCount; cx++ ) {
				Value m = oldRow.getElement(v.getColumnNumber(keyList.get(cx)));
				newRow.addElement(m);
			}
			newRow.addElement(new Value(ix));
			keyArray.setElement(newRow, ix);
		}
		
		/*
		 * Step three, sort the key array
		 */
		
		Status sts = sortArray(keyArray);
		if( sts.failed())
			return sts;
		
		/*
		 * Step four, create a new table from the old table.
		 */
		
		RecordStreamValue newTable = new RecordStreamValue(v.columnNames());

		/*
		 * Step five, copy the old rows to the new table in the order of
		 * the newly sorted key array.
		 */
		
		for( int ix = 1; ix <= keyArray.size(); ix++ ) {
			Value row = keyArray.getElement(ix);
			int oldPosition = row.getElement(keyCount+1).getInteger();
			Value oldRow = v.getElement(oldPosition);
			newTable.addElement(oldRow);
		}
		
		/*
		 * Final step, replace the elements in the original table with
		 * the newly sorted data.
		 */
		
		v.empty();
		for( int ix = 1;  ix <= newTable.size(); ix++ ) 
			v.addElement(newTable.getElement(ix));
		
		return new Status();
	}
	/**
	 * Sort an array of records. The Value array must contain only arrays, all
	 * of which must have at the same number of columns. Additionally, the 
	 * array column must contain only homogeneous scalar data types for all 
	 * records in the array.
	 * 
	 * @param array
	 *            The Value containing an array of arrays.
	 * @param column
	 *            The column number in the row arrays
	 * @return Status indicating if the sort was successful. It can fail
	 *         because the value isn't a record, or not all records have the
	 *         named member, or the member field values are not all the same
	 *         scalar type.
	 */
	public static Status sortArray(final Value array, final int column) {

		if (array == null)
			return new Status(Status.EXPARRAY);

		/*
		 * The data element must be an arrayValue or this isn't going to be very
		 * interesting.
		 */

		if (!array.isType(Value.ARRAY))
			return new Status(Status.EXPARRAY);

		if( array.size() < 1 )
			return new Status(Status.ARRAYBOUNDS, column);
		
		/*
		 * Sort doesn't have the nerve to try to sort arrays of disparate types.
		 * So verify that the arrayValue types are all the same, and that none
		 * of them are non-scalar items.
		 */

		int i;
		final int length = array.size();
		int type = Value.UNDEFINED;
		
		for (i = 1; i <= length; i++) {

			final Value r = array.getElementAsArray(i);
			int thisType = r.getType();
			if (thisType != Value.ARRAY)
				return new Status(Status.INVTYPE);
			if( r.size() < column )
				return new Status(Status.ARRAYBOUNDS, Integer.toString(column),
						new Status(Status.ATROW, Integer.toString(i)));
			
			final Value k = r.getElement(column);
			if (k == null)
				return new Status(Status.ARRAYBOUNDS, Integer.toString(column),
						new Status(Status.ATROW, Integer.toString(i)));

			thisType = k.getType();

			/*
			 * For our purposes, treat all numeric types as if they were double,
			 * since that's how we'll handle sorting them.
			 */
			if ((thisType == Value.BOOLEAN) | (thisType == Value.INTEGER))
				thisType = Value.DOUBLE;

			/*
			 * If we haven't seen any data yet, just assume the type of the
			 * first element in the arrayValue.
			 * 
			 * Otherwise, make sure the types match up, and are not an illegal
			 * type such as an arrayValue or program.
			 */

			if (type == Value.UNDEFINED)
				type = thisType;
			else if (type != thisType)
				return new Status(Status.TYPEMISMATCH);
			if (thisType == Value.ARRAY)
				return new Status(Status.INVTYPE);

		}

		/*
		 * Now that we know it's suitably homogeneous and contains only records
		 * of compatible types, then let's do the sort. For now, we do a bubble
		 * sort.  Later we can adapt this to use a variant of the QuickSort if we
		 * find that we're sorting large arrays of records. The performance on the
		 * existing sort falls off badly as the number of elements approaches 1000.
		 */

		for (i = 1; i <= length; i++)
			for (int j = i; j <= length; j++) {

				/*
				 * Make local copies of the arrayValue elements. We need them to
				 * be local because we're going to coerce types and therefore
				 * modify the data for the purposes of arrayValue comparison.
				 */
				final Value srcRecord = array.getElementAsArray(i);
				final Value tgtRecord = array.getElementAsArray(j);

				Value src = srcRecord.getElement(column).copy();
				Value tgt = tgtRecord.getElement(column).copy();

				boolean swap = false;

				if (type != Value.STRING) {
					if (src.getDouble() > tgt.getDouble())
						swap = true;
				} else if (src.getString().compareTo(tgt.getString()) > 0)
					swap = true;
				if (swap) {

					/*
					 * If the sort order says swap 'em, get fresh (undamaged
					 * by the type coercion required for comparison) references
					 * and do the swap.
					 */
					src = array.getElementAsArray(i);
					tgt = array.getElementAsArray(j);
					array.setElement(tgt, i);
					array.setElement(src, j);
				}

			}

		return new Status(Status.SUCCESS);
	}

	/**
	 * Sort an array based on the scalar data type of the array. The array must
	 * contain homogeneous data types.
	 * 
	 * @param array the VAlue containing the data to sort.
	 * @return Status indicating if the sort was successful, or if it failed due
	 *         to type mismatches.
	 */
	public static Status sortArray(final Value array) {
		
		if (array == null)
			return new Status(Status.EXPARRAY);

		/*
		 * The data element must be an arrayValue or this isn't going to be very
		 * interesting.
		 */

		if (array.getType() != Value.ARRAY)
			return new Status(Status.EXPARRAY);

		/*
		 * Sort doesn't have the nerve to try to sort arrays of disparate types.
		 * So verify that the arrayValue types are all the same, and that none
		 * of them are non-scalar items.
		 */

		int i;
		final int length = array.size();
		int type = Value.UNDEFINED;

		for (i = 1; i <= length; i++) {

			int thisType = array.getElement(i).getType();

			/*
			 * For our purposes, treat all numeric types as if they were double,
			 * since that's how we'll handle sorting them.
			 */
			if ((thisType == Value.BOOLEAN) | (thisType == Value.INTEGER))
				thisType = Value.DOUBLE;

			/*
			 * If we haven't seen any data yet, just assume the type of the
			 * first element in the arrayValue.
			 * 
			 * Otherwise, make sure the types match up, and are not an illegal
			 * type such as an arrayValue or program.
			 */

			if (type == Value.UNDEFINED)
				type = thisType;
			else if (type != thisType)
				return new Status(Status.TYPEMISMATCH);
			//if ((thisType == Value.ARRAY) | (thisType == Value.RECORD))
			//	return new Status(Status.INVTYPE);

		}

		/*
		 * Now that we know it's suitably homogeneous and doesn't contain arrays
		 * or programs, then let's do the sort. 
		 * 
		 * Initially this was implemented using the bubble sort below.  This is fine
		 * for hundreds of elements, but scaling sucks when you pass about 1,000 array
		 * elements.  So let's use a quicksort instead.  
		 */

		Value[] sortArray = new Value[length];
		for( i = 1; i <= length; i++) 
			sortArray[i-1] = array.getElement(i);
		Quicksort.quicksort( sortArray );

		for( i = 1; i <= length; i++)
			array.setElement(sortArray[i-1], i);
		return new Status();
		
	}

	/**
	 * Compile 'SORT' statement. Processes a token stream, and compiles it into
	 * a byte-code stream associated with the statement object. The first token
	 * in the input stream has already been removed, since it was the "verb"
	 * that told us what kind of statement object to create.
	 * 
	 * @param tokens
	 *            The token buffer being processed that contains the source to
	 *            compile.
	 * @return A Status value that indicates if the compilation was successful.
	 *         Compile errors are almost always syntax errors in the input
	 *         stream. When a compile error is returned, the byte-code stream is
	 *         invalid.
	 */

	public Status compile(final Tokenizer tokens) {

		if (!tokens.testNextToken(Tokenizer.IDENTIFIER))
			return status = new Status(Status.EXPARRAY);

		final String arrayName = tokens.nextToken();
		byteCode = new ByteCode(session, this);
		byteCode.add(ByteCode._LOAD, arrayName);

		if (tokens.assumeNextToken("BY")) {
			if (!tokens.testNextToken(Tokenizer.IDENTIFIER))
				return status = new Status(Status.INVSORT);
			byteCode.add(ByteCode._SORT, tokens.nextToken());
		} else
			byteCode.add(ByteCode._SORT);
		byteCode.add(ByteCode._STOR, arrayName);
		return status = new Status();
	}
}
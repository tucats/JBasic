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
 * COPYRIGHT 2003-2007 BY TOM COLE, TOMCOLE@USERS.SF.NET
 *
 * Created on Sep 18, 2012 by tom
 *
 */
package org.fernwood.jbasic.runtime;
import java.util.Vector;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.value.ObjectValue;
import org.fernwood.jbasic.value.Value;


/**
 * Simple class for bit field handling. A BitFieldMap is a byte buffer
 * used to hold packed integers expressed at bit-strings.  All the bit string
 * values are unsigned; it is the responsibility of the caller to arrange
 * for 2's compliment negative numbers if needed.
 * <p>
 * 
 * A BitFieldMap maintains a field list which describes each of the bit fields
 * in the array.  The bit fields need not describe every bit in the buffer;
 * i.e. there may be bytes or bit fields not touched by the BitFieldMap.
 * Fields have three attributes; a name, a base bit position (0-based in the 
 * byte array) and a length.
 * <p>
 * 
 * Data may be written to the byte array or read from it either by field
 * number (0-based), by field name (case sensitive by default) or as an array
 * of long integer values that unload or load all the fields at once.
 * <p>
 * 
 * As a rule, the routines do not signal errors.  A read operation may
 * return a 0 if the field number or name is invalid.  A write operation
 * will return a boolean result indicating success; failure can occur
 * from an invalid field number or name, or attempting to write a data
 * item that is larger than the bit field can hold.
 * 
 * @author cole
 *
 */
public class BitFieldMap {

	/**
	 * Do we print a flurry of diagnostic messages that show how the
	 * unpack and pack operations proceed?  By default, not so much...
	 */
	public static final boolean DEBUG = false;
	
	/**
	 * Does the field write routine attempt to optimize whole bytes?
	 */
	public static final boolean WRITE_OPT = true;
	

	/**
	 * Class definition for a single field of the BitFieldMap; this
	 * defines the name, base bit address, and field length of each
	 * value.
	 * @author cole
	 *
	 */
	private class BitField {

		

		
		/**
		 * Base position in the byte array, expressed as a bit number.
		 */
		int	base;
		
		/**
		 * Length of this field, expressed in bits
		 */
		int	length;
		
		/**
		 * Virtual field name.
		 */
		String name;
		
		/**
		 * User type data
		 */
		int type;
		
		/**
		 * Typed constructor
		 * @param theName Name of the field
		 * @param theKind the kind of the field
		 * @param theBase starting bit position (0-based)
		 * @param theType abstract data type indicator (used by user)
		 * @param theLength length of the bit field
		 */
		BitField( String theName, int theBase, int theLength, int theType) {
			base = theBase;
			name = theName;
			length = theLength;
			type = theType;
		}
	}
	
	
	/**
	 * Dynamic list of the bit fields being manipulated.
	 */
	private Vector<BitField> fields;
	
	/**
	 * Flag indicating if name lookups are caseSensitive or not; the
	 * default is true;
	 */
	private boolean caseSensitive;
	
	/**
	 * Simple constructor
	 */
	public BitFieldMap() {
		caseSensitive = true;
		fields = new Vector<BitField>();
	}
	
	/**
	 * Create a human-readable expression of the object.
	 */
	public String toString() {
		StringBuffer result = new StringBuffer("BitFieldMap(");
		
		result.append(Integer.toString(fields.size()));
		result.append(" fields [ ");
		for( BitField bf : fields) {
			if( bf.name != null)
				result.append(bf.name);
			result.append("(");
			result.append(Integer.toString(bf.length));
			result.append("@");
			result.append(Integer.toString(bf.base));
			result.append(") ");
		}
		result.append("]; buffer size ");
		result.append(Integer.toString(getBufferSize()));
		result.append("; hash ");
		result.append(Integer.toString(this.hashCode()));
		result.append(")");
		
		return result.toString();
	}
	/**
	 * Set the case sensitivity of methods that identify a field by name.
	 * @param flag true if the name is case-sensitive; i.e. the name
	 * must exactly match.  Set to false if the name can be in a different
	 * case than as declared.
	 */
	public void caseSensitivity( boolean flag ) {
		caseSensitive = flag;
	}
	
	/**
	 * Return an array of the user-encoded type information.
	 * @return int[] of the user type data
	 */
	public int[] getTypes() {
		
		int[] typeArray = new int[fields.size()];
		for( int ix = 0; ix < fields.size(); ix++)
			typeArray[ix] = fields.get(ix).type;
		return typeArray;
	}
	
	/**
	 * Add a field definition.
	 * @param theName Name of the field
	 * @param theBase base bit position in the buffer
	 * @param theLength length of the field
	 * @param theType integer type of data this field is used for (optional)
	 * @return the number of fields in the field array after the
	 * insert, or a -1 if the field name was already defined.
	 */
	public int addField( String theName, int theBase, int theLength, int theType) {
		if( findField(theName) >= 0 )
			return -1;
		
		fields.add(new BitField(theName, theBase, theLength, theType));
		
		return fields.size();
	}
	
	/**
	 * Given a field name, find the field number associated with it.
	 *
	 * @param theName the field name.  This is normally case-sensitive
	 * unless the BitFieldMap object has explicitly had it's case
	 * sensitivity disabled.
	 * @return the zero-based field number, or -1 if the field name
	 * does not exist.
	 */
	public int findField( String theName ) {
		
		for( int idx = 0; idx < fields.size(); idx++ ) {
			String fieldName = fields.get(idx).name;
			if( caseSensitive && fieldName.equals(theName))
				return idx;
			if( fieldName.equalsIgnoreCase(theName))
				return idx;
		}
		return -1;
	}
	
	/**
	 * Delete a field definition by name.
	 * @param theName the field name to remove
	 * @return true if the field was found and deleted, else false.
	 */
	public boolean deleteField( String theName ) {
		int idx = findField(theName);
		if( idx == -1)
			return false;
		fields.remove(idx);
		return true;
	}

	
	/**
	 * Get an integer value out of the buffer by field number.
	 * @param bytes the buffer to read
	 * @param fieldNumber the field position (0-based) to read.
	 * @return an integer value
	 */
	public int getInt( byte[] bytes, int fieldNumber) {
		long result = getLong(bytes, fieldNumber);

		return (int) result;
	}
	
	
	/**
	 * Given a byte buffer, get the nth field from the buffer
	 * @param bytes the byte buffer
	 * @param fieldNumber the zero-based position in the buffer.
	 * @return the field value
	 */
	public long getLong(byte[] bytes, int fieldNumber) {
		long x = 0;
		BitField f = fields.get(fieldNumber);
		int nextByte = f.base/8;
		int b = bytes[nextByte];
		if (b < 0 )
			b = 256+b; /* Fix 2's compliment values */
		
		int bitPos = f.base % 8;
		b = (b << bitPos) & 0xff;
		int bits = f.length;
		if(DEBUG)
			System.out.println("Field=" + fieldNumber + ", base=" + f.base + ", len=" + f.length + ", byte = " + f.base/8 + ", bit=" + f.base%8);
		
		/* Optimization - is the first entire byte used? */
		
		while( bits >= 8 && bitPos == 0 ) {
			if(DEBUG)
				System.out.println("  Fast-process entire byte " + b + "(" + BitFieldMap.paddedBinary(b,8) + ") at " + nextByte);
			x = (x<<8) + b;
			bits = bits - 8;
			if( bits > 0 ) {
				b = bytes[++nextByte];
				if (b < 0 )
					b = 256+b; /* Fix 2's compliment values */
			}
		}

		while( bits-- > 0 ) {
			int newBit = (b & 0x80) >> 7;
			if(DEBUG)
				System.out.println(" bit=" + bits + ", x=" + BitFieldMap.paddedBinary(x,32) + ", b=" + BitFieldMap.paddedBinary(b, 8) + ", newBit=" + newBit);
			
			x = (x << 1) + newBit;
			b = (b << 1);
			bitPos++;
			if( bits > 0 && bitPos == 8 ) {
				bitPos = 0;
				if(DEBUG)
					System.out.println("  Fetch new byte at " + (nextByte+1));
				b = bytes[++nextByte];
				if (b < 0 )
					b = 256+b; /* Fix 2's compliment values */

				/* Optimization - if the entire byte is part of the field, move it en-mass */
				
				while( bits >= 8 ) {
					if(DEBUG)
						System.out.println("  Fast-process entire byte " + BitFieldMap.paddedBinary(b,8));
					x = (x << 8) + b;
					bits = bits - 8;
					if( bits > 0 ) {
						if(DEBUG)
							System.out.println("  Fetch new byte at " + (nextByte+1));
						b = bytes[++nextByte];
						if (b < 0 )
							b = 256+b; /* Fix 2's compliment values */
					}
				}
			}
		}
		if(DEBUG)
			System.out.println("  result=" + x + ", " + BitFieldMap.paddedBinary(x,32));
		return x;
	}

	/**
	 * Return the number of fields in the bit-field.  This is the same as
	 * the size of the Long[] array returned from the getFields() method.
	 * @return int size of resulting array
	 */
	public int size() {
		return fields.size();
	}
	
	/**
	 * Utility routine to empty out a buffer
	 * @param buffer pointer to the byte buffer
	 * @return number of bytes emptied out.
	 */
	public int clearBuffer( byte[] buffer) {
		if( buffer == null )
			return 0;
		
		for( int ix = 0; ix < buffer.length; ix++)
			buffer[ix] = 0;
		return buffer.length;
		
	}
	/**
	 * Return an array containing the field names in order.
	 * @return an array of String items.
	 */
	public String[] getNames() {
		String[] names = new String[fields.size()];
		for( int n = 0; n < fields.size(); n++)
			names[n] = fields.get(n).name;
		return names;
	}
	
	

	/**
	 * Set an integer field value in the buffer.
	 * @param buffer the Byte Buffer
	 * @param fieldNum the field number
	 * @param value the int value to write
	 * @return true if the set is successful
	 */
	public boolean setInt(byte[] buffer, int fieldNum, int value ) {
		return setLong(buffer, fieldNum, value);
	}
	
	/**
	 * Put a longword value into a bit field in the buffer
	 * @param buffer The buffer to write the value into
	 * @param fieldNum the 0-based field number to write
	 * @param value the longword containing the integer value
	 * @return true if successfull, else invalid name, etc.
	 */
	public boolean setLong( byte[] buffer, int fieldNum, long value) {
		
		if( fieldNum < 0 || fieldNum > fields.size())
			return false;
		BitField bf = fields.get(fieldNum);
		int bytePos = (bf.base + bf.length-1) / 8;
		int bitPos  = 8 - ((bf.base + bf.length) % 8);
		if( bitPos == 8 )
			bitPos = 0;
		
		int b  = buffer[bytePos];
		if(DEBUG) {
			System.out.println("Field " + fieldNum + "; base=" + bf.base + "; len=" + bf.length);
			System.out.println(" Value = " + value + "; " + paddedBinary(value, bf.length));
		}
			
		if(DEBUG)
			System.out.println(" Fetching byte " + bytePos + "; b="  + paddedBinary(b,8));
		boolean partial = false;
		
		for( int count = bf.length; count > 0; count--) {

			int bit = (byte) (value & 1);
			value = value >> 1;
		    int bitMask = 1<<bitPos;
			int byteMask = (0xFF ^ bitMask);
			int newBit = (bit<<bitPos);
			b = (b & byteMask) + newBit;
			partial = true;
			
			if(DEBUG)
				System.out.println("  Bit " + count + "; b=" + bit + "; bitPos=" + bitPos + "; B=" + paddedBinary(b, 8));
			
			bitPos++;
			if( bitPos >= 8) {
				buffer[bytePos] = (byte) b;
				if(DEBUG)
					System.out.println(" Writing  byte " + bytePos + "; b="  + paddedBinary(b,8));
				if( count > 1 ) {
					b = buffer[--bytePos];
					partial = false;
					if(DEBUG)
						System.out.println(" Fetching byte " + bytePos + "; b="  + paddedBinary(b,8));
				}
				bitPos = 0;
				
				/* Optimize if this is for an entire byte */
				while( WRITE_OPT && count > 8 ) {
					b = (int) (value % 256);
					value = value >> 8;
					buffer[bytePos] = (byte) b;
					if(DEBUG)
						System.out.println("  Byte at " + count + "; B=" + paddedBinary(b, 8));
					if(DEBUG)
						System.out.println(" Writing  byte " + bytePos + "; b="  + paddedBinary(b,8));

					count = count - 8;
					if( count > 1) {
						b = buffer[--bytePos];
						partial = false;
						if(DEBUG)
							System.out.println(" Fetching byte " + bytePos + "; b="  + paddedBinary(b,8));
					}
				}
			}
		}
		if( partial ) {
			buffer[bytePos] = (byte) b;
			if(DEBUG)
				System.out.println(" Writing  byte " + bytePos + "; b="  + paddedBinary(b,8));
		}
		
		/* 
		 * For this to be successful, there must be no non-zero bits left in
		 * the data value.
		 */
		return (value == 0);
	}
	
	/**
	 * Get a string value out of a buffer using field position info.  
	 * 
	 * @param buffer the buffer to read data from
	 * @param fieldNumber the field number to read
	 * @return the string value.
	 */
	public String getString( byte[] buffer, int fieldNumber) {
	
		if( fieldNumber < 0 || fieldNumber >= fields.size())
			return null;
		
		byte[] bytes = getBytes(buffer, fieldNumber);
	
		StringBuffer result = new StringBuffer();
		for( int i = 0; i < bytes.length; i++ )
			result.append(bytes[i]);

		return result.toString();
		
	}

	/**
	 * Write a string value into the buffer in an arbitrary field position
	 * @param buffer the buffer
	 * @param fieldNumber the field number
	 * @param value the string to write
	 * @return true if the write was successful.
	 */
	public boolean setString( byte[] buffer, int fieldNumber, String value ) {
		
		if( fieldNumber < 0 || fieldNumber >= fields.size())
			return false;
		
		BitField bf = fields.get(fieldNumber);

		int resultLen = bf.length / 8;
		if( bf.length % 8 == 0 )
			resultLen++;
		
		byte[] alignedBuffer = new byte[resultLen];
		for( int i = 0; i < resultLen; i++ ) 
			alignedBuffer[i] = (byte) value.charAt(i);
		
		/*
		 * If the bitfield is already on a byte boundary and an
		 * even number of bytes, then just write the data to
		 * the buffer and call it a day.
		 */
		
		if(( bf.base % 8 == 0) && (bf.length % 8 == 0)) {
			
			int b = bf.base / 8;
			int l = bf.length / 8;
			for( int i = b; i <= b+l; i++ )
				buffer[i] = alignedBuffer[i];
		}
		
		/*
		 * Nope, it's not aligned so we have to create a new map to
		 * store the values a byte-at-a-time.
		 */
		
		else {
			
			BitFieldMap bm = new BitFieldMap();
			for( int i = bf.base; i < bf.base+bf.length; i = i + 8) {
				bm.addField(null, i, 8);
			}
			int l = bf.base + bf.length;
			if( l % 0 == 0)
				l = (l/8) + 1;
			else
				l = l/8;
			for( int i = 0; i < l; i++ )
				bm.setInt(buffer, i, alignedBuffer[i]);
				
		}
		return true;
		
	}
	/**
	 * Store a double in the buffer at an arbitrary byte position.
	 * @param buffer the data buffer
	 * @param fieldNumber the field number
	 * @param value the double value to write
	 * @return true if the write was successful.
	 */
	public boolean setDouble( byte[] buffer,  int fieldNumber, double value) {
		return setLong(buffer, fieldNumber, Double.doubleToLongBits(value));
	}
	
	
	/**
	 * Get a double from the buffer at an arbitrary position.
	 * @param buffer source buffer
	 * @param fieldNumber field number 
	 * @return a double from the buffer.
	 */
	public double getDouble( byte[] buffer, int fieldNumber) {

		/*
		 * Read a long from the buffer and convert it to a double.
		 */
		return Double.longBitsToDouble(getLong(buffer, fieldNumber));
	}

	/**
	 * Get a string value out of a buffer using field position info.  Note that
	 * the byte buffer need not be aligned; that is, a byte buffer can be read
	 * from within another byte buffer at an arbitrary bit position.
	 * 
	 * @param buffer the buffer to read data from
	 * @param fieldNumber the field number to read
	 * @return the bytes from the field..
	 */
	public byte[] getBytes( byte[] buffer, int fieldNumber) {
		
		
		if( fieldNumber < 0 || fieldNumber >= fields.size())
			return null;
		
		BitField bf = fields.get(fieldNumber);

		int resultLen = bf.length / 8;
		if( bf.length % 8 == 0 )
			resultLen++;
		
		byte[] result = new byte[resultLen];
		
		/*
		 * If the bitfield is already on a byte boundary and an
		 * even number of bytes, then just pull the data from
		 * the buffer and return it.
		 */
		
		if(( bf.base % 8 == 0) && (bf.length % 8 == 0)) {
			
			int b = bf.base / 8;
			int l = bf.length / 8;
			for( int i = b; i <= b+l; i++ )
				result[i] = buffer[i];
		}
		
		/*
		 * Nope, it's not aligned so we have to create a new map to
		 * fetch the values a byte-at-a-time.
		 */
		
		else {
			
			BitFieldMap bm = new BitFieldMap();
			for( int i = bf.base; i < bf.base+bf.length; i = i + 8) {
				bm.addField(null, i, 8);
			}
			int l = bf.base + bf.length;
			if( l % 0 == 0)
				l = (l/8) + 1;
			else
				l = l/8;
			for( int i = 0; i < l; i++ )
				result[i] = (byte) bm.getInt(buffer,i);
				
		}
		return result;
		
	}
	/**
	 * Utility function to pad a binary representation of a value.
	 * @param v value to print as binary data
	 * @param n width of the field to print in bits
	 * @return String representation
	 */
	static String paddedBinary( long v, int n ) {
		
		int offset = 256;
		if( n == 16 )
			offset = 65536;
		long xv = ( v < 0 ) ? ( v + offset ) : v;
		
		String vb = Long.toBinaryString(xv);
		StringBuffer b = new StringBuffer();
		if( vb.length() >= n )
			return vb.substring(0,n);
		
		for( int i = vb.length(); i < n; i++)
			b.append('0');
		return b.toString() + vb;
	}

	/**
	 * Get the size of the buffer described by the bit field map.
	 * @return integer describing the number of whole bytes that the
	 * buffer must contain.
	 */
	public int getBufferSize() {
		int maxBitPos = -1;
		
		for( BitField bf : fields)
			if( bf.base + bf.length > maxBitPos)
				maxBitPos = bf.base + bf.length;
				
		if( maxBitPos % 8 != 0)
			maxBitPos = maxBitPos + 8;
		return maxBitPos / 8;

	}

	/**
	 * Add a new field with no type; which assumes that the type is INTEGER.
	 * @param mapEntryName the name of the field
	 * @param mapEntryPos the starting 0-based bit position
	 * @param mapEntryLen the length in bits
	 * @return the count of fields now defined
	 */
	public int addField(String mapEntryName, int mapEntryPos, int mapEntryLen ) {

		return addField(mapEntryName, mapEntryPos, mapEntryLen, Value.INTEGER);
	}
	

	/**
	 * Given a JBASIC Field record, build a valid BitFieldMap from it.
	 * @param fieldRecord the record definition, typically built from
	 *  a FIELD statement with a BITFIELD clause.
	 * @return a newly created BitFieldMap, or null if an error occurred
	 * @throws JBasicException if an invalid/missing field is found in the 
	 * BITFIELD record
	 */
	public static BitFieldMap buildBitMap(Value fieldRecord) throws JBasicException {
		BitFieldMap bm;
		/*
		 * Did we store a copy of the pre-processed map data already?
		 * If so, just extract the objects from the field data.
		 */
		Value mapObject = fieldRecord.getElement("__MAP");
		if( mapObject != null ) {
			bm = (BitFieldMap) mapObject.getObject();
		} else {
			/*
			 * No cache of previously processed data, so scan the 
			 * field description and build a BitFieldMap.
			 */
			bm = new BitFieldMap();

			Value mapArray = fieldRecord.getElement("MAP");
			if( mapArray == null )
				throw new JBasicException(Status.NOMEMBER, "MAP");
			if( mapArray.getType() != Value.ARRAY)
				throw new JBasicException(Status.TYPEMISMATCH);
			int maxBitPos = 0;


			for( int ix = 0; ix < mapArray.size(); ix++) {
				Value mapEntry = mapArray.getElement(ix+1);
				if( mapEntry.getType() != Value.RECORD)
					throw new JBasicException(Status.TYPEMISMATCH);
				String mapEntryName = mapEntry.getString("NAME");					
				String mapEntryTypeName = mapEntry.getString("TYPE").toUpperCase();
				int mapEntryType = Value.nameToType(mapEntryTypeName);
				if( mapEntryType != Value.INTEGER &&
						mapEntryType != Value.STRING &&
						mapEntryType != Value.DOUBLE )
					throw new JBasicException(Status.TYPEMISMATCH);
				
				/*
				 * The position is optional, and if not included is calculated
				 * as next-available-bit.
				 */
				
				int mapEntryPos = maxBitPos;
				if( mapEntry.getElement("POS") != null)
					mapEntryPos = mapEntry.getElement("POS").getInteger();
				
				/* 
				 * Get this fields length, and update the max bit position 
				 * accordingly.
				 */
				
				int mapEntryLen=64;
				if( mapEntry.getElement("LEN") != null )
					mapEntryLen = mapEntry.getElement("LEN").getInteger();
				if( mapEntryPos+mapEntryLen > maxBitPos)
					maxBitPos = mapEntryPos+mapEntryLen;
				bm.addField(mapEntryName, mapEntryPos, mapEntryLen, mapEntryType);
			}

			/*
			 * Store what we've compiled back into the object so we don't
			 * have to rebuild the BitMap each time.
			 */
			
			fieldRecord.setElement( new ObjectValue(bm), "__MAP");

		}
		return bm;
	}
}

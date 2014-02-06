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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * Binary version of JBasicFile. This class supports BINARY file I/O, which
 * means that data is read and written in native binary representation from the
 * external file, which must be accessible to the user via a file name.
 * <p>
 * The operations that can be performed on a BINARY file are OPEN, CLOSE, GET,
 * PUT, and SEEK. The GET and PUT operations read/write one or more variables
 * from/to the file, and can be done by individual variable name or by defining
 * a record that reads a group of variables all at once.
 * <p>
 * A BINARY file has a position that can changes after each GET or PUT to point
 * to the "next" byte after the most recent operation. When a file is initially
 * opened, this is at byte zero. The SEEK statement and the setPos() operation
 * are done on a BINARY file to position to a specific byte in the file. This
 * lets the user write programs that perform true random addressing of records
 * in the file. Note that the language supports the use of a RECORD data type to
 * identify which record number (as opposed to byte number) is to be used; this
 * must be calculated at runtime by the _SEEK bytecode, and passed to the
 * JBFBinary file as a byte position.
 * 
 * @author cole
 * 
 */
public class JBFBinary extends JBasicFile {

	/**
	 * This is the random access file handle from the java.io package that is
	 * used to actually perform the random file I/O operations.
	 */
	RandomAccessFile dataStream;
	
	/**
	 * This is the default size for STRING fields if the length is not given.
	 * If this value is set to null, then an extra integer precedes the string
	 * to indicate it's actual length.  If it is non-null, then this is the
	 * default for "unsized" strings.
	 * <p>
	 * Note that this value isn't actually used in this class at all, but is
	 * used by opcodes that will communicate with the BINARY file type, such
	 * as the _GET opcode.
	 */
	
	public Value defaultStringSize;

	/**
	 * Create a JBasic binary file.
	 * @param jb the controlling session that owns the file.
	 */
	public JBFBinary(final JBasic jb) {
		super(jb);
		mode = MODE_BINARY;
	}

	/**
	 * Open the binary data file, using a provided file name. This operation
	 * must be performed before the file can be used for any I/O operation.
	 * 
	 * @param fn
	 *            The external physical file name expressed as a Value object.
	 *            Normally this is a string.
	 * @throws JBasicException  if a file I/O error occurs
	 */

	public void open(final Value fn, final SymbolTable symbols) throws JBasicException {
			
		final String extName = fn.getString();
		if( extName.equalsIgnoreCase(JBasic.CONSOLE_NAME)) {
			throw new JBasicException(Status.FILECONSOLE);
		}

		fname = extName;
		mode = MODE_BINARY;
		type = FILE;
		lastStatus = new Status(Status.SUCCESS);

		try {
			String fsName = JBasic.userManager.makeFSPath(this.jbenv, extName);
			dataStream = new RandomAccessFile(fsName, "rw");
		} catch (final FileNotFoundException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			lastStatus.print(jbenv);
			throw new JBasicException(lastStatus);
		}
		
		register();
		defaultStringSize = symbols.localReference("SYS$BINARY_STRING_SIZE");

		return;
	}

	/**
	 * Return an INTEGER value from a BINARY file. The file must already be
	 * open, and there must be data left in the file to read. The file position
	 * is advanced automatically.
	 * @param size the size of the integer to read in bytes.  Allowed values are 1, 2, and 4.
	 * @return A Value containing the integer read, or null if there was a read
	 *         error. The error condition is stored in the file status.
	 * @throws JBasicException if a file I/O error occurs
	 */
	public Value getInteger(int size) throws JBasicException {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return null;
		}

		try {
			switch( size ) {
			case 1:	return new Value(dataStream.read());
			case 2:	return new Value(dataStream.readShort());
			case 4: return new Value(dataStream.readInt());
			}
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			return null;
		}
		throw new JBasicException(Status.INVRECDEF, "invalid INTEGER size " + size);
	}
	

	/**
	 * Return an DOUBLE value from a BINARY file. The file must already be open,
	 * and there must be data left in the file to read. The file position is
	 * advanced automatically.
	 * 
	 * @return A Value containing the DOUBLE read, or null if there was a read
	 *         error. The error condition is stored in the file status.
	 */
	public Value getDouble() {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return null;
		}

		try {
			return new Value(dataStream.readDouble());
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			return null;
		}
	}

	/**
	 * Return an FLOAT value from a BINARY file. The file must already be open,
	 * and there must be data left in the file to read. The file position is
	 * advanced automatically.  The FLOAT value is promoted to a DOUBLE for
	 * use in JBasic
	 * 
	 * @return A Value containing the single-precision value read, or null 
	 * if there was a read error. The error condition is stored in the file status.
	 */
	public Value getFloat() {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return null;
		}

		try {
			return new Value(dataStream.readFloat());
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			return null;
		}
	}
	/**
	 * Return an BOOLEAN value from a BINARY file. The file must already be
	 * open, and there must be data left in the file to read. The file position
	 * is advanced automatically.
	 * 
	 * @return A Value containing the boolean read, or null if there was a read
	 *         error. The error condition is stored in the file status.
	 */
	public Value getBoolean() {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return null;
		}

		try {
			return new Value(dataStream.readBoolean());
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			return null;
		}
	}

	/**
	 * Return an STRING value from a BINARY file. The file must already be open,
	 * and there must be data left in the file to read. The file position is
	 * advanced automatically.
	 * 
	 * @param count
	 *            The number of bytes to read from the file and store in the
	 *            string.
	 * @return A Value containing the integer read, or null if there was a read
	 *         error. The error condition is stored in the file status.
	 * @throws JBasicException if a file I/O error occurs
	 */
	public Value getString(final int count) throws JBasicException {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return null;
		}

		try {
			final char stringData[] = new char[count];
			for (int n = 0; n < count; n++) {
				byte ch = dataStream. readByte();
				stringData[n] = (char) ch ;
			}

			return new Value(String.copyValueOf(stringData));

		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			throw new JBasicException(Status.EOF);
		}
	}

	/**
	 * Return an UNICODE String value from a BINARY file. The file must already be open,
	 * and there must be data left in the file to read. The file position is
	 * advanced automatically.  Note that the UNICODE data must be in UCS16 format.
	 * 
	 * @param count
	 *            The number of characters to read from the file and store in the
	 *            string.
	 * @return A Value containing the string read, or null if there was a read
	 *         error. The error condition is stored in the file status.
	 * @throws JBasicException if a file I/O error occurs
	 */
	public Value getUnicode(final int count) throws JBasicException {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return null;
		}

		try {
			StringBuffer stringData = new StringBuffer(count);
			for (int n = 0; n < count; n++) {
				stringData.append((char)dataStream.readShort());
			}

			return new Value(stringData.toString());

		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			throw new JBasicException(Status.EOF);
		}
	}
	/**
	 * Write a string value to a binary file for a given number of bytes.
	 * 
	 * @param s
	 *            The String value to write
	 * @param count
	 *            The number of bytes to write. IF the string is shorter than
	 *            count bytes, then it is truncated as it is written to the
	 *            file. IF count is longer than the string, then blanks are
	 *            written to the file
	 * @return A Status indicator telling if the string was written to the file.
	 * @throws JBasicException if a file I/O error occurs
	 */
	public boolean putString(final String s, final int count) throws JBasicException {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return false;
		}

		try {
			final int len = s.length();
			for (int n = 0; n < count; n++) {
				int ch = ' ';
				if (n >= len)
					ch = ' ';
				else
					ch = s.charAt(n);
				if( ch < 0 || ch > 255) {
					throw new JBasicException(Status.IOERROR, "character byte value out of range: " + ch);
				}
				dataStream.write(ch);
			}
			return true;

		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			return false;
		}

	}

	/**
	 * Write a UCS16 Unicode STRING value to a binary file for a given number of bytes.
	 * 
	 * @param s
	 *            The STRING value to write
	 * @param count
	 *            The number of characters to write. IF the string is shorter than
	 *            count bytes, then it is truncated as it is written to the
	 *            file. IF count is longer than the string, then blanks are
	 *            written to the file
	 * @return A Status indicator telling if the string was written to the file.
	 */
	public boolean putUnicode(final String s, final int count) {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return false;
		}

		try {
			final int len = s.length();
			for (int n = 0; n < count; n++) {
				int ch = ' ';
				if (n >= len)
					ch = ' ';
				else
					ch = s.charAt(n);
				dataStream.writeChar(ch);
			}
			return true;

		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			return false;
		}

	}

	/**
	 * Write a boolean value to a BINARY mode file
	 * 
	 * @param b
	 *            The boolean value to write
	 * @return True if the write operation as successful, and False if it
	 *         failed. In case of failure, the lastStatus field of the file
	 *         describes what went wrong.
	 */

	public boolean putBoolean(final boolean b) {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return false;
		}

		try {
			dataStream.writeBoolean(b);
			return true;
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			return false;
		}
	}

	/**
	 * Write an integer value to a BINARY mode file
	 * 
	 * @param i
	 *            The integer to write
	 * @param size The size of the integer value in bytes.  Valid
	 * values are 1, 2, or 4.
	 * @return True if the write operation as successful, and False if it
	 *         failed. In case of failure, the lastStatus field of the file
	 *         describes what went wrong.
	 * @throws JBasicException if the size is not a valid integer byte size.
	 */

	public boolean putInteger(final int i, int size) throws JBasicException {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return false;
		}

		try {
			switch( size ) {
			case 1:	dataStream.write(i);
					break;
			case 2: dataStream.writeShort(i);
					break;
			case 4: dataStream.writeInt(i);
					break;
			default:
				throw new JBasicException(Status.INVRECDEF, "invalid INTEGER size" + size);

			}
			return true;
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			return false;
		}

	}

	/**
	 * Write a double precision floating point value to a BINARY mode file
	 * 
	 * @param d
	 *            The double to write
	 * @return True if the write operation as successful, and False if it
	 *         failed. In case of failure, the lastStatus field of the file
	 *         describes what went wrong.
	 */
	public boolean putDouble(final double d) {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return false;
		}

		try {
			dataStream.writeDouble(d);
			return true;
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			return false;
		}
	}
	
	/**
	 * Write a single precision floating point value to a BINARY mode file
	 * 
	 * @param d
	 *            The DOUBLE to write, as a FLOAT
	 * @return True if the write operation as successful, and False if it
	 *         failed. In case of failure, the lastStatus field of the file
	 *         describes what went wrong.
	 */
	public boolean putFloat(final double d) {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return false;
		}

		try {
			dataStream.writeFloat((float) d);
			return true;
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			return false;
		}
	}

	/**
	 * Determine if the file is at end-of-file. This means that there is no more
	 * data that can be read from the file from it's current position. For an
	 * empty file, this will always be true.
	 * 
	 * @return Return <em>true</em> if a sequential read operation from the
	 *         current position will fail because there is no more data. Return
	 *         <em>false</em> if there is more data and we are not at
	 *         end-of-file.
	 */
	public boolean eof() {

		try {
			final long current = dataStream.getFilePointer();
			final long size = dataStream.length();
			// System.out.print("Current = " + current + "; size = " + size );
			return (size <= current);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Set the position of the file to an arbitrary byte.
	 * 
	 * @param p
	 *            the byte position (zero-based) in the file.
	 * @return the previous file position before the set operation, or -1 if the
	 *         set operation failed.
	 */
	public long setPos(final long p) {

		try {
			final long prev = dataStream.getFilePointer();
			dataStream.seek(p);
			return prev;
		} catch (final IOException e) {
			// Should never get here
			e.printStackTrace();
		}
		return -1;

	}

	/**
	 * Get the current binary file position.
	 * 
	 * @return the position in the file (zero-based) or -1 if the file is not
	 *         opened for BINARY access.
	 */

	public long getPos() {

		try {
			return dataStream.getFilePointer();
		} catch (final IOException e) {
			// Should never get here
			e.printStackTrace();
		}
		return -1;
	}

	public void close() {
		try {
			dataStream.close();
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			lastStatus.print(jbenv);
		}
		super.close();

	}

	/**
	 * Read an arbitrary array of bytes from the file as raw data.
	 * @param bufferLen the number of bytes desired
	 * @return a byte[] containing the data, or null if an EOF
	 * was encountered. Note that the byte[] may be shorter than
	 * the number of bytes requested if that was all the data in 
	 * the file.
	 */
	public byte[] getBytes(int bufferLen) {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return null;
		}

		try {
			byte[] buffer = new byte[bufferLen];
			int bytes = dataStream.read(buffer);
			if( bytes <=0 )
				return null;
			if( bytes == bufferLen )
				return buffer;
			byte[] shortBuffer = new byte[bytes];
			for( int ix = 0; ix < bytes; ix++ )
				shortBuffer[ix] = buffer[ix];
			return shortBuffer;
			
		} catch (final IOException e) {
			lastStatus = new Status(Status.IOERROR, e.toString());
			return null;
		}
	}

	/**
	 * Write an arbitrary byte buffer to the file as raw data.
	 * @param buffer the buffer to  write
	 * @return the number of bytes written, or -1 if there was
	 * an error.
	 */
	public int putBytes(byte[] buffer) {
		if (mode != MODE_BINARY) {
			lastStatus = new Status(Status.NOTBINARY);
			return -1;
		}

		try {
			dataStream.write(buffer);
		} catch (IOException e) {
			return -1;
		}
		return buffer.length;
	}
}

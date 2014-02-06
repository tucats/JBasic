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
package org.fernwood.jbasic.funcs;

import java.nio.ByteBuffer;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.BitFieldMap;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>GETBITS()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return an string representation of bits in a buffer.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = SETBITS( <em>buffer</em>, <em>value</em>, <em>start-pos</em>, <em>field-len</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Return an string buffer representing the contents of a bit-field inserted into 
 * a character buffer.
 * 
 * @author cole
 * 
 */
public class SetbitsFunction extends JBasicFunction {
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  an argument count or type error occurred
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {
		
		arglist.validate(4, 4, new int[] {Value.UNDEFINED, Value.UNDEFINED, Value.NUMBER, Value.NUMBER});

		Value value = arglist.element(1);
		int base = arglist.element(2).getInteger();
		int length = arglist.element(3).getInteger();


		
		byte[] bytes = null;
		ByteBuffer b = null;
		
		Value source = arglist.element(0);
		int type = source.getType();
		
		if( type == Value.STRING){ 
			String buffer = arglist.element(0).getString();
			int bufferLen = buffer.length();
			if( bufferLen < (base+length)/8)
				bufferLen = (base+length)/8;
			bytes = new byte[bufferLen];
			for( int ix = 0; ix < bytes.length; ix++)
				bytes[ix] = (byte) buffer.charAt(ix);
		} else if( type == Value.INTEGER) {
			b = ByteBuffer.allocate(4);
			b.putInt(source.getInteger());			
			bytes = b.array();
		} else throw new JBasicException(Status.ARGTYPE);
				
		
		BitFieldMap locker = new BitFieldMap();
		locker.addField("FIELD", base, length);
		
		switch(value.getType()) {
		
		case Value.INTEGER:
			locker.setLong(bytes, 0, value.getInteger());
			break;
			
		case Value.STRING:
			locker.setString(bytes, 0, value.getString());
			break;
			
		case Value.DOUBLE:
			locker.setDouble(bytes, 0, value.getDouble());
			break;
			
		default:
			throw new JBasicException(Status.INVTYPE);
			
		}
		
		if( type == Value.INTEGER && b != null)
			return new Value(b.getInt(0));
		
		StringBuffer outBuff = new StringBuffer();
		for( int ix = 0; ix < bytes.length; ix++ ) 
			outBuff.append((char)bytes[ix]);
		
		return new Value(outBuff.toString());
		

	}
}

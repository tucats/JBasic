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
 * <tr><td><b>Description:</b></td><td>Return an integer representation of bits in a buffer.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>i = GETPOS( <em>buffer</em>, <em>start-pos</em>, <em>field-len</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Integer</td></tr>
 * </table>
 * <p>
 * Return an integer representing the contents of a bit-field extracted from 
 * a character buffer.
 * 
 * @author cole
 * 
 */
public class GetbitsFunction extends JBasicFunction {
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
		
		arglist.validate(3, 4, new int[] {Value.UNDEFINED, Value.NUMBER, Value.NUMBER, Value.UNDEFINED});
		
		byte[] bytes = null;
		Value source = arglist.element(0);
		if( source.getType() == Value.STRING){ 
			String buffer = arglist.element(0).getString();
			bytes = new byte[buffer.length()];
			for( int ix = 0; ix < bytes.length; ix++)
				bytes[ix] = (byte) buffer.charAt(ix);
		} else if( source.getType() == Value.INTEGER) {
			ByteBuffer b = ByteBuffer.allocate(4);
			b.putInt(source.getInteger());			
			bytes = b.array();
		} else throw new JBasicException(Status.ARGTYPE);
		
		int base = arglist.element(1).getInteger();
		int length = arglist.element(2).getInteger();
		
		int type = Value.INTEGER;
		if( arglist.size() == 4) {
			Value typeDesignation = arglist.element(3);
			if( typeDesignation.getType() == Value.INTEGER || typeDesignation.getType() == Value.DOUBLE)
				type = typeDesignation.getInteger();
			else
				type = Value.nameToType(typeDesignation.getString().toUpperCase());
		}
		
		BitFieldMap locker = new BitFieldMap();
		locker.addField("FIELD", base, length);
		
		switch( type ) {
		
		case Value.INTEGER:
			return new Value(locker.getInt(bytes, 0));

		case Value.STRING:
			return new Value(locker.getString(bytes, 0));
			
		case Value.DOUBLE:
			return new Value(locker.getDouble(bytes, 0));	
			
		}
		
		throw new JBasicException(Status.INVTYPE);

	}
}

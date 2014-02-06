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
 * Created on Jun 6, 2007 by cole
 *
 */
package org.fernwood.jbasic.informats;

import org.fernwood.jbasic.informats.InputProcessor.Command;
import org.fernwood.jbasic.value.Value;


/**
 * HEX(length) Format<p>
 * 
 * This implements the HEX input format, which reads a textual
 * representation of a hexadecimal integer and converts it to a
 * binary integer value.  If the length is "*" then a varying-width field
 * is searched for the first integer found.  If an explicit length is given
 * then a field of exactly that size is searched.  The integer MUST be
 * right-justified within the field.
 * @author cole
 * @version version 1.0 Jun 6, 2007
 *
 */
class HexFormat extends Informat {
	
	Value run( InputProcessor input, Command cmd ) {
		int mark = input.mark();
		char ch = input.nextChar();
		int sign = 1;
		int v = 0;
		
		int len = cmd.length;
		input.fActive = false;
	
		/*
		 * Special case... if the length is negative, then it was the
		 * "*" operator for length and means "varying" and will search
		 * an arbitrarily large field.
		 */
		
		if( len == Command.VARYING )
			len = input.buffer.length();
		
		while( len > 0 ) {
			if( Character.isWhitespace(ch)) {
				ch = input.nextChar();
				len--;
			}
			else
				break;
		}
		
		int count = 0;
		
		while( count < len ) {
			int charpos = InputProcessor.findCharacter(ch, "0123456789ABCDEFabcdefgh");
			if( charpos > 0 ) {
				if( charpos > 16)
					charpos = charpos - 6;
				v = v * 16 + ( charpos-1);
				ch = input.nextChar();
				count++;
			}
			else {
				
				/*
				 * If we are searching for a varying-length field, 
				 * then a "bad character" just means we are done.
				 */
				
				if( cmd.length == Command.VARYING )
					break;
				/*
				 * Bad character, reset the buffer and signal error
				 */
				input.reset(mark);
				return null;
			}
		}

		/*	We will have parsed one ahead, put it back */
		
		if( count > 0 ) {
			input.backup();
			return new Value(v*sign);
		}
		input.reset(mark);
		return null;
	}
}

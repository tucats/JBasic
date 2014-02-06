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
 * CHAR(length) Format<p>
 * 
 * This implements the CHAR input format, which reads a textual
 * representation of character data.  If the length is "*" then 
 * a varying-width field
 * is searched for the first string enclosed by whitespace. 
 * If an explicit length is given
 * then a field of exactly that size is searched. 
 * @author cole
 * @version version 1.0 Jan 21, 2010
 *
 */
class CharFormat extends Informat {
	
	Value run( InputProcessor input, Command cmd ) {
		char ch = input.nextChar();
		StringBuffer result = new StringBuffer();
		
		int len = cmd.length;
		input.fActive = false;
	
		/*
		 * Special case... if the length is negative, then it was the
		 * "*" operator for length and means "varying" and will search
		 * an arbitrarily large field.
		 */
		
		if( len == Command.VARYING ) {
			len = input.buffer.length();

			while( len > 0 ) {
				if( Character.isWhitespace(ch)) {
					ch = input.nextChar();
					len--;
				}
				else
					break;
			}
		}

		int count = 0;
		
		while( count < len ) {
			if(Character.isWhitespace(ch) && cmd.length == Command.VARYING)
				break;
			
			result.append(ch);
			ch = input.nextChar();
			count++;
		}

		/*	We will have parsed one ahead, put it back */
		
		if( count > 0 ) {
			input.backup();
		}
		return new Value(result.toString());
	}
}

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
 * SKIP(length) Format<p>
 * 
 * This implements the SKIP format, which skips a fixed number of bytes
 * before resuming input processing. It can also be used to skip to
 * the end of the next whitespace block, or to skip until it passes a
 * specific character string pattern.
 * <p>
 * This format does not return a value.
 * 
 * @author cole
 * @version version 1.0 Jun 6, 2007
 *
 */
class SkipFormat extends Informat {
	
	Value run( InputProcessor input, Command cmd ) {
		
		/*
		 * See if we're matching a string value.
		 */
		if( cmd.string != null ) {
			int ix = input.currentPos;
			while( ix+cmd.length < input.buffer.length()) {
				if( input.buffer.substring(ix, ix+cmd.length).equals(cmd.string)) {
					input.currentPos = ix+cmd.length;
					input.fActive = true;
					return null;
				}
				ix++;
			}
			
			input.currentPos = input.buffer.length();
			input.fActive = false;
			return null;
		}
		/*
		 * If we are doing SKIP(*), this just skips to the next
		 * non-blank character.
		 */
		if( cmd.length == Command.VARYING ) {
			while( input.currentPos < input.buffer.length()) {
				char ch = input.nextChar();
				if( !Character.isWhitespace(ch)) {
					input.backup();
					return null;
				}
			}
			input.fActive = false;
			return null;
		}
		
		/*
		 * Nope, this is SKIP(n) where we skip an absolute number
		 * of characters.
		 */
		for( int ix = 0; ix < cmd.length; ix++ ) {
			if( input.currentPos > input.buffer.length()) {
				input.fActive = false;
				return null;
			}
			input.nextChar();
		}
		return null;
	}
}

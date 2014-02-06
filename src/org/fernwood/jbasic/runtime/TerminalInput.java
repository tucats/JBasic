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
 * Created on Oct 11, 2007 by cole
 *
 */
package org.fernwood.jbasic.runtime;

import java.io.IOException;
import java.io.Reader;

import org.fernwood.jbasic.JBasic;

import net.wimpi.telnetd.io.BasicTerminalIO;

/**
 * @author cole
 * @version version 1.0 Oct 11, 2007
 *
 */
public class TerminalInput extends Reader {

	BasicTerminalIO	channel;
	private boolean echoFlag;
	JBasic session;
	
	/**
	 * @param ch The BasicTerminalIO channel to be used to read text.
	 * 
	 */
	public TerminalInput( BasicTerminalIO ch ) {
		super();
		this.channel = ch;
		echoFlag = true;
	}

	/**
	 * Lock a reader session.
	 * @param lock The lock object
	 */
	public TerminalInput(Object lock) {
		super(lock);
	}

	/**
	 * @see java.io.Reader#close()
	 */
	public void close() throws IOException {
		channel.close();
	}

	/**
	 * @see java.io.Reader#read(char[], int, int)
	 */
	public int read(char[] cbuf, int off, int len) throws IOException {

		boolean reading = true;
		char[] buffer = new char[len];
		int pos = 0;
		
		while( reading ) {
			int ch = channel.read();
			char theChar = (char) ch;
			
			switch( ch ) {
			
			case 3:
				if( session != null ) {
					session.interrupt();
				}
				break;
				
			case BasicTerminalIO.BACKSPACE:
			case BasicTerminalIO.DELETE:
				if( pos > 0 ) {
					pos--;
					if( echoFlag ) {
						channel.write((char)8);
						channel.write(' ');
						channel.write((char)8);
					}
				}
				break;
				
			case BasicTerminalIO.ENTER:
				if( echoFlag )
					channel.write('\n');
				buffer[pos++] = '\n';
				reading = false;
				break;
				
				default:
					if( echoFlag )
						channel.write(theChar);
					buffer[pos++] = theChar;
					if( pos == len )
						reading = false;
			}
		}
		for( int ix = 0; ix < pos; ix++ )
			cbuf[ix + off] = buffer[ix];
		
		return pos;
	}

	/**
	 * Enable echo mode for this terminal session. When set the characters
	 * typed by the user are echoed on their terminal.  When clear, the
	 * characters are not echoed (such as for entering a password value).
	 * @param b true if echo mode is to be enabled
	 */
	public void setEcho(boolean b) {
		echoFlag = b;
	}
}

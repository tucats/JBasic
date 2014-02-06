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
 * Created on Mar 27, 2009 by tom
 *
 */
package org.fernwood.jbasic;

/**
 * 
 * A message object. Message objects are containers that have a message code
 * (a short string in uppercase) and a message text (the full text of the
 * message) as members. This message container is used as the object stored
 * in the TreeMap containing all the error return code and message mapping
 * data.
 * 
 * @author tom
 * @version version 1.0 Apr 19, 2005
 * 
 */
public class Message {

	private String mCode;

	String mText;

	public String toString() {
		return "CODE=" + mCode + ": \"" + mText + "\"";
	}
	/**
	 * Access function to get the current message code from a message
	 * object.
	 * 
	 * @return String containing the code for the message.
	 */
	public String getMessageCode() {
		return mCode;
	}

	/**
	 * Set the code for the current message object.
	 * 
	 * @param code
	 *            The uppercase string code.
	 */
	public void setMessageCode(final String code) {
		mCode = code;
	}

	/**
	 * Get the text of the current message object.
	 * 
	 * @return The message text.
	 */
	public String getMessageText() {
		return mText;
	}

	/**
	 * Set the text of the current message object
	 * 
	 * @param text
	 *            The text (including optional substitution operator)
	 */
	public void setMessageText(final String text) {
		mText = text;
	}

	/**
	 * Constructor to create a new message object.
	 * 
	 * @param c
	 *            The code value for the message
	 * @param t
	 *            The text (in english) of the message code.
	 */
	Message(final String c, final String t) {
		mCode = c;
		mText = t;
	}
};
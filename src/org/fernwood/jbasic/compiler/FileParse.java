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
package org.fernwood.jbasic.compiler;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;

/**
 * File Parser object. This is used to support compilation of file references in
 * the language. This handles the distinction between file references that may
 * optionally contain the keyword FILE or may have a sharp-sign ("#") prefix to
 * designate the file number.
 * <p>
 * This module handles the relationship between sharp-sign references and the
 * underlying file ID. If a reference is made to #3, this is converted to
 * __FILE3 by this routine for the purposes of identifying the file.
 * <p>
 * This object has two main functions:
 * <p>
 * <list>
 * <li>Parse a file reference by consuming tokens as needed.
 * <li>Generate the code to place the file reference on the stack. </list>
 * <p>
 * These functions are separated because the file reference is often parsed
 * early in the process of compiling a statement, but the code generation may
 * occur after more parsing has been done. The state of the parsed information
 * is stored in this object and can be used by the generation method
 * accordingly. A FileParse object can be re-used by calling the reference()
 * method with a new tokenizer; this replaces any previous content of the
 * object.
 * 
 * @author tom
 * @version version 1.0 Aug 30, 2006
 * 
 */
public class FileParse {

	/**
	 * Status of file parse operation.
	 */
	private Status status;

	/**
	 * File ID name parsed.
	 */
	private String fid;

	/**
	 * Flag indicating if the "#n" format allowed for file references.
	 * This is true when in GWBASIC compatibility mode.
	 */
	boolean allowNumeric;
	
	/**
	 * Flag indicating if the file name is indirect; i.e. the reference
	 * is really a variable containing the name of the file ref.  This
	 * occurs when the USING clause is employed.
	 */
	
	boolean indirect;
	
	/**
	 * @param tokens The current token stream being compiled.
	 * @param fileRequired is the FILE keyword required?  Some statements may
	 * allow the FILE keyword to be optional, so you only need give the 
	 * file identifier.  Others require a specific keyword to disambiguate
	 * parsing.
	 */
	public FileParse(final Tokenizer tokens, final boolean fileRequired) {
		status = new Status();
		fid = null;
		
		try {
			fid = reference(tokens, fileRequired);
		} catch (JBasicException e) {
			status = e.getStatus();
			fid = null;
		}
		allowNumeric = true;
	}

	/**
	 * @param tokens the input stream containing the text of a file reference.
	 * @param fFileRequired is the FILE keyword required in the context of
	 * this parse operation, or can we accept just an identifier?  This is
	 * up to the caller.  For example, CLOSE does not require the FILE
	 * keyword, but PRINT does.
	 * 
	 * @return Name of identifier to use for file name handling.
	 * @throws JBasicException the token stream does not reference a valid file id
	 */
	public String reference(final Tokenizer tokens, final boolean fFileRequired) throws JBasicException {

		boolean hadFile = false;
		boolean hadParens = false;
		
		int mark = tokens.getPosition();
		
		if (tokens.assumeNextToken("#")) {
			hadFile = true;
		} else if (tokens.assumeNextToken("FILE")) {
			hadFile = true;
			if( tokens.assumeNextSpecial("("))
				hadParens = true;
		}
		if (fFileRequired && !hadFile) {
			status = new Status(Status.FILESYNTAX);
			tokens.setPosition(mark);
			return null;

		}

		if( tokens.assumeNextToken("USING")) {
			indirect = true;
			if( tokens.assumeNextSpecial("("))
				hadParens = true;
		}
		/*
		 * Get the next token.  It must be an integer or an identifier.
		 */
		String fid = tokens.nextToken();
		if( tokens.getType() == Tokenizer.INTEGER) {
			fid = JBasic.FILEPREFIX + fid;
		}
		else if (tokens.getType() != Tokenizer.IDENTIFIER) {
			tokens.setPosition(mark);
			throw new JBasicException(Status.INVFID, fid);
		}

		if( hadParens)
			if( !tokens.assumeNextSpecial(")")) {
					tokens.setPosition(mark);
					throw new JBasicException(Status.PAREN);
			}
		return fid;
	}

	/**
	 * Generate the code needed to put a reference to the file on the stack.
	 * The reference must have been previously processed by the reference()
	 * method that does the parsing and determination of the ultimate file
	 * id name.
	 * 
	 * @param targetByteCode the ByteCode buffer into which the code is compiled to load 
	 * the reference on the stack.
	 */
	public void generate(final ByteCode targetByteCode) {
		targetByteCode.add(ByteCode._LOADFREF, indirect? 1 : 0, fid);
	}

	/**
	 * Return the parsed FID (file ID) which is an identifier string
	 * @return the FID value, or null if no valid FID has been parsed.
	 */
	public String getFID() {
		return fid;
	}
	
	/**
	 * Return the status of the last file parse operation.
	 * @return the Status value, or null if no parse has been done yet.
	 */
	public Status getStatus() {
		return status;
	}
	
	/**
	 * Return flag indicating if the file reference is meant to be
	 * indirect.
	 * @return true if the file reference is indirect; i.e. a USING clause
	 */
	
	public boolean getIndirect() {
		return indirect;
	}
	
	/**
	 * Return indicator if there was a parsing failure.
	 * @return true if a parse has been attempted and failed.  The getStatus()
	 * method can be used to retrieve the actual error.
	 */
	public boolean failed() {
		if( status == null )
			return false;
		return status.failed();
	}

	/**
	 * Return indicator if file ID parsing was successful.
	 * @return true if a parse has been attempted and succeeded.  If false,
	 * the getStatus()
	 * method can be used to retrieve the actual error.
	 */

	public boolean success() {
		return fid != null & !failed();
	}
}

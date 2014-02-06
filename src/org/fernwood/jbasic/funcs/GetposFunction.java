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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBFBinary;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>GETPOS()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Return current position of a BINARY file.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>i = GETPOS( <em>file-identifier</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Integer</td></tr>
 * </table>
 * <p>
 * Return an integer indicating the current file position of the given
 * file. The file must be opened in BINARY mode or an error is signaled.
 * The result is a zero-based integer value indicating where the next
 * byte will be read from the file.
 * <p>
 * The file identifier can be an identifier or an integer; if it is an
 * integer then it references a file by number, as used in the <em>#n</em>
 * notation.
 * 
 * @author cole
 * 
 */
public class GetposFunction extends JBasicFunction {
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

		JBFBinary f = null;

		arglist.validate(1, 1, null);

		Value v = arglist.element(0);
		if (v.isType(Value.INTEGER)) {
			final String fid = "__FILE_" + v.getString();
			v = symbols.reference(fid);
		}

		boolean fSignal = arglist.session.globals().findReference("SYS$SIGNAL_FUNCTION_ERRORS", false).getBoolean();
			
		JBasicFile fx = JBasicFile.lookup(arglist.session, v);
		if( fx == null) {
			if( fSignal )
				throw new JBasicException(Status.NOSUCHFID, v.getString());
			return new Value(false);
		}
		
		if( fx.getMode() != JBasicFile.MODE_BINARY) {
			if( fSignal)
				throw new JBasicException(Status.NOTBINARY, fx.getName());
			return new Value(false);
		}
			
		f = (JBFBinary) fx;
		return new Value(f.getPos());

	}
}

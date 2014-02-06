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
package org.fernwood.jbasic.opcodes;

import java.util.Iterator;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBFInput;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpQUIT extends AbstractOpcode {

	/**
	 * Quit from JBasic
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		/*
		 * Use the SYS$SAVEPROMPT system variable to contain the prompt string. If
		 * this variable does not exist or has a zero length (set to "") then no
		 * prompting is done. The string can be set to any arbitrary string the user
		 * wants, the default is set up during global symbol initialization.
		 */

		boolean willQuit = true;

		if (env.session.isRunning() && env.session.programs.unsaved()) {

			final JBFInput inputFile = (JBFInput) env.session.stdin();
			String thePrompt;
			thePrompt = env.session.getString("SYS$SAVEPROMPT");

			if (thePrompt != null && thePrompt.length() > 0) {

				env.session.stdout.print(thePrompt);

				final String inputData = inputFile.read();
				if (!inputData.toUpperCase().startsWith("Y"))
					willQuit = false;
			}
		}

		/*
		 * There was a prompt, and the user did not elect to quit.  So throw an
		 * error that says we DID NOT QUIT and why that's the case.
		 */
		if (!willQuit)
			throw new JBasicException(Status.UNSAVED);

		/*
		 * See if we are to automatically save any CATALOG objects
		 * These are records in the Root symbol area that contain
		 * TABLE objects that are to be persisted.  To be auto-saved,
		 * they must have a __CATALOG_NAME member with the file name
		 * to save, and a __CATALOG_AUTOSAVE set to true.
		 */
		
		SymbolTable root = env.localSymbols;
		while( root != null && !root.fRootTable )
			root = root.parentTable;
		
		if( root != null && root.fRootTable ) {
			Iterator<String> i = root.table.keySet().iterator();
			while( i.hasNext()) {
				String name = i.next();
				Value v = root.findReference(name, false);
				if( !v.isType(Value.RECORD))
					continue;
				if( v.getElement(OpCATALOG.CATALOG_FLAG) == null )
					continue;
				Value m = v.getElement(OpCATALOG.CATALOG_NAME);
				if( m == null )
					continue;
				
				String fName = m.getString();
				if( fName == null )
					continue;
				m = v.getElement(OpCATALOG.CATALOG_AUTO);
				if( m == null )
					continue;
				if( !m.getBoolean())
					continue;
				
				if( !OpCATALOG.isDirty(v))
					continue;
				
				//env.session.stdout.println("NOTE: Autosave " + v);
				env.session.run("SAVE CATALOG " + name);
			}
		}
		
		
		/*
		 * Power down the session by reporting that we're no longer
		 * running, and signal the get-out-of-Dodge message.
		 */
		env.codeStream.fRunning = false;
		env.session.running(false);
		throw new JBasicException(Status.QUIT);

	}

}

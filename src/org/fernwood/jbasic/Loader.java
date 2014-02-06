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
package org.fernwood.jbasic;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.XMLManager;
import org.fernwood.jbasic.value.Value;

/**
 * This class handles loading source files into working storage for JBasic.
 * <p>
 * 
 * This is done as a result of an explicit LOAD statement given by the user, by
 * the implicit LOAD of "Workspace.jbasic" from the user's home directory at
 * startup, or by an attempt to automatically load a file when an unrecognized
 * verb is given.
 * 
 * @author cole
 * 
 */
public class Loader {

	/**
	 * Load one or more programs from a source file. Scans the source file for
	 * <code>PROGRAM</code> statements, and uses the name of each one to
	 * create a program in memory.
	 * 
	 * @param session
	 *            The JBasic object containing the current session.
	 * @param pathName
	 *            The name of the source file to read. If the first character of
	 *            the name is an "@" then it indicates that the file is a named
	 *            resource in the Java resource path (typically embedded in the
	 *            JBasic.jar file).
	 * 
	 * @return Status block indicating if program file load was successful
	 */

	public static Status loadFile(final JBasic session, final String pathName) {

		BufferedReader infile = null;
		final boolean savedLoadingSystemObjects = session.isLoadingSystemObjects();
		final boolean fAutoRenumber = session.getBoolean("SYS$AUTORENUMBER");

		String defaultName = Utility.baseName(pathName);
		String fname = Utility.normalize(pathName);

		if (fname.substring(0, 1).equals("@")) {
			try {
				infile = openResourceStream(session, fname);
			} catch (JBasicException e) {
				return e.getStatus();
			}
		} else
			try {
				infile = new BufferedReader(new FileReader(fname));
			} catch (final FileNotFoundException e1) {

				return new Status(Status.INFILE, fname);
			}

		try {
			String line;
			final Tokenizer tokens = new Tokenizer(null, JBasic.compoundStatementSeparator);
			Program p = null;
			boolean hasLineNumbers = false;
			int lineCounter = 0;
			StringBuffer readBuffer = null;

			while ((line = infile.readLine()) != null) {

				/*
				 * Handle the line continuation character that might be on this
				 * line. If so, create or append it to a buffer that captures
				 * the line parts.
				 */
				int len = line.length();
				if (len > 0
						&& line.charAt(len - 1) == JBasic.LINE_CONTINUATION_CHAR) {
					if (readBuffer == null)
						readBuffer = new StringBuffer();
					readBuffer.append(line.substring(0, len - 1));
					continue;
				}

				/*
				 * If we had a previous buffer, add the new line to the previous
				 * buffer and use the entire buffer as the line to process
				 */

				if (readBuffer != null) {
					readBuffer.append(line);
					line = readBuffer.toString();
					readBuffer = null;
				}

				/*
				 * Handle any macro substitutions before we tokenize the line
				 */
				
				line = Utility.resolveMacros(session, line);
				
				/*
				 * From here on we must look at some specific tokens in the line
				 * to guide us about what to do with the line.
				 */
				lineCounter++;
				tokens.loadBuffer(line);

				/*
				 * If this is the first line, see if it is really an XML object
				 * by checking for a <VALUE> tag as the first three tokens of
				 * the first line of the file. Note that we only allow this in
				 * the first line of the file; no comments or other silliness
				 * are permitted in an XML file like this.
				 */

				if (lineCounter == 1) {
					Tokenizer xmlTokenizer = new Tokenizer(line);
					if (xmlTokenizer.assumeNextSpecial("<"))
						if (xmlTokenizer.assumeNextSpecial("?"))
							if (xmlTokenizer.assumeNextToken("XML")) {
								return loadXML(session, line, infile);
							}
				}

				// If this is an empty string and we're not in a program
				// block, then toss it away.

				if (tokens.testNextToken(Tokenizer.END_OF_STRING)
						&& (hasLineNumbers | (p == null)))
					continue;

				/*
				 * If we're loading a user-saved workspace, then there will be
				 * line numbers. Skip them if we find them.
				 */

				if (tokens.testNextToken(Tokenizer.INTEGER)) {
					tokens.nextToken();
					hasLineNumbers = true;
				}

				/*
				 * If this is a PROGRAM definition we have work to do to get the
				 * name of the program from the PROGRAM statement and create the
				 * next object to register.
				 */

				if (tokens.assumeNextToken("PROGRAM")) {
					defaultName = null;
					final String pn = JBasic.PROGRAM + tokens.nextToken();
					if (p != null) {
						/* Now would be the time to renumber the program! */
						if (fAutoRenumber || !hasLineNumbers)
							p.renumber(100, 10);
						/* And then mark it as not being modified */
						p.clearModifiedState();

					}
					p = new Program(session, pn);
					p.register();
					hasLineNumbers = false;
				}
				/*
				 * If this is a FUNCTION definition it's the same basic job as
				 * PROGRAM except we edit the name.
				 */

				if (tokens.assumeNextToken("FUNCTION")) {
					defaultName = null;
					String fn = tokens.nextToken();
					if (fn == null)
						return new Status(Status.INVPGM);
					fn = JBasic.FUNCTION + fn.toUpperCase();

					if (p != null) {
						/* Now would be the time to renumber the program! */
						if (fAutoRenumber || !hasLineNumbers)
							p.renumber(100, 10);
						p.clearModifiedState();
					}
					p = new Program(session, fn);
					p.register();
					hasLineNumbers = false;
				}

				// If this is a VERB definition it's the same
				// basic job as PROGRAM except we modify the name.

				if (tokens.assumeNextToken("VERB")) {
					defaultName = null;
					String fn = tokens.nextToken();
					if (fn == null)
						return new Status(Status.INVPGM);
					fn = JBasic.VERB + fn.toUpperCase();

					if (p != null) {
						/*
						 * Now would be the time to renumber the program. We do
						 * this if SYS$AUTORENUMBER is turned on, or if the
						 * program has no line numbers.
						 */
						if (fAutoRenumber || !hasLineNumbers)
							p.renumber(100, 10);
						p.clearModifiedState();
					}
					p = new Program(session, fn);
					p.register();
					hasLineNumbers = false;
				}

				/*
				 * If there is no PROGRAM type object active already, then if we
				 * have a default name from the path then use that for this new
				 * program name. Otherwise, we've got to complain about this.
				 */
				if (p == null) {
					if (defaultName != null) {
						final String pn = JBasic.PROGRAM + defaultName;
						p = new Program(session, pn);
						p.register();
						hasLineNumbers = true;
						p.add("1 PROGRAM " + pn);
					} else {
						session
								.setLoadingSystemObjects(savedLoadingSystemObjects);
						return new Status(Status.PGMNOTFIRST, fname);
					}
				}

				p.add(line);
			}
			if (p != null) {
				if (fAutoRenumber || !hasLineNumbers)
					p.renumber(100, 10);
				p.clearModifiedState();
			}
			infile.close();

		} catch (final IOException e) {
			session.setLoadingSystemObjects(savedLoadingSystemObjects);
			return new Status(Status.INFILE, fname);
		}
		session.setLoadingSystemObjects(savedLoadingSystemObjects);
		return new Status(Status.SUCCESS);

	}


	/**
	 * @param session
	 * @param fname
	 * @return
	 * @throws JBasicException 
	 */
	private static BufferedReader openResourceStream(final JBasic session,
			String fname) throws JBasicException {
		BufferedReader infile;
		final String fisname = fname.substring(1, fname.length());
		InputStream fis = JBasic.class.getResourceAsStream(fisname);
		if (fis == null)
			fis = JBasic.class.getResourceAsStream(fisname
					+ JBasic.DEFAULTEXTENSION);

		if (fis == null)
			throw new JBasicException(Status.INFILE, fname);
		session.setLoadingSystemObjects(true);
		infile = new BufferedReader(new InputStreamReader(fis));
		return infile;
	}


	/**
	 * Add a path name to the list of locations we search for automatic
	 * load operations (JBasic files named the same as an unrecognized verb, 
	 * which can be automatically loaded to define the VERB, then executed).
	 * <p>
	 * The list is kept in an array SYS$PATH which is the list of strings.
	 * 
	 * @param session
	 *            The JBasic object containing this session.
	 * @param path
	 *            A String containing a new path name to be added to the list.
	 * @throws JBasicException a fatal error occurred accessing the system
	 * symbol table.
	 */
	static public void addPath(final JBasic session, final String path) throws JBasicException {
		Value prefixList;
		try {
			prefixList = session.globals().reference("SYS$PATH");
		}
		catch (Exception e) {
			prefixList = null;
		}
		if (prefixList == null) {
			prefixList = new Value(Value.ARRAY, null);
			session.globals().insert("SYS$PATH", prefixList);
		}

		prefixList.addElement(new Value(path));
	}

	/**
	 * Static method that attempts to load a file given a string prefix for that
	 * file, which typically identifies the path (location) for the file.
	 * 
	 * @param session
	 *            the JBasic session into which the program object will be
	 *            loaded.
	 * @param prefix
	 *            The path prefix to prepend to the 'fname' parameter to form a
	 *            file name.
	 * @param fname
	 *            The filename that is added to the prefix.
	 * @return A Status value indicating if the load was successful. A
	 *         Status.SUCCESS means that it was loaded, and we should attempt to
	 *         resolve the VERB that triggered the auto load. Any other error
	 *         causes us to try another path from the SYS$PATH list instead.
	 */
	private static Status attemptLoad(final JBasic session, final String prefix,
			final String fname) {

		return loadFile(session, prefix + fname);
	}

	/**
	 * Determine if a given filename has already been attempted for load. This
	 * caching operation is used to prevent repeated syntax errors from
	 * generating file load attempts over and over.
	 * <p>
	 * 
	 * The method searches the SYS$LOAD_LIST array to see if there is a fileref
	 * entry for the item we're asked to load. If so, then the load state is
	 * returned to the caller. IF it wasn't found, then it tells the caller to
	 * try to load it anyway.
	 * 
	 * @param session
	 *            The instance of JBasic in which the program will be loaded.
	 * @param fname
	 *            The filename that we are testing to see if it has loaded or
	 *            not.
	 * @return A boolean flag. True means we have previously attempted a load
	 *         for this name. False means we should try one now.
	 */
	private static boolean alreadyLoaded(final JBasic session, final String fname) {
		Value loadList;
		try {
			loadList = session.globals().reference("SYS$LOAD_LIST");
		}
		catch( Exception e ) { 
			loadList = null; 
		}

		if( loadList == null )
			return false;

		final int len = loadList.size();

		for (int ix = 1; ix <= len; ix++) {
			final Value fileInfo = loadList.getElement(ix);
			if (fileInfo == null)
				continue;

			if (fileInfo.getType() != Value.RECORD) {
				continue;
			}

			final Value name = fileInfo.getElement("NAME");
			if (name == null)
				continue;
			if (name.getString().equalsIgnoreCase(fname))
				return true;
		}
		return false;

	}

	/**
	 * Given a filename, search the SYS$PATH list of locations to see if we can
	 * successfully load the file from one of those locations. This is used by
	 * the demand loader.
	 * <p>
	 * 
	 * When a verb is given that is not recognized, one of the steps is to see
	 * if there is a file of the name "Verb.jbasic" where "Verb" is the
	 * unrecognized command verb. The SYS$PATH list is used to decide where to
	 * look for this file. If found, it is loaded and we attempt to see if a
	 * VERB definition is now available for the filename.
	 * <p>
	 * 
	 * For performance reasons, if a filename is attempted once, it will not be
	 * re-tried in subsequent requests, using the alreadyLoaded() method.
	 * 
	 * @param session
	 *            The JBasic object containing this session.
	 * @param fname
	 *            The filename we are attempting to load.
	 * @return A Status indicating if the load was successful.
	 * @throws JBasicException if a system fault prevents access to the
	 * system global symbol space.
	 */
	static public Status pathLoad(final JBasic session, final String fname) throws JBasicException {

		final Value prefixList = session.globals().reference("SYS$PATH");

		final Value fileInfo = new Value(Value.RECORD, null);
		fileInfo.setElement(new Value(fname), "NAME");

		final Value loadList = session.globals().reference("SYS$LOAD_LIST");

		if (alreadyLoaded(session, fname))
			return new Status();

		final int len = prefixList.size();
		final int next = ( loadList == null ) ? 0 : loadList.size() + 1;
		for (int ix = 1; ix <= len; ix++) {
			final String prefix = prefixList.getString(ix);
			final Status sts = attemptLoad(session, prefix, fname);
			if (sts.success() && loadList != null) {
				fileInfo.setElement(new Value(true), "FOUND");
				fileInfo.setElement(new Value(prefix), "PATH");
				loadList.setElementOverride(fileInfo, next);
				return sts;
			}
		}

		if( loadList != null ) {
			fileInfo.setElement(new Value(false), "FOUND");
			loadList.setElementOverride(fileInfo, next);
		}

		return new Status(Status.FILENF, fname);
	}

	private static Status loadXML(JBasic session, String line, BufferedReader infile) throws IOException {

		Status sts = null;
		String XMLbuffer;

		/*
		 * Looks like a proper <Value> root, so let's
		 * read in the entire file as one big XML string.
		 */
		StringBuffer xml = new StringBuffer(line);
		while ((XMLbuffer = infile.readLine()) != null) {
			xml.append(XMLbuffer);
		}

		/*
		 * Convert the XML into a Value that contains the
		 * program definition.  If the XML is bogus, then
		 * we just report an error and we're done.
		 */
		XMLManager xmlParser = new XMLManager(session);
		xmlParser.setString(xml.toString());
		xmlParser.setPassword("cheerios");
		Value programValue = xmlParser.parseXML("JBasicProgram");
		if( programValue == null )
			sts = xmlParser.getStatus();
		else {
			/*
			 * Store the record Value in a temporary variable in
			 * the global symbol table.
			 */
			String tempVar = "__$TEMP_" + JBasic.getUniqueID();
			try {
				session.globals().insert(tempVar, programValue);
				/*
				 * Construct and run a NEW USING(v) command on the temporary
				 * variable we just created, which results in an attempt to
				 * load the program from the variable.
				 */
				sts = session.run("NEW USING(" + tempVar + ")");

			} catch (JBasicException e) {
				sts = e.getStatus();
			}

			/*
			 * Close the input file, and toss away the temporary variable
			 * we had created.  The return the status of the NEW USING
			 * command which will report if there were problems in the
			 * Value or a name conflict.
			 */
			infile.close();
			session.globals().delete(tempVar);
		}

		return sts;
	}
}

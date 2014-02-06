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

import java.io.File;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.FSMConnectionManager;
import org.fernwood.jbasic.runtime.FSMFile;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.runtime.XMLManager;
import org.fernwood.jbasic.value.Value;

/**
 * <b>FILETYPES()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Information about files in a directory.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = FILETYPES( <em>path-name</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of records</td></tr>
 * </table>
 * <p>
 * Create an ARRAY of RECORD objects that contain descriptors for 
 * each file or directory in the given path.  The records contain the
 * system meta data  known about each file in the directory
 * described by the string
 * argument. An empty string is interpreted to mean the current user's
 * default directory.  The resulting RECORD has the same format as the
 * <code>FILETYPE()</code> function. 
 * <p>
 * Items returned in the record include:
 * <p>
 * <table>
 * <tr><td><b>Item</b></td><td><b>Description</b></td></tr>
 * <tr><td>DIRECTORY</td><td>Is the object a directory?</td></tr>
 * <tr><td>FILE</td><td>Is the object a file?</td></tr>
 * <tr><td>HIDDEN</td><td>Is the object hidden?</td></tr>
 * <tr><td>MODIFIED</td><td>Date of last modification as a DOUBLE</td></tr>
 * <tr><td>NAME</td><td>Name of the object</td></tr>
 * <tr><td>PATH</td><td>Directory path of the object</td></tr>
 * <tr><td>READ</td><td>Can the object be read?</td></tr>
 * <tr><td>WRITE</td><td>Can the object be written?t</td></tr>
 * </table>
 * 
 * @author cole
 *
 */

public class FiletypesFunction extends JBasicFunction {
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param argList the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException   an argument count or type error occurred or
	 * a permissions error occurred for a sandboxed user.
	 */

	public Value run(final ArgumentList argList, final SymbolTable symbols) throws JBasicException {

		final Value array = new Value(Value.ARRAY, null);

		int pathIndex = 0;
		int pathCount = 1;

		argList.session.checkPermission(Permissions.DIR_IO);

		for (pathIndex = 0; pathIndex < argList.size(); pathIndex++) {
			String pathName = argList.stringElement(pathIndex);
			
			if (pathName.length() == 0)
				pathName = ".";

			if( pathName.charAt(0) == '_')
				continue;
			
			if( FSMConnectionManager.isFSMURL(pathName)) {
			
				FSMConnectionManager cnx = new FSMConnectionManager(argList.session);
				cnx.parse(pathName);
				String path = cnx.getPath();
				cnx.setPath(null);
				
				try {
					FSMFile f = new FSMFile(cnx.toString());
					String xmlStr = f.describeDomain(path);
					f.terminate();
					
					if( xmlStr == null)
						throw new JBasicException(Status.FILE, new Status(Status.FAULT, f.status()));
					
					symbols.findGlobalTable().insert("LAST$XML", xmlStr);
					XMLManager xml = new XMLManager(argList.session);
					xml.setString(xmlStr);
					Value fsmRecord = xml.parseXML();

					/*
					 * Need to fetch both the FILES and DOMAINS members if present
					 * as they form the array of items to return.
					 */
					
					Value fArray = fsmRecord.getElement("FILES");
					if( fArray != null )
						array.addElement(fArray);
					Value dArray = fsmRecord.getElement("DOMAINS");
					if( dArray != null)
						array.addElement(dArray);
					
					return array;
					
				} catch (Exception e) {
					if( argList.session.signalFunctionErrors())
						throw new JBasicException(Status.INVPATH, pathName);
					return new Value(Value.RECORD, null);
				}
				
				
			}
			
			
			String fsPath = JBasic.userManager.makeFSPath( argList.session, pathName);
			final File path = new File(fsPath);

			final File pathList[] = path.listFiles();
			if (pathList == null)
				continue;

			String separator = System.getProperty("file.separator");
			if( separator == null )
				separator = "/";

			for (File f : pathList) {

				final Value itemArray = new Value(Value.RECORD, null);

				try {
					String fullName = f.getCanonicalPath();
					fullName = JBasic.userManager.makeUserPath(argList.session, fullName);
					final String fileName = f.getName();
					
					int nLen = fullName.length();
					int fLen = fileName.length();
					if( f.isDirectory()) {
						if( !fullName.endsWith(separator))
							fullName = fullName + separator;
					}
					else
						fullName = fullName.substring(0, nLen - fLen);
					itemArray.setElement(new Value(fullName), "PATH");
				} catch (final Exception e) {
					e.printStackTrace();
				}
				itemArray.setElement(new Value(f.getName()), "NAME");
				itemArray.setElement(new Value(f
						.lastModified()), "MODIFIED");
				itemArray.setElement(new Value(f.canRead()), "READ");
				itemArray.setElement(new Value(f.canWrite()), "WRITE");
				itemArray.setElement(new Value(f
						.isDirectory()), "DIRECTORY");
				itemArray.setElement(new Value(f.isFile()), "FILE");
				itemArray.setElement(new Value(f.isHidden()), "HIDDEN");

				if( !f.isDirectory())
					itemArray.setElement(new Value(f.length()), "SIZE");
				
				array.setElement(itemArray, pathCount++);
			}
		}
		return array;
	}

	/**
	 * Return a RECORD structure that describes an FSM domain based on the
	 * XML description
	 * @param xml XML string
	 * @return a RECORD value
	 * @throws JBasicException  if there is a parsing error
	 */
	public Value FSMxml2( String xml ) throws JBasicException {
		Value result = new Value(xml);
		
		return result;
	}
	
}

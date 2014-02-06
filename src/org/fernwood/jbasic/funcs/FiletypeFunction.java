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
import java.text.SimpleDateFormat;
import java.util.Date;

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
 * <b>FILETYPE()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Information about a file.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>r = FILETYPE( <em>file-name</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Record</td></tr>
 * </table>
 * <p>
 * Create a RECORD object that contains descriptors for the various file
 * system metadata items known about the file described by the string
 * argument.  Items returned in the record include:
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
public class FiletypeFunction extends JBasicFunction {
	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param argList the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException   an argument count or type error occurred or a
	 * permissions error occurred for a sandboxed user.
	 */

	public Value run(final ArgumentList argList, final SymbolTable symbols) throws JBasicException {

		argList.validate(1, 1, new int[] {Value.STRING});

		argList.session.checkPermission(Permissions.DIR_IO);

		final Value itemArray = new Value(Value.RECORD, null);

		String pathName = argList.stringElement(0);

		if( FSMConnectionManager.isFSMURL(pathName)) {
		
			FSMConnectionManager cnx = new FSMConnectionManager(argList.session);
			cnx.parse(pathName);
			String path = cnx.getPath();
			cnx.setPath(null);
			
			try {
				FSMFile f = new FSMFile(cnx.toString());
				XMLManager xml = new XMLManager(argList.session);
				
				String xmlDescription = null;
				if( path.endsWith("/"))
					xmlDescription = f.describeDomain(path);
				else
					xmlDescription = f.describeFile(path);
				f.terminate();
				if( xmlDescription == null)
					throw new JBasicException(Status.FILE, new Status(Status.FAULT, f.status()));
				
				symbols.findGlobalTable().insert("LAST$XML", xmlDescription);
				xml.setString(xmlDescription);
				Value fsmRecord = xml.parseXML();
				Value result = new Value(Value.RECORD, null);
				
				String fName = fsmRecord.getElement("NAME").getString();
				
				int fLen = fName.length();
				int pLen = path.length();
				int chars = pLen - fLen;
				
				String fPath = path;
				if( chars>0 )
					fPath = path.substring(0,chars-1);
				
				result.setElement(new Value(fPath), "PATH");
				result.setElement(fsmRecord.getElement("NAME"), "NAME");
				result.setElement(fsmRecord.getElement("SIZE"), "SIZE");
				
				result.setElement(fsmRecord.getElement("MASK"), "MASK");
				Value t = null;
				t = fsmRecord.getElement("REPLICATION");
				if( t != null )	
					result.setElement(t, "REPLICATION");
				
				t = fsmRecord.getElement("BLOCKS");
				if( t != null )
					result.setElement(t, "BLOCKS");
			
				t = fsmRecord.getElement("DATE");
				if( t != null ) {
				SimpleDateFormat theDate = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy");
				Date d = theDate.parse(t.getString());
				result.setElement(new Value(d.getTime()), "MODIFIED");
				}
				else {
					t = fsmRecord.getElement("MODIFIED");
					if( t != null)
						result.setElement(t, "MODIFIED");
				}
				
				result.setElement(fsmRecord.getElement("READ"), "READ");
				result.setElement(fsmRecord.getElement("FILE"), "FILE");
				result.setElement(fsmRecord.getElement("DIRECTORY"), "DIRECTORY");
				result.setElement(fsmRecord.getElement("HIDDEN"), "HIDDEN");
				result.setElement(fsmRecord.getElement("WRITE"), "WRITE");
				return result;
				
			} catch (Exception e) {
				if( argList.session.signalFunctionErrors())
					throw new JBasicException(Status.FILE, new Status(Status.FAULT, e.toString()));
				return new Value(Value.RECORD, null);
			}
			

		}
		
		
		String fsPath = JBasic.userManager.makeFSPath(argList.session, pathName);
		final File f = new File(fsPath);
		String separator = System.getProperty("file.separator");
		if( separator == null )
			separator = "/";

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
			;
		}
		itemArray.setElement(new Value(f.getName()), "NAME");
		itemArray.setElement(new Value(f.lastModified()), "MODIFIED");
		itemArray.setElement(new Value(f.canRead()), "READ");
		itemArray.setElement(new Value(f.canWrite()), "WRITE");
		
		boolean isDirectory = f.isDirectory();
		itemArray.setElement(new Value(isDirectory), "DIRECTORY");
		itemArray.setElement(new Value(f.isFile()), "FILE");
		itemArray.setElement(new Value(f.isHidden()), "HIDDEN");
		if( !isDirectory )
			itemArray.setElement(new Value(f.length()), "SIZE");
		

		return itemArray;
	}
}

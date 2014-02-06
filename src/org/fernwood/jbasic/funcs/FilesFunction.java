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
 * <b>FILES()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Get list of files</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>a = FILES( <em>path</em> )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Array of strings</td></tr>
 * </table>
 * <p>
 *  Create an array object, which contains a list of strings, each describing
 * files in a path. The path is supplied as the function argument. If no
 * argument is given, or it is blank, then the current directory is assumed.
 *
 * Return an array containing the list of files found in the directory(s) specified as strings
 * in the argument list.
 * @author cole
 *
 */
public class FilesFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param argList the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException  an argument count or type error occurred or
	 * a permissions error occurred for a sandboxed user
	 */

	public Value run(final ArgumentList argList, final SymbolTable symbols) throws JBasicException {

		final Value array = new Value(Value.ARRAY, null);

		int pathIndex = 0;
		argList.session.checkPermission(Permissions.DIR_IO);

		/*
		 * If the argument list is empty, assume the current directory.
		 */
		if( argList.size() == 0 )
			argList.insert(new Value("."));

		/*
		 * Scan over all the arguments and create a result set based on the
		 * files in each path in the list, concatenated together.
		 */
		for (pathIndex = 0; pathIndex < argList.size(); pathIndex++) {
			String pathName = argList.stringElement(pathIndex);
			if (pathName.length() == 0)
				pathName = ".";


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
					if( fArray != null ) {
						for( int idx = 1; idx <= fArray.size(); idx++) {
							Value fItem = fArray.getElement(idx);
							Value fName = fItem.getElement("NAME");
							array.addElement(fName);
						}
					}
					Value dArray = fsmRecord.getElement("DOMAINS");
					if( dArray != null) {
						for( int idx = 1; idx <= dArray.size(); idx++) {
							Value fItem = dArray.getElement(idx);
							Value fName = fItem.getElement("NAME");
							if( fName != null )
								array.addElement(new Value(fName.getString() + "/"));
						}
					}

				} catch (Exception e) {
					if( argList.session.signalFunctionErrors())
						throw new JBasicException(Status.INVPATH, pathName);
					return new Value(Value.ARRAY, null);
				}

			}else {
				String fsName = JBasic.userManager.makeFSPath( argList.session, pathName);
				final File path = new File(fsName);

				File pathList[] = path.listFiles();
				if( pathList == null ) {
					if( argList.session.signalFunctionErrors())
						throw new JBasicException(Status.INVPATH, fsName);
					return new Value(Value.ARRAY, null); /* Signal error by sending back empty reply */
				}

				for (File pathElement : pathList)
					try {
						/*
						 * Get the actual file system path from the list and convert
						 * it to a user-space name if needed. Then store it in the
						 * result set.
						 */
						String userName = pathElement.getCanonicalPath();
						userName = JBasic.userManager.makeUserPath(argList.session, userName);
						if( userName.startsWith("_"))
							continue;
						array.addElement(new Value(userName));

					} catch (final Exception e) {
						continue;
					}
			}
		}
		return array;
	}

}

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
 * @author cole
 * @version version 1.0 Jun 6, 2007
 *
 */
/**
 * This is a shell class definition for an Informat which is the
 * class used to actually implement each format. All formats are
 * subclasses of this base class.
 * @author cole
 * @version version 1.0 Jun 6, 2007
 *
 */
abstract class Informat {
	Value run( InputProcessor input, Command cmd ) {
		return null;
	}
}

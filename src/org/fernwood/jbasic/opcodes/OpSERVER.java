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

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpSERVER extends AbstractOpcode {

	/**
	 * Define a logical name in the server name space
	 */
	public final static int SERVER_DEFINE = 1;
	
	/**
	 * Print a status summary of the server state
	 */
	
	public final static int SERVER_SHOW_STATUS = 2;
	
	/**
	 * Use the integer operand as a function selector for basic SERVER management
	 * operations.
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		int command = env.instruction.integerOperand;
		
		switch( command ) {
		
		case SERVER_DEFINE:  
			Value path = env.pop();
			Value logicalName = env.pop();
			env.session.getNamespace().addLogicalName(logicalName.getString(), path.getString());
			break;
			
		case SERVER_SHOW_STATUS:

			JBasic session = env.session;
			if (!session.hasPermission(Permissions.ADMIN_SERVER)) {
				JBasic.log.permission(session, Permissions.ADMIN_SERVER);
				throw new JBasicException(Status.SANDBOX, Permissions.ADMIN_SERVER);
			}
			
			boolean remote = false;
			String mode = session.getString("SYS$MODE");
			if (mode.equals("REMOTEUSER")) {
				remote = true;
				mode = "MULTIUSER";
			}
			session.stdout.print("SERVER=" + mode);

			if (mode.equals("MULTIUSER")) {
				if (remote)
					session.stdout.println(" (REMOTE USER)");
				else
					session.stdout.println(" (CONTROLLING SESSION)");
				int count = 0;
				session.stdout.println("  STARTED="
						+ session.getString("SYS$SERVER_START"));
				session.stdout.println("  PORT="
						+ session.getString("SYS$PORT"));
				Value userList = JBasic.userManager.userList();
				if (userList == null)
					throw new JBasicException(Status.FAULT, "user manager error");
				if (userList.size() == 0) {
					session.stdout.println("  NO USERS DEFINED");
					return;
				}
				session.stdout.println("  DATABASE="
						+ JBasic.userManager.getDatabaseFileName());
				session.stdout.print("  ACTIVE USERS=");

				for (int ix = 1; ix <= userList.size(); ix++) {
					Value user = userList.getElement(ix);
					String userName = user.getElement("USER").getString();
					boolean active = user.getElement("ACTIVE").getBoolean();
					if (active) {
						if (count > 0)
							session.stdout.print(", ");
						session.stdout.print(userName);
						count++;
					}
				}
				if (count == 0)
					session.stdout.print("<NONE>");
			}
			session.stdout.println();
			return;
			
			
		default:
			throw new JBasicException(Status.FAULT, new Status(Status.INVOPARG, command));
		
		}
		return;
		
	}

}

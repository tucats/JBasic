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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Optimizer;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.FSMConnectionManager;
import org.fernwood.jbasic.runtime.JBFOutput;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.JBasicFile;
import org.fernwood.jbasic.runtime.JBasicThread;
import org.fernwood.jbasic.runtime.SimpleCipher;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpSYS extends AbstractOpcode {

	/**
	 * Spawn a subprocess to execute a host shell command
	 */
	public final static int SYS_SPAWN = 0;

	/**
	 * Clear the screen, if possible
	 */
	public final static int SYS_CLEAR_SCREEN = 1;

	/**
	 * Enable the new text formatter for tokenized statements
	 */
	public final static int SYS_FORMATTER_ON = 2;

	/**
	 * Enable the classic (old) formatter for tokenized statements
	 */
	public final static int SYS_FORMATTER_OFF = 3;

	/**
	 * Push a new SANDBOX state on the permissions stack
	 */
	public final static int SYS_SBOX_PUSH = 4;

	/**
	 * Pop the SANDBOX state, reverting to the state before the last PUSH
	 */
	public final static int SYS_SBOX_POP = 5;

	/**
	 * Turn on string pooling in the Linker.
	 */
	public final static int SYS_STRING_POOL_ON = 6;

	/**
	 * Turn off string pooling in the linker.
	 */
	public final static int SYS_STRING_POOL_OFF = 7;

	/**
	 * Use the top of stack to set the threshhold for how many copies of
	 * a string must exist in the bytecode stream to qualify for pooling
	 */

	public static final int SYS_STRING_POOL_SIZE = 8;

	/**
	 * The top of stack must contain a string that is checked to see if it
	 * is the correct password to enable root access.
	 */
	public static final int SYS_ROOT = 9;

	/**
	 * The top of the stack contains a string value that is a permission
	 * setting.  To clear the permission, prefix the permission name with
	 * "NO".
	 */
	public static final int SYS_SETPERM = 10;

	/**
	 * Show the current permissions settings.  This loads and runs the
	 * $SHOWPERMISSIONS program that must be part of the Library or
	 * Workspace.
	 */
	public static final int SYS_SHOWPERM = 11;
	
	/**
	 * Use a value on the stack to add a new optimization pattern. The
	 * value must be a record with a PATTERN and a REPLACE field, each
	 * of which is a string array containing the pattern and/or replace
	 * operation specifications.
	 */
	public static final int SYS_ADD_OPT = 12;
	
	/**
	 * Write the current pattern-matching optimizer dictionary data to
	 * a file. The file name is the string operand, or if missing, the
	 * value on top of the stack.
	 */
	public static final int SYS_DUMP_OPT = 13;
	
	/**
	 * Report on statistics for the optimization pattern database.
	 */
	public static final int SYS_STAT_OPT = 14;
	
	/**
	 * Clear the optimizer statistics data.
	 */
	public static final int SYS_CLEARSTAT_OPT = 15;
	
	/**
	 * Display and clear tokenizer statistics.
	 */
	
	public static final int SYS_TOKENSTATS = 16;

	/**
	 * Create an FSM mountpoint.
	 */
	public static final int	SYS_FSM_MOUNT = 17;
	
	/**
	 * Dump the system event log
	 */
	
	public static final int SYS_DUMP_EVENTS = 18;
	
	/**
	 * Add a string to the event log
	 */
	public static final int SYS_ADD_EVENT = 19;
	
	/**
	 * Dump event queues for all remote users and sub-threads
	 */
	public static final int SYS_DUMP_ALL_EVENTS = 20;
	
	/**
	 * Clear the event queue of all user events
	 */
	public static final int SYS_CLEAR_EVENTS = 21;
	
	
	/**
	 * Execute the _Sys call to perform miscellaneous system functions.
	 * @param env the execution environment.
	 * @throws JBasicException  An error in the count or type of argument
	 * occurred.
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final int action = env.instruction.integerOperand;
		if( action == SYS_ROOT || action == SYS_SPAWN || action == SYS_CLEAR_SCREEN)
			env.session.checkPermission(Permissions.SHELL);
		
		Value v = null;
		Value sb = null;
		Status status = null;

		switch( action ) {
		
		case SYS_CLEAR_EVENTS:
			env.session.clearEvents();
			break;
			
		case SYS_DUMP_EVENTS:
			env.session.dumpEvents();
			break;
		
		case SYS_ADD_EVENT:
			String eventDescription = null;
			
			if( env.instruction.stringValid )
				eventDescription = env.instruction.stringOperand;
			else {
				v = env.pop();
				eventDescription = v.getString();
			}
			env.session.addEvent(eventDescription);
			break;
			
		case SYS_DUMP_ALL_EVENTS:
			env.session.dumpEvents();
			
			if( env.session.getChildThreads() != null )
				for( JBasicThread s : env.session.getChildThreads().values())
					if( s.getJBasic() != null )
						s.getJBasic().dumpEvents();
			
			if( JBasic.activeSessions != null )
				for( JBasic s : JBasic.activeSessions.values()) 
					if( s != null )
						s.dumpEvents();

			break;
			
		case SYS_FSM_MOUNT:
			
			Value mountArray = env.pop();
			
			if( mountArray.getType() != Value.ARRAY)
				throw new JBasicException(Status.EXPARRAY);
			int mountArraySize = mountArray.size();
			if( mountArraySize < 1 || mountArraySize > 2 )
				throw new JBasicException(Status.ARRAYBOUNDS, mountArraySize);
			String sep = System.getProperty("file.separator");
			if( sep == null )
				sep = "/";
			String mountPoint = mountArray.getElement(1).getString();
			if(!mountPoint.startsWith(sep))
				mountPoint = sep + mountPoint;
			
			String fsmName = null;
			if( mountArraySize == 2 )
				fsmName = mountArray.getElement(2).getString();
			
			FSMConnectionManager.registerMountPoint(mountPoint, fsmName);
			
			return;
			
		case SYS_TOKENSTATS:
			
			double average = (double)Tokenizer.globalTokenCount / (double) Tokenizer.globalBufferCount;
			env.session.stdout.println("TOKENIZER STATS:");
			env.session.stdout.println("   BUFFERS PROCESSED:   " + Tokenizer.globalBufferCount);
			env.session.stdout.println("   TOKENS  PROCESSED:   " + Tokenizer.globalTokenCount);
			env.session.stdout.println("   AVERAGE TOKEN COUNT: " + average);
			env.session.stdout.println("   MAX TOKENS/BUFFER:   " + Tokenizer.globalMaxCount);
			
			Tokenizer.globalBufferCount = 0;
			Tokenizer.globalTokenCount = 0;
			return;
			
		case SYS_ADD_OPT:
			status = env.session.pmOptimizer.add(env.pop());
			if( status.failed())
				throw new JBasicException(status);
			return;
		
		case SYS_DUMP_OPT:
			if( env.instruction.stringValid)
				status = env.session.pmOptimizer.dump(env.instruction.stringOperand);
			else
				status = env.session.pmOptimizer.dump(env.pop().getString());
			if( status.failed())
				throw new JBasicException(status);
			return;
		
		case SYS_STAT_OPT:
			if( env.instruction.stringValid)
				env.session.pmOptimizer.dumpOne(env.instruction.stringOperand);
			else
				env.session.pmOptimizer.statistics();
			return;
		
		case SYS_CLEARSTAT_OPT:
			env.session.pmOptimizer.clearStatistics();
			break;
			
		case SYS_ROOT:
			Value s = env.localSymbols.findGlobalTable().findReference("SYS$ROOTUSER", false);
			if( s == null ) {
				env.localSymbols.findGlobalTable().insertReadOnly("SYS$ROOTUSER", new Value(false));
				s = env.localSymbols.findGlobalTable().findReference("SYS$ROOTUSER", false);
			}
			
			SimpleCipher encrypter = new SimpleCipher(env.session);
			String pw = encrypter.encryptedString(env.pop().getString(), "rootuser");
			s.setBoolean(pw.equals("CLRWPALHCXMNWPHNKHMHPLHHNPCMGMCC"));
			return;
			
		case SYS_STRING_POOL_ON:
		case SYS_STRING_POOL_OFF:
			Optimizer.OPT_STRING_POOL = (action == SYS_STRING_POOL_ON);
			return;

		case SYS_STRING_POOL_SIZE:
			Optimizer.STRING_POOL_THRESHHOLD = env.pop().getInteger();
			Optimizer.OPT_STRING_POOL = (Optimizer.STRING_POOL_THRESHHOLD > 0);
			return;

		case SYS_SPAWN:	// Execute arbitrary string as system command

			/*
			 * See what the argument is... it can be a STRING or an array of
			 * STRING values.
			 */
			final Value cmdArg = env.pop();
			boolean isString = false;
			String[] cmd = null;
			if( cmdArg.getType() == Value.STRING) {
				isString = true;
			}
			else if( cmdArg.getType() == Value.ARRAY) {
				cmd = new String[cmdArg.size()];
				for( int idx = 0; idx < cmd.length; idx++ ){
					cmd[idx] = cmdArg.getElement(idx+1).getString();
				}
			}
			else
				throw new JBasicException(Status.INVTYPE, cmdArg.toString());

			JBasic.log.debug("SYSTEM(" + cmd + ")");
			final ExecManager e = new ExecManager();
			
			/*
			 * Depending on the argument (a string versus array of strings), use
			 * the correct form of spawn().
			 */
			Status sts = null;
			if( isString )
				sts = e.spawn(env.session.stdout, cmdArg.getString());
			else
				sts = e.spawn(env.session.stdout, cmd);

			env.localSymbols.insert("SYS$STATUS", new Value(sts));
			if( !sts.success())
				throw new JBasicException(sts);
			return;

		case SYS_CLEAR_SCREEN:	// Clear the screen if possible.

			String clsCommand = null;
			if( System.getProperty("os.name").startsWith("Window"))
				clsCommand = "cmd /c cls";
			else
				clsCommand = "clear";
			try {
				Process p = Runtime.getRuntime().exec(clsCommand);
				p.waitFor();
			} catch (Exception e1) {
				throw new JBasicException(Status.FAULT, "error invoking " + clsCommand + e1.toString());
			}
			return;

		case SYS_FORMATTER_ON: // Set the tokenizer format mode
			Tokenizer.USE_NEW_FORMATTER = true;
			reformatCurrentProgram(env.session);
			return;

		case SYS_FORMATTER_OFF: // Set the tokenizer format mode
			Tokenizer.USE_NEW_FORMATTER = false;
			reformatCurrentProgram(env.session);
			return;

		case SYS_SBOX_PUSH: // Push sandbox mode
		case SYS_SBOX_POP: // Pop sandbox mode

			sb = env.session.globals().findReference("SYS$SANDBOX", false);

			v = env.session.globals().findReference("SYS$SANDBOX_COUNT", false);
			if( v == null ) {
				env.session.globals().insert("SYS$SANDBOX_COUNT", 0);
				env.session.globals().markReadOnly("SYS$SANDBOX_COUNT");
				v = env.session.globals().findReference("SYS$SANDBOX_COUNT", false);				
			}
			int count = v.getInteger()+ (action == SYS_SBOX_PUSH? +1 : -1);
			v.setInteger(count);
			sb.setBoolean(count > 0 );
			return;

		case SYS_SHOWPERM:
			env.push(env.session.getUserIdentity().getPermissions());
			return;
					
		case SYS_SETPERM:

			/*
			 * If we're in a sandbox, then the ADMIN_USER flag must be set
			 * for us to do this.  If we are not in a sandbox, we always
			 * allow this operation.
			
			
			sb = env.session.globals().findReference("SYS$SANDBOX", false);
			if( sb.getBoolean() )
				env.session.checkPermission("ADMIN_USER");
			 */
			
			boolean flag = true;
			String pname = null;
			if( env.instruction.stringValid)
				pname = env.instruction.stringOperand.toUpperCase();
			else {
				Value pData = env.pop();
				/* 
				 * If this is an array, we have an explicit set operation 
				 */
				if( pData.getType() == Value.ARRAY ) {
					/* First, clear all permissions */
					env.session.setPermission("ALL", false);
					/* Then explicitly set the ones you find in the array */
					for( int ix = 1; ix <= pData.size(); ix++ ) {
						env.session.setPermission(pData.getString(ix), true);
					}
					return;
				}
				/* 
				 * Not an array, so just get the name from the value and 
				 * carry on...
				 */
				
				pname = pData.getString().toUpperCase();
			}
			
			if( pname.equals("NOALL") || pname.equals("NONE")){
				pname = "ALL";
				flag = false;
			}
			else
			if( !Permissions.valid(pname) && pname.length()>2 && pname.substring(0,2).equals("NO")) {
				if( Permissions.valid(pname.substring(2))) {
					flag = false;
					pname = pname.substring(2);
				}
			}
			
			if( !pname.equals("ALL") && !Permissions.valid(pname))
				throw new JBasicException(Status.INVPERM, pname);
			
			env.session.setPermission(pname, flag);
			return;
			
		default:
			throw new JBasicException(Status.INVOPARG, action);
		}

	}

	/**
	 * @param session The JBasic session whose current program is to be
	 * reformatted.
	 */
	private void reformatCurrentProgram(JBasic session) {
		if( session.globals().getBoolean("SYS$RETOKENIZE")) {
			String name = session.globals().getString("SYS$CURRENT_PROGRAM");
			if( name != null && name.length() > 0) {
				Program p = session.programs.find(name);
				if( p != null )
					p.reFormat();
			}
		}
	}

	class StreamGobbler extends Thread {
		InputStream is;
		JBFOutput stdout;

		String type;

		StreamGobbler(final InputStream newIS, final String streamType) {
			is = newIS;
			type = streamType;
		}

		void setOutput( JBasicFile basicFile ) {
			stdout = (JBFOutput) basicFile;
		}

		public void run() {
			try {
				final InputStreamReader isr = new InputStreamReader(is);
				final BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null)
					stdout.println(type + " " + line);
			} catch (final IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	/**
	 * Object for handling execution context. Used to support spawning of commands as
	 * needed.
	 * @author tom
	 * @version version 1.1 Dec 15, 2010
	 * 
	 */
	public class ExecManager {

		/**
		 * Spawn a command string passed as the argument.  The command string is broken
		 * up using blanks to separate the command arguments; this can lead to errors on
		 * Unix shell systems... the preferred routine is spawn(..., String[] cmdArray)
		 * which accepts an array of command arguments.
		 * 
		 * @param basicFile the file used to capture output from the command
		 * @param cmdArray a String[] array containing the command. The [0] element is
		 * the command verb, and any additional string elements are arguments.
		 * @return a Status indicating if the command was run successfully.
		 * 
		 */
		Status spawn(JBasicFile basicFile, final String realCommand) {

			try {
				final String osName = System.getProperty("os.name");
				final String[] cmd = new String[3];
				boolean onWindows = false;
				final String myOS = osName;
				String winName;
				
				try {
					winName = myOS.substring(0, 7);
				}
				catch (Exception e ) {
					winName = null;
				}
				
				if (winName != null && winName.equals("Windows")) { 
					onWindows = true;
					cmd[0] = (myOS.equals("Windows 95") ? "command.com" : "cmd.exe");
					cmd[1] = "/C";
					cmd[2] = realCommand;
				}

				final Runtime rt = Runtime.getRuntime();
				Process proc = null;

				try {
					if (onWindows)
						// System.out.println("Executing " + cmd[0] + " " +
						// cmd[1] + "
						// " + cmd[2]);
						proc = rt.exec(cmd);
					else
						// System.out.println("Execing " + realCommand );
						proc = rt.exec(realCommand);
				} catch (final Throwable t) {
					final Status sts = new Status(Status.EXEC, t.getLocalizedMessage());
					return sts;
				}

				// any error message?
				final StreamGobbler errorGobbler = new StreamGobbler(proc
						.getErrorStream(), "ERROR>");

				// any output?
				final StreamGobbler outputGobbler = new StreamGobbler(proc
						.getInputStream(), "");

				/*
				 * Kick off the threads that are going to read the error and
				 * output streams and route them back to the designated output
				 * file.
				 */
				errorGobbler.setOutput(basicFile);
				errorGobbler.start();
				outputGobbler.setOutput(basicFile);
				outputGobbler.start();

				/*
				 * Let the subprocess complete its work, and then grab
				 * the completion code.
				 */
				
				final int exitVal = proc.waitFor();
				
				/*
				 * Let's be sure the output streams have been flushed by
				 * waiting for the threads that are piping the error and
				 * output streams back to us to finish...
				 */
				errorGobbler.join(0);
				outputGobbler.join(0);
				
				/*
				 * Return the process completion code, where zero means
				 * success (by convention).
				 */
				return new Status(Status.SYSTEM, exitVal);

			} catch (final Throwable t) {
				//t.printStackTrace();
				return new Status(Status.FAULT, t.toString());
			}

		}

		/**
		 * Same as spawn(..string) but instead of a single string it accepts an array
		 * of strings, which are managed as separate arguments passed to the underlying
		 * command.  This lets arguments contain spaces without freaking out the helper
		 * function in spawn(...string)
		 * 
		 * @param basicFile the file used to capture output from the command
		 * @param cmdArray a String[] array containing the command. The [0] element is
		 * the command verb, and any additional string elements are arguments.
		 * @return a Status indicating if the command was run successfully.
		 * 
		 */
		Status spawn(JBasicFile basicFile, final String[] cmdArray) {

			try {
				final String osName = System.getProperty("os.name");
				String[] windowsCmdArray = null;
				boolean onWindows = false;
				String myOS = osName;
				String winName;
				
				try {
					winName = myOS.substring(0, 7);
				} catch (Exception e) {
					winName = null;
				}
				
				if (winName != null && winName.equals("Windows")) { 
					onWindows = true;
					windowsCmdArray = new String[2 + cmdArray.length];
					windowsCmdArray[0] = (myOS.equals("Windows 95") ? "command.com" : "cmd.exe");
					windowsCmdArray[1] = "/C";
					for( int i = 0; i < cmdArray.length; i++ )
						windowsCmdArray[2+i] = cmdArray[i];
				}

				final Runtime rt = Runtime.getRuntime();
				Process proc = null;

				try {
					if (onWindows)
						proc = rt.exec(windowsCmdArray);
					else
						proc = rt.exec(cmdArray);
				} catch (final Throwable t) {
					final Status sts = new Status(Status.EXEC, t.getLocalizedMessage());
					return sts;
				}

				// any error message?
				final StreamGobbler errorGobbler = new StreamGobbler(proc
						.getErrorStream(), "ERROR>");

				// any output?
				final StreamGobbler outputGobbler = new StreamGobbler(proc
						.getInputStream(), "");

				// kick them off
				errorGobbler.setOutput(basicFile);
				errorGobbler.start();
				outputGobbler.setOutput(basicFile);
				outputGobbler.start();

				// any error???
				final int exitVal = proc.waitFor();
				
				/*
				 * Let's be sure the output streams have been flushed by
				 * waiting for the threads that are piping the error and
				 * output streams back to us to finish...
				 */
				errorGobbler.join(0);
				outputGobbler.join(0);
				
				/*
				 * All done, set the return status to show the exit value
				 * as the parameter, where 0 means success (by convention).
				 */
				return new Status(Status.SYSTEM, exitVal);

			} catch (final Throwable t) {
				//t.printStackTrace();
				return new Status(Status.FAULT, t.toString());
			}
		}
	}
}

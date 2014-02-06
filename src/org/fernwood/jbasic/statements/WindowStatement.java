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
package org.fernwood.jbasic.statements;

import javax.swing.*;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.runtime.SymbolTable;

/**
 * WINDOW statement handler. Creates a simple interactive window for JBasic
 * execution.
 * 
 * @author cole
 * @version 1.0 August 12, 2004
 * 
 */

class WindowStatement extends Statement {

	static void createGUI() {

		/*
		 * Make sure window decorations make sense.
		 */
		JFrame.setDefaultLookAndFeelDecorated(true);

		/*
		 * Create a new window "frame", which is a first-class window with title
		 * bar, borders, etc. Tell it to just ditch the window when we're done.
		 */
		final JFrame frame = new JFrame("JBasic " + JBasic.version);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		/*
		 * Create something to go in the window. JLabel is a descendant of
		 * JComponent. We have to get the content pane of the frame to add an
		 * object to it.
		 */
		final JLabel label = new JLabel("Hello World");
		frame.getContentPane().add(label);

		/*
		 * Let's add a button
		 */

		final JButton submit = new JButton("Run");
		submit.setMnemonic('r');
		frame.getContentPane().add(submit);

		frame.getContentPane().validate();
		
		/*
		 * Display the window. Pack makes the window "shrink to fit"
		 */
		//frame.pack();
		frame.setVisible(true);
		

	}

	/**
	 * Execute 'window' statement.
	 * 
	 * @param symbols
	 *            The symbol table to use to resolve identifiers, or null if no
	 *            symbols allowed.
	 * @param tokens
	 *            The token buffer being processed that contains the expression.
	 */

	public Status run(final Tokenizer tokens, final SymbolTable symbols) {

		/*
		 * This bit of magic comes from Suns' tutorial. It creates a separate
		 * runnable thread whose content is a method called run that does the
		 * work - and it's run asynch to the rest of JBasic. This means the GUI
		 * has it's own thread life, and the GUI has a measure of thread-saftey.
		 * 
		 * See
		 * http://java.sun.com/docs/books/tutorial/uiswing/learn/example1.html
		 * 
		 */
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createGUI();
			}
		});

		return new Status(Status.SUCCESS);

	}
}
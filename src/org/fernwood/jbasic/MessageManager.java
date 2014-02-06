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
 * Created on Mar 27, 2009 by tom
 *
 */
package org.fernwood.jbasic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.fernwood.jbasic.runtime.JBFOutput;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.Value;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class manages message translations for status codes.  An instance
 * of this exists for each session.  IF the session is a child thread of
 * another thread or session, then the message database is inherited from
 * the parent.  Otherwise, the $INIT_MESSAGES program is re-run for the
 * thread to rebuild the default message database.
 * 
 * @author tom
 * @version version 1.0 Mar 28, 2009
 *
 */
public class MessageManager {

	/**
	 * The name of the file containing teh XML message definitions.
	 * This file can be found using the SYS$PATH locations (typically
	 * searching the jar if we are running from one else the default
	 * working directory on the host system).
	 */
	public static final String MESSAGE_FILE_NAME = "Messages.xml";

	JBasic session;

	/**
	 * This is the list of message text conversions for this
	 * session.
	 */
	public TreeMap<String,Message> messages;

	/**
	 * For debugging purposes.
	 */
	public String toString() {
		return "MessageManager for " + session.getInstanceID();
	}

	/**
	 * Instantiate a message manager for a given session.
	 * @param parentSession
	 */
	MessageManager(JBasic parentSession) {
		session = parentSession;
		synchronized (session) {
			session.messageManager = this;
			messages = null;

			if (session.parentSession == null)

				/* No parent, reload the message file */
				try {
					initMessages();
				} catch (JBasicException e) {
					session.stdout.println("ERROR: UNABLE TO INITIALIZE MESSAGE SUBSYSTEM");
				}
				else {

					/* Make a  copy of the parent's message tree for this session's use */
					if( session.parentSession.messageManager != null)
						messages = session.parentSession.messageManager.copy();
				}
		}
	}

	/**
	 * Create a copy of the current message map, to be given to child sessions.
	 * @return
	 */
	private TreeMap<String, Message> copy() {
		TreeMap<String, Message> messageCopy = new TreeMap<String, Message>();

		messageCopy.putAll(messages);

		return messageCopy;
	}

	/**
	 * Return the count of messages currently in the static message registry
	 * 
	 * @return An integer count of the messages, or zero of the message registry
	 *         has never been initialized.
	 */
	public int getMessageCount() {
		if (messages == null)
			return 0;
		return messages.values().size();
	}

	/**
	 * Store a message string in the in-memory message table. These are used
	 * later for matching with <code>signal</code> or <code>on</code>
	 * statements. Note that if the message table has never been set up in the
	 * first place, initialize it with the predefined messages before we try
	 * this one (also required to avoid a Collections null pointer if not set up
	 * right yet).
	 * 
	 * @param code
	 *            The message code, a short mnemonic string
	 * @param text
	 *            The full message text that is used when the error code is
	 *            signaled.
	 */
	public void defineMessage(final String code, final String text) {
		synchronized( session ) {
			if (messages == null) {
				try {
					initMessages();
				} catch (JBasicException e) {
					System.out.println("Unable to load message file!");
				}
			}
			/*
			 * Message is a enclosed object in Status. So we have to create a new
			 * instance of a status object to invoke it's new method to create a
			 * sub-object of Message. Yuck.
			 */
			messages.put(code, new Message(code, text));
		}
	}


	/**
	 * Initialize the message structure. This involves creating an initialized
	 * TreeMap data structure, and then filling it in with the known message
	 * values that are considered "built-in".
	 * 
	 * @throws JBasicException  there was a fatal error accessing the system
	 * symbol table
	 */

	public void initMessages() throws JBasicException {

		/* If already initialized, do nothing */

		if (messages != null)
			return;

		messages = new TreeMap<String,Message>();
		Status sts = null;
		
		/*
		 * Plan A, load the messages.xml file from one of the path locations
		 */
		
		final Value prefixList = session.globals().reference("SYS$PATH");
		for( int ix = 0; ix < prefixList.size(); ix++ ) {
			String prefix = prefixList.getString(ix+1);
			String messageFile = prefix + MESSAGE_FILE_NAME;
			sts = loadMessageFile(messageFile);
			if( sts.success()) {
				session.globals().insert("SYS$MESSAGE_FILE", messageFile);
				return;
			}
		}


		/*
		 * Plan B, use the Messages.jbasic program
		 */
		Loader.pathLoad(session, "Messages");
		final Program msgPgm = session.programs.find("$INIT_MESSAGES");
		if (msgPgm != null) {
			msgPgm.setSystemObject(true);
			msgPgm.link(true);
			sts = msgPgm.runExecutable(session.globals(),0);
			session.globals().insert("SYS$MESSAGE_FILE", "");
		}
			
		

		/*
		 * If no messages were defined, then warn the user of a problem.
		 */
		if( session.messageManager.getMessageCount() == 0)
			JBasic.log.debug("WARNING: No valid " + MESSAGE_FILE_NAME 
					+ " file or $INIT_MESSAGES program found!");

		return;
	}

	/**
	 * Get the iterator for the key values in this MessageManager's database.
	 * @return an iterator that returns the key values, or null if there are
	 * no messages defined.
	 */
	public Iterator iterator() {
		if( messages == null )
			return null;
		return messages.values().iterator();
	}

	/**
	 * Get a message object from the message database.
	 * @param msgKey the name of the signal.
	 * @return the Message object for the given key, which contains
	 * the translated message string.
	 */
	public Message get(String msgKey) {

		JBasic s = session;

		while(s != null) {

			if( s.messageManager == null )
				return null;
			if( s.messageManager.messages != null ) {
				Message m = s.messageManager.messages.get(msgKey);
				if( m != null )
					return m;
			}
			s = s.parentSession;
		}

		return null;

	}

	/**
	 * Return flag indicating if there is a message text translation for the
	 * given message code.
	 *
	 * @param code
	 *            The String containing the code, such as "*SUCCESS" or
	 *            "SYNTAX".
	 * @return A boolean indicating if there is a message translation available,
	 *         either internally or externally. In short, if this returns true,
	 *         then a getMessage() method call will return message text.
	 */
	public boolean hasMessageText(final String code) {

		Status sts = new Status(code);
		String m = sts.getMessage(session);

		return !m.equals(code);
	}

	/**
	 * Remove a message encoding text from the runtime message manager
	 * @param name the name of the message to delete. All language translations
	 * of the given code are removed.
	 * @return a count of the number of message mappings removed.  If this is
	 * zero, then no mappings were found.
	 */
	public int removeMessage(String name) {
		if( messages == null )
			return 0;

		Iterator i = messages.keySet().iterator();

		String match = null;
		int matchLen = 0;
		char last = name.charAt(name.length()-1);
		if(  last == '*' || last == '$') 
			match = name.substring(0, name.length()-2).toUpperCase();
		else
			match = name.toUpperCase() + "(";

		matchLen = match.length();

		Vector<String> codeList = new Vector<String>();

		/*
		 * Make a list of the matching items in the message data
		 */
		while( i.hasNext()) {
			String code = (String) i.next();
			if( code.length() <= matchLen)
				continue;
			String test = code.substring(0, matchLen);
			if( test.equals(match)) 
				codeList.add(code);
		}

		/*
		 * Now go through that list and delete the items. You can't
		 * do this from the above loop because the iterator won't
		 * tolerate the list being modified mid-iteration.
		 */

		for( int idx = 0; idx < codeList.size(); idx++ ) {
			String code = codeList.get(idx);
			messages.remove(code);
		}
		return codeList.size();
	}

	/**
	 * Given a message file path, attempt to laod the file.  The name can
	 * start with "@" to indicate a resource in the jar file, or it can
	 * start with "!" to indicate that both the resource and file system
	 * should be checked.  If neither character is present then the name
	 * indiates a path in the file system.
	 * @param fname The name of the file to read
	 * @return a Status indicating if the file was found, and if the
	 * XML contents are correctly formed.
	 */
	public Status loadMessageFile( String fname ) {

		InputStream is = null;

		try {

			if( fname.charAt(0) == '!') {
				is = JBasic.class.getResourceAsStream("/" + fname.substring(1));
				if( is == null ) {
					File f = new File(fname.substring(1));
					try {
					is = new FileInputStream(f);
					} catch (Exception e ) {
						is = null;
					}
				}
			}

			if( is == null ) {

				if( fname.charAt(0) == '@' ) {
					is = JBasic.class.getResourceAsStream(fname.substring(1));
				}
				else {
					File f = new File(fname);
					is = new FileInputStream(f);
				}
			}
		} catch (FileNotFoundException e) {
			return new Status(Status.FILENF, fname);
		}
		if( is != null )
			return loadMessageXML(is);

		return new Status(Status.FILENF, fname);
	}

	/**
	 * Load message definitions from a String which must contain the
	 * fully formed XML definition.  This is converted to an input
	 * stream and processed as if it was the contents of a file.
	 * @param data the String containing the XML data
	 * @return Status indicating if the XML was correclty formed.
	 */
	public Status loadMessageXML( String data ) {
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(data.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return new Status(Status.FAULT, e.toString());
		}
		return loadMessageXML(is);
	}
	/**
	 * Given an input stream containing an XML message definition, load the message(s) from
	 * the data.
	 * @param is a InputStream, typically created from a string or as a file
	 * @return a Status indicating if the parse went correctly.
	 */
	private Status loadMessageXML( InputStream is ) {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder dBuilder = null;

			/*
			 * Parse the data we were given as an input stream and build a DOM
			 * in memory.
			 */
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(is);

			/*
			 * Traverse the tree looking for <Message> elements>
			 */

			NodeList nodes = doc.getElementsByTagName("Message");

			// JBasic.log.info("DEFMSG: found " + nodes.getLength() + " element(s)");

			/*
			 * Loop over the list to process each one.
			 */

			for( int ix = 0; ix < nodes.getLength(); ix++ ) {
				Node node = nodes.item(ix);

				boolean fSuccess = false;
				String name = null;

				/*
				 * Fetch the name= and success= attributes, and set
				 * the local flags accordingly.
				 */
				NamedNodeMap attrs = node.getAttributes();
				Node attr = attrs.getNamedItem("name");
				if( attr != null )
					name = attr.getNodeValue();
				attr = attrs.getNamedItem("success");
				if( attr != null) {
					String s = attr.getNodeValue();
					if( s.equalsIgnoreCase("TRUE") || s.equalsIgnoreCase("YES"))
						fSuccess = true;
				}

				if( fSuccess && name != null && name.charAt(0) != '*')
					name = "*" + name;

				if( JBasic.log != null )
					JBasic.log.debug("DEFMSG: DEFINE " + name);
				
				NodeList msgs = node.getChildNodes();
				if( msgs == null )
					return new Status(Status.EXPMESSAGE);
				for( int in = 0; in < msgs.getLength(); in++ ) {
					Node msg = msgs.item(in);
					int t = msg.getNodeType();
					String lang = null;
					String text = null;

					if( t == Node.ELEMENT_NODE) {
						lang = msg.getNodeName();
						msg = msg.getFirstChild();
						text = msg.getNodeValue();

						text = new Value(text).denormalize();

						if( JBasic.log != null )
							JBasic.log.debug("DEFMSG:   (" + lang + ") " + text);
						defineMessage(name+"("+lang+")", text);
					}

				}
			}
			/*
			 * Force some cleanup.
			 */
			doc = null;
			dBuilder = null;

		} catch (Exception e) {
			return new Status(Status.XML, e.toString());
		}
		return new Status();
	}

	/**
	 * Write the current message data to a file.
	 * @param fn a String with the full path name of the file in which 
	 * to save the XML message database
	 * @return status of the save operation
	 */
	public Status saveMessageXML(String fn) {
		Status sts = new Status();
		JBFOutput f = new JBFOutput(session);

		try {
			f.open(new Value(fn), null);
		} catch (JBasicException e) {
			return new Status(Status.FILENF, fn);
		}

		Date d = new Date();

		f.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		f.println("<!--JBasic message definition file, created " + d.toString() + " -->");

		f.println("<MessageDefinitions>");
		Iterator i = iterator();
		String code = "";

		while( i.hasNext()) {
			Message m = (Message)i.next();
			String key = m.getMessageCode();
			int pp = key.lastIndexOf('(');
			String newCode = key.substring(0, pp);
			if( newCode.equals(code)) {

			} else {
				if( !code.equals(""))
					f.println("  </Message>");
				code = newCode;
				f.println("  <Message name=\"" + newCode + "\">");
			}
			String lang=key.substring(pp+1, pp+3);

			StringBuffer text = new StringBuffer();
			for( int n = 0; n < m.mText.length(); n++ ) {
				String c = m.mText.substring(n,n+1);
				try {
					byte[] bytes = c.getBytes("UTF8");
					if( bytes.length == 1 ) 
						text.append(c);
					else {
						text.append("\\u");
						String h = Integer.toHexString(c.codePointAt(0));
						while( h.length() < 4 )
							h = "0" + h;
						text.append(h);
					}
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			f.println("    <" + lang + ">" + text.toString() + "</" + lang + ">");
		}
		if( !code.equals(""))
			f.println("  </Message>");
		f.println("</MessageDefinitions>");
		f.close();

		return sts;
	}

	/**
	 * Clear all messages for this session.
	 */
	public void clearMessages() {
		messages = null;
	}
}

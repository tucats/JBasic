//License
/***
 * Java TelnetD library (embeddable telnet daemon)
 * Copyright (c) 2000-2005 Dieter Wimberger 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the author nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ***/

package net.wimpi.telnetd.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.Properties;

import net.wimpi.telnetd.BootException;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.runtime.JBFOutput;

/**
 * Class that implements a <tt>PortListener</tt>.<br>
 * If available, it accepts incoming connections and passes them to an
 * associated <tt>ConnectionManager</tt>.
 * 
 * @author Dieter Wimberger
 * @version 2.0 (16/07/2006)
 * @see net.wimpi.telnetd.net.ConnectionManager
 */
public class PortListener implements Runnable {

	private static final JBFOutput log = JBasic.log;

	private String m_Name;

	private int m_Port; // port number running on

	private int m_FloodProtection; // flooding protection

	private ServerSocket m_ServerSocket = null; // server socket

	private Thread m_Thread;

	private ConnectionManager m_ConnectionManager; // connection management
													// thread

	private boolean m_Stopping = false;

	private boolean m_Available; // Flag for availability

	/**
	 * Constructs a PortListener instance.<br>
	 * Its private because its called by a factory method.
	 * 
	 * @param port
	 *            int that specifies the port number of the server socket.
	 * @param floodprot
	 *            that specifies the server socket queue size.
	 */
	private PortListener(final String name, final int port, final int floodprot) {
		m_Name = name;
		m_Available = false;
		m_Port = port;
		m_FloodProtection = floodprot;

	}// constructor

	/**
	 * Returns the name of this <tt>PortListener</tt>.
	 * 
	 * @return the name as <tt>String</tt>.
	 */
	public String getName() {
		return m_Name;
	}// getName

	/**
	 * Tests if this <tt>PortListener</tt> is available.
	 * 
	 * @return true if available, false otherwise.
	 */
	public boolean isAvailable() {
		return m_Available;
	}// isAvailable

	/**
	 * Sets the availability flag of this <tt>PortListener</tt>.
	 * 
	 * @param b
	 *            true if to be available, false otherwise.
	 */
	public void setAvailable(final boolean b) {
		m_Available = b;
	}// setAvailable

	/**
	 * Starts this <tt>PortListener</tt>.
	 */
	public void start() {
		log.debug("start()");
		m_Thread = new Thread(this);
		m_Thread.start();
		m_Available = true;
	}// start

	/**
	 * Stops this <tt>PortListener</tt>, and returns when everything was
	 * stopped successfully.
	 */
	public void stop() {
		log.debug("stop()::" + this.toString());
		// flag stop
		m_Stopping = true;
		m_Available = false;
		// take down all connections
		if( m_ConnectionManager != null)
			m_ConnectionManager.stop();

		if( m_ServerSocket != null)
			// close server socket
			try {
				m_ServerSocket.close();
			} catch (final IOException ex) {
				log.error("stop()", ex);
			}

		// wait for thread to die
		try {
			if( m_Thread != null )
				m_Thread.join();
		} catch (final InterruptedException iex) {
			log.debug("stop() interrupted");
		}

		log.info("stop()::Stopped " + this.toString());
	}// stop

	/**
	 * Listen constantly to a server socket and handles incoming connections
	 * through the associated {a:link ConnectionManager}.
	 * 
	 * @see net.wimpi.telnetd.net.ConnectionManager
	 */
	public void run() {
		try {
			/*
			 * A server socket is opened with a connectivity queue of a size
			 * specified in int floodProtection. Concurrent login handling under
			 * normal circumstances should be handled properly, but denial of
			 * service attacks via massive parallel program logins should be
			 * prevented with this.
			 */
			m_ServerSocket = new ServerSocket(m_Port, m_FloodProtection);

			// log entry
			final Object[] args = { new Integer(m_Port),
					new Integer(m_FloodProtection) };
			log.info(MessageFormat.format(logmsg, args));

			do {
				try {
					final Socket s = m_ServerSocket.accept();
					if (m_Available) {
						m_ConnectionManager.makeConnection(s, m_Port);
					} else {
						// just shut down the socket
						s.close();
					}
				} catch (final SocketException ex) {
					if (m_Stopping) {
						// server socket was closed blocked in accept
						log.debug("run(): ServerSocket closed by stop()");
					} else {
						log.error("run()", ex);
					}
				}
			} while (!m_Stopping);

		} catch (final IOException e) {
			log.error("run()", e);
		}
		log.debug("run(): returning.");
	}// run

	/**
	 * Returns reference to ConnectionManager instance associated with the
	 * PortListener.
	 * 
	 * @return the associated ConnectionManager.
	 */
	public ConnectionManager getConnectionManager() {
		return m_ConnectionManager;
	}// getConnectionManager

	/**
	 * Factory method for a PortListener instance, returns an instance of a
	 * PortListener with an associated ConnectionManager.
	 * @param name Listener name for debugging purposes
	 * 
	 * @param settings
	 *            Properties that contain all configuration information.
	 * @return a Port Listener object
	 * @throws BootException if a connection failure occurs
	 */
	public static PortListener createPortListener(final String name,
			final Properties settings) throws BootException {

		PortListener pl = null;

		try {
			// 1. read settings of the port listener itself
			final int port = Integer.parseInt(settings.getProperty(name
					+ ".port"));
			final int floodprot = Integer.parseInt(settings.getProperty(name
					+ ".floodprotection"));

			if (new Boolean(settings.getProperty(name + ".secure"))
					.booleanValue()) {
				// do nothing for now, probably set factory in the future
			}
			pl = new PortListener(name, port, floodprot);
		} catch (final Exception ex) {
			log.error("createPortListener()", ex);
			throw new BootException(
					"Failure while creating PortListener instance:\n"
							+ ex.getMessage());
		}

		// 2. factorize a ConnectionManager, passing the settings, if we do not
		// have one yet
		if (pl.m_ConnectionManager == null) {
			pl.m_ConnectionManager = ConnectionManager.createConnectionManager(
					name, settings);
			try {
				pl.m_ConnectionManager.start();
			} catch (final Exception exc) {
				log.error("createPortListener()", exc);
				throw new BootException(
						"Failure while starting ConnectionManager watchdog thread:\n"
								+ exc.getMessage());
			}
		}
		return pl;
	}// createPortListener

	private static final String logmsg = "Listening to Port {0,number,integer} with a connectivity queue size of {1,number,integer}.";

}// class PortListener

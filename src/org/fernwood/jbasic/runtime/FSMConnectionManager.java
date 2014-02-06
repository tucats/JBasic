package org.fernwood.jbasic.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.fernwood.jbasic.JBasic;

/**
 * The FSMConnectionManager provides helper functions for a program to
 * specify a URL style of identifying the server, and locates optional
 * local user authentication information to pass to the FSM nameserver
 * as part of server authentication operations.
 * <p>
 * A connection to the FSM name server (where most metadata operations
 * are performaned) is idenitifed by five pieces of information:
 * <p>
 * <li>host name</li>
 * <li>port number</li>
 * <li>user name</li>
 * <li>password</li>
 * <li>default directory <i>optional</i></li>
 * 
 * This information is identified as a string, which can have two
 * possible formats -- a URL or a connection string.  The URL format
 * must begin with the prefix fsm:// which indicates that it is a
 * FileSystemManager URL.  If the string does not begin this way, it
 * must be a connection string.
 * <p>
 * A URL is of the form:
 * <br>
 * <code> &nbsp; &nbsp; &nbsp; fsm://<i>user</i>:<i>password</i>@<i>hostname</i>:<i>port</i>/<i>default-directory</i>
 * </code>
 * <p>
 * A connection string is a sequence of keywords followed by "=" and a value,
 * with each pair separated from the others by a comma. For example, such as 
 * <code> &nbsp; &nbsp; &nbsp; "USER=tom,PASSWORD=jazzy,HOST=asdgrd01,port=6500"
 * </code>
 * 
 * <p>
 * In a connection string, the keywords can be upper or lower case.  The
 * data values for username, host, and of course port number are case-insensitive.
 * The password and default directory are case-sensitive.
 * <p>
 * <br>
 * If any field is not given, then a default is chosen:
 * <br>
 * <li>user - the current operating system username</li>
 * <li>password - if a <code>.fsmrc</code> file exists, it is searched for a password</li>
 * <li>hostname - the current local host is assumed</li>
 * <li>port - the default port number is 6500</li>
 * <li>default-directory - the default directory is /</li>
 * 
 * <p>
 * 
 * @author cole
 *
 */
public class FSMConnectionManager {
	
	String username;
	
	String password;
	
	String host;
	
	int port;

	private String path;
	
	String error;
	
	int encodeVersion;
	
	JBasic session;
	
	static HashMap mountPointList = new HashMap<String,String>();
	
	/**
	 * Create a connection name object that parses a connection string into
	 * its constituent parts.
	 * 
	 * @param connectionString the String containing either an fsm:// URL or
	 * a plain text connection string.
	 */
	public FSMConnectionManager( String connectionString ) {
		username = System.getProperty("user.name");
		password = "?";
		host = "localhost";
		port = 6500;
		error = null;
		setPath(null);
		encodeVersion = 0;
		parse( connectionString );
	}
	

	/**
	 * Create a connection name object that parses a connection string into
	 * its constituent parts.
	 * @param theSession the enclosing JBasic session or null
	 * @param connectionString the String containing either an fsm:// URL or
	 * a plain text connection string.
	 */
	public FSMConnectionManager( JBasic theSession, String connectionString ) {
		session= theSession;
		username = username();
		password = "?";
		host = "localhost";
		port = 6500;
		error = null;
		setPath(null);
		encodeVersion = 0;
		parse( connectionString );
	}
	
	/**
	 * Create a generic connection object with all the defaults filled in.
	 */
	public FSMConnectionManager() {
		username = System.getProperty("user.name");
		password = "?";
		host = "localhost";
		port = 6500;
		error = null;
		setPath(null);
		encodeVersion = 0;
	}

	/**
	 * Create a generic connection object with all the defaults filled in.
	 * @param theSession the parent session, if any
	 */
	public FSMConnectionManager(JBasic theSession ) {
		session = theSession;
		username = username();
		password = "?";
		host = "localhost";
		port = 6500;
		error = null;
		setPath(null);
		encodeVersion = 0;
	}

	/**
	 * Bind a session to this connection if needed.
	 * @param theSession JBasic session to gather user info from
	 */
	public void setSession( JBasic theSession ) {
		session = theSession;
		username = username();
	}
	
	/**
	 * Set a non-standard password encoding version number.
	 * @param seed any integer value.  This must match the
	 * encoding version set in the FSM server.
	 */
	public void setEncodeVersion( int seed ) {
		encodeVersion = seed;
	}
	
	/**
	 * Convert a connection object to a string, which could be used
	 * to open the connection again later if needed.
	 */
	public String toString() {
		StringBuffer buff = new StringBuffer();
		
		if( username != null || username.length() > 0 ) 
			buff.append("UN=" + username);
		
		if( password != null && password.length() > 0 )  {
			if( buff.length() > 0 )
				buff.append(",");
			buff.append("PW=" + password);
		}
		
		if( buff.length() > 0 )
			buff.append(",");

		if( host == null || host.length() == 0 ) 
			buff.append("HOST=localhost");
		else 
			buff.append("HOST=" + host );
		
		
		if( port > 0 )
			buff.append(",PORT=" + Integer.toString(port));
		
		if( getPath() != null && getPath().length() > 0 ) {
			buff.append(",PATH=" + getPath());
		}
		return buff.toString();
	}
	
	/**
	 * Given a string, determine if it is a pseudo-mountpoint and
	 * convert it to the appropriate URL string.
	 * @param source the input string to convert
	 * @return the output string, or null if it cannot be converted
	 * because it is not a pseudo-mountpoint.
	 */
	
	static public String convertToURL( String source ) {

		/*
		 * If it's already a URL, then we're done.
		 */
		if( source.toLowerCase().startsWith("fsm://"))
			return source;

		/*
		 * Determine the separator character we'll use.
		 */
		String separator = System.getProperty("file.separator");
		if( separator == null )
			separator = "/";
		
		/*
		 * If it's a relative file name (no leading separator)
		 * then it cannot be a pseudo-mount point.  Also, if 
		 * it doesn't have at least one directory path at the
		 * start, it cannot be a mount point.
		 */
		
		if( !source.startsWith(separator))
			return null;
		
		int next = source.substring(1).indexOf(separator);
		if( next < 0)
			return null;
		
		String mountName = source.substring(0,next+1);
		
		/*
		 * Search the list of registered mount points to see if any of them
		 * contains a match.
		 */
	
		String prefix = (String) mountPointList.get(mountName);
		if( prefix == null)
			return null;
		
		/*
		 * If the URL doesn't have the trailing separator and the path doesnt
		 * start with one, add a separator now.
		 */
		if( !prefix.endsWith(separator) && !source.substring(next+1).startsWith(separator))
			prefix = prefix + separator;
		
		String URL = prefix + source.substring(next+1);
		JBasic.log.debug("FSM virtual mount point converts to " + URL);
		return URL;
		
	}
	
	/**
	 * Register a new name with the mount point registry
	 * @param name the name of the mountpoint
	 * @param prefix the FSM prefix to use instead, or null
	 * if the corresponding item is to be deleted.
	 */
	static public void registerMountPoint( String name, String prefix ) {
		if( prefix != null ) {
			FSMConnectionManager c = new FSMConnectionManager(prefix);
			if(c.host == null || c.host.length() == 0)
				c.host = "localhost";
			prefix = "fsm://" + c.username + "@" + c.host + ":" + c.port;
			mountPointList.put( name, prefix);
		}
		else
			mountPointList.remove(name);
	}
	
	/**
	 * Get an iterator that can traverse the list of registered mount points.
	 * @return Iterator
	 */
	static public Iterator mountPointIterator() {
		return mountPointList.keySet().iterator();
	}
	
	/**
	 * Given a mount point name, return the FSM URL
	 * @param name the virtual mount point
	 * @return string containing the matching URL.
	 */
	static public String findMountPoint( String name ) {
		return (String) mountPointList.get(name);
	}
	
	
	/**
	 * Parse a connection string and set the object fields
	 * accordingly.
	 * @param cnx The connection String.
	 */
	public void parse( String cnx ) {
		
		String newCnx = convertToURL(cnx);
		if( newCnx != null )
			cnx = newCnx;
		
		if( cnx.toLowerCase().startsWith("fsm://"))
			parseURL(cnx);
		else
			parseConnection(cnx);
		
		/*
		 * If the password is unspecified, try to see if we can read
		 * it from the user's private stash... each line in the file must
		 * be a username:password pair and we pick the one that matches
		 * the username setting.
		 */
		if( password.equals("?")) {
			String passFileName = System.getProperty("user.home") + "/.fsmrc";
			File passFile = new File(passFileName);
			if( passFile.exists()) {
				try {
					String buff=null;
					//System.out.println("DEBUG: reading password file, user:" + username);
					BufferedReader in = new BufferedReader(new FileReader(passFileName));
					while( true ) {
						buff = in.readLine();
						if( buff == null )
							break;
						//System.out.println("Reading line:" + buff);
						if( buff.startsWith( "version ")) {
							encodeVersion = Integer.parseInt(buff.substring(7).trim());
						}
						else {
							int colon = buff.indexOf(':');
							if( colon >= 0) {
								if( username.equals(buff.substring(0,colon))) {
									password = buff.substring(colon+1);
									if( !password.startsWith("@"))
										password = "@" + getHash(password);
									//System.out.println("DEBUG: password:" + password);
									break;
								}
							}
						}
					}
					in.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

	}
	
	/**
	 * Hash a password string. This is used to convert the security token into a
	 * text string representing a one-way hash of the string. The hash code must
	 * be known by the client to authenticate. The encodeVersion field is used
	 * as part of the "salt" for this one-way hash value.
	 * 
	 * @param password
	 *            the password string to hash
	 * @return an encoded hash of the password string.
	 */
	private String getHash(String password) {

		final String salt = "alpha-biscuit-circuit-damage-elfen-fish-version"
				+ Integer.toString(encodeVersion);
		if (password == null)
			return "";

		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.reset();
			digest.update(salt.getBytes("UTF-8"));
			final byte[] input = digest.digest(password.getBytes("UTF-8"));
			final StringBuffer result = new StringBuffer();
			for (final byte b : input) {
				result.append(Integer.toHexString(b + 128));
			}
			return result.toString();
		} catch (final Exception e) {
			return "";
		}
	}
	private void parseURL(String cnx) {
		
		try {
			
			if( !cnx.toLowerCase().startsWith("fsm://")) {
				error = "- invalid protocol in URL: " + cnx;
				return;
			}
			cnx = "http://" + cnx.substring(6);
			
			//System.out.println("URL=" + cnx);
			
			URL url = new URL(cnx);
			
			host = url.getHost();
			port = url.getPort();
			username = url.getUserInfo();
			setPath(url.getPath());
			if( getPath() == null || getPath().length()== 0)
				setPath("/home");
			
			
			if( username == null ) {
				username = username();
			}
			else {
				int colon = username.indexOf(':');
			
				if( colon >= 0) {
					password = username.substring(colon+1);
					username = username.substring(0,colon);
				}
			}
			
		} catch (MalformedURLException e) {
			error = "- Invalid URL " + cnx;
			return;
		}
		if( port < 0 )
			port = 6500;

	}
	
	private void parseConnection( String cnx) {
		
		username = username();
		password = "?";
		host = "localhost";
		port = 6500;

		class Keyword {
			String name;
			int id;
			Keyword(String theName, int theID) {
				name = theName;
				id = theID;
			}
		}
		
		Keyword[] keywords = new Keyword[] {
				new Keyword("USER", 1),
				new Keyword("U", 1),
				new Keyword("UN", 1),
				new Keyword("PASSWORD", 2),
				new Keyword("PW", 2),
				new Keyword("HOST", 3),
				new Keyword("H", 3),
				new Keyword("SERVER", 3),
				new Keyword("S", 3),
				new Keyword("PORT", 4 ),
				new Keyword("P", 4),
				new Keyword("PATH", 5),
				new Keyword("FILE", 5),
				new Keyword("CD", 5)
		};
		
		
		StringTokenizer tokens = new StringTokenizer(cnx, ",");
		error = null;
		
		while( tokens.hasMoreTokens()) {
			
			StringTokenizer pair = new StringTokenizer(tokens.nextToken(), "=");
			String keyword = pair.nextToken().toUpperCase();
			
			if( !pair.hasMoreTokens()) {
				error = "- missing '=' in connection string";
				return;
			}
			
			String value = pair.nextToken();
			
			/* Now we have a keyword and value; see what the keyword is. */
			
			int found = 0;
			for( Keyword k : keywords ) {
				if( k.name.equals(keyword)) {
					found = k.id;
					break;
				}
			}
			if( found == 0 ) {
				error = "- unexpected connection string keyword " + keyword;
				return;
			}
			
			switch( found ) {
			case 1:	
				username = value;
				break;
			case 2:
				password = value;
				break;
			case 3:
				host = value;
				break;
			case 4:
				try {
				port = Integer.parseInt(value);
				} catch (Exception e ) {
					port = 0;
					error = "- invalid PORT designation " + value;
					return;
				}
				break;
			case 5:
				setPath(value);
			}
		}
		
	}

	/**
	 * Utility routine to determine if a path string is really an FSM URL
	 * or not.
	 * @param path string to test
	 * @return true if the path is an FSM:// url
	 */
	static public boolean isFSMURL( String path ) {
		
		if( convertToURL( path ) != null )
			return true;
		
		/*
		 * It's not a registered pseudo-mountpoint so check for an explicit
		 * URL indicating FSM as the handler.
		 */
		if( path.length() <= 6)
			return false;
		return path.substring(0, 6).equalsIgnoreCase("FSM://");
	}
	
	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}
	
	private String username() {
		
		if( session != null ) {
			try {
			String mode = session.globals().findReference("SYS$MODE", false).getString();
			if( mode.equalsIgnoreCase("REMOTEUSER"))
				return session.globals().findReference("SYS$USER", false).getString();
			} catch (Exception e ) {
				return "-no username found-";				
			}
		}
		return System.getProperty("user.name");
	}
}

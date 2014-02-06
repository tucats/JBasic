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
package org.fernwood.jbasic.runtime;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.Expression;
import org.fernwood.jbasic.compiler.Tokenizer;
import org.fernwood.jbasic.value.Value;

/**
 * 
 * DATABASE version of JBasicFile. This class supports DATABASE file I/O, which
 * means that data is accessed via an externally loaded driver. The database is
 * identified via a connection string which is used in the open statement to
 * identify the driver, host, path, username, and password.
 * <p>
 * The driver must be a loaded class. The loading of classes is managed
 * automatically by including the name of the driver class in the system
 * variable SYS$DRIVERS which is an array of strings which contain driver class
 * names. All classes in this list are loaded if possible before a database is
 * opened, making them avaialble to the JDBC driver.
 * <p>
 * 
 * @author cole
 * 
 */
public class JBFDatabase extends JBasicFile {

	/**
	 * The JDBC Connection handle. This is what is filled in by an open()
	 * operation, and is a prerequisite for being able to perform any other FILE
	 * IO operations on a DATABASE file object.
	 */
	Connection connectionHandle;

	/**
	 * The JDBC statement handle. This is created each time a PRINT statement
	 * directs a string to the DATABASE file. The statement is compiled and
	 * executed, and optionally a result set may be created.
	 */
	Statement statementHandle;

	/**
	 * This is the result set, which is created by a statement that is a query.
	 * Not all statements will generate a result set. The result set is what is
	 * used to step through the data via a GET operation, and is used to test
	 * for EOF(). And EOF() will be reported if the result set is empty or if it
	 * does not exist.
	 */
	ResultSet resultSetHandle;

	/**
	 * This is a RECORD value that contains a map of the fields in the current
	 * result set. It is created once when the result set is created by a
	 * statement execution, and is used by the _GET bytecode to map result set
	 * fields to local variables. The format of this RECORD is identical to the
	 * format of a RECORD statement used for BINARY file I/O.
	 */
	Value fieldList;

	/**
	 * This is the command buffer used to accumulate command text for execute
	 * operations. We accumulate until a carriage control string is sent, which
	 * tells us to execute the buffer we've built.
	 */
	String commandBuffer;

	/**
	 * Default constructor for a database file object.
	 * 
	 * @param jb the controlling session that owns the file.
	 */
	public JBFDatabase(final JBasic jb) {
		super(jb);
		mode = MODE_DATABASE;
	}

	/**
	 * Open the database, using a provided name which is the JDBC connection
	 * string. This operation must be performed before the database can be used
	 * for any I/O operation.
	 * <p>
	 * <br>
	 * Implicit parameters include the system variables
	 * <code>SYS$DB_USERNAME</code> and <code>SYS$DB_PASSWORD</code>. These
	 * might be set by the user, but are more likely set as a result of the OPEN
	 * statement having a USER and/or PASSWORD clause. If there is no username
	 * specification, the current username of the process is assumed, with an
	 * empty password string.
	 * <p>
	 * 
	 * @param fn
	 *            The JDBC connection info indicating the driver and the
	 *            database to be accessed. This can be a string containing a
	 *            formalized connection string, or it can be a RECORD object
	 *            with fields for each part of the connection info (such as
	 *            HOST, DRIVER, PATH)
	 * @throws JBasicException if a SQL error occurs or the database table is
	 * not accessible.
	 */

	public void open(final Value fn, final SymbolTable symbols) throws JBasicException {

		connectionHandle = null;
		statementHandle = null;
		resultSetHandle = null;

		if( fn.getString().equalsIgnoreCase(JBasic.CONSOLE_NAME)) {
			throw new JBasicException(Status.FILECONSOLE);
		}

		String extName = " ";
		boolean isDSN = false;
		if (fn.getType() == Value.RECORD) {
			fname = fn.toString();
			isDSN = true;
		} else
			extName = fname = fn.getString();

		if( fn.getString().equalsIgnoreCase(JBasic.CONSOLE_NAME)) {
			throw new JBasicException(Status.FILECONSOLE);
		}

		mode = MODE_DATABASE;
		type = OTHER;

		/*
		 * Make sure that all driver(s) requested by the user are available.
		 * This is an array of strings called SYS$DRIVERS. The normal convention
		 * for how drivers become known to the DriverManager is to use
		 * Class.forName to scan over them, forcing the loader to make sure they
		 * are loaded and available.
		 */

		Value driverList;
		try {
			driverList = jbenv.globals().reference("SYS$DRIVERS");
		} catch (JBasicException e1) {
			driverList = null;
		}

		if (driverList != null)
			if (driverList.isType(Value.ARRAY) & driverList.updated())
				for (int ix = 1; ix <= driverList.size(); ix++) {
					final String driverName = driverList.getString(ix);

					try {
						final Class c = Class.forName(driverName);
						if (c == null) {
							lastStatus = new Status(Status.JDBC, "driver "
									+ driverName + " not found in class path");
							throw new JBasicException( lastStatus );
						}

					} catch (final java.lang.ClassNotFoundException e) {
						lastStatus = new Status(Status.JDBC, "driver "
								+ driverName + " not found in class path, " + e);
						throw new JBasicException(lastStatus);
					}
				}

		/*
		 * Get user name info. By default, there is no password and the user name
		 * is the current user's name. This can be overridden by using variables
		 * SYS$DB_USERNAME and SYS$DB_PASSWORD, which are created by using the
		 * USER and PASSWORD options in the OPEN statement, or could be created
		 * manually by the user (why?). If they were created by the OPEN
		 * statement, they'll get deleted after the open completes by additional
		 * byte code instructions.
		 */

		String cnUser = null;
		String cnPass = null;
		cnUser = symbols.getString("__USERNAME");
		cnPass = symbols.getString("__PASSWORD");
		
		if (cnUser == null)
			cnUser = System.getProperty("user.name");

		if (cnPass == null)
			cnPass = "";

		/*
		 * The name can be a DSN (database shortcut name) that starts with an
		 * "@" and contains an expression after it. The expression is usually
		 * the name of a record, but it might be the record itself. In either
		 * case, it's a record with fields UN (username), PW (password), and CN
		 * (connection string)
		 * 
		 * Note that we might get here by having just been given a record
		 * outright as the name (isDSN == true) which is great as well, we can
		 * just skip the evaluation of the string.
		 */

		if (isDSN || (extName.charAt(0) == '@')) {

			Value dsn = fn;

			if (!isDSN) {
				final Tokenizer tokens = new Tokenizer(extName.substring(1));
				final Expression exp = new Expression(jbenv);
				dsn = exp.evaluate(tokens, symbols);
				if (exp.status.failed()) {
					lastStatus = new Status(Status.JDBC, "invalid DSN, "
							+ exp.status.toString());
					throw new JBasicException(lastStatus);
				}
				
			}

			if (dsn.getType() != Value.RECORD) {
				lastStatus = new Status(Status.JDBC, "invalid DSN " + extName);
				throw new JBasicException(lastStatus);
			}

			Value un = dsn.getElement("UN");
			if (un == null)
				un = dsn.getElement("USER");
			if (un == null) {
				lastStatus = new Status(Status.JDBC,
						"invalid DSN, missing UN field");
				throw new JBasicException(lastStatus);
				}

			Value pw = dsn.getElement("PW");
			if (pw == null)
				pw = dsn.getElement("PASSWORD");

			if (pw == null) {
				 lastStatus = new Status(Status.JDBC,
						"invalid DSN, missing PW field");
				 throw new JBasicException(lastStatus);
			}

			Value cn = dsn.getElement("CN");
			if (cn == null)
				cn = new Value("");

			cnUser = un.getString();
			cnPass = pw.getString();
			extName = cn.getString();

			/*
			 * If there is a DRIVER field then it tells us we have a decomposed
			 * connection string that we must assemble ourselves.
			 */
			Value f = dsn.getElement("DRIVER");
			if (f != null) {

				final String driver = f.getString();

				String host = "localhost";
				f = dsn.getElement("HOST");
				if (f == null)
					f = dsn.getElement("SERVER");
				if (f != null)
					host = f.getString();

				String file = "";
				f = dsn.getElement("PATH");
				if (f == null)
					f = dsn.getElement("FILE");
				if (f != null)
					file = f.getString();

				extName = "jdbc:" + driver + ":" + host + ":" + file;

				// System.out.println("DEBUG: extName = " + extName);
			}
		}

		/*
		 * Now that we've done our best to load the driver(s), let's invite the
		 * connection manager to create a connection to one based on the
		 * contents of the connection string and our identity information.
		 */
		try {
			connectionHandle = DriverManager.getConnection(extName, cnUser,
					cnPass);
		} catch (final Exception e) {

			lastStatus = new Status(Status.JDBC, "connection error " + e);
			throw new JBasicException(lastStatus);
		}
		register();
		fileID.setElement(new Value(Value.ARRAY, null), "MAP");
		fileID.setElement(new Value(""), "QUERY");
		lastStatus = new Status();
		return;

	}

	/**
	 * Process a string as a SQL command. The string may be executed immediately
	 * or it may be concatenated to a string already being processed. This
	 * supports PRINT semantics, until "carriage control" is output, there is no
	 * work to be done.
	 * 
	 * @param s The string to add to the execution buffer.
	 * @param immediate true if the command buffer is now complete and should 
	 * be executed.  Set to true if just concatenating additional text to an
	 * as-yet incomplete command string.
	 * @throws JBasicException usually a Status.JDBC error with the underlying
	 * JDBC error returned by the driver.  May also be an error if the global
	 * variable SYS$DB_LAST_STATEMENT cannot be set to the last command string
	 * executed.
	 */
	public void execute(final String s, boolean immediate) throws JBasicException {

		lastStatus = new Status();
		cleanup();

		/*
		 * If we are building a command buffer, add this in, else it becomes the
		 * start of a new command buffer.
		 */
		if (commandBuffer == null)
			commandBuffer = s;
		else if (s != null)
			commandBuffer = commandBuffer + s;

		/*
		 * Assuming there is a known file handle, set the query buffer in the
		 * handle to match the current query string value.
		 */
		if( fileID != null )
			fileID.setElement(commandBuffer == null? "" : commandBuffer, "QUERY");
		
		/*
		 * If we aren't to execute yet, we're done for now.
		 */
		if (!immediate)
			return;

		/*
		 * If the command buffer is essentially empty, we have no work to do.
		 */
		if (commandBuffer == null)
			return;
		if (commandBuffer.trim().length() == 0)
			return;

		/*
		 * Okay, we're going to execute some stuff. Start by making a copy of
		 * the command buffer (and clearing the buffer that was being built).
		 * Also, create an empty field map.
		 */

		final String localCommandBuffer = commandBuffer;
		jbenv.globals().insert("SYS$DB_LAST_STATEMENT", localCommandBuffer);
		commandBuffer = null;
		fileID.setElement(new Value(Value.RECORD, null), "MAP");

		/*
		 * Create a statement object, which is needed for just about anything we
		 * do here.
		 */
		try {
			statementHandle = connectionHandle.createStatement();
		} catch (final Exception e) {
			cleanup();
			throw new JBasicException(Status.JDBC, "statement failure, " + e);			
		}

		/*
		 * Execute the statement.  If there is a result set, then capture that
		 * and build a field list for the database.
		 */
		try {
			resultSetHandle = statementHandle.executeQuery(localCommandBuffer);
			if (resultSetHandle != null) {
				fieldList = getFieldList();
				fileID.setElement(fieldList, "MAP");
			}
		} catch (final Exception e) {
			cleanup();
			throw new JBasicException(Status.JDBC, "statement failure " + e);
		}
		return;
	}

	/**
	 * Close the session to the JDBC server.  Errors are printed directly
	 * to the current session console.
	 */
	public void close() {
		cleanup();
		if (connectionHandle != null)
			try {
				connectionHandle.close();
			} catch (final Exception e) {
				final Status sts = new Status(Status.JDBC, "close error "
						+ e.toString());
				sts.print(jbenv);
			}
		super.close();
	}

	/**
	 * Cleanup the data structures that are left hanging after a statement
	 * operation.
	 * 
	 */
	void cleanup() {

		fieldList = null;

		try {
			if (resultSetHandle != null) {
				resultSetHandle.close();
				resultSetHandle = null;
			}
			if (statementHandle != null) {
				statementHandle.close();
				statementHandle = null;
			}
		} catch (final Exception e) {
			lastStatus = new Status(Status.JDBC, "cleanup error " + e);
			lastStatus.print(jbenv);
		}
	}

	/**
	 * Get a field from the current row of the result set by name.
	 * 
	 * @param fieldName
	 *            A string containing the name of the field to read.
	 * @return The string value, or null if the field was not valid.
	 */
	public Value getString(final String fieldName) {

		/*
		 * Use the active result set to get a field by name
		 */
		Value v = null;
		try {
			v = new Value(resultSetHandle.getString(fieldName));
		} catch (final SQLException e) {
			lastStatus = new Status(Status.JDBC, "field " + fieldName
					+ " read error " + e);
			return null;
		}
		return v;
	}

	/**
	 * Get a field from the current row of the result set by name.
	 * 
	 * @param fieldName
	 *            A string containing the name of the field to read.
	 * @return The string value, or null if the field was not valid.
	 */
	public Value getInteger(final String fieldName) {

		/*
		 * Use the active result set to get a field by name
		 */
		Value v = null;
		try {
			v = new Value(resultSetHandle.getInt(fieldName));
		} catch (final SQLException e) {
			lastStatus = new Status(Status.JDBC, "field " + fieldName
					+ " read error " + e);
			return null;
		}
		return v;
	}

	/**
	 * Get a field from the current row of the result set by name.
	 * 
	 * @param fieldName
	 *            A string containing the name of the field to read.
	 * @return The value from the field.
	 */
	public Value getDouble(final String fieldName) {

		/*
		 * Use the active result set to get a field by name
		 */
		Value v = null;
		try {
			v = new Value(resultSetHandle.getDouble(fieldName));
		} catch (final SQLException e) {
			lastStatus = new Status(Status.JDBC, "field " + fieldName
					+ " read error " + e);
			return null;
		}
		return v;
	}

	/**
	 * Get a field from the current row of the result set by name.
	 * 
	 * @param fieldName
	 *            A string containing the name of the field to read.
	 * @return The string value, or null if the field was not valid.
	 */
	public Value getBoolean(final String fieldName) {

		/*
		 * Use the active result set to get a field by name
		 */
		Value v = null;
		try {
			v = new Value(resultSetHandle.getBoolean(fieldName));
		} catch (final SQLException e) {
			lastStatus = new Status(Status.JDBC, "field " + fieldName
					+ " read error " + e);
			return null;
		}
		return v;
	}

	/**
	 * If there is more data available in the current result set for this
	 * database, the get that data in the result set handle.
	 * 
	 * @return true if there was more data, or false if the query result set has
	 *         been exhausted. If false, the lastStatus is also set to
	 *         Status.EOF.
	 */
	public boolean nextResult() {

		boolean moreData = false;

		try {
			moreData = resultSetHandle.next();
		} catch (final Exception e) {
			lastStatus = new Status(Status.JDBC, "result set read error, " + e);
		}

		if (!moreData)
			lastStatus = new Status(Status.EOF);

		return moreData;
	}

	/**
	 * Construct a field list array (used by the GET bytecode) from the
	 * information in the result set.
	 * 
	 * @return A value containing an array of records describing the current
	 *         result set.
	 */
	public Value getFieldList() {

		if (fieldList != null)
			return fieldList;

		fieldList = new Value(Value.ARRAY, null);

		if (resultSetHandle == null) {
			lastStatus = new Status(Status.JDBC,
					"no result set to get field data from");
			return null;
		}
		ResultSetMetaData x = null;

		try {
			x = resultSetHandle.getMetaData();
			final int count = x.getColumnCount();

			for (int ix = 1; ix <= count; ix++) {

				final String name = x.getColumnName(ix);
				final int type = x.getColumnType(ix);
				final int size = x.getColumnDisplaySize(ix);

				/* Remap the type to match our types */
				String tstring = null;

				switch (type) {
				case Types.INTEGER:
				case Types.BIGINT:
					tstring = "INTEGER";
					break;

				case Types.FLOAT:
				case Types.DOUBLE:
					tstring = "DOUBLE";
					break;

				case Types.BOOLEAN:
					tstring = "BOOLEAN";
					break;

				default:
					tstring = "STRING";
					break;

				}

				// System.out.println("DEBUG: field " + name + " is of type " +
				// tstring);

				final Value element = new Value(Value.RECORD, null);
				element.setElement(new Value(name), "NAME");
				element.setElement(new Value(tstring), "TYPE");
				if (tstring.equals("STRING"))
					element.setElement(new Value(size), "SIZE");
				fieldList.setElement(element, ix);
			}

		} catch (final SQLException e) {
			lastStatus = new Status(Status.JDBC, "error getting metadata, " + e);
			return null;
		}

		fileID.setElement(fieldList, "MAP");
		return fieldList;
	}

	/**
	 * End of file test for database; tests to see if the result set is
	 * exhausted or not.
	 * 
	 * @return True if there is no more data to be read form the result set that
	 *         represents the most recent statement executed (via PRINT).
	 */
	public boolean eof() {

		if (resultSetHandle == null)
			return true;

		try {

			boolean flag;

			flag = resultSetHandle.isLast();
			if (flag)
				return true;

			flag = resultSetHandle.isAfterLast();

			return flag;
		} catch (final SQLException e) {
			return true;
		}
	}

	/**
	 * Return a value containing the current string of the query command.
	 * @return a Value object containing the string, or an empty string if
	 * there is no active buffer.
	 */
	public Value getCommandBuffer() {
		if( commandBuffer == null )
			return new Value ("");
		return new Value(commandBuffer);
	}
}

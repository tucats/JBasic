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

import java.util.ArrayList;
import java.util.Iterator;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.statements.Statement;
import org.fernwood.jbasic.value.Value;

/**
 * <b>BYNAME()</b> JBasic Function
 * <p>
 * <table>
 * <tr>
 * <td><b>Description:</b></td>
 * <td>Process named input</td>
 * </tr>
 * <tr>
 * <td><b>Invocation:</b></td>
 * <td><code>r = BYNAME( <em>string-buffer</em>,  <em>key1</em>[, <em>key2</em>...] )</code></td>
 * </tr>
 * <tr>
 * <td><b>Returns:</b></td>
 * <td>Record</td>
 * </tr>
 * </table>
 * <p>
 * Given a string buffer and a list of variable names, will process the buffer
 * as a set of BY NAME input operations and assign the values to the named
 * variables. Values found in the input buffer that are not explicitly named
 * cause an error. Values with no value given in the input buffer are not 
 * included in the resulting record.
 * <p>
 * The result is returned in a record, where each named variable is a member
 * of the resulting record, and the member value is the value entered for each
 * member in the input buffer.
 * <p>
 * An alternate mode is to give no variable names.  In this case, the buffer
 * is processed as normal, but the result just contains all the values found,
 * and it is up to the caller (using the MEMBERS() function, perhaps) to 
 * decide what's in the record and what they want to do with the result.
 * 
 * @author cole
 */
public class BynameFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param arglist
	 *            the function argument list and count already popped from the
	 *            runtime data stack
	 * @param symbols
	 *            the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException if a parameter count or type error occurs
	 */

	public Value run(final ArgumentList arglist, final SymbolTable symbols)
	throws JBasicException {

		/*
		 * Must be at least one arguments, and there is no maximum. The argument
		 * must be a string. Really all must be strings, but we don't have a
		 * good way of dealing with that here.
		 */
		arglist.validate(1, -1, new int[] { Value.STRING });

		/*
		 * We cannot really make this work right if the same name is given more
		 * than once in the symbol table list. So scan over the arguments to be
		 * sure there are no duplicates. This also lets us check that they are
		 * all string names
		 */

		boolean fAllowMissing = false;
		boolean fHasNames = (arglist.size() > 1);
		ArrayList<String> expectedNames = null;

		if (fHasNames) {
			expectedNames = new ArrayList<String>();

			/*
			 * If there is only one argument and it's an array
			 * then this is a list of keys. 
			 */

			if( arglist.element(1).getType() == Value.ARRAY ) {
				fAllowMissing = true;
				Value array = arglist.element(1);
				for( int ix = 1; ix <= array.size(); ix++ ) {
					Value symName = array.getElement(ix);
					if( symName.getType() != Value.STRING )
						throw new JBasicException(Status.ARGTYPE);
					expectedNames.add(symName.getString().toUpperCase());
				}
			}

			/*
			 * Otherwise, each argument must be a string with a
			 * key name.
			 */
			else {

				for (int ix = 1; ix < arglist.size(); ix++) {

					Value symName = arglist.element(ix);
					if (symName.getType() != Value.STRING) {
						throw new JBasicException(Status.ARGTYPE);
					}

					expectedNames.add(symName.getString().toUpperCase());

					for (int iy = ix + 1; iy < arglist.size(); iy++) {
						if (symName.getString().equalsIgnoreCase(arglist
								.stringElement(iy))) {
							throw new JBasicException(Status.ARGERR);
						}
					}
				}
			}
		}
		/*
		 * We're going to return the result as a RECORD.  If we're allowed
		 * to have missing elements in the input, go ahead now and initialize
		 * all the members to missing and we'll overwrite them later.
		 */
		Value result = new Value(Value.RECORD, null);
		if( fAllowMissing && expectedNames != null ) {
			for( int ix = 0; ix < expectedNames.size(); ix++ ) {
				String key = expectedNames.get(ix);
				result.setElement(new Value(Double.NaN), key);
			}
			result.setElement(new Value(Value.RECORD, null), "_UNEXPECTED");
		}
		/*
		 * Get the buffer and build an executable statement for it. IF it is not
		 * syntactically valid, then throw an error now. We coerce this to be a
		 * LET statement by putting on at the start of the buffer. If the buffer
		 * (now dressed up as an assignment statement) fails, then we return an
		 * empty record to the caller indicating no useful data was found. It's
		 * up to the caller to decide what to do with this information.
		 * 
		 * If there is an error, let's stuff it in a variable.  This is NOT a 
		 * particularly great plan, but it's what I've got at the moment.  Maybe
		 * it should be put in SYS$STATUS which is the universal status variable?
		 */
		Statement assignStatement = new Statement();
		
		assignStatement.store("LET " + arglist.stringElement(0));
		
		symbols.insert("SYS$STATUS", new Value(assignStatement.status));
		if( assignStatement.status.failed())
			return result;

		/*
		 * Now we run the statement that was generated, using a private symbol
		 * table.
		 */

		SymbolTable inputSymbols = new SymbolTable(arglist.session,
										"Local to BYNAME function", null);
		Status runStatus = assignStatement.byteCode.run(inputSymbols, 0);
		if (runStatus.failed())
			return result;

		if (fHasNames && expectedNames != null) {
			/*
			 * Go through the list of symbol names we were given in the function
			 * call. If they have a value in the symbol table, get that value
			 * and assign it. When we use a symbol like this, delete it so we
			 * can't find it again.
			 */

			for (int ix = 0; ix < expectedNames.size(); ix++) {
				String symbolName = expectedNames.get(ix);
				Value value = inputSymbols.localReference(symbolName);
				if (value != null) {
					result.setElement(value, symbolName);
					inputSymbols.delete(symbolName);
				}
			}

			/*
			 * Last step - see if there are unused values in the input buffer's
			 * symbol table, indicating the user specified values we didn't want
			 * or need.
			 */
			
			Iterator keys = inputSymbols.table.keySet().iterator();
			Value unexpected = new Value(Value.RECORD, null);
			while (keys.hasNext()) {
				String key = (String) keys.next();
				if( fAllowMissing )
					unexpected.setElement(inputSymbols.localReference(key), key);
				else
					throw new JBasicException(Status.UNKVAR, key);
			}
			result.setElement(unexpected, "_UNEXPECTED");
		}

		else {

			/*
			 * No explicit list of names given, so just fetch all that were
			 * found.
			 */

			Iterator keys = inputSymbols.table.keySet().iterator();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				Value value = inputSymbols.localReference(key);
				result.setElement(value, key);
			}

		}
		/*
		 * All done, return the resulting record mapping the input value to the
		 * requested names.
		 */
		return result;

	}

}

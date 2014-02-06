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
package org.fernwood.jbasic.compiler;

import java.util.Vector;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.Instruction;
import org.fernwood.jbasic.value.Value;

/**
 * LValue objects for handling parsing and assignment of variables. The LValue
 * object holds all the information found during parsing an 'lvalue', which is
 * a compiler concept for the item on the left side of the assignment operator
 * in traditional language syntax. This class additionally has methods
 *  for generating code to store a data element in that lvalue. This is used by the LET
 * statement, ex:
 * <p>
 * <code>
 * LET NAMES[ 1 ] = "Tom"
 * </code>
 * <p>
 * In this example, the expression <code>"NAME[ 1 ]"</code> is the lvalue, and
 * identifies the first element of an arrayValue. The LValue object can parse
 * this part of the expression, and stores away all the information needed to
 * store into the referenced location later. This can also be used for simple
 * scalar variables, of course. The variable does not need to already exist,
 * since in JBasic variables are usually type-independant and created as they
 * are needed.
 * <p>
 * The LValue object has the following major functions:
 * <p>
 * <list>
 * <li>Parse the LVALUE (target of an assignment, usually Left of the "="
 * operator)
 * <li>Generate code to store the top stack value in the storage target </list>
 * <p>
 * <br>
 * These are separate functions because the storage location must be parsed
 * first before the data that can go in it. The LValue retains all the state
 * necessary to generate code for the storage operation as part of the parsing
 * function. This is often done by generating any subexpressions in the storage
 * value (such as index expressions) into a temporary bytecode stream stored in
 * the object. At generate-time the temporary bytecode can be emitted to
 * logically complete the storage operation.
 * <p><br>
 * Starting with Version 2.4 of JBasic, the LValue compiler can operate in
 * two modes - <em>reference</em> and <em>direct</em>.  When in <em>reference</em>
 * mode, the destination (the LVALUE itself) is addressed using the 
 * <code>_LOCREF</code>, <code>_LOCMEM</code>, and <code>_LOCIDX</code> 
 * instructions which <em>Load Or Create</em> the value, member, or array
 * index location and leave the actual object on the stack - not a copy.  The
 * store phase is create using a <code>_SET</code> instruction which copies the 
 * contents of one stack object directly to another without creating a new copy.
 * <p><br>
 * By contrast, <em>direct</em> mode can be used when the LVALUE reference is simple
 * enough (a scalar, single-level record member, or single-dimensional array)
 * and written using <code>_STORE</code>, <code>_STORR</code>, or 
 * <code>_STORA</code> operations.  This is higher
 * performance since many operations such as <code>INTEGER N=1</code> which
 * can be encoded as a single byte code instruction.
 * <p><br>
 * The LValue compiler first parses the LValue assuming that it will need to
 * use reference mode.  If after doing a compilation, it discovers that there
 * was at most one array index or record member reference, then the parser
 * backs up the token stream and re-compiles using the simpler "direct" method.
 * <p><br>
 * @author cole
 * @version 1.0 July 15, 2004
 */

public class LValue {

	/**
	 * Flag indicating if this LValue is representing an arrayValue reference
	 */
	boolean fArrayReference;

	/**
	 * Flag used to indicate that we are to ignore pre- and post-increment
	 * operations. This is passed down to the expression processor for
	 * array subscripts if needed, and deals with the case where the statement
	 * being compiled is ADD 3 TO A[B++].  The increment is done only once
	 * when the target is parsed as an addend, but not again when it is 
	 * processed as an lvalue.
	 */
	boolean fIgnoreIncrements;
	
	/**
	 * Flag indicating if this LValue represents a record member reference.
	 */
	boolean fRecordReference;

	/**
	 * Flag indicating if this LValue is compiled versus interpreted.
	 */
	boolean fCompile;

	/**
	 * Flag indicating if the variable should be set to read-only when the
	 * assignment is completed.
	 */
	boolean fReadOnly;

	/**
	 * Flag indicating if this LValue is in "reference mode" which means that
	 * the lvalue is sufficiently complicated that indirect references are
	 * required to establish the object to be set.  And example is "A.B.C=3"
	 * which requires that the object A.B.C be created/located and then _SET.
	 * When reference mode is not used, stores are done directly into the
	 * named variables, such as "N=1".
	 */
	private boolean fRefMode;
	
	/**
	 * Flag indicating if the storage will be marked as common when the
	 * operation completes.
	 */
	private boolean fCommon;

	/**
	 * Flag indicating if strong typing is enabled. This means that before a
	 * result can be stored in a variable, it's type is coerced based on the
	 * variable name.
	 */

	boolean fStrongTyping;

	/**
	 * This is the type that the output will be coerced to, based on the name of
	 * the LVALUE. This is calculated during the parsing of the LVALUE, and
	 * optionally used during generation of the store operation if strong typing
	 * is enabled.
	 */
	int requiredType;

	/**
	 * For a compiled LValue, this is where the bytecode is temporarily held
	 * that references the storage location. This is generated when the LValue
	 * is processed, but won't be written to the ultimate bytecode storage until
	 * the value(s) to store have been compiled.
	 */
	ByteCode byteCode;

	/**
	 * In USE_REFERENCE mode, this is where the code is built that creates
	 * the reference to the LValue.  In conventional mode, this code stream
	 * is used to hold array reference calculations.
	 */
	ByteCode referenceCode;

	/**
	 * A copy of the arrayValue container object parsed during lvalue processing
	 */
	Value array;

	/**
	 * The name of the destination scalar or arrayValue. This is important in
	 * the case of the scalar when the variable might be created by the lvalue
	 * store operation.
	 */
	String name;

	/**
	 * For a structure member LValue, this is the structure field.
	 */
	String member;

	/**
	 * Flag indicating if an error has occurred during processing this lvalue.
	 */
	public boolean error;

	/**
	 * The status block containing the details of the last error incurred while
	 * processing the lvalue.
	 */
	Status status;

	/**
	 * For an arrayValue lvalue, this is the index location parsed.
	 */
	int index;

	/**
	 * Indicator describing which symbol table we will store the result it. A
	 * value of 0 means our own, 1 means our parent, 2 means our grandparent,
	 * and so on. -1 means the global table, -2 the root table, and -3 the macro
	 * table.
	 */
	int scope;

	/**
	 * This is the environment (root JBasic object) that this LValue is owned
	 * by. This lets the LValue access the symbol table and runtime space for
	 * thinks like PROGRAM references.
	 */
	JBasic session;

	/**
	 * If the user has a list assignment, this lets us chain them
	 * together.  For example, LET A,B = [1,2] would have the primary
	 * LValue be "A" but a chained copy for "B".
	 */
	
	private LValue chained;
	
	private boolean fForceReference;

	private Vector<String> postList;

	private boolean	allowChaining;

	/**
	 * Create a new LValue object.
	 * 
	 * @param jb
	 *            The JBasic object that contains this session. This is used to
	 *            locate global symbol tables, etc.
	 * @param typeFlag
	 *            A boolean flag that indicates if strong (static) typing is
	 *            expected for this LVALUE generation. This is usually a copy of
	 *            the value from the bytecode setting.
	 */
	public LValue(final JBasic jb, final boolean typeFlag) {
		session = jb;
		fStrongTyping = typeFlag;
		status = new Status(Status.SUCCESS);
		index = 0;
		fArrayReference = false;
		fRecordReference = false;
		fCommon = false;
		error = false;
		byteCode = null;
		referenceCode = null;
		fCompile = true;
		scope = 0;
		postList = new Vector<String>();
		chained = null;
		}

	/**
	 * Set indicator telling if this use of LVALUE allows chained
	 * assignments.
	 * @param flag true if we allow chained assignments.
	 */
	public void allowChaining(boolean flag ) {
		allowChaining = flag;
	}
	
	/**
	 * When an LValue is encountered in compiled code, this method will parse
	 * the LValue, and generate temporary code to handle storing the result in
	 * the LValue. This temporary code is generated here but will not be emitted
	 * into the user's bytecode stream until the compileStore() method is
	 * called.
	 * 
	 * @param bc
	 *            The ByteCode where instructions will ultimately be written.
	 * @param tokens
	 *            A Tokenizer used to parse the elements of the LValue
	 *            definition.
	 * @return A Status indicating if compilation was successful. An error state
	 *         usually implies a syntax error in the input tokenizer stream.
	 */
	@SuppressWarnings("unchecked") 
	public Status compileLValue(final ByteCode bc, final Tokenizer tokens) {

		status = new Status(Status.SUCCESS);
		int nestCount = 0;
		fRefMode = true;	/* Assume reference mode until we see otherwise */

		fArrayReference = false;
		fRecordReference = false;
		error = false;
		byteCode = bc;
		fCompile = true;
		referenceCode = new ByteCode(session, null);

		if (!tokens.testNextToken(Tokenizer.IDENTIFIER))
			return status = new Status(Status.LVALUE, tokens.peek(0));

		name = tokens.nextToken();
		
		if( ReservedWords.isVerb(name))
			return status = new Status(Status.RESERVED, name);
		
		requiredType = determineType(name);
		int mark = tokens.getPosition();
		
		if( fRefMode ) {
			int firstRef = referenceCode.add(ByteCode._LOCREF, name );
			scope = 0;
			while( true ) {
				
				/*
				 * If it's a dot then this is a record member reference.
				 * Locate or create the member using _LOCMEM.
				 */
				if( tokens.assumeNextSpecial(".")) {
					if( !tokens.testNextToken(Tokenizer.IDENTIFIER))
						return status = new Status(Status.EXPSYNTAX,
								new Status(Status.INVRECSYN));
					member = tokens.nextToken();
					referenceCode.add(ByteCode._STRING, member);
					referenceCode.add(ByteCode._LOCMEM);
					fRecordReference = true;
					nestCount++;
					continue;
				}

				/*
				 * Support (multidimensional) array references in the
				 * LValue by calculating indexes that are accessed or
				 * created by _LOCIDX.
				 */
				if( tokens.assumeNextSpecial("[")) {
					fArrayReference = true;
					final Expression exp = new Expression(session);
					exp.setDeferPosts(true);
					while( true ) {
						exp.compile(referenceCode, tokens);
						if (exp.status.failed())
							return exp.status;

						referenceCode.add(ByteCode._LOCIDX);
						fArrayReference = true;
						nestCount++;
						if( tokens.assumeNextSpecial(","))
							continue;
						if (!tokens.assumeNextToken("]"))
							return status = new Status(Status.IDXEXP);
						break;
					}
					postList.addAll(exp.incrementList());
					continue;
				}
				/*
				 * It's not an array or record, so we must have parsed
				 * it all.  NOTE: Need to support object references at
				 * some point.
				 */
				break;
			}
			
			/*
			 * Attempt to parse the storage class info, such as COMMON
			 * or READONLY.
			 */
			if (tokens.assumeNextSpecial(new String [] { "<", "|", "/"})) {
				String startDelim = tokens.getSpelling();
				String endDelim = startDelim.equals("<") ? ">" : startDelim;
				
				while (true) {
					final String n = tokens.nextToken();
					if (n.equals(endDelim))
						break;
					if( n.equals("=")) {
						tokens.restoreToken();
						break;
					}
					
					/*
					 * Odd parsing quirk. If you specify
					 * 
					 * x<parent>=3
					 * 
					 * The the token stream is { "X", "<", "PARENT", ">=", "3" },
					 * which is clearly not what we want. If we find a ">=" token
					 * right here, let's assume it was meant as a ">" followed by a
					 * "=". We'll break out, but not before we push the "=" back on
					 * the token stream.
					 */
					if (n.equals(">=")) {
						tokens.restoreToken("=", Tokenizer.SPECIAL);
						break;
					}

					/*
					 * Handle the various characteristic keywords which can
					 * be a comma-separated list if needed.
					 */
					if (n.equalsIgnoreCase("READONLY"))
						fReadOnly = true;
					else if (n.equalsIgnoreCase("COMMON"))
						fCommon = true;
					else if (n.equalsIgnoreCase("ROOT"))
						scope = -2;
					else if (n.equalsIgnoreCase("PARENT"))
						scope = 1;
					else if (n.equalsIgnoreCase("GLOBAL"))
						scope = -1;
					else if (n.equalsIgnoreCase("MACRO"))
						scope = -3;
					else
						return status = new Status(Status.INVSCOPE, 
								name + startDelim + n + endDelim);
					if (tokens.assumeNextSpecial(","))
						continue;
				}
			}
			/*
			 * If the variable name is RND  then this is an error - you 
			 * can't assign values to pseudo-variables.
			 */
			if( name.equals("RND")) {
				return status = new Status(Status.LVALUE, "RND");
			}

			/*
			 * If there was a scope specification such as <ROOT> or <PARENT>
			 * then encode the scope value in the first instruction that 
			 * references/creates the base variable.
			 */
			if( scope != 0 ) {
				Instruction i = referenceCode.getInstruction(firstRef);
				i.integerOperand = scope;
				i.integerValid = true;
			}
						
			/* If we did have complex references, then we are done and will use the
			 * more cumbersome but powerful reference modes.
			 */
			
			if( nestCount > 1 || fIgnoreIncrements || fForceReference) {
				return status;
			}
			
			/*
			 * We could do this the older (and more performance-efficient)
			 * way, so reset ourselves and do it again the old way.
			 */
			tokens.setPosition(mark);
			referenceCode = new ByteCode(session, null);
			fArrayReference = false;
			fRecordReference = false;
			fCommon = false;
			fReadOnly = false;
			fRefMode = false;
			
		}

		if (tokens.assumeNextToken(".")) {
			member = tokens.nextToken();
			fRecordReference = true;
			requiredType = determineType(member);

		} else if (tokens.assumeNextToken("[")) {

			fArrayReference = true;
			final Expression exp = new Expression(session);
			exp.setIgnoreIncrements(fIgnoreIncrements);
			exp.compile(referenceCode, tokens);
			if (exp.status.failed())
				return exp.status;

			if (!tokens.assumeNextToken("]"))
				return status = new Status(Status.IDXEXP);

		}

		scope = 0;
		fReadOnly = false;

		if (tokens.assumeNextSpecial(new String [] { "<", "|", "/"})) {
			String startDelim = tokens.getSpelling();
			String endDelim = startDelim.equals("<") ? ">" : startDelim;
			while (true) {
				
				/*
				 * Previous parse attempt for reference mode may have
				 * eaten the closing ">" so test for possible equals
				 * sign as indicator that we're done.
				 */
				if( tokens.testNextToken("="))
					break;
				
				final String n = tokens.nextToken();
				if (n.equals(endDelim))
					break;

				/*
				 * Same as above, handle the case of X<PARENT>=3 reporting 
				 * incorrectly a ">=" single token.
				 */
				if (n.equals(">=")) {
					tokens.restoreToken("=", Tokenizer.SPECIAL);
					break;
				}

				/*
				 * Handle the various characteristic keywords which can
				 * be a comma-separated list if needed.
				 */
				if (n.equalsIgnoreCase("LOCAL"))
					scope = 0;
				else if (n.equalsIgnoreCase("PARENT"))
					scope = 1;
				else if (n.equalsIgnoreCase("GLOBAL"))
					scope = -1;
				else if (n.equalsIgnoreCase("ROOT"))
					scope = -2;
				else if (n.equalsIgnoreCase("MACRO"))
					scope = -3;
				else if (n.equalsIgnoreCase("READONLY"))
					fReadOnly = true;
				else if (n.equalsIgnoreCase("COMMON"))
					fCommon = true;
				else
					return status = new Status(Status.LVALUE, 
							name + startDelim + n + endDelim);
				if (tokens.assumeNextToken(","))
					continue;

			}
		}
		/*
		 * If the variable name is RND, error - you can't assign
		 * values to pseudo-variables.
		 */
		if( name.equals("RND")) {
			return status = new Status(Status.LVALUE, "RND");
		}

		/*
		 * Is this an assignment list? If so, then parse the next item as
		 * well.
		 */
		
		chained = null;
		if( allowChaining && tokens.assumeNextSpecial(",")) {
			chained = new LValue(session, error);
			chained.allowChaining(true);
			status = chained.compileLValue(bc, tokens);
		}
		/*
		 * All done.
		 */
		return status;
	}

	/**
	 * Is this LVALUE part of a chained LVALUE?
	 * @return true if there is a list of assignments to process.
	 */
	public boolean isChained() {
		return (chained != null);
	}
	/**
	 * For a previously processed LValue, this causes the compiled code for
	 * storing the top-of-stack into the LValue to be written to the user's
	 * byte code stream (provided previously in the compileLValue() method call.

	 * 
	 * @return A Status value indicating if there was an error in writing the
	 *         store instructions.
	 */
	public Status compileStore() {
		return compileStore(false);
	}

	/**
	 * For a previously processed LValue, this causes the compiled code for
	 * storing the top-of-stack into the LValue to be written to the user's
	 * byte code stream (provided previously in the compileLValue() method call.
	 * @param fillFlag if True, the store operation stores the value in all
	 * possible locations in the target array object, rather than just the
	 * specified index.  This is used in declarations such as INTEGER X[5] = 5
	 * 
	 * @return A Status value indicating if there was an error in writing the
	 *         store instructions.
	 */
	public Status compileStore(boolean fillFlag) {
		
		/*
		 * If we are asked to fill, then this is a declaration statement
		 * as opposed to an assignment or other traditional LVALUE.  When
		 * this is the case, we cannot support REFERENCE mode.  Throw an
		 * error.
		 */
		
		if( fRefMode && fillFlag ) 
			return new Status(Status.INVDIM);
		
		/*
		 * Do we have to use reference mode?  This is true when the
		 * lvalue is complex enough (more than one member dereference
		 * or dimension).
		 */
		if( fRefMode) {

			/*
			 * If we are doing strong typing, generate the code now that
			 * converts the object value we are storing into the desired
			 * data type.
			 */
			if (fStrongTyping)
				byteCode.add(ByteCode._CVT, requiredType, name);

			/*
			 * Emit the store operation into the reference byte code
			 * stream.
			 */
			referenceCode.add(ByteCode._SET);
			
			/*
			 * Handle any storage class settings.
			 */
			if (fReadOnly)
				referenceCode.add(ByteCode._PROT, scope, name);
			if( fCommon )
				referenceCode.add(ByteCode._COMMON, name);

			/*
			 * Lastly, copy the whole mess we've generated into the
			 * byte code stream that is being compiled to and we're
			 * done.
			 */
			if( fIgnoreIncrements) {
				for( int idx = 0; idx < referenceCode.size(); idx++ ) {
					if( referenceCode.getInstruction(idx).opCode == ByteCode._INCR)
						referenceCode.remove(idx);
				}
			}
			byteCode.concat(this.referenceCode);
			Expression.postIncrements(postList, byteCode);
			return new Status(Status.SUCCESS);
		}
		
		/* We do not need to use reference mode because the LValue is
		 * simple enough to allow direct store operations.  Before we
		 * generate that code, if strong typing is on, we will have 
		 * to add a convert operation.
		 */

		if (fStrongTyping && fillFlag && (requiredType != Value.ARRAY))
			byteCode.add(ByteCode._CVT, requiredType, name);

		/*
		 * Generate the code to store the lvalue, based on the kind of reference
		 * it was.
		 */

		if (fRecordReference) {
			byteCode.add(ByteCode._STRING, member.toUpperCase());
			byteCode.add(ByteCode._STORR, name);
		} else if (fArrayReference) {
			byteCode.concat(referenceCode);
			byteCode.add(ByteCode._SWAP);
			byteCode.add(fillFlag ? ByteCode._STORALL : ByteCode._STORA, name);
		} else if (scope != 0)
			byteCode.add(ByteCode._STOR, scope, name);
		else
			byteCode.add(ByteCode._STOR, name);
		
		/*
		 * Generate the attribute modifying instructions for READONLY or
		 * COMMON storage classes if needed.
		 */
		if (fReadOnly)
			byteCode.add(ByteCode._PROT, scope, name);
		if( fCommon )
			byteCode.add(ByteCode._COMMON, name);
		
		/*
		 * Final step, if there is a chained value, execute it as well.
		 */
		
		if( chained != null )
			return chained.compileStore(fillFlag);
		
		return new Status(Status.SUCCESS);
	}

	/**
	 * Given a symbol name, determine if it has a required type, if strong
	 * typing is enabled.
	 * 
	 * @param name
	 *            The symbol name to determine the implicit type of.
	 * @return an integer containing Value.STRING, Value.INTEGER, etc.
	 */

	public static int determineType(final String name) {

		int requiredType = Value.UNDEFINED;

		final char lastChar = name.charAt(name.length() - 1);

		switch (lastChar) {

		case '$':
			requiredType = Value.STRING;
			if (name.length() > 1)
				if (name.charAt(name.length() - 2) == '$')
					requiredType = Value.RECORD;
			break;

		case '#':
			requiredType = Value.INTEGER;
			break;

		case '!':
			requiredType = Value.BOOLEAN;
			break;

		default:
			requiredType = Value.DOUBLE;
		}

		return requiredType;
	}

	/**
	 * Generate a load of the intended target. This is used, for example, in
	 * the MID$(A$, 1, 3 ) = "To" case.  
	 * <p><br>
	 * If we are in reference mode, the 
	 * complex load operations were already compiled into a local bytecode,
	 * so just write that to the target bytecode.  If we are not in reference
	 * mode for this lvalue, then write the appropriate discrete load operations
	 * for the target.
	 *
	 */
	public void compileLoad() {

		if( fRefMode) {
			byteCode.concat(referenceCode);
			return;
		}
		
		if( !fArrayReference ) {
			byteCode.add(ByteCode._LOADREF, name );
			return;
		}

		if( fArrayReference ) {
			byteCode.concat(referenceCode);
			byteCode.add(ByteCode._INDEX, name);
		}
		else if( fRecordReference) {
			byteCode.add(ByteCode._STRING, member.toUpperCase());
			byteCode.add(ByteCode._LOADR, name);
		}

	}

	/**
	 * Set the required type of the result of the LValue.  This must be done
	 * before code is generated for the store of the lvalue.  If strong typing
	 * is not required, then don't call this routine!
	 * @param type A type code such as Value.INTEGER or Value.STRING.
	 */
	public void setType( int type ) {
		this.fStrongTyping = true;
		this.requiredType = type;
	}

	/**
	 * Set the flag that causes increment operations to be ignored.  See
	 * the Javadoc on the fIgnoreIncrements flag for more information.
	 * @param b true if the LValue compiler is to ingore autoincrements.
	 */
	public void setIgnoreIncrements(boolean b) {
		this.fIgnoreIncrements = b;
	}

	/**
	 * Set or clear the flag that forces the LValue operations to be done
	 * as reference instructions.  This is needed when the caller knows 
	 * that the lvalue is both a source and destination, such as the MID$()
	 * pseudo-function statement.
	 * @param b if TRUE then the lvalue will always be processed as a
	 * reference operation.  If false, then the compiler will determine if
	 * a direct or reference operation is most efficient.
	 */
	public void forceReference(boolean b) {
		fForceReference= b;
		
	}

	/**
	 * Return the count of chained LVALUE assignments.  For the simple case
	 * of LET A=B the count is 1.  For something like LET A,B,C=[1,2,3] the
	 * count is 3.  This always matches the number of LVALUES in the list,
	 * and may or may not correspond to the size of the assigned value.
	 * @return count of lvalues in the list
	 */
	public int count() {
		int count = 1;
		LValue next = this;
		while( next.chained != null ) {
			next = next.chained;
			count++;
		}
		return count;
	}
}
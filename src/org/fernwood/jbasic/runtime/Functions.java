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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Program;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.compiler.CompileContext;
import org.fernwood.jbasic.compiler.Linkage;
import org.fernwood.jbasic.funcs.JBasicFunction;
import org.fernwood.jbasic.statements.Statement;
import org.fernwood.jbasic.value.Value;


/**
 * Function processing.   This contains two primary mechanism that are static
 * functions.  First, this class implements the mechanism to re-map function names, 
 * so names of functions
 * from other dialects of BASIC can be converted to the correct local name (such 
 * as the LEN() function being converted to the native LENGTH() function.  Secondly,
 * this class implements the  mechanism used to invoke a
 * function at runtime, given the name of the function.  This presumes that the
 * function arguments have already been placed on the stack at runtime, and this
 * class has the job of locating the function to be called and executing it.
 * <p>
 * This loader uses Java reflection to locate functions.  Functions are 
 * implemented as separate classes in the
 * org.fernwood.jbasic.funcs package. Functions located in that package are
 * separate classes that must implement a run() method.
 * <p>
 * It is possible for a program that is using JBasic as an embedded scripting
 * language to add additional functions to the library.  These functions must
 * match the format of a subclass of org.fernwood.jbasic.funcs.JBasicFunction,
 * and must implement a run() method.  Additionally the package must have been
 * declared already via the JBasic addPackage() method.
 * <p>
 * Additionally, at this point the SHOW FUNCTIONS command doesn't know about the
 * new package location or how to scan it, so there is a "brute force" list of
 * function names maintained for use by that command.  This list <em>does not</em>
 * contain the list of functions added as external functions!
 * 
 * @author Tom Cole
 * @version 1.1 November 2006
 */

public class Functions {

	/**
	 * This class contains the information stored in each node in the
	 * function cache.  The fields describe the information needed to
	 * call a user-written or built-in function.  An object of this
	 * type is stored in the function cache for each function that
	 * is successfully located.
	 * 
	 * @author tom
	 * @version version 1.0 Nov 22, 2006
	 *
	 */
	static class FunctionCacheEntry { 
		/**
		 * Create a function cache entry, and provide the invocation
		 * information.
		 * @param m The run() method to invoke in the object to call
		 * a built-in function, or null if the function is not a
		 * builtin function.  For user-written functions, this is null.
		 * @param i The object describing the function.  This is either
		 * a Program object for user-written functions, or subclass of 
		 * the JBasicFunction class if it is a builtin.
		 */
		public FunctionCacheEntry(Method m, Object i) {
			theMethod = m;
			theInstance = i;
			counter = 1;
		}
		Method theMethod;
		Object theInstance;
		int	counter;
	}

	static HashMap<String, FunctionCacheEntry> functionCache;
	static int functionCacheTries;
	static int functionCacheHits;

	/**
	 * Given a function name, determine if it should be remapped, based on any
	 * current dialect settings, etc.  This mapping is stored in the global
	 * array variable SYS$FUNCTION_MAP and can be added to by the user if they
	 * wish.  This is an array of records, each of which contains a field OLD
	 * and a field NEW, which are used to map "old" names to "new" names.  
	 * <p>
	 * In addition to this array (which is currently set up by default in the
	 * MAIN program in the Library.jbasic module) there are a handful of 
	 * hard coded conversions that cannot be overridden, and are defined statically
	 * in this method.  Examples include <code>LEN()</code> -> <code>LENGTH()</code>
	 * and <code>SUBSTRING()</code> -> <code>SUBSTR()</code>.
	 * 
	 * @param session
	 *            the JBasic session, used to access SYS$FUNCTION_MAP global
	 * @param oldName a String containing the old name of the function to
	 * remap.
	 * @return a String containing the new name, or the old name if no changes
	 *         are required.
	 */
	public static String remapName(final JBasic session, final String oldName) {

		class NameMap {
			String oldName;

			String newName;

			NameMap(final String o, final String n) {
				oldName = o;
				newName = n;
			}
		}

		final NameMap map[] = new NameMap[] { 
				new NameMap("LEN", "LENGTH"),
				new NameMap("SUBSTRING", "SUBSTR"),
				new NameMap("LEFT$", "LEFT"), 
				new NameMap("RIGHT$", "RIGHT") };

		for (NameMap mapElement : map )
			if (mapElement.oldName.equals(oldName))
				return mapElement.newName;

		Value fmap;
		try {
			fmap = session.globals().reference("SYS$FUNCTION_MAP");
		} catch (JBasicException e) {
			return oldName;
		}

		if (!fmap.isType(Value.ARRAY))
			return oldName;

		for (int ix = 0; ix < fmap.size(); ix++) {
			final Value mapElement = fmap.getElement(ix + 1);
			if (mapElement == null)
				continue;
			if (!mapElement.isType(Value.RECORD))
				continue;
			final Value oldMapName = mapElement.getElement("OLD");
			if (oldMapName == null)
				continue;
			final Value newMapName = mapElement.getElement("NEW");
			if (newMapName == null)
				continue;
			if (oldMapName.getString().equalsIgnoreCase(oldName))
				return newMapName.getString().toUpperCase();
		}
		return oldName;
	}

	/**
	 * Method to invoke a function, passing it an argument list structure. This
	 * will attempt first to see if the name is found in the list of
	 * user-written function (program segments identified by the FUNCTION
	 * header). If so, then the associated user program is called with the
	 * constructed argument list. If there is no matching program name, then an
	 * intrinsic function is searched for matching the method invocation
	 * description (see 'reflection' in Java). If so, the named method is called
	 * with the function arguments given. In either case, the function is
	 * assumed to return a single Value which is returned to the caller.
	 * 
	 * @param session
	 *            The JBasic object that contains the current session.
	 * @param fname
	 *            The name of the function to call, i.e. "min"
	 * @param args
	 *            The arglist structure, which is an arg count and arg
	 *            arrayValue
	 * @param symbols
	 *            The symbol table that is active at runtime when the function is
	 *            invoked. This is used by functions that themselves reference
	 *            the symbol table information.
	 * @param debugger the controlling debugger, if any
	 * @return A Value containing the result of the function call
	 * @throws IllegalArgumentException if a parameter count or type error occurs
	 * @throws InvocationTargetException if an error occurs within the function
	 * @throws JBasicException if a parameter count or type error occurs
	 * @throws IllegalAccessException If a function name is invalid
	 */
	@SuppressWarnings("unchecked") 
	public static Value invokeFunction(final JBasic session, final String fname,
			final ArgumentList args, final SymbolTable symbols,
			final JBasicDebugger debugger) throws IllegalArgumentException, JBasicException, IllegalAccessException, InvocationTargetException {
		
	
		/*
		 * If no one has called a function before, create the function cache
		 * which is a hash map used to map function names to an object with
		 * the information needed to invoke the function.
		 */
		if( functionCache == null ) 
			functionCache = new HashMap<String, FunctionCacheEntry>();

		/*
		 * First action; try to see if we've already called this puppy once
		 * before.  Use the function name (which must already have been 
		 * normalized to uppercase) to locate the cache entry object if
		 * possible.
		 */
		functionCacheTries++;
		FunctionCacheEntry fe = functionCache.get(fname);
		
		/*
		 * If the function cache entry is not null, we have enough information
		 * to call the user-written or builtin function directly from here.
		 */
		if( fe != null ) {
			functionCacheHits++;
			fe.counter++;
			
			/*
			 * If the method object is null it means this isn't a builtin
			 * function, but is a user-written function.  Call it as a
			 * subroutine of the current program.
			 */
			if( fe.theMethod == null) {
				Program newPgm = (Program) fe.theInstance;
				return callUserFunction(args, symbols, debugger, newPgm, 0);
			}
			
			/*
			 * The method object was non-null, so it is a built-in object
			 * that we are calling.  Use the fields of the function cache
			 * entry to form an invocation of the underlying function object.
			 */
			Value v = null;
			try {
				v =  ((JBasicFunction)fe.theInstance).run(args, symbols );
			} catch (Exception e) {
				
				if( e.getClass() == JBasicException.class)
					throw (JBasicException) e;
				
				Throwable cause = e.getCause();
				if( cause != null && cause.getClass() == JBasicException.class) {
					throw (JBasicException) cause;

				}
				Status s = new Status(Status.FAULT, 
						new Status(Status.FUNCFAULT,e.toString()));
				throw new JBasicException(s);
			}
			return v;
		}

		/*
		 * A function can be built-in or identified as a program. Let's
		 * first try to see if there is a program that will work. These
		 * are defined by the name prefix "FUNCTION$" and the name of the
		 * function.
		 */

		String programName = JBasic.FUNCTION + fname;
		Program newPgm = session.programs.find(programName);
		if (newPgm != null) {
			functionCache.put(fname, new FunctionCacheEntry(null, newPgm));
			return callUserFunction(args, symbols, debugger, newPgm, 0);
		}

		/*
		 * Examine the function name for a wierd special case - JBASIC() would
		 * cause an exception because JBasicFunction is an abstract class.  So
		 * now that we know it's not a user-supplied function, just preempt
		 * the use of this (essentially) reserved name.
		 */
		if( fname.equals("JBASIC"))
			throw new JBasicException(Status.UNKFUNC, fname);

		/*
		 * Second, we see if it is a class in the org.fernwood.jbasic.funcs
		 * package which contains the stand-alone function classes. (See the
		 * comment at the top of this class for additional info on the migration
		 * of functions from static methods in this class to free-standing
		 * methods).
		 */
		Class c = null;
		Method m = null;
		Object i = null;
		
		try {
				
			c = findFunctionClass(session, fname);
			
			/*
			 * Functions invoked at runtime must have a run() method,
			 * so let's get a reference to it.
			 */
			final String aMethod = "run";
			m = c.getDeclaredMethod(aMethod, new Class[] {
						ArgumentList.class, SymbolTable.class });
	
			/*
			 * Create a new instance of the function object, which we
			 * will use to call the method.  
			 */
			i = c.newInstance();
			
		} catch (final Exception e) {
			
			/*
			 * If the class exists but the method does not, then it's because this
			 * function has no runtime component - it's all resolved at compile time.
			 * 
			 * To use this function, we'll create a new bytecode object and let the
			 * compiler generate code which will then be called to calculate the result.
			 */
			if(( c != null ) && ( m == null )&& ( e.getClass() == NoSuchMethodException.class)) {
				
				/*
				 * Make sure there is a compile method for this function, and then create an
				 * instance of the function object that we can use to invoke the method.
				 */
				JBasicFunction fn = null;
				try {
					m = c.getDeclaredMethod("compile", new Class[] {
							CompileContext.class});
					fn = (JBasicFunction) c.newInstance();
				} catch (Exception e1) {
					m = null;
					fn = null;
				}

				/*
				 * Assuming that went okay, let's get set up to use the compile()
				 * method of the function.  
				 */
				if( m != null ) {
					
					/*
					 * Create a bytecode area, and then copy all the arguments 
					 * into the bytecode as constant data.
					 */
					ByteCode tempCode = new ByteCode(session, null);
					for( int idx = 0; idx < args.size(); idx++ ) {
						tempCode.add(args.element(idx));
					}
					
					/*
					 * Create a context object that describes the compiler work (this
					 * is what's done by the expression compiler).
					 */
					CompileContext ctx = new CompileContext(fname, args.size(), tempCode, true);
					
					/*
					 * Finally, call the compiler method for the function on the given
					 * bytecode.  If that succeeds, then run the generate code and
					 * fetch the result. If either invoking the compile method or 
					 * running the resulting code causes an error, throw that instead.
					 */
					Status s = null;
					if( fn != null )
						s = fn.compile(ctx);
					else
						s = new Status(Status.FAULT, "null function compile method pointer");
					
					if( s.success()) {
						s = tempCode.run(symbols, 0);
						if( s.success()) 
							return tempCode.getResult();
					}
					throw new JBasicException(s);
				}
			}
			
			/*
			 * One last try, the function might really be a program name that
			 * the user is trying to call.  See if that works.
			 */
			programName = JBasic.PROGRAM + fname;
			newPgm = session.programs.find(programName);
			if (newPgm != null) {
				functionCache.put(fname, new FunctionCacheEntry(null, newPgm));
				return callUserFunction(args, symbols, debugger, newPgm, 0);
			}

			/*
			 * Nothing, wasn't a user function, a cached function, a builtin
			 * function, or a program being called as a function.  I give up.
			 */
			throw new JBasicException(Status.UNKFUNC, fname);
		}
		
		/*
		 * We now have enough information to store this in the function
		 * cache so subsequent calls won't have to do the various
		 * reflection operations (which are slightly slower than
		 * the table lookup we use for the cache).  Note that, depending
		 * on the way threads are started, the functionCache may not be
		 * ready yet - in which case we just don't store it in the cache
		 * this one time.
		 */
		if( functionCache != null )
			functionCache.put(fname, new FunctionCacheEntry(m, i));

		/*
		 * Invoke the method using the instance and an array containing
		 * the parameters.  The resulting object is really a Value
		 * so we cast it before returning it as the function result.
		 */
		final Object r = m.invoke(i, new Object[] { args, symbols });
		return (Value) r;
	}

	/**
	 * Given a function name in the current session, locate the function class that
	 * supports the function. 
	 * @param session The current JBasic session
	 * @param fname the name of the function to look up
	 * @return a Class object for the function.  Null is returned if the class
	 * does not exist.
	 * @throws JBasicException the function name is invalid or does not exist
	 */
	public static Class findFunctionClass(final JBasic session,
			final String fname) throws JBasicException {
		Class c;
		String aClass;
		/*
		 * Search for the class for the function we'd like to execute.
		 * Each function is a unique class.  We check for our own 
		 * builtins first (which cannot be overridden).  If we fail
		 * to find one of those, we check for a function declared in
		 * a known package, and use that if we can find it.  Note that
		 * external functions used this way can ONLY be used for
		 * runtime execution of the function via a run() method, the
		 * program does not have access to the compiled code at this time.
		 */
		
		String suffix = Statement.verbForm(fname) + "Function";
		aClass = "org.fernwood.jbasic.funcs." + suffix;
		try { 
			c = Class.forName(aClass);
		}
		catch (Exception e ) {
			aClass = session.findPackage(suffix);
			if( aClass == null ) {
				throw new JBasicException(Status.UNKFUNC, fname);
			}
			try {
				c = Class.forName(aClass);
			} catch (ClassNotFoundException e1) {
				throw new JBasicException(Status.UNKFUNC, fname);
			}
		}
		return c;
	}

		/**
		 * Invoke a user-written function that has been identified by name.
		 * This operation is common to functions that are found in the
		 * function cache as well as those that are looked up via reflection.
		 * 
		 * @param args The argument list to be used with this function.
		 * @param symbols The runtime symbol table.
		 * @param debugger The debugger instance to use during invocation
		 * of this function, or null if no debugger is active.
		 * @param functionPgm The program object representing the user-written
		 * function.
		 * @param startAddress Starting address in the program to begin
		 * execution in the new program.
		 * @return Value that is the result of the function.  If a runtime 
		 * error occurs, then null is returned.
		 * @throws JBasicException the function name is invalid or does
		 * not exist, an argument count or type error occurs, or a runtime
		 * error occurs in the user function code.
		 */
		public static Value callUserFunction(final ArgumentList args, 
					final SymbolTable symbols, 
					final JBasicDebugger debugger,  
					final Program functionPgm, 
					int startAddress) throws JBasicException {
			Status sts;
			Value d = null;
			final JBasic session = args.session;
			final Program oldPgm = session.programs.getCurrent();
			
			SymbolTable newTable = null;
			
			String programName;
			if( startAddress == 0 ) {
				programName = functionPgm.getName();
				newTable = new SymbolTable(session, "Local to "
						+ programName, symbols);
			}
			else {
				programName = symbols.getString(Linkage.ENTRY_PREFIX);
				newTable = symbols;
			}
			/*
			 * The argument list must be broken apart again to set up
			 * the implicit "CALL" operation that is implied by the
			 * function invocation.
			 * <p>
			 * This means that each argument is given a slot in the new
			 * function's symbol table in the array $ARGS.  The calling
			 * program can reference the argument list via this array at
			 * any time, in addition to referencing the arguments via
			 * explicitly named parameters.
			 */
			
			final Value argArray = new Value(Value.ARRAY, null);
			for (int argcount = 0; argcount < args.size(); argcount++)
				argArray.setElement(args.element(argcount), argcount + 1);
			newTable.insertLocal("$ARGS", argArray);
			
			/*
			 * Define additional local variables that tell the function
			 * about itself, how it got called, and the name of the
			 * calling program or function.
			 */
			newTable.insertLocal("$MODE", new Value("FUNCTION"));
			newTable.insertLocal("$THIS", new Value(programName));
			String parentName = "Console";
			
			Program cp = session.programs.getCurrent();
			
			if (cp != null)
				if (cp.isActive())
					parentName = cp.getName();
			newTable.insertLocal("$PARENT", new Value(parentName));
			
			/*
			 * Now run the program with the new symbol table and the
			 * supplied debugger object (if any).
			 */
			sts = functionPgm.run(newTable, startAddress, debugger);
			
			/*
			 * If there was a result, get it now
			 */
			
			try {
				d = newTable.value("ARG$RESULT");
			} catch (JBasicException e) {
				throw new JBasicException(Status.EXPRETVAL);
			}

			
			/*
			 * If the result of the function call was an error, then
			 * let's print it now (and trigger any ON-unit handlers).
			 * If the status was successful, the method does nothing.
			 */
			sts.printError(session);

			/*
			 * Restore the previous "current program" and we're done.
			 */

			session.programs.setCurrent(oldPgm);
			return d;
		}
		
		/**
		 * Remove the named program element from the function cache.  This is done
		 * when it is possible that a new user program has been loaded/created that
		 * would supersede a built-in function that has previously been run.  In this
		 * case we need to remove the cache element so that a subsequent call to the
		 * function will call the user program and not simply call the builtin function
		 * again.
		 * @param name The name of the function to remove.  If this parameter is null,
		 * then all function names are removed from the cache.
		 */
		public static void flushCache(String name) {
			
			if( functionCache == null )
				return;
			
			if( name == null ) {
				Functions.functionCache = null;
				return;
			}
			
			functionCache.remove(name);
			return;
		}
}


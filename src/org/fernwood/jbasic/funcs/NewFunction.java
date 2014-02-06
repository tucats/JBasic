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

import java.lang.reflect.Constructor;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Permissions;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.ObjectValue;
import org.fernwood.jbasic.value.Value;


/**
 * <b>NEW()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Create a new JBasic object</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>v = NEW(<em>parent-object<em> [, <em>class-name</em>])</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>Object record</td></tr>
 * </table>
 * <p>
 * This function creates a new JBasic object, based on a given class name or an object that is
 * an instance of that class.  If the optional second parameter is given, then the newly created
 * object is really a subclass of the named <em>class-name</em>.  Otherwise, the newly created
 * object is an object that is in the same class as the <em>parent object</em>.
 * 
 * @author cole

 */
public class NewFunction extends JBasicFunction {

	/**
	 * Runtime execution of the function via _CALLF
	 * 
	 * @param argList the function argument list and count already 
	 * popped from the runtime data stack
	 * @param symbols the currently active symbol table
	 * @return a Value containing the function result.
	 * @throws JBasicException an error occurred in the number or type of
	 * function arguments, an invalid class type or class constructor arguments
	 * were given, or a permission error occurred for a sandboxed
	 * user attempting to access native Java.
	 */
	@SuppressWarnings("unchecked") 
	public Value run(final ArgumentList argList, final SymbolTable symbols) throws JBasicException {

		/*
		 * Special case, if there is only one argument and it's a string, we're being 
		 * asked to create a Java object.  Alternatively, if there is one argument and 
		 * it is another Java object, use that object's class to create a new instance
		 * of the same class.
		 */

		String className = null;
		int argc = argList.size();

		if( argc >= 1 && argList.element(0).getType() == Value.STRING) {
			className = argList.stringElement(0);
		}
		else
			if( argc >= 1 && argList.element(0).isObject()) {
				ObjectValue v = (ObjectValue)argList.element(0);
				className = v.getObjectClass().getName();
			}

		if( className != null ) {

			/*
			 * Must be allowed to create Java classes, which is a security
			 * risk if not used properly.
			 */

			argList.session.checkPermission(Permissions.JAVA);

			ObjectValue newJBasicObject = null;
			int arraySize = argList.size()-1;
			Class constructorArgClasses[] = new Class[arraySize];
			Object constructorArgs[] = new Object[arraySize];
			Boolean isObjectParm[] = new Boolean[arraySize];
			try {

				for( int ix = 0; ix < argc-1; ix++ ) {
					Value argElement = argList.element(ix+1);
					Class argClass = argElement.getObjectClass();
					if( argClass == null ) 
						throw new JBasicException(Status.INVOBJOP, "use " + argElement.toString() + " with");
					constructorArgClasses[ix] = argClass;

					if( argElement.isObject()) {
						constructorArgs[ix] = ((ObjectValue)argElement).getObject();
						isObjectParm[ix] = true;
					}
					else {
						isObjectParm[ix] = false;
						switch( argElement.getType()) {

						case Value.INTEGER:
							constructorArgs[ix] = new Integer(argElement.getInteger());
							break;
						case Value.DOUBLE:
							constructorArgs[ix] = new Double(argElement.getDouble());
							break;
						case Value.BOOLEAN:
							constructorArgs[ix] = new Boolean(argElement.getBoolean());
							break;
						case Value.STRING:
							isObjectParm[ix] = true;
							constructorArgs[ix] = argElement.getString();
							break;
						}
					}
				}

				/*
				 * Attempt to find a constructor with the given parameters.  If one isn't
				 * found, then throw an error - otherwise, create the object with the
				 * argument list.
				 */
				
				String fullClassName = ObjectValue.fullClassName(className, symbols);
				if( fullClassName == null )
					fullClassName = className;
				
				Class theClass = Class.forName(fullClassName);
				Constructor c = theClass.getConstructor(constructorArgClasses);
				if( c == null )
					throw new JBasicException(Status.INVCLASS, ObjectValue.methodSignature( className, constructorArgClasses));
				Object newJavaObject = c.newInstance(constructorArgs);

				newJBasicObject = new ObjectValue(newJavaObject);
				return newJBasicObject;
			} catch (JBasicException jbe ) {
				throw jbe;
			} catch( Exception e ) {
				throw new JBasicException(Status.INVCLASS, ObjectValue.methodSignature( className, constructorArgClasses));
			}
		}

		/* It was not a Java object creation so process this as a JBasic
		 * object creation operation.
		 * 
		 * Make sure that the args are valid, the first object is a legitimate
		 * class or object value, and check to see if this is a Java Class
		 * designation created by the JAVACLASS() function.
		 */
		argList.validate(1, 2, new int[] { Value.RECORD, Value.STRING });

		Value classObject = argList.element(0);
		Value objectData = classObject.getElement("_OBJECT$DATA");
		if( objectData == null )
			throw new JBasicException(Status.INVCLASS, classObject.toString());

		/*
		 * Is this a class designation?
		 */

		Value classValue = objectData.getElement("CLASS");
		if( classValue == null )
			throw new JBasicException(Status.INVCLASS, classObject.toString());
		className = classValue.getString();

		/*
		 * Is it a Java class?
		 */

		Value isJava = objectData.getElement("ISJAVA");
		if( isJava != null && isJava.getBoolean()) {
			/*
			 * Must be allowed to create Java classes, which is a security
			 * risk if not used properly.
			 */

			argList.session.checkPermission(Permissions.JAVA);

			ObjectValue v = null;
			try {
				Class theClass = Class.forName(className);
				Object theObject = theClass.newInstance();
				v = new ObjectValue(theObject);
				return v;
			} catch( Exception e ) {
				throw new JBasicException(Status.INVCLASS, className);
			}
		}

		/*
		 * Plain old JBasic object, do that work now.
		 */
		final Value newObject = classObject.copy(true, 0);
		newObject.setName(null);
		newObject.setObjectAttribute("ID", new Value(JBasic.getUniqueID()));
		newObject.setObjectAttribute("ISJAVA", new Value(false));
		newObject.setObjectAttribute("CLASS", classObject.getObjectAttribute("NAME"));
		if (argList.size() == 1) {
			newObject.setObjectAttribute("ISCLASS", new Value(false));
			newObject.removeObjectAttribute("NAME");
		}
		else
			newObject.setObjectAttribute("NAME", new Value(argList.element(
					1).getString().toUpperCase()));
		return newObject;
	}

}

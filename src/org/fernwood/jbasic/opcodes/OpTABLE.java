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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.value.RecordStreamValue;
import org.fernwood.jbasic.value.Value;

/**
 * @author cole
 * 
 */
public class OpTABLE extends AbstractOpcode {

	/**
	 * Create a new TABLE value on the stack
	 * 
	 * @see org.fernwood.jbasic.opcodes.AbstractOpcode#execute(org.fernwood.jbasic.opcodes.InstructionContext)
	 */
	public void execute(final InstructionContext env) throws JBasicException {

		final int count = env.instruction.integerOperand;
		
		Value names = new Value(Value.ARRAY, null );
		
		/*
		 * There are several ways the metadata array can be constructed.  The
		 * simplest is when there is a non-zero count for _TABLE that tells
		 * how many _STRING definitions exist. Just copy them into the array.
		 */
		
		if( count > 0 ) {
			for( int idx = 0; idx < count; idx ++ ) {
				Value name = env.popForUpdate();
				name.coerce(Value.STRING);
				names.addElement(name);
			}
		}
		
		/*
		 * Or, there is no count and the top of the stack is an array.  If
		 * so, then the array can take two forms; an array of strings in which
		 * case it's the metadata array and we are done, or an array of records
		 * in which case we need NAME and TYPE values from the record; this
		 * matches the metadata for a DATABASE file type.
		 */
		
		else {
			Value metadata = env.pop();
			if( !metadata.isType(Value.ARRAY)) 
				throw new JBasicException(Status.WRONGTYPE, "ARRAY");
			
			int foundType = Value.UNDEFINED;
			for( int ix = 1; ix <= metadata.size(); ix++ ) {
				int itemType = metadata.getElement(ix).getType();
				if( foundType == Value.UNDEFINED)
					foundType = itemType;
				else
					if( foundType != itemType )
						throw new JBasicException(Status.INVTYPE);
			}
			if( foundType != Value.STRING && foundType != Value.RECORD)
				throw new JBasicException(Status.INVTYPE);

			if( foundType == Value.STRING )
				names = metadata;
			else
				for( int ix = 1; ix <= metadata.size(); ix++) {
					Value item = metadata.getElement(ix);
					Value name = item.getElement("NAME");
					Value type = item.getElement("TYPE");
					if( name == null )
						throw new JBasicException(Status.NOSUCHMEMBER, "NAME");
					if( type == null )
						throw new JBasicException(Status.NOSUCHMEMBER, "TYPE");
					names.setElement(new Value(name.getString() + "@" + type.getString()), ix);
				}
		}
		RecordStreamValue result = new RecordStreamValue(names);
		env.push(result);

	}

}

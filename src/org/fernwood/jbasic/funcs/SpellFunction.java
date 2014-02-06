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

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.JBasicException;
import org.fernwood.jbasic.runtime.SymbolTable;
import org.fernwood.jbasic.value.Value;

/**
 * <b>SPELL()</b> JBasic Function
 * <p>
 * <table>
 * <tr><td><b>Description:</b></td><td>Given a numeric parameter, "spell" it in text format.</td></tr>
 * <tr><td><b>Invocation:</b></td><td><code>s = SPELL( <em>integer-expression<em> [,<em>language-code</code>] )</code></td></tr>
 * <tr><td><b>Returns:</b></td><td>String</td></tr>
 * </table>
 * <p>
 * Given a numeric parameter, provide a text spelling of the argument.  This must
 * be sensitive to the current language encoding, and may return a value of
 * "NOT SUPPORTED IN LANGUAGE(xx)" where "xx" is the current language encoding.
 * At a minimum, "EN" will be supported.
 * <p>
 * If the second parameter is given, it must be a string with the language encoding
 * to use in place of the default.
 * 
 * @author cole
 * 
 */
public class SpellFunction extends JBasicFunction {

	/**
	 * Execute the function
	 * 
	 * @param arglist
	 *            The function argument list
	 * @param symbols
	 *            The runtime symbol table
	 * @return the Value of the function.
	 * @throws JBasicException  There was an error in the type or count of
	 * function arguments.
	 */
	
	public Value run(final ArgumentList arglist, final SymbolTable symbols) throws JBasicException {

		arglist.validate(1, 2, new int [] { Value.INTEGER, Value.STRING });
		
		String languageCode = arglist.session.getString("SYS$LANGUAGE");
		if( arglist.size() == 2 )
			languageCode = arglist.stringElement(1).toUpperCase().substring(0, 2);
		
		if( languageCode.equals("EN"))
			return new Value(spellEnglish( arglist.intElement(0)));
			
		if( languageCode.equals("FR"))
			return new Value(spellFrench( arglist.intElement(0)));
		
		throw new JBasicException(Status.LANGUAGE, languageCode);
		
	}

	/**
	 * @param integerValue
	 * @return String with value spelled in English.
	 */
	private String spellEnglish(int integerValue) {
		
		int i = integerValue;
		StringBuffer r = new StringBuffer();
		
		if( i < 0 ) {
			i = -i;
			r.append("minus");
		}
		if( i >= 1000000000) {
			int m = i / 1000000000;
			i = i % 1000000000;
			if( r.length() > 0)
				r.append(" ");
			r.append( spellEnglish(m));
			r.append( " billion");
			if( i == 0 )
				return r.toString();
		}
		
		if( i >= 1000000) {
			int m = i / 1000000;
			i = i % 1000000;
			if( r.length() > 0)
				r.append(" ");
			r.append( spellEnglish(m));
			r.append( " million");
			if( i == 0 )
				return r.toString();
		}
		
		if( i >= 1000) {
			int t = i / 1000;
			i = i % 1000;
			if( r.length() > 0)
				r.append(" ");
			r.append( spellEnglish(t));
			r.append( " thousand");
			if( i == 0 )
				return r.toString();
		}
		
		if( i >= 100 ) {
			int c = i / 100;
			i = i % 100;
			if( r.length() > 0 )
				r.append(" " );
			r.append( spellEnglish(c));
			r.append( " hundred");
			if( i == 0 )
				return r.toString();
		}
		
		if( i > 19 ) {
			int d = i / 10;
			i = i % 10;
			if( r.length() > 0 )
				r.append (" " );
			switch ( d ) {
			case 2: r.append("twenty"); break;
			case 3: r.append("thirty"); break;
			case 4: r.append("forty"); break;
			case 5: r.append("fifty"); break;
			case 6: r.append("sixty"); break;
			case 7: r.append("seventy"); break;
			case 8: r.append("eighty"); break;
			case 9: r.append("ninety"); break;
			}
		}
		
		if( i == 0 & r.length() == 0 )
			return "zero";
		
		if( r.length() > 0 )
			r.append(" " );
		switch(i) { 
		case 1: 	r.append("one");		break;
		case 2: 	r.append("two");		break;
		case 3: 	r.append("three");		break;
		case 4: 	r.append("four");		break;
		case 5: 	r.append("five");		break;
		case 6: 	r.append("six");		break;
		case 7: 	r.append("seven");		break;
		case 8: 	r.append("eight");		break;
		case 9: 	r.append("nine");		break;
		case 10:	r.append("ten");		break;
		case 11:	r.append("eleven");		break;
		case 12:	r.append("twelve");		break;
		case 13:	r.append("thirteen");	break;
		case 14:	r.append("fourteen");	break;
		case 15:	r.append("fifteen");	break;
		case 16:	r.append("sixteen");	break;
		case 17:	r.append("seventeen");	break;
		case 18:	r.append("eighteen");	break;
		case 19:	r.append("nineteen");	break;
		
		}
		return r.toString();
	}
	
	/**
	 * @param intValue
	 * @return String with value spelled in English.
	 */
	private String spellFrench(int intValue) {
		
		int i = intValue;
		StringBuffer r = new StringBuffer();
		
		if( i >= 1000000000) {
			int m = i / 1000000000;
			i = i % 1000000000;
			r.append( spellFrench(m));
			r.append( " milliard");
			if( i == 0 )
				return r.toString();
		}
		
		if( i >= 1000000) {
			int m = i / 1000000;
			i = i % 1000000;
			if( r.length() > 0)
				r.append(" ");
			r.append( spellFrench(m));
			r.append( " million");
			if( i == 0 )
				return r.toString();
		}
		
		if( i >= 1000) {
			int t = i / 1000;
			i = i % 1000;
			if( r.length() > 0)
				r.append(" ");
			if( t > 1 ) {
				r.append( spellFrench(t));
				r.append(" ");
			}
			r.append( "mille");
			if( i == 0 )
				return r.toString();
		}
		
		if( i >= 100 ) {
			int c = i / 100;
			i = i % 100;
			if( r.length() > 0 )
				r.append(" " );
			r.append( spellFrench(c));
			r.append( " cent");
			if( i == 0 )
				return r.toString();
		}
		
		if( i > 19 ) {
			int d = i / 10;
			i = i % 10;
			if( r.length() > 0 )
				r.append (" " );
			switch ( d ) {
			case 2: r.append("vingt"); break;
			case 3: r.append("trente"); break;
			case 4: r.append("quarante"); break;
			case 5: r.append("cinqant"); break;
			case 6: r.append("soixant"); break;
			case 7: r.append("soixant-dix"); break;
			case 8: r.append("quatre-vingts"); break;
			case 9: r.append("quatre-vingts-dix"); break;
			}
		}
		
		if( i == 0 & r.length() == 0 )
			return "zero";
		
		if( r.length() > 0 ) {
			if( i == 1 )
				r.append(" et ");
			else
				if( i > 1 )
					r.append("-");
		}
		
		switch(i) { 
		case 1: 	r.append("un");		break;
		case 2: 	r.append("deux");		break;
		case 3: 	r.append("trois");		break;
		case 4: 	r.append("quatre");		break;
		case 5: 	r.append("cinq");		break;
		case 6: 	r.append("six");		break;
		case 7: 	r.append("sept");		break;
		case 8: 	r.append("huit");		break;
		case 9: 	r.append("neuf");		break;
		case 10:	r.append("dix");		break;
		case 11:	r.append("onze");		break;
		case 12:	r.append("douze");		break;
		case 13:	r.append("trieze");	break;
		case 14:	r.append("quatorze");	break;
		case 15:	r.append("quinze");	break;
		case 16:	r.append("seize");	break;
		case 17:	r.append("dix-sept");	break;
		case 18:	r.append("dix-huit");	break;
		case 19:	r.append("dix-neuf");	break;
		
		}
		return r.toString();
	}
}

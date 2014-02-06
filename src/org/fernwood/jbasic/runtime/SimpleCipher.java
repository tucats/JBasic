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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.fernwood.jbasic.JBasic;
import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.value.Value;

/**
 * Support for CIPHER() and DECIPHER() functions.  This is a simple shell
 * around the java security mechanisms.  It handles setting up and managing simple
 * password-based encryption and decryption, using the strong levels of encryption
 * (as opposed to weapons-grade encryption.
 * 
 * @author cole
 *
 */
public class SimpleCipher {
	
	private String currentPassword;
	
	private JBasic env;
	
	private Status status;
	
	/**
	 * Create an instance of the cipher, with a default password.
	 * @param env
	 * The JBasic session object that contains this instance of an object.
	 */
	public SimpleCipher(JBasic env) {
		currentPassword = "SecretSauce";
		status = new Status();
	}
	
	/**
	 * Return the most recent status setting of the cipher object.
	 * @return a Status value indicating the last operation's state.  This will
	 * normally be SUCCESS.
	 */
	public Status getStatus() {
		return status;
	}
	/**
	 * Set the password string to be used with this encryption object.
	 * @param pw a Value containing the password.  This can be a STRING which
	 * is the actual password, or an ARRAY that contains an encrypted
	 * instance of the password.
	 */
	public void setPassword( Value pw ) {
		
		if( pw == null)
			currentPassword = "What the dead carry to the grave";
		else
		if( pw.getType() == Value.ARRAY ) {
			SimpleCipher s = new SimpleCipher(env);
			s.setPassword("SecretSauce");
			currentPassword = s.decrypt(pw).getString();
			//System.out.println("DEBUG: default pw = " + currentPassword);
		}
		else {
			currentPassword = pw.getString();
		}
		status = new Status();
	}
	
	/**
	 * Set the password to be used with this encryption object.
	 * @param pw a String containing the password string.
	 */
	public void setPassword( String pw ) {
		currentPassword = pw;
		status = new Status();
	}
	
	/**
	 * Decrypt a value object, given a password value.
	 * @param codedData The object containing an encoded array of integer
	 * values.
	 * @param dfpw A Value containing the decryption key.
	 * @return The resulting string, expressed as a value.
	 */
	
	public Value decrypt( Value codedData, Value dfpw ) {
		currentPassword = dfpw.getString();
		return decrypt( codedData );
	}
	
	/**
	 * Decrypt a coded array.  The coded array is an ARRAY Value that contains
	 * a list of integers (in the range -128..127).  These are decrypted
	 * using the current decryption password, and the result is returned
	 * as a STRING value.
	 * @param codedData the encoded data expressed as an array of integers.
	 * @return The decoded string.
	 */
	public Value decrypt( Value codedData ) {

		status = new Status();
		Cipher c1 = null;
		try {
			c1 = Cipher.getInstance("PBEWithMD5AndDES");
		} catch (NoSuchAlgorithmException e) {
			status = new Status(Status.FAULT, "Password-based encryption not installed on this system");
			status.print(env);
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}

		byte[] salt = {
		        (byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
		        (byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99
		    };

		    // Iteration count
		    int count = 20;
		    
		    PBEKeySpec pbeKeySpec = null;
		    PBEParameterSpec pbeParamSpec = null;
		    SecretKeyFactory keyFac = null;

		    // Create PBE parameter set
		    pbeParamSpec = new PBEParameterSpec(salt, count);
		    if( currentPassword == null )
		    	currentPassword = "cheerios";
		    
		    char[] pwc = currentPassword.toCharArray();
		    
		    pbeKeySpec = new PBEKeySpec(pwc);
		    try {
				keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
			} catch (NoSuchAlgorithmException e1) {
				status = new Status(Status.FAULT, "Password-based encryption not installed on this system");
				status.print(env);
			}
		    SecretKey pbeKey = null;
			try {
				pbeKey = keyFac.generateSecret(pbeKeySpec);
			} catch (InvalidKeySpecException e1) {
				e1.printStackTrace();
			}

		
		try {
			c1.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec );
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		
		Value array = codedData;
		int len = array.size();
		
		byte[] in = new byte[len];
		int cx;
		for( cx = 0; cx < len; cx++ ) {
			in[cx] = (byte) array.getInteger(cx+1);
		}

		byte[] out = null;
		
		try {
			out = c1.doFinal(in);
		} catch (Exception e) {
			return new Value("");
		}
		
		char[] outb = new char[out.length];
		for( cx = 0; cx < out.length; cx++ ) 
			outb[cx] = (char) out[cx];
		
		return new Value( String.copyValueOf(outb));
	}
	
	/**
	 * Encrypt a STRING value object, using a password STRING value.
	 * @param string The string to encode
	 * @param newPassword The password to use for the encoding
	 * @return A value containing an encoded array of integers representing
	 * the encrypted string.
	 */
	public Value encrypt( Value string, Value newPassword ) {
		currentPassword = newPassword.getString();
		return encrypt(string);
	}
	
	/**
	 * Encrypt a STRING value object, using the default password for
	 * this encryption object.
	 * @param string The string to encode
	 * @return A value containing an encoded array of integers representing
	 * the encrypted string.
	 */

	public Value encrypt( Value string ) {
		Cipher c1 = null;
		status = new Status();
		
		try {
			c1 = Cipher.getInstance("PBEWithMD5AndDES");
		} catch (NoSuchAlgorithmException e) {
			status = new Status(Status.FAULT, "Password-based encryption not installed on this system");
			status.print(env);
			return null;
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		}

		byte[] salt = {
		        (byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
		        (byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99
		    };

		    // Iteration count
		    int count = 20;
		    
		    PBEKeySpec pbeKeySpec = null;
		    PBEParameterSpec pbeParamSpec = null;
		    SecretKeyFactory keyFac = null;

		    // Create PBE parameter set
		    pbeParamSpec = new PBEParameterSpec(salt, count);
		    if( currentPassword == null )
		    	currentPassword = "cheerios";
		    
		    char[] pwc = currentPassword.toCharArray();
		    
		    pbeKeySpec = new PBEKeySpec(pwc);
		    try {
				keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
			} catch (NoSuchAlgorithmException e1) {
				status = new Status(Status.FAULT, "Password-based encryption not installed on this system");
				status.print(env);
			}
		    SecretKey pbeKey = null;
			try {
				pbeKey = keyFac.generateSecret(pbeKeySpec);
			} catch (InvalidKeySpecException e1) {
				e1.printStackTrace();
			}

		
		try {
			c1.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec );
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		
		byte[] in = string.getString().getBytes();
		byte[] out = null;
		
		try {
			out = c1.doFinal(in);
		} catch (Exception e) {
			return new Value("");
		}
		
		int len = out.length;
		Value v = new Value(Value.ARRAY, null);
		
		for( int cx = 0; cx < len; cx++ ) {
			v.setElement(new Value( out[cx]), cx+1);
		}
		return v;
	}
	
	/**
	 * Given a string (which is a password), create a string that is a hash
	 * of that password.
	 * @param password the password to convert to a hash
	 * @param secret a secret value used to salt the hash
	 * @return hashed password string.
	 */
	public String encryptedString( String password, String secret ) {
		Value codeArray = encrypt( new Value(password), new Value(secret));
		
		StringBuffer result = new StringBuffer(codeArray.size()*2);
		final String hexchar = "APKMQRSLNZYWHGCX";
		for( int ix = 1; ix <= codeArray.size(); ix++ ) {
			int b = codeArray.getInteger(ix);
			if( b < 0 )
				b = -b + 127;
			result.append(hexchar.charAt(b/16));
			result.append(hexchar.charAt(b%16));
		}
		return result.toString();
	}

}

package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class Credentials implements Serializable  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7444283028651497389L;
	private String user;
	private String password;

	public Credentials(String user, String password) {
		if (user == null || password == null)
			throw new IllegalArgumentException("User or password are null");
		this.user = user;
		this.password = password;
	}

	public String user() {
		return user;
	}

	public String password() {
		return password;
	}

	public String getBasicAuthorizationHeader() throws SEPASecurityException {
		String plainString = user + ":" + password;
		try {
			return "Basic " + new String(Base64.getEncoder().encode(plainString.getBytes("UTF-8")), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SEPASecurityException(e);
		}
	}
	
	public byte[] serialize() throws SEPASecurityException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(this);
			out.flush();
			return bos.toByteArray();
		} catch (IOException e) {
			throw new SEPASecurityException(e);
		} finally {
			try {
				bos.close();
			} catch (IOException ex) {

			}
		}	
	}
	
	public static Credentials deserialize(byte[] stream) throws SEPASecurityException {
		ByteArrayInputStream bis = new ByteArrayInputStream(stream);
		ObjectInput in = null;
		try {
		  in = new ObjectInputStream(bis);
		 return (Credentials) in.readObject(); 
		} catch (IOException | ClassNotFoundException e) {
			throw new SEPASecurityException(e);
		} finally {
		  try {
		    if (in != null) {
		      in.close();
		    }
		  } catch (IOException ex) {
		  }
		}	
	}	
}

/* The class represents the credentials of a generic identity
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.vaimee.sepa.api.commons.security;

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

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.logging.Logging;

public class Credentials implements Serializable  {
	private static final long serialVersionUID = 7444283028651497389L;
	private String user;
	private String password;

	public Credentials(String user, String password) {
		if (user == null || password == null)
			throw new IllegalArgumentException("User or password are null");
		this.user = user;
		this.password = password;
	}
	
	public String toString() {
		return "Credentials "+user+":"+password;
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
			Logging.getLogger().error(e.getMessage());
			throw new SEPASecurityException(e.getMessage());
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
			Logging.getLogger().error(e.getMessage());
			throw new SEPASecurityException("Serialize exception: "+e.getMessage());
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
			Logging.getLogger().error(e.getMessage());
			throw new SEPASecurityException("Deserialize exception: "+e.getMessage());
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

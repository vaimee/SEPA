/* This class is part of the SPARQL 1.1 SE Protocol (an extension of the W3C SPARQL 1.1 Protocol) API
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.api;

import java.io.File;
import java.security.Key;

import java.util.Base64;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

/**
 * The Class SPARQL11SEProperties.
 *
 * <pre>
 * {"parameters" : {
		"host" : "localhost" ,
		"ports" : {
			"http" : 8000 ,
			"https" : 8443 ,
			"ws" : 9000 ,
			"wss" : 9443}
		 ,
		"paths" : {
			"query" : "/query" ,
			"update" : "/update" ,
			"subscribe" : "/subscribe" ,
			"register" : "/oauth/register" ,
			"tokenRequest" : "/oauth/token" ,
			"securePath" : "/secure"}
		 ,
		"methods" : {
			"query" : "POST" ,
			"update" : "URL_ENCODED_POST"}
		 ,
		"formats" : {
			"query" : "JSON" ,
			"update" : "HTML"}
		 ,

		[OPTIONAL]
		"security" : {
			"client_id" : "..." ,
			"client_secret" : "..." ,
			"jwt" : "..." ,
			"expires" : "..." ,
			"type" : "..."}
	}}
 * </pre>
 */
public class SPARQL11SEProperties extends SPARQL11Properties {

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger("SPARQL11SEProperties");

	/**
	 * The new primitives introduced by the SPARQL 1.1 SE Protocol are:
	 *
	 * SECUREUPDATE,SECUREQUERY,SUBSCRIBE,SECURESUBSCRIBE,UNSUBSCRIBE,SECUREUNSUBSCRIBE,REGISTER,REQUESTTOKEN
	 *
	 *
	 * @author Luca Roffia (luca.roffia@unibo.it)
	 * @version 0.1
	 */
	public enum SPARQL11SEPrimitive {
		/** A secure update primitive */
		SECUREUPDATE,
		/** A subscribe primitive */
		SUBSCRIBE,
		/** A secure subscribe primitive. */
		SECURESUBSCRIBE,
		/** A unsubscribe primitive. */
		UNSUBSCRIBE,
		/** A secure unsubscribe primitive. */
		SECUREUNSUBSCRIBE,
		/** A register primitive. */
		REGISTER,
		/** A request token primitive. */
		REQUESTTOKEN,
		/** A secure query primitive. */
		SECUREQUERY
	}

	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param propertiesFile
	 *            the properties file
	 * @param secret
	 *            the secret
	 * @throws SEPAPropertiesException
	 */
	public SPARQL11SEProperties(String propertiesFile, byte[] secret) throws SEPAPropertiesException {
		this(new File(propertiesFile));

		SEPAEncryption.init(secret);
	}

	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param propertiesFile
	 *            the properties file
	 * @throws SEPAPropertiesException
	 */
	public SPARQL11SEProperties(String propertiesFile) throws SEPAPropertiesException {
		this(propertiesFile, null);

		if (propertiesFile == null)
			throw new IllegalArgumentException("Argument is null");
	}
	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param propertiesFile
	 *            the properties file
	 * @throws SEPAPropertiesException
	 */
	public SPARQL11SEProperties(File propertiesFile) throws SEPAPropertiesException {
		super(propertiesFile);
	}

	public String toString() {
		return parameters.toString();
	}

	/**
	 * <pre>
	 * {"parameters" : {
			"host" : "localhost" ,
			"ports" : {
				"http" : 8000 ,
				"https" : 8443 ,
				"ws" : 9000 ,
				"wss" : 9443}
			 ,
			"paths" : {
				"query" : "/query" ,
				"update" : "/update" ,
				"subscribe" : "/subscribe" ,
				"register" : "/oauth/register" ,
				"tokenRequest" : "/oauth/token" ,
				"securePath" : "/secure"}
			 ,
			"methods" : {
				"query" : "POST" ,
				"update" : "URL_ENCODED_POST"}
			 ,
			"formats" : {
				"query" : "JSON" ,
				"update" : "HTML"}
			 ,

			[OPTIONAL]
			"security" : {
				"client_id" : "..." ,
				"client_secret" : "..." ,
				"jwt" : "..." ,
				"expires" : "..." ,
				"type" : "..."}
		}}
	 * </pre>
	 */
	@Override
	protected void defaults() {
		super.defaults();

		// JsonObject ports = parameters.get("ports").getAsJsonObject();
		// ports.add("https", new JsonPrimitive(8443));
		// ports.add("ws", new JsonPrimitive(9000));
		// ports.add("wss", new JsonPrimitive(9443));

		JsonObject paths = parameters.get("paths").getAsJsonObject();
		paths.add("subscribe", new JsonPrimitive("/subscribe"));
		paths.add("register", new JsonPrimitive("/oauth/register"));
		paths.add("tokenRequest", new JsonPrimitive("/oauth/token"));
		paths.add("securePath", new JsonPrimitive("/secure"));
	}

	@Override
	protected void validate() throws SEPAPropertiesException {
		super.validate();

		try {
			// parameters.get("ports").getAsJsonObject().get("https").getAsInt();
			// parameters.get("ports").getAsJsonObject().get("ws").getAsInt();
			// parameters.get("ports").getAsJsonObject().get("wss").getAsInt();

			parameters.get("paths").getAsJsonObject().get("subscribe").getAsString();
			parameters.get("paths").getAsJsonObject().get("register").getAsString();
			parameters.get("paths").getAsJsonObject().get("tokenRequest").getAsString();
			parameters.get("paths").getAsJsonObject().get("securePath").getAsString();

		} catch (Exception e) {
			throw new SEPAPropertiesException(e);
		}
	}

	public String getSecurePath() {
		return parameters.get("paths").getAsJsonObject().get("securePath").getAsString();
	}

	public int getWsPort() {
		try {
			return parameters.get("ports").getAsJsonObject().get("ws").getAsInt();
		} catch (Exception e) {
			return -1;
		}
	}

	public String getSubscribePath() {
		return parameters.get("paths").getAsJsonObject().get("subscribe").getAsString();
	}

	public int getWssPort() {
		try {
			return parameters.get("ports").getAsJsonObject().get("wss").getAsInt();
		} catch (Exception e) {
			return -1;
		}
	}

	public int getHttpsPort() {
		try {
			return parameters.get("ports").getAsJsonObject().get("https").getAsInt();
		} catch (Exception e) {
			return -1;
		}
	}

	public String getRegisterPath() {
		return parameters.get("paths").getAsJsonObject().get("register").getAsString();
	}

	public String getTokenRequestPath() {
		return parameters.get("paths").getAsJsonObject().get("tokenRequest").getAsString();
	}

	private String getSecurityEncryptedValue(String value) throws SEPASecurityException {
		try {
			return parameters.get("security").getAsJsonObject().get(value).getAsString();
		} catch (Exception e) {
			throw new SEPASecurityException(e);
		}
	}

	/**
	 * Checks if is token expired.
	 *
	 * @return true, if is token expired
	 * @throws SEPASecurityException
	 * @throws NumberFormatException
	 */
	public boolean isTokenExpired() throws SEPASecurityException {
		Long expires = 0L;

		String encryptedValue = getSecurityEncryptedValue("expires");

		if (encryptedValue == null)
			return true;

		expires = Long.decode(SEPAEncryption.decrypt(encryptedValue));

		return (new Date().getTime() >= expires);
	}

	/**
	 * Gets the expiring seconds.
	 *
	 * @return the expiring seconds
	 * @throws SEPASecurityException
	 * @throws NumberFormatException
	 */
	public long getExpiringSeconds() throws SEPASecurityException {
		Long expires = 0L;

		String encryptedValue = getSecurityEncryptedValue("expires");

		if (encryptedValue == null)
			return 0;

		expires = Long.decode(SEPAEncryption.decrypt(encryptedValue));

		long seconds = ((expires - new Date().getTime()) / 1000);

		if (seconds < 0)
			seconds = 0;

		return seconds;
	}

	/**
	 * Gets the access token.
	 *
	 * @return the access token
	 * @throws SEPASecurityException
	 */
	public String getAccessToken() throws SEPASecurityException {
		String encryptedValue = getSecurityEncryptedValue("jwt");

		if (encryptedValue == null)
			return null;

		return SEPAEncryption.decrypt(encryptedValue);
	}

	/**
	 * Gets the token type.
	 *
	 * @return the token type
	 * @throws SEPASecurityException
	 */
	public String getTokenType() throws SEPASecurityException {
		String encryptedValue = getSecurityEncryptedValue("type");

		if (encryptedValue == null)
			return null;

		return SEPAEncryption.decrypt(encryptedValue);
	}

	/**
	 * Gets the basic authorization.
	 *
	 * @return the basic authorization
	 * @throws SEPASecurityException
	 */
	public String getBasicAuthorization() throws SEPASecurityException {
		String encryptedValue;

		if (parameters.get("security").getAsJsonObject().get("client_id") != null
				&& parameters.get("security").getAsJsonObject().get("client_secret") != null) {
			encryptedValue = parameters.get("security").getAsJsonObject().get("client_id").getAsString();

			String id = SEPAEncryption.decrypt(encryptedValue);

			encryptedValue = parameters.get("security").getAsJsonObject().get("client_secret").getAsString();

			String secret = SEPAEncryption.decrypt(encryptedValue);

			String authorization;
			try {
				byte[] buf = Base64.getEncoder().encode((id + ":" + secret).getBytes("UTF-8"));
				// authorization = Base64.getEncoder().encode((id + ":" +
				// secret).getBytes("UTF-8")).toString();
				authorization = new String(buf, "UTF-8");
			} catch (Exception e) {
				throw new SEPASecurityException(e);
			}

			return authorization;// .replace("\n", "");

		}

		return null;
	}

	/**
	 * Sets the credentials.
	 *
	 * @param id
	 *            the username
	 * @param secret
	 *            the password
	 * @throws SEPASecurityException
	 * @throws SEPAPropertiesException
	 */
	public void setCredentials(String id, String secret) throws SEPASecurityException, SEPAPropertiesException {
		logger.debug("Set credentials Id: " + id + " Secret:" + secret);

		// Save on file the encrypted version
		if (parameters.get("security") == null) {
			JsonObject credentials = new JsonObject();
			credentials.add("client_id", new JsonPrimitive(SEPAEncryption.encrypt(id)));
			credentials.add("client_secret", new JsonPrimitive(SEPAEncryption.encrypt(secret)));
			parameters.add("security", credentials);
		} else {
			parameters.get("security").getAsJsonObject().add("client_id",
					new JsonPrimitive(SEPAEncryption.encrypt(id)));
			parameters.get("security").getAsJsonObject().add("client_secret",
					new JsonPrimitive(SEPAEncryption.encrypt(secret)));
		}

		storeProperties(propertiesFile.getAbsolutePath());
	}

	/**
	 * Sets the JWT.
	 *
	 * @param jwt
	 *            the JSON Web Token
	 * @param expires
	 *            the date when the token will expire
	 * @param type
	 *            the token type (e.g., bearer)
	 * @throws SEPAPropertiesException
	 * @throws SEPASecurityException
	 *
	 */
	public void setJWT(String jwt, Date expires, String type) throws SEPASecurityException, SEPAPropertiesException {

		// Save on file the encrypted version
		if (parameters.get("security") == null) {
			JsonObject credentials = new JsonObject();
			credentials.add("jwt", new JsonPrimitive(SEPAEncryption.encrypt(jwt)));
			credentials.add("expires",
					new JsonPrimitive(SEPAEncryption.encrypt(String.format("%d", expires.getTime()))));
			credentials.add("type", new JsonPrimitive(SEPAEncryption.encrypt(type)));
			parameters.add("security", credentials);
		} else {
			parameters.get("security").getAsJsonObject().add("jwt", new JsonPrimitive(SEPAEncryption.encrypt(jwt)));
			parameters.get("security").getAsJsonObject().add("expires",
					new JsonPrimitive(SEPAEncryption.encrypt(String.format("%d", expires.getTime()))));
			parameters.get("security").getAsJsonObject().add("type", new JsonPrimitive(SEPAEncryption.encrypt(type)));
		}

		storeProperties(propertiesFile.getAbsolutePath());
	}

	/**
	 * The Class SEPAEncryption.
	 */
	private static class SEPAEncryption {

		/** The Constant ALGO. */
		// AES 128 bits (16 bytes)
		private static final String ALGO = "AES";

		/** The key value. */
		private static byte[] keyValue = new byte[] { '0', '1', 'R', 'a', 'v', 'a', 'm', 'i', '!', 'I', 'e', '2', '3',
				'7', 'A', 'N' };

		/** The key. */
		private static Key key = new SecretKeySpec(keyValue, ALGO);

		/**
		 * Inits the.
		 *
		 * @param secret
		 *            the secret
		 */
		private static void init(byte[] secret) {
			if (secret != null && secret.length == 16)
				keyValue = secret;
			key = new SecretKeySpec(keyValue, ALGO);
		}

		/**
		 * Encrypt.
		 *
		 * @param Data
		 *            the data
		 * @return the string
		 * @throws SEPASecurityException
		 *
		 */
		static String encrypt(String Data) throws SEPASecurityException {
			Cipher c;
			try {
				c = Cipher.getInstance(ALGO);
				c.init(Cipher.ENCRYPT_MODE, key);
				return new String(Base64.getEncoder().encode(c.doFinal(Data.getBytes("UTF-8"))));
			} catch (Exception e) {
				throw new SEPASecurityException(e);
			}

		}

		/**
		 * Decrypt.
		 *
		 * @param encryptedData
		 *            the encrypted data
		 * @return the string
		 * @throws SEPASecurityException
		 *
		 */
		static String decrypt(String encryptedData) throws SEPASecurityException {
			Cipher c;
			try {
				c = Cipher.getInstance(ALGO);
				c.init(Cipher.DECRYPT_MODE, key);
				return new String(c.doFinal(Base64.getDecoder().decode(encryptedData)));
			} catch (Exception e) {
				throw new SEPASecurityException(e);
			}

		}
	}
}

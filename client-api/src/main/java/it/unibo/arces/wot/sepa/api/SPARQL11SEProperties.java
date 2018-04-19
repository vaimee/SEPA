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
 "sparql11seprotocol": {
		"protocol": "ws",
		"availableProtocols": {
			"ws": {
				"port": 9000,
				"path": "/subscribe"
			},
			"wss": {
				"port": 9443,
				"path": "/subscribe"
			}
		},
		"security": {
			"register": "/oauth/register",
			"tokenRequest": "/oauth/token",
			"securePath": "/secure",
			"client_id": "jaJBrmgtqgW9jTLHeVbzSCH6ZIN1Qaf3XthmwLxjhw3WuXtt7VELmfibRNvOdKLs",
			"client_secret": "fkITPTMsHUEb9gVVRMP5CAeIE1LrfBYtNLdqtlTVZ/CqgqcuzEw+ZcVegW5dMnIg",
			"jwt": "xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfvx34vSSnhpkfcdYbZ+7KDaK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkfP7DEKe7LScGYaT4RcuIfNmywI4fAWabAI4zqedYbd5lXmYhbSmXviPTOQPKxhmZptZ6F5Q178nfK6Bik4/0PwUlgMsC6oVFeJtyPWvjfEP0nx9tGMOt+z9Rvbd7enGWRFspUQJS2zzmGlHW1m5QNFdtOCfTLUOKkyZV4JUQxI1CaP+QbIyIihuQDvIMbmNgbvDNBkj9VQOzg1WB7mj4nn4w7T8I9MpOxAXxnaPUvDk8QnL/5leQcUiFVTa1zlzambQ8xr/BojFB52fIz8LsrDRW/+/0CJJVTFYD6OZ/gepFyLK4yOu/rOiTLT5CF9H2NZQd7bi85zSmi50RHFa3358LvL50c4G84Gz7mkDTBV9JxBhlWVNvD5VR58rPcgESwlGEL2YmOQCZzYGWjTc5cyI/50ZX83sTlTbfs+Tab3pBlsRQu36iNznleeKPj6uVvql+3uvcjMEBqqXvj8TKxMi9tCfHA1vt9RijOap8ROHtnIe4iMovPzkOCMiHJPcwbnyi+6jHbrPI18WGghceZQT23qKHDUYQo2NiehLQG9MQZA1Ncx2w4evBTBX8lkBS4aLoCUoTZTlNFSDOohUHJCbeig9eV77JbLo0a4+PNH9bgM/icSnIG5TidBGyJpEkVtD7+/KphwM89izJam3OT",
			"expires": "04/5tRBT5n/VJ0XQASgs/w==",
			"type": "XPrHEX2xHy+5IuXHPHigMw=="
		}
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

	public enum SubscriptionProtocol {
		WS, WSS
	}

	private SubscriptionProtocol subscriptionProtocol;

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
		return jsap.toString();
	}

	/**
	 * <pre>
	"sparql11seprotocol": {
		"protocol": "ws",
		"availableProtocols": {
			"ws": {
				"port": 9000,
				"path": "/subscribe"
			},
			"wss": {
				"port": 9443,
				"path": "/subscribe"
			}
		},
		"security": {
			"register": "/oauth/register",
			"tokenRequest": "/oauth/token",
			"securePath": "/secure",
			"client_id": "jaJBrmgtqgW9jTLHeVbzSCH6ZIN1Qaf3XthmwLxjhw3WuXtt7VELmfibRNvOdKLs",
			"client_secret": "fkITPTMsHUEb9gVVRMP5CAeIE1LrfBYtNLdqtlTVZ/CqgqcuzEw+ZcVegW5dMnIg",
			"jwt": "xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfvx34vSSnhpkfcdYbZ+7KDaK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkfP7DEKe7LScGYaT4RcuIfNmywI4fAWabAI4zqedYbd5lXmYhbSmXviPTOQPKxhmZptZ6F5Q178nfK6Bik4/0PwUlgMsC6oVFeJtyPWvjfEP0nx9tGMOt+z9Rvbd7enGWRFspUQJS2zzmGlHW1m5QNFdtOCfTLUOKkyZV4JUQxI1CaP+QbIyIihuQDvIMbmNgbvDNBkj9VQOzg1WB7mj4nn4w7T8I9MpOxAXxnaPUvDk8QnL/5leQcUiFVTa1zlzambQ8xr/BojFB52fIz8LsrDRW/+/0CJJVTFYD6OZ/gepFyLK4yOu/rOiTLT5CF9H2NZQd7bi85zSmi50RHFa3358LvL50c4G84Gz7mkDTBV9JxBhlWVNvD5VR58rPcgESwlGEL2YmOQCZzYGWjTc5cyI/50ZX83sTlTbfs+Tab3pBlsRQu36iNznleeKPj6uVvql+3uvcjMEBqqXvj8TKxMi9tCfHA1vt9RijOap8ROHtnIe4iMovPzkOCMiHJPcwbnyi+6jHbrPI18WGghceZQT23qKHDUYQo2NiehLQG9MQZA1Ncx2w4evBTBX8lkBS4aLoCUoTZTlNFSDOohUHJCbeig9eV77JbLo0a4+PNH9bgM/icSnIG5TidBGyJpEkVtD7+/KphwM89izJam3OT",
			"expires": "04/5tRBT5n/VJ0XQASgs/w==",
			"type": "XPrHEX2xHy+5IuXHPHigMw=="
		}
	 * </pre>
	 */
	@Override
	protected void defaults() {
		super.defaults();

		JsonObject sparql11seprotocol = new JsonObject();
		sparql11seprotocol.add("protocol", new JsonPrimitive("ws"));

		JsonObject availableProtocols = new JsonObject();
		JsonObject ws = new JsonObject();
		JsonObject wss = new JsonObject();
		ws.add("port", new JsonPrimitive(9000));
		ws.add("path", new JsonPrimitive("/subscribe"));
		availableProtocols.add("ws", ws);
		ws.add("port", new JsonPrimitive(9443));
		ws.add("path", new JsonPrimitive("/subscribe"));
		availableProtocols.add("wss", wss);
		sparql11seprotocol.add("availableProtocols", availableProtocols);

		JsonObject security = new JsonObject();
		security.add("register", new JsonPrimitive("/oauth/register"));
		security.add("tokenRequest", new JsonPrimitive("/oauth/token"));
		security.add("securePath", new JsonPrimitive("/secure"));
		sparql11seprotocol.add("security", security);

		jsap.add("sparql11seprotocol", sparql11seprotocol);
	}

	@Override
	protected void validate() throws SEPAPropertiesException {
		super.validate();

		try {
			String protocol = jsap.get("sparql11seprotocol").getAsJsonObject().get("protocol").getAsString();

			jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject().get(protocol);

			switch (protocol) {
			case "ws":
				subscriptionProtocol = SubscriptionProtocol.WS;
				jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
						.get(protocol).getAsJsonObject().get("port").getAsInt();
				jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
						.get(protocol).getAsJsonObject().get("path").getAsString();

				break;
			case "wss":
				subscriptionProtocol = SubscriptionProtocol.WSS;
				jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
						.get(protocol).getAsJsonObject().get("port").getAsInt();
				jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
						.get(protocol).getAsJsonObject().get("path").getAsString();
				break;
			}

			if (jsap.get("sparql11seprotocol").getAsJsonObject().get("security") != null) {
				jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().get("register")
						.getAsString();
				jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().get("tokenRequest")
						.getAsString();
				jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().get("securePath")
						.getAsString();
			}

		} catch (Exception e) {
			throw new SEPAPropertiesException(e);
		}
	}

	public String getSecurePath() {
		return jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().get("securePath")
				.getAsString();
	}

	public int getWsPort() {
		try {
			return jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
					.get("ws").getAsJsonObject().get("port").getAsInt();
		} catch (Exception e) {
			return -1;
		}
	}

	public String getSubscribePath() {
		switch (jsap.get("sparql11seprotocol").getAsJsonObject().get("protocol").getAsString()) {
		case "ws":
			return jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
					.get("ws").getAsJsonObject().get("path").getAsString();
		case "wss":
			return jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
					.get("wss").getAsJsonObject().get("path").getAsString();
		}
		return null;
	}

	public int getWssPort() {
		try {
			return jsap.get("sparql11seprotocol").getAsJsonObject().get("availableProtocols").getAsJsonObject()
					.get("wss").getAsJsonObject().get("port").getAsInt();
		} catch (Exception e) {
			return -1;
		}
	}

	public int getHttpsPort() {
		try {
			if (jsap.get("sparql11protocol").getAsJsonObject().get("protocol").getAsString().equals("https"))
				return jsap.get("sparql11protocol").getAsJsonObject().get("port").getAsInt();
		} catch (Exception e) {
			return -1;
		}
		return -1;
	}

	public String getRegisterPath() {
		return jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().get("register")
				.getAsString();
	}

	public String getTokenRequestPath() {
		return jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().get("tokenRequest")
				.getAsString();
	}

	private String getSecurityEncryptedValue(String value) throws SEPASecurityException {
		try {
			return jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().get(value)
					.getAsString();
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

		if (jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().get("client_id") != null
				&& jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject()
						.get("client_secret") != null) {
			encryptedValue = jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject()
					.get("client_id").getAsString();

			String id = SEPAEncryption.decrypt(encryptedValue);

			encryptedValue = jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject()
					.get("client_secret").getAsString();

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
		if (jsap.get("sparql11seprotocol").getAsJsonObject().get("security") == null) {
			JsonObject credentials = new JsonObject();
			credentials.add("client_id", new JsonPrimitive(SEPAEncryption.encrypt(id)));
			credentials.add("client_secret", new JsonPrimitive(SEPAEncryption.encrypt(secret)));
			jsap.get("sparql11seprotocol").getAsJsonObject().add("security", credentials);
		} else {
			jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().add("client_id",
					new JsonPrimitive(SEPAEncryption.encrypt(id)));
			jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().add("client_secret",
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
		if (jsap.get("sparql11seprotocol").getAsJsonObject().get("security") == null) {
			JsonObject credentials = new JsonObject();
			credentials.add("jwt", new JsonPrimitive(SEPAEncryption.encrypt(jwt)));
			credentials.add("expires",
					new JsonPrimitive(SEPAEncryption.encrypt(String.format("%d", expires.getTime()))));
			credentials.add("type", new JsonPrimitive(SEPAEncryption.encrypt(type)));
			jsap.get("sparql11seprotocol").getAsJsonObject().add("security", credentials);
		} else {
			jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().add("jwt",
					new JsonPrimitive(SEPAEncryption.encrypt(jwt)));
			jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().add("expires",
					new JsonPrimitive(SEPAEncryption.encrypt(String.format("%d", expires.getTime()))));
			jsap.get("sparql11seprotocol").getAsJsonObject().get("security").getAsJsonObject().add("type",
					new JsonPrimitive(SEPAEncryption.encrypt(type)));
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

	public SubscriptionProtocol getSubscriptionProtocol() {
		return subscriptionProtocol;
	}
}

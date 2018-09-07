package it.unibo.arces.wot.sepa.commons.security;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.JWTResponse;

/**
The set of properties used for client authentication
 * 
 * 
 * <pre>
	"oauth": {
		"enable" : true,
		"register": "https://localhost:8443/oauth/register",
		"tokenRequest": "https://localhost:8443/oauth/token",
		"client_id": "jaJBrmgtqgW9jTLHeVbzSCH6ZIN1Qaf3XthmwLxjhw3WuXtt7VELmfibRNvOdKLs",
		"client_secret": "fkITPTMsHUEb9gVVRMP5CAeIE1LrfBYtNLdqtlTVZ/CqgqcuzEw+ZcVegW5dMnIg",
		"jwt": "xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+1jVSoiipV4Pcv8bH+8wJLJ2yRaVage8/TzdZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR15LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIhydKJWfvx34vSSnhpkfcdYbZ+7KDaK5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYkfP7DEKe7LScGYaT4RcuIfNmywI4fAWabAI4zqedYbd5lXmYhbSmXviPTOQPKxhmZptZ6F5Q178nfK6Bik4/0PwUlgMsC6oVFeJtyPWvjfEP0nx9tGMOt+z9Rvbd7enGWRFspUQJS2zzmGlHW1m5QNFdtOCfTLUOKkyZV4JUQxI1CaP+QbIyIihuQDvIMbmNgbvDNBkj9VQOzg1WB7mj4nn4w7T8I9MpOxAXxnaPUvDk8QnL/5leQcUiFVTa1zlzambQ8xr/BojFB52fIz8LsrDRW/+/0CJJVTFYD6OZ/gepFyLK4yOu/rOiTLT5CF9H2NZQd7bi85zSmi50RHFa3358LvL50c4G84Gz7mkDTBV9JxBhlWVNvD5VR58rPcgESwlGEL2YmOQCZzYGWjTc5cyI/50ZX83sTlTbfs+Tab3pBlsRQu36iNznleeKPj6uVvql+3uvcjMEBqqXvj8TKxMi9tCfHA1vt9RijOap8ROHtnIe4iMovPzkOCMiHJPcwbnyi+6jHbrPI18WGghceZQT23qKHDUYQo2NiehLQG9MQZA1Ncx2w4evBTBX8lkBS4aLoCUoTZTlNFSDOohUHJCbeig9eV77JbLo0a4+PNH9bgM/icSnIG5TidBGyJpEkVtD7+/KphwM89izJam3OT",
		"expires": "04/5tRBT5n/VJ0XQASgs/w==",
		"type": "XPrHEX2xHy+5IuXHPHigMw=="
	}
}
 * </pre>
 */

public class AuthenticationProperties {
	/** The log4j2 logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The properties file. */
	protected final File propertiesFile;

	private final Encryption encryption;

	protected final JsonObject jsap;

	private final String registrationURL;
	private final String tokenRequestURL;
	
	public AuthenticationProperties(String jsapFileName, byte[] secret) throws SEPAPropertiesException {
		FileReader in;

		propertiesFile = new File(jsapFileName);

		try {
			in = new FileReader(propertiesFile);
			jsap = new JsonParser().parse(in).getAsJsonObject();
			
			if (secret != null)
				encryption = new Encryption(secret);
			else
				encryption = new Encryption();
			
			jsap.get("oauth").getAsJsonObject().get("enable").getAsBoolean();
			
			registrationURL = jsap.get("oauth").getAsJsonObject().get("register").getAsString();
			tokenRequestURL = jsap.get("oauth").getAsJsonObject().get("tokenRequest").getAsString();
			
		} catch (Exception e) {
			throw new SEPAPropertiesException(e);
		}
	}

	public AuthenticationProperties(String jsapFileName) throws SEPAPropertiesException, SEPASecurityException {
		this(jsapFileName, null);
	}

	public boolean isEnabled() {
		try {
			return jsap.get("oauth").getAsJsonObject().get("enable").getAsBoolean();
		}
		catch(Exception e) {
			return false;
		}	
	}

	public String getRegisterUrl() {
		return registrationURL;
	}

	public String getTokenRequestUrl()  {
		return tokenRequestURL;
	}

	private String getSecurityEncryptedValue(String value) {
		try {
			return jsap.get("oauth").getAsJsonObject().get(value).getAsString();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Checks if is token expired.
	 *
	 * @return true, if is token expired
	 * @throws SEPASecurityException
	 * @throws NumberFormatException
	 */
	public synchronized boolean isTokenExpired() {
		return getExpiringTime() == 0;
	}

	/**
	 * Gets the expiring seconds.
	 *
	 * @return the expiring seconds
	 * @throws SEPASecurityException
	 * @throws NumberFormatException
	 */
	public synchronized long getExpiringTime() {
		try {
			long expires = Long.decode(encryption.decrypt(getSecurityEncryptedValue("expires")));
			long now = new Date().getTime();
			
			logger.debug("@getExpiringTime Diff:"+(expires-now)+" Now: "+now+" Expires: "+expires);
			
			if (expires-now < 0) return 0;
			return expires-now;
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * Gets the access token.
	 *
	 * @return the access token
	 * @throws SEPASecurityException
	 */
	public synchronized String getBearerAuthorizationHeader() {
		try {
			return "Bearer " + encryption.decrypt(getSecurityEncryptedValue("jwt"));
		} catch (Exception e) {
		}
		
		return null;
	}
	
	public synchronized String getToken() {
		try {
			return encryption.decrypt(getSecurityEncryptedValue("jwt"));
		} catch (SEPASecurityException e) {
			return null;
		}
	}

	/**
	 * Gets the token type.
	 *
	 * @return the token type
	 * @throws SEPASecurityException
	 */
	public synchronized String getTokenType() {
		try {
			return encryption.decrypt(getSecurityEncryptedValue("type"));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Gets the basic authorization.
	 *
	 * @return the basic authorization
	 * @throws SEPASecurityException
	 */
	public synchronized String getBasicAuthorizationHeader() {
		try {
			return "Basic " + new String(Base64.getEncoder().encode(
					(encryption.decrypt(getSecurityEncryptedValue("client_id"))
							+ ":"
							+ encryption.decrypt(getSecurityEncryptedValue("client_secret")))
											.getBytes("UTF-8")),
					"UTF-8");
		} catch (Exception e) {
			return null;	
		}
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
	public synchronized void setCredentials(String id, String secret) throws SEPAPropertiesException, SEPASecurityException {
		logger.debug("@setCredentials Id: " + id + " Secret:" + secret);

		// Save on file the encrypted version
		if (!jsap.has("oauth")) {
			JsonObject credentials = new JsonObject();
			credentials.add("client_id", new JsonPrimitive(encryption.encrypt(id)));
			credentials.add("client_secret", new JsonPrimitive(encryption.encrypt(secret)));
			jsap.add("oauth", credentials);
		} else {
			jsap.get("oauth").getAsJsonObject().add("client_id", new JsonPrimitive(encryption.encrypt(id)));
			jsap.get("oauth").getAsJsonObject().add("client_secret",
					new JsonPrimitive(encryption.encrypt(secret)));
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
	public void setJWT(JWTResponse jwt) throws SEPASecurityException, SEPAPropertiesException {
		logger.debug("@setJWT: "+jwt);
		
		long expires = new Date().getTime() + 1000 * jwt.getExpiresIn();
		
		// Save on file the encrypted version
		if (!jsap.has("oauth")) {
			JsonObject credentials = new JsonObject();
			credentials.add("jwt", new JsonPrimitive(encryption.encrypt(jwt.getAccessToken())));
			credentials.add("expires", new JsonPrimitive(encryption.encrypt(String.format("%d", expires))));
			credentials.add("type", new JsonPrimitive(encryption.encrypt(jwt.getTokenType())));
			jsap.add("oauth", credentials);
		} else {
			jsap.get("oauth").getAsJsonObject().add("jwt", new JsonPrimitive(encryption.encrypt(jwt.getAccessToken())));
			jsap.get("oauth").getAsJsonObject().add("expires",
					new JsonPrimitive(encryption.encrypt(String.format("%d", expires))));
			jsap.get("oauth").getAsJsonObject().add("type", new JsonPrimitive(encryption.encrypt(jwt.getTokenType())));
		}

		storeProperties(propertiesFile.getAbsolutePath());
		
	}

	/**
	 * Store properties.
	 *
	 * @param propertiesFile
	 *            the properties file
	 * @throws SEPAPropertiesException
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	protected void storeProperties(String propertiesFile) throws SEPAPropertiesException {
		FileWriter out = null;

		try {
			out = new FileWriter(propertiesFile);
			out.write(jsap.toString());
			out.close();
		} catch (IOException e) {
			throw new SEPAPropertiesException(e);
		}
	}
}

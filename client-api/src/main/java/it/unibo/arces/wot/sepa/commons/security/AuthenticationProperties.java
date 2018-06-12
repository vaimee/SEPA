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

/**
 * The Class SPARQL11Properties includes all the properties needed to connect to
 * a SPARQL 1.1 Protocol Service: the URLs used by queries and updates (scheme,
 * host, port and path), the HTTP method used by the primitives (GET, POST or
 * URL_ENCODED_POST) and the format of the results (JSON, XML, HTML, CSV). The
 * update result format is implementation specific. While for the query the
 * "formats" is the required return format, for the update it specifies the
 * format implemented by the SPARQL 1.1 Protocol service.
 * 
 * 
 * <pre>
	"authentication": {
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
	protected File propertiesFile;

	private Encryption encryption;

	protected JsonObject jsap;

	public AuthenticationProperties(String jsapFileName) throws SEPAPropertiesException {
		this(new File(jsapFileName));
	}

	public AuthenticationProperties(File jsapFile) throws SEPAPropertiesException {
		FileReader in;
		try {
			in = new FileReader(jsapFile);
			jsap = new JsonParser().parse(in).getAsJsonObject();
			propertiesFile = jsapFile;
			encryption = new Encryption();
		} catch (Exception e) {
			throw new SEPAPropertiesException(e);
		}
	}

	public AuthenticationProperties(String jsapFileName, byte[] secret) throws SEPAPropertiesException {
		this(new File(jsapFileName), secret);
	}

	public AuthenticationProperties(File jsapFile, byte[] secret) throws SEPAPropertiesException {
		FileReader in;
		try {
			in = new FileReader(jsapFile);
			jsap = new JsonParser().parse(in).getAsJsonObject();
			propertiesFile = jsapFile;
			encryption = new Encryption(secret);
		} catch (Exception e) {
			throw new SEPAPropertiesException(e);
		}
	}

	public String getRegisterUrl() throws SEPASecurityException {
		try {
			return jsap.get("authentication").getAsJsonObject().get("register").getAsString();
		} catch (Exception e) {
			throw new SEPASecurityException(e);
		}
	}

	public String getTokenRequestUrl() throws SEPASecurityException {
		try {
			return jsap.get("authentication").getAsJsonObject().get("tokenRequest").getAsString();
		} catch (Exception e) {
			throw new SEPASecurityException(e);
		}
	}

	private String getSecurityEncryptedValue(String value) throws SEPASecurityException {
		try {
			return jsap.get("authentication").getAsJsonObject().get(value).getAsString();
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
	public boolean isTokenExpired() {
		try {
			return (new Date().getTime() >= Long.decode(encryption.decrypt(getSecurityEncryptedValue("expires"))));
		} catch (Exception e) {
			return true;
		}
	}

	/**
	 * Gets the expiring seconds.
	 *
	 * @return the expiring seconds
	 * @throws SEPASecurityException
	 * @throws NumberFormatException
	 */
	public long getExpiringSeconds() {
		try {
			return ((Long.decode(encryption.decrypt(getSecurityEncryptedValue("expires"))) - new Date().getTime())
					/ 1000);
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
	public String getBearerAuthorizationHeader() throws SEPASecurityException {
		try {
			return "Bearer " + encryption.decrypt(getSecurityEncryptedValue("jwt"));
		} catch (Exception e) {
			throw new SEPASecurityException(e);
		}
	}

	/**
	 * Gets the token type.
	 *
	 * @return the token type
	 * @throws SEPASecurityException
	 */
	public String getTokenType() throws SEPASecurityException {
		try {
			return encryption.decrypt(getSecurityEncryptedValue("type"));
		} catch (Exception e) {
			throw new SEPASecurityException(e);
		}
	}

	/**
	 * Gets the basic authorization.
	 *
	 * @return the basic authorization
	 * @throws SEPASecurityException
	 */
	public String getBasicAuthorizationHeader() throws SEPASecurityException {
		try {
			return "Basic " + new String(Base64.getEncoder().encode(
					(encryption.decrypt(jsap.get("authentication").getAsJsonObject().get("client_id").getAsString())
							+ ":"
							+ encryption.decrypt(
									jsap.get("authentication").getAsJsonObject().get("client_secret").getAsString()))
											.getBytes("UTF-8")),
					"UTF-8");
		} catch (Exception e) {
			throw new SEPASecurityException(e);
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
	public void setCredentials(String id, String secret) throws SEPASecurityException, SEPAPropertiesException {
		logger.debug("Set credentials Id: " + id + " Secret:" + secret);

		// Save on file the encrypted version
		if (!jsap.has("authentication")) {
			JsonObject credentials = new JsonObject();
			credentials.add("client_id", new JsonPrimitive(encryption.encrypt(id)));
			credentials.add("client_secret", new JsonPrimitive(encryption.encrypt(secret)));
			jsap.add("authentication", credentials);
		} else {
			jsap.get("authentication").getAsJsonObject().add("client_id", new JsonPrimitive(encryption.encrypt(id)));
			jsap.get("authentication").getAsJsonObject().add("client_secret",
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
	public void setJWT(String jwt, Date expires, String type) throws SEPASecurityException, SEPAPropertiesException {

		// Save on file the encrypted version
		if (!jsap.has("authentication")) {
			JsonObject credentials = new JsonObject();
			credentials.add("jwt", new JsonPrimitive(encryption.encrypt(jwt)));
			credentials.add("expires", new JsonPrimitive(encryption.encrypt(String.format("%d", expires.getTime()))));
			credentials.add("type", new JsonPrimitive(encryption.encrypt(type)));
			jsap.add("authentication", credentials);
		} else {
			jsap.get("authentication").getAsJsonObject().add("jwt", new JsonPrimitive(encryption.encrypt(jwt)));
			jsap.get("authentication").getAsJsonObject().add("expires",
					new JsonPrimitive(encryption.encrypt(String.format("%d", expires.getTime()))));
			jsap.get("authentication").getAsJsonObject().add("type", new JsonPrimitive(encryption.encrypt(type)));
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
		FileWriter out;
		try {
			out = new FileWriter(propertiesFile);
			out.write(jsap.toString());
			out.close();
		} catch (Exception e) {
			throw new SEPAPropertiesException(e);
		}

	}
}

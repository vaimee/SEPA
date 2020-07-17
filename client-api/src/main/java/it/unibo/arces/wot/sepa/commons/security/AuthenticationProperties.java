/* Authentication properties which includes the token for OAuth 2.0 authorization
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

package it.unibo.arces.wot.sepa.commons.security;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
 * The set of properties used for client authentication
 * 
 * 
 * <pre>
	"oauth": {
		"enable" : true,
		"ssl" : "TLSv1.2",
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

	private final Encryption encryption;

	private final boolean enabled;

	private final String registrationURL;
	private final String tokenRequestURL;

	private String clientId = null;
	private String clientSecret = null;

	private String jwt = null;
	private long expires = -1;
	private String type = null;

	private String ssl = "TLS";
	private boolean trustAll = false;

	private JsonObject jsap;
	private File propertiesFile;
	
	public AuthenticationProperties(String jsapFileName, byte[] secret)
			throws SEPAPropertiesException, SEPASecurityException {
		propertiesFile = new File(jsapFileName);

		FileReader in;
		try {
			in = new FileReader(propertiesFile);
		} catch (FileNotFoundException e) {
			throw new SEPAPropertiesException("FileNotFoundException. " + e.getMessage());
		}

		jsap = new JsonParser().parse(in).getAsJsonObject();

		try {
			in.close();
		} catch (IOException e) {
			throw new SEPAPropertiesException("IOException. " + e.getMessage());
		}

		if (secret != null)
			encryption = new Encryption(secret);
		else
			encryption = new Encryption();

		if (jsap.has("oauth")) {
			JsonObject oauthJsonObject = jsap.getAsJsonObject("oauth");

			if (oauthJsonObject.has("enable"))
				enabled = oauthJsonObject.get("enable").getAsBoolean();
			else {
				enabled = false;
			}

			if (oauthJsonObject.has("trustall"))
				trustAll = oauthJsonObject.get("trustall").getAsBoolean();
			else {
				trustAll = false;
			}

			if (oauthJsonObject.has("ssl"))
				ssl = oauthJsonObject.get("ssl").getAsString();

			if (enabled) {
				registrationURL = oauthJsonObject.get("register").getAsString();
				tokenRequestURL = oauthJsonObject.get("tokenRequest").getAsString();
				// ssl = oauthJsonObject.get("ssl").getAsString();

				if (oauthJsonObject.has("client_id"))
					clientId = encryption.decrypt(oauthJsonObject.get("client_id").getAsString());
				if (oauthJsonObject.has("client_secret"))
					clientSecret = encryption.decrypt(oauthJsonObject.get("client_secret").getAsString());
				if (oauthJsonObject.has("jwt"))
					jwt = encryption.decrypt(oauthJsonObject.get("jwt").getAsString());
				if (oauthJsonObject.has("expires"))
					expires = Long.decode(encryption.decrypt(oauthJsonObject.get("expires").getAsString()));
				if (oauthJsonObject.has("type"))
					type = encryption.decrypt(oauthJsonObject.get("type").getAsString());

			} else {
				registrationURL = null;
				tokenRequestURL = null;
			}
		} else {
			enabled = false;
			registrationURL = null;
			tokenRequestURL = null;
		}

	}

	public AuthenticationProperties(String jsap) throws SEPAPropertiesException, SEPASecurityException {
		this(jsap, null);
	}

	public AuthenticationProperties() {
		enabled = false;
		registrationURL = null;
		tokenRequestURL = null;
		encryption = new Encryption();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getRegisterUrl() {
		return registrationURL;
	}

	public String getTokenRequestUrl() {
		return tokenRequestURL;
	}

	/**
	 * Gets the access token.
	 *
	 * @return the access token
	 */
	public String getBearerAuthorizationHeader() {
		if (jwt != null)
			return "Bearer " + jwt;
		else
			return null;
	}

	public String getToken() {
		return jwt;
	}

	/**
	 * Gets the token type.
	 *
	 * @return the token type
	 */
	public String getTokenType() {
		return type;
	}

	/**
	 * Gets the basic authorization.
	 *
	 * @return the basic authorization
	 * @throws SEPASecurityException
	 */
	public String getBasicAuthorizationHeader() throws SEPASecurityException {
		if (clientId != null && clientSecret != null) {
			String plainString = clientId + ":" + clientSecret;
			try {
				return "Basic " + new String(Base64.getEncoder().encode(plainString.getBytes("UTF-8")), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new SEPASecurityException(e);
			}
		} else
			return null;
	}

	/**
	 * Sets the credentials.
	 *
	 * @param id     the username
	 * @param secret the password
	 */
	public void setCredentials(String id, String secret) throws SEPAPropertiesException, SEPASecurityException {
		// logger.debug("@setCredentials Id: " + id + " Secret:" + secret);

		clientId = id;
		clientSecret = secret;
	}

	public boolean isClientRegistered() {
		return clientId != null && clientSecret != null;
	}

	public boolean isTokenExpired() {
		if (expires < 0 || jwt == null)
			return true;
		return expires > new Date().getTime();
	}

	/**
	 * Sets the JWT.
	 *
	 * @param jwt the JSON Web Token
	 * @throws SEPAPropertiesException
	 * @throws SEPASecurityException
	 *
	 */
	public void setJWT(JWTResponse jwt) throws SEPASecurityException, SEPAPropertiesException {
		logger.debug("@setJWT: " + jwt);

		this.expires = new Date().getTime() + 1000 * jwt.getExpiresIn();
		this.jwt = jwt.getAccessToken();
		this.type = jwt.getTokenType();
	}

	/**
	 * Store properties.
	 *
	 * @param propertiesFile the properties file
	 * @throws SEPAPropertiesException
	 * @throws SEPASecurityException
	 * @throws IOException             Signals that an I/O exception has occurred.
	 */
	public synchronized void storeProperties() throws SEPAPropertiesException, SEPASecurityException {
		jsap.add("oauth", new JsonObject());
		jsap.getAsJsonObject("oauth").add("enable", new JsonPrimitive(enabled));

		if (ssl != null) {
			jsap.getAsJsonObject("oauth").add("ssl", new JsonPrimitive(ssl));
		}

		jsap.getAsJsonObject("oauth").add("trustall", new JsonPrimitive(trustAll));

		if (registrationURL != null)
			jsap.getAsJsonObject("oauth").add("register", new JsonPrimitive(registrationURL));
		if (tokenRequestURL != null)
			jsap.getAsJsonObject("oauth").add("tokenRequest", new JsonPrimitive(tokenRequestURL));
		if (clientId != null)
			jsap.getAsJsonObject("oauth").add("client_id", new JsonPrimitive(encryption.encrypt(clientId)));
		if (clientSecret != null)
			jsap.getAsJsonObject("oauth").add("client_secret", new JsonPrimitive(encryption.encrypt(clientSecret)));

		if (jwt != null)
			jsap.getAsJsonObject("oauth").add("jwt", new JsonPrimitive(encryption.encrypt(jwt)));
		if (expires != -1)
			jsap.getAsJsonObject("oauth").add("expires",
					new JsonPrimitive(encryption.encrypt(String.format("%d", expires))));
		if (type != null)
			jsap.getAsJsonObject("oauth").add("type", new JsonPrimitive(encryption.encrypt(type)));

		FileWriter out;
		try {
			out = new FileWriter(propertiesFile);
			out.write(jsap.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new SEPAPropertiesException("IOException: " + propertiesFile.getPath() + " " + e.getMessage());
		}
	}

	public String getClientId() {
		return clientId;
	}

	public String getSSLProtocol() {
		return ssl;
	}

	public boolean trustAll() {
		return trustAll;
	}
}

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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.security.*;

import java.util.Date;
import java.util.NoSuchElementException;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Base64Encoder;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

//import sun.misc.*;

/**
 * The Class SPARQL11SEProperties.
 * 
 * {"parameters":{
	"host":"localhost",
	"port":8000,
	"scheme":"http",
	"path":"/sparql",
	"query":{"method":"POST","format":"JSON"},
	"update":{"method":"URL_ENCODED_POST","format":"HTML"},
	"subscribe":{"port":9000,"scheme":"ws"},
	"securesubscribe":{"port":9443,"scheme":"wss","path":"/secure/sparql"},
	"secureupdate":{"port":8443,"scheme":"https"},
	"securequery":{"port":8443,"scheme":"https"},
	"authorizationserver":{
		"register":"/oauth/register","requesttoken":"/oauth/token",
		"port":8443,"scheme":"https"},
	"security":{
		"client_id":"2onesV7vDw9TvXTBt6JlrD0MV6f8eU+Xd8hqTpyok0PcnuFi19HwGTOwdJ56uZDR",
		"client_secret":"qp6AulTNzU3jMdUY45+eNgQ3+iilVaBuAADR64w9vqmVYtzk814g2x5ZLAgngT7s",
		"jwt":"xabtQWoH8RJJk1FyKJ78J8h8i2PcWmAugfJ4J6nMd+3Adk0TRGMLTGccxJUiJyyc2yRaVage8/Tz\ndZJiz2jdRP8bhkuNzFhGx6N1/1mgmvfKihLheMmcU0pLj5uKOYWFb+TB98n1IpNO4G69lia2YoR1\n5LScBzibBPpmKWF+XAr5TeDDHDZQK4N3VBS/e3tFL/yOhkfC9Mw45s3mz83oydQazps2cFzookIh\nydKJWfupSsIpj+KmOAjcfC9/tTs3K5uCw8It/0FKvsuW0MAboo4X49sDS+AHTOnVUf67wnnPqJ2M\n1thThv3dIr/WNn+8xJovJWkwcpGP4T7nH7MOCfZzVnKTHr4hN3q14VUWHYne1Mbui7F238uxPBhm\nGoMoSnd7dpaVGHZK9Kfa97HuiKN8s2SfRBcyLOnlBczjgQAaKYdJRUXndWQhPIu1W0oZUxH//6Kx\nA+cquekGC+mzeC8QscLmuwOkBaYIX2Va9600gErGqtHisgNwUUH/g73zjO4pD+xLL/cXuudp89Vq\nu+FyVDOqH5GoCX3G4PMPXLoVuBm4Zt2yQdPvpshH3mrGJsPxS8f1PeVnR6Iy5Wbc8a5jiGYHljbs\n0498sKRA0rko/LHSCZwQwuKwuMd110ZvvmQhBUX/23appJ1Wj9hrS1/G5mPXvFQGuZGf+dgynvPT\njeF4RZQVcsfY7jxTwxVC0VRq7dRIncRgmNOHmfKBA18h9fd6gix5RYEX69NvPKEolFyy2wJJxaci\nwW1ub235Gzd/gn+hnNox1g2rIKPu5XY6ttF0L5HwQmk8aYhusOY=",
		"expires":"gVpKtUqSbe+km85RCBcsBQ==",
		"type":"XPrHEX2xHy+5IuXHPHigMw=="}}
 */
public class SPARQL11SEProperties extends SPARQL11Properties {
	private long expires = 0;
	private String jwt = null;
	private String tokenType = null;
	private String authorization = null;
	private String id = null;
	private String secret = null;
	
	//Base64 encoding-decoding
	static Base64Encoder base64 = new Base64Encoder();
	static ByteArrayOutputStream out64 = new ByteArrayOutputStream();
	
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
	* */
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
		 SECUREQUERY};
		
	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param propertiesFile the properties file
	 * @param secret the secret
	 * @throws IOException 
	 * @throws NoSuchElementException 
	 * @throws FileNotFoundException 
	 */
	public SPARQL11SEProperties(String propertiesFile,byte[] secret) throws FileNotFoundException, NoSuchElementException, IOException {
		super(propertiesFile);
		SEPAEncryption.init(secret);
	}
	
	/**
	 * Instantiates a new SPARQL 11 SE properties.
	 *
	 * @param propertiesFile the properties file
	 * @throws IOException 
	 * @throws NoSuchElementException 
	 * @throws FileNotFoundException 
	 */
	public SPARQL11SEProperties(String propertiesFile) throws FileNotFoundException, NoSuchElementException, IOException {
		this(propertiesFile,null);
	}
	
	public String toString() {
		return parameters.toString();
	}
	
	@Override
	protected void defaults() {
		super.defaults();
		
		JsonObject subscribe = new JsonObject();
		subscribe.add("port", new JsonPrimitive(9000));
		subscribe.add("scheme", new JsonPrimitive("ws"));
		subscribe.add("path", new JsonPrimitive("/sparql"));
		parameters.add("subscribe", subscribe);
		
		JsonObject securesubscribe = new JsonObject();
		securesubscribe.add("port", new JsonPrimitive(9443));
		securesubscribe.add("scheme", new JsonPrimitive("wss"));
		securesubscribe.add("path", new JsonPrimitive("/secure/sparql"));
		parameters.add("securesubscribe", securesubscribe);
		
		JsonObject secureUpdate = new JsonObject();
		secureUpdate.add("port", new JsonPrimitive(8443));
		secureUpdate.add("scheme", new JsonPrimitive("https"));
		parameters.add("secureupdate", secureUpdate);
		
		JsonObject secureQuery = new JsonObject();
		secureQuery.add("port", new JsonPrimitive(8443));
		secureQuery.add("scheme", new JsonPrimitive("https"));
		parameters.add("securequery", secureQuery);
		
		JsonObject register = new JsonObject();
		register.add("register", new JsonPrimitive("/oauth/register"));
		register.add("requesttoken", new JsonPrimitive("/oauth/token"));
		register.add("port", new JsonPrimitive(8443));
		register.add("scheme", new JsonPrimitive("https"));
		parameters.add("authorizationserver", register);
	}
	
	protected void loadProperties() throws FileNotFoundException, NoSuchElementException, IOException{
		super.loadProperties();
		
		if (doc.get("security") != null) {
			if (doc.get("security").getAsJsonObject().get("expires") != null) 
				expires = Long.decode(SEPAEncryption.decrypt(doc.get("security").getAsJsonObject().get("expires").getAsString()));
			else
				expires = 0;
			
			if (doc.get("security").getAsJsonObject().get("jwt") != null) 
				jwt = SEPAEncryption.decrypt(doc.get("security").getAsJsonObject().get("jwt").getAsString());
			else
				jwt = null;
			
			if (doc.get("security").getAsJsonObject().get("type") != null) 
				tokenType =  SEPAEncryption.decrypt(doc.get("security").getAsJsonObject().get("type").getAsString());
			else
				tokenType = null;
			
			if (doc.get("security").getAsJsonObject().get("client_id") != null && doc.get("security").getAsJsonObject().get("client_secret") != null ) {
				id = SEPAEncryption.decrypt(doc.get("security").getAsJsonObject().get("client_id").getAsString());
				secret = SEPAEncryption.decrypt(doc.get("security").getAsJsonObject().get("client_secret").getAsString());
				try {
					//authorization = new BASE64Encoder().encode((id + ":" + secret).getBytes("UTF-8"));
					Base64Encoder base64 = new Base64Encoder();
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					byte[] toEncode = (id + ":" + secret).getBytes("UTF-8");
					base64.encode(toEncode,0,toEncode.length,out);
					authorization= out.toString("UTF-8");
					
					//TODO need a "\n", why?
					authorization = authorization.replace("\n", "");
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
				}	
			}
			else
				authorization = null;
		}
	}
	
	public int getSubscribePort() {
		return getParameter("subscribe","port",9000);
	}
	
	public String getSubscribePath() {
		return getParameter("subscribe","path","/sparql");
	}
	
	public String getSubscribeScheme() {
		return getParameter("subscribe","scheme","ws");
	}
	
	public int getSubscribeSecurePort(){
		return getParameter("securesubscribe","port",9443);
	}
	
	public int getUpdateSecurePort(){
		return getParameter("secureupdate","port",8443);
	}
	
	public int getQuerySecurePort() {
		return getParameter("securequery","port",8443);
	}

	public String getRegistrationScheme() {
		return getParameter("authorizationserver","scheme","https");
	}

	public String getRequestTokenScheme() {
		return getParameter("authorizationserver","scheme","https");
	}
	
	public String getRegistrationPath() {
		return getParameter("authorizationserver","register","/oauth/register");
	}
	
	public String getRequestTokenPath() {
		return getParameter("authorizationserver","requesttoken","/oauth/token");
	}
	 
	public int getRegistrationPort() {
		return getParameter("authorizationserver","port",8443);
	}

	public int getRequestTokenPort() {
		return getParameter("authorizationserver","port",8443);
	}
	
	
	
	/**
	 * Checks if is token expired.
	 *
	 * @return true, if is token expired
	 */
	public boolean isTokenExpired() {
		return (new Date().getTime() >= expires);
	}
	
	/**
	 * Gets the expiring seconds.
	 *
	 * @return the expiring seconds
	 */
	public long getExpiringSeconds() {
		long seconds = ((expires - new Date().getTime())/1000);
		if (seconds < 0) seconds = 0;
		return seconds;
	}
	
	/**
	 * Gets the access token.
	 *
	 * @return the access token
	 */
	public String getAccessToken() {
		return jwt;	
	}

	/**
	 * Gets the token type.
	 *
	 * @return the token type
	 */
	public String getTokenType() {
		return tokenType;
	}
	
	/**
	 * Gets the basic authorization.
	 *
	 * @return the basic authorization
	 */
	public String getBasicAuthorization() {	
		return authorization;
	}
	
	/**
	 * Sets the credentials.
	 *
	 * @param id the username
	 * @param secret the password
	 * @throws IOException 
	 */
	public void setCredentials(String id,String secret) throws IOException {	
		logger.debug("Set credentials Id: "+id+" Secret:"+secret);
		
		this.id = id;
		this.secret = secret;
		
		try {			
			byte[] toEncode = (id + ":" + secret).getBytes("UTF-8");
			
			base64.encode(toEncode,0,toEncode.length,out64);
			authorization= out64.toString("UTF-8");
			
			//TODO need a "\n", why?
			authorization = authorization.replace("\n", "");
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
		}
		
		//Save on file the encrypted version	
		if (parameters.get("security")==null) {
			JsonObject credentials = new JsonObject();
			credentials.add("client_id",new JsonPrimitive(SEPAEncryption.encrypt(id)));
			credentials.add("client_secret",new JsonPrimitive(SEPAEncryption.encrypt(secret)));		
			parameters.add("security",credentials);
		}
		else {
			parameters.get("security").getAsJsonObject().add("client_id",new JsonPrimitive(SEPAEncryption.encrypt(id)));
			parameters.get("security").getAsJsonObject().add("client_secret",new JsonPrimitive(SEPAEncryption.encrypt(secret)));	
		}
		
		storeProperties(propertiesFile);
	}
	
	/**
	 * Sets the JWT.
	 *
	 * @param jwt the JSON Web Token
	 * @param expires the date when the token will expire
	 * @param type the token type (e.g., bearer)
	 * @throws IOException 
	 */
	public void setJWT(String jwt, Date expires,String type) throws IOException {	
		
		this.jwt = jwt;
		this.expires = expires.getTime();
		this.tokenType = type;
		
		//Save on file the encrypted version
		if (parameters.get("security")==null) {
			JsonObject credentials = new JsonObject();
			credentials.add("jwt",new JsonPrimitive(SEPAEncryption.encrypt(jwt)));
			credentials.add("expires",new JsonPrimitive(SEPAEncryption.encrypt(String.format("%d", expires.getTime()))));
			credentials.add("type",new JsonPrimitive(SEPAEncryption.encrypt(type)));	
			parameters.add("security",credentials);
		}
		else {
			parameters.get("security").getAsJsonObject().add("jwt",new JsonPrimitive(SEPAEncryption.encrypt(jwt)));
			parameters.get("security").getAsJsonObject().add("expires",new JsonPrimitive(SEPAEncryption.encrypt(String.format("%d", expires.getTime()))));
			parameters.get("security").getAsJsonObject().add("type",new JsonPrimitive(SEPAEncryption.encrypt(type)));	
		}
				
		storeProperties(propertiesFile);
	}
	
	/**
	 * The Class SEPAEncryption.
	 */
	private static class SEPAEncryption {
		
		/** The Constant ALGO. */
		//AES 128 bits (16 bytes)
		private static final String ALGO = "AES";
	    
    	/** The key value. */
    	private static byte[] keyValue = new byte[] { '0', '1', 'R', 'a', 'v', 'a', 'm','i', '!', 'I', 'e','2', '3', '7', 'A', 'N' };
	    
    	/** The key. */
    	private static Key key = new SecretKeySpec(keyValue, ALGO);
		
	    /**
    	 * Inits the.
    	 *
    	 * @param secret the secret
    	 */
    	private static void init(byte[] secret) {
	    	if (secret != null && secret.length == 16) keyValue = secret;
	    	key = new SecretKeySpec(keyValue, ALGO);
	    }
	    
	    /**
    	 * Encrypt.
    	 *
    	 * @param Data the data
    	 * @return the string
	     * @throws IOException 
    	 */
    	public static String encrypt(String Data) throws IOException {
			try {
				Cipher c = Cipher.getInstance(ALGO);
				c.init(Cipher.ENCRYPT_MODE, key);
				byte[] encVal = c.doFinal(Data.getBytes());
				
				base64.encode(encVal,0,encVal.length,out64);
				
				return out64.toString("UTF-8");
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
				logger.fatal(e.getMessage());
				return null;
			}
		}
		
		/**
		 * Decrypt.
		 *
		 * @param encryptedData the encrypted data
		 * @return the string
		 */
		public static String decrypt(String encryptedData) {
			try {
				Cipher c = Cipher.getInstance(ALGO);
				c.init(Cipher.DECRYPT_MODE, key);
				
				base64.decode(encryptedData, out64);			
				byte[] decordedValue = out64.toByteArray();

				byte[] decValue = c.doFinal(decordedValue);
		        return new String(decValue);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException | IllegalBlockSizeException | BadPaddingException e) {
				logger.fatal(e.getMessage());
				return null;
			}
	    }
	}

	public String getSecureQueryScheme() {
		return getParameter("securequery","scheme","https");
	}

	public String getSecureUpdateScheme() {
		return getParameter("secureupdate","scheme","https");
	}

	public Object getSecureSubscribeScheme() {
		return getParameter("securesubscribe","scheme","wss");
	}

	public String getSecureUpdatePath() {
		return getParameter("secureupdate","path","/sparql");
	}

	public String getSecureQueryPath() {
		return getParameter("securequery","path","/sparql");
	}

	public int getSecureSubscribePort() {
		return getParameter("securesubscribe","port",9443);
	}

	public String getSecureSubscribePath() {
		return getParameter("securesubscribe","path","/secure/sparql");
	}

	
}

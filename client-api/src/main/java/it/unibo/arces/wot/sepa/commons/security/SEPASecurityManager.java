/* This class implements the TLS 1.0 security mechanism 
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

package it.unibo.arces.wot.sepa.commons.security;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.RegistrationRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.JWTResponse;
import it.unibo.arces.wot.sepa.commons.response.RegistrationResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.timing.Timings;

/**
 * <pre>
 * The Class SecurityManager.
 * 
 * * ### Key and Certificate Storage ###
 * 
 * The Java platform provides for long-term persistent storage of cryptographic
 * keys and certificates via key and certificate stores. Specifically, the
 * java.security.KeyStore class represents a key store, a secure repository of
 * cryptographic keys and/or trusted certificates (to be used, for example,
 * during certification path validation), and the java.security.cert.CertStore
 * class represents a certificate store, a public and potentially vast
 * repository of unrelated and typically untrusted certificates. A CertStore may
 * also store CRLs.
 * 
 * KeyStore and CertStore implementations are distinguished by types. The Java
 * platform includes the standard PKCS11 and PKCS12 key store types (whose
 * implementations are compliant with the corresponding PKCS specifications from
 * RSA Security). It also contains a proprietary file-based key store type
 * called JKS (which stands for "Java Key Store"), and a type called DKS
 * ("Domain Key Store") which is a collection of keystores that are presented as
 * a single logical keystore.
 * 
 * ### PKI Tools ###
 * 
 * There are two built-in tools for working with keys, certificates, and key
 * stores:
 * 
 * keytool is used to create and manage key stores. It can
 * 
 * - Create public/private key pairs
 * 
 * - Display, import, and export X.509 v1, v2, and v3 certificates stored as
 * files
 * 
 * - Create self-signed certificates
 * 
 * ### Secure Communication ###
 * 
 * The data that travels across a network can be accessed by someone who is not
 * the intended recipient. When the data includes private information, such as
 * passwords and credit card numbers, steps must be taken to make the data
 * unintelligible to unauthorized parties. It is also important to ensure that
 * you are sending the data to the appropriate party, and that the data has not
 * been modified, either intentionally or unintentionally, during transport.
 * 
 * Cryptography forms the basis required for secure communication, and that is
 * described in Section 4. The Java platform also provides API support and
 * provider implementations for a number of standard secure communication
 * protocols.
 * 
 * ### SSL/TLS ###
 * 
 * The Java platform provides APIs and an implementation of the SSL and TLS
 * protocols that includes functionality for data encryption, message integrity,
 * server authentication, and optional client authentication. Applications can
 * use SSL/TLS to provide for the secure passage of data between two peers over
 * any application protocol, such as HTTP on top of TCP/IP.
 * 
 * The javax.net.ssl.SSLSocket class represents a network socket that
 * encapsulates SSL/TLS support on top of a normal stream socket
 * (java.net.Socket). Some applications might want to use alternate data
 * transport abstractions (e.g., New-I/O); the javax.net.ssl.SSLEngine class is
 * available to produce and consume SSL/TLS packets.
 * 
 * The Java platform also includes APIs that support the notion of pluggable
 * (provider-based) key managers and trust managers. A key manager is
 * encapsulated by the javax.net.ssl.KeyManager class, and manages the keys used
 * to perform authentication. A trust manager is encapsulated by the
 * TrustManager class (in the same package), and makes decisions about who to
 * trust based on certificates in the key store it manages.
 * 
 * The Java platform includes a built-in provider that implements the SSL/TLS
 * protocols:
 * 
 * SSLv3 TLSv1 TLSv1.1 TLSv1.2
 * 
 * </pre>
 * 
 * @see HostnameVerifier
 */
public class SEPASecurityManager implements HostnameVerifier {

	/**
	 * The JAVA key store.
	 * 
	 * @see KeyStore
	 */
	private final KeyStore keystore;

	/**
	 * The SSLConnectionSocketFactory context.
	 * 
	 * @see SSLConnectionSocketFactory
	 */
	private final SSLConnectionSocketFactory sslsf;

	/** The log4j2 logger. */
	private static final Logger logger = LogManager.getLogger();

	private final AuthenticationProperties oauthProperties;

	private final KeyManagerFactory kmfactory;
	private final TrustManagerFactory tmf;
	
	private static final String[] protocolStrings = {"TLSv1","TLSv1.1","TLSv1.2"};

	/**
	 * Instantiates a new Security Manager.
	 *
	 * @param protocol    the protocol
	 * @param jksName     the jks name
	 * @param jksPassword the jks password
	 * @param keyPassword the key password
	 * @throws SEPASecurityException
	 */
	public SEPASecurityManager(String jksName, String jksPassword, String keyPassword,
			AuthenticationProperties oauthProp) throws SEPASecurityException {
		// Arguments check
		if (jksName == null || jksPassword == null || keyPassword == null)
			throw new SEPASecurityException("JKS name or passwords are null");

		// Initialize SSL context
		File f = new File(jksName);
		if (!f.exists() || f.isDirectory())
			throw new SEPASecurityException(jksName + " not found");

		try {
			keystore = KeyStore.getInstance("JKS");
			keystore.load(new FileInputStream(jksName), jksPassword.toCharArray());

			kmfactory = KeyManagerFactory.getInstance("SunX509");
			kmfactory.init(keystore, keyPassword.toCharArray());

			tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(keystore);

			// Trust own CA and all self-signed certificates and allow TLSv1 protocol only
			sslsf = new SSLConnectionSocketFactory(
					SSLContexts.custom()
							.loadTrustMaterial(new File(jksName), jksPassword.toCharArray(),
									new TrustSelfSignedStrategy())
							.build(),
							protocolStrings, null, this);
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
				| UnrecoverableKeyException | KeyManagementException e) {
			throw new SEPASecurityException(e.getMessage());
		}

		oauthProperties = oauthProp;
	}

	static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		}
	} };

	public static SSLContext getSSLContextTrustAllCa(String protocol) throws SEPASecurityException {
		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance(protocol);
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new SEPASecurityException(e);
		}

		return sc;
	}

	public static SSLContext getSSLContext(String protocol, String caCertFile) throws SEPASecurityException {
		try {
			// Load certificates into the keystore
			KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
			caKs.load(null, null);

			FileInputStream fis = new FileInputStream(caCertFile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			CertificateFactory cf;
			cf = CertificateFactory.getInstance("X.509");
			while (bis.available() > 0) {
				X509Certificate caCert = (X509Certificate) cf.generateCertificate(bis);
				caKs.setCertificateEntry(caCert.getIssuerX500Principal().getName(), caCert);
			}
			
			// Trust manager
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
			tmf.init(caKs);
			
			// Create SSL context
			SSLContext sslContext = SSLContext.getInstance(protocol);
			sslContext.init(null, tmf.getTrustManagers(), null);

			return sslContext;
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
				| KeyManagementException e) {
			e.printStackTrace();
			throw new SEPASecurityException(e);
		}
	}

	public SSLContext getSSLContext(String protocol) throws SEPASecurityException {
		SSLContext sslContext;

		try {
			sslContext = SSLContext.getInstance(protocol);
		} catch (NoSuchAlgorithmException e) {
			throw new SEPASecurityException(e);
		}
		try {
			sslContext.init(kmfactory.getKeyManagers(), tmf.getTrustManagers(), null);
		} catch (KeyManagementException e) {
			throw new SEPASecurityException(e);
		}

		return sslContext;
	}

	public KeyStore getKeyStore() {
		return keystore;
	}

	public CloseableHttpClient getSSLHttpClient() {
		return HttpClients.custom().setSSLSocketFactory(sslsf).build();
	}

	@Override
	public boolean verify(String hostname, SSLSession session) {
		// TODO IMPORTANT Verify X.509 certificate

		return true;
	}

	/**
	 * Register the identity and store the credentials into the Authentication
	 * properties
	 * 
	 * @param identity is a string that identifies the client (e.g., registration
	 *                 code, MAC address, EPC, ...)
	 * @return RegistrationResponse or ErrorResponse in case of an error
	 * 
	 * @see RegistrationResponse
	 * @see ErrorResponse
	 * @throws SEPAPropertiesException
	 * @throws SEPASecurityException
	 */
	public Response register(String identity) throws SEPASecurityException, SEPAPropertiesException {
		if (oauthProperties == null)
			throw new SEPAPropertiesException("Authorization properties are null");

		Response ret = register(oauthProperties.getRegisterUrl(), identity);

		if (ret.isRegistrationResponse()) {
			RegistrationResponse reg = (RegistrationResponse) ret;
			oauthProperties.setCredentials(reg.getClientId(), reg.getClientSecret());
		} else {
			logger.error(ret);
		}

		return ret;
	}

	/**
	 * Returns the Bearer authentication header if the token is not expired, otherwise requests and returns a fresh token
	 * 
	 * @throws SEPAPropertiesException
	 * @throws SEPASecurityException
	 * 
	 * @see AuthenticationProperties
	 */
	public String getAuthorizationHeader() throws SEPASecurityException, SEPAPropertiesException {
		if (oauthProperties == null) {
			logger.warn("OAuth properties are null");
			return null;
		}

		return oauthProperties.getBearerAuthorizationHeader();
	}
	
	public void storeOAuthProperties() throws SEPAPropertiesException, SEPASecurityException {
		oauthProperties.storeProperties();
	}
	
	public boolean isTokenExpired() {
		return oauthProperties.isTokenExpired();
	}
	
	public boolean isClientRegistered() {
		return oauthProperties.isClientRegistered();
	}
	
	public void setClientCredentials(String username,String password) throws SEPAPropertiesException, SEPASecurityException {
		oauthProperties.setCredentials(username, password);
	}

	public Response refreshToken() throws SEPAPropertiesException, SEPASecurityException {	
		if(!isClientRegistered()) {
			return new ErrorResponse(401,"invalid_client","Client is not registered");
		}
		
		Response ret = requestToken(oauthProperties.getTokenRequestUrl(),
				oauthProperties.getBasicAuthorizationHeader());
	
		if (ret.isJWTResponse()) {
			JWTResponse jwt = (JWTResponse) ret;
	
			logger.debug("New token: "+ jwt);
	
			oauthProperties.setJWT(jwt);
		} else {
			logger.error("FAILED to refresh token " + new Date() + " Response: " + ret);
		}
		
		return ret;
	}

//	/**
//	 * It is used to request a new token using the "Basic" credentials stored in the
//	 * AuthenticationProperties. When retrieved, the token is stored within the
//	 * AuthenticationProperties.
//	 * 
//	 * @return In case of success, it returns an JWTResponse. Otherwise an
//	 *         ErrorResponse is returned as specified in RFC6749
//	 * @throws SEPASecurityException
//	 * @throws SEPAPropertiesException
//	 * @see ErrorResponse
//	 * @see JWTResponse
//	 * @see AuthenticationProperties
//	 */
//	private void requestToken() throws SEPASecurityException, SEPAPropertiesException {
//		Response ret = requestToken(oauthProperties.getTokenRequestUrl(),
//				oauthProperties.getBasicAuthorizationHeader());
//
//		if (ret.isJWTResponse()) {
//			JWTResponse jwt = (JWTResponse) ret;
//
//			logger.debug(jwt);
//
//			oauthProperties.setJWT(jwt);
//		} else {
//			logger.error("requestToken@ " + new Date() + " Response: " + ret);
//		}
//	}

//	/**
//	 * Returns true if the token is expired or not available. If the token is
//	 * expired, the client MUST request a new token to renew the authorization
//	 * header.
//	 * 
//	 * @throws SEPAPropertiesException
//	 * 
//	 * @see AuthenticationProperties
//	 */
//	private boolean isTokenExpired() {
//		return oauthProperties.isTokenExpired();
//	}

	private Response register(String url, String identity) {
		logger.info("REGISTER " + identity);

		CloseableHttpResponse response = null;
		long start = Timings.getTime();

		try {
			URI uri = new URI(url);
			ByteArrayEntity body = new ByteArrayEntity(new RegistrationRequest(identity).toString().getBytes("UTF-8"));

			HttpPost httpRequest = new HttpPost(uri);
			httpRequest.setHeader("Content-Type", "application/json");
			httpRequest.setHeader("Accept", "application/json");
			httpRequest.setEntity(body);

			logger.trace(httpRequest);

			try {
				response = getSSLHttpClient().execute(httpRequest);
			} catch (IOException e) {
				logger.error("HTTP EXECUTE: " + e.getMessage());
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "HttpExecute", e.getMessage());
			}

			logger.debug("Response: " + response);
			HttpEntity entity = response.getEntity();
			String jsonResponse = EntityUtils.toString(entity, Charset.forName("UTF-8"));

			EntityUtils.consume(entity);

			JsonObject json = new JsonParser().parse(jsonResponse).getAsJsonObject();

			if (json.has("error")) {
				int code = json.get("status_code").getAsInt();
				String error = json.get("error").getAsString();
				String description = json.get("error_description").getAsString();

				ErrorResponse ret = new ErrorResponse(code, error, description);
				logger.error(ret);

				return ret;
			}

			String id = json.get("credentials").getAsJsonObject().get("client_id").getAsString();
			String secret = json.get("credentials").getAsJsonObject().get("client_secret").getAsString();
			JsonElement signature = json.get("credentials").getAsJsonObject().get("signature");

			Timings.log("REGISTER", start, Timings.getTime());

			return new RegistrationResponse(id, secret, signature);

		} catch (URISyntaxException e) {
			logger.error(e.getMessage());
			Timings.log("REGISTER_ERROR", start, Timings.getTime());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "URISyntaxException", e.getMessage());
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
			Timings.log("REGISTER_ERROR", start, Timings.getTime());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "UnsupportedEncodingException",
					e.getMessage());
		} catch (ParseException e) {
			logger.error(e.getMessage());
			Timings.log("REGISTER_ERROR", start, Timings.getTime());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "ParseException", e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
			Timings.log("REGISTER_ERROR", start, Timings.getTime());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "IOException", e.getMessage());
		} finally {
			try {
				if (response != null)
					response.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
				Timings.log("REGISTER_ERROR", start, Timings.getTime());
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "IOException", e.getMessage());
			}
		}
	}

	private Response requestToken(String url, String authorization) {
		logger.info("TOKEN_REQUEST: " + authorization);

		CloseableHttpResponse response = null;
		long start = Timings.getTime();

		try {
			URI uri = new URI(url);

			HttpPost httpRequest = new HttpPost(uri);
			httpRequest.setHeader("Content-Type", "application/json");
			httpRequest.setHeader("Accept", "application/json");
			httpRequest.setHeader("Authorization", authorization);

			try {
				response = getSSLHttpClient().execute(httpRequest);
				// break;
			} catch (IOException e) {
				logger.error("HTTP EXECUTE: " + e.getMessage());
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "HttpExecute", e.getMessage());
			}

			logger.debug("Response: " + response);
			HttpEntity entity = response.getEntity();
			String jsonResponse = EntityUtils.toString(entity, Charset.forName("UTF-8"));
			EntityUtils.consume(entity);

			// Parse response
			JsonObject json = new JsonParser().parse(jsonResponse).getAsJsonObject();

			if (json.has("error")) {
				Timings.log("TOKEN_REQUEST", start, Timings.getTime());
				ErrorResponse error = new ErrorResponse(json.get("status_code").getAsInt(),
						json.get("error").getAsString(), json.get("error_description").getAsString());
				return error;
			}

			return new JWTResponse(json);
		} catch (Exception e) {
			logger.error(e.getMessage());
			Timings.log("TOKEN_REQUEST", start, Timings.getTime());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Exception", e.getMessage());
		} finally {
			try {
				if (response != null)
					response.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
				Timings.log("TOKEN_REQUEST", start, Timings.getTime());
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "IOException", e.getMessage());
			}
		}
	}

	public String getClientId() {
		return oauthProperties.getClientId();
	}
}

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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import java.util.Date;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
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


public class ClientSecurityManager {

	/** The log4j2 logger. */
	private static final Logger logger = LogManager.getLogger();

	private final AuthenticationProperties oauthProperties;

	private final String jksName;
	private final String jksPassword;

	public ClientSecurityManager(AuthenticationProperties oauthProp)  throws SEPASecurityException {
		this.jksName = null;
		this.jksPassword = null;
		
		oauthProperties = oauthProp;
		
		if (!oauthProperties.trustAll()) throw new SEPASecurityException("Missing JKS file and password");
	}
	
	public ClientSecurityManager(AuthenticationProperties oauthProp, String jksName, String jksPassword) throws SEPASecurityException {		
		oauthProperties = oauthProp;
		
		if (oauthProperties.trustAll()) throw new SEPASecurityException("Wrong constructor. Trust all. JKS parameters not used");
		
		// Arguments check
		if (jksName == null || jksPassword == null ) //|| keyPassword == null)
			throw new SEPASecurityException("JKS name or passwords are null");

		// Initialize SSL context
		File f = new File(jksName);
		if (!f.exists() || f.isDirectory())
			throw new SEPASecurityException(jksName + " not found");

		this.jksName = jksName;
		this.jksPassword = jksPassword;			
	}
	
	public SSLContext getSSLContext() throws SEPASecurityException {
		if (!oauthProperties.trustAll()) return new SSLManager().getSSLContextFromJKS(jksName, jksPassword);
		return new SSLManager().getSSLContextTrustAllCa(oauthProperties.getSSLProtocol());
	}
	
	public CloseableHttpClient getSSLHttpClient() throws SEPASecurityException {
		if (!oauthProperties.trustAll()) return new SSLManager().getSSLHttpClient(jksName, jksPassword);
		return new SSLManager().getSSLHttpClientTrustAllCa(oauthProperties.getSSLProtocol());
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
	public Response register(String identity, int timeout) throws SEPASecurityException, SEPAPropertiesException {
		if (oauthProperties == null)
			throw new SEPAPropertiesException("Authorization properties are null");

		Response ret = register(oauthProperties.getRegisterUrl(), identity, timeout);

		if (ret.isRegistrationResponse()) {
			RegistrationResponse reg = (RegistrationResponse) ret;
			oauthProperties.setCredentials(reg.getClientId(), reg.getClientSecret());
		} else {
			logger.error(ret);
		}

		return ret;
	}

	public Response register(String identity) throws SEPASecurityException, SEPAPropertiesException {
		return register(identity, 5000);
	}

	/**
	 * Returns the Bearer authentication header if the token is not expired,
	 * otherwise requests and returns a fresh token
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

	public void setClientCredentials(String username, String password)
			throws SEPAPropertiesException, SEPASecurityException {
		oauthProperties.setCredentials(username, password);
	}

	public Response refreshToken(int timeout) throws SEPAPropertiesException, SEPASecurityException {
		if (!isClientRegistered()) {
			return new ErrorResponse(401, "invalid_client", "Client is not registered");
		}

		Response ret = requestToken(oauthProperties.getTokenRequestUrl(),
				oauthProperties.getBasicAuthorizationHeader(),timeout);

		if (ret.isJWTResponse()) {
			JWTResponse jwt = (JWTResponse) ret;

			logger.debug("New token: " + jwt);

			oauthProperties.setJWT(jwt);
		} else {
			logger.error("FAILED to refresh token " + new Date() + " Response: " + ret);
		}

		return ret;
	}
	
	public Response refreshToken() throws SEPAPropertiesException, SEPASecurityException {
		return refreshToken(5000);
	}

	private Response register(String url, String identity, int timeout) throws SEPASecurityException {
		if (identity == null) throw new SEPASecurityException("Identity is null");
			
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

			// Set timeout
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
					.build();
			httpRequest.setConfig(requestConfig);

			logger.trace(httpRequest);

			try {
				if (!oauthProperties.trustAll()) response = new SSLManager().getSSLHttpClient(jksName, jksPassword).execute(httpRequest);
				else response = new SSLManager().getSSLHttpClientTrustAllCa(oauthProperties.getSSLProtocol()).execute(httpRequest);
//				response = new SSLManager().getSSLHttpClient(jksName, jksPassword).execute(httpRequest);
			} catch (IOException | SEPASecurityException e) {
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

	private Response requestToken(String url, String authorization,int timeout) {
		logger.info("TOKEN_REQUEST: " + authorization);

		CloseableHttpResponse response = null;
		long start = Timings.getTime();

		try {
			URI uri = new URI(url);

			HttpPost httpRequest = new HttpPost(uri);
			httpRequest.setHeader("Content-Type", "application/json");
			httpRequest.setHeader("Accept", "application/json");
			httpRequest.setHeader("Authorization", authorization);

			// Set timeout
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
					.build();
			httpRequest.setConfig(requestConfig);

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

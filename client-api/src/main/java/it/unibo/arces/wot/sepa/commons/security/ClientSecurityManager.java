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

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;

import javax.net.ssl.SSLContext;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.JWTResponse;
import it.unibo.arces.wot.sepa.commons.response.RegistrationResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.AuthenticationProperties.OAUTH_PROVIDER;

public class ClientSecurityManager implements Closeable {

	/** The log4j2 logger. */
	private static final Logger logger = LogManager.getLogger();

	private final AuthenticationProperties oauthProperties;
	
	private final AuthenticationService oauth;
	
	public ClientSecurityManager(AuthenticationProperties oauthProp) throws SEPASecurityException {		
		oauthProperties = oauthProp;
		
		if (oauthProperties.getProvider().equals(OAUTH_PROVIDER.SEPA)) oauth = new DefaultAuthenticationService(oauthProp);
		else oauth = new KeycloakAuthenticationService(oauthProp);
	}
	
	public SSLContext getSSLContext() throws SEPASecurityException {
		return oauth.getSSLContext();
	}
	
	public CloseableHttpClient getSSLHttpClient() {
		return oauth.getSSLHttpClient();
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
	public Response register(String client_id, String username,String initialAccessToken,int timeout) throws SEPASecurityException, SEPAPropertiesException {
		if (oauthProperties == null)
			throw new SEPAPropertiesException("Authorization properties are null");

		Response ret = oauth.register(client_id,username, initialAccessToken,timeout);

		if (ret.isRegistrationResponse()) {
			RegistrationResponse reg = (RegistrationResponse) ret;
			oauthProperties.setCredentials(reg.getClientId(), reg.getClientSecret());
		} else {
			logger.error(ret);
		}

		return ret;
	}

	public Response register(String client_id,String username,String initialAccessToken) throws SEPASecurityException, SEPAPropertiesException {
		return register(client_id,username,initialAccessToken, 5000);
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

		Response ret = oauth.requestToken(oauthProperties.getBasicAuthorizationHeader(),timeout);

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

	@Override
	public void close() throws IOException {
		oauth.close();
	}
}

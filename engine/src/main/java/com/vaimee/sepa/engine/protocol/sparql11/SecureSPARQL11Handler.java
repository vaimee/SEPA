/* HTTP handler for SPARQL 1.1 update over SSL
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

package com.vaimee.sepa.engine.protocol.sparql11;

import org.apache.http.Header;
import org.apache.http.HttpRequest;

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.security.ClientAuthorization;
import com.vaimee.sepa.engine.bean.SEPABeans;
import com.vaimee.sepa.engine.dependability.Dependability;
import com.vaimee.sepa.engine.scheduling.Scheduler;
import com.vaimee.sepa.logging.Logging;

public class SecureSPARQL11Handler extends SPARQL11Handler implements SecureSPARQL11HandlerMBean {

	public SecureSPARQL11Handler(Scheduler scheduler,String queryPath,String updatePath) throws IllegalArgumentException {
		super(scheduler,queryPath,updatePath);
		
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	/**
	 * Operation when receiving a HTTP request at a protected endpoint
	 * 
<pre>
Specific to HTTP request:
1. Check if the request contains an Authorization header. 
2. Check if the request contains an Authorization: Bearer-header with non-null/empty contents 
3. Check if the value of the Authorization: Bearer-header is a JWT object 

Token validation:
4. Check if the JWT object is signed 
5. Check if the signature of the JWT object is valid. This is to be checked with AS public signature verification key 
6. Check the contents of the JWT object 
7. Check if the value of "iss" is https://wot.arces.unibo.it:8443/oauth/token 
8. Check if the value of "aud" contains https://wot.arces.unibo.it:8443/sparql 
9. Accept the request as well as "sub" as the originator of the request and process it as usual
 
Respond with 401 if not

According to RFC6749, the error member can assume the following values: invalid_request, invalid_client, invalid_grant, unauthorized_client, unsupported_grant_type, invalid_scope.
	
	invalid_request
               The request is missing a required parameter, includes an
               unsupported parameter value (other than grant type),
               repeats a parameter, includes multiple credentials,
               utilizes more than one mechanism for authenticating the
               client, or is otherwise malformed.

         invalid_client
               Client authentication failed (e.g., unknown client, no
               client authentication included, or unsupported
               authentication method).  The authorization server MAY
               return an HTTP 401 (Unauthorized) status code to indicate
               which HTTP authentication schemes are supported.  If the
               client attempted to authenticate via the "Authorization"
               request header field, the authorization server MUST
               respond with an HTTP 401 (Unauthorized) status code and
               include the "WWW-Authenticate" response header field
               matching the authentication scheme used by the client.

         invalid_grant
               The provided authorization grant (e.g., authorization
               code, resource owner credentials) or refresh token is
               invalid, expired, revoked, does not match the redirection
               URI used in the authorization request, or was issued to
               another client.

         unauthorized_client
               The authenticated client is not authorized to use this
               authorization grant type.

         unsupported_grant_type
               The authorization grant type is not supported by the
               authorization server.
</pre>
	 * @throws SEPASecurityException 
	 */
	@Override
	protected ClientAuthorization authorize(HttpRequest request) throws SEPASecurityException {
		// Extract Bearer authorization
		Header[] bearer = request.getHeaders("Authorization");

		if (bearer.length == 0) {
			Logging.error("Authorization header is missing");
			return new ClientAuthorization("invalid_request","Authorization header is missing");
		}

		if (bearer.length > 1) {
			Logging.error("Multiple authorization headers not allowed");
			return new ClientAuthorization("invalid_request","Multiple authorization headers not allowed");
		}
		
		if (!bearer[0].getValue().toUpperCase().startsWith("BEARER ")) {
			Logging.error("Authorization must be ***Bearer JWT***");
			return new ClientAuthorization("unsupported_grant_type","Authorization header must be ***Bearer JWT***");
		}

		// ******************
		// JWT validation
		// ******************
		String jwt = bearer[0].getValue().split(" ")[1];

		return Dependability.validateToken(jwt);
	}

	@Override
	public long getErrors_AuthorizingFailed() {
		return jmx.getErrors_AuthorizingFailed();
	}
}

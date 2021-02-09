/* This class represents an response to an access token request. It contains a description of the token (e.g., JWT)
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

package it.unibo.arces.wot.sepa.commons.response;

import java.text.ParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jwt.SignedJWT;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

/**
 * Produce JWT compliant with WoT W3C recommendations
 * 
 * {"access_token":"eyJhbGciOiJSUzI1NiJ9.
 * eyJzdWIiOiJTRVBBRW5naW5lIiwiYXVkIjpbImh0dHBzOlwvXC93b3QuYXJjZXMudW5pYm8uaXQ6ODQ0M
 * 1wvc3BhcnFsIiwid3NzOlwvXC93b3QuYXJjZXMudW5pYm8uaXQ6OTQ0M1wvc3BhcnFsIl0sIm5iZiI6MT
 * Q5MTAzMzQ4MjI2MiwiaXNzIjoiaHR0cHM6XC9cL3dvdC5hcmNlcy51bmliby5pdCIsImV4cCI6MTQ5MTA
 * zNzA4MjI2MiwiaWF0IjoxNDkxMDMzNDgyMjYyLCJqdGkiOiJjZTIwZmM3NC05NWU1LTQ2NzEtYTllOS1k
 * MjMwZmE4NTlhMTQ6NjhhMmYwOWQtN2E4NS00YzU1LTgxOWUtZWU1YWRhYjgxNDI1In0.IwTisstsZhJVu
 * Guhes4s9GE6sikh0rPtJg4QtY1DFT3OZ3WDF05OCwsBCe6dkNOn__68-e_9cEoiFY4s4KQ8heRQHpyRuD
 * QK0vTOefpgumKtRHrlCe0JGHBnPNqo8Zp7cVivZnin8NsePcuweFgZxWfaOC-EH5ClpqjPEbjj65g",
 * "token_type":"bearer", "expires_in":3600}
 * 
 * Keycloak OpenID Connect token
 * 
 * {"access_token":"..isSaVqlen4bH0C5oAg1--",
 * "expires_in":18000,
 * "refresh_expires_in":1800,
 * "refresh_token":"..",
 * "token_type":"bearer",
 * "not-before-policy":0,
 * "session_state":"bc94b90c-fd2c-417c-a19f-4f5c98653236",
 * "scope":"profile email"}
 */

public class JWTResponse extends Response {
	/** The log4j2 logger. */
	private static final Logger logger = LogManager.getLogger();
	
	/**
	 * Instantiates a new JWT response.
	 * @throws SEPASecurityException 
	 *
	 */
	public JWTResponse(SignedJWT token) throws SEPASecurityException {
		super();
		
		json = new JsonObject();
		
		json.add("access_token", new JsonPrimitive(token.serialize()));
		json.add("token_type", new JsonPrimitive("bearer"));
		
		try {
			json.add("expires_in", new JsonPrimitive(token.getJWTClaimsSet().getExpirationTime().getTime()-token.getJWTClaimsSet().getIssueTime().getTime()));
		} catch (ParseException e) {
			logger.error(e.getMessage());
			throw new SEPASecurityException(e);
		}
	}
	
	public JWTResponse(JsonObject json) {
		super();
		this.json = json;
	}
	
	/**
	 * Gets the access token.
	 *
	 * @return the access token
	 */
	public String getAccessToken() {
		try {
			return json.get("access_token").getAsString();
		}
		catch(Exception e) {
			return "";
		}
	}

	/**
	 * Gets the token type.
	 *
	 * @return the token type
	 */
	public String getTokenType() {
		try {
			return json.get("token_type").getAsString();
		}
		catch(Exception e) {
			return "";
		}
	}

	/**
	 * Gets the expires in.
	 *
	 * @return the expires in
	 */
	public long getExpiresIn() {
		try {
			return json.get("expires_in").getAsLong();
		}
		catch(Exception e) {
			return 0;
		}
	}
}

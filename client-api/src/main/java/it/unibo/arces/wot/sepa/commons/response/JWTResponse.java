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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
 */
public class JWTResponse extends Response {

	/**
	 * Instantiates a new JWT response.
	 *
	 * @param access_token
	 *            the access token
	 * @param token_type
	 *            the token type
	 * @param expiring
	 *            the expiring
	 */
	public JWTResponse(String access_token, String token_type, long expiring) {
		super();
		JsonObject jwt = new JsonObject();
		
		if (access_token != null)
			jwt.add("access_token", new JsonPrimitive(access_token));
		if (token_type != null)
			jwt.add("token_type", new JsonPrimitive(token_type));
		if (expiring > 0)
			jwt.add("expires_in", new JsonPrimitive(expiring));
		
		json.add("token", jwt);
	}

	/**
	 * Gets the access token.
	 *
	 * @return the access token
	 */
	public String getAccessToken() {
		try {
			return json.get("token").getAsJsonObject().get("access_token").getAsString();
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
			return json.get("token").getAsJsonObject().get("token_type").getAsString();
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
			return json.get("token").getAsJsonObject().get("expires_in").getAsLong();
		}
		catch(Exception e) {
			return 0;
		}
	}
}

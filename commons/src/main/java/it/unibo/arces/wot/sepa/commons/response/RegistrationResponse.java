/* This class represents the response to a registration request
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

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

// TODO: Auto-generated Javadoc
/**
 * {"client_id":"5b60a155-bada-4499-bc6f-26b4d37bc1ef",
 * "client_secret":"40e18d77-421c-48ce-a44a-14da1238e923", "signature":
 * {"kty":"RSA", "e":"AQAB", "x5t":"...", "kid":"sepacertificate",
 * "x5c":["..."], "n":"..."}}
 */
public class RegistrationResponse extends Response {

	/**
	 * Instantiates a new registration response.
	 *
	 * @param client_id
	 *            the client id
	 * @param client_secret
	 *            the client secret
	 * @param jwkPublicKey
	 *            the jwk public key
	 */
	public RegistrationResponse(String client_id, String client_secret, JsonElement jwkPublicKey) {
		super();
		json.add("client_id", new JsonPrimitive(client_id));
		json.add("client_secret", new JsonPrimitive(client_secret));
		json.add("signature", jwkPublicKey);
	}

	/**
	 * Gets the client id.
	 *
	 * @return the client id
	 */
	public String getClientId() {
		return json.get("client_id").getAsString();
	}

	/**
	 * Gets the client secret.
	 *
	 * @return the client secret
	 */
	public String getClientSecret() {
		return json.get("client_secret").getAsString();
	}
}

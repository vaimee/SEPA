/* This class represents a unsubscribe request
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
package it.unibo.arces.wot.sepa.commons.request;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.request.Request;

/**
 * The Class UnsubscribeRequest.
 */
public class UnsubscribeRequest extends Request {

	/**
	 * Instantiates a new unsubscribe request.
	 *
	 * @param token
	 *            the token
	 * @param subId
	 *            the sub id
	 */
	public UnsubscribeRequest(String subId,String authorization,long timeout) {
		super(subId, authorization,timeout);
	}
	
	/**
	 * Gets the subscribe UUID.
	 *
	 * @return the subscribe UUID
	 */
	public String getSubscribeUUID() {
		return super.getSPARQL();
	}

	@Override
	public String toString() {
		// Create SPARQL 1.1 Subscribe JSON request
		JsonObject body = new JsonObject();
		JsonObject request = new JsonObject();
		body.add("spuid", new JsonPrimitive(getSubscribeUUID()));
		if (getAuthorizationHeader() != null)
			body.add("authorization", new JsonPrimitive(getAuthorizationHeader()));
		
		body.add("timeout", new JsonPrimitive(getTimeout()));
		
		request.add("unsubscribe", body);

		return request.toString();
	}
}

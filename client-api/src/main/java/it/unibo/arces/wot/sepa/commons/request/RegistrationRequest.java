/* This class represents a registration request
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * The Class RegistrationRequest.
 */
public class RegistrationRequest {
	
	/** The JSON. */
	JsonObject json = new JsonObject();
	
	/**
	 * Instantiates a new registration request.
	 *
	 * @param id the unique identifier of the client
	 */
	public RegistrationRequest(String id) {
		JsonObject body = new JsonObject();
		body.add("client_identity", new JsonPrimitive(id));
		JsonArray grants = new JsonArray();
		grants.add("client_credentials");
		body.add("grant_types", grants);
		
		json.add("register", body);
	}
	
	/**
	 * {"register":{"client_identity":"id","grant_types":["client_credentials"]}}.
	 *
	 * @return the string
	 */
	public String toString() {
		return json.toString();
	}

}

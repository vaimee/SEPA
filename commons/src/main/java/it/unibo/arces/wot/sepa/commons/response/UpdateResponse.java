/* This class represents the response to a SPARQL 1.1 Update
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
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.response.Response;

// TODO: Auto-generated Javadoc
/**
 * This class represents the response of a SPARQL 1.1 Update
 */

public class UpdateResponse extends Response {

	/**
	 * Instantiates a new update response.
	 *
	 * @param token
	 *            the token
	 * @param body
	 *            the body
	 */
	public UpdateResponse(Integer token, String body) {
		super(token);

		try {
			JsonObject jbody = new JsonParser().parse(body).getAsJsonObject();
			
			json.add("updateResponse", new JsonObject());
			json.get("updateResponse").getAsJsonObject().add("body", jbody);
			json.get("updateResponse").getAsJsonObject().add("isJsonBody", new JsonPrimitive(true));
			
		}
		catch(Exception e) {
			json.add("updateResponse", new JsonObject());
			json.get("updateResponse").getAsJsonObject().add("body", new JsonPrimitive(body));
			json.get("updateResponse").getAsJsonObject().add("isJsonBody", new JsonPrimitive(false));
		}
	}

	/**
	 * Instantiates a new update response.
	 *
	 * @param body
	 *            the body
	 */
	public UpdateResponse(String body) {
		super();

		try {
			JsonObject jbody = new JsonParser().parse(body).getAsJsonObject();
			
			json.add("response", new JsonObject());
			json.get("response").getAsJsonObject().add("body", jbody);
			json.get("response").getAsJsonObject().add("isJson", new JsonPrimitive(true));
			
		}
		catch(Exception e) {
			json.add("response", new JsonObject());
			json.get("response").getAsJsonObject().add("body", new JsonPrimitive(body));
			json.get("response").getAsJsonObject().add("isJson", new JsonPrimitive(false));
		}
	}

	public UpdateResponse(UpdateResponse ret) {
		super(ret.getToken());
		json = ret.json;
	}
}

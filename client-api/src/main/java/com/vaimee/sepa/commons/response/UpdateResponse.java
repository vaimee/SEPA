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
package com.vaimee.sepa.commons.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Set;
import org.apache.jena.sparql.core.Quad;



/**
 * This class represents the response of a SPARQL 1.1 Update
 */

public class UpdateResponse extends Response {
	/**
	 * Instantiates a new update response.
	 *
	 * @param body
	 *            the body
	 */
        public final Set<Quad>     updatedTuples;
        public final Set<Quad>     removedTuples;
        
        public UpdateResponse(Set<Quad> r, Set<Quad> u) {
            updatedTuples = u;
            removedTuples = r;
        }
	public UpdateResponse(String body) {
		super();
                updatedTuples = null;
                removedTuples = null;
		try {
			JsonObject jbody = new Gson().fromJson(body,JsonObject.class);
			
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
}

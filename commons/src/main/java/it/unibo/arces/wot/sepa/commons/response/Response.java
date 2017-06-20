/* This class represents a generic abstract response
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

// TODO: Auto-generated Javadoc
/**
 * This class represents the response to a generic request.
* */

public abstract class Response {
	
	/** The json. */
	protected JsonObject json;
	
	/** The token. */
	private int token = -1;

	/**
	 * Instantiates a new response.
	 *
	 * @param token the token
	 */
	public Response(Integer token) {
		this.token = token;
		json = new JsonObject();
	}
	
	/**
	 * Instantiates a new response.
	 */
	public Response() {
		json = new JsonObject();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return json.toString();
	}
	
	/**
	 * Gets the token.
	 *
	 * @return the token
	 */
	public int getToken() {
		return token;
	}
	
	/**
	 * Gets the as json object.
	 *
	 * @return the as json object
	 */
	public JsonObject getAsJsonObject(){
		return json;
	}
}

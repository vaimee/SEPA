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

/**
 * This class represents the response to a generic request.
* */

public abstract class Response {
	
	/** The json. */
	protected JsonObject json;

	public boolean isError() {
		return this instanceof ErrorResponse;
	}
	public boolean isJWTResponse() {
		return this instanceof JWTResponse;
	}
	public boolean isNotification() {
		return this instanceof Notification;
	}
	public boolean isQueryResponse() {
		return (this instanceof QueryResponse);
	}
	public boolean isRegistrationResponse() {
		return this instanceof RegistrationResponse;
	}
	public boolean isSubscribeResponse() {
		return this instanceof SubscribeResponse;
	}
	public boolean isUnsubscribeResponse() {
		return this instanceof UnsubscribeResponse;
	}
	public boolean isUpdateResponse() {
		return this instanceof UpdateResponse;
	}
	
	/**
	 * Instantiates a new response.
	 */
	public Response() {
		json = new JsonObject();
	}
	
	@Override
	public String toString() {
		return json.toString();
	}
}

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
		return this.getClass().equals(ErrorResponse.class);
	}
	public boolean isJWTResponse() {
		return this.getClass().equals(JWTResponse.class);
	}
	public boolean isNotification() {
		return this.getClass().equals(Notification.class);
	}
	public boolean isQueryResponse() {
		return this.getClass().equals(QueryResponse.class);
	}
	public boolean isRegistrationResponse() {
		return this.getClass().equals(RegistrationResponse.class);
	}
	public boolean isSubscribeResponse() {
		return this.getClass().equals(SubscribeResponse.class);
	}
	public boolean isUnsubscribeResponse() {
		return this.getClass().equals(UnsubscribeResponse.class);
	}
	public boolean isUpdateResponse() {
		return this.getClass().equals(UpdateResponse.class);
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

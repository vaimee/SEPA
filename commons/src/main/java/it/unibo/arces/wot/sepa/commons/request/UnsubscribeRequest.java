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

import it.unibo.arces.wot.sepa.commons.request.Request;

/**
 * The Class UnsubscribeRequest.
 */
public class UnsubscribeRequest extends Request {
	
	/**
	 * Instantiates a new unsubscribe request.
	 *
	 * @param token the token
	 * @param subId the sub id
	 */
	public UnsubscribeRequest(Integer token, String subId) {
		super(token, subId);
	}
	
	/**
	 * Instantiates a new unsubscribe request.
	 *
	 * @param subID the sub ID
	 */
	public UnsubscribeRequest(String subID) {
		super(subID);
	}

	/**
	 * Gets the subscribe UUID.
	 *
	 * @return the subscribe UUID
	 */
	public String getSubscribeUUID(){
		return super.getSPARQL();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (token != -1) return "UNSUBSCRIBE #"+token+" "+getSubscribeUUID();
		return "UNSUBSCRIBE "+getSubscribeUUID();
	}
}

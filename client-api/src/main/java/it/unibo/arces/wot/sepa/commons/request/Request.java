/* This class represents a generic abstract request
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

/**
 * This class represents a generic request (i.e., QUERY, UPDATE, SUBSCRIBE,
 * UNSUBSCRIBE)
 */

public abstract class Request {
	protected long timeout = 5000;
	protected String authorizationHeader = null;
	
	/** The sparql. */
	protected String sparql;

	/**
	 * Instantiates a new request.
	 *
	 * @param sparql
	 *            the sparql
	 * @param auth
	 *            the authorization header (e.g., Basic ... , Bearer ... , ...). 
	 *            
	 *            The 'Basic' HTTP Authentication Scheme, https://tools.ietf.org/html/rfc7617
	 */

	public Request(String sparql,String auth,long timeout) {
		this.sparql = sparql;
		this.authorizationHeader = auth;
	}

	@Override
	public int hashCode() {
		return sparql.hashCode();
	}
	
	/**
	 * Gets the sparql.
	 *
	 * @return the sparql
	 */
	public String getSPARQL() {
		return sparql;
	}

	public String getAuthorizationHeader() {
		return authorizationHeader;
	}
	
	public boolean isQueryRequest() {
		return this.getClass().equals(QueryRequest.class);
	}

	public boolean isUpdateRequest() {
		return this.getClass().equals(UpdateRequest.class);
	}

	public boolean isSubscribeRequest() {
		return this.getClass().equals(SubscribeRequest.class);
	}

	public boolean isUnsubscribeRequest() {
		return this.getClass().equals(UnsubscribeRequest.class);
	}

	public long getTimeout() {
		return timeout;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}

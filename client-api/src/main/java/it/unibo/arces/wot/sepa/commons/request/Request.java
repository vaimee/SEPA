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

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;

/**
 * This class represents a generic request (i.e., QUERY, UPDATE, SUBSCRIBE,
 * UNSUBSCRIBE)
 */

public abstract class Request {

	/** The token. */
	protected int token = -1;

	/** The sparql. */
	protected String sparql;

//	/**
//	 * Authorization related members
//	 * 
//	 * 1) The 'Basic' HTTP Authentication Scheme,
//	 * https://tools.ietf.org/html/rfc7617
//	 */
//
//	protected enum AUTHENTICATION_SCHEMA {
//		DISABLED, BASIC
//	};
//
//	protected AUTHENTICATION_SCHEMA authorization = AUTHENTICATION_SCHEMA.DISABLED;
//	protected String basicAuthorizationHeader;

	protected HTTPMethod method = HTTPMethod.POST;
	protected String id = null;

	protected int timeout = 5000;
	
	protected String scheme = null;
	protected String host = null;
	protected int port = -1;
	protected String path = null;

	protected String authorizationHeader = null;
	
	/**
	 * Instantiates a new request.
	 *
	 * @param token
	 *            the token
	 * @param sparql
	 *            the sparql
	 * @param auth
	 *            the authorization header (e.g., Basic ... , Bearer ... , ...). 
	 *            
	 *            The 'Basic' HTTP Authentication Scheme, https://tools.ietf.org/html/rfc7617
	 */

	public Request(int token, String sparql,String auth) {
		this.token = token;
		this.sparql = sparql;
		this.authorizationHeader = auth;
	}
	
	public Request(int token, String sparql) {
		this.token = token;
		this.sparql = sparql;
	}

	/**
	 * Instantiates a new request.
	 *
	 * @param sparql
	 *            the sparql
	 */
	public Request(String sparql) {
		this.token = -1;
		this.sparql = sparql;
	}
	
	public Request(String sparql,String auth) {
		this.token = -1;
		this.sparql = sparql;
		this.authorizationHeader = auth;
	}

	public void setToken(int token) {
		this.token = token;
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
	 * Gets the sparql.
	 *
	 * @return the sparql
	 */
	public String getSPARQL() {
		return sparql;
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
	
	/**
	 * Default implementation. Two requests are equal if they belong to the same class and their SPARQL strings are equals. SPARQL matching should be based on SPARQL algebra
	 * and SPARQL semantics. The default implementation provides a syntax based matching. 
	 */
	@Override
	public boolean equals(Object obj) {
		if (!obj.getClass().equals(this.getClass())) return false;
		return sparql.equals(((QueryRequest)obj).sparql);
	}
	
	public HTTPMethod getHttpMethod() {
		return method;
	}
	
	public String getID() {
		return id;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getScheme() {
		return scheme;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getPath() {
		return path;
	}

	public String getAuthorizationHeader() {
		return authorizationHeader;
	}
}

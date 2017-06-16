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

// TODO: Auto-generated Javadoc
/**
 * This class represents a generic request (i.e., QUERY, UPDATE, SUBSCRIBE,
 * UNSUBSCRIBE)
 */

public abstract class Request {

	/** The token. */
	protected int token = -1;

	/** The sparql. */
	protected String sparql;

	/**
	 * Instantiates a new request.
	 *
	 * @param token
	 *            the token
	 * @param sparql
	 *            the sparql
	 */
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
}

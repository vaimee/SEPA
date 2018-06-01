/* This class represents a subscribe request
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

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;

// TODO: Auto-generated Javadoc
/**
 * This class represents the request of performing a SPARQL 1.1 Subscribe
* */

public class SubscribeRequest extends QueryRequest {
	
	/** The alias. */
	private String alias = null;
	
	/**
	 * Instantiates a new subscribe request.
	 *
	 * @param sparql the sparql
	 */
	public SubscribeRequest(String sparql) {
		this(-1,sparql);
	}

	/**
	 * Instantiates a new subscribe request.
	 *
	 * @param sparql the sparql
	 * @param alias the alias
	 */
	public SubscribeRequest(String sparql,String alias) {
		this(-1,sparql,alias);
		this.alias = alias;
	}

	/**
	 * Instantiates a new subscribe request.
	 *
	 * @param token the token
	 * @param sparql the sparql
	 * @param alias the alias
	 */
	public SubscribeRequest(Integer token, String sparql, String alias) {
		super(token, sparql);
		this.alias = alias;
	}

	/**
	 * Instantiates a new subscribe request.
	 *
	 * @param token the token
	 * @param sparql the sparql
	 */
	public SubscribeRequest(Integer token, String sparql) {
		super(token, sparql);
	}

	/* (non-Javadoc)
	 * @see wot.arces.unibo.SEPA.commons.request.QueryRequest#toString()
	 */
	public String toString() {
		String str = "SUBSCRIBE";
		if (token != -1) str += " #"+token;
		if (alias != null) str += "("+alias+")";
		str += " "+sparql;

		return str;
	}
	
	/**
	 * This method returns the alias of the subscription. 
	 * 
	 * @return The subscription alias or <i>null</i> is not present
	* */
	public String getAlias() {
		return alias;
	}
}

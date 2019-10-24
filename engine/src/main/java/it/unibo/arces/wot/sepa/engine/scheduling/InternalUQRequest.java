/* The engine internal abstract representation of a update or query request
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

package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.engine.dependability.authorization.Credentials;

public abstract class InternalUQRequest extends InternalRequest {
	protected String sparql;
	protected String defaultGraphUri;
	protected String namedGraphUri;
	
	public InternalUQRequest(String sparql,String defaultGraphUri,String namedGraphUri,Credentials credentials) {
		super(credentials);
		
		if (sparql == null) throw new IllegalArgumentException("SPARQL is null");
		
		this.sparql = sparql;
		this.defaultGraphUri = defaultGraphUri;
		this.namedGraphUri = namedGraphUri;
	}
	
	public String getSparql() {
		return sparql;
	}
	
	public String getDefaultGraphUri() {
		return defaultGraphUri;
	}
	
	public String getNamedGraphUri() {
		return namedGraphUri;
	}
	
	@Override
	public int hashCode() {
		return sparql.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return sparql.equals(((InternalUQRequest)obj).sparql);
	}
}

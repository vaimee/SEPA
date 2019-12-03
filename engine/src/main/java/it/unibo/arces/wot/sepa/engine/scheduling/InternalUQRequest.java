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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import it.unibo.arces.wot.sepa.engine.dependability.authorization.ClientAuthorization;

public abstract class InternalUQRequest extends InternalRequest {
	protected String sparql;
	protected String defaultGraphUri;
	protected String namedGraphUri;
	
	public InternalUQRequest(String sparql,String defaultGraphUri,String namedGraphUri,ClientAuthorization auth) {
		super(auth);
		
		if (sparql == null) throw new IllegalArgumentException("SPARQL is null");
		
		this.sparql = evaluateFunctions(sparql);
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
	
	// Override the SPARQL now() function to return the UTC time
	private String evaluateFunctions(String sparql) {
		OffsetDateTime utc = OffsetDateTime.now(ZoneOffset.UTC);
		String timestamp = "'" + utc.toString() + "'^^xsd:dateTime";
		
		String temp = sparql.replace("now()", timestamp);
		
		return temp;		
	}
}

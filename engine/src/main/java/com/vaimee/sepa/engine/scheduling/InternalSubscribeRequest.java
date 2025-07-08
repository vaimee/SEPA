/* The engine internal representation of a subscribe request
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
package com.vaimee.sepa.engine.scheduling;

import java.util.Set;

import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASparqlParsingException;
import com.vaimee.sepa.api.commons.response.Notification;
import com.vaimee.sepa.api.commons.security.ClientAuthorization;
import com.vaimee.sepa.engine.core.EventHandler;
import com.vaimee.sepa.engine.gates.Gate;

public class InternalSubscribeRequest extends InternalQueryRequest {

	private String alias = null;
	private EventHandler gate;
	
	public InternalSubscribeRequest(String sparql, String alias,Set<String> defaultGraphUri, Set<String> namedGraphUri,EventHandler gate,ClientAuthorization auth) throws SEPASparqlParsingException {
		super(sparql, defaultGraphUri, namedGraphUri,auth);
		
		this.alias = alias;
		this.gate = gate;
	}
	
	@Override
	public String toString() {
		return "*SUBSCRIBE* "+sparql + " DEFAULT GRAPH URI: <"+defaultGraphUri + "> NAMED GRAPH URI: <" + namedGraphUri+">";
	}
	
	public String getAlias() {
		return alias;
	}
	
	public void setEventHandler(EventHandler gate) {
		this.gate = gate;
	}

	public void notifyEvent(Notification notify) throws SEPAProtocolException {
		if (gate != null) gate.notifyEvent(notify);	
	}
	
	public String getGID() {
		if (Gate.class.isInstance(gate)) {
			return ((Gate)gate).getGID();
		}
		return null;
	}

	public boolean ping() {
		if (gate != null) return gate.ping();
		return false;
	}
}

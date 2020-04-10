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
package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.gates.Gate;

public class InternalSubscribeRequest extends InternalQueryRequest {

	private String alias = null;
	private EventHandler gate;
	
	public InternalSubscribeRequest(String sparql, String alias,String defaultGraphUri, String namedGraphUri,EventHandler gate,ClientAuthorization auth) {
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
	
//	public EventHandler getEventHandler() {
//		return gate;
//	}

	public void notifyEvent(Notification notify) throws SEPAProtocolException {
		if (gate != null) gate.notifyEvent(notify);	
	}
	
	public String getGID() {
		if (Gate.class.isInstance(gate)) {
			return ((Gate)gate).getGID();
		}
		return null;
	}
}

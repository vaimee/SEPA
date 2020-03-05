/* The interface to be implemented by a subscription protocol implementation 
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.api;

import java.io.Closeable;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;

public abstract class SubscriptionProtocol implements Closeable {
	protected ISubscriptionHandler handler;
	protected final ClientSecurityManager sm;
	
	public void setHandler(ISubscriptionHandler handler) {
		this.handler = handler;
	}

	public SubscriptionProtocol() {
		this.sm = null;
		this.handler = null;
	}
	
	public SubscriptionProtocol(ISubscriptionHandler handler) {
		this.handler = handler;
		this.sm = null;
	}
	
	public SubscriptionProtocol(ISubscriptionHandler handler,ClientSecurityManager sm) {
		this.handler = handler;
		this.sm = sm;
	}
	

	public abstract void subscribe(SubscribeRequest request) throws SEPAProtocolException;

	public abstract void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException;
}

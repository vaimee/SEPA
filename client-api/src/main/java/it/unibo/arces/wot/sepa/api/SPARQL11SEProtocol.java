/* This class is part of the SPARQL 1.1 SE Protocol (an extension of the W3C SPARQL 1.1 Protocol) API
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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;

import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

/**
 * This class implements the SPARQL 1.1 Secure event protocol with SPARQL 1.1
 * subscribe language.
 *
 * @see <a href="http://wot.arces.unibo.it/TR/sparql11-se-protocol.html">SPARQL
 *      1.1 Secure Event Protocol</a>
 * @see <a href="http://wot.arces.unibo.it/TR/sparql11-subscribe.html">SPARQL
 *      1.1 Subscribe Language</a>
 */
public class SPARQL11SEProtocol extends SPARQL11Protocol {
	private static final Logger logger = LogManager.getLogger();

	private SubscriptionProtocol subscriptionProtocol;
	
	public SPARQL11SEProtocol(SubscriptionProtocol protocol, SEPASecurityManager sm) throws SEPAProtocolException {
		super(sm);
		
		if (protocol == null) throw new IllegalArgumentException("Protocol is null");
		if (!protocol.isSecure()) throw new SEPAProtocolException(new IllegalArgumentException("Mixing secure and not secure protocols is not allowed"));
		
		this.subscriptionProtocol = protocol;
	}
	
	public SPARQL11SEProtocol(SubscriptionProtocol protocol) throws SEPAProtocolException {
		if (protocol == null  ) {
			logger.error("One or more arguments are null");
			throw new IllegalArgumentException("One or more arguments are null");
		}
		
		if (protocol.isSecure()) throw new SEPAProtocolException(new IllegalArgumentException("Security parameters are missing"));
		
		this.subscriptionProtocol = protocol;
	}
	
	public boolean isSecure() {
		return subscriptionProtocol.isSecure();
	}

	/**
	 * Subscribe with a SPARQL 1.1 Subscription language. All the notification will
	 * be forwarded to the {@link ISubscriptionHandler} of this instance.
	 *
	 * @param request
	 * @return A valid {@link Response} if the subscription is successful <br>
	 *         an {@link ErrorResponse} otherwise
	 * @throws SEPAProtocolException 
	 */
	public void subscribe(SubscribeRequest request) throws SEPAProtocolException {
		logger.debug(request.toString());
		
		subscriptionProtocol.subscribe(request);
	}

	/**
	 * Unsubscribe with a SPARQL 1.1 Subscription language. Note that you must
	 * supply a SPUID that identify the subscription that you want to delete. This
	 * primitive does not free any resources, you must call the {@link #close()}
	 * method.
	 *
	 * @param request
	 * @return A valid {@link Response} if the unsubscription is successful <br>
	 *         an {@link ErrorResponse} otherwise
	 * @throws SEPAProtocolException 
	 */
	public void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException {
		logger.debug(request.toString());
		
		subscriptionProtocol.unsubscribe(request);
	}

	/**
	 * Free the http connection manager and the WebSocket client.
	 * @throws SEPAProtocolException 
	 *
	 * @throws IOException
	 */
	@Override
	public void close() {
		super.close();
		subscriptionProtocol.close();
	}
}

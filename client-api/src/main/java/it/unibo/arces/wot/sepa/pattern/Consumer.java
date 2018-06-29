/* This class abstracts a consumer of the SEPA Application Design Pattern
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

package it.unibo.arces.wot.sepa.pattern;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;
import it.unibo.arces.wot.sepa.api.ISubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.api.protocol.websocket.WebSocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public abstract class Consumer extends Client implements IConsumer {
	private static final Logger logger = LogManager.getLogger("Consumer");

	protected String sparqlSubscribe = null;
	protected String subID = "";
	private Bindings forcedBindings;

	protected SPARQL11SEProtocol client;

	public Consumer(JSAP appProfile, String subscribeID, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException {
		super(appProfile);

		if (subscribeID == null) {
			logger.fatal("Subscribe ID is null");
			throw new SEPAProtocolException(new IllegalArgumentException("Subscribe ID is null"));
		}

		if (appProfile.getSPARQLQuery(subscribeID) == null) {
			logger.fatal("SUBSCRIBE ID [" + subscribeID + "] not found in " + appProfile.getFileName());
			throw new IllegalArgumentException(
					"SUBSCRIBE ID [" + subscribeID + "] not found in " + appProfile.getFileName());
		}

		sparqlSubscribe = appProfile.getSPARQLQuery(subscribeID);

		forcedBindings = appProfile.getQueryBindings(subscribeID);

		if (sparqlSubscribe == null) {
			logger.fatal("SPARQL subscribe is null");
			throw new SEPAProtocolException(new IllegalArgumentException("SPARQL subscribe is null"));
		}

		ISubscriptionProtocol protocol = null;

		if (appProfile.getSubscribeProtocol(subscribeID).equals(SubscriptionProtocol.WS)) {
			throw new SEPAProtocolException(new IllegalArgumentException("Wrong constructor. Use the unsecure one"));
		}

		protocol = new WebSocketSubscriptionProtocol(appProfile.getSubscribeHost(subscribeID),
				appProfile.getSubscribePort(subscribeID), appProfile.getSubscribePath(subscribeID),sm);

		client = new SPARQL11SEProtocol(protocol, this);
		
		subID = subscribeID;
	}

	public Consumer(JSAP appProfile, String subscribeID) throws SEPAProtocolException {
		super(appProfile);

		if (subscribeID == null) {
			logger.fatal("Subscribe ID is null");
			throw new SEPAProtocolException(new IllegalArgumentException("Subscribe ID is null"));
		}

		if (appProfile.getSPARQLQuery(subscribeID) == null) {
			logger.fatal("SUBSCRIBE ID [" + subscribeID + "] not found in " + appProfile.getFileName());
			throw new IllegalArgumentException(
					"SUBSCRIBE ID [" + subscribeID + "] not found in " + appProfile.getFileName());
		}

		sparqlSubscribe = appProfile.getSPARQLQuery(subscribeID);

		forcedBindings = appProfile.getQueryBindings(subscribeID);

		if (sparqlSubscribe == null) {
			logger.fatal("SPARQL subscribe is null");
			throw new SEPAProtocolException(new IllegalArgumentException("SPARQL subscribe is null"));
		}

		ISubscriptionProtocol protocol = null;

		if (appProfile.getSubscribeProtocol(subscribeID).equals(SubscriptionProtocol.WSS)) {
			throw new SEPAProtocolException(new IllegalArgumentException("Missing security parameters"));
		}

		protocol = new WebSocketSubscriptionProtocol(appProfile.getSubscribeHost(subscribeID),
				appProfile.getSubscribePort(subscribeID), appProfile.getSubscribePath(subscribeID));

		client = new SPARQL11SEProtocol(protocol, this);
		
		subID = subscribeID;
	}

	public final void setSubscribeBindingValue(String variable, RDFTerm value) throws IllegalArgumentException {
		forcedBindings.setBindingValue(variable, value);

	}

	public final Response subscribe() throws SEPASecurityException, IOException, SEPAPropertiesException {
		String authorizationHeader = null;
		
		if (isSecure()) {
			if (!getToken())
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "Failed to get or renew token");
			if (appProfile.getAuthenticationProperties()!= null)
				authorizationHeader = appProfile.getAuthenticationProperties().getBearerAuthorizationHeader();
		}

		String sparql = prefixes() + replaceBindings(sparqlSubscribe, forcedBindings);
		
		Response response = client.subscribe(new SubscribeRequest(sparql, null, appProfile.getDefaultGraphURI(subID),
				appProfile.getNamedGraphURI(subID),
				authorizationHeader));

		if (response.isSubscribeResponse()) {
			subID = ((SubscribeResponse) response).getSpuid();
		}

		return response;
	}

	public final Response unsubscribe() throws SEPASecurityException, IOException, SEPAPropertiesException {
		logger.debug("UNSUBSCRIBE " + subID);

		if (isSecure()) {
			if (!getToken())
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "Failed to get or renew token");
		}

		return client.unsubscribe(
				new UnsubscribeRequest(subID, appProfile.getAuthenticationProperties().getBearerAuthorizationHeader()));
	}

	@Override
	public void close() throws IOException {
		client.close();
	}

	@Override
	public final void onSemanticEvent(Notification notify) {
		ARBindingsResults results = notify.getARBindingsResults();

		BindingsResults added = results.getAddedBindings();
		BindingsResults removed = results.getRemovedBindings();

		// Dispatch different notifications based on notify content
		if (!added.isEmpty())
			onAddedResults(added);
		if (!removed.isEmpty())
			onRemovedResults(removed);
		onResults(results);
	}
}

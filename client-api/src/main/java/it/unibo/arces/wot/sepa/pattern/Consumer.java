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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.api.protocols.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.Notification;
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

		SubscriptionProtocol protocol = null;

		protocol = new WebsocketSubscriptionProtocol(appProfile.getSubscribeHost(subscribeID),
				appProfile.getSubscribePort(subscribeID), appProfile.getSubscribePath(subscribeID),sm,this);

		client = new SPARQL11SEProtocol(protocol);
		
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

		SubscriptionProtocol protocol = null;

		protocol = new WebsocketSubscriptionProtocol(appProfile.getSubscribeHost(subscribeID),
				appProfile.getSubscribePort(subscribeID), appProfile.getSubscribePath(subscribeID),this);

		client = new SPARQL11SEProtocol(protocol);
		
		subID = subscribeID;
	}

	public final void setSubscribeBindingValue(String variable, RDFTerm value) throws IllegalArgumentException {
		forcedBindings.setBindingValue(variable, value);

	}

	public final void subscribe(long timeout) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException {
		String authorizationHeader = null;
		
		if (isSecure()) authorizationHeader = sm.getAuthorizationHeader();

		String sparql = prefixes() + replaceBindings(sparqlSubscribe, forcedBindings);
		
		client.subscribe(new SubscribeRequest(sparql, null, appProfile.getDefaultGraphURI(subID),
				appProfile.getNamedGraphURI(subID),
				authorizationHeader,timeout));
	}

	public final void unsubscribe(long timeout) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException {
		logger.debug("UNSUBSCRIBE " + subID);

		String oauth =  null;
		if (isSecure()) oauth = sm.getAuthorizationHeader();

		client.unsubscribe(
				new UnsubscribeRequest(subID, oauth,timeout));
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

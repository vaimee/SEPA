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

import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;
import it.unibo.arces.wot.sepa.logging.Logging;
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;

public abstract class Consumer extends Client implements IConsumer {
	private final String sparqlSubscribe;	
	protected final String subID;
	private final ForcedBindings forcedBindings;
	private boolean subscribed = false;
	private SPARQL11SEProtocol client;
	private String spuid = null;
	private SubscriptionProtocol protocol;
	
	public Consumer(JSAP appProfile, String subscribeID)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(appProfile);

		if (subscribeID == null) {
			Logging.logger.fatal("Subscribe ID is null");
			throw new SEPAProtocolException(new IllegalArgumentException("Subscribe ID is null"));
		}
		
		if (appProfile.getSPARQLQuery(subscribeID) == null) {
			Logging.logger.fatal("SUBSCRIBE ID [" + subscribeID + "] not found in " + appProfile.getFileName());
			throw new IllegalArgumentException(
					"SUBSCRIBE ID [" + subscribeID + "] not found in " + appProfile.getFileName());
		}

		subID = subscribeID;
		
		sparqlSubscribe = appProfile.getSPARQLQuery(subscribeID);

		forcedBindings = (ForcedBindings) appProfile.getQueryBindings(subscribeID);

		if (sparqlSubscribe == null) {
			Logging.logger.fatal("SPARQL subscribe is null");
			throw new SEPAProtocolException(new IllegalArgumentException("SPARQL subscribe is null"));
		}

		// Subscription protocol
		
		protocol = new WebsocketSubscriptionProtocol(appProfile.getSubscribeHost(subID),
				appProfile.getSubscribePort(subID), appProfile.getSubscribePath(subID),this,sm);

		client = new SPARQL11SEProtocol(protocol,sm);
	}
	
	public final void setSubscribeBindingValue(String variable, RDFTerm value) throws SEPABindingsException {
		forcedBindings.setBindingValue(variable, value);
	}

	public final void subscribe() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		subscribe(TIMEOUT, NRETRY);
	}
	
	public final void subscribe(long timeout,long nRetry) throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		String authorizationHeader = null;
		
		this.TIMEOUT = timeout;
		this.NRETRY = nRetry;
		
		if (isSecure()) authorizationHeader = appProfile.getAuthenticationProperties().getBearerAuthorizationHeader();
		
		client.subscribe(new SubscribeRequest(appProfile.addPrefixesAndReplaceBindings(sparqlSubscribe, addDefaultDatatype(forcedBindings,subID,true)), null, appProfile.getDefaultGraphURI(subID),
				appProfile.getNamedGraphURI(subID),
				authorizationHeader,timeout,nRetry));
	}

	public final void unsubscribe() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		unsubscribe(TIMEOUT, NRETRY);
	}
	
	public final void unsubscribe(long timeout,long nRetry) throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		Logging.logger.debug("UNSUBSCRIBE " + spuid);

		String authorizationHeader = null;
		
		if (isSecure()) authorizationHeader = appProfile.getAuthenticationProperties().getBearerAuthorizationHeader();
		
		client.unsubscribe(
				new UnsubscribeRequest(spuid, authorizationHeader,timeout,nRetry));
	}

	@Override
	public void close() throws IOException {
		super.close();
		client.close();
		protocol.close();
	}

	public boolean isSubscribed() {
		return subscribed;
	}
	
	@Override
	public final void onSemanticEvent(Notification notify) {		
		ARBindingsResults results = notify.getARBindingsResults();

		BindingsResults added = results.getAddedBindings();
		BindingsResults removed = results.getRemovedBindings();

		Logging.logger.trace("onSemanticEvent: "+notify.getSpuid()+" "+notify.getSequence());
		
		if (notify.getSequence() == 0) {
			onFirstResults(added);
			return;
		}
		
		onResults(results);
		
		// Dispatch different notifications based on notify content
		if (!added.isEmpty())
			onAddedResults(added);
		if (!removed.isEmpty())
			onRemovedResults(removed);
	}
	
	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		Logging.logger.warn("onBrokenConnection");
		subscribed = false;
		
		// Auto reconnection mechanism
		if (appProfile.reconnect()) {
			try {
				protocol = new WebsocketSubscriptionProtocol(appProfile.getSubscribeHost(subID),
						appProfile.getSubscribePort(subID), appProfile.getSubscribePath(subID),this,sm);
				client = new SPARQL11SEProtocol(protocol,sm);
			} catch (SEPASecurityException | SEPAProtocolException e1) {
				Logging.logger.error(e1.getMessage());
				return;
			}	
			
			while(!subscribed) {
				try {
					subscribe(TIMEOUT,NRETRY);
				} catch (SEPASecurityException | SEPAPropertiesException | SEPAProtocolException
						| SEPABindingsException e) {
					Logging.logger.error(e.getMessage());
					if (Logging.logger.isTraceEnabled()) e.printStackTrace();
				}
				try {
					synchronized (client) {
						client.wait(TIMEOUT);	
					}
				} catch (InterruptedException e) {
					Logging.logger.error(e.getMessage());
					if (Logging.logger.isTraceEnabled()) e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		Logging.logger.error(errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		synchronized(client) {
			Logging.logger.trace("onSubscribe");
			subscribed = true;
			this.spuid = spuid;
			client.notify();
			synchronized(this) {
				notify();
			}
		}
	}

	@Override
	public void onUnsubscribe(String spuid) {
		Logging.logger.trace("onUnsubscribe");
		synchronized(client) {
			Logging.logger.trace("onUnsubscribe");
			subscribed = false;
			this.spuid = null;
			client.notify();
			synchronized(this) {
				notify();
			}
		}
	}
	
	@Override
	public void onAddedResults(BindingsResults results) {
		Logging.logger.trace("Added results "+results);
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		Logging.logger.trace("Removed results "+results);
	}
	
	@Override
	public void onResults(ARBindingsResults results) {
		Logging.logger.trace("Results "+results);
	}

	@Override
	public void onFirstResults(BindingsResults results) {
		Logging.logger.trace("First results "+results);
	}
}

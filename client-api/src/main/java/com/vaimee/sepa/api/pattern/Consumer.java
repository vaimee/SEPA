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

package com.vaimee.sepa.api.pattern;

import java.io.IOException;

import com.vaimee.sepa.api.commons.sparql.ARBindingsResults;
import com.vaimee.sepa.api.commons.sparql.BindingsResults;
import com.vaimee.sepa.api.commons.sparql.RDFTerm;
import com.vaimee.sepa.logging.Logging;
import com.vaimee.sepa.api.SubscriptionProtocol;
import com.vaimee.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import com.vaimee.sepa.api.SPARQL11SEProtocol;
import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.properties.SubscriptionProtocolProperties;
import com.vaimee.sepa.api.commons.request.SubscribeRequest;
import com.vaimee.sepa.api.commons.request.UnsubscribeRequest;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Notification;

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
			Logging.getLogger().fatal("Subscribe ID is null");
			throw new SEPAProtocolException(new IllegalArgumentException("Subscribe ID is null"));
		}
		
		if (appProfile.getSPARQLQuery(subscribeID) == null) {
			Logging.getLogger().fatal("SUBSCRIBE ID [" + subscribeID + "] not found");
			throw new IllegalArgumentException(
					"SUBSCRIBE ID [" + subscribeID + "] not found");
		}

		subID = subscribeID;
		
		sparqlSubscribe = appProfile.getSPARQLQuery(subscribeID);

		forcedBindings = (ForcedBindings) appProfile.getQueryBindings(subscribeID);

		if (sparqlSubscribe == null) {
			Logging.getLogger().fatal("SPARQL subscribe is null");
			throw new SEPAProtocolException(new IllegalArgumentException("SPARQL subscribe is null"));
		}

		// Subscription protocol
		SubscriptionProtocolProperties properties = appProfile.getSubscribeProtocol(subID);
		protocol = new WebsocketSubscriptionProtocol(appProfile.getSubscribeHost(subID),properties,this,sm);

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
		
		client.subscribe(new SubscribeRequest(appProfile.addPrefixesAndReplaceBindings(sparqlSubscribe, addUpdateDefaultDatatype(forcedBindings,subID,true)), null, appProfile.getDefaultGraphURI(subID),
				appProfile.getNamedGraphURI(subID),
				authorizationHeader,timeout,nRetry));
	}

	public final void unsubscribe() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		unsubscribe(TIMEOUT, NRETRY);
	}
	
	public final void unsubscribe(long timeout,long nRetry) throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		Logging.getLogger().debug("UNSUBSCRIBE " + spuid);

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

	public final boolean isSubscribed() {
		return subscribed;
	}
	
	@Override
	public final void onSemanticEvent(Notification notify) {		
		ARBindingsResults results = notify.getARBindingsResults();

		BindingsResults added = results.getAddedBindings();
		BindingsResults removed = results.getRemovedBindings();

		Logging.getLogger().trace("onSemanticEvent: "+notify.getSpuid()+" "+notify.getSequence());
		
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
	public final void onBrokenConnection(ErrorResponse errorResponse) {
		Logging.getLogger().warn("onBrokenConnection");
		subscribed = false;
		
		// Auto reconnection mechanism
		if (appProfile.reconnect()) {
			try {
				SubscriptionProtocolProperties properties = appProfile.getSubscribeProtocol(subID);
				protocol = new WebsocketSubscriptionProtocol(appProfile.getSubscribeHost(subID),properties,this,sm);
				client = new SPARQL11SEProtocol(protocol,sm);
			} catch (SEPASecurityException | SEPAProtocolException e1) {
				Logging.getLogger().error(e1.getMessage());
				return;
			}	
			
			while(!subscribed) {
				try {
					subscribe(TIMEOUT,NRETRY);
				} catch (SEPASecurityException | SEPAPropertiesException | SEPAProtocolException
						| SEPABindingsException e) {
					Logging.getLogger().error(e.getMessage());
					if (Logging.getLogger().isTraceEnabled()) e.printStackTrace();
				}
				try {
					synchronized (client) {
						client.wait(TIMEOUT);	
					}
				} catch (InterruptedException e) {
					Logging.getLogger().error(e.getMessage());
					if (Logging.getLogger().isTraceEnabled()) e.printStackTrace();
				}
			}
		}
	}

	@Override
	public final void onSubscribe(String spuid, String alias) {
		synchronized(client) {
			Logging.getLogger().trace("onSubscribe");
			subscribed = true;
			this.spuid = spuid;
			client.notify();
			synchronized(this) {
				notify();
			}
		}
	}

	@Override
	public final void onUnsubscribe(String spuid) {
		Logging.getLogger().trace("onUnsubscribe");
		synchronized(client) {
			Logging.getLogger().trace("onUnsubscribe");
			subscribed = false;
			this.spuid = null;
			client.notify();
			synchronized(this) {
				notify();
			}
		}
	}
	
	@Override
	public void onError(ErrorResponse errorResponse) {
		Logging.getLogger().error(errorResponse);
	}

	
	@Override
	public void onAddedResults(BindingsResults results) {
		Logging.getLogger().trace("Added results "+results);
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		Logging.getLogger().trace("Removed results "+results);
	}
	
	@Override
	public void onResults(ARBindingsResults results) {
		Logging.getLogger().trace("Results "+results);
	}

	@Override
	public void onFirstResults(BindingsResults results) {
		Logging.getLogger().trace("First results "+results);
	}
}

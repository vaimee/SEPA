/* This class implements a generic client of the SEPA Application Design Pattern (including the query primitive)
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
import java.net.URISyntaxException;
import java.util.Hashtable;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.RegistrationResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class GenericClient extends Client {

	class Handler implements ISubscriptionHandler {
		private ISubscriptionHandler _handler;
		private String _url;
		private SPARQL11SEProtocol _client;
		
		public Handler(String url,SPARQL11SEProtocol client, ISubscriptionHandler handler) {
			_url = url;
			_client = client;
			_handler = handler;
		}
		
		@Override
		public void onSemanticEvent(Notification notify) {
			if (_handler != null) _handler.onSemanticEvent(notify);
		}

		@Override
		public void onBrokenConnection() {
			if (_handler != null) _handler.onBrokenConnection();
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			if (_handler != null) _handler.onError(errorResponse);
		}

		@Override
		public void onSubscribe(String spuid, String alias) {
			activeUrls.put(_url, _client);
			subscriptions.put(spuid, _client);
			if (_handler != null) _handler.onSubscribe(spuid,alias);
		}

		@Override
		public void onUnsubscribe(String spuid) {
			if (_handler != null) _handler.onUnsubscribe(spuid);
			subscriptions.remove(spuid);
		}
		
	}
	// URL ==> client
	private Hashtable<String, SPARQL11SEProtocol> activeUrls = new Hashtable<String, SPARQL11SEProtocol>();
	
	// SPUID ==> URL
	private Hashtable<String, SPARQL11SEProtocol> subscriptions = new Hashtable<String, SPARQL11SEProtocol>();
	
	public GenericClient(JSAP appProfile) throws SEPAProtocolException {
		super(appProfile,null);
	}

	public GenericClient(JSAP appProfile, SEPASecurityManager sm) throws SEPAProtocolException {
		super(appProfile,sm);
	}

	public Response update(String ID, String sparql, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		return _update(ID, sparql, forced, timeout);
	}

	public Response update(String ID, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		return _update(ID, null, forced, timeout);
	}

	public Response query(String ID, String sparql, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		return _query(ID, sparql, forced, timeout);
	}

	public Response query(String ID, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		try {
			return _query(ID, null, forced, timeout);
		} catch (IOException e) {
			throw new SEPAProtocolException(e);
		}
	}

	public void subscribe(String ID, String sparql, Bindings forced, ISubscriptionHandler handler, long timeout)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		try {
			_subscribe(ID, sparql, forced, handler,timeout);
		} catch (IOException | URISyntaxException e) {
			throw new SEPAProtocolException(e);
		}
	}

	public void subscribe(String ID, Bindings forced, ISubscriptionHandler handler, long timeout)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		try {
			_subscribe(ID, null, forced, handler,timeout);
		} catch (IOException | URISyntaxException e) {
			throw new SEPAProtocolException(e);
		}
	}

	public void unsubscribe(String subID,long timeout) throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		if (!subscriptions.containsKey(subID)) return;

		String auth = null;
		try {
			auth = sm.getAuthorizationHeader();
		} catch (Exception e) {
		}

		subscriptions.get(subID).unsubscribe(new UnsubscribeRequest(subID, auth,timeout));
	}

	private Response _update(String ID, String sparql, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		SPARQL11Protocol client;

		String auth = null;
		if (isSecure()) {
			client = new SPARQL11Protocol(sm);		
			try {
				auth = sm.getAuthorizationHeader();
			} catch (Exception e) {
			}
		} else
			client = new SPARQL11Protocol();

		if (sparql == null)
			sparql = appProfile.getSPARQLUpdate(ID);
		Response ret = client.update(new UpdateRequest(appProfile.getUpdateMethod(ID),
				appProfile.getUpdateProtocolScheme(ID), appProfile.getUpdateHost(ID), appProfile.getUpdatePort(ID),
				appProfile.getUpdatePath(ID), addPrefixesAndReplaceBindings(sparql, forced),
				appProfile.getUsingGraphURI(ID), appProfile.getUsingNamedGraphURI(ID), auth,timeout));
		client.close();

		return ret;
	}

	private Response _query(String ID, String sparql, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		SPARQL11Protocol client;

		String auth = null;
		if (isSecure()) {
			client = new SPARQL11Protocol(sm);
			try {
				auth = sm.getAuthorizationHeader();
			} catch (Exception e) {
			}
		} else
			client = new SPARQL11Protocol();

		if (sparql == null)
			sparql = appProfile.getSPARQLQuery(ID);
		Response ret = client.query(new QueryRequest(appProfile.getQueryMethod(ID),
				appProfile.getQueryProtocolScheme(ID), appProfile.getQueryHost(ID), appProfile.getQueryPort(ID),
				appProfile.getQueryPath(ID), addPrefixesAndReplaceBindings(sparql, forced),
				appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID), auth,timeout));
		client.close();

		return ret;
	}

	private void _subscribe(String ID, String sparql, Bindings forced, ISubscriptionHandler handler,long timeout)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, URISyntaxException, SEPABindingsException {

		// Create client
		String url = null;
		SubscriptionProtocol protocol = null;
		String auth = null;
		SPARQL11SEProtocol client = null;

		switch (appProfile.getSubscribeProtocol(ID)) {
		case WS:
			url = "ws_" + appProfile.getSubscribeHost(ID) + "_" + appProfile.getSubscribePort(ID) + "_"
					+ appProfile.getSubscribePath(ID);

			if (!activeUrls.containsKey(url)) {
				protocol = new WebsocketSubscriptionProtocol(appProfile.getSubscribeHost(ID),
						appProfile.getSubscribePort(ID), appProfile.getSubscribePath(ID));
				client = new SPARQL11SEProtocol(protocol);
				
				protocol.setHandler(new Handler(url,client,handler));
				
			} else
				client = activeUrls.get(url);

			break;
		case WSS:
			url = "wss_" + appProfile.getSubscribeHost(ID) + "_" + appProfile.getSubscribePort(ID) + "_"
					+ appProfile.getSubscribePath(ID);

			if (!activeUrls.containsKey(url)) {
				protocol = new WebsocketSubscriptionProtocol(appProfile.getSubscribeHost(ID),
						appProfile.getSubscribePort(ID), appProfile.getSubscribePath(ID));
				protocol.enableSecurity(sm);
				client = new SPARQL11SEProtocol(protocol);
				
				protocol.setHandler(new Handler(url,client,handler));
			} else
				client = activeUrls.get(url);

			try {
				auth = sm.getAuthorizationHeader();
			} catch (Exception e) {
				throw new SEPASecurityException("Failed to get bearer authorization header");
			}

			break;
		}

		// Send request
		if (sparql == null)
			sparql = appProfile.getSPARQLQuery(ID);

		SubscribeRequest req = new SubscribeRequest(addPrefixesAndReplaceBindings(sparql, forced), null,
				appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID), auth,timeout);

		client.subscribe(req);
	}

	@Override
	public void close() throws IOException {
		for (SPARQL11SEProtocol client : activeUrls.values())
			client.close();
	}

	// Registration to the Authorization Server (AS)
	public Response register(String jksFile,String storePwd,String keyPwd,String identity) throws SEPASecurityException, SEPAPropertiesException {
		SEPASecurityManager security = new SEPASecurityManager(jksFile,storePwd,keyPwd,appProfile.getAuthenticationProperties());

		Response ret = security.register(identity);

		if (ret.isRegistrationResponse()) {
			RegistrationResponse registration = (RegistrationResponse) ret;
			appProfile.getAuthenticationProperties().setCredentials(registration.getClientId(),
					registration.getClientSecret());
		}

		return ret;
	}
}

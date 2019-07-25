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

/**
 * The Class GenericClient.
 */
public class GenericClient extends Client {

	/**
	 * The Class Handler.
	 */
	class Handler implements ISubscriptionHandler {

		/** The handler. */
		private ISubscriptionHandler _handler;

		/** The url. */
		private String _url;

		/** The client. */
		private SPARQL11SEProtocol _client;

		/**
		 * Instantiates a new handler.
		 *
		 * @param url     the url
		 * @param client  the client
		 * @param handler the handler
		 */
		public Handler(String url, SPARQL11SEProtocol client, ISubscriptionHandler handler) {
			_url = url;
			_client = client;
			_handler = handler;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * it.unibo.arces.wot.sepa.api.ISubscriptionHandler#onSemanticEvent(it.unibo.
		 * arces.wot.sepa.commons.response.Notification)
		 */
		@Override
		public void onSemanticEvent(Notification notify) {
			if (_handler != null)
				_handler.onSemanticEvent(notify);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see it.unibo.arces.wot.sepa.api.ISubscriptionHandler#onBrokenConnection()
		 */
		@Override
		public void onBrokenConnection() {
			if (_handler != null)
				_handler.onBrokenConnection();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * it.unibo.arces.wot.sepa.api.ISubscriptionHandler#onError(it.unibo.arces.wot.
		 * sepa.commons.response.ErrorResponse)
		 */
		@Override
		public void onError(ErrorResponse errorResponse) {
			if (_handler != null)
				_handler.onError(errorResponse);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see it.unibo.arces.wot.sepa.api.ISubscriptionHandler#onSubscribe(java.lang.
		 * String, java.lang.String)
		 */
		@Override
		public void onSubscribe(String spuid, String alias) {
			activeUrls.put(_url, _client);
			subscriptions.put(spuid, _client);
			if (_handler != null)
				_handler.onSubscribe(spuid, alias);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * it.unibo.arces.wot.sepa.api.ISubscriptionHandler#onUnsubscribe(java.lang.
		 * String)
		 */
		@Override
		public void onUnsubscribe(String spuid) {
			if (_handler != null)
				_handler.onUnsubscribe(spuid);
			subscriptions.remove(spuid);
		}

	}

	/** The active urls. */
	// URL ==> client
	private Hashtable<String, SPARQL11SEProtocol> activeUrls = new Hashtable<String, SPARQL11SEProtocol>();

	/** The subscriptions. */
	// SPUID ==> URL
	private Hashtable<String, SPARQL11SEProtocol> subscriptions = new Hashtable<String, SPARQL11SEProtocol>();

	/**
	 * Instantiates a new generic client.
	 *
	 * @param appProfile the JSAP profile
	 * @throws SEPAProtocolException the SEPA protocol exception
	 */
	public GenericClient(JSAP appProfile) throws SEPAProtocolException {
		super(appProfile, null);
	}

	/**
	 * Instantiates a new generic client.
	 *
	 * @param appProfile the JSAP profile
	 * @param sm         the security manager (needed for secure connections)
	 * @throws SEPAProtocolException the SEPA protocol exception
	 */
	public GenericClient(JSAP appProfile, SEPASecurityManager sm) throws SEPAProtocolException {
		super(appProfile, sm);
	}

	/**
	 * Update.
	 *
	 * @param ID      the identifier of the update within the JSAP
	 * @param sparql  if specified it replaces the default SPARQL in the JSAP
	 * @param forced  the forced bindings
	 * @param timeout the timeout
	 * @return the response
	 * @throws SEPAProtocolException   the SEPA protocol exception
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws IOException             Signals that an I/O exception has occurred.
	 * @throws SEPAPropertiesException the SEPA properties exception
	 * @throws SEPABindingsException   the SEPA bindings exception
	 */
	public Response update(String ID, String sparql, Bindings forced, int timeout) throws SEPAProtocolException,
			SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		return _update(ID, sparql, forced, timeout);
	}

	/**
	 * Update.
	 *
	 * @param ID      the identifier of the update within the JSAP
	 * @param forced  the forced bindings
	 * @param timeout the timeout
	 * @return the response
	 * @throws SEPAProtocolException   the SEPA protocol exception
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws IOException             Signals that an I/O exception has occurred.
	 * @throws SEPAPropertiesException the SEPA properties exception
	 * @throws SEPABindingsException   the SEPA bindings exception
	 */
	public Response update(String ID, Bindings forced, int timeout) throws SEPAProtocolException, SEPASecurityException,
			IOException, SEPAPropertiesException, SEPABindingsException {
		return _update(ID, null, forced, timeout);
	}

	/**
	 * Query.
	 *
	 * @param ID      the identifier of the query within the JSAP
	 * @param sparql  if specified it replaces the default SPARQL in the JSAP
	 * @param forced  the forced bindings
	 * @param timeout the timeout
	 * @return the response
	 * @throws SEPAProtocolException   the SEPA protocol exception
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws IOException             Signals that an I/O exception has occurred.
	 * @throws SEPAPropertiesException the SEPA properties exception
	 * @throws SEPABindingsException   the SEPA bindings exception
	 */
	public Response query(String ID, String sparql, Bindings forced, int timeout) throws SEPAProtocolException,
			SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		return _query(ID, sparql, forced, timeout);
	}

	/**
	 * Query.
	 *
	 * @param ID      the identifier of the query within the JSAP
	 * @param forced  the forced
	 * @param timeout the timeout
	 * @return the response
	 * @throws SEPAProtocolException   the SEPA protocol exception
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws SEPAPropertiesException the SEPA properties exception
	 * @throws SEPABindingsException   the SEPA bindings exception
	 */
	public Response query(String ID, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		return _query(ID, null, forced, timeout);

	}

	/**
	 * Subscribe.
	 *
	 * @param ID      the identifier of the subscribe within the JSAP
	 * @param sparql  if specified it replaces the default SPARQL in the JSAP
	 * @param forced  the forced
	 * @param handler the handler
	 * @param timeout the timeout
	 * @throws SEPAProtocolException   the SEPA protocol exception
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws SEPAPropertiesException the SEPA properties exception
	 * @throws SEPABindingsException   the SEPA bindings exception
	 */
	public void subscribe(String ID, String sparql, Bindings forced, ISubscriptionHandler handler, long timeout,
			String alias)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		_subscribe(ID, sparql, forced, handler, timeout, alias);

	}

	public void subscribe(String ID, String sparql, Bindings forced, ISubscriptionHandler handler, long timeout)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		_subscribe(ID, sparql, forced, handler, timeout, null);

	}

	/**
	 * Subscribe.
	 *
	 * @param ID      the identifier of the subscribe within the JSAP
	 * @param forced  the forced
	 * @param handler the handler
	 * @param timeout the timeout
	 * @throws SEPAProtocolException   the SEPA protocol exception
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws SEPAPropertiesException the SEPA properties exception
	 * @throws SEPABindingsException   the SEPA bindings exception
	 */
	public void subscribe(String ID, Bindings forced, ISubscriptionHandler handler, long timeout, String alias)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		_subscribe(ID, null, forced, handler, timeout, alias);

	}

	public void subscribe(String ID, Bindings forced, ISubscriptionHandler handler, long timeout)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		_subscribe(ID, null, forced, handler, timeout, null);

	}

	/**
	 * Unsubscribe.
	 *
	 * @param ID      the SPUID of the active subscription
	 * @param timeout the timeout
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws SEPAPropertiesException the SEPA properties exception
	 * @throws SEPAProtocolException   the SEPA protocol exception
	 */
	public void unsubscribe(String subID, long timeout)
			throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		if (!subscriptions.containsKey(subID))
			return;

		String auth = null;
		try {
			auth = sm.getAuthorizationHeader();
		} catch (Exception e) {
		}

		subscriptions.get(subID).unsubscribe(new UnsubscribeRequest(subID, auth, timeout));
	}

	/**
	 * Update.
	 *
	 * @param ID      the identifier of the update within the JSAP
	 * @param sparql  if specified it replaces the default SPARQL in the JSAP
	 * @param forced  the forced
	 * @param timeout the timeout
	 * @return the response
	 * @throws SEPAProtocolException   the SEPA protocol exception
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws IOException             Signals that an I/O exception has occurred.
	 * @throws SEPAPropertiesException the SEPA properties exception
	 * @throws SEPABindingsException   the SEPA bindings exception
	 */
	private Response _update(String ID, String sparql, Bindings forced, int timeout) throws SEPAProtocolException,
			SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		SPARQL11Protocol client = new SPARQL11Protocol(sm);

		String auth = null;
		try {
			auth = sm.getAuthorizationHeader();
		} catch (Exception e) {
		}
		
		if (sparql == null)
			sparql = appProfile.getSPARQLUpdate(ID);
		
		if (sparql == null) {
			try {
				client.close();
			} catch (IOException e) {
			
			}
			throw new SEPAProtocolException("SPARQL update not found "+ID);
		}

		Response ret = client.update(new UpdateRequest(appProfile.getUpdateMethod(ID),
				appProfile.getUpdateProtocolScheme(ID), appProfile.getUpdateHost(ID), appProfile.getUpdatePort(ID),
				appProfile.getUpdatePath(ID), appProfile.addPrefixesAndReplaceBindings(sparql, addDefaultDatatype(forced,ID,false)),
				appProfile.getUsingGraphURI(ID), appProfile.getUsingNamedGraphURI(ID), auth, timeout));
		
		
		try {
			client.close();
		} catch (IOException e) {
			throw new SEPAProtocolException(e);
		}

		return ret;
	}

	/**
	 * Query.
	 *
	 * @param ID      the identifier of the query within the JSAP
	 * @param sparql  if specified it replaces the default SPARQL in the JSAP
	 * @param forced  the forced
	 * @param timeout the timeout
	 * @return the response
	 * @throws SEPAProtocolException   the SEPA protocol exception
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws IOException             Signals that an I/O exception has occurred.
	 * @throws SEPAPropertiesException the SEPA properties exception
	 * @throws SEPABindingsException   the SEPA bindings exception
	 */
	private Response _query(String ID, String sparql, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		SPARQL11Protocol client = new SPARQL11Protocol(sm);

		if (sparql == null)
			sparql = appProfile.getSPARQLQuery(ID);

		if (sparql == null) {
			try {
				client.close();
			} catch (IOException e) {
			
			}
			throw new SEPAProtocolException("SPARQL query not found "+ID);
		}
	
		String auth = null;
		try {
			auth = sm.getAuthorizationHeader();
		} catch (Exception e) {

		}

		Response ret = client.query(new QueryRequest(appProfile.getQueryMethod(ID),
				appProfile.getQueryProtocolScheme(ID), appProfile.getQueryHost(ID), appProfile.getQueryPort(ID),
				appProfile.getQueryPath(ID), appProfile.addPrefixesAndReplaceBindings(sparql, addDefaultDatatype(forced,ID,true)),
				appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID), auth, timeout));

		try {
			client.close();
		} catch (IOException e) {
			throw new SEPAProtocolException(e);
		}

		return ret;
	}

	/**
	 * Subscribe.
	 *
	 * @param ID      the identifier of the subscribe within the JSAP
	 * @param sparql  if specified it replaces the default SPARQL in the JSAP
	 * @param forced  the forced
	 * @param handler the handler
	 * @param timeout the timeout
	 * @throws SEPAProtocolException   the SEPA protocol exception
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws IOException             Signals that an I/O exception has occurred.
	 * @throws SEPAPropertiesException the SEPA properties exception
	 * @throws URISyntaxException      the URI syntax exception
	 * @throws SEPABindingsException   the SEPA bindings exception
	 */
	private void _subscribe(String ID, String sparql, Bindings forced, ISubscriptionHandler handler, long timeout,
			String alias)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {

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

				protocol.setHandler(new Handler(url, client, handler));

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

				protocol.setHandler(new Handler(url, client, handler));
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
		
		if (sparql == null) {
			try {
				client.close();
			} catch (IOException e) {
			
			}
			throw new SEPAProtocolException("SPARQL query not found "+ID);
		}

		SubscribeRequest req = new SubscribeRequest(appProfile.addPrefixesAndReplaceBindings(sparql, addDefaultDatatype(forced,ID,true)), alias,
				appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID), auth, timeout);

		client.subscribe(req);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		for (SPARQL11SEProtocol client : activeUrls.values())
			client.close();
	}

	/**
	 * Register.
	 *
	 * @param jksFile  the jks file
	 * @param storePwd the store pwd
	 * @param keyPwd   the key pwd
	 * @param identity the identity
	 * @return the response
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws SEPAPropertiesException the SEPA properties exception
	 */
	// Registration to the Authorization Server (AS)
	public Response register(String jksFile, String storePwd, String keyPwd, String identity)
			throws SEPASecurityException, SEPAPropertiesException {
		SEPASecurityManager security = new SEPASecurityManager(jksFile, storePwd, keyPwd,
				appProfile.getAuthenticationProperties());

		Response ret = security.register(identity);

		if (ret.isRegistrationResponse()) {
			RegistrationResponse registration = (RegistrationResponse) ret;
			appProfile.getAuthenticationProperties().setCredentials(registration.getClientId(),
					registration.getClientSecret());
		}

		return ret;
	}
}

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
import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties;
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
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.RegistrationResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;

/**
 * The Class GenericClient.
 */
public final class GenericClient extends Client implements ISubscriptionHandler {
	private ISubscriptionHandler handler;

	private final long TIMEOUT = 60000;
	private final long NRETRY = 0;

	// Subscription request handling
	private Request req = null;
	private Object subLock = new Object();
	private String url = null;
	private SPARQL11SEProtocol client = null;

	/** The active urls. */
	// URL ==> client
	private Hashtable<String, SPARQL11SEProtocol> activeClients = new Hashtable<String, SPARQL11SEProtocol>();

	/** The subscriptions. */
	// SPUID ==> client
	private Hashtable<String, SPARQL11SEProtocol> subscriptions = new Hashtable<String, SPARQL11SEProtocol>();

	@Override
	public void onSemanticEvent(Notification notify) {
		if (handler != null)
			handler.onSemanticEvent(notify);
	}

	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		if (handler != null)
			handler.onBrokenConnection(errorResponse);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		if (errorResponse.isTokenExpiredError()) {
			try {
				sm.refreshToken();
			} catch (SEPAPropertiesException | SEPASecurityException e) {
				logger.error(e.getMessage());
				if (logger.isTraceEnabled())
					e.printStackTrace();
				if (handler != null)
					handler.onError(
							new ErrorResponse(401, "invalid_grant", "Failed to refresh token. " + e.getMessage()));
				return;
			}

			try {
				req.setAuthorizationHeader(sm.getAuthorizationHeader());
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
				if (logger.isTraceEnabled())
					e.printStackTrace();
				if (handler != null)
					handler.onError(new ErrorResponse(401, "invalid_grant",
							"Failed to get authorization header. " + e.getMessage()));
				return;
			}

			if (req.isSubscribeRequest()) {
				try {
					client.subscribe((SubscribeRequest) req);
				} catch (SEPAProtocolException e) {
					logger.error(e.getMessage());
					if (logger.isTraceEnabled())
						e.printStackTrace();
					if (handler != null)
						handler.onError(
								new ErrorResponse(401, "invalid_grant", "Failed to subscribe. " + e.getMessage()));
					return;
				}
			} else {
				try {
					client.unsubscribe((UnsubscribeRequest) req);
				} catch (SEPAProtocolException e) {
					logger.error(e.getMessage());
					if (logger.isTraceEnabled())
						e.printStackTrace();
					if (handler != null)
						handler.onError(
								new ErrorResponse(401, "invalid_grant", "Failed to unsubscribe. " + e.getMessage()));
					return;
				}
			}
		}

		if (handler != null)
			handler.onError(errorResponse);

	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		synchronized (subLock) {
			activeClients.put(url, client);
			subscriptions.put(spuid, client);
			req = null;
			subLock.notify();
		}

		if (handler != null)
			handler.onSubscribe(spuid, alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		synchronized (subLock) {
			subscriptions.remove(spuid);
			req = null;
			subLock.notify();
		}

		if (handler != null)
			handler.onUnsubscribe(spuid);
	}

//	/**
//	 * Instantiates a new generic client.
//	 *
//	 * @param appProfile the JSAP profile
//	 * @throws SEPAProtocolException the SEPA protocol exception
//	 */
//	public GenericClient(JSAP appProfile) throws SEPAProtocolException {
//		this(appProfile, null, null);
//	}
//
//	/**
//	 * Instantiates a new generic client.
//	 *
//	 * @param appProfile the JSAP profile
//	 * @param sm         the security manager (needed for secure connections)
//	 * @throws SEPAProtocolException the SEPA protocol exception
//	 */
//	public GenericClient(JSAP appProfile, ClientSecurityManager sm) throws SEPAProtocolException {
//		this(appProfile, sm, null);
//	}

	public GenericClient(JSAP appProfile, ClientSecurityManager sm, ISubscriptionHandler handler)
			throws SEPAProtocolException {
		super(appProfile, sm);

		this.handler = handler;
	}

	public void setHandler(ISubscriptionHandler handler) {
		this.handler = handler;
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
	public Response update(String ID, String sparql, Bindings forced, long timeout, long nRetry)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPABindingsException {
		return _update(ID, sparql, forced, timeout, nRetry);
	}

	public Response update(String ID, String sparql, Bindings forced) throws SEPAProtocolException,
			SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		return _update(ID, sparql, forced, TIMEOUT, NRETRY);
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
	public Response update(String ID, Bindings forced, long timeout, long nRetry) throws SEPAProtocolException,
			SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		return _update(ID, null, forced, timeout, nRetry);
	}

	public Response update(String ID, Bindings forced) throws SEPAProtocolException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPABindingsException {
		return _update(ID, null, forced, TIMEOUT, NRETRY);
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
	public Response query(String ID, String sparql, Bindings forced, long timeout, long nRetry)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPABindingsException {
		return _query(ID, sparql, forced, timeout, nRetry);
	}

	public Response query(String ID, String sparql, Bindings forced) throws SEPAProtocolException,
			SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		return _query(ID, sparql, forced, TIMEOUT, NRETRY);
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
	public Response query(String ID, Bindings forced, long timeout, long nRetry)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		return _query(ID, null, forced, timeout, nRetry);
	}

	public Response query(String ID, Bindings forced)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		return _query(ID, null, forced, TIMEOUT, NRETRY);
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
	 * @throws InterruptedException
	 */
	public void subscribe(String ID, String sparql, Bindings forced, String alias, long timeout, long nRetry)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException,
			InterruptedException {
		_subscribe(ID, sparql, forced, alias, timeout, nRetry);
	}

	public void subscribe(String ID, String sparql, Bindings forced, String alias) throws SEPAProtocolException,
			SEPASecurityException, SEPAPropertiesException, SEPABindingsException, InterruptedException {
		_subscribe(ID, sparql, forced, alias, TIMEOUT, NRETRY);
	}

	public void subscribe(String ID, String sparql, Bindings forced, long timeout, long nRetry)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException,
			InterruptedException {
		_subscribe(ID, sparql, forced, null, timeout, nRetry);
	}

	public void subscribe(String ID, String sparql, Bindings forced) throws SEPAProtocolException,
			SEPASecurityException, SEPAPropertiesException, SEPABindingsException, InterruptedException {
		_subscribe(ID, sparql, forced, null, TIMEOUT, NRETRY);
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
	 * @throws InterruptedException
	 */
	public void subscribe(String ID, Bindings forced, String alias, long timeout, long nRetry)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException,
			InterruptedException {
		_subscribe(ID, null, forced, alias, timeout, nRetry);
	}

	public void subscribe(String ID, Bindings forced, String alias) throws SEPAProtocolException, SEPASecurityException,
			SEPAPropertiesException, SEPABindingsException, InterruptedException {
		_subscribe(ID, null, forced, alias, TIMEOUT, NRETRY);
	}

	public void subscribe(String ID, Bindings forced, long timeout, long nRetry) throws SEPAProtocolException,
			SEPASecurityException, SEPAPropertiesException, SEPABindingsException, InterruptedException {
		_subscribe(ID, null, forced, null, timeout, nRetry);
	}

	public void subscribe(String ID, Bindings forced) throws SEPAProtocolException,
			SEPASecurityException, SEPAPropertiesException, SEPABindingsException, InterruptedException {
		_subscribe(ID, null, forced, null, TIMEOUT, NRETRY);
	}

	/**
	 * Unsubscribe.
	 *
	 * @param ID      the SPUID of the active subscription
	 * @param timeout the timeout
	 * @throws SEPASecurityException   the SEPA security exception
	 * @throws SEPAPropertiesException the SEPA properties exception
	 * @throws SEPAProtocolException   the SEPA protocol exception
	 * @throws InterruptedException
	 */
	public void unsubscribe(String subID)
			throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		unsubscribe(subID,TIMEOUT,NRETRY);
	}
	public void unsubscribe(String subID, long timeout, long nRetry)
			throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		if (!subscriptions.containsKey(subID))
			return;

		String auth = null;
		try {
			if (sm != null) {
				auth = sm.getAuthorizationHeader();
			}
		} catch (Exception e) {
		}

		synchronized (subLock) {
			if (req != null)
				subLock.wait();

			req = new UnsubscribeRequest(subID, auth, timeout, nRetry);

			subscriptions.get(subID).unsubscribe((UnsubscribeRequest) req);
		}
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
	private Response _update(String ID, String sparql, Bindings forced, long timeout, long nRetry)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		SPARQL11Protocol client = new SPARQL11Protocol(sm);

		String auth = null;
		try {
			if (sm != null)
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
			throw new SEPAProtocolException("SPARQL update not found " + ID);
		}

		Response ret = client
				.update(new UpdateRequest(appProfile.getUpdateMethod(ID), appProfile.getUpdateProtocolScheme(ID),
						appProfile.getUpdateHost(ID), appProfile.getUpdatePort(ID), appProfile.getUpdatePath(ID),
						appProfile.addPrefixesAndReplaceBindings(sparql, addDefaultDatatype(forced, ID, false)),
						appProfile.getUsingGraphURI(ID), appProfile.getUsingNamedGraphURI(ID), auth, timeout, nRetry));

		while (isSecure() && ret.isError()) {
			ErrorResponse errorResponse = (ErrorResponse) ret;

			if (errorResponse.isTokenExpiredError()) {
				try {
					sm.refreshToken();
				} catch (SEPAPropertiesException | SEPASecurityException e) {
					logger.error("Failed to refresh token: " + e.getMessage());
				}
			} else {
				logger.error("Failed to refresh token: " + errorResponse);
				break;
			}

			auth = sm.getAuthorizationHeader();

			ret = client.update(new UpdateRequest(appProfile.getUpdateMethod(ID),
					appProfile.getUpdateProtocolScheme(ID), appProfile.getUpdateHost(ID), appProfile.getUpdatePort(ID),
					appProfile.getUpdatePath(ID),
					appProfile.addPrefixesAndReplaceBindings(sparql, addDefaultDatatype(forced, ID, false)),
					appProfile.getUsingGraphURI(ID), appProfile.getUsingNamedGraphURI(ID), auth, timeout, nRetry));
		}

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
	private Response _query(String ID, String sparql, Bindings forced, long timeout, long nRetry)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		SPARQL11Protocol client = new SPARQL11Protocol(sm);

		if (sparql == null)
			sparql = appProfile.getSPARQLQuery(ID);

		if (sparql == null) {
			try {
				client.close();
			} catch (IOException e) {

			}
			throw new SEPAProtocolException("SPARQL query not found " + ID);
		}

		String auth = null;
		try {
			if (sm != null)
				auth = sm.getAuthorizationHeader();
		} catch (Exception e) {

		}

		Response ret = client
				.query(new QueryRequest(appProfile.getQueryMethod(ID), appProfile.getQueryProtocolScheme(ID),
						appProfile.getQueryHost(ID), appProfile.getQueryPort(ID), appProfile.getQueryPath(ID),
						appProfile.addPrefixesAndReplaceBindings(sparql, addDefaultDatatype(forced, ID, true)),
						appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID), auth, timeout, nRetry));

		while (isSecure() && ret.isError()) {
			ErrorResponse errorResponse = (ErrorResponse) ret;

			if (errorResponse.isTokenExpiredError()) {
				try {
					sm.refreshToken();
				} catch (SEPAPropertiesException | SEPASecurityException e) {
					logger.error("Failed to refresh token: " + e.getMessage());
				}
			} else {
				logger.error("Failed to refresh token: " + errorResponse);
				break;
			}

			auth = sm.getAuthorizationHeader();

			ret = client.query(new QueryRequest(appProfile.getQueryMethod(ID), appProfile.getQueryProtocolScheme(ID),
					appProfile.getQueryHost(ID), appProfile.getQueryPort(ID), appProfile.getQueryPath(ID),
					appProfile.addPrefixesAndReplaceBindings(sparql, addDefaultDatatype(forced, ID, true)),
					appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID), auth, timeout, nRetry));

		}

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
	 * @throws InterruptedException
	 */
	private void _subscribe(String ID, String sparql, Bindings forced, String alias, long timeout, long nRretry)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException,
			InterruptedException {

		if (sparql == null)
			sparql = appProfile.getSPARQLQuery(ID);

		if (sparql == null)
			throw new SEPAProtocolException("SPARQL query not found " + ID);

		synchronized (subLock) {
			if (req != null)
				subLock.wait();

			if (appProfile.getSubscribeProtocol(ID).equals(SPARQL11SEProperties.SubscriptionProtocol.WS)) {
				url = "ws_" + appProfile.getSubscribeHost(ID) + "_" + appProfile.getSubscribePort(ID) + "_"
						+ appProfile.getSubscribePath(ID);
			} else {
				url = "wss_" + appProfile.getSubscribeHost(ID) + "_" + appProfile.getSubscribePort(ID) + "_"
						+ appProfile.getSubscribePath(ID);
			}

			if (activeClients.containsKey(url)) {
				client = activeClients.get(url);
			} else {
				SubscriptionProtocol protocol = new WebsocketSubscriptionProtocol(appProfile.getSubscribeHost(ID),
						appProfile.getSubscribePort(ID), appProfile.getSubscribePath(ID), this, sm);
				client = new SPARQL11SEProtocol(protocol);
			}

			String auth = null;
			if (sm != null)
				auth = sm.getAuthorizationHeader();

			req = new SubscribeRequest(
					appProfile.addPrefixesAndReplaceBindings(sparql, addDefaultDatatype(forced, ID, true)), alias,
					appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID), auth, timeout, nRretry);

			client.subscribe((SubscribeRequest) req);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		for (SPARQL11SEProtocol client : activeClients.values())
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
	public Response register(String jksFile, String storePwd, String identity)
			throws SEPASecurityException, SEPAPropertiesException {
		ClientSecurityManager security;
		if (appProfile.getAuthenticationProperties().trustAll())
			security = new ClientSecurityManager(appProfile.getAuthenticationProperties());
		else
			security = new ClientSecurityManager(appProfile.getAuthenticationProperties(), jksFile, storePwd);

		Response ret = security.register(identity);

		if (ret.isRegistrationResponse()) {
			RegistrationResponse registration = (RegistrationResponse) ret;
			appProfile.getAuthenticationProperties().setCredentials(registration.getClientId(),
					registration.getClientSecret());
		}

		return ret;
	}

}

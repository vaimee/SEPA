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

import java.util.Hashtable;

import org.apache.http.HttpStatus;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.ISubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.protocol.websocket.WebSocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.RegistrationResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class GenericClient extends Client {

	private Hashtable<String, SPARQL11SEProtocol> subscribedClients = new Hashtable<String, SPARQL11SEProtocol>();
	private Hashtable<String, SPARQL11SEProtocol> subscriptions = new Hashtable<String, SPARQL11SEProtocol>();

	public GenericClient(JSAP appProfile) throws SEPAProtocolException, SEPASecurityException {
		super(appProfile);
	}

	public void changeProfile(JSAP appProfile) throws SEPAProtocolException {
		if (appProfile == null) {
			logger.fatal("Application profile is null. Client cannot be initialized");
			throw new SEPAProtocolException(new IllegalArgumentException("Application profile is null"));
		}
		this.appProfile = appProfile;

		logger.debug("SEPA parameters: " + appProfile.printParameters());

		addNamespaces(appProfile);
	}

	public Response update(String ID, String sparql, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException {
		SPARQL11Protocol client;

		if (appProfile.getUpdateProtocolScheme(ID).equals("https")) {
			client = new SPARQL11Protocol("sepa.jks", "sepa2017", "sepa2017");
			if(!getToken()) {
				client.close();
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"Failed to get or renew token");
			}
		}
		else client = new SPARQL11Protocol();

		Response ret = client.update(new UpdateRequest(appProfile.getUpdateMethod(ID),
				appProfile.getUpdateProtocolScheme(ID), appProfile.getUpdateHost(ID), appProfile.getUpdatePort(ID),
				appProfile.getUpdatePath(ID), prefixes() + replaceBindings(sparql, forced),
				timeout, appProfile.getUsingGraphURI(ID), appProfile.getUsingNamedGraphURI(ID),
				appProfile.getAuthenticationProperties().getBearerAuthorizationHeader()));
		client.close();

		return ret;
	}
	
	public Response update(String ID, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException {
		SPARQL11Protocol client;

		if (appProfile.getUpdateProtocolScheme(ID).equals("https")) {
			client = new SPARQL11Protocol("sepa.jks", "sepa2017", "sepa2017");
			if(!getToken()) {
				client.close();
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"Failed to get or renew token");
			}
		}
		else client = new SPARQL11Protocol();

		Response ret = client.update(new UpdateRequest(appProfile.getUpdateMethod(ID),
				appProfile.getUpdateProtocolScheme(ID), appProfile.getUpdateHost(ID), appProfile.getUpdatePort(ID),
				appProfile.getUpdatePath(ID), prefixes() + replaceBindings(appProfile.getSPARQLUpdate(ID), forced),
				timeout, appProfile.getUsingGraphURI(ID), appProfile.getUsingNamedGraphURI(ID),
				appProfile.getAuthenticationProperties().getBearerAuthorizationHeader()));
		client.close();

		return ret;
	}

	public Response query(String ID, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException {
		SPARQL11Protocol client;

		if (appProfile.getQueryProtocolScheme(ID).equals("https")) {
			client = new SPARQL11Protocol("sepa.jks", "sepa2017", "sepa2017");
			if(!getToken()) {
				client.close();
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"Failed to get or renew token");
			}
		}
		else client = new SPARQL11Protocol();
		
		Response ret = client.query(new QueryRequest(appProfile.getQueryMethod(ID),
				appProfile.getQueryProtocolScheme(ID), appProfile.getQueryHost(ID), appProfile.getQueryPort(ID),
				appProfile.getQueryPath(ID), prefixes() + replaceBindings(appProfile.getSPARQLQuery(ID), forced),
				timeout, appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID),
				appProfile.getAuthenticationProperties().getBearerAuthorizationHeader()));
		client.close();

		return ret;
	}

	public Response query(String ID, String sparql, Bindings forced, int timeout)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException {
		SPARQL11Protocol client;

		if (appProfile.getQueryProtocolScheme(ID).equals("https")) {
			client = new SPARQL11Protocol("sepa.jks", "sepa2017", "sepa2017");
			if(!getToken()) {
				client.close();
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"Failed to get or renew token");
			}
		}
		else client = new SPARQL11Protocol();
		
		Response ret = client.query(new QueryRequest(appProfile.getQueryMethod(ID),
				appProfile.getQueryProtocolScheme(ID), appProfile.getQueryHost(ID), appProfile.getQueryPort(ID),
				appProfile.getQueryPath(ID), prefixes() + replaceBindings(sparql, forced),
				timeout, appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID),
				appProfile.getAuthenticationProperties().getBearerAuthorizationHeader()));
		client.close();

		return ret;
	}
	
	public Response subscribe(String ID, String sparql, Bindings forced, ISubscriptionHandler handler)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException {

		// Create client
		String url;
		if (appProfile.getSubscribeProtocol(ID).equals(SubscriptionProtocol.WS)) {
			url = "ws_" + appProfile.getSubscribeHost(ID) + "_" + appProfile.getSubscribePort(ID) + "_"
					+ appProfile.getSubscribePath(ID);
		} else {
			url = "wss_" + appProfile.getSubscribeHost(ID) + "_" + appProfile.getSubscribePort(ID) + "_"
					+ appProfile.getSubscribePath(ID);
		}
		if (!subscribedClients.containsKey(url)) {
			ISubscriptionProtocol protocol = null;
			switch (appProfile.getSubscribeProtocol(ID)) {
			case WS:
				protocol = new WebSocketSubscriptionProtocol(appProfile.getSubscribeHost(ID),
						appProfile.getSubscribePort(ID), appProfile.getSubscribePath(ID), false);
				break;
			case WSS:
				protocol = new WebSocketSubscriptionProtocol(appProfile.getSubscribeHost(ID),
						appProfile.getSubscribePort(ID), appProfile.getSubscribePath(ID), true);
				
				if(!getToken()) return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"Failed to get or renew token");
				
				break;
			}

			subscribedClients.put(url, new SPARQL11SEProtocol(protocol, handler));
		}

		// Send request
		SubscribeRequest req = new SubscribeRequest(prefixes() + replaceBindings(sparql, forced),
				null, appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID),
				appProfile.getAuthenticationProperties().getBearerAuthorizationHeader());
		
		Response ret = subscribedClients.get(url).subscribe(req);

		// Parse response
		if (ret.isSubscribeResponse())
			subscriptions.put(((SubscribeResponse) ret).getSpuid(), subscribedClients.get(url));

		return ret;
	}
	
	public Response subscribe(String ID, Bindings forced, ISubscriptionHandler handler)
			throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException {

		// Create client
		String url;
		if (appProfile.getSubscribeProtocol(ID).equals(SubscriptionProtocol.WS)) {
			url = "ws_" + appProfile.getSubscribeHost(ID) + "_" + appProfile.getSubscribePort(ID) + "_"
					+ appProfile.getSubscribePath(ID);
		} else {
			url = "wss_" + appProfile.getSubscribeHost(ID) + "_" + appProfile.getSubscribePort(ID) + "_"
					+ appProfile.getSubscribePath(ID);
		}
		if (!subscribedClients.containsKey(url)) {
			ISubscriptionProtocol protocol = null;
			switch (appProfile.getSubscribeProtocol(ID)) {
			case WS:
				protocol = new WebSocketSubscriptionProtocol(appProfile.getSubscribeHost(ID),
						appProfile.getSubscribePort(ID), appProfile.getSubscribePath(ID), false);
				break;
			case WSS:
				protocol = new WebSocketSubscriptionProtocol(appProfile.getSubscribeHost(ID),
						appProfile.getSubscribePort(ID), appProfile.getSubscribePath(ID), true);
				
				if(!getToken()) return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"Failed to get or renew token");
				
				break;
			}

			subscribedClients.put(url, new SPARQL11SEProtocol(protocol, handler));
		}

		// Send request
		SubscribeRequest req = new SubscribeRequest(prefixes() + replaceBindings(appProfile.getSPARQLQuery(ID), forced),
				null, appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID),
				appProfile.getAuthenticationProperties().getBearerAuthorizationHeader());
		
		Response ret = subscribedClients.get(url).subscribe(req);

		// Parse response
		if (ret.isSubscribeResponse())
			subscriptions.put(((SubscribeResponse) ret).getSpuid(), subscribedClients.get(url));

		return ret;
	}

	public Response unsubscribe(String subID) throws SEPASecurityException, IOException, SEPAPropertiesException {
		if (!subscriptions.containsKey(subID))
			return new ErrorResponse(HttpStatus.SC_BAD_REQUEST, subID + " not present");

		if (subscriptions.get(subID).isSecure()) {
			if(!getToken()) return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"Failed to get or renew token");
		}
		
		return subscriptions.get(subID).unsubscribe(
				new UnsubscribeRequest(subID, appProfile.getAuthenticationProperties().getBearerAuthorizationHeader()));
	}

	@Override
	public void close() throws IOException {
		for (SPARQL11SEProtocol client : subscribedClients.values())
			client.close();
	}

	// Registration to the Authorization Server (AS)
	public Response register(String identity) throws SEPASecurityException, SEPAPropertiesException {
		SEPASecurityManager security = new SEPASecurityManager();

		Response ret = security.register(appProfile.getAuthenticationProperties().getRegisterUrl(), identity);

		if (ret.isRegistrationResponse()) {
			RegistrationResponse registration = (RegistrationResponse) ret;
			appProfile.getAuthenticationProperties().setCredentials(registration.getClientId(),
					registration.getClientSecret());
		}

		return ret;
	}
}

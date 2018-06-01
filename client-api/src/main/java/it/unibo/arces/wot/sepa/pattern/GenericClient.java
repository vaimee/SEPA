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

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.ISubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.protocol.websocket.WebSocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;

public class GenericClient extends Client {

	private Hashtable<String, SPARQL11SEProtocol> subscribedClients = new Hashtable<String, SPARQL11SEProtocol>();
	private Hashtable<String, SPARQL11SEProtocol> subscriptions = new Hashtable<String, SPARQL11SEProtocol>();

	public GenericClient(ApplicationProfile appProfile) throws SEPAProtocolException, SEPASecurityException {
		super(appProfile);
	}

	public Response update(String ID, String SPARQL_UPDATE, Bindings forced, String usingGraphUri,
			String usingNamedGraphUri, HTTPMethod method, int timeout)
			throws SEPAProtocolException, SEPASecurityException, IOException {

		UpdateRequest req = new UpdateRequest(-1,method, appProfile.getUpdateProtocolScheme(ID),
				appProfile.getUpdateHost(ID), appProfile.getUpdatePort(ID), appProfile.getUpdatePath(ID),
				prefixes() + replaceBindings(SPARQL_UPDATE, forced), timeout, usingGraphUri, usingNamedGraphUri);

		SPARQL11Protocol client = new SPARQL11Protocol();
		Response ret = client.update(req);
		client.close();

		return ret;
	}

	public Response query(String ID, String SPARQL_QUERY, Bindings forced, String defaultGraphUri, String namedGraphUri,
			HTTPMethod method, int timeout) throws SEPAProtocolException, SEPASecurityException, IOException {

		QueryRequest req = new QueryRequest(-1,method, appProfile.getQueryProtocolScheme(ID), appProfile.getQueryHost(ID),
				appProfile.getQueryPort(ID), appProfile.getQueryPath(ID),
				prefixes() + replaceBindings(SPARQL_QUERY, forced), timeout, defaultGraphUri, namedGraphUri);

		SPARQL11Protocol client = new SPARQL11Protocol();
		Response ret = client.query(req);
		client.close();

		return ret;
	}

	public Response subscribe(String ID, String SPARQL_SUBSCRIBE, Bindings forced, String defaultGraphUri,
			String namedGraphUri, ISubscriptionHandler handler) throws SEPAProtocolException, SEPASecurityException {

		SubscribeRequest req = new SubscribeRequest(prefixes() + replaceBindings(SPARQL_SUBSCRIBE, forced));

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
				break;
			}
			
			subscribedClients.put(url,new SPARQL11SEProtocol(appProfile,protocol, handler));
		}

		Response ret = subscribedClients.get(url).subscribe(req);

		if (ret.isSubscribeResponse())
			subscriptions.put(((SubscribeResponse) ret).getSpuid(), subscribedClients.get(url));

		return ret;
	}

	public Response unsubscribe(String subID) {
		if (!subscriptions.contains(subID))
			return new ErrorResponse(400, subID + " not present");

		return subscriptions.get(subID).unsubscribe(new UnsubscribeRequest(subID));
	}

	@Override
	public void close() throws IOException {
		for (SPARQL11SEProtocol client : subscribedClients.values())
			client.close();
	}

//	public Response secureUpdate(String SPARQL_UPDATE, Bindings forced) {
//		return protocolClient.secureUpdate(new UpdateRequest(prefixes() + replaceBindings(SPARQL_UPDATE, forced)));
//	}
//
//	public Response query(String SPARQL_QUERY, Bindings forced) {
//		return protocolClient.query(new QueryRequest(prefixes() + replaceBindings(SPARQL_QUERY, forced)));
//	}
//
//	public Response secureQuery(String SPARQL_QUERY, Bindings forced) {
//		return protocolClient.secureQuery(new QueryRequest(prefixes() + replaceBindings(SPARQL_QUERY, forced)));
//	}
//
//	public Response secureSubscribe(String SPARQL_SUBSCRIBE, Bindings forced) {
//		return protocolClient
//				.secureSubscribe(new SubscribeRequest(prefixes() + replaceBindings(SPARQL_SUBSCRIBE, forced)));
//	}
//
//	public Response secureUnsubscribe(String subID) {
//		return protocolClient.secureUnsubscribe(new UnsubscribeRequest(subID));
//	}

//	// Registration to the Authorization Server (AS)
//	public Response register(String identity) {
//		SPARQL11SEProtocol client = new SPARQL11SEProtocol();
//		Response ret = client.register(identity);
//		try {
//			client.close();
//		} catch (IOException e) {
//			logger.warn(e.getMessage());
//		}
//		return ret;
//	}
//
//	// Token request to the Authorization Server (AS)
//	public Response requestToken() {
//		SPARQL11SEProtocol client = new SPARQL11SEProtocol();
//		Response ret = client.requestToken();
//		try {
//			client.close();
//		} catch (IOException e) {
//			logger.warn(e.getMessage());
//		}
//		return ret;
//	}
//
//	// Retrieve the token expiring seconds
//	public long getTokenExpiringSeconds() throws SEPASecurityException {
//		SPARQL11SEProtocol client = new SPARQL11SEProtocol();
//		long ret = client.getTokenExpiringSeconds();
//		try {
//			client.close();
//		} catch (IOException e) {
//			logger.warn(e.getMessage());
//		}
//		return ret;
//	}

}

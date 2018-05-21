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

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class GenericClient extends Client {

	public GenericClient(ApplicationProfile appProfile,ISubscriptionHandler handler) throws SEPAProtocolException, SEPASecurityException {
		super(appProfile);
		protocolClient = new SPARQL11SEProtocol(appProfile, handler);
	}

	public Response update(String ID,String SPARQL_UPDATE, Bindings forced,String usingGraphUri,String usingNamedGraphUri) throws SEPAProtocolException, SEPASecurityException {
		protocolClient = new SPARQL11SEProtocol(appProfile,ID,true);
		// TODO: move default timeout in jsap
		UpdateRequest req = new UpdateRequest(prefixes() + replaceBindings(SPARQL_UPDATE, forced));
		// Graphs
		req.setUsingGraphUri(usingGraphUri);
		req.setNamedGraphUri(usingNamedGraphUri);
		// Authentication
		if (appProfile.isAuthenticationRequiredForUpdate(SPARQL_UPDATE)) {
			
		}
		return protocolClient.update(req,5000);
	}
	
	public Response update(String ID,String SPARQL_UPDATE, Bindings forced,int timeout) throws SEPAProtocolException, SEPASecurityException {
		protocolClient = new SPARQL11SEProtocol(appProfile,ID,true);
		return protocolClient.update(new UpdateRequest(prefixes() + replaceBindings(SPARQL_UPDATE, forced)),timeout);
	}
	
	public Response update(String SPARQL_UPDATE, Bindings forced,int timeout) {
		return protocolClient.update(new UpdateRequest(prefixes() + replaceBindings(SPARQL_UPDATE, forced)),timeout);
	}
	public Response update(String SPARQL_UPDATE, Bindings forced) {
		// TODO: move default timeout in jsap
		return protocolClient.update(new UpdateRequest(prefixes() + replaceBindings(SPARQL_UPDATE, forced)),5000);
	}

	public Response secureUpdate(String SPARQL_UPDATE, Bindings forced) {
		return protocolClient.secureUpdate(new UpdateRequest(prefixes() + replaceBindings(SPARQL_UPDATE, forced)));
	}

	public Response query(String SPARQL_QUERY, Bindings forced) {
		return protocolClient.query(new QueryRequest(prefixes() + replaceBindings(SPARQL_QUERY, forced)));
	}

	public Response secureQuery(String SPARQL_QUERY, Bindings forced) {
		return protocolClient.secureQuery(new QueryRequest(prefixes() + replaceBindings(SPARQL_QUERY, forced)));
	}

	public Response subscribe(String SPARQL_SUBSCRIBE, Bindings forced) {
		return protocolClient.subscribe(new SubscribeRequest(prefixes() + replaceBindings(SPARQL_SUBSCRIBE, forced)));
	}

	public Response secureSubscribe(String SPARQL_SUBSCRIBE, Bindings forced) {
		return protocolClient
				.secureSubscribe(new SubscribeRequest(prefixes() + replaceBindings(SPARQL_SUBSCRIBE, forced)));
	}

	public Response unsubscribe(String subID) {
		return protocolClient.unsubscribe(new UnsubscribeRequest(subID));
	}

	public Response secureUnsubscribe(String subID) {
		return protocolClient.secureUnsubscribe(new UnsubscribeRequest(subID));
	}

	// Registration to the Authorization Server (AS)
	public Response register(String identity) {
		return protocolClient.register(identity);
	}

	// Token request to the Authorization Server (AS)
	public Response requestToken() {
		return protocolClient.requestToken();
	}
	
	// Retrieve the token expiring seconds
	public long getTokenExpiringSeconds() throws SEPASecurityException {
		return protocolClient.getTokenExpiringSeconds();
	}
}

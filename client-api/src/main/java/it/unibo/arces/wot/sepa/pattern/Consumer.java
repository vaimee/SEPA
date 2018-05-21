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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;

public abstract class Consumer extends Client implements IConsumer {
	private static final Logger logger = LogManager.getLogger("Consumer");
	
	protected String sparqlSubscribe = null;
	protected String subID = "";
	
	public Consumer(ApplicationProfile appProfile, String subscribeID) throws SEPAProtocolException, SEPASecurityException {
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
		
		protocolClient = new SPARQL11SEProtocol(appProfile,this);
	}

	public final Response subscribe(Bindings forcedBindings){
		if (sparqlSubscribe == null) {
			logger.fatal("SPARQL SUBSCRIBE not defined");
			return null;
		}

		if (protocolClient == null) {
			logger.fatal("Client not initialized");
			return null;
		}

		String sparql = prefixes() + replaceBindings(sparqlSubscribe, forcedBindings);

		logger.debug("<SUBSCRIBE> ==> " + sparql);

		Response response = protocolClient.subscribe(new SubscribeRequest(sparql));
		
		if (response.isSubscribeResponse()) {
			subID = ((SubscribeResponse)response).getSpuid();	
		}
		
		return response;
	}

	public final Response unsubscribe() {
		logger.debug("UNSUBSCRIBE " + subID);

		if (protocolClient == null) {
			logger.fatal("Client not initialized");
			return new ErrorResponse(-1,400,"Client not initialized");
		}

		return protocolClient.unsubscribe(new UnsubscribeRequest(subID));
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

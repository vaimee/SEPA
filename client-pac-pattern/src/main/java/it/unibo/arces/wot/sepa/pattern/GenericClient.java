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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.api.INotificationHandler;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;

public abstract class GenericClient extends Client implements INotificationHandler,IGenericClient {	
	
	public GenericClient(ApplicationProfile appProfile)
			throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
		super(appProfile);
		protocolClient.setNotificationHandler(this);
	}
	
	public Response update(String SPARQL_UPDATE,Bindings forced) {
		return protocolClient.update(new UpdateRequest(prefixes() + replaceBindings(SPARQL_UPDATE,forced)));	
	 }
	
	public Response query(String SPARQL_QUERY,Bindings forced) {	
		return protocolClient.query(new QueryRequest(prefixes() + replaceBindings(SPARQL_QUERY,forced)));
	}
	
	public Response subscribe(String SPARQL_SUBSCRIBE,Bindings forced) {	
		try {
			return protocolClient.subscribe(new SubscribeRequest(prefixes() + replaceBindings(SPARQL_SUBSCRIBE,forced)));
		} catch (InvalidKeyException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | KeyStoreException
				| CertificateException | IOException | URISyntaxException | InterruptedException e) {
			return new ErrorResponse(500,e.getMessage());
		}
		
	}
	 
	public Response unsubscribe(String subID)  {
		try {
			return protocolClient.unsubscribe(new UnsubscribeRequest(subID));
		} catch (InvalidKeyException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | KeyStoreException
				| CertificateException | IOException | URISyntaxException | InterruptedException e) {
			return new ErrorResponse(500,e.getMessage());
		}
	}
	
	@Override
	public void onSemanticEvent(Notification notify) {
		ARBindingsResults results = notify.getARBindingsResults();

		BindingsResults added = results.getAddedBindings();
		BindingsResults removed = results.getRemovedBindings();

		// Replace prefixes
		for (Bindings bindings : added.getBindings()) {
			for (String var : bindings.getVariables()) {
				if (bindings.isURI(var)) {
					for (String prefix : URI2PrefixMap.keySet())
						if (bindings.getBindingValue(var).startsWith(prefix)) {
							bindings.addBinding(var, new RDFTermURI(bindings.getBindingValue(var).replace(prefix,
									URI2PrefixMap.get(prefix) + ":")));
							break;
						}
				}
			}
		}
		for (Bindings bindings : removed.getBindings()) {
			for (String var : bindings.getVariables()) {
				if (bindings.isURI(var)) {
					for (String prefix : URI2PrefixMap.keySet())
						if (bindings.getBindingValue(var).startsWith(prefix)) {
							bindings.addBinding(var, new RDFTermURI(bindings.getBindingValue(var).replace(prefix,
									URI2PrefixMap.get(prefix) + ":")));
							break;
						}
				}
			}
		}

		// Dispatch different notifications based on notify content
		if (!added.isEmpty())
			onAddedResults(added);
		if (!removed.isEmpty())
			onRemovedResults(removed);
		onResults(results);
		
	}
	
	@Override
	public void onPing() {
		onKeepAlive();
	}

	@Override
	public void onBrokenSocket() {
		onBrokenSubscription();
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		onSubscriptionError(errorResponse);
	}
}

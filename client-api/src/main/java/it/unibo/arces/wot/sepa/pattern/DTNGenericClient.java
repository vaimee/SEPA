package it.unibo.arces.wot.sepa.pattern;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Hashtable;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.dtn.DTNSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.DTNProtocol;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.dtn.JAL.BundleEID;
import it.unibo.dtn.JAL.BundleEIDDTNScheme;
import it.unibo.dtn.JAL.BundleEIDIPNScheme;
import it.unibo.dtn.JAL.exceptions.JALIPNParametersException;
import it.unibo.dtn.JAL.exceptions.JALLocalEIDException;
import it.unibo.dtn.JAL.exceptions.JALOpenException;
import it.unibo.dtn.JAL.exceptions.JALRegisterException;

public class DTNGenericClient extends GenericClient {

	class Handler implements ISubscriptionHandler {
		private BundleEID _destination;
		private DTNSubscriptionProtocol _client;
		private ISubscriptionHandler _handler;
		
		public Handler(BundleEID destination, DTNSubscriptionProtocol client, ISubscriptionHandler handler) {
			this._destination = destination;
			this._client = client;
			this._handler = handler;
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
			activeDestinations.put(_destination, _client);
			subscriptions.put(spuid, _client);
			if (_handler != null) _handler.onSubscribe(spuid,alias);
		}

		@Override
		public void onUnsubscribe(String spuid) {
			if (_handler != null) _handler.onUnsubscribe(spuid);
			subscriptions.remove(spuid);
		}
		
	}

	private static final long DEFAULTTIMEOUTUNSUBSCRIBE = Integer.MAX_VALUE; // XXX check if is valid
	
	// Destination ==> client
	private Hashtable<BundleEID, DTNSubscriptionProtocol> activeDestinations = new Hashtable<>();
	
	// SUBID ==> client
	private Hashtable<String, DTNSubscriptionProtocol> subscriptions = new Hashtable<>();
	
	public DTNGenericClient(JSAP appProfile) throws SEPAProtocolException {
		this(appProfile, null);
	}
	
	public DTNGenericClient(JSAP appProfile, SEPASecurityManager sm) throws SEPAProtocolException {
		super(appProfile, sm);
	}

	public Response update(String ID, String sparql, Bindings forced, int timeout) throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		try {
			return _update(ID, sparql, forced, timeout);
		} catch (JALRegisterException e) {
			throw new SEPAPropertiesException(e);
		}
	}

	public Response update(String ID, Bindings forced, int timeout) throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		return update(ID, null, forced, timeout);
	}
	
	public Response query(String ID, String sparql, Bindings forced, int timeout) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		try {
			return _query(ID, sparql, forced, timeout);
		} catch (IOException | JALRegisterException e) {
			throw new SEPAPropertiesException(e);
		}
	}

	public Response query(String ID, Bindings forced, int timeout) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		return query(ID, null, forced, timeout);
	}
	
	public void subscribe(String ID, String sparql, Bindings forced, ISubscriptionHandler handler, long timeout) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		try {
			_subscribe(ID, sparql, forced, handler,timeout);
		} catch (IOException | URISyntaxException | JALRegisterException e) {
			throw new SEPAProtocolException(e);
		}
	}

	public void subscribe(String ID, Bindings forced, ISubscriptionHandler handler, long timeout) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		subscribe(ID, null, forced, handler,timeout);
	}
	
	public void unsubscribe(String subID,long timeout) throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		if (!subscriptions.containsKey(subID)) return;

		subscriptions.get(subID).unsubscribe(new UnsubscribeRequest(subID, null, timeout));
	}
	
	@Override
	public void close() throws IOException {
		for (DTNSubscriptionProtocol client : activeDestinations.values()) {
			String SUBID = getSUBID(client);
			if (SUBID != null) {
				try {
					client.unsubscribe(new UnsubscribeRequest(SUBID, null, DEFAULTTIMEOUTUNSUBSCRIBE));
				} catch (Throwable e) {}
			}
			client.close();
		}
	}

	/***** PRIVATE FUNCTIONS *****/
	
	private String getSUBID(DTNSubscriptionProtocol client) {
		for (String SUBID : subscriptions.keySet()) {
			if (client.equals(subscriptions.get(SUBID)))
				return SUBID;
		}
		return null;
	}
	
	/**
	 * @throws JALRegisterException 
	 * @throws JALIPNParametersException 
	 * @throws JALOpenException 
	 * @throws JALLocalEIDException 
	 * 
	 */
	private Response _update(String ID, String sparql, Bindings forced, int timeout) throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException, JALLocalEIDException, JALOpenException, JALIPNParametersException, JALRegisterException {
		DTNProtocol client = new DTNProtocol();

		if (sparql == null) {
			sparql = appProfile.getSPARQLUpdate(ID);
		}
		Response ret = client.update(new UpdateRequest(HTTPMethod.POST,
				appProfile.getUpdateProtocolSchemeDTN(ID), appProfile.getUpdateDestinationDTN(ID), appProfile.getUpdateDemuxIPN(ID),
				appProfile.getUpdateDemuxDTN(ID), appProfile.addPrefixesAndReplaceBindings(sparql, addDefaultDatatype(forced,ID,false)),
				appProfile.getUsingGraphURI(ID), appProfile.getUsingNamedGraphURI(ID), null, timeout));
		client.close();

		return ret;
	}
	
	private Response _query(String ID, String sparql, Bindings forced, int timeout) throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException, JALLocalEIDException, JALOpenException, JALIPNParametersException, JALRegisterException {
		DTNProtocol client = new DTNProtocol();

		if (sparql == null) {
			sparql = appProfile.getSPARQLQuery(ID);
		}
		Response ret = client.query(new QueryRequest(HTTPMethod.POST,
				appProfile.getQueryProtocolSchemeDTN(ID), appProfile.getQueryDestinationDTN(ID), appProfile.getQueryDemuxIPN(ID),
				appProfile.getQueryDemuxDTN(ID), appProfile.addPrefixesAndReplaceBindings(sparql, addDefaultDatatype(forced,ID,false)),
				appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID), null, timeout));
		client.close();

		return ret;
	}
	
	private void _subscribe(String ID, String sparql, Bindings forced, ISubscriptionHandler handler,long timeout) throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, URISyntaxException, SEPABindingsException, JALLocalEIDException, JALOpenException, JALIPNParametersException, JALRegisterException {
		BundleEID destination = null;
		
		String schema = appProfile.getQueryProtocolSchemeDTN(ID);
		if (schema.equals("ipn")) {
			try {
				destination = new BundleEIDIPNScheme(Integer.parseInt(appProfile.getQueryDestinationDTN(ID)), appProfile.getSubscribeDemuxIPN((ID)));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Error on host. Must be an integer");
			}
		} else if (schema.equals("dtn")) {
			destination = new BundleEIDDTNScheme(appProfile.getQueryDestinationDTN(ID), appProfile.getSubscribeDemuxDTN(ID));
		} else {
			throw new IllegalArgumentException("No schema found for DTN protocol.");
		}
		
		final DTNSubscriptionProtocol client;

		if (!this.activeDestinations.containsKey(destination)) {
			client = new DTNSubscriptionProtocol(destination);
			client.setHandler(new Handler(destination, client, handler));
		} else {
			client = this.activeDestinations.get(destination);
		}

		// Send request
		if (sparql == null) {
			sparql = appProfile.getSPARQLQuery(ID);
		}

		SubscribeRequest req = new SubscribeRequest(appProfile.addPrefixesAndReplaceBindings(sparql, addDefaultDatatype(forced,ID,false)), null,
				appProfile.getDefaultGraphURI(ID), appProfile.getNamedGraphURI(ID), null, timeout);

		client.subscribe(req);
	}

}

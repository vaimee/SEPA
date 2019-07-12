package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.pattern.Consumer;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class MqttAdapterPool extends Consumer {
	private static final Logger logger = LogManager.getLogger();

	private HashMap<String,MqttAdapter> adapters = new HashMap<String,MqttAdapter>();

	public MqttAdapterPool(JSAP appProfile, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException, SEPABindingsException {
		super(appProfile, "MQTT_BROKERS", sm);
		
		subscribe(5000);
	}
	
	public void close() throws IOException {
		super.close();
		
		for (MqttAdapter adapter : adapters.values())
			try {
				adapter.close();
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
	}
	
	@Override
	public void onResults(ARBindingsResults results) {
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			String url = bindings.getValue("url");
			int port = Integer.parseInt(bindings.getValue("port"));
			
			String user = bindings.getValue("user");
			if (user.equals("")) user = null;
			String password = bindings.getValue("password");
			if (password.equals("")) password = null;
			
			String clientId = bindings.getValue("clientId");
			if (clientId.equals("")) clientId = null;
			
			String sslProtocol = bindings.getValue("sslProtocol");
			if (sslProtocol.equals("")) sslProtocol = null;
			
			String caCertFile = bindings.getValue("caFile");
			if (caCertFile.equals("")) caCertFile = null;
			
			try {
				MqttAdapter adapter = new MqttAdapter(appProfile, sm, url,port,clientId,user,password,sslProtocol,caCertFile);
				adapters.put(String.format(url+":%d", port),adapter);
			} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException e) {
				logger.error(e.getMessage());
			}
		}

	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			String url = bindings.getValue("url");
			int port = Integer.parseInt(bindings.getValue("port"));
			try {
				adapters.get(String.format(url+":%d", port)).close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			};
			
			adapters.remove(String.format(url+":%d", port));
		}

	}

	@Override
	public void onBrokenConnection() {
		logger.error("Broken connection");
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error(errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.info("Subscribed. SPUID: "+spuid+" alias: "+alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.info("Unsubscribed. SPUID: "+spuid);
	}

	@Override
	public void onFirstResults(BindingsResults results) {
		onAddedResults(results);		
	}
}

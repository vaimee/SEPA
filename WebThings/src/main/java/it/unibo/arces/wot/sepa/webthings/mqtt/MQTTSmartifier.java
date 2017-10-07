package it.unibo.arces.wot.sepa.webthings.mqtt;

import java.io.FileNotFoundException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.NoSuchElementException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.protocol.SSLSecurityManager;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

public class MQTTSmartifier extends Aggregator implements MqttCallback {
	private static final Logger logger = LogManager.getLogger("MQTTSmartifier");

	private MqttClient mqttClient;

	private String[] topicsFilter = { "#" };

	// Topics mapping
	private HashMap<String, String> topic2observation = new HashMap<String, String>();

	public MQTTSmartifier(String jsap) throws UnrecoverableKeyException, KeyManagementException, InvalidKeyException,
			IllegalArgumentException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
			FileNotFoundException, NoSuchElementException, NullPointerException, ClassCastException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, URISyntaxException {
		super(new ApplicationProfile(jsap), "ADD_OBSERVATION", "UPDATE_OBSERVATION_VALUE");

		logger.info("Parse extended data...");
	}
	
	@Override
	public void onAddedResults(BindingsResults results){
		for (Bindings bindings : results.getBindings()) {
			topic2observation.put(bindings.getBindingValue("topic"), bindings.getBindingValue("observation"));		
		}
	}

	private void updateObservationValue(String observation, String value) {
		Bindings bindings = new Bindings();
		bindings.addBinding("observation", new RDFTermURI(observation));
		bindings.addBinding("value", new RDFTermLiteral(value));

		logger.info("Update observation: " + bindings);
		update(bindings);
	}

	@Override
	public void connectionLost(Throwable arg0) {
		logger.error("Connection lost: " + arg0.getMessage());

		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {

			}

			logger.warn("Connecting...");
			try {
				mqttClient.connect();
			} catch (MqttException e) {
				logger.fatal("Failed to connect: " + e.getMessage());
				continue;
			}

			logger.warn("Subscribing...");
			try {
				mqttClient.subscribe(topicsFilter);
			} catch (MqttException e) {
				logger.fatal("Failed to subscribe " + e.getMessage());
				continue;
			}

			break;
		}

		logger.info("Connected and subscribed!");

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {

	}

	@Override
	public void messageArrived(String topic, MqttMessage value) throws Exception {
		logger.debug(topic + " " + value.toString());

		if (topic2observation.containsKey(topic)) {
			updateObservationValue(topic2observation.get(topic), value.toString());
		} else {
			logger.warn("Topic not found: " + topic);
		}
	}

	public void start() throws KeyManagementException, NoSuchAlgorithmException, MqttException, UnrecoverableKeyException, KeyStoreException, CertificateException, FileNotFoundException, IOException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, URISyntaxException, InterruptedException {
		// Subscribe to observation-topic mapping
		Response ret = subscribe(null);
		if (ret.isError()) {
			logger.fatal("Failed to subscribe: "+ret);
			throw new InterruptedException(ret.toString());
		}
		SubscribeResponse results = (SubscribeResponse) ret;
		onAddedResults(results.getBindingsResults());
		
		// MQTT
		JsonObject mqtt = getApplicationProfile().getExtendedData().get("mqtt").getAsJsonObject();

		String url = mqtt.get("url").getAsString();
		int port = mqtt.get("port").getAsInt();
		JsonArray topics = mqtt.get("topics").getAsJsonArray();

		topicsFilter = new String[topics.size()];
		int i = 0;
		for (JsonElement topic : topics) {
			topicsFilter[i] = topic.getAsString();
			i++;
		}

		boolean sslEnabled = false;
		if (mqtt.get("ssl") != null)
			sslEnabled = mqtt.get("ssl").getAsBoolean();

		String serverURI = null;
		if (sslEnabled) {
			serverURI = "ssl://" + url + ":" + String.format("%d", port);
		} else {
			serverURI = "tcp://" + url + ":" + String.format("%d", port);
		}

		// Create client
		logger.info("Creating MQTT client...");
		String clientID = MqttClient.generateClientId();
		logger.info("Client ID: " + clientID);
		logger.info("Server URI: " + serverURI);
		mqttClient = new MqttClient(serverURI, clientID);

		// Connect
		logger.info("Connecting...");
		MqttConnectOptions options = new MqttConnectOptions();
		if (sslEnabled) {
			SSLSecurityManager sm = new SSLSecurityManager("TLSv1","sepa.jks", "sepa2017", "sepa2017");
			logger.info("Set SSL security");
			options.setSocketFactory(sm.getSSLContext().getSocketFactory());
		}
		mqttClient.connect(options);

		// Subscribe
		mqttClient.setCallback(this);
		logger.info("Subscribing...");
		mqttClient.subscribe(topicsFilter);

		logger.info("MQTT client " + clientID + " subscribed to " + serverURI + " Topic filter " + topicsFilter);
	}

	public void stop() {
		try {
			if (topicsFilter != null)
				mqttClient.unsubscribe(topicsFilter);
		} catch (MqttException e1) {
			logger.error("Failed to unsubscribe " + e1.getMessage());
		}

		try {
			mqttClient.disconnect();
		} catch (MqttException e) {
			logger.error("Failed to disconnect " + e.getMessage());
		}

	}
}

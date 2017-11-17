package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SSLSecurityManager;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
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

	public MQTTSmartifier(String jsap) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile(jsap), "ADD_OBSERVATION", "UPDATE_OBSERVATION_VALUE");
	}

	@Override
	public void onAddedResults(BindingsResults results) {
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
		String topicValue = value.toString();
		logger.debug(topic + " " + topicValue);

		if (topic2observation.containsKey(topic)) {
			updateObservationValue(topic2observation.get(topic), topicValue);
		}
		
		// Check if value can be parsed with regex
		// e.g. pepoli:6lowpan:network = | ID: NODO1 | Temperature: 24.60 | Humidity: 35.40 | Pressure: 1016.46

		else if (appProfile.getExtendedData().get("regexTopics").getAsJsonObject().get(topic) != null) {
			JsonArray arr = appProfile.getExtendedData().get("regexTopics").getAsJsonObject().get(topic)
					.getAsJsonArray();
			for (JsonElement regex : arr) {
				Pattern p = Pattern.compile(regex.getAsString());
				Matcher m = p.matcher(value.toString());
				if (m.matches()) {
					for (int i = 0; i < m.groupCount(); i++) {
						topic += ":" + m.group(i);
					}
					topic = topic.replace(":", "/");

					topicValue = m.group("value");
					
					updateObservationValue(topic2observation.get(topic), topicValue);
				}
			}
		} 
		
		// Check if value can be parsed with JSON
		// e.g. {"moistureValue":3247, "nodeId":"device3", "timestamp":"2017-11-15T10:00:02.123028089Z"}
		
		else if (appProfile.getExtendedData().get("jsonTopics").getAsJsonObject().get(topic) != null) {
			String idMember = appProfile.getExtendedData().get("jsonTopics").getAsJsonObject().get(topic).getAsJsonObject().get("id").getAsString();
			String valueMember = appProfile.getExtendedData().get("jsonTopics").getAsJsonObject().get(topic).getAsJsonObject().get("value").getAsString();
			
			JsonObject json = new JsonParser().parse(topicValue).getAsJsonObject();
			String topicSuffix = json.get(idMember).getAsString();
			topicValue = json.get(valueMember).getAsString();
			
			topic += "/"+topicSuffix;
			
			updateObservationValue(topic2observation.get(topic), topicValue);		
		} else {
			logger.warn("TOPIC NOT FOUND: " + topic + " = " + topicValue);
		}
	}

	public boolean start() {
		// Subscribe to observation-topic mapping
		Response ret = subscribe(null);

		if (ret.isError()) {
			logger.fatal("Failed to subscribe: " + ret);
			return false;
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
		try {
			mqttClient = new MqttClient(serverURI, clientID);
		} catch (MqttException e) {
			logger.error(e.getMessage());
			return false;
		}

		// Connect
		logger.info("Connecting...");
		MqttConnectOptions options = new MqttConnectOptions();
		if (sslEnabled) {
			SSLSecurityManager sm;
			try {
				sm = new SSLSecurityManager("TLSv1", "sepa.jks", "sepa2017", "sepa2017");
			} catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException
					| CertificateException | IOException e) {
				logger.error(e.getMessage());
				return false;
			}
			logger.info("Set SSL security");
			try {
				options.setSocketFactory(sm.getSSLContext().getSocketFactory());
			} catch (KeyManagementException | NoSuchAlgorithmException e) {
				logger.error(e.getMessage());
				return false;
			}
		}
		try {
			mqttClient.connect(options);
		} catch (MqttException e) {
			logger.error(e.getMessage());
		}

		// Subscribe
		mqttClient.setCallback(this);
		logger.info("Subscribing...");
		try {
			mqttClient.subscribe(topicsFilter);
		} catch (MqttException e) {
			logger.error(e.getMessage());
			return false;
		}

		logger.info("MQTT client " + clientID + " subscribed to " + serverURI + " Topic filter " + topicsFilter);

		return true;
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

	@Override
	public void onResults(ARBindingsResults results) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPing() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBrokenSocket() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		// TODO Auto-generated method stub

	}
}

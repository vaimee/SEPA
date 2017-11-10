package it.unibo.arces.wot.sepa.webthings.mqtt;

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

import it.unibo.arces.wot.framework.ThingDescription;
import it.unibo.arces.wot.framework.interaction.EventPublisher;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SSLSecurityManager;

import java.io.FileReader;
import java.io.IOException;

import java.security.KeyManagementException;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import java.security.UnrecoverableKeyException;

import java.security.cert.CertificateException;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MQTTWebThing implements MqttCallback {
	private MqttClient mqttClient;

	private static final Logger logger = LogManager.getLogger("MQTTAdapter");

	private boolean created = false;

	public static HashMap<String, String> debugHash = new HashMap<String, String>();
	private HashMap<String, String> topicResponseCache = new HashMap<String, String>();

	private String serverURI = null;
	private String[] topicsFilter = { "#" };
	private boolean sslEnabled = false;
	private String clientID = "MQTTWebThing";

	// WoT APIs
	private EventPublisher event = null;
	private ThingDescription webThing = null;
	private String mqttEvent = "wot:mqttMessageReceived";

	//private SSLSecurityManager sm = new SSLSecurityManager("TLSv1","sepa.jks","sepa2017","sepa2017");
	
	public MQTTWebThing(String jsonFile) throws IOException, SEPAPropertiesException, SEPAProtocolException, SEPASecurityException {

		loadJSON(jsonFile);

		String thingURI = new String(serverURI);
		thingURI = thingURI.replace(":", "_");
		thingURI = thingURI.replace("/", "_");
		thingURI = "wot:MQTTBroker_" + thingURI;
		webThing = new ThingDescription(thingURI, clientID);
		webThing.addEvent(mqttEvent, "MQTT Event");
		event = new EventPublisher(thingURI);
	}

	private void loadJSON(String fileName) throws IOException {
		FileReader in = null;

		in = new FileReader(fileName);

		if (in != null) {
			JsonObject root = new JsonParser().parse(in).getAsJsonObject();

			String url = root.get("url").getAsString();
			int port = root.get("port").getAsInt();
			JsonArray topics = root.get("topics").getAsJsonArray();

			topicsFilter = new String[topics.size()];
			int i = 0;
			for (JsonElement topic : topics) {
				topicsFilter[i] = topic.getAsString();
				i++;
			}

			if (root.get("ssl") != null) this.sslEnabled = root.get("ssl").getAsBoolean();
			else this.sslEnabled = false;
			
			if (sslEnabled){
				serverURI = "ssl://" + url + ":" + String.format("%d", port);
			} else {
				serverURI = "tcp://" + url + ":" + String.format("%d", port);
			}
		}
		if (in != null)
			in.close();
	}

	@Override
	public void connectionLost(Throwable arg0) {
		logger.error(arg0.getMessage());
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {

	}

	@Override
	public void messageArrived(String topic, MqttMessage value) throws Exception {
		logger.debug(topic + " " + value.toString());

		event.post(mqttEvent, topic + "&" + value.toString());

		topicResponseCache.put(topic, topic + "&" + value.toString());

		if (debugHash.containsKey(topic)) {
			if (!debugHash.get(topic).equals(value.toString())) {
				logger.debug(topic + " " + debugHash.get(topic) + "-->" + value.toString());
			}
		}

		debugHash.put(topic, value.toString());
	}

	public static void main(String[] args) throws SEPAPropertiesException, SEPAProtocolException, SEPASecurityException, IOException {

		if (args.length != 1) {
			logger.error("Please specify the configuration file (.json)");
			System.exit(1);
		}

		MQTTWebThing adapter = new MQTTWebThing(args[0]);

		if (adapter.start()) {
			logger.info("Press any key to exit...");
			System.in.read();
			adapter.stop();
			logger.info("Stopped");
		} else {
			logger.fatal("NOT running");
			logger.info("Press any key to exit...");
			System.in.read();
		}
	}

	public boolean start(){
		try {
			mqttClient = new MqttClient(serverURI, clientID);
		} catch (MqttException e) {
			logger.fatal("Failed to create MQTT client " + e.getMessage());
			return created;
		}

		try {
			MqttConnectOptions options = new MqttConnectOptions();
			if (sslEnabled) {
				SSLSecurityManager sm;
				try {
					sm = new SSLSecurityManager("TLSv1","sepa.jks","sepa2017","sepa2017");
				} catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException
						| NoSuchAlgorithmException | CertificateException | IOException e) {
					logger.error(e.getMessage());
					return false;
				}
				try {
					options.setSocketFactory(sm.getSSLContext().getSocketFactory());
				} catch (KeyManagementException | NoSuchAlgorithmException e) {
					logger.error(e.getMessage());
					return false;
				}
			}
			mqttClient.connect(options);
		} catch (MqttException e) {
			logger.fatal(e.getMessage());
			return created;
		}

		mqttClient.setCallback(this);

		try {
			mqttClient.subscribe(topicsFilter);
		} catch (MqttException e) {
			logger.fatal("Failed to subscribe " + e.getMessage());
			return created;
		}

		String topics = "";
		for (int i = 0; i < topicsFilter.length; i++)
			topics += "\"" + topicsFilter[i] + "\" ";

		logger.info("MQTT client " + clientID + " subscribed to " + serverURI + " Topic filter " + topics);

		created = true;

		return created;
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
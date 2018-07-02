package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;

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

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class MQTTAdapter extends Producer implements MqttCallback {
	private static final Logger logger = LogManager.getLogger();

	private MqttClient mqttClient;
	private String[] topicsFilter = null;
	private String serverURI = null;

	public static void main(String[] args) throws IOException, SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		if (args.length != 1) {
			logger.error("Please provide the jsap file as argument");
			System.exit(-1);
		}
		
		MQTTAdapter adapter = new MQTTAdapter(args[0]);
		adapter.start();
		
		System.out.println("Press any key to exit...");
		System.in.read();
		
		adapter.stop();
		adapter.close();
	}
	
	public MQTTAdapter(String jsap) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new JSAP(jsap), "MQTT_MESSAGE");
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
		logger.info(topic + " " + value.toString());

		setUpdateBindingValue("topic",new RDFTermLiteral(topic));
		setUpdateBindingValue("value",new RDFTermLiteral(value.toString()));
		setUpdateBindingValue("broker",new RDFTermURI(serverURI));
		update();
	}

	public boolean start() throws SEPASecurityException {
		/*
		 * test.mosquitto.org 1883
		 * giove.arces.unibo.it 52877
		 * 
		 * */
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
			logger.info("Set SSL security");
			
			SEPASecurityManager sm = new SEPASecurityManager("sepa.jks", "sepa2017", "sepa2017");
			options.setSocketFactory(sm.getSSLContext().getSocketFactory());
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

		String printTopics = "Topic filter ";
		for (String s : topicsFilter) {
			printTopics += s + " ";
		}
		logger.info("MQTT client " + clientID + " subscribed to " + serverURI + printTopics);
		
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
}

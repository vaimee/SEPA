package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;
import java.util.Map.Entry;

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

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class MqttAdapter extends Producer implements MqttCallback {
	private static final Logger logger = LogManager.getLogger();

	private MqttClient mqttClient;
	private String serverURI;
	private String[] topicsFilter = { "#" };

	private boolean sslEnabled = false;
	private MqttConnectOptions options;
	
	public void simulator(JsonObject topics) {
		new Thread() {
			public void run() {
				while (true) {
					for (Entry<String, JsonElement> observation : topics.entrySet()) {
						String topic = observation.getKey();
						int min = observation.getValue().getAsJsonArray().get(0).getAsInt();
						int max = observation.getValue().getAsJsonArray().get(1).getAsInt();
						String value = String.format("%.2f", min + (Math.random() * (max - min)));						
						
						logger.info("[Simulate MQTT message] Topic: "+topic+ " Value: "+value);
						
						try {
							setUpdateBindingValue("topic",new RDFTermLiteral(topic));
							setUpdateBindingValue("value",new RDFTermLiteral(value));
							setUpdateBindingValue("broker",new RDFTermLiteral("simulator"));
							
							update();
						} catch (SEPASecurityException | IOException | SEPAPropertiesException | SEPABindingsException e) {
							logger.error(e.getMessage());
						}
						
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							return;
						}
					}
				}
			}
		}.start();
	}
	
	public MqttAdapter(JSAP appProfile, SEPASecurityManager sm,JsonObject mqtt,boolean sim)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, MqttException {
		super(appProfile, "MQTT_MESSAGE", sm);
		
		if (sim) {
			simulator(mqtt);
			return;
		}
		
		//JsonObject mqtt = appProfile.getExtendedData().get("mqtt").getAsJsonObject();
		
		String url = mqtt.get("url").getAsString();
		int port = mqtt.get("port").getAsInt();
		JsonArray topics = mqtt.get("topics").getAsJsonArray();

		topicsFilter = new String[topics.size()];
		int i = 0;
		for (JsonElement topic : topics) {
			topicsFilter[i] = topic.getAsString();
			i++;
		}

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

		mqttClient = new MqttClient(serverURI, clientID);

		// Options
		options = new MqttConnectOptions();
		if (sslEnabled) {
			logger.info("Set SSL security");
			options.setSocketFactory(sm.getSSLContext().getSocketFactory());
		}

		if (mqtt.has("username")) {
			options.setUserName(mqtt.get("username").getAsString());
		}

		if (mqtt.has("password")) {
			options.setPassword(mqtt.get("password").getAsString().toCharArray());
		}

		// Connect
		logger.info("Connecting...");
		try {
			mqttClient.connect(options);
		} catch (MqttException e) {
			logger.error(e.getMessage());
		}

		// Subscribe
		mqttClient.setCallback(this);
		logger.info("Subscribing...");

		mqttClient.subscribe(topicsFilter);

		for (String topic : topicsFilter)
			logger.info("MQTT client " + clientID + " subscribed to " + serverURI + " Topic filter " + topic);
	}

	@Override
	public void connectionLost(Throwable cause) {
		logger.error("Connection lost: " + cause.getMessage());

		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {

			}

			logger.warn("Connecting...");
			try {
				mqttClient.connect(options);
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
	public void messageArrived(String topic, MqttMessage message)  {
		byte[] payload = message.getPayload();
		String converted = "";
		for (int i = 0; i < payload.length; i++) {
			if (payload[i] == 0)
				break;
			converted += String.format("%c", payload[i]);
		}

		logger.info(serverURI+" message received: " + topic + " " + converted);
		
		try {
			setUpdateBindingValue("topic",new RDFTermLiteral(topic));
			setUpdateBindingValue("value",new RDFTermLiteral(converted));
			setUpdateBindingValue("broker",new RDFTermLiteral(serverURI));
			
			update();
		} catch (SEPASecurityException | IOException | SEPAPropertiesException | SEPABindingsException e) {
			logger.error(e.getMessage());
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() throws IOException {
		super.close();
		
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

package it.unibo.arces.wot.sepa.webthings.mqtt;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
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
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.protocol.SSLSecurityManager;

public class MQTTSmartifier implements MqttCallback {
	private MqttClient mqttClient;

	private static final Logger logger = LogManager.getLogger("MQTTSmartifier");

	private boolean created = false;

	private String serverURI = null;
	private String[] topicsFilter = { "#" };
	private boolean sslEnabled = false;
	private String clientID = "MQTTSmartifier";

	private SSLSecurityManager sm = new SSLSecurityManager("TLSv1","sepa.jks","sepa2017","sepa2017");
	
	public MQTTSmartifier(String jsonFile) throws UnrecoverableKeyException, KeyManagementException, InvalidKeyException,
			IllegalArgumentException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
			FileNotFoundException, NoSuchElementException, NullPointerException, ClassCastException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, URISyntaxException {

		loadJSON(jsonFile);
	}

	private void loadJSON(String fileName) throws IOException {
		FileReader in = null;

		in = new FileReader(fileName);

		if (in != null) {
			JsonObject root = new JsonParser().parse(in).getAsJsonObject();
			
			//MQTT
			JsonObject mqtt =  root.get("mqtt").getAsJsonObject();

			String url = mqtt.get("url").getAsString();
			int port = mqtt.get("port").getAsInt();
			JsonArray topics = mqtt.get("topics").getAsJsonArray();

			topicsFilter = new String[topics.size()];
			int i = 0;
			for (JsonElement topic : topics) {
				topicsFilter[i] = topic.getAsString();
				i++;
			}

			if (mqtt.get("ssl") != null) this.sslEnabled = mqtt.get("ssl").getAsBoolean();
			else this.sslEnabled = false;
			
			if (sslEnabled){
				serverURI = "ssl://" + url + ":" + String.format("%d", port);
			} else {
				serverURI = "tcp://" + url + ":" + String.format("%d", port);
			}
			
			//Semantic mappings
			
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

	
	}

	public static void main(String[] args) throws IOException, URISyntaxException, UnrecoverableKeyException,
			KeyManagementException, InvalidKeyException, IllegalArgumentException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, NoSuchElementException, NullPointerException,
			ClassCastException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {

		if (args.length != 1) {
			logger.error("Please specify the JSAP file (.jsap)");
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

	public boolean start() throws KeyManagementException, NoSuchAlgorithmException {
		try {
			mqttClient = new MqttClient(serverURI, clientID);
		} catch (MqttException e) {
			logger.fatal("Failed to create MQTT client " + e.getMessage());
			return created;
		}

		try {
			MqttConnectOptions options = new MqttConnectOptions();
			if (sslEnabled) {
				options.setSocketFactory(sm.getSSLContext().getSocketFactory());
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

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
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.protocol.SSLSecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class MQTTSmartifier implements MqttCallback {
	private static final Logger logger = LogManager.getLogger("MQTTSmartifier");
	
	private MqttClient mqttClient;
	private MqttConnectOptions options;

	private String serverURI = null;
	private String[] topicsFilter = { "#" };
	private boolean sslEnabled = false;
	private String clientID = null;

	//SEPA
	private SSLSecurityManager sm = new SSLSecurityManager("TLSv1", "sepa.jks", "sepa2017", "sepa2017");
	private ApplicationProfile app;
	private Producer observationCreator;
	private Producer observationUpdater;
	
	//Current observation and topics matching hash
	private HashMap<String,JsonObject> observationMap = new HashMap<String,JsonObject>();
	private HashMap<MQTTTopic,String> topicMap = new HashMap<MQTTTopic,String>();
	
	class MQTTTopic {
		private Pattern topic;
		private Matcher matcher;
		private String pattern;
		
		public MQTTTopic(String pattern) {
			topic = Pattern.compile(pattern);
			this.pattern = pattern;
		}
		
		public String getPattern() {
			return pattern;
			
		} 
		@Override
		public boolean equals(Object obj) {
			if (!obj.getClass().equals(this.getClass())) return false;
			matcher = topic.matcher(((MQTTTopic)obj).getPattern());
			return matcher.find();
		}
		
		public String getMatching() {
			int nGroups = matcher.groupCount();
			if (nGroups == 3) return matcher.group(2);
			return "value";
		}
	}
	
	public MQTTSmartifier(String jsap) throws UnrecoverableKeyException, KeyManagementException, InvalidKeyException,
			IllegalArgumentException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
			FileNotFoundException, NoSuchElementException, NullPointerException, ClassCastException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, URISyntaxException {

		app = new ApplicationProfile(jsap);
		
		observationCreator = new Producer(app,"ADD_OBSERVATION");
		observationUpdater = new Producer(app,"UPDATE_OBSERVATION_VALUE");
		
		parseExtendedData(app.getExtendedData());
	}
	
	private void addObservation(String observation,String comment,String label,String location, String unit,String topic){
		Bindings bindings = new Bindings();
		bindings.addBinding("observation", new RDFTermURI(observation.replace("/", "-")));
		bindings.addBinding("comment", new RDFTermLiteral(comment));
		bindings.addBinding("label", new RDFTermLiteral(label));
		bindings.addBinding("location", new RDFTermURI(location));
		bindings.addBinding("unit", new RDFTermURI(unit));
		observationCreator.update(bindings);
		
		observationMap.put(observation, new JsonObject());
		topicMap.put(new MQTTTopic(topic), observation);
	}

	private void updateObservationValue(String observation,String value){
		Bindings bindings = new Bindings();
		bindings.addBinding("observation", new RDFTermURI(observation.replace("/", "-")));
		bindings.addBinding("value", new RDFTermLiteral(value));
		observationUpdater.update(bindings);	
	}

	private void parseExtendedData(JsonObject extended) throws IOException {
		if (extended == null)
			throw new IllegalArgumentException();

		// MQTT
		JsonObject mqtt = extended.get("mqtt").getAsJsonObject();

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
			this.sslEnabled = mqtt.get("ssl").getAsBoolean();
		else
			this.sslEnabled = false;

		if (sslEnabled) {
			serverURI = "ssl://" + url + ":" + String.format("%d", port);
		} else {
			serverURI = "tcp://" + url + ":" + String.format("%d", port);
		}

		// Semantic mappings
		JsonObject mappings = extended.get("semantic-mappings").getAsJsonObject();
		
		for (Entry<String,JsonElement> mapping : mappings.entrySet()) {
			String topic = mapping.getKey();
			
			String observation = mapping.getValue().getAsJsonObject().get("observation").getAsString();
			String unit = mapping.getValue().getAsJsonObject().get("unit").getAsString();
			String location = mapping.getValue().getAsJsonObject().get("location").getAsString();
			String comment = mapping.getValue().getAsJsonObject().get("comment").getAsString();
			String label = mapping.getValue().getAsJsonObject().get("label").getAsString();
		
			addObservation(observation,comment,label,location,unit,topic);	
		}
		
	}

	@Override
	public void connectionLost(Throwable arg0) {
		logger.error("Connection lost: "+arg0.getMessage());
		
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				
			}
			
			logger.warn("Connecting...");
			try {
				mqttClient.connect();
			} catch (MqttException e) {
				logger.fatal("Failed to connect: "+e.getMessage());
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

		MQTTTopic matching = new MQTTTopic(topic);
		
		for (Entry<MQTTTopic,String> topicPattern :topicMap.entrySet()) {
			if (topicPattern.getKey().equals(matching)) {
				String observation = topicPattern.getValue();
				JsonObject observationObject = observationMap.get(observation);
				String valueKey = topicPattern.getKey().getMatching();
				
				observationObject.add(valueKey,new JsonPrimitive(value.toString()));
				
				updateObservationValue(observation,observationObject.toString());
				break;
			}
		}
	}

	public static void main(String[] args) throws IOException, URISyntaxException, UnrecoverableKeyException,
			KeyManagementException, InvalidKeyException, IllegalArgumentException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, NoSuchElementException, NullPointerException,
			ClassCastException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, MqttException {
	      
		if (args.length != 1) {
			logger.error("Please specify the JSAP file (.jsap)");
			System.exit(1);
		}

		MQTTSmartifier adapter = new MQTTSmartifier(args[0]);

		adapter.start();
		
		logger.info("Press any key to exit...");
		System.in.read();
		
		adapter.stop();
		
		logger.info("Stopped");	
	}

	public void start() throws KeyManagementException, NoSuchAlgorithmException, MqttException {
		//Create client
		clientID = MqttClient.generateClientId();
		mqttClient = new MqttClient(serverURI, clientID);
		
		//Connect
		options = new MqttConnectOptions();
		if (sslEnabled) options.setSocketFactory(sm.getSSLContext().getSocketFactory());
		mqttClient.connect(options);	
		
		//Subscribe
		mqttClient.setCallback(this);
		mqttClient.subscribe(topicsFilter);

		String topics = "";
		for (int i = 0; i < topicsFilter.length; i++)
			topics += "\"" + topicsFilter[i] + "\" ";

		logger.info("MQTT client " + clientID + " subscribed to " + serverURI + " Topic filter " + topics);
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

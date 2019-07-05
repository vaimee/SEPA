package it.unibo.arces.wot.sepa.apps.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

class Callback implements MqttCallback {
	private static final Logger logger = LogManager.getLogger();
	
	private final String url;
	private final String topic;
	private final String clientId;
	private MqttClient client;
	
	public Callback(MqttClient client,String url,String topic,String clientId) {
		this.url = url;
		this.topic = topic;
		this.clientId = clientId;
		this.client = client;
	}
	
	@Override
	public void connectionLost(Throwable cause) {
		logger.debug("@connectionLost " + cause);
		try {
			client.close();
			client = new MqttClient(url, clientId);
			client.connect();
			client.subscribe(topic);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		logger.debug("@messageArrived Topic: " + topic+ " Message: "+message);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		logger.debug("@deliveryComplete " + token);
	}	
}

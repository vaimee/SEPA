package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class MqttAdapter extends Aggregator implements MqttCallbackExtended {
	private static final Logger logger = LogManager.getLogger();

	private MqttClient mqttClient;
	private String serverURI;
	private ArrayList<String> topics = new ArrayList<String>();
	private ArrayList<String> topicsRemoved = new ArrayList<String>();

	private MqttConnectOptions options;

	private Thread subThread;
	private Thread unsubThread;
	private Thread connThread;

	public void enableMqttDebugging() {
		LoggerFactory.setLogger(Logging.class.getName());
	}

	public MqttAdapter(JSAP appProfile, SEPASecurityManager sm, JsonObject sim)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(appProfile, "MQTT_BROKER_TOPICS", "MQTT_MESSAGE", sm);

		simulator(sim);
	}

	public MqttAdapter(JSAP appProfile, SEPASecurityManager sm, String url, int port, String clientId, String user,
			String password, String protocol, String caCertFile)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		super(appProfile, "MQTT_BROKER_TOPICS", "MQTT_MESSAGE", sm);

		// Options
		options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);

		logger.debug("Client ID: " + clientId);

		if (protocol != null) {
			logger.info("SSL enabled");

			serverURI = String.format("ssl://%s:%d", url, port);

			// CA certificates
			if (caCertFile == null)
				options.setSocketFactory(SEPASecurityManager.getSSLContextTrustAllCa(protocol).getSocketFactory());
			else
				options.setSocketFactory(SEPASecurityManager.getSSLContext(protocol, caCertFile).getSocketFactory());

		} else {
			serverURI = String.format("tcp://%s:%d", url, port);
		}
		logger.info("MQTT client to broker: " + serverURI);

		// Create client
		logger.debug("Creating MQTT client...");
		if (clientId != null) {

			try {
				mqttClient = new MqttClient(serverURI, clientId);
			} catch (MqttException e) {
				throw new SEPASecurityException(e);
			}
		} else
			try {
				mqttClient = new MqttClient(serverURI, MqttClient.generateClientId());
			} catch (MqttException e) {
				throw new SEPASecurityException(e);
			}

		if (user != null) {
			logger.debug("User: " + user);
			options.setUserName(user);
		}

		if (password != null) {
			logger.debug("Password: " + password);
			options.setPassword(password.toCharArray());
		}

		mqttClient.setCallback(this);

		connThread = new Thread() {
			public void run() {
				while (!this.isInterrupted()) {
					try {
						logger.debug(serverURI + " connecting...");
						mqttClient.connect(options);
						break;
					} catch (MqttSecurityException e) {
						logger.error(serverURI + " MqttSecurityException: " + e.getMessage());
					} catch (MqttException e) {
						logger.error(serverURI + " MqttException: " + e.getMessage());
					}
				}
			}
		};
		connThread.setName(serverURI + " conn");
		connThread.start();

		subThread = new Thread() {
			public void run() {
				while (!this.isInterrupted()) {
					synchronized (topicsRemoved) {
						try {
							logger.debug(serverURI + " wait for topics");
							topicsRemoved.wait();
						} catch (InterruptedException e) {
							return;
						}

						for (String topic : topicsRemoved) {
							while (true) {

								try {
									logger.debug(serverURI + " unsubscribe from: " + topic);
									mqttClient.unsubscribe(topic);
									topics.remove(topic);
								} catch (MqttException e) {
									try {
										Thread.sleep(500);
									} catch (InterruptedException e1) {
										return;
									}
									logger.warn(serverURI + " exception on unsubscribe: " + e.getMessage());
									continue;
								}

								logger.debug(serverURI + " unsubscribed from: " + topic);

								break;
							}
						}

						topicsRemoved.clear();
					}
				}
			}
		};
		subThread.setName(serverURI + " sub");
		subThread.start();

		unsubThread = new Thread() {
			public void run() {
				while (true) {
					synchronized (topics) {
						try {
							logger.debug(serverURI + " wait for topics");
							topics.wait();
						} catch (InterruptedException e) {
							return;
						}

						for (String topic : topics) {
							while (true) {

								try {
									logger.debug(serverURI + " subscribe to " + topic);
									mqttClient.subscribe(topic);
								} catch (MqttException e) {
									try {
										Thread.sleep(500);
									} catch (InterruptedException e1) {
										return;
									}
									logger.warn(serverURI + " exception on subscribe: " + e.getMessage());
									continue;
								}

								logger.debug(serverURI + " subscribed to: " + topic);

								break;
							}
						}
					}

				}
			}
		};
		unsubThread.setName(serverURI + " unsub");
		unsubThread.start();

		try {
			setSubscribeBindingValue("url", new RDFTermLiteral(url));
			setSubscribeBindingValue("port", new RDFTermLiteral(String.format("%d", port), "xsd:integer"));
		} catch (SEPABindingsException e1) {
			logger.error(serverURI + " failed to set bindings: " + e1.getMessage());
		}

		try {
			logger.debug(serverURI + " subscribe to SEPA...");
			subscribe(5000);
		} catch (SEPASecurityException | SEPAPropertiesException | SEPAProtocolException
				| SEPABindingsException e) {
			logger.error(serverURI + " failed to subscribe: " + e.getMessage());
		}
	}

	public void simulator(JsonObject topics) {
		new Thread() {
			public void run() {
				while (true) {
					for (Entry<String, JsonElement> observation : topics.entrySet()) {
						String topic = observation.getKey();
						int min = observation.getValue().getAsJsonArray().get(0).getAsInt();
						int max = observation.getValue().getAsJsonArray().get(1).getAsInt();
						String value = String.format("%.2f", min + (Math.random() * (max - min)));

						logger.info("[Simulate MQTT message] Topic: " + topic + " Value: " + value);

						try {
							setUpdateBindingValue("topic", new RDFTermLiteral(topic));
							setUpdateBindingValue("value", new RDFTermLiteral(value));
							setUpdateBindingValue("broker", new RDFTermLiteral("simulator"));

							OffsetDateTime utc = OffsetDateTime.now(ZoneOffset.UTC);			
							setUpdateBindingValue("timestamp", new RDFTermLiteral(utc.toString()));
							
							update();
						} catch (SEPASecurityException | SEPAProtocolException | SEPAPropertiesException
								| SEPABindingsException e) {
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

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		logger.debug(serverURI + " @connectComplete reconnect: " + reconnect);

		synchronized (topics) {
			logger.debug(serverURI + " notify!");
			topics.notify();
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		logger.warn(serverURI + " connection lost: " + cause.getMessage());
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		byte[] payload = message.getPayload();
		String converted = "";
		for (int i = 0; i < payload.length; i++) {
			if (payload[i] == 0)
				break;
			converted += String.format("%c", payload[i]);
		}

		logger.info(serverURI + " message received: " + topic + " " + converted);

		try {
			OffsetDateTime utc = OffsetDateTime.now(ZoneOffset.UTC);
			
			setUpdateBindingValue("topic", new RDFTermLiteral(topic));
			setUpdateBindingValue("value", new RDFTermLiteral(converted));
			setUpdateBindingValue("broker", new RDFTermLiteral(serverURI));			
			setUpdateBindingValue("timestamp", new RDFTermLiteral(utc.toString(),"xsd:dateTime"));
			
			update();
		} catch (SEPASecurityException | SEPAProtocolException | SEPAPropertiesException | SEPABindingsException e) {
			logger.error(serverURI + " exception:" + e.getMessage());
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {

	}

	@Override
	public void close() throws IOException {
		super.close();

		if (subThread != null)
			if (subThread.isAlive())
				subThread.interrupt();
		if (unsubThread != null)
			if (unsubThread.isAlive())
				unsubThread.interrupt();
		if (connThread != null)
			if (connThread.isAlive())
				connThread.interrupt();

		try {
			mqttClient.disconnect();
		} catch (MqttException e) {
			logger.warn(serverURI + " failed to disconnect " + e.getMessage());
		}

		try {
			mqttClient.close();
		} catch (MqttException e) {
			logger.warn(serverURI + " failed to close " + e.getMessage());
		}
	}

	@Override
	public void onResults(ARBindingsResults results) {

	}

	@Override
	public void onAddedResults(BindingsResults results) {
		synchronized (topics) {
			for (Bindings bindings : results.getBindings()) {
				String topicString = bindings.getValue("topic");

				if (!topics.contains(topicString))
					topics.add(topicString);
			}

			logger.debug(serverURI + " notify!");
			topics.notify();
		}
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		synchronized (topicsRemoved) {
			for (Bindings bindings : results.getBindings()) {
				String topicString = bindings.getValue("topic");

				if (!topicsRemoved.contains(topicString))
					topicsRemoved.add(topicString);
			}

			logger.debug(serverURI + " notify!");
			topicsRemoved.notify();
		}
	}

	@Override
	public void onBrokenConnection() {
		logger.warn(serverURI + " onBrokenConnection");
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error(serverURI + " onError " + errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug(serverURI + " onSubscribe SPUID: " + spuid + " alias: " + alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug(serverURI + " onUnsubscribe SPUID: " + spuid);
	}

	@Override
	public void onFirstResults(BindingsResults results) {
		onAddedResults(results);	
	}
}

package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class MQTTMonitor {
	private static final Logger logger = LogManager.getLogger();

	// Produce observations coming from MQTT matching with the semantic mapping
	private static MQTTSmartifier smartifier;
	
	// Add observation based on the semantic mapping stored in JSAP
	private static MQTTInitializer mqttInitializer;

	public static void main(String[] args)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException {

		logger.info("Create initializer");
		mqttInitializer = new MQTTInitializer();
		mqttInitializer.init();
		
		// Create MQTT smartifier
		logger.info("Create MQTT smartifier");
		smartifier = new MQTTSmartifier();

		logger.info("Start MQTT smartifier");
		if (smartifier.start()) {
			logger.info("Press any key to exit...");
			try {
				System.in.read();
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}

			logger.info("Stop MQTT smartifier");
			smartifier.stop();

			logger.info("Stopped");
		}
		
		System.exit(1);
	}
}

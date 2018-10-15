package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class MQTTMonitor {
	private static final Logger logger = LogManager.getLogger();

	// Produce observations coming from MQTT matching with the semantic mapping
	private static MQTTSmartifier smartifier;
	
	// Add observation based on the semantic mapping stored in JSAP
	private static MQTTMapper mqttInitializer;

	private static SEPASecurityManager sm = null;
	
	public static void main(String[] args)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException, MqttException {
		if (args.length != 1) {
			logger.error("Please provide the jsap file as argument");
			System.exit(-1);
		}
		
		JSAP app = new JSAP(args[0]);			
		if (app.isSecure()) sm = new SEPASecurityManager("sepa.jks", "sepa2017", "sepa2017",app.getAuthenticationProperties());
		
		// Logger
		ObservationLogger analytics = new ObservationLogger(app,sm);
		analytics.subscribe(5000);
		
		// Remover
		ObservationRemover remover = new ObservationRemover(app,sm);
		remover.removeAll();
		remover.close();
		
		
		
		// Inizializer
		mqttInitializer = new MQTTMapper(app,sm);
		mqttInitializer.init();
		mqttInitializer.close();
		
		// Create MQTT smartifier
		smartifier = new MQTTSmartifier(app,sm);
		smartifier.start();
		
		logger.info("Press any key to exit...");
		try {
			System.in.read();
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		logger.info("Stop MQTT smartifier");
		smartifier.stop();

		logger.info("Stopped");
		
		analytics.close();
		
		System.exit(1);
	}
}

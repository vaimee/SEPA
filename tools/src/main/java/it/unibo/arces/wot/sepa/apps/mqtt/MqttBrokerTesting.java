package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class MqttBrokerTesting {
	
	public static void main(String[] args) throws SEPASecurityException, InterruptedException, SEPAProtocolException, SEPAPropertiesException, SEPABindingsException, IOException {
		// Wizzilab
		String url ="roger.wizzilab.com";
		int port = 8883;
		String clientId = "ffa574972ab9:1";
		String caCertFile = "/etc/ssl/cert.pem";
		String user = "ffa574972ab9";
		String password = "6e257b56172ea934d79ee1f5c2c1c7a9";
		String protocol = "SSL";
		
		
		MqttAdapter clientAdapter = new MqttAdapter(new JSAP("base.jsap"), new SEPASecurityManager("sepa.jks", "sepa2017", "sepa2017", null), url, port, clientId, user, password, protocol, caCertFile);
		
		synchronized (clientAdapter) {
			clientAdapter.wait();	
		}
		clientAdapter.close();
	}
}

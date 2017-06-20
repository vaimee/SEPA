/* This class is the main entry point of the Semantic Event Processing Architecture (SEPA) Engine
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package it.unibo.arces.wot.sepa.engine.core;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.regex.PatternSyntaxException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nimbusds.jose.JOSEException;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;

import it.unibo.arces.wot.sepa.engine.processing.Processor;

import it.unibo.arces.wot.sepa.engine.protocol.http.HttpServer;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpsServer;
import it.unibo.arces.wot.sepa.engine.protocol.websocket.WebsocketServer;
import it.unibo.arces.wot.sepa.engine.protocol.websocket.SecureWebsocketServer;

import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

/**
 * This class represents the SPARQL Subscription (SUB) Engine of the Semantic
 * Event Processing Architecture (SEPA)
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.6
 */

public class Engine extends Thread implements EngineMBean {
	// Properties, logging
	private static final Logger logger = LogManager.getLogger("Engine");
	private EngineProperties engineProperties = null;
	private SPARQL11Properties endpointProperties = null;

	// JMX properties
	private static Date startDate;

	// Primitives scheduler/dispatcher
	private Scheduler scheduler = null;

	// Primitives processor
	private Processor processor = null;

	// SPARQL 1.1 Protocol handler
	private HttpServer httpGate = null;

	// SPARQL 1.1 SE Protocol handler
	private WebsocketServer wsServer;
	private SecureWebsocketServer wssServer;
	private HttpsServer httpsGate = null;

	//Outh 2.0 Authorization Server
	private static AuthorizationManager oauth;
	private static String storeName ="sepa.jks";
	private static String storePassword ="sepa2017";
	private static String jwtAlias = "sepakey";
	private static String jwtPassword ="sepa2017";
	private static String serverCertificate = "sepacert";

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("java [JMX] -jar sepa-engine.jar [OPTIONS]");
		System.out.println("");
		System.out.println("JMX:");
		System.out.println("-Dcom.sun.management.config.file=jmx.properties : to enable JMX remote managment");
		System.out.println("OPTIONS:");
		System.out.println("-help : to print this help");
		System.out.println("-storename=<name> : file name of the JKS     (default: sepa.jks)");
		System.out.println("-storepwd=<pwd> : password of the JKS        (default: sepa2017)");
		System.out.println("-alias=<jwt> : alias for the JWT key      	 (default: sepakey)");
		System.out.println("-aliaspwd=<pwd> : password of the JWT key    (default: sepa2017)");
		System.out.println("-certificate=<crt> : name of the certificate (default: sepacert)");
		
	}
	private static void parsingArgument(String[] args) throws PatternSyntaxException {
		String[] tmp;
		for (String arg : args) {
			if (arg.equals("-help")) {
				printUsage();
				return;
			}
			if (arg.startsWith("-")) {
				tmp = arg.split("=");
				switch(tmp[0]) {
				case "-storename":
					storeName = tmp[1];
					break;
				case "-storepwd":		
					storePassword = tmp[1];
					break;
				case "-alias":
					jwtAlias = tmp[1];
					break;
				case "-aliaspwd":
					jwtPassword = tmp[1];
					break;
				case "-certificate":		
					serverCertificate = tmp[1];
					break;
				default:
					break;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		//Set Grizzly logging level
		java.util.logging.Logger grizzlyNetworkListener = java.util.logging.Logger
				.getLogger("org.glassfish.grizzly.http.server.NetworkListener");
		java.util.logging.Logger grizzlyHttpServer = java.util.logging.Logger
				.getLogger("org.glassfish.grizzly.http.server.HttpServer");
		grizzlyNetworkListener.setLevel(Level.SEVERE);
		grizzlyHttpServer.setLevel(Level.SEVERE);

		//Command arguments
		parsingArgument(args);
				
		System.out
				.println("##########################################################################################");
		System.out
				.println("# SEPA Engine Ver 0.7.5  Copyright (C) 2016-2017                                         #");
		System.out
				.println("# Web of Things Research @ ARCES - University of Bologna (Italy)                         #");
		System.out
				.println("#                                                                                        #");
		System.out
				.println("# This program comes with ABSOLUTELY NO WARRANTY                                         #");
		System.out
				.println("# This is free software, and you are welcome to redistribute it under certain conditions #");
		System.out
				.println("# GNU GENERAL PUBLIC LICENSE, Version 3, 29 June 2007                                    #");
		System.out
				.println("#                                                                                        #");
		System.out
				.println("# GITHUB: https://github.com/arces-wot/sepa                                              #");
		System.out
				.println("# WEB: http://wot.arces.unibo.it                                                         #");
		System.out
				.println("# WIKI: https: // github.com/arces-wot/SEPA/wiki                                         #");
		System.out
				.println("##########################################################################################");
		System.out.println("");
		System.out
				.println("--------------------------------- Maven dependencies -------------------------------------");
		System.out.println("<!-- https://mvnrepository.com/artifact/org.apache.httpcomponents/httpcore-nio -->"
				+ "\n<!-- https://mvnrepository.com/artifact/org.apache.httpcomponents/httpcore -->"

				+ "\n<!-- https://mvnrepository.com/artifact/org.java-websocket/Java-WebSocket -->"

				+ "\n<!-- https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-api -->"
				+ "\n<!-- https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core -->"

				+ "\n<!-- https://mvnrepository.com/artifact/com.google.code.gson/gson -->"

				+ "\n<!-- https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt -->"
				
				+ "\n<!-- https://mvnrepository.com/artifact/commons-io/commons-io -->");
		
		// Engine creation and initialization
		Engine engine = new Engine();

		//OAUTH 2.0 Authorization Manager
		try {
			oauth = new AuthorizationManager(storeName, storePassword, jwtAlias, jwtPassword,serverCertificate);
		} catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException | JOSEException e1) {
			logger.fatal(e1.getLocalizedMessage());
			System.exit(1);
		}
		
		// Initialize
		try {
			if (!engine.init()) System.exit(1);
		} catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException
				| NotCompliantMBeanException | UnrecoverableKeyException | KeyManagementException
				| IllegalArgumentException | NoSuchElementException | KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException  | NullPointerException | ClassCastException  e) {
			logger.fatal(e.getMessage());
			System.exit(1);
		}

		// Starting main engine thread
		engine.start();

		// Welcome message
		System.out.println("");
		System.out.println("*****************************************************************************************");
		System.out.println("*                      SEPA Engine Ver 0.7.5 is up and running                          *");
		System.out.println("*                                 Let Things Talk                                       *");
		System.out.println("*****************************************************************************************");
	}

	public Engine() {
		SEPABeans.registerMBean("SEPA:type=Engine", this);
	}

	@Override
	public void start() {

		this.setName("SEPA Engine");

		// Scheduler
		scheduler.start();

		// SPARQL 1.1 Protocol handler
		new Thread(httpGate).start();
		
		// SPARQL 1.1 SE Protocol handler
		new Thread(httpsGate).start();

		wsServer.start();
		
		wssServer.start();

		super.start();

		startDate = new Date();
	}

	public boolean init() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		// Initialize SPARQL 1.1 processing service properties
		boolean propertiesFound = true;
		try {
			endpointProperties = new SPARQL11Properties("endpoint.jpar");
		} catch (NumberFormatException | InvalidKeyException | NoSuchElementException | NullPointerException
				| ClassCastException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | IOException e) {
			logger.warn("Open and modify JPAR file and run again the engine");
			propertiesFound = false;
		}

		// Initialize SPARQL 1.1 SE processing service properties
		try {
			engineProperties = new EngineProperties("engine.jpar");
		} catch (IllegalArgumentException | NoSuchElementException | IOException e) {
			logger.warn("Open and modify JPAR file and run again the engine");
			propertiesFound = false;
		}
		
		if (!propertiesFound) return false;

		// SPARQL 1.1 SE request processor
		processor = new Processor(endpointProperties);

		// SPARQL 1.1 SE request scheduler
		scheduler = new Scheduler(engineProperties, processor);

		// SPARQL 1.1 Protocol
		System.out
				.println("---------- SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)  ---------------");
		httpGate = new HttpServer(engineProperties, scheduler);

		// SPARQL 1.1 SE Protocol
		System.out.println("");
		System.out
				.println("------ SPARQL SE 1.1 Protocol (https://wot.arces.unibo.it/TR/sparql11-se-protocol/)  -----");
		wsServer = new WebsocketServer(engineProperties.getWsPort(),engineProperties.getSubscribePath(),engineProperties.getKeepAlivePeriod(), scheduler);
		
		httpsGate = new HttpsServer(engineProperties, scheduler, oauth);
		
		wssServer = new SecureWebsocketServer(engineProperties.getWssPort(),engineProperties.getSecurePath()+engineProperties.getSubscribePath(),engineProperties.getKeepAlivePeriod(), scheduler, oauth);

		return true;
	}

	@Override
	public Date getStartDate() {
		return startDate;
	}
}

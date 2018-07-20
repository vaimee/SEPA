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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.LogManager;

import com.nimbusds.jose.JOSEException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.dependability.AuthorizationManager;
import it.unibo.arces.wot.sepa.engine.dependability.DependabilityManager;

import it.unibo.arces.wot.sepa.engine.processing.Processor;

import it.unibo.arces.wot.sepa.engine.protocol.websocket.WebsocketServer;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpGate;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpsGate;
import it.unibo.arces.wot.sepa.engine.protocol.websocket.SecureWebsocketServer;

import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

/**
 * This class represents the SPARQL Subscription Broker (Core) of the SPARQL
 * Event Processing Architecture (SEPA)
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.9.5
 */

public class Engine implements EngineMBean {
	private static Engine engine;
	private static String version = "0.9.5";
	private EngineProperties properties = null;

	// Scheduler request queue
	//private final SchedulerQueue schedulerQueue = new SchedulerQueue();
	
	// Primitives scheduler/dispatcher
	private Scheduler scheduler = null;

	// Primitives scheduler/dispatcher
	private Processor processor = null;

	// SPARQL 1.1 Protocol handler
	private HttpGate httpGate = null;

	// SPARQL 1.1 SE Protocol handler
	private WebsocketServer wsServer;
	private SecureWebsocketServer wssServer;
	private HttpsGate httpsGate = null;
	private int wsShutdownTimeout = 5000;

	// Oauth 2.0 Authorization Server
	private AuthorizationManager oauth;

	// Dependability manager
	private DependabilityManager dependabilityMng;

	// JKS Credentials
	private String storeName = "sepa.jks";
	private String storePassword = "sepa2017";
	private String jwtAlias = "sepakey";
	private String jwtPassword = "sepa2017";
	private String serverCertificate = "sepacert";
	
	// Properties files
	private String engineJpar = "engine.jpar";
	private String endpointJpar = "endpoint.jpar";

	// Logging file name
	private void setLoggingFileName() {
		// Logging
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HH_mm_ss"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());
		System.setProperty("logFilename", nowAsISO);
		org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager
				.getContext(false);
		ctx.reconfigure();
	}

	private void printUsage() {
		System.out.println("Usage:");
		System.out.println("java [JMX] [JVM] [LOG4J] -jar SEPAEngine_X.Y.Z.jar [-help] [-engine=engine.jpar] [-endpoint=endpoint.jpar] [JKS OPTIONS]");
		System.out.println("Options: ");
		System.out.println("-engine : can be used to specify the JSON configuration parameters for the engine (default: engine.jpar)");
		System.out.println("-endpoint : can be used to specify the JSON configuration parameters for the endpoint (default: endpoint.jpar)");
		System.out.println("-help : to print this help");
		
		System.out.println("");
		System.out.println("JMX:");
		System.out.println("-Dcom.sun.management.config.file=jmx.properties : to enable JMX remote managment");
		System.out.println("");
		System.out.println("JVM:");
		System.out.println("-XX:+UseG1GC");
		System.out.println("");
		System.out.println("LOG4J");
		System.out.println("-Dlog4j.configurationFile=./log4j2.xml");
		System.out.println("");
		System.out.println("JKS OPTIONS:");
		System.out.println("-storename=<name> : file name of the JKS     (default: sepa.jks)");
		System.out.println("-storepwd=<pwd> : password of the JKS        (default: sepa2017)");
		System.out.println("-alias=<jwt> : alias for the JWT key         (default: sepakey)");
		System.out.println("-aliaspwd=<pwd> : password of the JWT key    (default: sepa2017)");
		System.out.println("-certificate=<crt> : name of the certificate (default: sepacert)");
	}

	private void parsingArgument(String[] args) throws PatternSyntaxException {
		String[] tmp;
		for (String arg : args) {
			if (arg.equals("-help")) {
				printUsage();
				return;
			}
			if (arg.startsWith("-")) {
				tmp = arg.split("=");
				switch (tmp[0]) {
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
				case "-engine":
					engineJpar = tmp[1];
					break;
				case "-endpoint":
					endpointJpar = tmp[1];
					break;
				default:
					break;
				}
			}
		}
	}

	public Engine(String[] args) throws SEPASecurityException, SEPAProtocolException {
		// Set logging file name with the current timestamp YYYYMMDD_HH_MM_SS
		setLoggingFileName();

		// Command arguments
		parsingArgument(args);

		System.out
				.println("##########################################################################################");
		System.out
				.println("# SEPA Broker                                                                            #");
		System.out
				.println("# Dynamic Linked Data & Web of Things Research - University of Bologna (Italy)           #");
		System.out
				.println("# Copyright (C) 2016-2018                                                                #");
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
				.println("# WEB: http://site.unibo.it/wot                                                          #");
		System.out.println("# WIKI: https://github.com/arces-wot/SEPA/wiki                                           #");
		System.out
				.println("##########################################################################################");

		// Beans
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		EngineBeans.setVersion(version);

		// Initialize SPARQL 1.1 SE processing service properties
		try {
			properties = new EngineProperties(engineJpar);
		} catch (SEPAPropertiesException e) {}

		EngineBeans.setEngineProperties(properties);
		
		SPARQL11Properties endpointProperties = null;
		try {
			endpointProperties = new SPARQL11Properties(endpointJpar );
		} catch (SEPAPropertiesException e2) {}

		// OAUTH 2.0 Authorization Manager
		if (properties.isSecure()) {
			try {
				oauth = new AuthorizationManager(storeName, storePassword, jwtAlias, jwtPassword, serverCertificate);
			} catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException
					| CertificateException | IOException | JOSEException e1) {
				System.err.println(e1.getLocalizedMessage());
				System.exit(1);
			}
		}

		// SPARQL 1.1 SE request scheduler
		scheduler = new Scheduler(properties);
		scheduler.start();

		// Dependability manager
		dependabilityMng = new DependabilityManager(scheduler.getSchedulerQueue());

		// SEPA Processor
		try {
			processor = new Processor(endpointProperties, properties, scheduler.getSchedulerQueue());
		} catch (SEPAProtocolException e1) {
			System.err.println(e1.getMessage());
			System.exit(1);
		}
		processor.start();

		// SPARQL protocol service
		int port = endpointProperties.getDefaultPort();
		String portS = "";
		if (port != -1)
			portS = String.format(":%d", port);

		String queryMethod = "";
		switch (endpointProperties.getQueryMethod()) {
		case POST:
			queryMethod = " (Method: POST)";
			break;
		case GET:
			queryMethod = " (Method: GET)";
			break;
		case URL_ENCODED_POST:
			queryMethod = " (Method: URL ENCODED POST)";
			break;
		}

		String updateMethod = "";
		switch (endpointProperties.getUpdateMethod()) {
		case POST:
			updateMethod = " (Method: POST)";
			break;
		case GET:
			updateMethod = " (Method: GET)";
			break;
		case URL_ENCODED_POST:
			updateMethod = " (Method: URL ENCODED POST)";
			break;
		}

		System.out.println("SPARQL 1.1 endpoint");
		System.out.println("----------------------");
		System.out.println("SPARQL 1.1 Query     | http://" + endpointProperties.getDefaultHost() + portS
				+ endpointProperties.getDefaultQueryPath() + queryMethod);
		System.out.println("SPARQL 1.1 Update    | http://" + endpointProperties.getDefaultHost() + portS
				+ endpointProperties.getUpdatePath() + updateMethod);
		System.out.println("----------------------");
		System.out.println("");

		// Protocol gates
		System.out.println("SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
		System.out.println("----------------------");
		try {
			if (!properties.isSecure())
				httpGate = new HttpGate(properties, scheduler);
			else
				httpsGate = new HttpsGate(properties, scheduler, oauth);
		} catch (SEPAProtocolException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}

		// Protocol gates
		System.out.println("----------------------");
		System.out.println("");
		System.out.println("SPARQL 1.1 SE Protocol (https://mml.arces.unibo.it/TR/sparql11-se-protocol/)");
		System.out.println("----------------------");

		if (!properties.isSecure()) {
			wsServer = new WebsocketServer(properties.getWsPort(), properties.getSubscribePath(), scheduler,
					dependabilityMng);
			wsServer.start();
			synchronized (wsServer) {
				try {
					wsServer.wait(5000);
				} catch (InterruptedException e) {
					throw new SEPAProtocolException(e);
				}
			}
		} else {
			wssServer = new SecureWebsocketServer(properties.getWssPort(),
					properties.getSecurePath() + properties.getSubscribePath(), scheduler, oauth, dependabilityMng);

			wssServer.start();
			synchronized (wssServer) {
				try {
					wssServer.wait(5000);
				} catch (InterruptedException e) {
					throw new SEPAProtocolException(e);
				}
			}
		}

		System.out.println("----------------------");

		// Welcome message
		System.out.println("");
		System.out.println("*****************************************************************************************");
		System.out.println(
				"*                      SEPA Broker Ver " + version + " is up and running                          *");
		System.out.println("*                                Let Things Talk!                                       *");
		System.out.println("*****************************************************************************************");
	}

	public static void main(String[] args) throws SEPASecurityException, SEPAProtocolException {
		engine = new Engine(args);

		// Attach CTRL+C hook
		Runtime.getRuntime().addShutdownHook(new EngineShutdownHook(engine));
	}

	public void shutdown() {
		System.out.println("Stopping...");

		if (!properties.isSecure()) {
			System.out.println("Stopping HTTP gate...");
			httpGate.shutdown();

			try {
				System.out.println("Stopping WS gate...");
				wsServer.stop(wsShutdownTimeout);
			} catch (InterruptedException e) {
				System.err.println(e.getMessage());
			}
		}

		if (properties.isSecure()) {
			System.out.println("Stopping HTTPS gate...");
			httpsGate.shutdown();

			try {
				System.out.println("Stopping WSS gate...");
				wssServer.stop(wsShutdownTimeout);
			} catch (InterruptedException e) {
				System.err.println(e.getMessage());
			}
		}

		System.out.println("Stopping Processor...");
		processor.interrupt();

		System.out.println("Stopped...bye bye :-)");
	}

	@Override
	public String getUpTime() {
		return EngineBeans.getUpTime();
	}

	@Override
	public void resetAll() {
		EngineBeans.resetAll();
	}

	@Override
	public String getVersion() {
		return EngineBeans.getVersion();
	}
	
	@Override
	public String getQueryPath() {
		return EngineBeans.getQueryPath();
	}
	
	@Override
	public String getUpdatePath() {
		return EngineBeans.getUpdatePath();
	}
	
	@Override
	public String getSubscribePath() {
		return EngineBeans.getSubscribePath();
	}

	@Override
	public String getSecurePath() {
		return EngineBeans.getSecurePath();
	}
	
	@Override
	public String getRegisterPath() {
		return EngineBeans.getRegisterPath();
	}
	
	@Override
	public String getTokenRequestPath() {
		return EngineBeans.getTokenRequestPath();
	}
	
	@Override
	public int getHttpPort() {
		return EngineBeans.getHttpPort();
	}
	
	@Override
	public int getHttpsPort() {
		return EngineBeans.getHttpsPort();
	}
	
	@Override
	public int getWsPort() {
		return EngineBeans.getWsPort();
	}
	
	@Override
	public int getWssPort() {
		return EngineBeans.getWssPort();
	}
	
	@Override
	public boolean getSecure() {
		return EngineBeans.getSecure();
	}
}

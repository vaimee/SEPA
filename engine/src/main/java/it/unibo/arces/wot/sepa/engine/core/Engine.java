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
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;
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

import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;

import it.unibo.arces.wot.sepa.engine.protocol.websocket.WebsocketServer;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpGate;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpsGate;
import it.unibo.arces.wot.sepa.engine.protocol.websocket.SecureWebsocketServer;

import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

/**
 * This class represents the SPARQL Subscription (SUB) Engine of the Semantic
 * Event Processing Architecture (SEPA)
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.7.7
 */

public class Engine extends Thread implements EngineMBean {
	// Properties, logging
	private static final Logger logger = LogManager.getLogger("Engine");

	// Primitives scheduler/dispatcher
	private Scheduler scheduler = null;
	private Thread schedulerThread = null;

	// SPARQL 1.1 Protocol handler
	private HttpGate httpGate = null;

	// SPARQL 1.1 SE Protocol handler
	private WebsocketServer wsServer;
	private SecureWebsocketServer wssServer;
	private HttpsGate httpsGate = null;
	private static final int wsShutdownTimeout = 5000;

	// Outh 2.0 Authorization Server
	private static AuthorizationManager oauth;

	// JKS Credentials
	private static String storeName = "sepa.jks";
	private static String storePassword = "sepa2017";
	private static String jwtAlias = "sepakey";
	private static String jwtPassword = "sepa2017";
	private static String serverCertificate = "sepacert";

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("java [JMX] [JVM] -jar SEPAEngine_X.Y.Z.jar [OPTIONS]");
		System.out.println("");
		System.out.println("JMX:");
		System.out.println("-Dcom.sun.management.config.file=jmx.properties : to enable JMX remote managment");
		System.out.println("JVM:");
		System.out.println("-Xmx500m -Xms500m -Xmn250m -XX:+UseConcMarkSweepGC -XX:ParallelCMSThreads=2");
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
				default:
					break;
				}
			}
		}
	}

	public static void main(String[] args) throws MalformedObjectNameException, InstanceAlreadyExistsException,
			MBeanRegistrationException, NotCompliantMBeanException, UnrecoverableKeyException, KeyManagementException,
			InvalidKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
			IllegalArgumentException, NoSuchElementException, NullPointerException, ClassCastException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, URISyntaxException {

		// Command arguments
		parsingArgument(args);

		System.out
				.println("##########################################################################################");
		System.out
				.println("# SEPA Engine Ver 0.7.7  Copyright (C) 2016-2017                                         #");
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

		// Engine creation and initialization
		Engine engine = new Engine();

		// OAUTH 2.0 Authorization Manager
		try {
			oauth = new AuthorizationManager(storeName, storePassword, jwtAlias, jwtPassword, serverCertificate);
		} catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException | JOSEException e1) {
			logger.fatal(e1.getLocalizedMessage());
			System.exit(1);
		}

		// Initialize
		engine.init();

		// Starting main engine thread
		engine.start();

		// Attach CTRL+C hook
		Runtime.getRuntime().addShutdownHook(new EngineShutdownHook(engine));
	}

	public Engine() {
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		this.setName("SEPA Engine");
	}

	public boolean init() throws MalformedObjectNameException, InstanceAlreadyExistsException,
			MBeanRegistrationException, NotCompliantMBeanException, UnrecoverableKeyException, KeyManagementException,
			KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InvalidKeyException,
			IllegalArgumentException, NoSuchElementException, NullPointerException, ClassCastException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, URISyntaxException {

		// Initialize SPARQL 1.1 SE processing service properties
		EngineProperties properties;
		try {
			properties = new EngineProperties("engine.jpar");
		} catch (IllegalArgumentException | NoSuchElementException | IOException e) {
			logger.warn("Open and modify JPAR file and run again the engine");
			return false;
		}

		// SPARQL 1.1 SE request scheduler
		scheduler = new Scheduler(properties);

		// SPARQL 1.1 Protocol
		httpGate = new HttpGate(properties, scheduler);
		httpGate.init();

		// SPARQL 1.1 SE Protocol
		httpsGate = new HttpsGate(properties, scheduler, oauth);
		httpsGate.init();

		wsServer = new WebsocketServer(properties.getWsPort(), properties.getSubscribePath(), scheduler,
				properties.getKeepAlivePeriod(), properties.getTimeout());
		wssServer = new SecureWebsocketServer(properties.getWssPort(),
				properties.getSecurePath() + properties.getSubscribePath(), scheduler, oauth,
				properties.getKeepAlivePeriod(), properties.getTimeout());

		EngineBeans.setEngineProperties(properties);

		return true;
	}

	@Override
	public void run() {
		// Scheduler
		schedulerThread = new Thread(scheduler);
		schedulerThread.setName("SEPA scheduler");
		
		schedulerThread.start();
		synchronized (schedulerThread) {
			try {
				schedulerThread.wait();
			} catch (InterruptedException e) {
				logger.warn(e.getMessage());
			}
		}
		
		// Protocol gates
		System.out.println("SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
		System.out.println("----------------------");

		httpGate.start();
		synchronized (httpGate) {
			try {
				httpGate.wait();
			} catch (InterruptedException e) {
				logger.warn(e.getMessage());
			}
		}

		System.out.println("----------------------");
		System.out.println("");
		System.out.println("SPARQL 1.1 SE Protocol (https://wot.arces.unibo.it/TR/sparql11-se-protocol/)");
		System.out.println("----------------------");

		httpsGate.start();
		synchronized (httpsGate) {
			try {
				httpsGate.wait();
			} catch (InterruptedException e) {
				logger.warn(e.getMessage());
			}
		}

		wsServer.start();
		synchronized (wsServer) {
			try {
				wsServer.wait();
			} catch (InterruptedException e) {
				return;
			}
		}

		wssServer.start();
		synchronized (wssServer) {
			try {
				wssServer.wait();
			} catch (InterruptedException e) {
				return;
			}
		}
		System.out.println("----------------------");

		// Welcome message
		System.out.println("");
		System.out.println("*****************************************************************************************");
		System.out.println("*                      SEPA Engine Ver 0.7.7 is up and running                          *");
		System.out.println("*                                Let Things Talk!                                       *");
		System.out.println("*****************************************************************************************");
	}

	public void shutdown() {
		logger.info("Stopping...");

		logger.info("Stopping scheduler...");
		schedulerThread.interrupt();

		logger.info("Stopping HTTP gate...");
		httpGate.interrupt();

		logger.info("Stopping HTTPS gate...");
		httpsGate.interrupt();

		try {
			logger.info("Stopping WS gate...");
			wsServer.stop(wsShutdownTimeout);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage());
		}
		try {
			logger.info("Stopping WSS gate...");
			wssServer.stop(wsShutdownTimeout);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage());
		}

		logger.info("Stopped...bye bye :-)");
	}

	@Override
	public String getUpTime() {
		return EngineBeans.getUpTime();
	}

	@Override
	public String getURL_Query() {
		return EngineBeans.getQueryURL();
	}

	@Override
	public String getURL_Update() {
		return EngineBeans.getUpdateURL();
	}

	@Override
	public String getURL_SecureQuery() {
		return EngineBeans.getSecureQueryURL();
	}

	@Override
	public String getURL_SecureUpdate() {
		return EngineBeans.getSecureUpdateURL();
	}

	@Override
	public String getURL_Registration() {
		return EngineBeans.getRegistrationURL();
	}

	@Override
	public String getURL_TokenRequest() {
		return EngineBeans.getTokenRequestURL();
	}

	@Override
	public long getTimeout() {
		return EngineBeans.getTimeout();
	}

	@Override
	public void setTimeout(long l) {
		EngineBeans.setTimeout(l);
	}

	@Override
	public long getKeepalive() {
		return EngineBeans.getKeepalive();
	}

	@Override
	public void setKeepalive(long l) {
		EngineBeans.setKeepalive(l);
	}
}

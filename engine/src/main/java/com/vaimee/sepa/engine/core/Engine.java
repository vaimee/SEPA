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
package com.vaimee.sepa.engine.core;

import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;

import com.vaimee.sepa.engine.bean.EngineBeans;
import com.vaimee.sepa.engine.bean.SEPABeans;
import com.vaimee.sepa.engine.dependability.Dependability;
import com.vaimee.sepa.engine.dependability.DependabilityMonitor;
import com.vaimee.sepa.engine.gates.http.HttpGate;
import com.vaimee.sepa.engine.gates.http.HttpsGate;
import com.vaimee.sepa.engine.gates.websocket.SecureWebsocketServer;
import com.vaimee.sepa.engine.gates.websocket.WebsocketServer;
import com.vaimee.sepa.engine.processing.Processor;
import com.vaimee.sepa.engine.scheduling.Scheduler;
import com.vaimee.sepa.logging.Logging;

/**
 * This class represents the SPARQL Subscription Broker (Core) of the SPARQL
 * Event Processing Architecture (SEPA)
 *
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.13.0
 */

public class Engine implements EngineMBean {
	private final static String version = "0.99.99";
	private EngineProperties properties = null;

	// Primitives scheduler/dispatcher
	private Scheduler scheduler = null;

	// Primitives scheduler/dispatcher
	private Processor processor = null;

	// SPARQL 1.1 Protocol handler
	private HttpGate httpGate = null;

	// SPARQL 1.1 SE Protocol handler
	private WebsocketServer wsServer = null;
	private HttpsGate httpsGate = null;

	public Engine(String[] args) {
		System.out
				.println("##########################################################################################");
		System.out
				.println("#                           ____  _____ ____   _                                         #");
		System.out.println(
				"#                          / ___|| ____|  _ \\ / \\                                        #");
		System.out.println(
				"#                          \\___ \\|  _| | |_) / _ \\                                       #");
		System.out
				.println("#                           ___) | |___|  __/ ___ \\                                      #");
		System.out.println(
				"#                          |____/|_____|_| /_/   \\_\\                                     #");
		System.out
				.println("#                                                                                        #");
		System.out
				.println("#                     SPARQL Event Processing Architecture                               #");
		System.out
				.println("#                                                                                        #");
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
				.println("#                                                                                        #");
		System.out
				.println("# @prefix git: <https://github.com/> .                                                   #");
		System.out
				.println("# @prefix dc: <http://purl.org/dc/elements/1.1/> .                                       #");
		System.out
				.println("#                                                                                        #");
		System.out
				.println("# git:vaimee/sepa dc:title 'SEPA' ;                                                      #");
		System.out
				.println("# dc:creator git:lroffia ;                                                               #");
		System.out
				.println("# dc:contributor git:relu91 ;                                                            #");
		System.out
				.println("# dc:contributor git:GregorioMonari ;                                                    #");
		System.out
				.println("# dc:format <https://java.com> ;                                                         #");
		System.out
				.println("# dc:publisher <https://github.com> .                                                    #");
		System.out
				.println("##########################################################################################");
		System.out.println("");

		// Beans
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		EngineBeans.setVersion(version);

		// Dependability monitor
		new DependabilityMonitor();

		try {
			// Initialize SPARQL 1.1 SE processing service properties
			properties = new EngineProperties(args);

			EngineBeans.setEngineProperties(properties);		

			// SPARQL 1.1 SE request scheduler
			scheduler = new Scheduler(properties);
			Dependability.setScheduler(scheduler);

			// SEPA Processor
			processor = new Processor(properties, scheduler);

			// SPARQL protocol service
			int port = properties.getEndpointProperties().getPort();
			String portS = "";
			if (port != -1)
				portS = String.format(":%d", port);

			String queryMethod = "";
			switch (properties.getEndpointProperties().getQueryMethod()) {
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
			switch (properties.getEndpointProperties().getUpdateMethod()) {
			case POST:
				updateMethod = " (Method: POST)";
				break;
			case URL_ENCODED_POST:
				updateMethod = " (Method: URL ENCODED POST)";
				break;
			}

			System.out.println("SPARQL 1.1 endpoint");
			System.out.println("----------------------");
			System.out.println("SPARQL 1.1 Query     | " + properties.getEndpointProperties().getProtocolScheme() + "://"
					+ properties.getEndpointProperties().getHost() + portS + properties.getEndpointProperties().getQueryPath() + queryMethod);
			System.out.println("SPARQL 1.1 Update    | " + properties.getEndpointProperties().getProtocolScheme() + "://"
					+ properties.getEndpointProperties().getHost() + portS + properties.getEndpointProperties().getUpdatePath() + updateMethod);
			System.out.println("----------------------");
			System.out.println("");

			// SPARQL 1.1 protocol gates
			System.out.println("SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
			System.out.println("----------------------");

			if (!properties.isSecure())
				httpGate = new HttpGate(properties, scheduler);
			else
				httpsGate = new HttpsGate(properties, scheduler);

			// SPARQL 1.1 SE protocol gates
			System.out.println("----------------------");
			System.out.println("");
			System.out.println("SPARQL 1.1 SE Protocol (http://mml.arces.unibo.it/TR/sparql11-se-protocol.html)");
			System.out.println("----------------------");

			if (!properties.isSecure()) {
				wsServer = new WebsocketServer(properties.getWsPort(), properties.getSubscribePath(), scheduler);
			} else {
				wsServer = new SecureWebsocketServer(443, properties.getSubscribePath(), scheduler);
			}

			// Start all
			scheduler.start();
			processor.start();
			wsServer.start();

			synchronized (wsServer) {
				wsServer.wait(5000);
			}

			System.out.println("----------------------");

			// Welcome message
			System.out.println("");
			System.out.println(
					"*****************************************************************************************");
			System.out.println(
					"*                        SEPA Broker Ver is up and running                              *");
			System.out.println(
					"*                        Let Things Talk and Data Be Free!                              *");
			System.out.println(
					"*****************************************************************************************");
			System.out.print("Version " + version);

			Logging.init();

		} catch (SEPASecurityException | IllegalArgumentException | SEPAProtocolException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (SEPAPropertiesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws SEPASecurityException, SEPAProtocolException {
		// Attach CTRL+C hook
		Runtime.getRuntime().addShutdownHook(new EngineShutdownHook(new Engine(args)));
	}

	public void shutdown() throws InterruptedException {
		System.out.println("Stopping...");

		if (httpGate != null) {
			System.out.println("Stopping HTTP gate...");
			httpGate.shutdown();
		}

		if (httpsGate != null) {
			System.out.println("Stopping HTTPS gate...");
			httpsGate.shutdown();
		}

		if (wsServer != null) {
			System.out.println("Stopping WebSocket gate...");
			wsServer.stop(properties.getWsShutdownTimeout());
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
	public int getWsPort() {
		return EngineBeans.getWsPort();
	}

	@Override
	public boolean getSecure() {
		return EngineBeans.getSecure();
	}

	@Override
	public String getSSLCertificate() {
		return EngineBeans.getSSLCertificate();
	}

	@Override
	public void refreshSSLCertificate() {
		EngineBeans.refreshSSLCertificate();
	}
}

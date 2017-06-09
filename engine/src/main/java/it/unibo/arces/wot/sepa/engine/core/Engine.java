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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.engine.beans.SEPABeans;
import it.unibo.arces.wot.sepa.engine.processing.Processor;
import it.unibo.arces.wot.sepa.engine.protocol.HTTPGate;
import it.unibo.arces.wot.sepa.engine.protocol.HTTPSGate;
import it.unibo.arces.wot.sepa.engine.protocol.WSGate;
import it.unibo.arces.wot.sepa.engine.protocol.WSSGate;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

/**
 * This class represents the SPARQL Subscription (SUB) Engine of the Semantic Event Processing Architecture (SEPA)
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.6
* */

public class Engine extends Thread implements EngineMBean {
	//Properties, logging
	private EngineProperties engineProperties = null;
	private SPARQL11Properties endpointProperties = null;
	
	//JMX properties
	private static Date startDate; 
	
	//Primitives scheduler/dispatcher
	private Scheduler scheduler = null;
	
	//Primitives processor
	private Processor processor = null;
	
	//SPARQL 1.1 Protocol handler
	private HTTPGate httpGate = null;
	private HTTPSGate httpsGate = null;
	
	//SPARQL 1.1 SE Protocol handler
	private WSGate websocketApp;
	private WSSGate secureWebsocketApp;
	
	private AuthorizationManager am = new AuthorizationManager("sepa.jks","*sepa.jks*","SepaKey","*SepaKey*","SepaCertificate");
	
	public static void main(String[] args) throws IllegalArgumentException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, FileNotFoundException, NoSuchElementException, IOException {
		//Set Grizzly logging level
		java.util.logging.Logger grizzlyNetworkListener = java.util.logging.Logger.getLogger("org.glassfish.grizzly.http.server.NetworkListener");
		java.util.logging.Logger grizzlyHttpServer = java.util.logging.Logger.getLogger("org.glassfish.grizzly.http.server.HttpServer");
		grizzlyNetworkListener.setLevel(Level.SEVERE);
		grizzlyHttpServer.setLevel(Level.SEVERE);
		
		System.out.println("##########################################################################################");
		System.out.println("# SEPA Engine Ver 0.7.0  Copyright (C) 2016-2017                                         #");
		System.out.println("# University of Bologna (Italy)                                                          #");
		System.out.println("#                                                                                        #");
		System.out.println("# This program comes with ABSOLUTELY NO WARRANTY                                         #");                                    
		System.out.println("# This is free software, and you are welcome to redistribute it under certain conditions #");
		System.out.println("# GNU GENERAL PUBLIC LICENSE, Version 3, 29 June 2007                                    #");
		System.out.println("#                                                                                        #");
		System.out.println("# GitHub: https://github.com/arces-wot/sepa                                              #");
		System.out.println("# Web: http://wot.arces.unibo.it                                                         #");
		System.out.println("##########################################################################################");
		System.out.println("");		
		System.out.println("--------------------------------- Maven dependencies -------------------------------------");
		System.out.println("<!-- https://mvnrepository.com/artifact/org.apache.httpcomponents/httpcore-nio -->"
				+ "\n<!-- https://mvnrepository.com/artifact/org.apache.httpcomponents/httpcore -->"
				
				+ "\n<!-- https://mvnrepository.com/artifact/org.glassfish.tyrus.bundles/tyrus-standalone-client -->"
				+ "\n<!-- https://mvnrepository.com/artifact/org.glassfish.grizzly/grizzly-websockets-server -->"

				+ "\n<!-- https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-api -->"
				+ "\n<!-- https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core -->"
				
				+ "\n<!-- https://mvnrepository.com/artifact/com.google.code.gson/gson -->"
				
				+ "\n<!-- https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt -->"
				+ "\n<!-- https://mvnrepository.com/artifact/commons-io/commons-io -->");
		
		//Engine creation and initialization
		Engine engine = new Engine();
		engine.init();
	
		System.out.println("--------------------- SPARQL 1.1 Secure Event Protocol handlers --------------------------");
		
		//Starting main engine thread
		engine.start();
		
		//Welcome message
		System.out.println("");
		System.out.println("*****************************************************************************************");
		System.out.println("*                      SEPA Engine Ver 0.7.0 is up and running                          *");
		System.out.println("*                                 Let Things Talk                                       *");
		System.out.println("*****************************************************************************************");
	}
	
	public Engine() {
		SEPABeans.registerMBean("SEPA:type=Engine",this);		
	}
	
	@Override
	public void start() {
		
		this.setName("SEPA Engine");
		
		//Scheduler
		scheduler.start();
		
		//SPARQL 1.1 Protocol handlers
		httpGate.start();
		httpsGate.start();
		
		//SPARQL 1.1 SE Protocol handler for WebSocket based subscriptions
		websocketApp.start();
		secureWebsocketApp.start();
		
		super.start();
		
		startDate = new Date();
	}
	
	public boolean init() throws IllegalArgumentException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, FileNotFoundException, NoSuchElementException, IOException {			
		//Initialize SPARQL 1.1 processing service properties
		endpointProperties = new SPARQL11Properties("endpoint.jpar");
		
		//Initialize SPARQL 1.1 SE processing service properties
		engineProperties = new EngineProperties("engine.jpar");
		
		//SPARQL 1.1 SE request processor
		processor = new Processor(endpointProperties);
		
		//SPARQL 1.1 SE request scheduler
		scheduler = new Scheduler(engineProperties,processor);
		
		//SPARQL 1.1 Protocol handlers
		httpGate = new HTTPGate(engineProperties,scheduler);
		httpsGate = new HTTPSGate(engineProperties,scheduler,am);
		
		//SPARQL 1.1 SE Protocol handler for WebSocket based subscriptions
		websocketApp = new WSGate(engineProperties,scheduler);
		secureWebsocketApp = new WSSGate(engineProperties,scheduler,am);
		        
        return true;
	}

	@Override
	public Date getStartDate() {
		return startDate;
	}
}

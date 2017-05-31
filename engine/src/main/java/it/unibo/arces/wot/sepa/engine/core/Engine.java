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

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
	private static final Logger logger = LogManager.getLogger("Engine");
	
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
		System.out.println("##########################################################################################");
		System.out.println("# SEPA Engine Ver 0.6  Copyright (C) 2016-2017                                           #");
		System.out.println("# University of Bologna (Italy)                                                          #");
		System.out.println("#                                                                                        #");
		System.out.println("# This program comes with ABSOLUTELY NO WARRANTY                                         #");                                    
		System.out.println("# This is free software, and you are welcome to redistribute it under certain conditions #");
		System.out.println("# GNU GENERAL PUBLIC LICENSE, Version 3, 29 June 2007                                    #");
		System.out.println("#                                                                                        #");
		System.out.println("# GitHub: https://github.com/vaimee/sepatools                                            #");
		System.out.println("# Web: http://wot.arces.unibo.it                                                         #");
		System.out.println("##########################################################################################");
		System.out.println("");		
		System.out.println("Dependencies");
		System.out.println("com.google.code.gson          2.8.0       Apache 2.0");
		System.out.println("com.nimbusds                  4.34.2      The Apache Software License, Version 2.0");
		System.out.println("commons-io                    2.5         Apache License, Version 2.0");
		System.out.println("commons-logging               1.2         The Apache Software License, Version 2.0");
		System.out.println("org.apache.httpcomponents     4.5.3       Apache License, Version 2.0");
		System.out.println("org.apache.httpcomponents     4.4.6       Apache License, Version 2.0");
		System.out.println("org.apache.logging.log4j      2.8.1       Apache License, Version 2.0");
		System.out.println("org.bouncycastle              1.56        Bouncy Castle Licence");
		System.out.println("org.eclipse.paho              1.1.1       Eclipse Public License - Version 1.0");
		System.out.println("org.glassfish.grizzly         2.3.30      CDDL+GPL");
		System.out.println("org.glassfish.tyrus.bundles   1.13.1      Dual license consisting of the CDDL v1.1 and GPL v2");
		System.out.println("org.jdom                      2.0.6       Similar to Apache License but with the acknowledgment clause removed");
		System.out.println("");
		
		//Engine creation and initialization
		Engine engine = new Engine();
		engine.init();
		
		//Starting main engine thread
		engine.start();
	}
	
	public Engine() {
		SEPABeans.registerMBean("SEPA:type=Engine",this);		
	}
	
	@Override
	public void start() {
		
		this.setName("SEPA Engine");
		logger.info("SUB Engine starting...");	
		
		//Scheduler
		scheduler.start();
		
		//SPARQL 1.1 Protocol handlers
		httpGate.start();
		httpsGate.start();
		
		//SPARQL 1.1 SE Protocol handler for WebSocket based subscriptions
		websocketApp.start();
		secureWebsocketApp.start();
		
		super.start();
		logger.info("SUB Engine started");	
		System.out.println("");	
		
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

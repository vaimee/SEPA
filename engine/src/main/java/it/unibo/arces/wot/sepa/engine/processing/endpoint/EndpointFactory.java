/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import static it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.PROTOCOL_SCHEMA_EX_JENA;
import static it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.PROTOCOL_SCHEMA_REMOTE;
import static it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.PROTOCOL_SCHEMA_STD_JENA;

import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.logging.Logging;

/**
 *
 * @author Lorenzo
 */
public class EndpointFactory {
	
	private static SPARQLEndpoint firstDataset;
	private static SPARQLEndpoint secondDataset;
	
	/*
	 * As default the EndpointFactory will return THE instance of the correct Endpoint
	 * following the protocolSchema, and in case of a not "remote" endpoint using the firstDataset specification.
	 */
	public static SPARQLEndpoint getInstance(final String protocolSchema) {
		if(firstDataset==null) {
			Logging.logger.trace("EndpointFactory using: " + protocolSchema);
			switch(protocolSchema.toLowerCase().trim()) {
			default:
			case PROTOCOL_SCHEMA_STD_JENA:
				firstDataset = new JenaInMemoryEndpoint(EngineBeans.getFirstDatasetMode(),EngineBeans.getFirstDatasetPath()); 
				break;
			case PROTOCOL_SCHEMA_EX_JENA:
				firstDataset = new SjenarEndpoint(EngineBeans.getFirstDatasetMode(),EngineBeans.getFirstDatasetPath());
				break;
			case PROTOCOL_SCHEMA_REMOTE:
				firstDataset = new RemoteEndpoint();
				break;
			}
		}
		return firstDataset;
	}

	/*
	 * This is a specific case, used for the LUTT double dataset system
	 * will return a compliant endpoint (for the use of LUTT) 
	 * setted as SECOND DATASET
	 * WARN: if LUTT are disable, the first dataset will be returned instead the second
	 */
	public static SPARQLEndpoint getInstanceSecondStore(final String protocolSchema) {
		if(EngineBeans.isLUTTEnabled()) {
			if(secondDataset==null) {
				Logging.logger.trace("EndpointFactory[second dataset] using: SjenarEndpointDoubleStore");
				switch(protocolSchema.toLowerCase().trim()) {
				default:
				case PROTOCOL_SCHEMA_STD_JENA:
					secondDataset = new JenaInMemoryEndpoint(EngineBeans.getSecondDatasetMode(),EngineBeans.getSecondDatasetPath()); 
					break;
				case PROTOCOL_SCHEMA_EX_JENA:
					secondDataset = new SjenarEndpoint(EngineBeans.getSecondDatasetMode(),EngineBeans.getSecondDatasetPath());
					break;
				}
			}
			return secondDataset;
		}else{
			Logging.logger.warn("EndpointFactory: request for an istance for the second dataset with LUTT disabled! (this not make sense, return the first dataset)");
			return getInstance(protocolSchema);
		}
	}

	public static void reset() {
		firstDataset=null;
		secondDataset=null;
	}

}

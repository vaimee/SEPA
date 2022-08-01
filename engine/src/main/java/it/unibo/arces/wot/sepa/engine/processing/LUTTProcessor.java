/*
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
 * */

package it.unibo.arces.wot.sepa.engine.processing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpStatus;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.bean.ProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.QueryProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.UpdateProcessorBeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.EndpointFactory;
import it.unibo.arces.wot.sepa.engine.processing.subscriptions.SPUManager;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequestWithQuads;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.logging.Logging;

public class LUTTProcessor extends Processor {
	
	// SPARQL Second Processors (the first one is inherited)
	private final UpdateProcessor updateProcessor2;
	private final QueryProcessor queryProcessor2;


	/*
	 * LUTTProcessor add a second dataset in order to implement the dual dataset system for the LUTT usage
	 * INFO: we will have 2 UpdateProcessor and 2 QueryProcessor, but the original QueryProcessor of the first dataset
	 * will not be used
	 */
	public LUTTProcessor(SPARQL11Properties endpointProperties, EngineProperties properties, Scheduler scheduler)
			throws IllegalArgumentException, SEPAProtocolException, SEPASecurityException {
		super(endpointProperties, properties, scheduler);
		// TODO Auto-generated constructor stub
		updateProcessor2 = new UpdateProcessor(endpointProperties,EndpointFactory.getInstanceSecondStore(getEndpointHost()));
		queryProcessor2 = new QueryProcessor(endpointProperties,EndpointFactory.getInstanceSecondStore(getEndpointHost()));
		if(EngineBeans.isLUTTEnabled()) {
			Logging.logger.warn("LUTTProcessor was instantiated even the LUTT are disabled!");
		}
	}


	public synchronized Response processUpdate(InternalUpdateRequest update) {
		InternalUpdateRequest preRequest = update;
		if(!update.isAclRequest()) {
			//if there are not SPU we need anyway extract the AR for build the INSERT-DELETE
			try {
					//JENAR-AR 		(done)	
					preRequest = ARQuadsAlgorithm.extractJenaARQuads(update, updateProcessor);
					//alghoritm AR 	(...pending)
					//preRequest = ARQuadsAlgorithm.extractARQuads(update, queryProcessor);
			} catch (SEPAProcessingException | SPARQL11ProtocolException | SEPASparqlParsingException | SEPASecurityException | IOException e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "update_processing", e.getMessage());
			}
			if (spuManager.doUpdateARQuadsExtraction(update)) {
				if(preRequest instanceof InternalUpdateRequestWithQuads ) 
				{
					Response voidResponse =((InternalUpdateRequestWithQuads)preRequest).getResponseNothingToDo();
					if(voidResponse==null) {
						spuManager.subscriptionsProcessingPreUpdate(preRequest);
					}else {
						//THE UPDATE DOSEN'T AFFECT THE STORE
						//we can skipp all the remain process.
						spuManager.setNoActiveSPU(); //remove all active SPU
						return voidResponse;
					}
				}else {
					// PRE-UPDATE processing (standard)
					spuManager.subscriptionsProcessingPreUpdate(preRequest);
				}
			}	
		}
	

		// Endpoint UPDATE
		Response ret;
		try {
			//is is not a ACL request INSERT DATA and DELETE DATA update built with the AR
			//note: an ACL request can be resolved by updateProcessor2 o updateProcessor, because in case
			//both will use the same dataset for the ACL request
			ret = updateProcessor2.process(preRequest);
		} catch (SEPASecurityException | IOException e) {
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "sparql11endpoint", e.getMessage());
		}

		// STOP processing?
		if (ret.isError()) {
			Logging.logger.error("*** UPDATE ENDPOINT PROCESSING FAILED *** " + ret);
			spuManager.abortSubscriptionsProcessing();
			return ret;
		}

		// POST-UPDATE processing
		spuManager.subscriptionsProcessingPostUpdate(ret);

		return ret;
	}



	
	public Response processQueryOnSecondStore(InternalQueryRequest query) throws SEPASecurityException, IOException {
		return queryProcessor2.process(query);
	}
	
	public Response processQueryOnFirstStore(InternalQueryRequest query) throws SEPASecurityException, IOException {
		return queryProcessor2.process(query);
	}

	
	
}

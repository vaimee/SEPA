package it.unibo.arces.wot.sepa.engine.processing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

class UpdateProcessingThread extends Thread {
	private static final Logger logger = LogManager.getLogger();
	
	private final Processor processor;
	private final Scheduler scheduler;
	private final UpdateProcessor endpoint;
	
	public UpdateProcessingThread(Processor processor) {
		setName("SEPA-Update-Processor");
		
		this.processor = processor;
		this.scheduler = processor.getScheduler();
		this.endpoint = processor.getUpdateProcessor();
	}

	public void run() {
		while (processor.isRunning()) {
			ScheduledRequest request;
			try {
				request = scheduler.waitUpdateRequest();
			} catch (InterruptedException e) {
				return;
			}

			// Update request
			InternalUpdateRequest update = (InternalUpdateRequest)request.getRequest();
			
			// Notify update (not reliable)
			if (!processor.isUpdateReilable()) scheduler.addResponse(request.getToken(),new UpdateResponse("Processing: "+update));
						
			// PRE-processing update request
			InternalUpdateRequest preRequest;
			try {
				preRequest = endpoint.preProcess(update);
			} catch (SEPAProcessingException e2) {
				logger.error("Update pre-processing failed: "+update+" exception: "+e2.getMessage());
				ErrorResponse errorResponse = new ErrorResponse(502, "Endpoint update pre-processing failed: "+update, e2.getMessage());
				scheduler.addResponse(request.getToken(),errorResponse);
				continue;
			}
			
			// PRE-processing subscriptions (endpoint not yet updated)
			try {
				processor.preUpdateProcessing(preRequest);
			} catch (SEPAProcessingException e) {
				logger.error("PreUpdateProcessing failed: "+e.getMessage());
				ErrorResponse errorResponse = new ErrorResponse(500, "PreUpdateProcessing failed: "+update, e.getMessage());
				scheduler.addResponse(request.getToken(),errorResponse);
				continue;
			}
			
			// Processing UPDATE
			Response ret;
			try {
				ret = endpoint.process(preRequest);
			} catch (InterruptedException e1) {
				logger.error("Processing failed: "+e1.getMessage());
				ErrorResponse errorResponse = new ErrorResponse(502, "Endpoint update processing failed: "+update, e1.getMessage());
				scheduler.addResponse(request.getToken(),errorResponse);
				continue;
			}

			// Notify update result
			if (processor.isUpdateReilable()) scheduler.addResponse(request.getToken(),ret);

			// Subscription processing (post update)
			try {
				processor.postUpdateProcessing(ret);
			} catch (SEPAProcessingException e) {
				logger.warn("Post update processing failed: "+e.getMessage());
				continue;
			}
		}
	}
}

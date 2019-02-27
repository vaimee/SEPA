package it.unibo.arces.wot.sepa.engine.processing;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

class UpdateProcessingThread extends Thread {
	private final Processor processor;
	
	public UpdateProcessingThread(Processor processor) {
		this.processor = processor;
		setName("SEPA-Update-Processor");
	}

	public void run() {
		while (processor.isRunning()) {
			ScheduledRequest request;
			try {
				request = processor.getScheduler().waitUpdateRequest();
			} catch (InterruptedException e) {
				return;
			}

			// Update request
			InternalUpdateRequest update = (InternalUpdateRequest)request.getRequest();
			
			// Notify update (not reliable)
			if (!processor.isUpdateReilable()) processor.getScheduler().addResponse(request.getToken(),new UpdateResponse("Processing: "+update));
						
			// PRE-processing update request
			InternalUpdateRequest preRequest = processor.getUpdateProcessor().preProcess(update);
			
			// PRE-processing subscriptions (pre update)
			processor.preUpdateProcessing(preRequest);
			
			// Processing update
			Response ret = processor.getUpdateProcessor().process(preRequest);

			// Notify update result
			if (processor.isUpdateReilable()) processor.getScheduler().addResponse(request.getToken(),ret);

			// Subscription processing (post update)
			processor.postUpdateProcessing(ret);
		}
	}
}

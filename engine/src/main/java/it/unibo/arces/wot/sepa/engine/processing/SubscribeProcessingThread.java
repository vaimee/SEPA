package it.unibo.arces.wot.sepa.engine.processing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUnsubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

class SubscribeProcessingThread extends Thread {
	private static final Logger logger = LogManager.getLogger();

	private final Processor processor;

	public SubscribeProcessingThread(Processor processor) {
		this.processor = processor;

		setName("SEPA-Subscribe-Processor");
	}
	
	public void run() {
		while (processor.isRunning()) {
			try {
				// Wait request...
				ScheduledRequest request = processor.getScheduler().waitSubscribeUnsubscribeRequest();
				logger.debug(">> " + request);

				// Process request
				Response response = null;
				if (request.isSubscribeRequest()) {
					response = processor.subscribe((InternalSubscribeRequest) request.getRequest());
				}else if (request.isUnsubscribeRequest()) {
					String sid = ((InternalUnsubscribeRequest) request.getRequest()).getSID();
					String gid = ((InternalUnsubscribeRequest) request.getRequest()).getGID();
					response = processor.unsubscribe(sid,gid);
				}
				logger.debug("<< " + response);

				// Send back response
				processor.getScheduler().addResponse(request.getToken(), response);

			} catch (SEPAProcessingException e) {
				logger.warn(e.getMessage());
				continue;
			} catch (InterruptedException e) {
				logger.warn(e.getMessage());
				return;
			}
		}
	}
}

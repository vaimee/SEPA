package it.unibo.arces.wot.sepa.engine.processing;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

class QueryProcessingThread extends Thread{
	private final Processor processor;
	
	public QueryProcessingThread(Processor processor) {
		this.processor = processor; 
		setName("SEPA-Query-Processing");
	}
	
	public void run() {
		while(processor.isRunning()) {
			ScheduledRequest request;
			try {
				request = processor.getScheduler().waitQueryRequest();
			} catch (InterruptedException e) {
				return;
			}
			
			InternalQueryRequest query = (InternalQueryRequest) request.getRequest();
			Response ret = processor.getQueryProcessor().process(query);
			
			processor.getScheduler().addResponse(request.getToken(),ret);
		}
	}	
}

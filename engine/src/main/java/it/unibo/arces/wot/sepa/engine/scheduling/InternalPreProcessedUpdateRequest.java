package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;

public class InternalPreProcessedUpdateRequest extends InternalUpdateRequest{

	ErrorResponse retErrorResponse = null;
	
	public InternalPreProcessedUpdateRequest(ErrorResponse errorResponse) {
		super(null, null, null, null);
		retErrorResponse = errorResponse;
	}
	
	public InternalPreProcessedUpdateRequest(InternalUpdateRequest req) {
		super(req.getSparql(), req.getDefaultGraphUri(), req.getNamedGraphUri(), req.getClientAuthorization());
	}
	
	public boolean preProcessingFailed() {
		return retErrorResponse != null;
	}
	
	public ErrorResponse getErrorResponse() {
		return retErrorResponse;
	}

}

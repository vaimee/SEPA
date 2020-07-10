package it.unibo.arces.wot.sepa.engine.scheduling;

import org.apache.jena.query.QueryException;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;

public class InternalPreProcessedUpdateRequest extends InternalUpdateRequest{
	ErrorResponse retErrorResponse = null;
	
	public InternalPreProcessedUpdateRequest(ErrorResponse errorResponse) throws QueryException {
		super(null, null, null, null);
		retErrorResponse = errorResponse;
	}
	
	public InternalPreProcessedUpdateRequest(InternalUpdateRequest req) throws QueryException  {
		super(req.getSparql(), req.getDefaultGraphUri(), req.getNamedGraphUri(), req.getClientAuthorization());
	}
	
	public boolean preProcessingFailed() {
		return retErrorResponse != null;
	}
	
	public ErrorResponse getErrorResponse() {
		return retErrorResponse;
	}

}

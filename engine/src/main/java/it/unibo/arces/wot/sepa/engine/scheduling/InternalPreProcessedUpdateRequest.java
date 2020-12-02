package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;

public class InternalPreProcessedUpdateRequest extends InternalUpdateRequest{
	ErrorResponse retErrorResponse = null;
	
	public InternalPreProcessedUpdateRequest(ErrorResponse errorResponse) {
		super(null, null, null, null);
		retErrorResponse = errorResponse;
	}
	
	public InternalPreProcessedUpdateRequest(InternalUpdateRequest req) {
		// TODO: added and removed algorithm goes here
		// 1) "sparql" member should be updated with insert/delete data
		// 2) Added and removed quads can be represented as ARBindingsResults with the convention : ?g ?s ?p ?o
		/*
		 * ++++++++++++++++++++++++++++++++ CREATING NEW BRANCH ON GITHUB
		 */
		super(req.getSparql(), req.getDefaultGraphUri(), req.getNamedGraphUri(), req.getClientAuthorization());
	}
	
	public boolean preProcessingFailed() {
		return retErrorResponse != null;
	}
	
	public ErrorResponse getErrorResponse() {
		return retErrorResponse;
	}

}

package it.unibo.arces.wot.sepa.engine.scheduling;

import java.util.Set;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.engine.processing.lutt.LUTT;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;

public class InternalUpdateRequestWithQuads extends InternalUpdateRequest {

	//private ARBindingsResults quadsArBindingsResults;
	private LUTT hitter;
	private Response responseNothingToDo= null;
	

	public InternalUpdateRequestWithQuads(String sparql, Set<String> defaultGraphUri, Set<String> namedGraphUri,
			ClientAuthorization auth,LUTT hitter) throws SPARQL11ProtocolException, SEPASparqlParsingException {
		super(sparql, defaultGraphUri, namedGraphUri, auth);
		this.hitter=hitter;
		//quadsArBindingsResults = quads;
	}
	
	public LUTT getHitterLUTT() {
		return this.hitter;
		//return quadsArBindingsResults;
	}
	
	public Response getResponseNothingToDo() {
		return responseNothingToDo;
	}

	public void setResponseNothingToDo(Response responseNothingToDo) {
		this.responseNothingToDo = responseNothingToDo;
	}

}

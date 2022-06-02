package it.unibo.arces.wot.sepa.engine.scheduling;

import java.util.Set;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;

public class InternalUpdateRequestWithQuads extends InternalUpdateRequest {

	//private ARBindingsResults quadsArBindingsResults;
	private LUTT hitter;
	private Response responseNothingToDo= null;
	private String rollbackSparql = null;

	
	public InternalUpdateRequestWithQuads(String sparql, Set<String> defaultGraphUri, Set<String> namedGraphUri,
			ClientAuthorization auth,ARBindingsResults quads) throws SPARQL11ProtocolException, SEPASparqlParsingException {
		super(sparql, defaultGraphUri, namedGraphUri, auth);
		quadsArBindingsResults = quads;
	}
	
	
	public InternalUpdateRequestWithQuads(String sparql,String rollback, Set<String> defaultGraphUri, Set<String> namedGraphUri,
			ClientAuthorization auth,LUTT hitter) throws SPARQL11ProtocolException, SEPASparqlParsingException {
		super(sparql, defaultGraphUri, namedGraphUri, auth);
		this.hitter=hitter;
		rollbackSparql=rollback;
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

	public boolean hasRollBakc() {
		return this.rollbackSparql!=null;
	}
	
	public String getRollback() {
		return this.rollbackSparql;
	}
}

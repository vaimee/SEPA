package it.unibo.arces.wot.sepa.engine.scheduling;

import java.util.Set;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;

public class InternalUpdateRequestWithQuads extends InternalUpdateRequest {

	private ARBindingsResults quadsArBindingsResults;
	
	public InternalUpdateRequestWithQuads(String sparql, Set<String> defaultGraphUri, Set<String> namedGraphUri,
			ClientAuthorization auth,ARBindingsResults quads) throws SPARQL11ProtocolException, SEPASparqlParsingException {
		super(sparql, defaultGraphUri, namedGraphUri, auth);
		quadsArBindingsResults = quads;
	}
	
	public ARBindingsResults getARBindingsResults() {
		return quadsArBindingsResults;
	}

}

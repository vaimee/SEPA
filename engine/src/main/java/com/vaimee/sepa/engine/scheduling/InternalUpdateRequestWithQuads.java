package com.vaimee.sepa.engine.scheduling;

import java.util.Set;

import com.vaimee.sepa.api.commons.exceptions.SEPASparqlParsingException;
import com.vaimee.sepa.api.commons.security.ClientAuthorization;
import com.vaimee.sepa.api.commons.sparql.ARBindingsResults;
import com.vaimee.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;

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

package com.vaimee.sepa.engine.processing;

import java.util.ArrayList;

import com.vaimee.sepa.commons.exceptions.SEPAProcessingException;
import com.vaimee.sepa.commons.exceptions.SEPASparqlParsingException;
import com.vaimee.sepa.commons.sparql.ARBindingsResults;
import com.vaimee.sepa.commons.sparql.BindingsResults;
import com.vaimee.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import com.vaimee.sepa.engine.scheduling.InternalUpdateRequest;
import com.vaimee.sepa.engine.scheduling.InternalUpdateRequestWithQuads;

public class ARQuadsAlgorithm {

	public static InternalUpdateRequestWithQuads extractARQuads(InternalUpdateRequest update, QueryProcessor queryProcessor) throws SEPAProcessingException, SPARQL11ProtocolException, SEPASparqlParsingException {
		ARBindingsResults quads = new ARBindingsResults(new BindingsResults(new ArrayList<>(),new ArrayList<>()), new BindingsResults(new ArrayList<>(),new ArrayList<>()));
		return new InternalUpdateRequestWithQuads(update.getSparql(), update.getDefaultGraphUri(), update.getNamedGraphUri(), update.getClientAuthorization(), quads);
	}

}

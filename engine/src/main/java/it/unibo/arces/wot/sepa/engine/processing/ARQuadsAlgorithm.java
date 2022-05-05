package it.unibo.arces.wot.sepa.engine.processing;

import java.util.ArrayList;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequestWithQuads;

public class ARQuadsAlgorithm {

	public static InternalUpdateRequestWithQuads extractARQuads(InternalUpdateRequest update, QueryProcessor queryProcessor) throws SEPAProcessingException, SPARQL11ProtocolException, SEPASparqlParsingException {
		ARBindingsResults quads = new ARBindingsResults(new BindingsResults(new ArrayList<>(),new ArrayList<>()), new BindingsResults(new ArrayList<>(),new ArrayList<>()));
		return new InternalUpdateRequestWithQuads(update.getSparql(), update.getDefaultGraphUri(), update.getNamedGraphUri(), update.getClientAuthorization(), quads);
	}

}

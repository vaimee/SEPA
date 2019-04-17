package it.unibo.arces.wot.sepa.commons.jsonld.algorithm;

import java.util.concurrent.Callable;

import org.apache.commons.rdf.api.Dataset;

import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdInput;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdOptions;

public class DeserializationAlgorithm implements Callable<Dataset> {
	private final JsonLdInput input;
	private final JsonLdOptions options;
	
	public DeserializationAlgorithm(JsonLdInput input, JsonLdOptions options) {
		this.input = input;
		this.options = options;
	}
	
	public DeserializationAlgorithm(JsonLdInput input) {
		this(input,new JsonLdOptions());
	}
	
	@Override
	public Dataset call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}

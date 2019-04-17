package it.unibo.arces.wot.sepa.commons.jsonld.algorithm;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.rdf.api.Dataset;

import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdDictionary;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdOptions;

public class SerializationAlgorithm implements Callable<List<JsonLdDictionary>> {

	private final Dataset input;
	private final JsonLdOptions options;
	
	public SerializationAlgorithm(Dataset input, JsonLdOptions options) {
		this.input = input;
		this.options = options;
	}
	
	public SerializationAlgorithm(Dataset input) {
		this(input,new JsonLdOptions());
	}
	
	@Override
	public List<JsonLdDictionary> call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}

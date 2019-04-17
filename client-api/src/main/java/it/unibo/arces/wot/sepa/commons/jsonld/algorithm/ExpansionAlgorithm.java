package it.unibo.arces.wot.sepa.commons.jsonld.algorithm;

import java.util.List;
import java.util.concurrent.Callable;

import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdDictionary;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdInput;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdOptions;

public class ExpansionAlgorithm implements Callable<List<JsonLdDictionary>> {

	private final JsonLdInput input;
	private final JsonLdOptions options;
	
	public ExpansionAlgorithm(JsonLdInput input,JsonLdOptions options) {
		this.input = input;
		this.options = options;
	}
	
	public ExpansionAlgorithm(JsonLdInput input) {
		this(input,new JsonLdOptions());
	}
	
	@Override
	public List<JsonLdDictionary> call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}

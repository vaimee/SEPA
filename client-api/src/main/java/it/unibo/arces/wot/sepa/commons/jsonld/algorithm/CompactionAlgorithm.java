package it.unibo.arces.wot.sepa.commons.jsonld.algorithm;

import java.util.concurrent.Callable;

import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdDictionary;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdInput;
import it.unibo.arces.wot.sepa.commons.jsonld.JsonLdOptions;
import it.unibo.arces.wot.sepa.commons.jsonld.context.JsonLdContext;

public class CompactionAlgorithm implements Callable<JsonLdDictionary> {

	private final JsonLdInput input;
	private final JsonLdContext context;
	private final JsonLdOptions options;
	
	public CompactionAlgorithm(JsonLdInput input, JsonLdContext context, JsonLdOptions options) {
		this.input = input;
		this.context = context;
		this.options = options;
	}
	
	public CompactionAlgorithm(JsonLdInput input, JsonLdContext context) {
		this(input,context,new JsonLdOptions());
	}
	
	@Override
	public JsonLdDictionary call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}

package it.unibo.arces.wot.sepa.commons.jsonld;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.rdf.api.Dataset;

import it.unibo.arces.wot.sepa.commons.jsonld.algorithm.CompactionAlgorithm;
import it.unibo.arces.wot.sepa.commons.jsonld.algorithm.ExpansionAlgorithm;
import it.unibo.arces.wot.sepa.commons.jsonld.algorithm.FlatteringAlgorithm;
import it.unibo.arces.wot.sepa.commons.jsonld.algorithm.SerializationAlgorithm;
import it.unibo.arces.wot.sepa.commons.jsonld.context.JsonLdContext;
import it.unibo.arces.wot.sepa.commons.jsonld.algorithm.DeserializationAlgorithm;

/**
 * Implement the JSON LD Processor interface
 * 
 * @see https://www.w3.org/TR/json-ld11-api/#the-jsonldprocessor-interface
 * */
public class JsonLdProcessor {
	private static final ExecutorService exe = Executors.newSingleThreadExecutor();
	
	public static Future<JsonLdDictionary> compact(JsonLdInput input, JsonLdContext context, JsonLdOptions options) {
		return exe.submit(new CompactionAlgorithm(input, context, options));
	}

	public static Future<List<JsonLdDictionary>> expand(JsonLdInput input, JsonLdOptions options) {
		return exe.submit(new ExpansionAlgorithm(input,options));
	}

	public static Future<JsonLdDictionary> flatten(JsonLdInput input, JsonLdContext context, JsonLdOptions options) {
		return exe.submit(new FlatteringAlgorithm(input,context,options));
	}

	public static Future<List<JsonLdDictionary>> fromRdf(Dataset input, JsonLdOptions options) {
		return exe.submit(new SerializationAlgorithm(input,options));
	}

	public static Future<Dataset> toRdf(JsonLdInput input, JsonLdOptions options) {
		return exe.submit(new DeserializationAlgorithm(input,options));
	}
}

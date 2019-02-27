package it.unibo.arces.wot.sepa.engine.protocol.ngsild;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.jsonldjava.JsonLdBlankNode;
import org.apache.commons.rdf.jsonldjava.JsonLdIRI;
import org.apache.commons.rdf.jsonldjava.JsonLdRDF;
import org.apache.commons.rdf.jsonldjava.experimental.JsonLdParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.engine.bean.NgisLdHandlerBeans;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUQRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.timing.Timings;

public class NgsiLdRdfMapper implements ResponseHandler {
	protected static final Logger logger = LogManager.getLogger();

	public static final String ngsiLdEntitiesGraph = "http://uri.etsi.org/ngsi-ld/Entities";
	public static final String ngsiLdContextGraph = "http://uri.etsi.org/ngsi-ld/Context";

	public static final String ngsiLdContextPrimitive = "http://uri.etsi.org/ngsi-ld/hasContextPrimitive";
	public static final String ngsiLdContextElement = "http://uri.etsi.org/ngsi-ld/hasContextElement";
	public static final String ngsiLdContextTerm = "http://uri.etsi.org/ngsi-ld/hasContextTerm";
	public static final String ngsiLdContextIri = "http://uri.etsi.org/ngsi-ld/hasContextIri";

	private final JsonLdIRI contextGraph;
	private final JsonLdIRI hasContextPrimitive;
	private final JsonLdIRI hasContextElement;
	private final JsonLdIRI hasContextTerm;
	private final JsonLdIRI hasContextIri;
	private final JsonLdIRI rdfType;

	private final String rdfTypeIri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

	private final JsonLdRDF ld;

	protected final Scheduler scheduler;

	private Boolean updateResult = new Boolean(false);
	private JsonObject queryResult = new JsonObject();

	protected final NgisLdHandlerBeans jmx;

	private enum ACTION {
		STORE_CONTEXT, GET_CONTEXT, ENTITY_EXISTS
	};

	private ACTION action = ACTION.STORE_CONTEXT;

	public NgsiLdRdfMapper(Scheduler scheduler, NgisLdHandlerBeans jmx) {
		ld = new JsonLdRDF();
		contextGraph = ld.createIRI(ngsiLdContextGraph);

		hasContextPrimitive = ld.createIRI(ngsiLdContextPrimitive);
		hasContextElement = ld.createIRI(ngsiLdContextElement);
		hasContextTerm = ld.createIRI(ngsiLdContextTerm);
		hasContextIri = ld.createIRI(ngsiLdContextIri);

		rdfType = ld.createIRI(rdfTypeIri);

		this.scheduler = scheduler;
		this.jmx = jmx;
	}

	public Dataset fromJsonLd(JsonObject in) throws IllegalStateException, IllegalArgumentException,
			InterruptedException, ExecutionException, TimeoutException, IOException {
		return fromJsonLd(in, 5);
	}

	public Dataset fromJsonLd(JsonObject in, int timeout) throws IllegalStateException, IllegalArgumentException,
			InterruptedException, ExecutionException, TimeoutException, IOException {

		// Parse input stream as JSON-LD
		InputStream json = new ByteArrayInputStream(in.toString().getBytes(StandardCharsets.UTF_8));
		Dataset ldDataset = ld.createDataset();
		new JsonLdParser().base(ngsiLdEntitiesGraph).source(json).contentType(RDFSyntax.JSONLD).target(ldDataset)
				.parse().get(timeout, TimeUnit.SECONDS);

		return ldDataset;
	}

	public JsonObject toJsonLd(Dataset in, JsonObject frame) throws IOException {
		// Options
		JsonLdOptions opts = new JsonLdOptions();
		opts.setUseNativeTypes(true);
		opts.setPruneBlankNodeIdentifiers(true);
		opts.setOmitGraph(true);

		// NTRIPLES
		String ntriples = ntriples(in);

		// Parse NQUADS input string
		Object jsonObject = JsonLdProcessor.fromRDF(ntriples, opts);

		// Parse JSON-LD frame from input stream
		InputStream json = new ByteArrayInputStream(frame.toString().getBytes(StandardCharsets.UTF_8));
		final Object frameObj = JsonUtils.fromInputStream(json);

		// Framing algorithm
		Object framed = JsonLdProcessor.frame(jsonObject, frameObj, opts);

		return new JsonParser().parse(framed.toString()).getAsJsonObject();
	}

	public String getEntityUri(Dataset in) {
		for (Triple triple : in.getGraph().iterate()) {
			if (triple.getPredicate().getIRIString().equals("rdf:type")
					|| triple.getPredicate().getIRIString().equals(rdfTypeIri)) {
				return triple.getSubject().toString().replaceAll("<", "").replaceAll(">", "");
			}
		}
		return null;
	}

	/**
	 * 
	 * "@context": { "name": "http://schema.org/name", "image": {
	 * "@id":"http://schema.org/image", "@type": "@id" }}
	 * 
	 * "@context": "https://json-ld.org/contexts/person.jsonld"
	 * 
	 * "@context": { "@vocab": "http://schema.org/", "knows": {"@type": "@id"} }
	 * 
	 * "@context": [ "https://json-ld.org/contexts/person.jsonld",
	 * "https://json-ld.org/contexts/place.jsonld",
	 * {"title":"http://purl.org/dc/terms/title"} ]
	 * 
	 */
	private Dataset extractContext(String entityId, JsonElement context) {
		Dataset ldDataset = ld.createDataset();
		JsonLdIRI entity = ld.createIRI(entityId);

		if (context.isJsonArray()) {
			JsonArray arr = (JsonArray) context.getAsJsonArray();
			for (JsonElement elem : arr) {
				if (elem.isJsonPrimitive()) {
					ldDataset.add(contextGraph, entity, hasContextPrimitive, ld.createLiteral(elem.getAsString()));
				} else if (elem.isJsonObject()) {
					JsonObject obj = (JsonObject) elem.getAsJsonObject();
					for (Entry<String, JsonElement> subElem : obj.entrySet()) {
						JsonLdBlankNode bn = ld.createBlankNode();
						ldDataset.add(contextGraph, entity, hasContextElement, bn);
						ldDataset.add(contextGraph, bn, hasContextTerm, ld.createLiteral(subElem.getKey()));
						ldDataset.add(contextGraph, bn, hasContextIri,
								ld.createLiteral(subElem.getValue().getAsString()));
					}
				}
			}
		} else if (context.isJsonPrimitive()) {
			ldDataset.add(contextGraph, entity, hasContextPrimitive, ld.createLiteral(context.getAsString()));
		} else if (context.isJsonObject()) {
			JsonObject obj = (JsonObject) context.getAsJsonObject();
			for (Entry<String, JsonElement> subElem : obj.entrySet()) {
				JsonLdBlankNode bn = ld.createBlankNode();
				ldDataset.add(contextGraph, entity, hasContextElement, bn);
				ldDataset.add(contextGraph, bn, hasContextTerm, ld.createLiteral(subElem.getKey()));
				ldDataset.add(contextGraph, bn, hasContextIri, ld.createLiteral(subElem.getValue().getAsString()));
			}
		}

		return ldDataset;
	}

	private JsonObject query(String sparql, ACTION act) {
		// Scheduler QUERY request
		action = act;
		InternalUQRequest sepaRequest = new InternalQueryRequest(sparql, NgsiLdRdfMapper.ngsiLdContextGraph,
				NgsiLdRdfMapper.ngsiLdContextGraph);
		Timings.log(sepaRequest);
		ScheduledRequest req = scheduler.schedule(sepaRequest, this);

		// Request not scheduled
		if (req == null) {
			logger.error("Out of tokens");
			jmx.outOfTokens();
			return new JsonObject();
		}

		// Wait for response
		try {
			synchronized (queryResult) {
				queryResult.wait();
			}
		} catch (InterruptedException e) {
			logger.error(e);
		}

		return queryResult;
	}

	private boolean update(String sparql, ACTION act) {
		// Scheduler UPDATE request
		action = act;
		InternalUQRequest sepaRequest = new InternalUpdateRequest(sparql, NgsiLdRdfMapper.ngsiLdContextGraph,
				NgsiLdRdfMapper.ngsiLdContextGraph);
		Timings.log(sepaRequest);
		ScheduledRequest req = scheduler.schedule(sepaRequest, this);

		// Request not scheduled
		if (req == null) {
			logger.error("Out of tokens");
			jmx.outOfTokens();
			return false;
		}

		// Wait for response
		try {
			synchronized (updateResult) {
				updateResult.wait();
			}
		} catch (InterruptedException e) {
			logger.error(e);
		}

		return updateResult;
	}

	public boolean storeEntityFrame(String entityId, String type, JsonElement context) {
		Dataset entityContext = extractContext(entityId, context);
		entityContext.add(contextGraph, ld.createIRI(entityId), rdfType, ld.createIRI(type));

		String sparql = "INSERT DATA { GRAPH <" + NgsiLdRdfMapper.ngsiLdContextGraph + "> {" + ntriples(entityContext)
				+ "}}";

		return update(sparql, ACTION.STORE_CONTEXT);
	}

	/**
	 * { "@type" : "...", "@context" : ... }
	 */
	public JsonObject getEntityFrame(String entityId) {
		String construct = "SELECT ?type ?primitive ?term ?iri FROM <" + ngsiLdContextGraph
				+ "> WHERE {{?id rdf:type ?type} UNION {?id <" + ngsiLdContextPrimitive + "> ?primitive} UNION {?id <"
				+ ngsiLdContextElement + "> ?element . ?element <" + ngsiLdContextTerm + "> ?term . ?element <"
				+ ngsiLdContextIri + "> ?iri} }";

		construct = construct.replace("?id", entityId);

		return query(construct, ACTION.GET_CONTEXT);
	}

	public boolean entityExists(String entityId) {
		String ask = "ASK {<"+entityId+"> ?p ?o}";

		JsonObject ret = query(ask, ACTION.ENTITY_EXISTS);

		return ret.getAsBoolean();
	}

	public String ntriples(Dataset in) {
		String nquads = "";

		// Serialize the results as NQUADS (NTRIPLES as default graph is used)
		for (Triple triple : in.getGraph().iterate()) {
			nquads += triple.getSubject().ntriplesString() + " " + triple.getPredicate().ntriplesString() + " "
					+ triple.getObject().ntriplesString() + " .\r\n";
		}
		return nquads;
	}

	@Override
	public void sendResponse(Response response) throws SEPAProtocolException {
		logger.debug(action + " " + response);

		switch (action) {
		case ENTITY_EXISTS:

			break;
		case STORE_CONTEXT:
			if (response.isError())
				updateResult = false;
			else
				updateResult = true;
			synchronized (updateResult) {
				updateResult.notify();
			}
			break;
		case GET_CONTEXT:
			queryResult = new JsonObject();
			if (!response.isError()) {
				QueryResponse results = (QueryResponse) response;
				for (Bindings bindings : results.getBindingsResults().getBindings()) {
					if (bindings.getValue("?type") != null)
						queryResult.add("@type", new JsonPrimitive(bindings.getValue("?type")));
				}
			}
			synchronized (queryResult) {
				queryResult.notify();
			}
			break;
		}

	}
}

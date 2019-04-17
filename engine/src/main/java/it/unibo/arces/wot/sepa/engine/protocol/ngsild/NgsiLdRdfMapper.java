package it.unibo.arces.wot.sepa.engine.protocol.ngsild;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.jsonldjava.core.Context;
import com.github.jsonldjava.core.JsonLdApi;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.core.RDFDataset.Node;
import com.github.jsonldjava.core.RDFDataset.Quad;
import com.github.jsonldjava.core.RDFDatasetUtils;
import com.github.jsonldjava.utils.JsonUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
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

	private static final String ngsiLdEntitiesGraph = "http://uri.etsi.org/ngsi-ld/Entities";
	private static final String ngsiLdContextGraph = "http://uri.etsi.org/ngsi-ld/Context";

	private static final String ngsiLdContextPrimitive = "http://uri.etsi.org/ngsi-ld/hasContextPrimitive";
	private static final String ngsiLdContextElement = "http://uri.etsi.org/ngsi-ld/hasContextElement";
	private static final String ngsiLdContextTerm = "http://uri.etsi.org/ngsi-ld/hasContextTerm";
	private static final String ngsiLdContextIri = "http://uri.etsi.org/ngsi-ld/hasContextIri";

	private static final String rdfTypeIri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

	protected final Scheduler scheduler;
	protected final NgisLdHandlerBeans jmx;

	protected Response response;

	protected NgsiLdError lastError = NgsiLdError.InternalError;

	public NgsiLdRdfMapper(Scheduler scheduler, NgisLdHandlerBeans jmx) {
		this.scheduler = scheduler;
		this.jmx = jmx;
	}

	private List<String> getRemoteContext(JsonElement context) {
		List<String> m = new ArrayList<String>();

		if (context == null)
			return m;

		if (context.isJsonArray()) {
			JsonArray arr = (JsonArray) context.getAsJsonArray();
			for (JsonElement elem : arr) {
				if (elem.isJsonPrimitive())
					m.add(elem.getAsString());
			}
		} else if (context.isJsonPrimitive())
			m.add(context.getAsString());

		return m;
	}

	private Context getLocalContext(JsonElement context) {
		Map<String, Object> m = new HashMap<String, Object>();

		if (context == null)
			return new Context(m);

		if (context.isJsonArray()) {
			JsonArray arr = (JsonArray) context.getAsJsonArray();
			for (JsonElement elem : arr) {
				if (elem.isJsonPrimitive())
					continue;
				if (elem.isJsonObject()) {
					JsonObject obj = (JsonObject) elem.getAsJsonObject();
					for (Entry<String, JsonElement> subElem : obj.entrySet()) {
						if (subElem.getValue().isJsonPrimitive())
							m.put(subElem.getKey(), subElem.getValue().getAsString());
						else
							m.put(subElem.getKey(), getLocalContext(subElem.getValue()));
					}
				}
			}
		} else if (context.isJsonObject()) {
			JsonObject obj = (JsonObject) context.getAsJsonObject();
			for (Entry<String, JsonElement> subElem : obj.entrySet()) {
				if (subElem.getValue().isJsonPrimitive())
					m.put(subElem.getKey(), subElem.getValue().getAsString());
				else
					m.put(subElem.getKey(), getLocalContext(subElem.getValue()));
			}
		}

		return new Context(m);
	}

	/**
	 * Converts from a JSON LD object into a RDF graph
	 */
	private RDFDataset fromJsonLd(JsonObject in) {
		// JSON-LD JAVA
		Map<String, Object> jsonLd = null;
		try {
			jsonLd = (Map<String, Object>) JsonUtils.fromString(in.toString());
		} catch (IOException e) {
			logger.error(e.getMessage());
			lastError = NgsiLdError.BadRequestData;
			lastError.setTitle("Failed to parse JSON-LD");
			lastError.setDetail(e.getMessage());
			return null;
		}

		JsonLdOptions options = new JsonLdOptions();
		options.setProcessingMode(JsonLdOptions.JSON_LD_1_1);
		//
		// String localContextString = null;
		// try {
		// localContextString = new String(Files.readAllBytes(Paths.get("td.jsonld")));
		// } catch (IOException e) {
		// logger.error(e.getMessage());
		// }
		// DocumentLoader dl = new DocumentLoader();
		// dl.addInjectedDoc("https://www.w3.org/ns/td.jsonld", localContextString);
		// options.setDocumentLoader(dl);

		// Context localContext = getLocalContext(in.get("@context"));
		// List<String> remoteContext = getRemoteContext(in.get("@context"));
		//
		// Context context = new Context();
		// context.parse(localContext,remoteContext);

		// RDFDataset rdf = new JsonLdApi(jsonLd,options).toRDF();
		// RDFDataset rdf = api.toRDF();

		RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(jsonLd, options);
		logger.debug(rdf.toString());

		return rdf;

		// Model model = ModelFactory.createDefaultModel();

		// RDF4J
		// org.eclipse.rdf4j.rio.RDFParser rdfParser =
		// Rio.createParser(org.eclipse.rdf4j.rio.RDFFormat.JSONLD);
		// try {
		// org.eclipse.rdf4j.model.Model results = Rio.parse(in, "",
		// org.eclipse.rdf4j.rio.RDFFormat.JSONLD);
		// return model;
		// } catch (RDFParseException | UnsupportedRDFormatException | IOException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }

		// JENA
		// RDFParser.fromString(jsonld.toString()).lang(Lang.JSONLD).parse(model);
		// return model;

		// try {
		// Dataset ds = new JsonLdRDF().createDataset();
		// new
		// JsonLdParser().source(jsonld.toString()).contentType(RDFSyntax.JSONLD).target(ds).parse().get(30,
		// TimeUnit.SECONDS);
		// } catch (IllegalStateException | IllegalArgumentException |
		// InterruptedException | ExecutionException
		// | TimeoutException | IOException e1) {
		// logger.error(e1.getMessage());
		// lastError = NgsiLdError.InvalidRequest;
		// lastError.setTitle("Failed to convert from JSON");
		// lastError.setDetail(e1.getMessage());
		// return null;
		// }

		// JENA
		// try{
		// InputStream in = new
		// ByteArrayInputStream(jsonld.toString().getBytes(StandardCharsets.UTF_8));
		// RDFDataMgr.read(model, in, null, Lang.JSONLD);
		// return model;
		// }
		// catch(org.apache.jena.riot.RiotException e) {
		// logger.error(e.getMessage());
		// lastError = NgsiLdError.InvalidRequest;
		// lastError.setTitle("Failed to convert from JSON");
		// lastError.setDetail(e.getMessage());
		// return null;
		// }

	}

	/**
	 * Takes an RDF dataset as input and convert it into a JSON LD object according
	 * to the JSON LD frame
	 */

	private JsonObject toJsonLd(RDFDataset rdf, JsonObject frame) {
		JsonLdOptions options = new JsonLdOptions();
		options.setPruneBlankNodeIdentifiers(true);
		options.setOmitGraph(true);

		// Convert to JSON-LD
		final Object in = new JsonLdApi(options).fromRDF(rdf, true);

		// Read frame
		Object frm = null;
		try {
			frm = JsonUtils.fromString(frame.getAsJsonObject().toString());
		} catch (IOException e) {
			logger.error(e.getMessage());
			lastError = NgsiLdError.BadRequestData;
			lastError.setTitle("Failed to parse JSON-LD frame");
			lastError.setDetail(e.getMessage());
			return null;

		}

		String jsonld = null;

		// Framing
		Object out = JsonLdProcessor.frame(in, frm, options);

		try {
			jsonld = JsonUtils.toString(out);
		} catch (IOException e) {
			logger.error(e.getMessage());
			lastError = NgsiLdError.BadRequestData;
			lastError.setTitle("Failed to serialize JSON-LD object as string");
			lastError.setDetail(e.getMessage());
			return null;
		}

		// Return as JsonObject
		JsonObject jsonldObj = new JsonParser().parse(jsonld).getAsJsonObject();

		// Return original context
		jsonldObj.remove("@context");
		jsonldObj.add("@context", frame.get("@context"));

		return jsonldObj;
	}

	private JsonObject toJsonLd2(RDFDataset rdf, JsonObject frame) {
		JsonLdOptions options = new JsonLdOptions();
		options.setPruneBlankNodeIdentifiers(true);
		options.setOmitGraph(true);

		// Convert to JSON-LD
		final Object in = new JsonLdApi(options).fromRDF(rdf, true);

		// Read frame
		Object frm = null;
		try {
			frm = JsonUtils.fromString(frame.getAsJsonObject().toString());
		} catch (IOException e) {
			logger.error(e.getMessage());
			lastError = NgsiLdError.BadRequestData;
			lastError.setTitle("Failed to parse JSON-LD frame");
			lastError.setDetail(e.getMessage());
			return null;

		}

		String jsonld = null;

		// Framing
		Object out = JsonLdProcessor.frame(in, frm, options);

		try {
			jsonld = JsonUtils.toString(out);
		} catch (IOException e) {
			logger.error(e.getMessage());
			lastError = NgsiLdError.BadRequestData;
			lastError.setTitle("Failed to serialize JSON-LD object as string");
			lastError.setDetail(e.getMessage());
			return null;
		}

		// Return as JsonObject
		JsonObject jsonldObj = new JsonParser().parse(jsonld).getAsJsonObject();

		return jsonldObj;
	}

	private JsonObject toJsonLdJena(Model entity, JsonObject frame) {
		OutputStream out = new ByteArrayOutputStream();

		// Frame
		JsonLDWriteContext ctx = new JsonLDWriteContext();
		ctx.setFrame(frame.toString());

		// Serialize
		DatasetGraph g = DatasetFactory.wrap(entity).asDatasetGraph();
		RDFWriter w = RDFWriter.create().format(RDFFormat.JSONLD_FRAME_PRETTY).source(g).context(ctx).build();
		w.output(out);

		// Post-processing
		JsonObject postPro = new JsonParser().parse(out.toString()).getAsJsonObject();
		JsonObject graph = postPro.getAsJsonArray("@graph").get(0).getAsJsonObject();
		JsonElement pruned = postProcessingJsonLdGraph(graph);
		pruned.getAsJsonObject().add("@context", frame.get("@context"));
		return pruned.getAsJsonObject();
	}

	private JsonElement postProcessingJsonLdGraph(JsonElement obj) {
		JsonElement pruned = null;

		if (obj.isJsonObject()) {
			pruned = new JsonObject();
			for (Entry<String, JsonElement> elem : obj.getAsJsonObject().entrySet()) {
				if (elem.getKey().equals("id")) {
					if (elem.getValue().getAsString().startsWith("_:"))
						continue;
					else
						pruned.getAsJsonObject().add("@id", elem.getValue());
				} else if (elem.getKey().equals("type")) {
					pruned.getAsJsonObject().add("@type", elem.getValue());
				} else if (elem.getValue().isJsonPrimitive())
					pruned.getAsJsonObject().add(elem.getKey(), elem.getValue());
				else if (elem.getValue().isJsonArray()) {
					pruned.getAsJsonObject().add(elem.getKey(), new JsonArray());
					for (JsonElement arrEl : elem.getValue().getAsJsonArray()) {
						pruned.getAsJsonObject().getAsJsonArray(elem.getKey()).add(postProcessingJsonLdGraph(arrEl));
					}
				} else
					pruned.getAsJsonObject().add(elem.getKey(), postProcessingJsonLdGraph(elem.getValue()));
			}
		} else if (obj.isJsonArray()) {
			pruned = new JsonArray();
			for (JsonElement elem : obj.getAsJsonArray()) {
				pruned.getAsJsonArray().add(postProcessingJsonLdGraph(elem));
			}
		} else
			return obj;

		return pruned;
	}

	/**
	 * Serializes the RDF graph as NTRIPLES according to
	 * https://www.w3.org/TR/n-triples/
	 */
	private String nTriples(RDFDataset in) {
		return RDFDatasetUtils.toNQuads(in);
		// String triples = "";
		// for (String gn : in.graphNames()) {
		// for (Quad q : in.getQuads(gn)) {
		// triples += q.getSubject().getValue()+" "+q.getPredicate().getValue() + " " +
		// q.getObject().getValue() + " . ";
		// }
		// }
		// return triples;
	}
	// private String nTriples(Model in) {
	// OutputStream out = new ByteArrayOutputStream();
	// RDFDataMgr.write(out, in, Lang.NTRIPLES);
	//
	// return out.toString();
	// }

	/**
	 * Build an RDF graph from the bindings results of a construct {?subject
	 * ?predicate ?object}
	 */
	private RDFDataset buildRdfGraph(List<Bindings> bindings) {
		RDFDataset model = new RDFDataset();

		for (Bindings binding : bindings) {
			Node sub = null;
			Node pred = new RDFDataset.IRI(binding.getValue("predicate"));
			Node obj = null;

			// Subject
			try {
				if (binding.isBNode("subject")) {
					if (!binding.getValue("subject").startsWith("_:"))
						sub = new RDFDataset.BlankNode("_:" + binding.getValue("subject"));
					else
						sub = new RDFDataset.BlankNode(binding.getValue("subject"));
				} else
					sub = new RDFDataset.IRI(binding.getValue("subject"));
			} catch (SEPABindingsException e) {
				logger.error(e.getMessage());
				lastError = NgsiLdError.InternalError;
				lastError.setTitle("Failed to create subject node");
				lastError.setDetail(e.getMessage());
				return null;
			}

			// Object
			try {
				if (binding.isLiteral("object"))
					obj = new RDFDataset.Literal(binding.getValue("object"), binding.getDatatype("object"),
							binding.getLanguage("object"));
				else if (binding.isBNode("object")) {
					if (!binding.getValue("object").startsWith("_:"))
						obj = new RDFDataset.BlankNode("_:" + binding.getValue("object"));
					else
						obj = new RDFDataset.BlankNode(binding.getValue("object"));
				} else
					obj = new RDFDataset.IRI(binding.getValue("object"));
			} catch (SEPABindingsException e) {
				logger.error(e.getMessage());
				lastError = NgsiLdError.InternalError;
				lastError.setTitle("Failed to create object node");
				lastError.setDetail(e.getMessage());
				return null;
			}

			model.addTriple(sub.getValue(), pred.getValue(), obj.getValue());
		}

		return model;
	}
	// private Model buildRdfGraph(List<Bindings> bindings) {
	// Model model = ModelFactory.createDefaultModel();
	//
	// for (Bindings triple : bindings) {
	// Resource sub = null;
	// Property pred = model.createProperty(triple.getValue("predicate"));
	// RDFNode obj = null;
	//
	// // Subject
	// try {
	// if (triple.isBNode("subject"))
	// sub = model.createResource(new AnonId(triple.getValue("subject")));
	// else
	// sub = model.createResource(triple.getValue("subject"));
	// } catch (SEPABindingsException e) {
	// logger.error(e.getMessage());
	// lastError = NgsiLdError.InternalError;
	// lastError.setTitle("Failed to create resource");
	// lastError.setDetail(e.getMessage());
	// }
	//
	// // Object
	// try {
	// if (triple.isLiteral("object"))
	// obj = model.createLiteral(triple.getValue("object"));
	// else if (triple.isBNode("object"))
	// obj = model.createResource(new AnonId(triple.getValue("object")));
	// else
	// obj = model.createResource(triple.getValue("object"));
	// } catch (SEPABindingsException e) {
	// logger.error(e.getMessage());
	// lastError = NgsiLdError.InternalError;
	// lastError.setTitle("Failed to create resource");
	// lastError.setDetail(e.getMessage());
	// }
	//
	// model.add(sub, pred, obj);
	// }
	//
	// return model;
	// }

	/**
	 * Build an RDF graph corresponding to the @context member of the JSON LD
	 * element
	 * 
	 * The context element is supposed to be in one of the following forms:
	 * 
	 * <pre>
	 * "@context": { 
	 *  "name": "http://schema.org/name", 
	 *  "image": {"@id":"http://schema.org/image", "@type": "@id" }}
	 * 
	 * "@context": "https://json-ld.org/contexts/person.jsonld"
	 * 
	 * "@context": { 
	 *  "@vocab": "http://schema.org/", 
	 *  "knows": {"@type": "@id"} }
	 * 
	 * "@context": [ 
	 *  "https://json-ld.org/contexts/person.jsonld",
	 *  "https://json-ld.org/contexts/place.jsonld",
	 *  {"title":"http://purl.org/dc/terms/title"} ]
	 * </pre>
	 */
	private RDFDataset extractContext(String entityId, JsonElement context) {
		RDFDataset m = new RDFDataset();

		Node entity = new RDFDataset.IRI(entityId);
		Node primitive = new RDFDataset.IRI(ngsiLdContextPrimitive);
		Node element = new RDFDataset.IRI(ngsiLdContextElement);
		Node term = new RDFDataset.IRI(ngsiLdContextTerm);
		Node iri = new RDFDataset.IRI(ngsiLdContextIri);

		if (context.isJsonArray()) {
			JsonArray arr = (JsonArray) context.getAsJsonArray();
			for (JsonElement elem : arr) {
				if (elem.isJsonPrimitive()) {
					Node obj = new RDFDataset.Literal(elem.getAsString(), "", "");
					m.addTriple(entity.getValue(), primitive.getValue(), obj.getValue());
				} else if (elem.isJsonObject()) {
					JsonObject obj = (JsonObject) elem.getAsJsonObject();
					for (Entry<String, JsonElement> subElem : obj.entrySet()) {
						Node bn = new RDFDataset.BlankNode("_:" + UUID.randomUUID());
						m.addTriple(entity.getValue(), element.getValue(), bn.getValue());
						Node ctxTerm = new RDFDataset.Literal(subElem.getKey(), "", "");
						Node ctxIri = new RDFDataset.Literal(subElem.getValue().getAsString(), "", "");
						m.addTriple(bn.getValue(), term.getValue(), ctxTerm.getValue());
						m.addTriple(bn.getValue(), iri.getValue(), ctxIri.getValue());
					}
				}
			}
		} else if (context.isJsonPrimitive()) {
			Node obj = new RDFDataset.Literal(context.getAsString(), "", "");
			m.addTriple(entity.getValue(), primitive.getValue(), obj.getValue());

		} else if (context.isJsonObject()) {
			JsonObject obj = (JsonObject) context.getAsJsonObject();
			for (Entry<String, JsonElement> subElem : obj.entrySet()) {
				Node bn = new RDFDataset.BlankNode("_:" + UUID.randomUUID());
				m.addTriple(entity.getValue(), element.getValue(), bn.getValue());
				Node ctxTerm = new RDFDataset.Literal(subElem.getKey(), "", "");
				Node ctxIri = new RDFDataset.Literal(subElem.getValue().getAsString(), "", "");
				m.addTriple(bn.getValue(), term.getValue(), ctxTerm.getValue());
				m.addTriple(bn.getValue(), iri.getValue(), ctxIri.getValue());
			}
		}

		return m;
	}
	// private Model extractContext(String entityId, JsonElement context) {
	// Model m = ModelFactory.createDefaultModel();
	//
	// Resource entity = m.createResource(entityId);
	// Property primitive = m.createProperty(ngsiLdContextPrimitive);
	// Property element = m.createProperty(ngsiLdContextElement);
	// Property term = m.createProperty(ngsiLdContextTerm);
	// Property iri = m.createProperty(ngsiLdContextIri);
	//
	// if (context.isJsonArray()) {
	// JsonArray arr = (JsonArray) context.getAsJsonArray();
	// for (JsonElement elem : arr) {
	// if (elem.isJsonPrimitive()) {
	// m.add(entity, primitive, m.createLiteral(elem.getAsString()));
	// } else if (elem.isJsonObject()) {
	// JsonObject obj = (JsonObject) elem.getAsJsonObject();
	// for (Entry<String, JsonElement> subElem : obj.entrySet()) {
	// Resource bn = m.createResource(new AnonId());
	// m.add(entity, element, bn);
	// m.add(bn, term, m.createLiteral(subElem.getKey()));
	// m.add(bn, iri, m.createLiteral(subElem.getValue().getAsString()));
	// }
	// }
	// }
	// } else if (context.isJsonPrimitive()) {
	// m.add(entity, primitive, m.createLiteral(context.getAsString()));
	// } else if (context.isJsonObject()) {
	// JsonObject obj = (JsonObject) context.getAsJsonObject();
	// for (Entry<String, JsonElement> subElem : obj.entrySet()) {
	// Resource bn = m.createResource(new AnonId());
	// m.add(entity, element, bn);
	// m.add(bn, term, m.createLiteral(subElem.getKey()));
	// m.add(bn, iri, m.createLiteral(subElem.getValue().getAsString()));
	// }
	// }
	//
	// return m;
	// }

	/**
	 * Sends the SPARQL query to the endpoint
	 */
	private Response query(String sparql) {
		// Scheduler QUERY request
		InternalUQRequest sepaRequest = new InternalQueryRequest(sparql, null, null);
		Timings.log(sepaRequest);
		ScheduledRequest req = scheduler.schedule(sepaRequest, this);

		// Request not scheduled
		if (req == null) {
			logger.error("Out of tokens");
			jmx.outOfTokens();
			return new ErrorResponse(500, "Out of tokens", "Request cannot be scheduled");
		}

		// Wait for response
		try {
			synchronized (scheduler) {
				scheduler.wait();
			}
		} catch (InterruptedException e) {
			logger.error(e);
		}

		return response;
	}

	/**
	 * Sends the SPARQL update to the endpoint
	 */
	private Response update(String sparql) {
		// Scheduler UPDATE request
		InternalUQRequest sepaRequest = new InternalUpdateRequest(sparql, null, null);
		Timings.log(sepaRequest);
		ScheduledRequest req = scheduler.schedule(sepaRequest, this);

		// Request not scheduled
		if (req == null) {
			logger.error("Out of tokens");
			jmx.outOfTokens();
			return new ErrorResponse(500, "Out of tokens", "Request cannot be scheduled");
		}

		// Wait for response
		try {
			synchronized (scheduler) {
				scheduler.wait();
			}
		} catch (InterruptedException e) {
			logger.error(e);
		}

		return response;
	}

	/**
	 * Stores the JSON LD frame of an entity in the form { "@type" : "...",
	 * "@context" : ... }
	 */
	private boolean storeEntityFrame(String entityId, String type, JsonElement context) {
		// Model entityContext = extractContext(entityId, context);
		// entityContext.add(entityContext.createResource(entityId),
		// entityContext.createProperty(rdfTypeIri),
		// entityContext.createResource(type));

		RDFDataset entityContext = extractContext(entityId, context);

		Node subject = new RDFDataset.IRI(entityId);
		Node predicate = new RDFDataset.IRI(rdfTypeIri);
		Node object = new RDFDataset.IRI(type);
		entityContext.addTriple(subject.getValue(), predicate.getValue(), object.getValue());

		String sparql = "INSERT DATA { GRAPH <" + NgsiLdRdfMapper.ngsiLdContextGraph + "> {" + nTriples(entityContext)
				+ "}}";

		Response ret = update(sparql);

		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			lastError = NgsiLdError.InternalError;
			lastError.setTitle(error.getError());
			lastError.setDetail(error.getErrorDescription());
			return false;
		}

		return true;
	}

	/**
	 * Gets the previously stored JSON LD frame of an entity which is supposed to
	 * assume the form { "@type" : "...", "@context" : ... }
	 */
	private JsonObject getEntityFrame(String entityId) {
		String construct = "SELECT ?type ?primitive ?term ?iri FROM <" + ngsiLdContextGraph
				+ "> WHERE {{?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type} UNION {?id <"
				+ ngsiLdContextPrimitive + "> ?primitive} UNION {?id <" + ngsiLdContextElement
				+ "> ?element . ?element <" + ngsiLdContextTerm + "> ?term . ?element <" + ngsiLdContextIri
				+ "> ?iri} }";

		construct = construct.replace("?id", "<" + entityId + ">");

		Response ret = query(construct);

		JsonObject frame = new JsonObject();
		frame.getAsJsonObject().add("@context", new JsonArray());

		if (!ret.isError()) {
			QueryResponse results = (QueryResponse) ret;
			for (Bindings bindings : results.getBindingsResults().getBindings()) {
				if (bindings.getValue("type") != null)
					frame.getAsJsonObject().add("@type", new JsonPrimitive(bindings.getValue("type")));
				else if (bindings.getValue("primitive") != null) {
					frame.getAsJsonObject().get("@context").getAsJsonArray().add(bindings.getValue("primitive"));
				} else if (bindings.getValue("term") != null && bindings.getValue("iri") != null) {
					JsonObject term = new JsonObject();
					term.add(bindings.getValue("term"), new JsonPrimitive(bindings.getValue("iri")));
					frame.getAsJsonObject().get("@context").getAsJsonArray().add(term);
				}
			}
		}

		if (frame.getAsJsonArray("@context").size() == 1) {
			JsonElement context = frame.getAsJsonArray("@context").get(0);
			frame.remove("@context");
			frame.add("@context", context);
		}

		return frame;
	}

	/**
	 * Checks if NGSI-LD entity identified by the "@id" member is present or not
	 */
	public boolean entityExists(JsonObject jsonld) {
		String entityId = "<" + jsonld.get("@id").getAsString() + ">";
		String ask = "ASK {" + entityId + " ?p ?o}";

		Response ret = query(ask);

		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			lastError = NgsiLdError.InternalError;
			lastError.setTitle(error.getError());
			lastError.setDetail(error.getErrorDescription());
			return false;
		}

		try {
			return ((QueryResponse) response).getAskResult();
		} catch (SEPABindingsException e) {
			logger.error(e);
			lastError = NgsiLdError.InternalError;
			lastError.setTitle("Failed to get ASK result");
			lastError.setDetail(e.getMessage());
			return false;
		}
	}

	/**
	 * Creates a new NGSI-LD entity
	 */
	public boolean createEntity(JsonObject jsonld) {
		// From JSON-LD to RDF
		RDFDataset ds = fromJsonLd(jsonld);

		if (ds == null)
			return false;

		if (ds.isEmpty()) {
			lastError = NgsiLdError.InternalError;
			lastError.setTitle("Failed to convert");
			lastError.setDetail(jsonld.toString());
			return false;
		}

		// Extract entity id, type and context
		String id = jsonld.get("@id").getAsString();
		String type = getEntityType(ds, id);
		if (type == null) {
			lastError = NgsiLdError.InternalError;
			lastError.setTitle("Entity @type not found");
			lastError.setDetail("Entity id: " + id);
			return false;
		}
		JsonElement context = jsonld.get("@context");

		// Insert triples
		String triples = nTriples(ds);
		String sparql = "INSERT DATA { GRAPH <" + NgsiLdRdfMapper.ngsiLdEntitiesGraph + "> {" + triples + "}}";
		Response ret = update(sparql);
		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			lastError = NgsiLdError.InternalError;
			lastError.setTitle(error.getError());
			lastError.setDetail(error.getErrorDescription());
			return false;
		}

		// Store frame
		boolean store = storeEntityFrame(id, type, context);

		// Delete triples if frame cannot be stored
		if (!store) {
			sparql = "WITH <" + NgsiLdRdfMapper.ngsiLdEntitiesGraph + "> DELETE WHERE {"
					+ triples.replaceAll(" _:", " ?") + "}";
			update(sparql);

			lastError = NgsiLdError.InternalError;
			lastError.setTitle("Failed to store entity");
			lastError.setDetail(jsonld.toString());

			return false;
		}

		return true;
	}
	// public boolean createEntity(JsonObject jsonld) {
	// // From JSON-LD to RDF
	// Model ds = fromJsonLd(jsonld);
	//
	// if (ds == null)
	// return false;
	//
	// if (ds.isEmpty()) {
	// lastError = NgsiLdError.InternalError;
	// lastError.setTitle("Failed to convert");
	// lastError.setDetail(jsonld.toString());
	// return false;
	// }
	//
	// // Extract entity id, type and context
	// String id = jsonld.get("@id").getAsString();
	// String type = getEntityType(ds, id);
	// if (type == null) {
	// lastError = NgsiLdError.InternalError;
	// lastError.setTitle("Entity @type not found");
	// lastError.setDetail("Entity id: " + id);
	// return false;
	// }
	// JsonElement context = jsonld.get("@context");
	//
	// // SPARQL UPDATE
	// String sparql = "INSERT DATA { GRAPH <" + NgsiLdRdfMapper.ngsiLdEntitiesGraph
	// + "> {" + nTriples(ds) + "}}";
	// Response ret = update(sparql);
	// if (ret.isError()) {
	// ErrorResponse error = (ErrorResponse) ret;
	// lastError = NgsiLdError.InternalError;
	// lastError.setTitle(error.getError());
	// lastError.setDetail(error.getErrorDescription());
	// return false;
	// }
	//
	// boolean store = storeEntityFrame(id, type, context);
	//
	// if (!store) {
	// sparql = "DELETE DATA { GRAPH <" + NgsiLdRdfMapper.ngsiLdEntitiesGraph + ">
	// {" + nTriples(ds) + "}}";
	// update(sparql);
	//
	// lastError = NgsiLdError.InternalError;
	// lastError.setTitle("Failed to store entity");
	// lastError.setDetail(jsonld.toString());
	//
	// return false;
	// }
	//
	// return true;
	// }

	/**
	 * Gets the type of the entity identified by id
	 */
	private String getEntityType(RDFDataset ds, String id) {
		for (String gn : ds.graphNames()) {
			for (Quad q : ds.getQuads(gn)) {
				if (!q.getSubject().getValue().equals(id))
					continue;
				if (!q.getPredicate().getValue().equals(rdfTypeIri))
					continue;
				return q.getObject().getValue();
			}
		}

		return null;
	}
	// private String getEntityType(Model ds, String id) {
	// // Create selector
	// Resource sub = ds.createResource(id);
	// Property pred = ds.createProperty(rdfTypeIri);
	// RDFNode obj = null;
	// Selector s = new SimpleSelector(sub, pred, obj);
	//
	// // Select statement
	// StmtIterator iter = ds.listStatements(s);
	// if (iter.hasNext()) {
	// obj = iter.next().getObject();
	// return obj.asResource().getURI();
	// }
	//
	// return null;
	// }

	/**
	 * Retrieve JSON LD description of the entity idenfied by id
	 */
	public JsonObject getEntityById(String id) {
		int iteration = -1;
		QueryResponse results = null;
		do {
			iteration++;
			String query = buildConstruct(iteration).replace("id", id);
			logger.debug(query);

			Response ret = query(query);

			if (ret.isError()) {
				ErrorResponse error = (ErrorResponse) ret;
				lastError = NgsiLdError.InternalError;
				lastError.setTitle(error.getError());
				lastError.setDetail(error.getErrorDescription());
				return null;
			}

			results = (QueryResponse) ret;
		} while (!noMoreBNodes(results.getBindingsResults().getBindings()));

		// Get entity RDF graph
		// Model entity = buildRdfGraph(results.getBindingsResults().getBindings());
		RDFDataset entity = buildRdfGraph(results.getBindingsResults().getBindings());

		// Get entity frame
		JsonObject frame = getEntityFrame(id);

		// Build JSON-LD response
		return toJsonLd(entity, frame);
	}

	/**
	 * Response received by the scheduler
	 */
	@Override
	public void sendResponse(Response response) throws SEPAProtocolException {
		this.response = response;

		synchronized (scheduler) {
			scheduler.notify();
		}
	}

	/**
	 * Used to build the CONSTRUCT query for entity retrieval
	 */
	private String buildConstruct(int it) {
		return "CONSTRUCT {" + buildConstructPattern(it) + "} FROM <" + ngsiLdEntitiesGraph + "> WHERE { "
				+ buildWherePattern(it) + " }";
	}

	/**
	 * Used to build the CONSTRUCT query for entity retrieval
	 */
	private String buildConstructPattern(int it) {
		if (it == 0)
			return "?entity ?p0 ?o0";
		else
			return buildConstructPattern(it - 1) + " . ?o" + (it - 1) + " ?p" + it + " ?o" + it;
	}

	/**
	 * Used to build the CONSTRUCT query for entity retrieval
	 */
	private String buildWherePattern(int it) {
		if (it == 0)
			return "{VALUES ?entity {<id>} ?entity ?p0 ?o0}";
		else
			return buildWherePattern(it - 1) + " UNION { VALUES ?entity {<id>} " + buildConstructPattern(it) + " }";
	}

	/**
	 * Used to build the CONSTRUCT query for entity retrieval
	 */
	private boolean noMoreBNodes(List<Bindings> bindings) {
		ArrayList<String> subBNodes = new ArrayList<String>();
		ArrayList<String> objBNodes = new ArrayList<String>();

		for (Bindings bind : bindings) {
			try {
				if (bind.isBNode("subject"))
					subBNodes.add(bind.getValue("subject"));
				if (bind.isBNode("object"))
					objBNodes.add(bind.getValue("object"));
			} catch (SEPABindingsException e) {
				continue;
			}
		}

		for (String node : subBNodes)
			objBNodes.remove(node);

		return objBNodes.isEmpty();
	}

	/**
	 * Get last error
	 */
	public NgsiLdError getLastError() {
		return lastError;
	}
}

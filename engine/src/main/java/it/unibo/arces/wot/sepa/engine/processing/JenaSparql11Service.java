package it.unibo.arces.wot.sepa.engine.processing;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class JenaSparql11Service extends SPARQL11Protocol {

	public Response query(QueryRequest req) {
		JsonObject json = null;
		
		String url = req.getScheme() + "://" + req.getHost() + ":"
				+ req.getPort() + req.getPath();
		Query query = QueryFactory.create(req.getSPARQL());
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService(url, query)) {
			ResultSet results = qexec.execSelect();
			OutputStream out = new ByteArrayOutputStream();	
			ResultSetFormatter.outputAsJSON(out,results);
			json = (JsonObject) new JsonParser().parse(out.toString());
		}
		return new QueryResponse(json);
	}
}

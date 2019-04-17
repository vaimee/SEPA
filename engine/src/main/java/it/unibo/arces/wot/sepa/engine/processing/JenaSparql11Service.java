package it.unibo.arces.wot.sepa.engine.processing;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

//import org.apache.jena.query.Query;
//import org.apache.jena.query.QueryExecution;
//import org.apache.jena.query.QueryExecutionFactory;
//import org.apache.jena.query.QueryFactory;
//import org.apache.jena.query.ResultSet;
//import org.apache.jena.query.ResultSetFormatter;
//import org.apache.jena.rdf.model.Model;
//import org.apache.jena.rdf.model.Statement;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

/**
SELECT, CONSTRUCT, DESCRIBE
{
"head" { 
   "vars" : [ ... ] ,
   "link" : [ ... ] },
"results": { 
    "bindings": [
               {
                 "a" : { ... } ,
                 "b" : { ... } 
               } ,
               {
                 "a" : { ... } ,
                 "b" : { ... } 
               }
             ]
 }}
 
 ASK
 { 
  "head" : { } ,
  "boolean" : true
}
 * */
public class JenaSparql11Service extends SPARQL11Protocol {

	public Response query(QueryRequest req) {
		JsonObject json = new JsonObject();
		json.add("head", new JsonObject());
		
		String url = req.getScheme() + "://" + req.getHost() + ":"
				+ req.getPort() + req.getPath();
		
//		Query query = QueryFactory.create(req.getSPARQL());
//		
//		try (QueryExecution qexec = QueryExecutionFactory.sparqlService(url, query)) {
//			switch(query.getQueryType()) {
//			case Query.QueryTypeAsk:				
//				json.add("boolean", new JsonPrimitive(qexec.execAsk()));				
//				break;
//			case Query.QueryTypeConstruct:
//			case Query.QueryTypeDescribe:
//				Model model = null;
//				if (query.getQueryType() == Query.QueryTypeConstruct) model = qexec.execConstruct();
//				else model = qexec.execDescribe();
//				
//				JsonArray vars = new JsonArray();
//				vars.add("subject");
//				vars.add("predicate");
//				vars.add("object");				
//				json.getAsJsonObject("head").add("vars", vars);
//				
//				json.add("results", new JsonObject());
//				json.getAsJsonObject("results").add("bindings", new JsonArray());
//				
//				/*
//				 * 
//RDF Term	JSON form
//IRI I	{"type": "uri", "value": "I"}
//Literal S	{"type": "literal","value": "S"}
//Literal S with language tag L	{ "type": "literal", "value": "S", "xml:lang": "L"}
//Literal S with datatype IRI D	{ "type": "literal", "value": "S", "datatype": "D"}
//Blank node, label B	{"type": "bnode", "value": "B"}
//				 * */
//				for (Statement stm : model.listStatements().toSet()) {
//					// Subject (bnode or URI)
//					JsonObject sub = new JsonObject();
//					if (stm.getSubject().isAnon()) {
//						sub.add("type", new JsonPrimitive("bnode"));
//					}
//					else sub.add("type", new JsonPrimitive("uri"));
//					sub.add("value", new JsonPrimitive(stm.getSubject().toString()));
//					
//					// Predicate (bnode or URI)
//					JsonObject pred = new JsonObject();
//					pred.add("type", new JsonPrimitive("uri"));
//					pred.add("value", new JsonPrimitive(stm.getPredicate().toString()));
//					
//					// Object (literal datatype & lang)
//					JsonObject obj = new JsonObject();
//					if (stm.getObject().isAnon()) {
//						obj.add("type", new JsonPrimitive("bnode"));
//					}
//					else if (stm.getObject().isLiteral()) {
//						obj.add("type", new JsonPrimitive("literal"));
//						if(stm.getObject().asLiteral().getDatatypeURI() != null) obj.add("datatype", new JsonPrimitive(stm.getObject().asLiteral().getDatatypeURI()));
//						if(stm.getObject().asLiteral().getLanguage() != null) obj.add("xml:lang", new JsonPrimitive(stm.getObject().asLiteral().getLanguage()));
//					}
//					else obj.add("type", new JsonPrimitive("uri"));
//					obj.add("value", new JsonPrimitive(stm.getObject().toString()));
//					
//					JsonObject node = new JsonObject();
//					node.add("subject", sub);
//					node.add("predicate", pred);
//					node.add("object", obj);
//					
//					json.getAsJsonObject("results").getAsJsonArray("bindings").add(node);
//				}
//				break;
//			case Query.QueryTypeSelect:
//				ResultSet results = qexec.execSelect();
//				OutputStream out = new ByteArrayOutputStream();	
//				ResultSetFormatter.outputAsJSON(out,results);
//				json = (JsonObject) new JsonParser().parse(out.toString());
//				break;
//			}
//			
//		}
		
		return new QueryResponse(json);
		
	}
}

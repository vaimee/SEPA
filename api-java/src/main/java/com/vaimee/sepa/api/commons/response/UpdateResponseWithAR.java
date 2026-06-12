package com.vaimee.sepa.api.commons.response;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vaimee.sepa.api.commons.sparql.*;

import org.apache.jena.sparql.core.Quad;

import java.util.Set;

public class UpdateResponseWithAR extends UpdateResponse {
	private final Set<Quad> addedTuples;
	private final Set<Quad> removedTuples;

	public UpdateResponseWithAR(Set<Quad> addedTuples, Set<Quad> removedTuples) {
		super("Update response with AR quads");
		this.addedTuples = addedTuples;
		this.removedTuples = removedTuples;

		json.add("quads",new JsonObject());
		json.getAsJsonObject("quads").add("added",new JsonArray());
		json.getAsJsonObject("quads").add("removed",new JsonArray());

		if (addedTuples != null) {
			for (Quad q : addedTuples) {
				Bindings b = new Bindings();
				b.addBinding("graph",new RDFTermURI(q.getGraph().toString()));
				b.addBinding("subject",(q.getSubject().isURI() ? new RDFTermURI(q.getSubject().toString()) : q.getSubject().isBlank() ? new RDFTermBNode(q.getSubject().toString()) : new RDFTermLiteral(q.getSubject().getLiteralLexicalForm())));
				b.addBinding("predicate",new RDFTermURI(q.getPredicate().toString()));
				b.addBinding("object",(q.getObject().isURI() ? new RDFTermURI(q.getObject().toString()) : q.getObject().isBlank() ? new RDFTermBNode(q.getObject().toString()) : new RDFTermLiteral(q.getObject().getLiteralLexicalForm())));

				json.getAsJsonObject("quads").getAsJsonArray("added").add(b.toJson());
			}
		}

		if (removedTuples != null) {
			for (Quad q : removedTuples) {
				Bindings b = new Bindings();
				b.addBinding("graph",new RDFTermURI(q.getGraph().toString()));
				b.addBinding("subject",(q.getSubject().isURI() ? new RDFTermURI(q.getSubject().toString()) : q.getSubject().isBlank() ? new RDFTermBNode(q.getSubject().toString()) : new RDFTermLiteral(q.getSubject().getLiteralLexicalForm())));
				b.addBinding("predicate",new RDFTermURI(q.getPredicate().toString()));
				b.addBinding("object",(q.getObject().isURI() ? new RDFTermURI(q.getObject().toString()) : q.getObject().isBlank() ? new RDFTermBNode(q.getObject().toString()) : new RDFTermLiteral(q.getObject().getLiteralLexicalForm())));

				json.getAsJsonObject("quads").getAsJsonArray("removed").add(b.toJson());
			}
		}
	}

}

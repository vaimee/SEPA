package com.vaimee.sepa.engine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaimee.sepa.engine.processing.subscriptions.GraphPolicy;

public class GraphPolicyTest {
	private static final String LIVE = "http://example.org/unit1_live";
	private static final String HISTORY = "http://example.org/unit1_history";
	private static final String OTHER = "http://example.org/other";

	private static Set<String> graphs(String where) {
		return GraphPolicy.graphsOf("SELECT * WHERE { " + where + " }");
	}

	@Test
	public void findsGraphsWhereverTheyAre() {
		assertEquals(Set.of(LIVE), graphs("GRAPH <" + LIVE + "> { ?s ?p ?o }"));
		assertEquals(Set.of(LIVE, HISTORY), graphs("GRAPH <" + LIVE + "> { ?s ?p ?o } OPTIONAL { GRAPH <" + HISTORY + "> { ?s ?q ?v } }"));
		assertEquals(Set.of(LIVE, HISTORY), graphs("{ GRAPH <" + LIVE + "> { ?s ?p ?o } } UNION { GRAPH <" + HISTORY + "> { ?s ?p ?o } }"));
		assertEquals(Set.of(LIVE, HISTORY), graphs("GRAPH <" + LIVE + "> { ?s ?p ?o } MINUS { GRAPH <" + HISTORY + "> { ?s ?p ?o } }"));
		assertEquals(Set.of(LIVE, HISTORY), graphs("GRAPH <" + LIVE + "> { ?s ?p ?o } FILTER NOT EXISTS { GRAPH <" + HISTORY + "> { ?s ?q ?v } }"));
		assertEquals(Set.of(LIVE, HISTORY), graphs("GRAPH <" + LIVE + "> { ?s ?p ?o } { SELECT ?s WHERE { GRAPH <" + HISTORY + "> { ?s ?q ?v } } }"));
		assertEquals(Set.of(GraphPolicy.ANY_GRAPH), graphs("GRAPH ?g { ?s ?p ?o }"));
		assertEquals(Set.of(GraphPolicy.DEFAULT_GRAPH), graphs("?s ?p ?o"));
		assertEquals(Set.of(LIVE), GraphPolicy.graphsOf("SELECT * FROM <" + LIVE + "> WHERE { ?s ?p ?o }"));
		assertEquals(Set.of(LIVE), GraphPolicy.graphsOf("SELECT * FROM NAMED <" + LIVE + "> WHERE { GRAPH ?g { ?s ?p ?o } }"));
	}

	@Test
	public void allowsSubscriptionsOnLiveGraphsOnly() throws Exception {
		Path jsap = Files.createTempFile("policy", ".jsap");
		try (FileWriter w = new FileWriter(jsap.toFile())) {
			w.write("{ \"graphPolicy\": { \"live\": [\".*_live\"], \"history\": [\".*_history\"] } }");
		}
		GraphPolicy.load(jsap.toString());

		assertNull(GraphPolicy.check("SELECT * WHERE { GRAPH <" + LIVE + "> { ?s ?p ?o } }"));
		assertNotNull(GraphPolicy.check("SELECT * WHERE { GRAPH <" + HISTORY + "> { ?s ?p ?o } }"));
		assertNotNull(GraphPolicy.check("SELECT * WHERE { GRAPH <" + LIVE + "> { ?s ?p ?o } OPTIONAL { GRAPH <" + HISTORY + "> { ?s ?q ?v } } }"));
		assertNotNull(GraphPolicy.check("SELECT * WHERE { GRAPH <" + OTHER + "> { ?s ?p ?o } }"));
		assertNotNull(GraphPolicy.check("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } }"));
		assertNotNull(GraphPolicy.check("SELECT * WHERE { ?s ?p ?o }"));
		assertEquals(403, GraphPolicy.check("SELECT * WHERE { GRAPH <" + HISTORY + "> { ?s ?p ?o } }").getStatusCode());
	}

	@Test
	public void joinsThePoliciesOfSeveralJsaps() throws Exception {
		Path mdw = Files.createTempFile("mdw", ".jsap");
		Files.writeString(mdw, "{ \"graphPolicy\": { \"live\": [\".*_live\"], \"history\": [\".*_history\"] } }");
		Path agora = Files.createTempFile("agora", ".jsap");
		Files.writeString(agora, "{ \"graphPolicy\": { \"live\": [\"" + OTHER + "\"] } }");
		GraphPolicy.load(mdw + "," + agora);
		assertNull(GraphPolicy.check("SELECT * WHERE { GRAPH <" + LIVE + "> { ?s ?p ?o } }"));
		assertNull(GraphPolicy.check("SELECT * WHERE { GRAPH <" + OTHER + "> { ?s ?p ?o } }"));
		assertNotNull(GraphPolicy.check("SELECT * WHERE { GRAPH <" + HISTORY + "> { ?s ?p ?o } }"));
	}

	@Test
	public void withoutAPolicyEverythingIsAllowed() throws Exception {
		Path jsap = Files.createTempFile("nopolicy", ".jsap");
		Files.writeString(jsap, "{ }");
		GraphPolicy.load(jsap.toString());
		assertNull(GraphPolicy.check("SELECT * WHERE { ?s ?p ?o }"));
	}

	@AfterEach
	public void reset() throws Exception {
		Path jsap = Files.createTempFile("nopolicy", ".jsap");
		Files.writeString(jsap, "{ }");
		GraphPolicy.load(jsap.toString());
	}
}

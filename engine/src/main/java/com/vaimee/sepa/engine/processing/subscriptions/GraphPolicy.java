package com.vaimee.sepa.engine.processing.subscriptions;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprFunctionOp;
import org.apache.jena.sparql.expr.ExprVisitorBase;
import org.apache.jena.sparql.algebra.walker.Walker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.logging.Logging;

/**
 * Which graphs clients may subscribe to, read from the "graphPolicy" section
 * of the JSAPs given to the engine (-jsap a.jsap,b.jsap):
 *
 * <pre>
 * "graphPolicy": {
 *   "live":    [ "regex", ... ],
 *   "history": [ "regex", ... ]
 * }
 * </pre>
 *
 * Live graphs hold the current state and are small: subscriptions are allowed
 * on them only. History graphs grow and do not change once written, so they
 * are read with queries: a subscription would re-run its whole query at each
 * update for nothing. With a policy, a subscription is accepted only if every
 * graph it reads matches a live pattern and none matches a history pattern;
 * the default graph and variable graphs (GRAPH ?g) are refused, since they can
 * read any graph. Without a policy every subscription is accepted.
 */
public class GraphPolicy {
	/** Marker of a variable graph (GRAPH ?g) in {@link #graphsOf(String)}. */
	public static final String ANY_GRAPH = "*";
	/** Marker of the default graph in {@link #graphsOf(String)}. */
	public static final String DEFAULT_GRAPH = "";

	private static volatile GraphPolicy current = null;

	private final List<Pattern> live = new ArrayList<>();
	private final List<Pattern> history = new ArrayList<>();

	GraphPolicy(List<String> live, List<String> history) {
		for (String regex : live) this.live.add(Pattern.compile(regex));
		for (String regex : history) this.history.add(Pattern.compile(regex));
	}

	/**
	 * Reads the policy of one or more JSAPs (comma separated) and joins their
	 * patterns, so each application declares its own graphs. JSAPs without
	 * "graphPolicy" add nothing; with none at all subscriptions are unrestricted.
	 */
	public static void load(String jsaps) throws IOException {
		List<String> live = new ArrayList<>();
		List<String> history = new ArrayList<>();
		boolean declared = false;
		for (String jsap : jsaps.split(",")) {
			if (jsap.isBlank()) continue;
			try (Reader reader = new FileReader(jsap.trim())) {
				JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
				if (!root.has("graphPolicy")) {
					Logging.warn("No graphPolicy in " + jsap.trim());
					continue;
				}
				JsonObject policy = root.getAsJsonObject("graphPolicy");
				live.addAll(strings(policy, "live"));
				history.addAll(strings(policy, "history"));
				declared = true;
			}
		}
		current = declared ? new GraphPolicy(live, history) : null;
		if (current == null) Logging.warn("No graph policy in " + jsaps + ": subscriptions are allowed on every graph");
		else Logging.info("Graph policy from " + jsaps + ": live " + current.live + ", history " + current.history);
	}

	private static List<String> strings(JsonObject policy, String key) {
		List<String> ret = new ArrayList<>();
		JsonArray array = policy.has(key) ? policy.getAsJsonArray(key) : new JsonArray();
		for (JsonElement e : array) ret.add(e.getAsString());
		return ret;
	}

	/** The error refusing a subscription, or null when the policy allows it (or there is none). */
	public static ErrorResponse check(String sparql) {
		GraphPolicy policy = current;
		return policy == null ? null : policy.verify(sparql);
	}

	ErrorResponse verify(String sparql) {
		Set<String> graphs;
		try {
			graphs = graphsOf(sparql);
		} catch (Exception e) {
			// The SPU initialisation reports parsing errors
			return null;
		}
		for (String graph : graphs) {
			String reason = null;
			if (graph.equals(ANY_GRAPH))
				reason = "a variable graph (GRAPH ?g) can read historical graphs: name the live graphs";
			else if (graph.equals(DEFAULT_GRAPH))
				reason = "the default graph is not a live graph: use GRAPH <live graph>";
			else if (matches(history, graph))
				reason = "<" + graph + "> is a historical graph: read it with a query";
			else if (!matches(live, graph))
				reason = "<" + graph + "> is not declared live in the graph policy";
			if (reason != null) return new ErrorResponse(403, "subscription_not_allowed", "Subscriptions are allowed on live graphs only: " + reason);
		}
		return null;
	}

	private static boolean matches(List<Pattern> patterns, String graph) {
		for (Pattern p : patterns) if (p.matcher(graph).matches()) return true;
		return false;
	}

	/**
	 * Every graph the query reads, wherever its GRAPH is (OPTIONAL, UNION,
	 * MINUS, EXISTS, subqueries): graph IRIs, {@link #ANY_GRAPH} for a variable
	 * graph, {@link #DEFAULT_GRAPH} for patterns outside GRAPH. FROM replaces
	 * the default graph with the graphs it names.
	 */
	public static Set<String> graphsOf(String sparql) {
		Query query = QueryFactory.create(sparql);
		Set<String> ret = new HashSet<>();
		collect(Algebra.toQuadForm(Algebra.compile(query)), ret);
		if (query.hasDatasetDescription()) {
			List<String> from = query.getDatasetDescription().getDefaultGraphURIs();
			if (ret.remove(DEFAULT_GRAPH)) {
				ret.addAll(from);
				if (from.isEmpty()) ret.add(DEFAULT_GRAPH);
			}
			// FROM NAMED limits GRAPH ?g to the graphs it names
			List<String> named = query.getDatasetDescription().getNamedGraphURIs();
			if (!named.isEmpty() && ret.remove(ANY_GRAPH)) ret.addAll(named);
		}
		return ret;
	}

	private static void collect(Op op, Set<String> graphs) {
		ExprVisitorBase exists = new ExprVisitorBase() {
			@Override
			public void visit(ExprFunctionOp funcOp) {
				// Outside the enclosing GRAPH: an EXISTS inside GRAPH <g> reads
				// as the default graph, which refuses more, never less.
				collect(Algebra.toQuadForm(funcOp.getGraphPattern()), graphs);
			}
		};
		OpWalker.walk(op, new OpVisitorBase() {
			@Override
			public void visit(OpQuadPattern quads) {
				add(quads.getGraphNode(), graphs);
			}

			@Override
			public void visit(OpGraph graph) {
				add(graph.getNode(), graphs);
			}

			@Override
			public void visit(OpFilter filter) {
				for (Expr e : filter.getExprs()) Walker.walk(e, exists);
			}
		});
	}

	private static void add(Node node, Set<String> graphs) {
		if (node.isVariable()) graphs.add(ANY_GRAPH);
		else if (Quad.isDefaultGraph(node)) graphs.add(DEFAULT_GRAPH);
		else if (node.isURI()) graphs.add(node.getURI());
	}
}

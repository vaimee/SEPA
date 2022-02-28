package it.unibo.arces.wot.sepa.engine.processing.lutt;

import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.Syntax;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.lang.SPARQLParser;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementAssign;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementDataset;
import org.apache.jena.sparql.syntax.ElementExists;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementMinus;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementNotExists;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitor;

public class QueryLUTTextraction{

	private static SPARQLParser sparqlPArser = SPARQLParser.createParser(Syntax.syntaxSPARQL_11);
	public static LUTT exstract(String sparqlQuery) {
		Query jenaQuery = sparqlPArser.parse(new Query(), sparqlQuery);
		Element where = jenaQuery.getQueryPattern();
		List<String> graphs= jenaQuery.getGraphURIs();
		List<String> namedGraphs= jenaQuery.getNamedGraphURIs();
		PrefixMapping prefixs=jenaQuery.getPrefixMapping();
		QElementVisitor visitor = new QElementVisitor();
		where.visit(visitor);
		return new LUTT(
			visitor.getJollyTriple(),
			visitor.getQuads()
		);
	}
	
	
}

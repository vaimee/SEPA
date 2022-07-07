package it.unibo.arces.wot.sepa.engine.processing.lutt;

import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.Syntax;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.lang.SPARQLParser;
import org.apache.jena.sparql.syntax.Element;


public class QueryLUTTextraction{

	private static SPARQLParser sparqlParser = SPARQLParser.createParser(Syntax.syntaxSPARQL_11);
	public static LUTT exstract(String sparqlQuery) {
		Query jenaQuery = sparqlParser.parse(new Query(), sparqlQuery);
		Element where = jenaQuery.getQueryPattern();
		List<String> graphs= jenaQuery.getGraphURIs();
		List<String> namedGraphs= jenaQuery.getNamedGraphURIs();
		PrefixMapping prefixs=jenaQuery.getPrefixMapping();
		QElementVisitor visitor = new QElementVisitor();
		where.visit(visitor);
		for(int x =0;x<namedGraphs.size();x++) {
			visitor.addFromNamed(namedGraphs.get(x), x==namedGraphs.size()-1);
		}
		if(graphs.size()>0) {
			graphs.forEach((String g)->{
				visitor.addFrom(g);
			});
		}else {
			visitor.setNoFromClause();
		}
		return new LUTT(
			visitor.getJollyTriple(),
			visitor.getQuads()
		);
	}
	
	
}
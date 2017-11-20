package it.unibo.arces.wot.sepa.engine.processing;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.Transform;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.optimize.OpVisitorExprPrepare;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.lang.arq.ARQParser;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.jena.sparql.syntax.Template;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SPARQLAnalyzer {

	String test = null;
	
	public void setString(String s) {
		test = s;
	}
	
	class MyTransform extends TransformCopy
	{	    
	    @Override
	    public Op transform(OpBGP opBGP)
	    {	    	
	    	// create a new construct query
	    	Query q = QueryFactory.make();
	    	q.setQueryConstructType();
	    	
	    	// parse the bgp
	    	BasicPattern b = opBGP.getPattern();
	    	Iterator<Triple> opIterator = b.iterator();
	    	Template ttt = new Template(b);
	    	q.setConstructTemplate(ttt);
	    	ElementGroup body = new ElementGroup();
    		ElementUnion union = new ElementUnion();

	    	while (opIterator.hasNext()){	    		
	    		Triple bb = opIterator.next();	
	    		
	    		// for the query
	    		ElementTriplesBlock block = new ElementTriplesBlock(); // Make a BGP
	    		block.addTriple(bb);	    		
	    		body.addElement(block);		
	    		logger.debug(bb.toString());	    	
	    		
	    		// union
	    		union.addElement(block);
	    		
	    	} 
	    		    	
	    	q.setQueryPattern(body);
	    	q.setQueryPattern(union);
    		
	    	setString(q.toString());
	    	logger.debug(q.toString());			
	    	
	    	return opBGP;    	
	    }	  
	}
		
	// attributes
	private static String sparqlText;
	private final static Logger logger = LogManager.getLogger("SPARQLAnalyzer");
	
	// Constructor 
	SPARQLAnalyzer(String request){
		// store the query text
		sparqlText = request;
	}
		
	// Construct Generator
	HashMap<String,String> getConstruct() {
		
		// This method allows to derive the CONSTRUCT queries
		// from the SPARQL UPDATE. CONSTRUCT queries are  
		// needed to know the added and removed triples
		
		// Initialize results
		HashMap<String,String> finalConstruct = new HashMap<String,String>();
		String insCons = "";
		String delCons = "";
		
		// Control variables
		Boolean insertFound = false;
		Boolean deleteFound = false;
		Boolean insDelDataFound = false;
		
		// Is it a SPARQL INSERT DATA?
		String id_pattern = "(^|\\s+)INSERT\\s+DATA\\s*";
		Pattern r = Pattern.compile(id_pattern);
		Matcher m = r.matcher(sparqlText);	
		if (m.find()){		
			insDelDataFound = true;
			insCons = sparqlText.replaceFirst("INSERT\\s+DATA", "CONSTRUCT ") + " WHERE {}";	
//			insCons = insCons.replaceFirst("GRAPH\\s+\\<.*\\>", "");
			logger.debug(insCons);
		};

		// Is it a SPARQL DELETE DATA?
		if (!insDelDataFound) {
			String dd_pattern = "(^|\\s+)DELETE\\s+DATA\\s*";
			r = Pattern.compile(dd_pattern);
			m = r.matcher(sparqlText);	
			if (m.find()){
				insDelDataFound = true;
				delCons = sparqlText.replace(m.group(0), "CONSTRUCT ") + " WHERE {}";
				logger.debug(delCons.toString());
			};
		};
				
		// Check for INSERT / DELETE
		if (!insDelDataFound) {
		
			// Is there an INSERT clause?
			String i_pattern = "(^|\\s+)INSERT\\s*";
			r = Pattern.compile(i_pattern);
			m = r.matcher(sparqlText);	
			if (m.find()){
				insertFound = true;
				insCons = sparqlText.replaceFirst("INSERT", "CONSTRUCT");
			};
		
			// Is there a DELETE clause?
			String d_pattern = "(^|\\s+)DELETE\\s*";
			r = Pattern.compile(d_pattern);
			m = r.matcher(sparqlText);	
			if (m.find()){
				deleteFound = true;
				delCons = sparqlText.replaceFirst("DELETE", "CONSTRUCT");
			};
		
			// Was it a DELETE/INSERT?
			if (insertFound && deleteFound) {
				// initialize clauses
				String insClause = "";
				String delClause = "";
				String whereClause = "";
				
				// get the delete, insert and where clauses
				String searchPattern = "(?s)(DELETE\\s*\\{.*\\})\\s*(INSERT\\s*\\{.*\\})(\\s*WHERE.*\\z)";
				r = Pattern.compile(searchPattern);
				m = r.matcher(sparqlText);	
				if (m.find()){								
					delClause = m.group(1).replaceFirst("DELETE", "CONSTRUCT");
					insClause = m.group(2).replaceFirst("INSERT", "CONSTRUCT");
					whereClause = m.group(3);
					insCons = insClause + whereClause;
					delCons = delClause + whereClause;
				};
			}
			else if (insertFound) {
				// build the insCons
				insCons = sparqlText.replaceFirst(i_pattern, "CONSTRUCT ");
			}	
			else if (deleteFound) {
				// build the delCons
				delCons = sparqlText.replaceFirst(d_pattern, "CONSTRUCT ");
			};							
		};
		
		// debug print
		logger.debug("Added data construct query: " + insCons);
		logger.debug("Deleted data construct query: " + delCons);
		
		// update output
		finalConstruct.put("InsertConstruct", insCons);
		finalConstruct.put("DeleteConstruct", delCons);
		
		// return
		return finalConstruct;
	}
	
	// LUTT generator
	List<TriplePath> getLutt(){

		// debug print
		logger.debug("Analyzing query " + sparqlText);
		
		// create a variable for the LUTT
		List<TriplePath> lutt = new ArrayList();

		// extract basic graph patterns
		Query q = QueryFactory.create(sparqlText);
		Element e = q.getQueryPattern();

		// This will walk through all parts of the query
		ElementWalker.walk(e,
		
			// For each element...
			new ElementVisitorBase() {
				        
				// ...when it's a block of triples...
				public void visit(ElementPathBlock el) {
				        
				// ...go through all the triples...
				Iterator<TriplePath> triples = el.patternElts();
				while (triples.hasNext()) {
				            	
					// get the current triple pattern
				    TriplePath t = triples.next();		            			            
				    lutt.add(t);
		 
				    // debug print
				    logger.debug("Found Triple Pattern: " + t.getSubject() + " " + t.getPredicate() + " " + t.getObject());		            }
				}
			}
		);
		
		// return!
		return lutt;			
	}
	
	// Construct Generator
	String getConstructFromQuery() throws ParseException {
		
		// This method allows to derive the CONSTRUCT query
		// from the SPARQL SUBSCRIPTION

		// get the algebra from the query
		Query qqq = new Query();		
		qqq = QueryFactory.create(sparqlText, Syntax.syntaxSPARQL);								
		Op op = Algebra.compile(qqq);
		
	    // get the algebra version of the construct query and 
		// convert it back to query 
		Transform transform = new MyTransform() ;
		op = Transformer.transform(transform, op) ;		
		Query q = OpAsQuery.asQuery(op); 	
		
		// return		
		return test;
		
	}	

}

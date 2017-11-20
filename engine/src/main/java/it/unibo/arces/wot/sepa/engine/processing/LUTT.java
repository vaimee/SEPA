package it.unibo.arces.wot.sepa.engine.processing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

/**
 * The class LUTT is used to determine if a subscription
 * should be triggered by an update or not.
 * 
 * @author fabioviola
 *
 */
public class LUTT {

	// the lutt
	public List<TriplePath> lutt = null;
	
	// the logger
	private final Logger logger;
	
	/**
	 * The constructor of the LUTT class takes the sparql code
	 * of the subscription and then builds the LUTT, a list of 
	 * triple patterns used to speed up the processing of 
	 * notifications
	 * 
	 * @param uuid
	 * @param sparqlText
	 */
	public LUTT(String uuid, String sparqlText) {
		
		// get the logger
		logger = LogManager.getLogger("LUTT_" + uuid);	
		logger.debug("Initializing LUTT");
				
		// create a variable for the LUTT
		lutt = new ArrayList<TriplePath>();

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
					}
				}
			}
		);				
	}
	
	/** checkLutt
	 * 
	 * checkLutt analyzes triples that have been added and removed
	 * by an update request, in order to see if they match the lutt.
	 * If this method returns true, we can trigger the SPU procesing
	 * 
	 * @param ar added triples
	 * @param dr deleted triples
	 * @return the list of matching bindings
	 */
	public BindingsResults checkLutt(BindingsResults bindingsList) {

		// define a list for the matching patterns
		List<Bindings> matching = new ArrayList<Bindings>();
		BindingsResults matchingbr = new BindingsResults(bindingsList.getVariables(), matching);
			
		// get an iterator
		Iterator<Bindings> ari = bindingsList.getBindings().iterator();
		
		// iterate over the list
		while (ari.hasNext()) {
			
			// get an element and check if it matches the lutt
			Bindings bindings = ari.next();
			
			// if a match for these bindings has been found
			// add the bindings to the list of matching ones
			if (checkBindings(bindings)) {			
				matchingbr.add(bindings);
			}			
		}
		
		// return
		return matchingbr;				
	}

	/** 
	 * This method is used to check if a triple of the lutt
	 * is matched by an added/removed triple.
	 * 
	 * @param bindings
	 * @return	true it the triple matches the lutt, false otherwise
	 */
	private boolean checkBindings(Bindings bindings) {
		
		// iterate over the LUTT
		Iterator<TriplePath> li = lutt.iterator();			
		while (li.hasNext()) {
			
			// get a triple pattern from the lutt
			TriplePath element = li.next();			
			Node lS = element.getSubject();
			Node lP = element.getPredicate();
			Node lO = element.getObject();
			
			// read fields from the added bindings
			if (checkField(lS, bindings.getBindingValue("subject")))
				if (checkField(lP, bindings.getBindingValue("predicate")))
					if (checkField(lO, bindings.getBindingValue("object")))
						return true; // match found															
		}
		
		// no match found		
		return false;		
	}
		
	/** 
	 * This method is used to check if a field of the lutt
	 * is matched by an added/removed binding.
	 * 
	 * @param n
	 * @param s
	 * @return
	 */
	private boolean checkField(Node n, String s) {
		
		if ((n.isVariable()) || (n.isURI() && (n.getURI().toString().equals(s))) || (n.isLiteral() && (n.getLiteral().toString().equals(s)))) {		
			return true;
		}
		else {			
			return false;
		}
	}
	
	
}

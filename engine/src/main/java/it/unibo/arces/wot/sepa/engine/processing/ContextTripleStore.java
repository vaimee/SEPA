package it.unibo.arces.wot.sepa.engine.processing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;

/**
 * The class ContextTripleStore is used to provide a local
 * context store to an SPU (in particular to the SPUSmart.
 * In this way determining notifications becomes faster, since
 * the SPU performs the query on the small local store, instead
 * of the global one.
 * 
 * @author fabioviola
 *
 */
public class ContextTripleStore {

	// Jena model
	private Model cts;

	// the logger
	private final Logger logger;
	
	/**
	 * The constructor of the ContexTripleStore requires only
	 * the ID of the subscription that holds the store.
	 * 
	 * @param uuid The id of the subscription
	 */
	public ContextTripleStore(String uuid){		
		
		// get the logger
		logger = LogManager.getLogger("ContextTripleStore_" + uuid);	
		logger.debug("Initializing local context store");
		
		// initialize an empty model
		cts = ModelFactory.createDefaultModel();		
	}
	
	/**
	 * insertFromBindings is used to fill the local store with
	 * data coming from the results of a Construct. This method 
	 * is used during the initialization of an SPUSmart to put
	 * the initial results into the local context store.
	 * 
	 * @param consBindings the results of the construct query
	 */
	
	// insert from construct results
	public void insertFromBindings(BindingsResults consBindings) {
		
		// debug
		logger.debug("Pushing data into the local store");
		
		// get the list of bindings
		List<Bindings> consBindingsList = consBindings.getBindings();
		
		// get an iterator for the previous list
		Iterator<Bindings> consBindingsListIter = consBindingsList.iterator();		
		
		// iterate over bindings
		while (consBindingsListIter.hasNext()) {

			// retrieve a binding row
			Bindings el = consBindingsListIter.next();
			
			// read subject, predicate and object
			Resource s = cts.createResource(el.getBindingValue("subject"));
			Property p = cts.createProperty(el.getBindingValue("predicate"));			
			String o = el.getBindingValue("object");
			
			// analyze the object to detect if it is a datatype or an object property
			// then add the property to the model (i.e. to insert a statement)
			if (el.isLiteral("object")) {				
				s.addProperty(p, o);				 
			} else { // object property							
				s.addProperty(p, cts.createResource(o));
			}												
		}		
	}

	// insert from construct results
	public void removeFromBindings(BindingsResults consBindings) {
		
		// debug
		logger.debug("Pushing data into the local store");
		
		// get the list of bindings
		List<Bindings> consBindingsList = consBindings.getBindings();
		
		// get an iterator for the previous list
		Iterator<Bindings> consBindingsListIter = consBindingsList.iterator();		
		
		// iterate over bindings
		while (consBindingsListIter.hasNext()) {

			// retrieve a binding row
			Bindings el = consBindingsListIter.next();
			
			// read subject, predicate and object
			Resource s = cts.getResource(el.getBindingValue("subject"));			
			Property p = cts.getProperty(el.getBindingValue("predicate"));			
			String o = el.getBindingValue("object");			
			
			// analyze the object to detect if it is a datatype or an object property
			// then add the property to the model (i.e. to insert a statement)
			if (el.isLiteral("object")) {			
				Literal oo = cts.createLiteral(o);
				cts.removeAll(s, p, oo);				 
			} else { // object property							
				Resource oo = cts.getResource(o); 				
				cts.removeAll(s, p, oo);
			}												
		}		
	}
	
	
	
	/**
	 * This method is used to perform a query to the local store.
	 * Since the local store is handled by a Jena model, this 
	 * method also converts results of the query to BindingResults.
	 * 
	 * @param queryText The SPARQL code of the query
	 * @return results of the query
	 */
	public BindingsResults query(String queryText) {
		
		// debug info
		logger.debug("Querying the local store");
		
		// Query to the local store
		Query qry = QueryFactory.create(queryText);
		QueryExecution qe = QueryExecutionFactory.create(qry, cts);
		ResultSet rs = qe.execSelect();		
		
		// Now we have to convert the results to Bindings,
		// so first of all initialize variables for bindings			
		List<Bindings> bl = new ArrayList<Bindings>();
		Set<String> varset = new HashSet<String>(rs.getResultVars());
		BindingsResults br = new BindingsResults(varset, bl);
		
		// iterate over rows of the results		
		while (rs.hasNext()) {			
			QuerySolution el = rs.next();									
			Bindings binding = new Bindings();
			
			// iterate over cells of a row
			Iterator<String> vars = el.varNames();
			while (vars.hasNext()) {
				
				// add the binding
				String varName = vars.next();
				RDFNode cellValue = el.get(varName);				
				if (cellValue.isLiteral()) {	
					RDFTermLiteral cellValueL = new RDFTermLiteral(cellValue.toString());
					binding.addBinding(varName, cellValueL);
				} else {
					RDFTermURI cellValueU = new RDFTermURI(cellValue.toString());
					binding.addBinding(varName, cellValueU);
				}												
			}					
			br.add(binding);		
		}
		
		// return
		return br;
		
	}
	
	public long getCtsSize() {
		return cts.size();
	}
	
}

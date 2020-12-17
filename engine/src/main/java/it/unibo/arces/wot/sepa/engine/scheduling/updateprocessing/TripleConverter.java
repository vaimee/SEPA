package it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermBNode;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec.EpSpecFactory;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec.IEndPointSpecification;

public class TripleConverter {

	public static String nuplaToString(Bindings triple) throws SEPABindingsException {
		if(triple.getVariables().size()<1){
			return null;
		}
		String tripl = "";
		for (String var : triple.getVariables()) {
			tripl+=triple.isURI(var)? "<"+triple.getValue(var)+">": "\""+triple.getValue(var)+"\"" ;
			tripl+= " ";
		}				
		tripl+=" .";
		return tripl;
		
	} 
	
	public static String tripleToString(Bindings triple) throws SEPABindingsException {
		IEndPointSpecification eps = EpSpecFactory.getInstance();
		if(triple.getVariables().contains(eps.s()) && triple.getVariables().contains(eps.p())  && triple.getVariables().contains(eps.o()) ) {
			String s =triple.isURI(eps.s())? "<"+triple.getValue(eps.s())+">": "\""+triple.getValue(eps.s())+"\"";			
			String p =triple.isURI(eps.p())? "<"+triple.getValue(eps.p())+">": "\""+triple.getValue(eps.p())+"\"";
			String o =triple.isURI(eps.o())? "<"+triple.getValue(eps.o())+">": "\""+triple.getValue(eps.o())+"\"";
			return s+ " "+ p + " " + o;
		}else {
			return null;
		}		
	} 

	public static Triple bindingToTriple(Bindings bindings) throws SEPABindingsException{
		IEndPointSpecification eps = EpSpecFactory.getInstance();
		String subject = bindings.getValue(eps.s());
		String predicate = bindings.getValue(eps.p());
		String object = bindings.getValue(eps.o());			
		
		Node s = bindings.isBNode(eps.s()) ? NodeFactory.createBlankNode(subject) : NodeFactory.createURI(subject);
		Node p = bindings.isBNode(eps.p()) ? NodeFactory.createBlankNode(predicate) : NodeFactory.createURI(predicate);

		Node o = null;
		if(!bindings.isBNode(eps.o())){
			o = bindings.isURI(eps.o()) ? NodeFactory.createURI(object) : NodeFactory.createLiteral(object);
		}else{
			o = NodeFactory.createBlankNode(object);
		}

		return new Triple(s,p,o);
	}

	public static Bindings convertTripleToBindings(Triple t) {
		IEndPointSpecification eps = EpSpecFactory.getInstance();
		Bindings temp = new Bindings();
		if(t.getSubject().isLiteral()){
			temp.addBinding(eps.s(), new RDFTermLiteral(t.getSubject().getLiteral().toString())); //this is not allowed in SPARQL1.1
		}else if(t.getSubject().isURI()) {
			temp.addBinding(eps.s(), new RDFTermURI(t.getSubject().getURI()));			
		}else if(t.getSubject().isBlank()) {
			temp.addBinding(eps.s(), new RDFTermBNode(t.getSubject().toString()));		
		}else {
			System.out.println("Warning, cannot convert Subject of Triple to Bindings, for triple: "+t.toString());
		}
		if(t.getPredicate().isLiteral()){
			temp.addBinding(eps.p(), new RDFTermLiteral(t.getPredicate().getLiteral().toString())); //this is not allowed in SPARQL1.1
		}else if(t.getPredicate().isURI()) {
			temp.addBinding(eps.p(), new RDFTermURI(t.getPredicate().getURI()));			
		}else if(t.getPredicate().isBlank()) {
			temp.addBinding(eps.p(), new RDFTermBNode(t.getPredicate().toString()));		
		}else {
			System.out.println("Warning, cannot convert Predicate of Triple to Bindings, for triple: "+t.toString());
		}
		if(t.getObject().isLiteral()){
			temp.addBinding(eps.o(), new RDFTermLiteral(t.getObject().getLiteral().toString())); 
		}else if(t.getObject().isURI()) {
			temp.addBinding(eps.o(), new RDFTermURI(t.getObject().getURI()));			
		}else if(t.getObject().isBlank()) {
			temp.addBinding(eps.o(), new RDFTermBNode(t.getObject().toString()));		
		}else {
			System.out.println("Warning, cannot convert Object of Triple to Bindings, for triple: "+t.toString());
		}
		return temp;
	}
	
	
	
}

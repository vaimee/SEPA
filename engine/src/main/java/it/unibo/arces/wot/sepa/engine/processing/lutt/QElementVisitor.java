package it.unibo.arces.wot.sepa.engine.processing.lutt;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.TriplePath;
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

public class QElementVisitor implements ElementVisitor{

	//----------------------------------SUPPORT FOR VISITOR (in order to build LUTT)
	private HashMap<String,ArrayList<LUTTTriple>> quads=null;
	private ArrayList<LUTTTriple> triple=null;
	private ArrayList<LUTTTriple> jolly_triple=null;

	public QElementVisitor() {
		this.jolly_triple= new ArrayList<LUTTTriple>();
		this.triple= new ArrayList<LUTTTriple>();
		this.quads=new HashMap<String,ArrayList<LUTTTriple>>();
	}
	
	public boolean hasTriples() {
		return triple.size()>0;
	}
	
	public boolean hasQuads() {
		return quads.keySet().size()>0;
	}
	
	public boolean hasJollyTriples() {
		return  jolly_triple.size()>0;
	}
	
	public void addJollyTriple(LUTTTriple t) {
		jolly_triple.add(t);
	}
	
	public void addTriple(LUTTTriple t) {
		triple.add(t);
	}
	
	public void addQuads(String graph,LUTTTriple t) {
		if(quads.containsKey(graph)) {
			quads.get(graph).add(t);
		}else {
			ArrayList<LUTTTriple> lt = new ArrayList<LUTTTriple>();
			lt.add(t);
			quads.put(graph, lt);
		}
	}
	
	public void addJollyTriple(ArrayList<LUTTTriple> t) {
		jolly_triple.addAll(t);
	}
	
	public void addTriple(ArrayList<LUTTTriple> t) {
		triple.addAll(t);
	}
	
	public void addQuads(String graph,ArrayList<LUTTTriple> t) {
		if(quads.containsKey(graph)) {
			quads.get(graph).addAll(t);
		}else {
			quads.put(graph, t);
		}
	}
	
	public HashMap<String, ArrayList<LUTTTriple>> getQuads() {
		return quads;
	}

	public ArrayList<LUTTTriple> getTriple() {
		return triple;
	}

	public ArrayList<LUTTTriple> getJollyTriple() {
		return jolly_triple;
	}

	/*
	 * The namedGraph will substitute all the variable-graph in the query
	 * we can have more than one FROM NAMED clause,
	 * so exist the "removeJollyTriple" parameter.
	 * If "removeJollyTriple" is TRUE the triple list that correspond to a variable-graph
	 * will be cleaned. "removeJollyTriple" must be true just for the last FROM NAMED graph.
	 */
	public void addFromNamed(String namedGraph,boolean removeJollyTriple) {
		jolly_triple.forEach((LUTTTriple t)->{
			if(quads.keySet().contains(namedGraph)){
				quads.get(namedGraph).add(t);
			}else {
				ArrayList<LUTTTriple> triples = new ArrayList<LUTTTriple>();
				triples.add(t);
				quads.put(namedGraph, triples);	
			}
		});
		if(removeJollyTriple) {
			jolly_triple.clear();
		}
	}
	
	/*
	 * If we have one or more FROM clauses,
	 * it's required to use that method, 
	 * otherwise the triples that hasen't a graph will be lost
	 * and the LUTT will not has that triples.
	 * (if the query hasn't a FROM clause,
	 *  pls use the "setNoFromClause" method.)
	 */
	public void addFrom(String graph) {
		triple.forEach((LUTTTriple t)->{
			if(quads.keySet().contains(graph)){
				quads.get(graph).add(t);
			}else {
				ArrayList<LUTTTriple> triples = new ArrayList<LUTTTriple>();
				triples.add(t);
				quads.put(graph, triples);	
			}
		});
	}
	
	/*
	 * That method will add the remaining triples (without a graph)
	 * to the jolly list
	 */
	public void setNoFromClause() {
		if(triple.size()>0) {
			jolly_triple.addAll(triple);
		}
	}
	
	//----------------------------------REAL VISITOR
	@Override
	public void visit(ElementTriplesBlock arg0) {
		// TODO Auto-generated method stub
		System.out.println("QElementVisitor.ElementTriplesBlock");
	}
	
	@Override
	public void visit(ElementPathBlock arg0) {
		// TODO Auto-generated method stub
		arg0.getPattern().getList().forEach((TriplePath tp)->{
			String s = convertNode(tp.getSubject());
			String p = convertNode(tp.getPredicate());
			String o = convertNode(tp.getObject());
			this.addTriple(new LUTTTriple(s, p,o));
		});

//		System.out.println("QElementVisitor.ElementPathBlock");
	}
	
	@Override
	public void visit(ElementFilter arg0) {
		// TODO Auto-generated method stub
//		arg0.getExpr().visit(this);

		System.out.println("QElementVisitor.ElementFilter");
	}
	
	@Override
	public void visit(ElementAssign arg0) {
		// TODO Auto-generated method stub

		System.out.println("QElementVisitor.ElementAssign");
	}
	
	@Override
	public void visit(ElementBind arg0) {
		// TODO Auto-generated method stub

		System.out.println("QElementVisitor.ElementBind");
	}
	
	@Override
	public void visit(ElementData arg0) {
		// TODO Auto-generated method stub

		System.out.println("QElementVisitor.ElementData");
	}
	
	@Override
	public void visit(ElementUnion arg0) {
		arg0.getElements().forEach((Element e)->{
			e.visit(this);
		});
//		System.out.println("QElementVisitor.ElementUnion");
	}
	
	@Override
	public void visit(ElementOptional arg0) {
		// TODO Auto-generated method stub

		System.out.println("QElementVisitor.ElementOptional");
	}
	
	@Override
	public void visit(ElementGroup arg0) {
		//that visit is the first
		arg0.getElements().forEach((Element e)->{
			e.visit(this);
		});
//		System.out.println("QElementVisitor.ElementGroup");
	}
	
	@Override
	public void visit(ElementDataset arg0) {
		// TODO Auto-generated method stub

		System.out.println("QElementVisitor.ElementDataset");
	}
	
	@Override
	public void visit(ElementNamedGraph arg0) {
		QElementVisitor partial=new QElementVisitor();
		arg0.getElement().visit(partial);
		if(partial.hasTriples()) {
			if(arg0.getGraphNameNode().isVariable()) {
				this.addJollyTriple(partial.getTriple());
			}else if(arg0.getGraphNameNode().isURI()){
				this.addQuads(arg0.getGraphNameNode().getURI(), partial.getTriple());
			}else if(arg0.getGraphNameNode().isBlank()){
				//that can be triky
				//it should't be appen, but we consider it as graph-var
				this.addJollyTriple(partial.getTriple());
			}
		}
		//else {}//that should't appen
//		System.out.println("QElementVisitor.ElementNamedGraph");
	}
	
	@Override
	public void visit(ElementExists arg0) {
		// TODO Auto-generated method stub

		System.out.println("QElementVisitor.ElementExists");
	}
	
	@Override
	public void visit(ElementNotExists arg0) {
		// TODO Auto-generated method stub

		System.out.println("QElementVisitor.ElementNotExists");
	}
	
	@Override
	public void visit(ElementMinus arg0) {
		// TODO Auto-generated method stub

		System.out.println("QElementVisitor.ElementMinus");
	}
	
	@Override
	public void visit(ElementService arg0) {
		// TODO Auto-generated method stub

		System.out.println("QElementVisitor.ElementService");
	}
	
	@Override
	public void visit(ElementSubQuery arg0) {
		// TODO Auto-generated method stub

		System.out.println("QElementVisitor.ElementSubQuery");
	}
	
	//---------------------------UTILS
	private String convertNode(Node n) {
		if(n.isVariable() || n.isBlank()) {
			return null;
		}else if(n.isURI()){
			return n.getURI();
		}else {
			return n.getLiteralLexicalForm();
		}
	}
}
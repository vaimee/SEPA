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

	public QElementVisitor() {}
	
	public boolean hasTriples() {
		return triple!=null && triple.size()>0;
	}
	
	public boolean hasQuads() {
		return quads!=null && quads.keySet().size()>0;
	}
	
	public boolean hasJollyTriples() {
		return jolly_triple!=null && jolly_triple.size()>0;
	}
	
	public void safeAddJollyTriple(LUTTTriple t) {
		if(jolly_triple==null) {
			jolly_triple= new ArrayList<LUTTTriple>();
		}
		jolly_triple.add(t);
	}
	
	public void safeAddTriple(LUTTTriple t) {
		if(triple==null) {
			triple= new ArrayList<LUTTTriple>();
		}
		triple.add(t);
	}
	
	public void safeAddQuads(String graph,LUTTTriple t) {
		if(quads==null) {
			quads= new HashMap<String,ArrayList<LUTTTriple>>();
		}
		if(quads.containsKey(graph)) {
			quads.get(graph).add(t);
		}else {
			ArrayList<LUTTTriple> lt = new ArrayList<LUTTTriple>();
			lt.add(t);
			quads.put(graph, lt);
		}
	}
	
	public void safeAddJollyTriple(ArrayList<LUTTTriple> t) {
		if(jolly_triple==null) {
			jolly_triple= new ArrayList<LUTTTriple>();
		}
		jolly_triple.addAll(t);
	}
	
	public void safeAddTriple(ArrayList<LUTTTriple> t) {
		if(triple==null) {
			triple= new ArrayList<LUTTTriple>();
		}
		triple.addAll(t);
	}
	
	public void safeAddQuads(String graph,ArrayList<LUTTTriple> t) {
		if(quads==null) {
			quads= new HashMap<String,ArrayList<LUTTTriple>>();
		}
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

	//----------------------------------REAL VISITOR
	@Override
	public void visit(ElementTriplesBlock arg0) {
		// TODO Auto-generated method stub
		String s="";
	}
	
	@Override
	public void visit(ElementPathBlock arg0) {
		// TODO Auto-generated method stub
		arg0.getPattern().getList().forEach((TriplePath tp)->{
			String s = convertNode(tp.getSubject());
			String p = convertNode(tp.getPredicate());
			String o = convertNode(tp.getObject());
			this.safeAddTriple(new LUTTTriple(s, p,o));
		});
	}
	
	@Override
	public void visit(ElementFilter arg0) {
		// TODO Auto-generated method stub
//		arg0.getExpr().visit(this);
		String s="";
	}
	
	@Override
	public void visit(ElementAssign arg0) {
		// TODO Auto-generated method stub

		String s="";
	}
	
	@Override
	public void visit(ElementBind arg0) {
		// TODO Auto-generated method stub

		String s="";
	}
	
	@Override
	public void visit(ElementData arg0) {
		// TODO Auto-generated method stub

		String s="";
	}
	
	@Override
	public void visit(ElementUnion arg0) {
		// TODO Auto-generated method stub

		String s="";
	}
	
	@Override
	public void visit(ElementOptional arg0) {
		// TODO Auto-generated method stub

		String s="";
	}
	
	@Override
	public void visit(ElementGroup arg0) {
		//that visit is the first
		arg0.getElements().forEach((Element e)->{
			e.visit(this);
		});
		String s="";
	}
	
	@Override
	public void visit(ElementDataset arg0) {
		// TODO Auto-generated method stub

		String s="";
	}
	
	@Override
	public void visit(ElementNamedGraph arg0) {
		QElementVisitor partial=new QElementVisitor();
		arg0.getElement().visit(partial);
		if(partial.hasTriples()) {
			if(arg0.getGraphNameNode().isVariable()) {
				this.safeAddJollyTriple(partial.getTriple());
			}else if(arg0.getGraphNameNode().isURI()){
				this.safeAddQuads(arg0.getGraphNameNode().getURI(), partial.getTriple());
			}else if(arg0.getGraphNameNode().isBlank()){
				//that can be triky
				//it should't be appen, but we consider it as graph-var
				this.safeAddJollyTriple(partial.getTriple());
			}
		}
		//else {}//that should't appen
		String s="";
	}
	
	@Override
	public void visit(ElementExists arg0) {
		// TODO Auto-generated method stub

		String s="";
	}
	
	@Override
	public void visit(ElementNotExists arg0) {
		// TODO Auto-generated method stub

		String s="";
	}
	
	@Override
	public void visit(ElementMinus arg0) {
		// TODO Auto-generated method stub

		String s="";
	}
	
	@Override
	public void visit(ElementService arg0) {
		// TODO Auto-generated method stub

		String s="";
	}
	
	@Override
	public void visit(ElementSubQuery arg0) {
		// TODO Auto-generated method stub

		String s="";
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

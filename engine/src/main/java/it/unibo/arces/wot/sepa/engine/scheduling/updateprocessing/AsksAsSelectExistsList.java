package it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.engine.processing.QueryProcessor;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec.EpSpecFactory;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec.IEndPointSpecification;

public class AsksAsSelectExistsList implements IAsk{
	
	/*
	 * Example:
	 * 
			
		SELECT ?x ?i {
			VALUES (?g ?s ?p ?o ?i) {
				(<prova3><s0><p0><o0>0)
				(<prova3><s1><p1><o1>1)
				(<prova2><s2><p2><o2>2)
			}
			BIND(EXISTS{GRAPH ?g {?s ?p ?o}} AS ?x)
		}

	 */

	private HashMap<Integer,BindingsWrapper> tripleList;
	private HashMap<String,BindingsResults>  added=null;
	private HashMap<String,BindingsResults>  removed=null;
	private ArrayList<UpdateExtractedData> ueds = new ArrayList<UpdateExtractedData> ();
	private InternalUpdateRequest req;
	private QueryProcessor processor;
	
	public AsksAsSelectExistsList(ArrayList<UpdateExtractedData> ueds, InternalUpdateRequest req, QueryProcessor processor) throws NumberFormatException, SEPASecurityException, IOException, SEPABindingsException {
		this.ueds=ueds;
		this.req=req;
		this.processor=processor;
		this.init();
	}
	
	protected void init() throws NumberFormatException, SEPASecurityException, IOException, SEPABindingsException {
		int orderIndex = 0;
		tripleList= new HashMap<Integer,BindingsWrapper>();	
		removed= new HashMap<String,BindingsResults>();
		added= new HashMap<String,BindingsResults>();
		String values = "";
		for (UpdateExtractedData updateConstruct : ueds) {
			if(updateConstruct.needDelete()) {
				String deleteGraph= updateConstruct.getRemovedGraph();
				for (Bindings bind : updateConstruct.getRemoved().getBindings()) {
					tripleList.put(orderIndex,new BindingsWrapper(bind,orderIndex,false,deleteGraph));
					values+=incapsulate(deleteGraph,bind,orderIndex);
					orderIndex++;
				}
			}
			if(updateConstruct.needInsert()) {
				String addedGraph= updateConstruct.getAddedGraph();
				for (Bindings bind : updateConstruct.getAdded().getBindings()) {
					tripleList.put(orderIndex,new BindingsWrapper(bind,orderIndex,false,addedGraph));
					values+=incapsulate(addedGraph,bind,orderIndex);
					orderIndex++;
				}
			}
		}
		if(orderIndex>0) {
			for (Bindings bind : getBindings(generateSelect(values)).getBindings()) {
				BindingsWrapper temp =tripleList.get(Integer.parseInt(bind.getValue("i")));
				if(EpSpecFactory.getInstance().asksAsSelectExistListCompare(bind.getValue("x"))) {
					if(temp.isAdded()) {
						addToAdded(temp.getGraph(), temp.getBind());
					}else {
						//tripla di tipo REMOVED che risulta presente sulla KB
						addToRemoved(temp.getGraph(), temp.getBind());
					}
				}
			}
		}
	}
	
	

	private void addToAdded(String graph, Bindings bind) {
		if(added.containsKey(graph)) {
			added.get(graph).add(bind);
		}else {
			ArrayList<Bindings> bindList = new ArrayList<Bindings>();
			bindList.add(bind);
			ArrayList<String> vars = EpSpecFactory.getInstance().vars();
			added.put(graph,new BindingsResults(vars, bindList));	
		}
	}
	
	private void addToRemoved(String graph, Bindings bind) {	
		if(removed.containsKey(graph)) {
			removed.get(graph).add(bind);
		}else {
			ArrayList<Bindings> bindList = new ArrayList<Bindings>();
			bindList.add(bind);
			ArrayList<String> vars =EpSpecFactory.getInstance().vars();
			removed.put(graph,new BindingsResults(vars, bindList));	
		}
	}
	
	protected String generateSelect(String values) {
		IEndPointSpecification eps = EpSpecFactory.getInstance();
		return "SELECT ?x ?i {VALUES (?"+eps.g()
			+" ?"+eps.s()
			+" ?"+eps.p()
			+" ?"+eps.o()
			+" ?i) { \n" + values+ "} BIND(EXISTS{GRAPH ?"+eps.g()
			+" {?"+eps.s()
			+" ?"+eps.p()
			+ "?"+eps.o()+"}} AS ?x)}";
	}
	
	protected String incapsulate(String graph,Bindings bind,int index ) throws SEPABindingsException {
		String t = TripleConverter.tripleToString(bind);
		if(t==null) {
			throw new SEPABindingsException("Not valid bind to incapsulate.");
		}
		return "(<"+graph+">"+t+" "+index+")\n";
	}
	

	
	public BindingsResults getBindings(String query) throws SEPASecurityException, IOException  {	
		InternalQueryRequest askquery=new InternalQueryRequest(
				query,
				req.getDefaultGraphUri(),
				req.getNamedGraphUri(),
				req.getClientAuthorization()
				);
		
		return ((QueryResponse)processor.process(askquery)).getBindingsResults();
		
	}
	

	public ArrayList<UpdateExtractedData> filter() throws SEPABindingsException {
		HashMap<String,BindingsResults> alredyExist_E  = this.getReorganizedBindingsForAdded();
		HashMap<String,BindingsResults> realRemoved_E = this.getReorganizedBindingsForRemoved();
		for (UpdateExtractedData constructs : ueds) {
			
			String graph = constructs.getAddedGraph();
			
			if(constructs.needInsert() && alredyExist_E.containsKey(graph) ){
				constructs.removeBingingFromAddedList(alredyExist_E.get(graph)); 
				alredyExist_E.remove(graph);
			}	
			
			graph = constructs.getRemovedGraph();
			if(constructs.needDelete()) {
				if(realRemoved_E.containsKey(graph)) {
					constructs.setRemoved(realRemoved_E.get(graph));				
					realRemoved_E.remove(graph);
				}else {
					constructs.clearRemoved();
				}
			}
		
			
		}
		return ueds;
	}
	
	
	public HashMap<String,BindingsResults> getReorganizedBindingsForAdded() throws SEPABindingsException  {
		
		return added;		
		
	}
	
	
	public HashMap<String,BindingsResults> getReorganizedBindingsForRemoved() throws SEPABindingsException  {
		
		return removed;		
		
	}

	public boolean needAskSelectForAdded() {
		return   added!=null &&   added.size()>0;
	}


	public boolean needAskSelectForRemoved() {
		return   removed!=null &&   removed.size()>0;
	}


	
}

class BindingsWrapper{
	private Bindings bind;
	private int listNumer;
	private boolean added;
	private String graph;
	
	public BindingsWrapper(Bindings bind, int listNumer, boolean added,String graph) {
		super();
		this.bind = bind;
		this.listNumer = listNumer;
		this.added = added;
		this.graph=graph;
	}
	public Bindings getBind() {
		return bind;
	}
	public void setBind(Bindings bind) {
		this.bind = bind;
	}
	public int getListNumer() {
		return listNumer;
	}
	public void setListNumer(int listNumer) {
		this.listNumer = listNumer;
	}
	public boolean isAdded() {
		return added;
	}
	public void setAdded(boolean added) {
		this.added = added;
	}
	public String getGraph() {
		return graph;
	}
	public void setGraph(String graph) {
		this.graph = graph;
	}
	
	
}

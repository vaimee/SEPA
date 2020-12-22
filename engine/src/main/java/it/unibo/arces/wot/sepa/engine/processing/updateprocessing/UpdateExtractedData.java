package it.unibo.arces.wot.sepa.engine.processing.updateprocessing;

import java.util.ArrayList;

import org.apache.jena.graph.Triple;


import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.engine.processing.epspec.EpSpecFactory;


public class UpdateExtractedData {

	//---------------------------------Construct data
    private String deleteConstruct=null;
    private String insertConstruct=null;
    private boolean skipConstruct=false;
    
    //---------------------------------ASK data
	private BindingsResults added=null;
	private BindingsResults removed=null;
	private String  addedGraph=null;
	private String  removedGraph=null;

	
	
	private UpdateExtractedData(String deleteConstruct, String insertConstruct, boolean skipConstruct,
			BindingsResults added, BindingsResults removed, String addedGraph, String removedGraph) {
		super();
		this.deleteConstruct = deleteConstruct;
		this.insertConstruct = insertConstruct;
		this.skipConstruct = skipConstruct;
		this.added = added;
		this.removed = removed;
		this.addedGraph = addedGraph;
		this.removedGraph = removedGraph;
	}//for clone



	public UpdateExtractedData(ArrayList<Triple> r,ArrayList<Triple> a, String graph) {
		ArrayList<String> vars = EpSpecFactory.getInstance().vars();
		
		this.added= new BindingsResults(vars,  new ArrayList<Bindings>());
		if(a!=null) {
			for (Triple triple : a) {
				this.added.add(TripleConverter.convertTripleToBindings(triple));
			}
		}
			
		this.removed=new BindingsResults(vars,  new ArrayList<Bindings>());
		if(r!=null) {
			for (Triple triple : r) {
				this.removed.add(TripleConverter.convertTripleToBindings(triple));
			}
		}
//		System.out.println("this.removed" + this.removed.size());
		this.addedGraph=graph;
		this.removedGraph=graph;
		this.skipConstruct=true;
	}
	
    public UpdateExtractedData(String deleteConstruct, String insertConstruct ){
        if(deleteConstruct == null || insertConstruct == null){
            throw new IllegalArgumentException("Construct query cannot be null");
        }

        this.deleteConstruct = deleteConstruct;
        this.insertConstruct = insertConstruct;
    }
    public UpdateExtractedData(String deleteConstruct, String insertConstruct,String deleteGraph,String insertGraph ){
        if(deleteConstruct == null || insertConstruct == null){
            throw new IllegalArgumentException("Construct query cannot be null");
        }

        this.deleteConstruct = deleteConstruct;
        this.insertConstruct = insertConstruct;
        if(deleteGraph.length()>0) {
            this.removedGraph=deleteGraph;
        }  
        if(insertGraph.length()>0) {
        	this.addedGraph=insertGraph;
        }
    }
    
    public UpdateExtractedData(String deleteConstruct, String insertConstruct,String graph){
        if(deleteConstruct == null || insertConstruct == null){
            throw new IllegalArgumentException("Construct query cannot be null");
        }

        this.deleteConstruct = deleteConstruct;
        this.insertConstruct = insertConstruct;
        if(graph.length()>0) {
            this.removedGraph=graph;
        	this.addedGraph=graph;
        }  
    }
	public boolean needInsert() {
		return added!=null && added.size()>0;
	}
	
	public boolean needDelete() {
		return removed!=null && removed.size()>0;
	}
	

	
	public BindingsResults getAdded() {
		return added;
	}


	public void setAdded(BindingsResults added) {
		this.added = added;
	}


	public BindingsResults getRemoved() {
		return removed;
	}


	public void setRemoved(BindingsResults removed) {
		this.removed = removed;
	}
	
	
	public BindingsResults addRemovedGraphVar() {
		if(this.removed==null) {
			return null;
		}
		for(Bindings bind : this.removed.getBindings()) {
			bind.addBinding(EpSpecFactory.getInstance().g(), new RDFTermURI(this.removedGraph));	
		}
		return this.removed;
	}
	
	public String getAddedGraph() {
		return addedGraph;
	}


	public void setAddedGraph(String addedGraph) {
		this.addedGraph = fixGraphIfNeed(addedGraph);
	}


	public String getRemovedGraph() {
		return removedGraph;
	}


	public void setRemovedGraph(String removedGraph) {
		this.removedGraph = fixGraphIfNeed(removedGraph);
	}


	public void setGraph(String graph) {
		this.setAddedGraph(graph);
		this.setRemovedGraph(graph);
	}
	

	public boolean isSkipConstruct() {
		return skipConstruct;
	}
	


	
	public void removeBingingFromAddedList(Bindings bindings) {
		if(this.added !=null) {
			this.added.remove(bindings);
		}else {
			System.out.println("Warning: added BindingResult is null");
		}
	}
	
	public void removeBingingFromRemovedList(Bindings bindings) {
		if(this.removed !=null) {
			this.removed.remove(bindings);
		}else {
			System.out.println("Warning: removed BindingResult is null");
		}	
	}
	
	
	public void removeBingingFromAddedList(BindingsResults bindings) {
		if(this.added !=null) {
			for (Bindings b : bindings.getBindings()) {
				this.added.remove(b);
			}			
		}else {
			System.out.println("Warning: added BindingResult is null");
		}
	}
	
	
	
	public BindingsResults addAddedGraphVar() {
		if(this.added==null) {
			return null;
		}
		for(Bindings bind : this.added.getBindings()) {
			bind.addBinding(EpSpecFactory.getInstance().g(), new RDFTermURI(this.addedGraph));	
		}
		return this.added;
	}
	
	public void clearRemoved() {
		removed=null;
	}
	
	
	/**
     * Get delete construct string. An empty string indicates that there are no deleted
     * triples
     * @return a construct sparql query string
     */
    public String getInsertConstruct() {
        return insertConstruct;
    }

    /**
     * Get delete construct string. An empty string indicates that there are no deleted
     * triples
     * @return a construct sparql query string
     */
    public String getDeleteConstruct() {
        return deleteConstruct;
    }
    
    private String fixGraphIfNeed(String g) {
    	if(g==null) {
    		return null;
    	}
    	String ris = g;
    	if(!g.contains("<")) {
    		ris="<"+ris;
    	}
    	if(!g.contains(">")) {
    		ris+=">";
    	}
    	return ris;
    }
    public UpdateExtractedData clone() {
    	return new UpdateExtractedData(deleteConstruct,  insertConstruct,  skipConstruct,
    			new BindingsResults(added.toJson()),new BindingsResults(removed.toJson()) , addedGraph,  removedGraph) ;
    }
    
   
}

package it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.modify.request.*;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec.EpSpecFactory;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec.IEndPointSpecification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SPARQLAnalyzer {


	class ToConstructUpdateVisitor extends UpdateVisitorBase {
		private ArrayList<UpdateExtractedData> results = new ArrayList<UpdateExtractedData>();
		private IEndPointSpecification eps = EpSpecFactory.getInstance();
		@Override
		public void visit(UpdateDataInsert updateDataInsert) {
			logger.debug("Visit on UpdateDataInsert");
			ConstructGenerator cg = new ConstructGenerator(updateDataInsert.getQuads());	
			HashMap<String,ArrayList<Triple>> insertTriples =cg.getAllTriple();
			for (String graph :insertTriples.keySet()) {
				results.add(new UpdateExtractedData(null,insertTriples.get(graph),graph));
			}
		}

		@Override
		public void visit(UpdateDataDelete updateDataDelete) {
			logger.debug("Visit on UpdateDataDelete");
			ConstructGenerator cg = new ConstructGenerator(updateDataDelete.getQuads());	
			HashMap<String,ArrayList<Triple>> deleteTriples =cg.getAllTriple();
			for (String graph :deleteTriples.keySet()) {
				results.add(new UpdateExtractedData(deleteTriples.get(graph),null,graph));
			}
		}

		@Override
		public void visit(UpdateDeleteWhere updateDeleteWhere) {
			logger.debug("Visit on UpdateDeleteWhere");
			ConstructGenerator cg = new ConstructGenerator(updateDeleteWhere.getQuads());
			HashMap<String,String> deleteStrings =cg.getConstructsWithGraphs(true);
			for (String graph : deleteStrings.keySet()) {
				results.add(new UpdateExtractedData(deleteStrings.get(graph), "",graph,""));
			}
		}

		@Override
		public void visit(UpdateModify updateModify) {
			logger.debug("Visit on UpdateModify");
			HashMap<String,String> insertStrings=null;
			HashMap<String,String> deleteStrings=null;		
//			updateModify.getUsingNamed()
//			updateModify.getUsing()
			if (updateModify.hasDeleteClause() && !updateModify.getDeleteAcc().getQuads().isEmpty()) {				
				ConstructGenerator cg = new ConstructGenerator(updateModify.getDeleteAcc().getQuads(),updateModify.getWithIRI());				
				deleteStrings=cg.getConstructsWithGraphs(updateModify.getWherePattern().toString());				
			}

			if (updateModify.hasInsertClause() && !updateModify.getInsertAcc().getQuads().isEmpty()) {
				ConstructGenerator cg = new ConstructGenerator(updateModify.getInsertAcc().getQuads(),updateModify.getWithIRI());	
				insertStrings=cg.getConstructsWithGraphs(updateModify.getWherePattern().toString());		
			}
			
			if(insertStrings!=null && deleteStrings!=null) {
				Set<String> graphs = new HashSet<String>(insertStrings.keySet());
				graphs.addAll(deleteStrings.keySet());
				for (String graph : graphs) {
					String deleteString="";
					String insertString="";
					if(insertStrings.containsKey(graph)) {
						insertString=insertStrings.get(graph);
					}
					if(deleteStrings.containsKey(graph)) {
						deleteString=deleteStrings.get(graph);
					}
					results.add(new UpdateExtractedData(deleteString, insertString,graph));
				}
			}else if(insertStrings==null) {
				for (String graph : deleteStrings.keySet()) {
					results.add(new UpdateExtractedData(deleteStrings.get(graph), "",graph));
				}
			}else {//deleteStrings==null
				for (String graph : insertStrings.keySet()) {
					results.add(new UpdateExtractedData("", insertStrings.get(graph),graph));
				}
			}			
		}

		@Override
		public void visit(UpdateClear update) {
			logger.debug("Visit on UpdateClear");
			String deleteConstruct = "CONSTRUCT { ?"+eps.s()
					+" ?"+eps.p()
					+" ?"+eps.o()
					+" } WHERE { GRAPH <" + update.getGraph().getURI()
					+ "> { ?"+eps.s()
					+" ?"+eps.p()
					+" ?"+eps.o()+" } . }";
			results.add(new UpdateExtractedData(deleteConstruct, "",update.getGraph().getURI(),""));
		
		}

		@Override
		public void visit(UpdateDrop update) {
			logger.debug("Visit on UpdateDrop");
			String deleteConstruct = "CONSTRUCT { ?"+eps.s()
					+" ?"+eps.p()
					+" ?"+eps.o()
					+" } WHERE { GRAPH <" + update.getGraph().getURI()
					+ "> { ?"+eps.s()
					+" ?"+eps.p()
					+" ?"+eps.o()+" } . }";
			results.add(new UpdateExtractedData(deleteConstruct, "",update.getGraph().getURI(),""));
		
		}

		@Override
		public void visit(UpdateCopy update) {
			logger.debug("Visit on UpdateCopy");
			String deleteConstruct = "CONSTRUCT { ?"+eps.s()+" ?"+eps.p()+" ?"+eps.o()+" } WHERE { GRAPH <" + update.getDest().getGraph().getURI()
					+ "> { ?"+eps.s()+" ?"+eps.p()+" ?"+eps.o()+" } . }";
			String insertConstruct = "CONSTRUCT { ?"+eps.s()+" ?"+eps.p()+" ?"+eps.o()+" } WHERE { GRAPH <" + update.getSrc().getGraph().getURI()
					+ "> { ?"+eps.s()+" ?"+eps.p()+" ?"+eps.o()+" } . }";
			results.add(new UpdateExtractedData(deleteConstruct, insertConstruct,update.getDest().getGraph().getURI(),update.getSrc().getGraph().getURI()));

		}

		@Override
		public void visit(UpdateAdd update) {
			logger.debug("Visit on UpdateAdd");
			String insertConstruct = "CONSTRUCT { ?"+eps.s()+" ?"+eps.p()+" ?"+eps.o()+" } WHERE { GRAPH <" + update.getDest().getGraph().getURI()
					+ "> { ?"+eps.s()+" ?"+eps.p()+" ?"+eps.o()+" } . }";
			results.add(new UpdateExtractedData("", insertConstruct,"",update.getDest().getGraph().getURI()));

		}

		
		public ArrayList<UpdateExtractedData> getResult() {
			return results;
		}



	}

	// attributes
	private String sparqlText;
	private final static Logger logger = LogManager.getLogger("SPARQLAnalyzer");

	// Constructor
	public SPARQLAnalyzer(String request) {
		// store the query text
		sparqlText = request;
	}

	public ArrayList<UpdateExtractedData> getConstructs() {
		//System.out.println("sparqlText:\n"+sparqlText);
		UpdateRequest updates = UpdateFactory.create(sparqlText);
		for (Update up : updates) {			
			ToConstructUpdateVisitor updateVisitor = new ToConstructUpdateVisitor();
			up.visit(updateVisitor);
			return updateVisitor.getResult();
		}
		throw new IllegalArgumentException("No valid operation found");
	}



}
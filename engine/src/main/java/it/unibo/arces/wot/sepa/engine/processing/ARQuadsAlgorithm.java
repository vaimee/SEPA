package it.unibo.arces.wot.sepa.engine.processing;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.engine.processing.epspec.EpSpecFactory;
import it.unibo.arces.wot.sepa.engine.processing.epspec.IEndPointSpecification;
import it.unibo.arces.wot.sepa.engine.processing.updateprocessing.IAsk;
import it.unibo.arces.wot.sepa.engine.processing.updateprocessing.SPARQLAnalyzer;
import it.unibo.arces.wot.sepa.engine.processing.updateprocessing.TripleConverter;
import it.unibo.arces.wot.sepa.engine.processing.updateprocessing.UpdateExtractedData;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequestWithQuads;

public class ARQuadsAlgorithm {
	protected final Logger logger = LogManager.getLogger();
	protected QueryProcessor queryProcessor;
	protected InternalUpdateRequest originalUpdate;
	
	ARQuadsAlgorithm(InternalUpdateRequest update, QueryProcessor queryProcessor){
		this.queryProcessor=queryProcessor;
		this.originalUpdate=update;
	}
	
	public QueryResponse processQuery(InternalQueryRequest query) throws SEPASecurityException, IOException {
		return (QueryResponse)queryProcessor.process(query);			
	}
	
	public InternalUpdateRequestWithQuads extractARQuads() throws SEPAProcessingException, SPARQL11ProtocolException, SEPASparqlParsingException {
		ARBindingsResults quads = new ARBindingsResults(new BindingsResults(new ArrayList<>(),new ArrayList<>()), new BindingsResults(new ArrayList<>(),new ArrayList<>()));
	
		logger.log(Level.getLevel("ARQuadsAlgorithm"), "AddedRemovedAlgorithm --- START");
	
		String insertDeleteUpdate = null;
		long start = System.nanoTime();			
		ArrayList<UpdateExtractedData> constructsList;
		
		try {
			constructsList = getAddedRemovedFrom(originalUpdate);
			insertDeleteUpdate = genereteInsertDeleteUpdate(constructsList);
			quads =generateARBindingsResults(constructsList);
		} catch (SEPASecurityException e) {
			throw new SEPAProcessingException("SEPASecurityException: " + e.getMessage());
		} catch (IOException e) {
			throw new SEPAProcessingException("IOException: " + e.getMessage());
		} catch (SEPABindingsException e) {
			throw new SEPASparqlParsingException("SEPABindingsException: " + e.getMessage());
		} 
			
		if(insertDeleteUpdate!=null) {
			long stop = System.nanoTime();
			logger.trace("ARQuadsAlgorithm executed in" + (stop - start) + " ns");
		}else {
			logger.error("ARQuadsAlgorithm", "ARQuadsAlgorithm run into a error.");
		}

		logger.log(Level.getLevel("ARQuadsAlgorithm"), "AddedRemovedAlgorithm --- END");
		
		//--------------> using --> insertDeleteUpdate
		return new InternalUpdateRequestWithQuads(insertDeleteUpdate,originalUpdate.getSparql(), originalUpdate.getDefaultGraphUri(), originalUpdate.getNamedGraphUri(), originalUpdate.getClientAuthorization(), quads);
		
		//--------------> using --> originalUpdate
		//return new InternalUpdateRequestWithQuads(originalUpdate.getSparql(),originalUpdate.getSparql(), originalUpdate.getDefaultGraphUri(), originalUpdate.getNamedGraphUri(), originalUpdate.getClientAuthorization(), quads);
		
	}

	
	private  String genereteInsertDeleteUpdate(ArrayList<UpdateExtractedData> constructsList) throws SEPAProcessingException, SEPABindingsException {
		if(constructsList.size()<=0) {
			return null;
		}
		
		String delete = "DELETE DATA {";
		boolean needDelete = false;
		String insert = "INSERT DATA  {";				
		boolean needInsert = false;
		
		for (UpdateExtractedData contruct : constructsList) {
			
			if(contruct.needDelete()) {						
				if(contruct.getRemovedGraph()==null) {
					throw new SEPAProcessingException("Miss graph for generate Delete update.");
				}
				needDelete=true;						
				delete+="\nGRAPH<"+ contruct.getRemovedGraph()+ "> \n{\n";
				for (Bindings triple : contruct.getRemoved().getBindings()) {					
					
						//System.out.println("triple-->"+tripleToString(triple)); 
						String temp = TripleConverter.tripleToString(triple) +" .";
						if(temp!=null) {
							delete+=temp+"\n";
						}
				}
				delete+="}";
			}//else ignore
			
			if(contruct.needInsert()) {						
				if(contruct.getAddedGraph()==null) {
					throw new SEPAProcessingException("Miss graph for generate Insert update.");
				}
				needInsert=true;						
				insert+="\nGRAPH<"+ contruct.getAddedGraph()+ "> {\n";
				for (Bindings triple : contruct.getAdded().getBindings()) {					
					
						//System.out.println("triple-->"+tripleToString(triple)); //ok
						String temp = TripleConverter.tripleToString(triple) +" .";
						if(temp!=null) {
							insert+=temp+"\n";
						}
				}
				insert+="}";
			}//else ignore
			
		}
		
		
		delete+="}";
		insert+="}";
		
		String insertDeleteUpdate = "";
		if(needDelete) {
			insertDeleteUpdate=delete;
			if(needInsert) {
				insertDeleteUpdate+=";"+insert;
			}
		}else if(needInsert) {
			insertDeleteUpdate=insert;
		}else {
			return null;
		}
		return insertDeleteUpdate;
	
		
	}
	
	private ArrayList<UpdateExtractedData>  getAddedRemovedFrom(InternalUpdateRequest req) throws SEPASecurityException, IOException, SEPABindingsException, SEPASparqlParsingException  {
		//--------------------------------------------------------------------------CONSTRUCT SECTION
		long startConstruct = System.nanoTime();		
		SPARQLAnalyzer sa = new SPARQLAnalyzer(req.getSparql());
		ArrayList<UpdateExtractedData> constructsList = sa.getConstructs();
		int countConstructExecution =0;
		int countConstructSkipped =0;
		//for each graph
		for (UpdateExtractedData constructs : constructsList) {
			if(!constructs.isSkipConstruct()) {
				
				String dc = constructs.getDeleteConstruct();
				if (dc.length() > 0) {	
					logger.debug("Construct n°"+countConstructExecution+" (removed type)",dc);
					InternalQueryRequest construct=new InternalQueryRequest(
							dc,
							req.getDefaultGraphUri(),
							req.getNamedGraphUri(),
							req.getClientAuthorization()
							);
					
					QueryResponse response = (QueryResponse)queryProcessor.process(construct);					
					constructs.setRemoved(response.getBindingsResults());
					countConstructExecution++;
				}else {
					constructs.setRemoved( new BindingsResults(new JsonObject()));
				}

				String ac = constructs.getInsertConstruct();
				if (ac.length() > 0) {
					logger.debug("Construct n°"+countConstructExecution+" (added type)",ac);					
					InternalQueryRequest construct=new InternalQueryRequest(
							ac,
							req.getDefaultGraphUri(),
							req.getNamedGraphUri(),
							req.getClientAuthorization()
							);
					
					QueryResponse response = (QueryResponse)queryProcessor.process(construct);					
					constructs.setAdded(response.getBindingsResults());
					countConstructExecution++;
										
				}else {
					constructs.setAdded(new BindingsResults(new JsonObject()));
				}
				
			}else {
				countConstructSkipped++;
			}
		}			
		long endConstruct = System.nanoTime();
		logger.trace("Executed "+countConstructExecution+" constructs in " + (endConstruct - startConstruct) + " ns; skipped: "+ countConstructSkipped);
		
	
		
		
		//--------------------------------------------------------------------------ASK SECTION
		
	
		long startAsk = System.nanoTime();	
		//Ottengo la ask migliore a seconda dell'end point
		//ad esempio sappiamo che la "AsksAsSelectExistsList" è la più performante ma 
		//non sempre funziona su Virtuoso
		IAsk asks= EpSpecFactory.getInstance().getAsk(constructsList, req, this);
		constructsList=asks.filter();
		long endAsk = System.nanoTime();
		logger.trace("Ask (selectAsAsk) execution time: " + (endAsk - startAsk) + " ns");
	
		return constructsList;
	}
	
	private ARBindingsResults generateARBindingsResults(ArrayList<UpdateExtractedData> constructsList) {
			
		IEndPointSpecification eps =  EpSpecFactory.getInstance();
		
		ArrayList<String> vars = eps.vars();
		vars.add(eps.g());
		BindingsResults added=new BindingsResults(vars,  new ArrayList<Bindings>());;
		BindingsResults removed=new BindingsResults(vars,  new ArrayList<Bindings>());;		
		long start = System.nanoTime();
		for (UpdateExtractedData ued : constructsList) {
			if(ued.needInsert()) {
				for (Bindings binds : ued.getAdded().getBindings()) {
					binds.addBinding(eps.g(), new RDFTermURI(ued.getAddedGraph()));
					added.add(binds);
				}
			}
			if(ued.needDelete()) {
				for (Bindings binds : ued.getRemoved().getBindings()) {
					binds.addBinding(eps.g(), new RDFTermURI(ued.getRemovedGraph()));
					removed.add(binds);
				}
			}
			
		}
		long stop = System.nanoTime();
		logger.debug("Added triples: "+ added.size()+ "; removed triples: "+ removed.size());
		logger.trace("Overhead for generateARBindingsResults" + (stop - start) + " ns");
		return new ARBindingsResults(added,removed);
	}
	
}

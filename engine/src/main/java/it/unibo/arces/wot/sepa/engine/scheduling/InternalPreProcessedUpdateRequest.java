package it.unibo.arces.wot.sepa.engine.scheduling;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;

import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.engine.processing.QueryProcessor;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.AsksAsSelectExistsList;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.AsksAsSelectGraphAsVar;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.IAsk;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.SPARQLAnalyzer;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.TripleConverter;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.UpdateExtractedData;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec.EpSpecFactory;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec.IEndPointSpecification;

public class InternalPreProcessedUpdateRequest extends InternalUpdateRequest{
	protected ErrorResponse retErrorResponse = null;	
	protected String originalUpdate;
	protected ARBindingsResults arBindingsResults;
	protected QueryProcessor processor;
	
	public InternalPreProcessedUpdateRequest(ErrorResponse errorResponse) {
		super(null, null, null, null);
		retErrorResponse = errorResponse;
	}
	
	public InternalPreProcessedUpdateRequest(InternalUpdateRequest req) {		
		super(req.getSparql(), req.getDefaultGraphUri(), req.getNamedGraphUri(), req.getClientAuthorization());
	
		boolean doIt =false;
		if(doIt) {
			logger.log(Level.getLevel("updateProcessing"), "AddedRemovedAlgorithm --- START");
			
			
			//questo è da sistemare, il processor va ottenuto in un modo più pulito?
			try {
				this.processor =new QueryProcessor( new SPARQL11Properties("endpoint.jpar"));
			} catch (SEPAProtocolException e) {
				retErrorResponse = errorResponseFromException(e,true);
			} catch (SEPASecurityException e) {
				retErrorResponse = errorResponseFromException(e,true);
			} catch (SEPAPropertiesException e) {
				retErrorResponse = errorResponseFromException(e,false);
			}
			
			
			if(retErrorResponse==null) {
				this.originalUpdate = req.getSparql(); 		
				long start = System.nanoTime();			
				ArrayList<UpdateExtractedData> constructsList;
				
				try {
					constructsList = getAddedRemovedFrom(req);
					this.sparql = genereteInsertDeleteUpdate(constructsList);	
					generateARBindingsResults(constructsList);
				} catch (SEPASecurityException e) {
					retErrorResponse = errorResponseFromException(e,true);
				} catch (IOException e) {
					retErrorResponse = errorResponseFromException(e,false);
				} catch (SEPABindingsException e) {
					retErrorResponse = errorResponseFromException(e,false);
				} catch (NullPointerException e) {
					retErrorResponse = errorResponseFromException(e,false);
				} catch (Exception e) {
					retErrorResponse = errorResponseFromException(e,false);
				}
				
				if(retErrorResponse==null) {
					long stop = System.nanoTime();
					logger.trace("AddedRemovedAlgorithm executed in" + (stop - start) + " ns");
				}else {
					logger.error("AddedRemovedAlgorithm", "Error",retErrorResponse);
				}
		
			}else {
				logger.error("AddedRemovedAlgorithm", "Error",retErrorResponse);
			}
			logger.log(Level.getLevel("updateProcessing"), "AddedRemovedAlgorithm --- END");
			
		}
		
	}	
	
	private ErrorResponse errorResponseFromException(Exception e, boolean badReq) {
		String msg = "No information about the exception";
		if(e.getMessage()!=null) {
			msg=e.getMessage();
		}else {
			 e.printStackTrace();
		}
		if(badReq) {
			return  new ErrorResponse(HttpStatus.SC_BAD_REQUEST,e.getClass().getName(),msg);			
		}else {
			return  new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR,e.getClass().getName(),msg);		
		}
	}
	
	private String genereteInsertDeleteUpdate(ArrayList<UpdateExtractedData> constructsList) throws Exception {
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
					throw new Exception("Miss graph for generate Delete update.");
				}
				needDelete=true;						
				delete+="\nGRAPH<"+ contruct.getRemovedGraph()+ "> \n{\n";
				for (Bindings triple : contruct.getRemoved().getBindings()) {					
					
					try {
						//System.out.println("triple-->"+tripleToString(triple)); 
						String temp = TripleConverter.tripleToString(triple) +" .";
						if(temp!=null) {
							delete+=temp+"\n";
						}
					} catch (SEPABindingsException e) {
						e.printStackTrace();
					}
				}
				delete+="}";
			}//else ignore
			
			if(contruct.needInsert()) {						
				if(contruct.getAddedGraph()==null) {
					throw new Exception("Miss graph for generate Insert update.");
				}
				needInsert=true;						
				insert+="\nGRAPH<"+ contruct.getAddedGraph()+ "> {\n";
				for (Bindings triple : contruct.getAdded().getBindings()) {					
					
					try {
						//System.out.println("triple-->"+tripleToString(triple)); //ok
						String temp = TripleConverter.tripleToString(triple) +" .";
						if(temp!=null) {
							insert+=temp+"\n";
						}
					} catch (SEPABindingsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
	
	private ArrayList<UpdateExtractedData>  getAddedRemovedFrom(InternalUpdateRequest req) throws SEPASecurityException, IOException, SEPABindingsException  {
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
					
					QueryResponse response = (QueryResponse)this.processor.process(construct);					
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
					
					QueryResponse response = (QueryResponse)this.processor.process(construct);					
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
		//AsksAsSelectExistsList not pass all test on Virtuoso 
		//IAsk asks= new AsksAsSelectExistsList(constructsList, req, this.processor);
		IAsk asks= new AsksAsSelectGraphAsVar(constructsList, req, this.processor);
		constructsList=asks.filter();
		long endAsk = System.nanoTime();
		logger.trace("Ask (selectAsAsk) execution time: " + (endAsk - startAsk) + " ns");
	
		return constructsList;
	}
	
	private void generateARBindingsResults(ArrayList<UpdateExtractedData> constructsList) {
			
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
		arBindingsResults = new ARBindingsResults(added,removed);
	}
	
	public ARBindingsResults getARBindingsResults() {
		return arBindingsResults;
	}
	
	public boolean preProcessingFailed() {
		return retErrorResponse != null;
	}
	
	public ErrorResponse getErrorResponse() {
		return retErrorResponse;
	}

	public String getOriginalUpdate() {
		return originalUpdate;
	}


}

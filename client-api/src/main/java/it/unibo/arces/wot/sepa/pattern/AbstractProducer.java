package it.unibo.arces.wot.sepa.pattern;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.ISPARQL11Interface;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;

public abstract class AbstractProducer extends Client implements IProducer {
	private static final Logger logger = LogManager.getLogger();
	
	protected String sparqlUpdate = null;
	protected String SPARQL_ID = "";
	private Bindings forcedBindings;
	
	private ISPARQL11Interface client;
	
	public AbstractProducer(JSAP appProfile,String updateID,SEPASecurityManager sm, ISPARQL11Interface client) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException  {
		super(appProfile,sm);
		
		if (appProfile.getSPARQLUpdate(updateID) == null) {
			logger.fatal("UPDATE ID [" +updateID+"] not found in "+appProfile.getFileName());
			throw new IllegalArgumentException("UPDATE ID [" +updateID+"] not found in "+appProfile.getFileName());
		}
		
		SPARQL_ID = updateID;
		
		sparqlUpdate = appProfile.getSPARQLUpdate(updateID);
		
		forcedBindings = appProfile.getUpdateBindings(updateID);
		
		this.client = client;
	}
	
	public Response update() throws SEPASecurityException, SEPAPropertiesException, SEPABindingsException, SEPAProtocolException {
		return update(0);
	}
	
	@Override
	public Response update(int timeout) throws SEPASecurityException, SEPAPropertiesException, SEPABindingsException, SEPAProtocolException{	 
		String authorizationHeader = null;
		
		if (isSecure()) authorizationHeader = sm.getAuthorizationHeader();
		
		UpdateRequest req = new UpdateRequest(appProfile.getUpdateMethod(SPARQL_ID), appProfile.getUpdateProtocolScheme(SPARQL_ID),appProfile.getUpdateHost(SPARQL_ID), appProfile.getUpdatePort(SPARQL_ID),
					appProfile.getUpdatePath(SPARQL_ID), appProfile.addPrefixesAndReplaceBindings(sparqlUpdate, addDefaultDatatype(forcedBindings,SPARQL_ID,false)),
					appProfile.getUsingGraphURI(SPARQL_ID), appProfile.getUsingNamedGraphURI(SPARQL_ID),authorizationHeader,timeout);
		 
		 Response retResponse = client.update(req);
		 
		 if (retResponse.isError()) {
			 if (isSecure()) {
				 ErrorResponse errorResponse = (ErrorResponse) retResponse;
				 if (errorResponse.isTokenExpiredError()) {
					 try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						throw new SEPAProtocolException(e);
					}
					 
				 }
				 
				 authorizationHeader = sm.getAuthorizationHeader();
					
				 req = new UpdateRequest(appProfile.getUpdateMethod(SPARQL_ID), appProfile.getUpdateProtocolScheme(SPARQL_ID),appProfile.getUpdateHost(SPARQL_ID), appProfile.getUpdatePort(SPARQL_ID),
								appProfile.getUpdatePath(SPARQL_ID), appProfile.addPrefixesAndReplaceBindings(sparqlUpdate, addDefaultDatatype(forcedBindings,SPARQL_ID,false)),
								appProfile.getUsingGraphURI(SPARQL_ID), appProfile.getUsingNamedGraphURI(SPARQL_ID),authorizationHeader,timeout);
					 
				 retResponse = client.update(req);
			 }
		 }
		 
		 return retResponse;
	 }

	@Override
	public void close() throws IOException {
		client.close();
	}

	public final void setUpdateBindingValue(String variable, RDFTerm value) throws SEPABindingsException {
		forcedBindings.setBindingValue(variable, value);
	}

}

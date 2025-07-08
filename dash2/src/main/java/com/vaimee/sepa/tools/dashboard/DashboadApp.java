package com.vaimee.sepa.tools.dashboard;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.sparql.Bindings;
import com.vaimee.sepa.api.commons.sparql.RDFTermLiteral;
import com.vaimee.sepa.api.pattern.GenericClient;
import com.vaimee.sepa.api.pattern.JSAP;

import java.io.IOException;

public class DashboadApp {
	private GenericClient sepaClient;
	
	private final String defaultGraph = "https://sepa.vaimee.com/default/graph";
	private final int timeout = 60000;
	private final int nretry = 1;

	public DashboadApp(JSAP appProfile, DashboardHandler handler) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		sepaClient = new GenericClient(appProfile, handler);
	}
	
	public Response query(String queryID, String sparql, Bindings forced, int timeout, int nretry) {
		try {
			return sepaClient.query(queryID, sparql, forced, timeout, nretry);
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException
				 | SEPABindingsException e) {
			return new ErrorResponse(500, e.getMessage(), e.getMessage());
		}
	}
	
	public Response update(String queryID, String sparql,Bindings forced,int timeout,int nretry) {
		try {
			return sepaClient.update(queryID, sparql, forced, timeout, nretry);
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException
				| SEPABindingsException e) {
			return new ErrorResponse(500, e.getMessage(), e.getMessage());
		}
	}
	
	private Response query(String queryID, Bindings bindings) {
		try {
			if (bindings.getValue("graph")!= null) 
				if (bindings.getValue("graph").equals(defaultGraph)) return sepaClient.query(queryID+"_DEFAULT", null, bindings, timeout, nretry);
			return sepaClient.query(queryID, null, bindings, timeout, nretry);
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException
				| SEPABindingsException e) {
			return new ErrorResponse(500, e.getMessage(), e.getMessage());
		}
	}

	private Response update(String updateID, Bindings bindings) {
		try {
			if (bindings.getValue("graph").equals(defaultGraph)) return sepaClient.update(updateID+"_DEFAULT", null, bindings, timeout, nretry);
			else return sepaClient.update(updateID, null, bindings, timeout, nretry);
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException
				| SEPABindingsException e) {
			return new ErrorResponse(500, e.getMessage(), e.getMessage());
		}
	}
	
	//UPDATES

	public Response dropGraph(Bindings forced) {
		return update("___DASHBOARD_DROP_GRAPH", forced);
	}

	public Response updateLiteral(Bindings forcedBindings) {
		return update("___DASHBOARD_UPDATE_LITERAL", forcedBindings);
	}

	public Response updateLiteralBnode(Bindings forcedBindings) {
		return update("___DASHBOARD_UPDATE_LITERAL_BNODE", forcedBindings);
	}

	public Response updateUri(Bindings forcedBindings) {
		return update("___DASHBOARD_UPDATE_URI", forcedBindings);
	}

	public void subscribe(String queryID, String sparql, Bindings bindings, long timeout, long nretry)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException,
			InterruptedException {
		sepaClient.subscribe(queryID, sparql, bindings, timeout, nretry);
	}

	public void unsubscribe(String spuid, long timeout, long nretry) throws SEPAProtocolException,
			SEPASecurityException, SEPAPropertiesException, SEPABindingsException, InterruptedException {
		sepaClient.unsubscribe(spuid, timeout, nretry);
	}

	public Response graphs(int max) {
		try {
			Bindings fb = new Bindings();
			fb.addBinding("max", new RDFTermLiteral(String.valueOf(max),"xsd:integer"));
			return sepaClient.query("___DASHBOARD_GRAPHS", null,fb, timeout, nretry);
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException | IOException e) {
			return new ErrorResponse(500, e.getMessage(), e.getMessage());
		}
	}
	
	//QUERIES

	public Response topClasses(Bindings forced) {
		return query("___DASHBOARD_TOP_CLASSES", forced);
	}

	public Response uriGraph(Bindings forced) {
		return query("___DASHBOARD_URI_GRAPH", forced);
	}

	public Response bnodeGraph(Bindings forced) {
		return query("___DASHBOARD_BNODE_GRAPH", forced);
	}

	public Response individuals(Bindings forced) {	
		return query("___DASHBOARD_INDIVIDUALS", forced);
	}

	public Response subClasses(Bindings forced) {
		return query("___DASHBOARD_SUB_CLASSES", forced);
	}

	public String getHost() {
		return sepaClient.getApplicationProfile().getHost();
	}

}

package it.unibo.arces.wot.sepa.engine.protocol.ngsild.handlers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.engine.bean.NgisLdHandlerBeans;
import it.unibo.arces.wot.sepa.engine.protocol.ngsild.NgsiLdError;
import it.unibo.arces.wot.sepa.engine.protocol.ngsild.NgsiLdRdfMapper;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public abstract class ResourceHandler {
	protected static final Logger logger = LogManager.getLogger();
	
	protected Matcher matcher = null;
	protected final Pattern pattern;
	protected final NgisLdHandlerBeans jmx;
	
	private int statusCode = HttpStatus.SC_OK;
	private JsonObject responseBody = null;
	private String responseContentType = "application/json";
	private Header[] headers;
	
	protected final NgsiLdRdfMapper ngsiLdRdfMapper;
	
	private final NgsiLdError operationNotSupported = NgsiLdError.OperationNotSupported;
	private final NgsiLdError invalidRequest = NgsiLdError.InvalidRequest;
	
	public ResourceHandler(Scheduler scheduler,String regex,NgisLdHandlerBeans jmx) {
		this.jmx = jmx;
		pattern = Pattern.compile(regex);
		ngsiLdRdfMapper = new NgsiLdRdfMapper(scheduler,jmx);
	}
	
	public final boolean matches(String resourceUri) {
		logger.debug("Pattern: "+pattern+" ResourceURI: "+resourceUri);
		matcher = pattern.matcher(resourceUri);
		return matcher.matches();
	}
	
	protected final void setResponse(int code,String contentType,JsonObject body,Header[] headers) {
		statusCode = code;
		responseContentType = contentType;
		responseBody = body;
		this.headers = headers;
	}
	
	public final void processRequest(String method,JsonObject body,String link) {
		switch(method) {
		case "GET":
			get(link);
			break;
		case "POST":
			post(body,link);
			break;
		case "PATCH":
			patch(body,link);
			break;
		case "DELETE":
			delete(link);
			break;
		}
	}
	public final String getResponseContentType() {return responseContentType;}
	public final JsonObject getResponseBody() {return responseBody;}
	public final int getStatusCode() {return statusCode;}
	public final Header[] getResponseHeaders() {return headers;}
	
	protected boolean validate(JsonObject obj) {
		return true;
	}
	
	protected void get(String link) {
		operationNotSupported.setTitle("HTTP method not supported");
		operationNotSupported.setDetail("GET "+link);
		setResponse(operationNotSupported.getErrorCode(),"application/json",operationNotSupported.getJsonResponse(),null);
	}
	
	protected void post(JsonObject body,String link) {
		if (!validate(body)) {
			invalidRequest.setTitle("Request body not valid");
			invalidRequest.setDetail("POST "+body);
			setResponse(invalidRequest.getErrorCode(),"application/json",invalidRequest.getJsonResponse(),null);
		}
		else {
			operationNotSupported.setTitle("HTTP method not supported");
			operationNotSupported.setDetail("POST "+body+ " "+ link);
			setResponse(operationNotSupported.getErrorCode(),"application/json",operationNotSupported.getJsonResponse(),null);
		}
	}
	
	protected void patch(JsonObject body,String link) {
		if (!validate(body)) {
			invalidRequest.setTitle("Request body not valid");
			invalidRequest.setDetail("PATCH "+body);
			setResponse(invalidRequest.getErrorCode(),"application/json",invalidRequest.getJsonResponse(),null);
		}
		else {
			operationNotSupported.setTitle("HTTP method not supported");
			operationNotSupported.setDetail("PATCH "+body+ " "+ link);
			setResponse(operationNotSupported.getErrorCode(),"application/json",operationNotSupported.getJsonResponse(),null);
		}
	}
	
	protected void delete(String link) {
		operationNotSupported.setTitle("HTTP method not supported");
		operationNotSupported.setDetail("DELETE " + link);
		setResponse(operationNotSupported.getErrorCode(),"application/json",operationNotSupported.getJsonResponse(),null);
	}
}

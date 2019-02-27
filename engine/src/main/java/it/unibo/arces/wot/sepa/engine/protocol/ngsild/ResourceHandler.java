package it.unibo.arces.wot.sepa.engine.protocol.ngsild;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.engine.bean.NgisLdHandlerBeans;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public abstract class ResourceHandler implements ResponseHandler {
	protected static final Logger logger = LogManager.getLogger();
	
	protected final Scheduler scheduler;
	protected Matcher matcher = null;
	protected final Pattern pattern;
	protected final NgisLdHandlerBeans jmx;
	
	private int statusCode = HttpStatus.SC_OK;
	private JsonObject responseBody = null;
	private String responseContentType = "application/json";
	private Header[] headers;
	
	protected final NgsiLdRdfMapper ngsiLdRdfMapper;
	
	public ResourceHandler(Scheduler scheduler,String regex,NgisLdHandlerBeans jmx) {
		this.scheduler = scheduler;
		this.jmx = jmx;
		pattern = Pattern.compile(regex);
		ngsiLdRdfMapper = new NgsiLdRdfMapper(scheduler,jmx);
	}
	
	public final boolean matches(String resourceUri) {		
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
	
	protected void get(String link) {
		setResponse(NgsiLdError.OperationNotSupported.getErrorCode(),"application/json",NgsiLdError.buildResponse(NgsiLdError.OperationNotSupported, "HTTP method not supported", "GET "+link),null);
	}
	
	protected void post(JsonObject body,String link) {
		setResponse(NgsiLdError.OperationNotSupported.getErrorCode(),"application/json",NgsiLdError.buildResponse(NgsiLdError.OperationNotSupported, "HTTP method not supported", "POST "+body+ " "+ link),null);
	}
	
	protected void patch(JsonObject body,String link) {
		setResponse(NgsiLdError.OperationNotSupported.getErrorCode(),"application/json",NgsiLdError.buildResponse(NgsiLdError.OperationNotSupported, "HTTP method not supported", "PATCH "+body+" "+link),null);
	}
	
	protected void delete(String link) {
		setResponse(NgsiLdError.OperationNotSupported.getErrorCode(),"application/json",NgsiLdError.buildResponse(NgsiLdError.OperationNotSupported, "HTTP method not supported", "DELETE "+link),null);
	}

	
}

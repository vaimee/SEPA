package it.unibo.arces.wot.sepa.engine.protocol.ngsild;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Table 6.3.2-1: Mapping of error types to HTTP status codes
 * 
 * Error Type - HTTP status
 * 
 * http://uri.etsi.org/ngsi-ld/errors/InvalidRequest - 400
 * http://uri.etsi.org/ngsi-ld/errors/BadRequestData - 400
 * http://uri.etsi.org/ngsi-ld/errors/AlreadyExists - 409
 * http://uri.etsi.org/ngsi-ld/errors/OperationNotSupported - 422
 * http://uri.etsi.org/ngsi-ld/errors/ResourceNotFound - 404
 * http://uri.etsi.org/ngsi-ld/errors/InternalError - 500
 * http://uri.etsi.org/ngsi-ld/errors/TooComplexQuery - 403
 * http://uri.etsi.org/ngsi-ld/errors/TooManyResults - 403
 * 
 * JSON message: 
 * • type: Error type as per clause 5.5.2. 
 * • title: Error title which shall be a short string summarizing the error. 
 * • detail: A detailed message that should convey enough information about the error.
 * 
 * IETF RFC 7807: "Problem Details for HTTP APIs". Available at https://tools.ietf.org/html/rfc7807.
 * 
 **/
public enum NgsiLdError {
	InvalidRequest(400,"http://uri.etsi.org/ngsi-ld/errors/InvalidRequest"),
	BadRequestData(400,"http://uri.etsi.org/ngsi-ld/errors/BadRequestData"),
	AlreadyExists(409,"http://uri.etsi.org/ngsi-ld/errors/InvalidRequest"),
	OperationNotSupported(422,"http://uri.etsi.org/ngsi-ld/errors/OperationNotSupported"),
	ResourceNotFound(404,"http://uri.etsi.org/ngsi-ld/errors/ResourceNotFound"),
	InternalError(500,"http://uri.etsi.org/ngsi-ld/errors/InternalError"),
	TooComplexQuery(403,"http://uri.etsi.org/ngsi-ld/errors/TooComplexQuery"),
	TooManyResults(403,"http://uri.etsi.org/ngsi-ld/errors/TooManyResults"),
	MethodNotAllowed(405,"http://uri.etsi.org/ngsi-ld/errors/MethodNotAllowed"),
	RequestEntityTooLarge(413,"http://uri.etsi.org/ngsi-ld/errors/RequestEntityTooLarge"),
	LengthRequired(411,"http://uri.etsi.org/ngsi-ld/errors/LengthRequired"),
	UnsupportedMediaType(415,"http://uri.etsi.org/ngsi-ld/errors/UnsupportedMediaType"),
	TooManyPendingRequests(429,"http://wot.arces.unibo.it/TooManyPendingRequests");
	
	private final int code;
	private final String uri;
	private String title ="";
	private String detail ="";
	
	NgsiLdError(int code,String uri) {
		this.code = code;
		this.uri =uri;
	}
	
	public String getType() {
		return uri;
	}
	
	public int getErrorCode() {
		return code;
	}
	
	@Override
	public String toString() {
		return getJsonResponse().toString();
	}

	public JsonObject getJsonResponse() {
		JsonObject error = new JsonObject();
		
		error.add("type", new JsonPrimitive(uri));
		error.add("title", new JsonPrimitive(title));
		error.add("detail", new JsonPrimitive(detail));
		
		return error;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}
}

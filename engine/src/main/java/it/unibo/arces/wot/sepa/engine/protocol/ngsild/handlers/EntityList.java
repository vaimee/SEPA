package it.unibo.arces.wot.sepa.engine.protocol.ngsild.handlers;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;

import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.engine.bean.NgisLdHandlerBeans;
import it.unibo.arces.wot.sepa.engine.protocol.ngsild.NgsiLdError;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

/**
 * Table 6.2-1: Resources and HTTP methods defined on them
 * 
 * Resource Name: Entity List Resource URI: /entities/ HTTP Method: 1) POST ==>
 * Entity creation /entities/ 2) GET ==> Query entities /entities?query
 * 
 */
public class EntityList extends ResourceHandler {

	public EntityList(Scheduler s, NgisLdHandlerBeans jmx) {
		super(s, "\\/entities$|\\/entities\\/$|\\/entities(\\?)(?<query>.+)", jmx);
	}

	@Override
	protected boolean validate(JsonObject jsonld) {
		return true;
	}
	
	@Override
	protected void post(JsonObject jsonld, String link) {
		super.post(jsonld, link);

		// Query string must no be present
		if (matcher.group("query") != null) {
			NgsiLdError error = NgsiLdError.InvalidRequest;
			error.setTitle("Failed to create entity");
			error.setDetail(jsonld.toString());
			setResponse(error.getErrorCode(), "application/json",error.getJsonResponse(),null);
			return;
		}

		// Entity exists
		if (ngsiLdRdfMapper.entityExists(jsonld)) {
			NgsiLdError error = NgsiLdError.AlreadyExists;
			error.setTitle("Entity exists");
			error.setDetail(jsonld.toString());
			setResponse(error.getErrorCode(), "application/json",error.getJsonResponse(),null);
			return;
		}

		// Create entity
		if (ngsiLdRdfMapper.createEntity(jsonld)) {
			Header location = new BasicHeader("Location", "/ngsi-ld/v1/entities/" + jsonld.get("@id").getAsString());
			Header[] headers = new Header[1];
			headers[0] = location;
			setResponse(HttpStatus.SC_CREATED, null, null, headers);
		} else {
			NgsiLdError error = ngsiLdRdfMapper.getLastError();
			setResponse(error.getErrorCode(), "application/json",error.getJsonResponse(),null);
		}
	}

	@Override
	protected void get(String link) {
		super.get(link);

		if (matcher.group("query") == null || link == null) {
			NgsiLdError error = NgsiLdError.InvalidRequest;
			error.setTitle("Invalid request");
			error.setDetail("Query string is missing or link header is null");
			setResponse(error.getErrorCode(), "application/json",error.getJsonResponse(),null);
			return;
		}

		// TODO: to be implemented
		NgsiLdError error = NgsiLdError.OperationNotSupported;
		error.setTitle("API not implemented");
		error.setDetail("Query entities");
		setResponse(HttpStatus.SC_NOT_IMPLEMENTED, "application/json", error.getJsonResponse(), null);

	}
}

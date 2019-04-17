package it.unibo.arces.wot.sepa.engine.protocol.ngsild.handlers;

import org.apache.http.HttpStatus;

import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.engine.bean.NgisLdHandlerBeans;
import it.unibo.arces.wot.sepa.engine.protocol.ngsild.NgsiLdError;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

/**
 * Table 6.2-1: Resources and HTTP methods defined on them
 * 
 * Resource Name: Entity by id Resource URI: /entities/{entityId} HTTP Method:
 * 1) GET 2) DELETE
 */
public class EntityById extends ResourceHandler {

	public EntityById(Scheduler s, NgisLdHandlerBeans jmx) {
		super(s, "\\/entities\\/(?<entityId>[^\\?]+)|\\/entities\\/(?<id>.+)\\?(?<query>.+)", jmx);
	}

	@Override
	protected void delete(String link) {
		super.delete(link);

		// TODO: to be implemented
		NgsiLdError error = NgsiLdError.OperationNotSupported;
		error.setTitle("API not implemented");
		error.setDetail("Entity deletion by id");
		setResponse(HttpStatus.SC_NOT_IMPLEMENTED, "application/json", error.getJsonResponse(), null);
	}

	@Override
	protected void get(String link) {
		super.get(link);

		// Get entity ID
		String entityId = matcher.group("entityId");

		// TODO: implement query attributes (id + attrs)
		if (entityId == null) {
			NgsiLdError error = NgsiLdError.OperationNotSupported;
			error.setTitle("API not implemented");
			error.setDetail("Filtering entities by attributes or relationships");
			setResponse(HttpStatus.SC_NOT_IMPLEMENTED, "application/json", error.getJsonResponse(), null);
			return;
		}

		// Get entity graph
		JsonObject jsonld = ngsiLdRdfMapper.getEntityById(entityId);
		if (jsonld == null) {
			NgsiLdError error = ngsiLdRdfMapper.getLastError();
			setResponse(error.getErrorCode(), "application/json", error.getJsonResponse(), null);
			return;
		}

		setResponse(200, "application/ld+json", jsonld, null);
	}
}

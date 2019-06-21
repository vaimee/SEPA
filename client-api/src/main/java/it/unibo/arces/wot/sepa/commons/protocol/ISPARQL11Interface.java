package it.unibo.arces.wot.sepa.commons.protocol;

import java.io.Closeable;
import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;

public interface ISPARQL11Interface extends Closeable {
	
	public Response query(QueryRequest req);
	
	public Response update(UpdateRequest req);
	
	public void close() throws IOException;
	
}

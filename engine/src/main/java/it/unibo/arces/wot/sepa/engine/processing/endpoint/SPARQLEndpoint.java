package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.acl.SEPAUserInfo;

public interface SPARQLEndpoint extends AutoCloseable {
	public Response query(QueryRequest req,SEPAUserInfo usr);
	public Response update(UpdateRequest req,SEPAUserInfo usr);
        
        default public Response query(QueryRequest req) {
            return query(req,null);
        }
        default public Response update(UpdateRequest req) {
            return update(req,null);
        }
        
	public void close();
}

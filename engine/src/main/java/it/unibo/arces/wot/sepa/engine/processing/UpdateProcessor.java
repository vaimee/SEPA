/* This class implements the processing of a SPARQL 1.1 UPDATE
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.engine.processing;

import java.util.concurrent.Semaphore;

import com.google.gson.JsonObject;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.ProcessorBeans;
import it.unibo.arces.wot.sepa.timing.Timings;

public class UpdateProcessor {
	private static final Logger logger = LogManager.getLogger();

	private SPARQL11Protocol endpoint;
	private Semaphore endpointSemaphore;
	private SPARQL11Properties properties;

	public UpdateProcessor(SPARQL11Properties properties, Semaphore endpointSemaphore) throws SEPAProtocolException {
		endpoint = new SPARQL11Protocol();
		this.endpointSemaphore = endpointSemaphore;
		this.properties = properties;
	}

	public synchronized Response process(UpdateRequest req) {
		long start = Timings.getTime();

		if (endpointSemaphore != null)
			try {
				endpointSemaphore.acquire();
			} catch (InterruptedException e) {
				return new ErrorResponse(500, e.getMessage());
			}
		// Get the constructs needed to determine added and removed data

		start = System.currentTimeMillis();
		SPARQLAnalyzer sa = new SPARQLAnalyzer(req.getSPARQL());
		UpdateConstruct constructs = sa.getConstruct();


		BindingsResults added =  new BindingsResults(new JsonObject());
		BindingsResults removed =  new BindingsResults(new JsonObject());

		String dc = constructs.getDeleteConstruct();

		if (dc.length() > 0) {
			removed = getTriples(req.getTimeout(), dc);
		}

		String ac = constructs.getInsertConstruct();
		if (ac.length() > 0) {
			added = getTriples(req.getTimeout(), ac);
		}

		long stop = System.currentTimeMillis();
		logger.debug("* ADDED REMOVED PROCESSING ("+(stop-start)+" ms) *");

		ProcessorBeans.updateTimings(start, stop);
		// UPDATE the endpoint
		Response ret;
		UpdateRequest request = new UpdateRequest(req.getToken(), properties.getUpdateMethod(),
				properties.getDefaultProtocolScheme(), properties.getDefaultHost(), properties.getDefaultPort(),
				properties.getUpdatePath(), req.getSPARQL(), req.getTimeout(), req.getUsingGraphUri(),
				req.getUsingNamedGraphUri(), req.getAuthorizationHeader());
		logger.trace(request);
		ret = endpoint.update(request);

		if (endpointSemaphore != null)
			endpointSemaphore.release();

		stop = Timings.getTime();
		logger.trace("Response: " + ret.toString());
		Timings.log("UPDATE_PROCESSING_TIME", start, stop);
		ProcessorBeans.updateTimings(start, stop);

		ret = ret.isUpdateResponse() ? new UpdateResponseWithAR((UpdateResponse) ret,added,removed) : ret;
		return ret;
	}

	private BindingsResults getTriples(int timeout, String dc) {
		BindingsResults removed;
		QueryRequest cons1 = new QueryRequest(dc);
		cons1.setTimeout(timeout);
		logger.debug(cons1.toString());
		removed = ((QueryResponse) endpoint.query(cons1)).getBindingsResults();
		logger.debug(removed);
		return removed;
	}
}

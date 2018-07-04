/* This class implements the processing of a SPARQL 1.1 QUERY
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.AuthenticationProperties;
import it.unibo.arces.wot.sepa.engine.bean.ProcessorBeans;
import it.unibo.arces.wot.sepa.timing.Timings;

public class QueryProcessor {
	private static final Logger logger = LogManager.getLogger();

	private SPARQL11Protocol endpoint;
	private Semaphore endpointSemaphore;
	private SPARQL11Properties properties;

	public QueryProcessor(SPARQL11Properties properties, Semaphore endpointSemaphore) throws SEPAProtocolException {
		this.endpoint = new SPARQL11Protocol();
		this.endpointSemaphore = endpointSemaphore;
		this.properties = properties;
	}

	public synchronized Response process(QueryRequest req) {
		long start = Timings.getTime();

		if (endpointSemaphore != null)
			try {
				endpointSemaphore.acquire();
			} catch (InterruptedException e) {
				return new ErrorResponse(500, e.getMessage());
			}

		// Authorized access to the endpoint
		String authorizationHeader = null;
		try {
			// TODO: to implement also bearer authentication
			AuthenticationProperties oauth = new AuthenticationProperties(properties.getFilename());
			if (oauth.isEnabled())
				authorizationHeader = oauth.getBasicAuthorizationHeader();
		} catch (SEPAPropertiesException | SEPASecurityException e) {
			logger.warn(e.getMessage());
		}

		Response ret;
		QueryRequest request;
		request = new QueryRequest(req.getToken(), properties.getQueryMethod(), properties.getDefaultProtocolScheme(),
				properties.getDefaultHost(), properties.getDefaultPort(), properties.getDefaultQueryPath(),
				req.getSPARQL(), req.getTimeout(), req.getDefaultGraphUri(), req.getNamedGraphUri(),
				authorizationHeader);

		ret = endpoint.query(request);

		if (endpointSemaphore != null)
			endpointSemaphore.release();

		long stop = Timings.getTime();
		logger.trace("Response: " + ret.toString());
		Timings.log("QUERY_PROCESSING_TIME", start, stop);
		ProcessorBeans.queryTimings(start, stop);

		return ret;
	}
}

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.AuthenticationProperties;
import it.unibo.arces.wot.sepa.engine.bean.QueryProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.timing.Timings;

public class QueryProcessor implements QueryProcessorMBean {
	protected static final Logger logger = LogManager.getLogger();

	protected final SPARQL11Protocol endpoint;
	protected final SPARQL11Properties properties;

	public QueryProcessor(SPARQL11Properties properties) throws SEPAProtocolException {
		this.endpoint = new SPARQL11Protocol();
		this.properties = properties;
		
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	public Response process(InternalQueryRequest req) {
		long start = Timings.getTime();

		// Authorized access to the endpoint
		String authorizationHeader = null;
		AuthenticationProperties oauth = null;
		try {
			// TODO: to implement also bearer authentication
			oauth = new AuthenticationProperties(properties.getJSAPFilename());
			if (oauth.isEnabled())
				authorizationHeader = oauth.getBasicAuthorizationHeader();
		} catch (SEPAPropertiesException | SEPASecurityException e) {
			logger.warn("Endpoint authorization: "+e.getMessage());
		}

		Response ret;
		QueryRequest request;
		request = new QueryRequest(properties.getQueryMethod(), properties.getProtocolScheme(),
				properties.getHost(), properties.getPort(), properties.getQueryPath(),
				req.getSparql(), req.getDefaultGraphUri(), req.getNamedGraphUri(),
				authorizationHeader,QueryProcessorBeans.getTimeout());

		ret = endpoint.query(request);

		long stop = Timings.getTime();
		
		logger.trace("Response: " + ret.toString());
		Timings.log("QUERY_PROCESSING_TIME", start, stop);
		QueryProcessorBeans.timings(start, stop);

		return ret;
	}

	@Override
	public void reset() {
		QueryProcessorBeans.reset();
	}

	@Override
	public long getRequests() {
		return QueryProcessorBeans.getRequests();
	}

	@Override
	public float getTimingsCurrent() {
		return QueryProcessorBeans.getCurrent();
	}

	@Override
	public float getTimingsMin() {
		return QueryProcessorBeans.getMin();
	}

	@Override
	public float getTimingsAverage() {
		return QueryProcessorBeans.getAverage();
	}

	@Override
	public float getTimingsMax() {
		return QueryProcessorBeans.getMax();
	}

	@Override
	public int getTimeout() {
		return QueryProcessorBeans.getTimeout();
	}

	@Override
	public void setTimeout(int t) {
		QueryProcessorBeans.setTimeout(t);
	}

	@Override
	public void scale_ms() {
		QueryProcessorBeans.scale_ms();
		
	}

	@Override
	public void scale_us() {
		QueryProcessorBeans.scale_us();
	}

	@Override
	public void scale_ns() {
		QueryProcessorBeans.scale_ns();
	}

	@Override
	public String getUnitScale() {
		return QueryProcessorBeans.getUnitScale();
	}
}

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

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.QueryProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.UpdateProcessorBeans;
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

	public Response process(InternalQueryRequest req) throws SEPASecurityException {
		// Build the request
		QueryRequest request;
		request = new QueryRequest(properties.getQueryMethod(), properties.getProtocolScheme(),
				properties.getHost(), properties.getPort(), properties.getQueryPath(),
				req.getSparql(), req.getDefaultGraphUri(), req.getNamedGraphUri(),
				req.getBasicAuthorizationHeader(),req.getInternetMediaType(),QueryProcessorBeans.getTimeout(),0);
		
		int n = 0;
		Response ret;
		do {
			long start = Timings.getTime();
			ret = endpoint.query(request);
			long stop = Timings.getTime();
			
			UpdateProcessorBeans.timings(start, stop);
			logger.trace("Response: " + ret.toString());
			Timings.log("QUERY_PROCESSING_TIME", start, stop);
			
			n++;
			
			if (ret.isTimeoutError()) {
				QueryProcessorBeans.timedOutRequest();
				logger.error("*** TIMEOUT *** ("+n+"/"+QueryProcessorBeans.getTimeoutNRetry()+") "+req);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.warn("Failed to sleep...");
				}
			}
		} while(ret.isTimeoutError() && n < QueryProcessorBeans.getTimeoutNRetry());
		
		// Request ABORTED
		if (ret.isTimeoutError()) {
			logger.error("*** REQUEST ABORTED *** "+request);
			QueryProcessorBeans.abortedRequest();
		}
		
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

	@Override
	public int getTimeoutNRetry() {
		return QueryProcessorBeans.getTimeoutNRetry();
	}

	@Override
	public void setTimeoutNRetry(int n) {
		QueryProcessorBeans.setTimeoutNRetry(n);
	}

	@Override
	public long getTimedOutRequests() {
		return QueryProcessorBeans.getTimedOutRequests();
	}

	@Override
	public long getAbortedRequests() {
		return QueryProcessorBeans.getAbortedRequests();
	}
}

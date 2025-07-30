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

package com.vaimee.sepa.engine.processing;

import java.io.IOException;

import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.properties.SPARQL11Properties;
import com.vaimee.sepa.api.commons.request.QueryRequest;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.engine.bean.QueryProcessorBeans;
import com.vaimee.sepa.engine.bean.SEPABeans;
import com.vaimee.sepa.engine.bean.UpdateProcessorBeans;
import com.vaimee.sepa.engine.processing.endpoint.JenaInMemoryEndpoint;
import com.vaimee.sepa.engine.processing.endpoint.RemoteEndpoint;
import com.vaimee.sepa.engine.processing.endpoint.SPARQLEndpoint;
import com.vaimee.sepa.engine.scheduling.InternalQueryRequest;
import com.vaimee.sepa.logging.Logging;

class QueryProcessor implements QueryProcessorMBean {
	protected final SPARQL11Properties properties;
	protected SPARQLEndpoint endpoint;

	public QueryProcessor(SPARQL11Properties properties) throws SEPAProtocolException, SEPASecurityException {
		this.properties = properties;
		
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		
		if (properties.getProtocolScheme().equals("jena-api") && properties.getHost().equals("in-memory")) endpoint = new JenaInMemoryEndpoint();
		else endpoint = new RemoteEndpoint();
	}

	public Response process(InternalQueryRequest req) throws SEPASecurityException, IOException {
		// Build the request
		QueryRequest request;
		request = new QueryRequest(properties.getQueryMethod(), properties.getProtocolScheme(),
				properties.getHost(), properties.getPort(), properties.getQueryPath(),
				req.getSparql(), req.getDefaultGraphUri(), req.getNamedGraphUri(),
				req.getBasicAuthorizationHeader(),req.getInternetMediaType(),QueryProcessorBeans.getTimeout(),0);
		
		int n = 0;
		Response ret;
		do {
			Logging.Timestamp start = new Logging.Timestamp();
			ret = endpoint.query(request);
			Logging.Timestamp stop = new Logging.Timestamp();
			
			UpdateProcessorBeans.timings(start.get(), stop.get());
			Logging.trace("Response: " + ret.toString());
			Logging.logTiming("QUERY_PROCESSING_TIME", start, stop);
			
			n++;
			
			if (ret.isTimeoutError()) {
				QueryProcessorBeans.timedOutRequest();
				Logging.error("*** TIMEOUT *** ("+n+"/"+QueryProcessorBeans.getTimeoutNRetry()+") "+req);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Logging.warn("Failed to sleep...");
				}
			}
		} while(ret.isTimeoutError() && n < QueryProcessorBeans.getTimeoutNRetry());
		
		// Request ABORTED
		if (ret.isTimeoutError()) {
			Logging.error("*** REQUEST ABORTED *** "+request);
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

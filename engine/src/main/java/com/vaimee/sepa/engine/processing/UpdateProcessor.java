/** This class implements the processing of a SPARQL 1.1 UPDATE
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.9.12
*/
/*
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
import com.vaimee.sepa.api.commons.request.UpdateRequest;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.engine.bean.SEPABeans;
import com.vaimee.sepa.engine.bean.UpdateProcessorBeans;
import com.vaimee.sepa.engine.processing.endpoint.JenaInMemoryEndpoint;
import com.vaimee.sepa.engine.processing.endpoint.RemoteEndpoint;
import com.vaimee.sepa.engine.processing.endpoint.SPARQLEndpoint;
import com.vaimee.sepa.engine.scheduling.InternalUpdateRequest;
import com.vaimee.sepa.logging.Logging;

class UpdateProcessor implements UpdateProcessorMBean {
	protected final SPARQL11Properties properties;
	protected SPARQLEndpoint endpoint;
	
	public UpdateProcessor(SPARQL11Properties properties) throws SEPAProtocolException, SEPASecurityException {
		this.properties = properties;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		
		if (properties.getProtocolScheme().equals("jena-api") && properties.getHost().equals("in-memory")) endpoint = new JenaInMemoryEndpoint();
		else endpoint = new RemoteEndpoint();
	}

	public Response process(InternalUpdateRequest req) throws SEPASecurityException, IOException {
		// ENDPOINT UPDATE (set timeout and set retry = 0)
		UpdateRequest request = new UpdateRequest(properties.getUpdateMethod(), properties.getProtocolScheme(),
				properties.getHost(), properties.getPort(), properties.getUpdatePath(), req.getSparql(),
				req.getDefaultGraphUri(), req.getNamedGraphUri(), req.getBasicAuthorizationHeader(),
				UpdateProcessorBeans.getTimeout(), 0);
		Logging.trace(request);

		Response ret;
		int n = 0;
		do {
			long start = Logging.getTime();
			ret = endpoint.update(request);
			long stop = Logging.getTime();

			UpdateProcessorBeans.timings(start, stop);

			Logging.trace("Response: " + ret.toString());
			Logging.logTiming("UPDATE_PROCESSING_TIME", start, stop);

			n++;

			if (ret.isTimeoutError()) {
				UpdateProcessorBeans.timedOutRequest();
				Logging.error("*TIMEOUT* (" + n + "/" + UpdateProcessorBeans.getTimeoutNRetry() + ") " + req);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Logging.warn("Failed to sleep...");
				}
			}
		} while (ret.isTimeoutError() && n < UpdateProcessorBeans.getTimeoutNRetry());

		if (ret.isTimeoutError()) {
			Logging.error("*** REQUEST ABORTED *** " + request);
			UpdateProcessorBeans.abortedRequest();
		}

		return ret;
	}

	@Override
	public void reset() {
		UpdateProcessorBeans.reset();
	}

	@Override
	public long getRequests() {
		return UpdateProcessorBeans.getRequests();
	}

	@Override
	public float getTimingsCurrent() {
		return UpdateProcessorBeans.getCurrent();
	}

	@Override
	public float getTimingsMin() {
		return UpdateProcessorBeans.getMin();
	}

	@Override
	public float getTimingsAverage() {
		return UpdateProcessorBeans.getAverage();
	}

	@Override
	public float getTimingsMax() {
		return UpdateProcessorBeans.getMax();
	}

	@Override
	public long getTimeout() {
		return UpdateProcessorBeans.getTimeout();
	}

	@Override
	public void setTimeout(long t) {
		UpdateProcessorBeans.setTimeout(t);
	}

	@Override
	public void scale_ms() {
		UpdateProcessorBeans.scale_ms();
	}

	@Override
	public void scale_us() {
		UpdateProcessorBeans.scale_us();
	}

	@Override
	public void scale_ns() {
		UpdateProcessorBeans.scale_ns();
	}

	@Override
	public String getUnitScale() {
		return UpdateProcessorBeans.getUnitScale();
	}

	@Override
	public int getTimeoutNRetry() {
		return UpdateProcessorBeans.getTimeoutNRetry();
	}

	@Override
	public void setTimeoutNRetry(int n) {
		UpdateProcessorBeans.setTimeoutNRetry(n);
	}

	@Override
	public long getTimedOutRequests() {
		return UpdateProcessorBeans.getTimedOutRequests();
	}

	@Override
	public long getAbortedRequests() {
		return UpdateProcessorBeans.getAbortedRequests();
	}
}

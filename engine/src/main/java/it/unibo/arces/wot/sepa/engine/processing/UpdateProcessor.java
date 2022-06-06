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

package it.unibo.arces.wot.sepa.engine.processing;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.acl.SEPAUserInfo;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.UpdateProcessorBeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.RemoteEndpoint;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.SPARQLEndpoint;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.SjenarEndpoint;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.SjenarEndpointDoubleStore;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.logging.Logging;
import it.unibo.arces.wot.sepa.logging.Timings;

class UpdateProcessor implements UpdateProcessorMBean {
	protected final SPARQL11Properties properties;

	public UpdateProcessor(SPARQL11Properties properties) throws SEPAProtocolException, SEPASecurityException {
		this.properties = properties;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	private Response process(InternalUpdateRequest req,Boolean firstStore) throws SEPASecurityException, IOException {
		//System.out.println("------------------->UPDATE ON: "+firstStore );
		// ENDPOINT UPDATE (set timeout and set retry = 0)
		UpdateRequest request = new UpdateRequest(properties.getUpdateMethod(), properties.getProtocolScheme(),
				properties.getHost(), properties.getPort(), properties.getUpdatePath(), req.getSparql(),
				req.getDefaultGraphUri(), req.getNamedGraphUri(), req.getBasicAuthorizationHeader(),
				UpdateProcessorBeans.getTimeout(), 0);
		Logging.logger.trace(request);

		Response ret;
		int n = 0;
		do {
			long start = Timings.getTime();
			SPARQLEndpoint endpoint;
			if(EngineProperties.getIstance().isLUTTEnabled()) {
				endpoint = new SjenarEndpointDoubleStore(firstStore);
			}else if (properties.getProtocolScheme().equals("jena-api") && properties.getHost().equals("in-memory")) {
				endpoint = new SjenarEndpoint();
			}else {
				endpoint = new RemoteEndpoint();
			}
                        
                        
			ret = endpoint.update(request);
			endpoint.close();
			long stop = Timings.getTime();

			UpdateProcessorBeans.timings(start, stop);

			Logging.logger.trace("Response: " + ret.toString());
			Timings.log("UPDATE_PROCESSING_TIME", start, stop);

			n++;

			if (ret.isTimeoutError()) {
				UpdateProcessorBeans.timedOutRequest();
				Logging.logger.error("*TIMEOUT* (" + n + "/" + UpdateProcessorBeans.getTimeoutNRetry() + ") " + req);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Logging.logger.warn("Failed to sleep...");
				}
			}
		} while (ret.isTimeoutError() && n < UpdateProcessorBeans.getTimeoutNRetry());

		if (ret.isTimeoutError()) {
			Logging.logger.error("*** REQUEST ABORTED *** " + request);
			UpdateProcessorBeans.abortedRequest();
		}

		return ret;
	}
	
	public Response process(InternalUpdateRequest req) throws SEPASecurityException, IOException {
		//as default we will use first storage for updated
		return process(req,true);
	}
	
	public Response processOnSecondStore(InternalUpdateRequest req) throws SEPASecurityException, IOException {
		if (EngineProperties.getIstance().isLUTTEnabled()) {
			return process(req,false);
		}else {
			throw new IOException("You are not using double storage and LUTT system.");
		}
	}
	
	public Response processOnFirstStore(InternalUpdateRequest req) throws SEPASecurityException, IOException {
		if (EngineProperties.getIstance().isLUTTEnabled()) {
	        return process(req,true);
		}else {
			throw new IOException("You are not using double storage and LUTT system.");
		}
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

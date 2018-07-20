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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.AuthenticationProperties;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.UpdateProcessorBeans;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.timing.Timings;

class UpdateProcessor implements UpdateProcessorMBean {
	private static final Logger logger = LogManager.getLogger();

	private final SPARQL11Protocol endpoint;
	private final Semaphore endpointSemaphore;
	private final SPARQL11Properties properties;

	public UpdateProcessor(SPARQL11Properties properties, Semaphore endpointSemaphore) throws SEPAProtocolException {
		endpoint = new SPARQL11Protocol();
		this.endpointSemaphore = endpointSemaphore;
		this.properties = properties;
		
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	public synchronized Response process(InternalUpdateRequest req) {
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
			//TODO: to implement also bearer authentication
			AuthenticationProperties oauth = new AuthenticationProperties(properties.getFilename());
			if (oauth.isEnabled()) authorizationHeader = oauth.getBasicAuthorizationHeader();			
		} catch (SEPAPropertiesException | SEPASecurityException e) {
			logger.warn(e.getMessage());
		}
				
		// UPDATE the endpoint
		Response ret;
		UpdateRequest request = new UpdateRequest(properties.getUpdateMethod(),
				properties.getDefaultProtocolScheme(), properties.getDefaultHost(), properties.getDefaultPort(),
				properties.getUpdatePath(), req.getSparql(), req.getDefaultGraphUri(),
				req.getNamedGraphUri(), authorizationHeader,UpdateProcessorBeans.getTimeout());
		logger.trace(request);
		ret = endpoint.update(request);

		if (endpointSemaphore != null)
			endpointSemaphore.release();

		long stop = Timings.getTime();
		UpdateProcessorBeans.timings(start, stop);
		
		logger.trace("Response: " + ret.toString());
		Timings.log("UPDATE_PROCESSING_TIME", start, stop);
		
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
}

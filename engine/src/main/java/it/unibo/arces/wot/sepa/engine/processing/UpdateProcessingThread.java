/* Class for processing SPARQL 1.1 update requests
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

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

class UpdateProcessingThread extends Thread {
	private static final Logger logger = LogManager.getLogger();
	
	private final Processor processor;
	
	public UpdateProcessingThread(Processor processor) {
		this.processor = processor;
		setName("SEPA-Update-Processor");
	}

	public void run() {
		while (processor.isRunning()) {
			ScheduledRequest request;
			try {
				request = processor.getScheduler().waitUpdateRequest();
			} catch (InterruptedException e) {
				return;
			}

			// Update request
			InternalUpdateRequest update = (InternalUpdateRequest)request.getRequest();
			
			// Notify update (not reliable)
			if (!processor.isUpdateReilable()) processor.getScheduler().addResponse(request.getToken(),new UpdateResponse("Processing: "+update));
						
			// PRE-processing update request
			InternalUpdateRequest preRequest;
			try {
				preRequest = processor.getUpdateProcessor().preProcess(update);
			} catch (SEPAProcessingException e2) {
				logger.error("Pre processing aborted: "+e2.getMessage());
				ErrorResponse errorResponse = new ErrorResponse(500, "PreProcessing failed: "+update, e2.getMessage());
				processor.getScheduler().addResponse(request.getToken(),errorResponse);
				continue;
			}
			
			// PRE-processing subscriptions (endpoint not yet updated)
			try {
				processor.preUpdateProcessing(preRequest);
			} catch (SEPAProcessingException e) {
				logger.error("PreUpdateProcessing failed: "+e.getMessage());
				ErrorResponse errorResponse = new ErrorResponse(500, "PreUpdateProcessing failed: "+update, e.getMessage());
				processor.getScheduler().addResponse(request.getToken(),errorResponse);
				continue;
			}
			
			// Processing UPDATE
			Response ret;
			try {
				ret = processor.getUpdateProcessor().process(preRequest);
			} catch (InterruptedException e1) {
				logger.error("Processing failed: "+e1.getMessage());
				ErrorResponse errorResponse = new ErrorResponse(500, "Processing failed: "+update, e1.getMessage());
				processor.getScheduler().addResponse(request.getToken(),errorResponse);
				continue;
			}

			// Notify update result
			if (processor.isUpdateReilable()) processor.getScheduler().addResponse(request.getToken(),ret);

			// Subscription processing (post update)
			try {
				processor.postUpdateProcessing(ret);
			} catch (SEPAProcessingException e) {
				logger.warn("Post processing failed: "+e.getMessage());
				continue;
			}
		}
	}
}

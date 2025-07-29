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

package com.vaimee.sepa.engine.processing;

import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.response.UpdateResponse;
import com.vaimee.sepa.engine.scheduling.InternalUpdateRequest;
import com.vaimee.sepa.engine.scheduling.ScheduledRequest;
import com.vaimee.sepa.logging.Logging;

class UpdateProcessingThread extends Thread {
	private final Processor processor;

	public UpdateProcessingThread(Processor processor) {
		this.processor = processor;
		setName("SEPA-Update-Processor");
	}

	public void run() {
		while (processor.isRunning()) {
                    try {
			ScheduledRequest request;
			try {
				Logging.trace("Wait for update requests...");
				request = processor.waitUpdateRequest();
			} catch (InterruptedException e) {
				return;
			}

			// Update request
			InternalUpdateRequest update = (InternalUpdateRequest) request.getRequest();

			// Notify update (not reliable)
			if (!processor.isUpdateReliable()) {
				Logging.trace("Notify client of update processing (not reliable)");
				processor.addResponse(request.getToken(), new UpdateResponse("Processing: " + update));
			}

			// Process update
			Logging.trace("Start processing update...");
			Response ret = processor.processUpdate(update);
			Logging.trace(ret);
			Logging.trace("Update processing COMPLETED");

			// Notify update result
			if (processor.isUpdateReliable()) {
				Logging.trace("Notify client of update processing (reliable)");
				processor.addResponse(request.getToken(), ret);
			}
                    } catch(Throwable t) {
                        System.err.println(t);
                        t.printStackTrace(System.err);
                    }
		}
	}
}

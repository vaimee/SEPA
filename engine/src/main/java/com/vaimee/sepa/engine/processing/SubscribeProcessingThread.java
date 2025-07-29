/* Class for processing SPARQL 1.1 subscribe requests
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
import com.vaimee.sepa.engine.scheduling.InternalSubscribeRequest;
import com.vaimee.sepa.engine.scheduling.ScheduledRequest;
import com.vaimee.sepa.logging.Logging;

class SubscribeProcessingThread extends Thread {
	private final Processor processor;

	public SubscribeProcessingThread(Processor processor) {
		this.processor = processor;

		setName("SEPA-Subscribe-Processor");
	}
	
	public void run() {
		while (processor.isRunning()) {
			try {
				// Wait request...
				ScheduledRequest request = processor.waitSubscribeRequest();
				Logging.debug(">> " + request);

				// Process request
				Response response = processor.processSubscribe((InternalSubscribeRequest) request.getRequest());
				
				Logging.debug("<< " + response);

				// Send back response
				processor.addResponse(request.getToken(), response);

			} catch (InterruptedException e) {
				Logging.warn("Exit");
				return;
			}
		}
	}
}

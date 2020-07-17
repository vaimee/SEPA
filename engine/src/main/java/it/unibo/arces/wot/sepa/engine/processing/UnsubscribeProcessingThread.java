/** Class for processing SPARQL 1.1 subscribe requests
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

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUnsubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

class UnsubscribeProcessingThread extends Thread {
	private static final Logger logger = LogManager.getLogger();

	private final Processor processor;

	public UnsubscribeProcessingThread(Processor processor) {
		this.processor = processor;

		setName("SEPA-Unsubscribe-Processor");
	}
	
	public void run() {
		while (processor.isRunning()) {
			try {
				// Wait request...
				ScheduledRequest request = processor.waitUnsubscribeRequest();
				logger.debug(">> " + request);

				// Process request
				String sid = ((InternalUnsubscribeRequest) request.getRequest()).getSID();
				String gid = ((InternalUnsubscribeRequest) request.getRequest()).getGID();
				Response response = processor.processUnsubscribe(sid,gid);
				
				logger.debug("<< " + response);

				// Send back response
				processor.addResponse(request.getToken(), response);

			} catch (InterruptedException e) {
				logger.warn(e.getMessage());
				return;
			}
		}
	}
}

/* Class for processing SPARQL 1.1 query requests
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

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.engine.scheduling.InternalQueryRequest;
import com.vaimee.sepa.engine.scheduling.ScheduledRequest;
import com.vaimee.sepa.logging.Logging;

class QueryProcessingThread extends Thread{
	private final Processor processor;
	
	public QueryProcessingThread(Processor processor) {
		this.processor = processor; 
		setName("SEPA-Query-Processor");
	}
	
	public void run() {
		while(processor.isRunning()) {
			ScheduledRequest request;
			try {
				request = processor.waitQueryRequest();
			} catch (InterruptedException e) {
				return;
			}
			
			InternalQueryRequest query = (InternalQueryRequest) request.getRequest();
			
			Response ret;
			try {
				ret = processor.processQuery(query);
			} catch (SEPASecurityException | IOException e) {
				Logging.logger.error(e.getMessage());
				if (Logging.logger.isTraceEnabled()) e.printStackTrace();
				ret = new ErrorResponse(401,"SEPASecurityException",e.getMessage());
			}
			
			processor.addResponse(request.getToken(),ret);
		}
	}	
}

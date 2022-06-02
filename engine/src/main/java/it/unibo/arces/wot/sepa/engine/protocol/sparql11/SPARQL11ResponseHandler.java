/* HTTP response handler for the SPARQL 1.1 protocol
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


package it.unibo.arces.wot.sepa.engine.protocol.sparql11;

import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.HttpAsyncExchange;


import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.HTTPHandlerBeans;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.gates.http.HttpUtilities;
import it.unibo.arces.wot.sepa.logging.Logging;
import it.unibo.arces.wot.sepa.logging.Timings;

public class SPARQL11ResponseHandler implements ResponseHandler {
	private HttpAsyncExchange handler;
	private HTTPHandlerBeans jmx;
	
	public SPARQL11ResponseHandler(HttpAsyncExchange httpExchange, HTTPHandlerBeans jmx) {
		this.handler = httpExchange;
		this.jmx = jmx;
		jmx.start(handler);
	}

	@Override
	public void sendResponse(Response response) {
		if (response.isError()) {
			ErrorResponse err = (ErrorResponse) response;
			HttpUtilities.sendFailureResponse(handler,err);
			Logging.logger.error(err);
			jmx.timeoutRequest();
		}
		else
			HttpUtilities.sendResponse(handler, HttpStatus.SC_OK, response.toString());
		
		Timings.log(response);
		jmx.stop(handler);
		Logging.logger.trace(response);	
	}
}

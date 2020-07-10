/* HTTP echo
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

package it.unibo.arces.wot.sepa.engine.gates.http;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;


public class EchoHandler implements HttpAsyncRequestHandler<HttpRequest> {
	
	public void handleInternal(HttpRequest request, HttpResponse response, HttpContext context)
			throws HttpException, IOException {
		
		response.setStatusCode(HttpStatus.SC_OK);
		NStringEntity entity = new NStringEntity(
				HttpUtilities.buildEchoResponse(request).toString(),
                ContentType.create("application/json", "UTF-8"));
         response.setEntity(entity);
	}

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(HttpRequest data, HttpAsyncExchange httpExchange, HttpContext context)
			throws HttpException, IOException {
		HttpResponse response = httpExchange.getResponse();
        handleInternal(data, response, context);
        httpExchange.submitResponse(new BasicAsyncResponseProducer(response));		
	}

}

package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

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

import it.unibo.arces.wot.sepa.engine.protocol.http.HttpUtilities;


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

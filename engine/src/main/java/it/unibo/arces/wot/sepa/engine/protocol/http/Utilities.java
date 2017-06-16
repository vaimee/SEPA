package it.unibo.arces.wot.sepa.engine.protocol.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Utilities {
	public static void sendResponse(HttpAsyncExchange exchange, int httpResponseCode, String response) {
		exchange.getResponse().addHeader("Content-Type", "application/json");
		exchange.getResponse().setStatusCode(httpResponseCode);

		try {
			exchange.getResponse().setEntity(new NStringEntity(response, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			exchange.getResponse().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}

		exchange.submitResponse(new BasicAsyncResponseProducer(exchange.getResponse()));
	}

	public static  void failureResponse(HttpAsyncExchange exchange, int httpResponseCode, String responseBody) {
		JsonObject json = buildEchoResponse(exchange.getRequest());

		json.add("body", new JsonPrimitive(responseBody));
		json.add("code", new JsonPrimitive(httpResponseCode));

		sendResponse(exchange, httpResponseCode, json.toString());
	}
	
	public static JsonObject buildEchoResponse(HttpRequest request) {
		JsonObject json = new JsonObject();

		json.add("method", new JsonPrimitive(request.getRequestLine().getMethod().toUpperCase()));
		json.add("protocol", new JsonPrimitive(request.getProtocolVersion().getProtocol()));

		JsonObject headers = new JsonObject();

		for (Header header : request.getAllHeaders()) {
			headers.add(header.getName(), new JsonPrimitive(header.getValue()));
		}
		json.add("headers", headers);

		String body = "";
		HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
		try {
			body = EntityUtils.toString(entity);
		} catch (ParseException | IOException e) {
			body = e.getLocalizedMessage();
		}

		json.add("body", new JsonPrimitive(body));

		return json;
	}
}

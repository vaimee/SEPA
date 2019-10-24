/* HTTP utility class
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
//import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.util.EntityUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;

public class HttpUtilities {
	private static final Logger logger = LogManager.getLogger();

	public static void sendResponse(HttpAsyncExchange exchange, int httpResponseCode, String body) {
		exchange.getResponse().setStatusCode(httpResponseCode);
		HttpEntity entity = new StringEntity(body, ContentType.create("application/json", Consts.UTF_8));
		exchange.getResponse().setEntity(entity);
		if(!exchange.isCompleted()) exchange.submitResponse(new BasicAsyncResponseProducer(exchange.getResponse()));	
	}

	public static void sendFailureResponse(HttpAsyncExchange exchange, ErrorResponse error) {	
		sendResponse(exchange,error.getStatusCode(), error.toString());
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

		json.add("body",getRequestBodyJSON(request));

		return json;
	}

	private static JsonPrimitive getRequestBodyJSON(HttpRequest request) {
		String body = "";
		// Check if the request has a body ( a POST or PUT )
		if(request instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			try {
				body = EntityUtils.toString(entity);
			} catch (ParseException | IOException e) {
				body = e.getMessage();
				logger.error(body);
			}
		}
		return new JsonPrimitive(body);
	}

	public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
	    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
	    String[] pairs = query.split("&");
	    for (String pair : pairs) {
	        int idx = pair.indexOf("=");
	        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
	    }
	    return query_pairs;
	}
}

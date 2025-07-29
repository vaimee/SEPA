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

package com.vaimee.sepa.engine.gates.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.logging.Logging;

public class HttpUtilities {
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
				Logging.error(body);
			}
		}
		return new JsonPrimitive(body);
	}

	public static Map<String, Set<String>> splitQuery(String query) throws UnsupportedEncodingException {
	    /*
	    * query (exactly 1)
	    * default-graph-uri (0 or more)
	    * named-graph-uri (0 or more)
	    *
	    * using-graph-uri (0 or more)
	    * using-named-graph-uri (0 or more)
	    * */
		Map<String, Set<String>> query_pairs = new LinkedHashMap<String, Set<String>>();
		int query_index = query.indexOf("query=");
		int update_index = query.indexOf("update=");

		int index = (query_index != -1 ? query_index : update_index);

		int content_index = query.indexOf('=',index);
		String keyString = URLDecoder.decode(query.substring(0, content_index), "UTF-8");
		String valueString = URLDecoder.decode(query.substring(content_index + 1), "UTF-8");
		if (!query_pairs.containsKey(keyString)) query_pairs.put(keyString, new HashSet<String>());
		query_pairs.get(keyString).add(valueString);

		/*int default_graph_uri_index = query.indexOf("default-graph-uri=");
		int named_graph_uri_index = query.indexOf("named-graph-uri=");

		int using_graph_uri_index = query.indexOf("using-graph-uri=");
		int using_named_graph_uri_index = query.indexOf("using-named-graph-uri=");

		if (query_index!=-1) {
			int query_content_index = query.indexOf('=',query_index);

			if (default_graph_uri_index==-1 && named_graph_uri_index==-1) {
				String keyString = URLDecoder.decode(query.substring(0, query_content_index), "UTF-8");
				String valueString = URLDecoder.decode(query.substring(query_content_index + 1), "UTF-8");
				if (!query_pairs.containsKey(keyString))  query_pairs.put(keyString, new HashSet<String>());
				query_pairs.get(keyString).add(valueString);
			} else if (default_graph_uri_index!=-1 && named_graph_uri_index==-1){
				if (default_graph_uri_index > query_index) {

				}
				else {

				}
			} else if (default_graph_uri_index==-1 && named_graph_uri_index!=-1) {

			} else {

			}
		}

	    String[] pairs = query.split("&");
	    for (String pair : pairs) {
	        int idx = pair.indexOf("=");
	        String keyString = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
	        String valueString = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
	        if (!query_pairs.containsKey(keyString))  query_pairs.put(keyString, new HashSet<String>());  	
	        query_pairs.get(keyString).add(valueString);
	    }*/
	    return query_pairs;
	}
}

/* This class has been implemented for CORS management
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

package com.vaimee.sepa.engine.dependability; 

import org.apache.http.Header;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import com.vaimee.sepa.logging.Logging;

/**
 * The Class CORSManager.
 */
class CORSManager {
	
	/**
	 * Process a CORS (Cross-Origin Resource Sharing) pre-flight request. <br>
	 * 	 
	 * References:<br>
	 * <a href="https://www.w3.org/TR/cors">CORS</a><br>
	 * <a href="https://www.w3.org/wiki/CORS">CORS wiki</a><br>
	 * <a href="https://fetch.spec.whatwg.org/">CORS specification</a><br>
	 *
	 * @param httpExchange the http exchange
	 * @return true, if the pre-flight request has been successfully handled
	 */
	public static boolean processCORSRequest(HttpAsyncExchange exchange){
		if(exchange.getRequest().getRequestLine().getMethod().toUpperCase().equals("OPTIONS")) {
			Logging.logger.debug("CORS pre-flight request");
	
			/*
			 * If the Origin header is not present terminate this set of steps. The request is outside the scope of this specification.
			 */
			Logging.logger.debug("Get Origin header");
			Header[] origins = exchange.getRequest().getHeaders("Origin");
			if (origins.length != 1) {
				Logging.logger.debug("Origin header is not present. Number of origin headers: "+origins.length);
				return false;
			}
			
			String allowOrigin = origins[0].getValue();	
			Logging.logger.debug("Check origin: "+allowOrigin);
			if(!allowedOrigin(allowOrigin)) {
				Logging.logger.debug("Origin: "+allowOrigin+" is not allowed");
				return false;
			}
			Logging.logger.debug("Origin: "+allowOrigin+ " ALLOWED");
			
			/*
			 * Let method be the value as result of parsing the Access-Control-Request-Method header.
			 * If there is no Access-Control-Request-Method header or if parsing failed, do not set any additional headers and terminate this set of steps. 
			 * The request is outside the scope of this specification.
			 */
			
			Logging.logger.debug("Get Access-Control-Request-Method header");
			Header[] methods = exchange.getRequest().getHeaders("Access-Control-Request-Method" );
			if (methods.length != 1) {
				Logging.logger.debug("Method not specified. Number of methods:"+methods.length);
				return false;
			}
			
			String allowMethod = methods[0].getValue();
			Logging.logger.debug("Check method: "+allowMethod);
			if(!allowedMethod(allowMethod)) {
				Logging.logger.debug("Method: "+allowMethod+ " NOT allowed");
				return false;
			}
			Logging.logger.debug("Method: "+allowMethod+ " ALLOWED");
			
			/*
			 * Let header field-names be the values as result of parsing the Access-Control-Request-Headers headers.
			 * If there are no Access-Control-Request-Headers headers let header field-names be the empty list.
			 * If parsing failed do not set any additional headers and terminate this set of steps. The request is outside the scope of this specification.
			 */
			
			String fieldNames = "";
			Header[] headers = exchange.getRequest().getHeaders("Access-Control-Request-Headers");		
			for (Header temp : headers) {
				if (fieldNames.equals("")) fieldNames = temp.getValue();
				else fieldNames = fieldNames +","+temp.getValue();	
			}
			
			/*
			 * If the resource supports credentials add a single Access-Control-Allow-Origin header, with the value of the Origin header as value, 
			 * and add a single Access-Control-Allow-Credentials header with the case-sensitive string "true" as value.
			 * Otherwise, add a single Access-Control-Allow-Origin header, with either the value of the Origin header or the string "*" as value.
			 */
			if (allowOrigin != null) exchange.getResponse().addHeader("Access-Control-Allow-Origin", allowOrigin);
			
			/*
			 * If method is a simple method this step may be skipped.
			 * Add one or more Access-Control-Allow-Methods headers consisting of (a subset of) the list of methods.
			 */
			if (allowMethod != null) exchange.getResponse().addHeader("Access-Control-Allow-Methods", allowMethod);
			
			/*
			 * If each of the header field-names is a simple header and none is Content-Type, this step may be skipped.
			 * Add one or more Access-Control-Allow-Headers headers consisting of (a subset of) the list of headers.
			 */
			filterHeaders(fieldNames);
			if (!fieldNames.equals("")) exchange.getResponse().addHeader("Access-Control-Allow-Headers", fieldNames);		   
			
			for (Header head : exchange.getResponse().getAllHeaders())
				Logging.logger.debug(head);
			return true;
		}
		else {
			/*
			 * If the Origin header is not present terminate this set of steps. The request is outside the scope of this specification.
			 */
			Logging.logger.trace("Method: "+ exchange.getRequest().getRequestLine().getMethod().toUpperCase()+" should be OPTIONS");
			
			String allowOrigin = null;
			
			Header[] origins = exchange.getRequest().getHeaders("Origin");
			if (origins.length == 0) return true;
			if (origins.length > 1) return false;
			
			allowOrigin = origins[0].getValue();	
			
			boolean allowed = allowedOrigin(allowOrigin);
			
			if (allowed) exchange.getResponse().addHeader("Access-Control-Allow-Origin", allowOrigin);
			
			return allowed;
		}
	}
	
	/*
	 * If any of the header field-names is not a ASCII case-insensitive match for any of the values in list of headers do not set any additional headers and terminate this set of steps.
	 */	
	private static void filterHeaders(String allowHeaders) {
		//TODO filter headers
	}

	/*
	 * If method is not a case-sensitive match for any of the values in list of methods do not set any additional headers and terminate this set of steps.
	 */
	private static boolean allowedMethod(String allowMethod) {
		//TODO check method against a list of allowed methods
		Logging.logger.debug("All methods are allowed");
		return true;
	}

	/*
	 * If the value of the Origin header is not a case-sensitive match for any of the values in list of origins do not set any additional headers and terminate this set of steps.
	 */
	private static boolean allowedOrigin(String allowOrigin) {
		//TODO check origin against a list of allowed origins
		Logging.logger.debug("All origins are allowed");
		return true;
	}

	/*
	 * A CORS preflight request is a CORS request that checks to see if the CORS protocol is understood and a server is aware using specific methods and headers.
	 * It is an OPTIONS request, using three HTTP request headers: Access-Control-Request-Method, Access-Control-Request-Headers, and the Origin header.
	 * */
	public static boolean isPreFlightRequest(HttpAsyncExchange exchange) {
		return exchange.getRequest().getRequestLine().getMethod().equalsIgnoreCase("OPTIONS");
	}
}

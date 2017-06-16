/* This class represents an error response
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
package it.unibo.arces.wot.sepa.commons.response;

import com.google.gson.JsonPrimitive;

// TODO: Auto-generated Javadoc
/**
 * It represents a generic error. If it applies, the use of HTTP status codes is RECOMMENDED 
 * (<b><a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">RFC 2616</a></b>)<br><br>
 *
	400 Bad Request<br>
	401 Unauthorized<br>
	402 Payment Required<br>
	403 Forbidden<br>
	404 Not Found<br>
	405 Method Not Allowed<br>
	406 Not Acceptable<br>
	407 Proxy Authentication Required<br>
	408 Request Timeout<br>
	409 Conflict<br>
	410 Gone<br>
	411 Length Required<br>
	412 Precondition Failed<br>
	413 Request Entity Too Large<br>
	414 Request-URI Too Long<br>
	415 Unsupported Media Type<br>
	416 Requested Range Not Satisfiable<br>
	417 Expectation Failed<br>

	500 Internal Server Error<br>
	501 Not Implemented<br>
	502 Bad Gateway<br>
	503 Service Unavailable<br>
	504 Gateway Timeout<br>
	505 HTTP Version Not Supported<br><br>

 * The JSON representation of an error response follows:<br>
 *
 * {<br>
 * 		"body" : "Internal Server Error: SPARQL endpoint not found" , <br>
 * 		"code" : 500<br>
 * }<br>
 * <br> Body is optional
* */
public class ErrorResponse extends Response {	
	/**
	 * Instantiates a new error response.
	 *
	 * @param token the token
	 * @param code the code
	 * @param message the message
	 */
	public ErrorResponse(int token,int code,String message) {
		super(token);

		if (message != null) json.add("body", new JsonPrimitive(message));
		json.add("code", new JsonPrimitive(code));
	}
	
	/**
	 * Instantiates a new error response.
	 *
	 * @param code the code
	 * @param message the message
	 */
	public ErrorResponse(int code,String message) {
		super();

		if (message != null) json.add("body", new JsonPrimitive(message));
		json.add("code", new JsonPrimitive(code));
	}
	
	/**
	 * Instantiates a new error response.
	 *
	 * @param code the code
	 */
	public ErrorResponse(int code) {
		super();

		json.add("code", new JsonPrimitive(code));
	}
	
	/**
	 * Gets the error code.
	 *
	 * @return the error code
	 */
	public int getErrorCode() {
		return json.get("code").getAsInt();
	}
	
	/**
	 * Gets the error message.
	 *
	 * @return the error message
	 */
	public String getErrorMessage() {
		if (json.get("body") != null) return json.get("body").getAsString();
		return "";
	}
}

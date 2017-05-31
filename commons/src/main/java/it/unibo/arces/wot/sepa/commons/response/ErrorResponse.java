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
	
	/** The Constant BAD_REQUEST. */
	public static final int BAD_REQUEST = 400;
	
	/** The Constant UNAUTHORIZED. */
	public static final int UNAUTHORIZED = 401;
	
	/** The Constant PAYMENT_REQUIRED. */
	public static final int PAYMENT_REQUIRED = 402;
	
	/** The Constant FORBIDDEN. */
	public static final int FORBIDDEN = 403;
	
	/** The Constant NOT_FOUND. */
	public static final int NOT_FOUND = 404;
	
	/** The Constant NOT_ALLOWED. */
	public static final int NOT_ALLOWED = 405;
	
	/** The Constant NOT_ACCEPTABLE. */
	public static final int NOT_ACCEPTABLE = 406;
	
	/** The Constant PROXY_AUTHENTICATION_REQUIRED. */
	public static final int PROXY_AUTHENTICATION_REQUIRED = 407;
	
	/** The Constant REQUEST_TIMEOUT. */
	public static final int REQUEST_TIMEOUT = 408 ;
	
	/** The Constant CONFLICT. */
	public static final int CONFLICT = 409;
	
	/** The Constant GONE. */
	public static final int GONE = 410;
	
	/** The Constant LENGTH_REQUIRED. */
	public static final int LENGTH_REQUIRED = 411;
	
	/** The Constant PRECONDITION_FAILED. */
	public static final int PRECONDITION_FAILED = 412;
	
	/** The Constant REQUEST_ENTITY_TOO_LARGE. */
	public static final int REQUEST_ENTITY_TOO_LARGE = 413;
	
	/** The Constant REQUEST_URI_TOO_LONG. */
	public static final int REQUEST_URI_TOO_LONG = 414;
	
	/** The Constant UNSUPPORTED_MEDIA_TYPE. */
	public static final int UNSUPPORTED_MEDIA_TYPE = 415;
	
	/** The Constant REQUESTED_RANGE_NOT_SATISFIABLE. */
	public static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;
	
	/** The Constant EXPECTATION_FAILED. */
	public static final int EXPECTATION_FAILED = 417;

	/** The Constant INTERNAL_SERVER_ERROR. */
	public static final int INTERNAL_SERVER_ERROR = 500;
	
	/** The Constant NOT_IMPLEMENTED. */
	public static final int NOT_IMPLEMENTED = 501;
	
	/** The Constant BAD_GATEWAY. */
	public static final int BAD_GATEWAY = 502;
	
	/** The Constant SERVICE_UNAVAILABLE. */
	public static final int SERVICE_UNAVAILABLE = 503;
	
	/** The Constant GATEWAY_TIMEOUT. */
	public static final int GATEWAY_TIMEOUT = 504;
	
	/** The Constant HTTP_VERSION_NOT_SUPPORTED. */
	public static final int HTTP_VERSION_NOT_SUPPORTED = 505;
	
	
	/**
	 * Instantiates a new error response.
	 *
	 * @param token the token
	 * @param code the code
	 * @param message the message
	 */
	public ErrorResponse(Integer token,int code,String message) {
		super(token);

		if (message != null) json.add("body", new JsonPrimitive(message));
		json.add("code", new JsonPrimitive(code));
	}
	
	/**
	 * Instantiates a new error response.
	 *
	 * @param token the token
	 * @param code the code
	 */
	public ErrorResponse(int token,int code) {
		super(token);

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

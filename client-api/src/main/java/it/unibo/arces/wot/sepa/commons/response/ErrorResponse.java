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

<pre>

The JSON representation of an error response follows:<br>

 {
   "error":"Unless specified otherwise see RFC6749. Otherwise, this is specific of the SPARQL 1.1 SE Protocol",
   "error_description":"Unless specified otherwise, see RFC6749. Otherwise, this is specific of the SPARQL 1.1 SE Protocol", (OPTIONAL)
   "status_code" : the HTTP status code (would be 400 for Oauth 2.0 errors).
 }
  
  5.2.  Error Response

   The authorization server responds with an HTTP 400 (Bad Request)
   status code (unless specified otherwise) and includes the following
   parameters with the response:

   error
         REQUIRED.  A single ASCII [USASCII] error code from the
         following:

         invalid_request
               The request is missing a required parameter, includes an
               unsupported parameter value (other than grant type),
               repeats a parameter, includes multiple credentials,
               utilizes more than one mechanism for authenticating the
               client, or is otherwise malformed.

         invalid_client
               Client authentication failed (e.g., unknown client, no
               client authentication included, or unsupported
               authentication method).  The authorization server MAY
               return an HTTP 401 (Unauthorized) status code to indicate
               which HTTP authentication schemes are supported.  If the
               client attempted to authenticate via the "Authorization"
               request header field, the authorization server MUST
               respond with an HTTP 401 (Unauthorized) status code and
               include the "WWW-Authenticate" response header field
               matching the authentication scheme used by the client.

         invalid_grant
               The provided authorization grant (e.g., authorization
               code, resource owner credentials) or refresh token is
               invalid, expired, revoked, does not match the redirection
               URI used in the authorization request, or was issued to
               another client.

         unauthorized_client
               The authenticated client is not authorized to use this
               authorization grant type.

         unsupported_grant_type
               The authorization grant type is not supported by the
               authorization server.


Hardt                        Standards Track                   [Page 45]
 
RFC 6749                        OAuth 2.0                   October 2012


         invalid_scope
               The requested scope is invalid, unknown, malformed, or
               exceeds the scope granted by the resource owner.

         Values for the "error" parameter MUST NOT include characters
         outside the set %x20-21 / %x23-5B / %x5D-7E.

   error_description
         OPTIONAL.  Human-readable ASCII [USASCII] text providing
         additional information, used to assist the client developer in
         understanding the error that occurred.
         Values for the "error_description" parameter MUST NOT include
         characters outside the set %x20-21 / %x23-5B / %x5D-7E.

   error_uri
         OPTIONAL.  A URI identifying a human-readable web page with
         information about the error, used to provide the client
         developer with additional information about the error.
         Values for the "error_uri" parameter MUST conform to the
         URI-reference syntax and thus MUST NOT include characters
         outside the set %x21 / %x23-5B / %x5D-7E.

   The parameters are included in the entity-body of the HTTP response
   using the "application/json" media type as defined by [RFC4627].  The
   parameters are serialized into a JSON structure by adding each
   parameter at the highest structure level.  Parameter names and string
   values are included as JSON strings.  Numerical values are included
   as JSON numbers.  The order of parameters does not matter and can
   vary.

   For example:

     HTTP/1.1 400 Bad Request
     Content-Type: application/json;charset=UTF-8
     Cache-Control: no-store
     Pragma: no-cache

     {
       "error":"invalid_request"
     }
</pre>
* */
public class ErrorResponse extends Response {	
	/**
	 * Instantiates a new error response.
	 * The JSON representation of an error response follows:
<pre>
 {
   "error":"Unless specified otherwise, see RFC6749. Otherwise, this is specific of the SPARQL 1.1 SE Protocol",
   "error_description":"Unless specified otherwise, see RFC6749. Otherwise, this is specific of the SPARQL 1.1 SE Protocol", (OPTIONAL)
   "status_code" : the HTTP status code (would be 400 for Oauth 2.0 errors).
 }
 </pre>
	 *
	 * @param code HTTP status code
	 * @param error the error
	 * @param description the description (optional)
	 */
	public ErrorResponse(int code,String error,String description) {
		super();
		
		if (error == null || description == null) throw new IllegalArgumentException("One or more parameters are null");

		json.add("error", new JsonPrimitive(error));
		json.add("status_code", new JsonPrimitive(code));
		json.add("error_description", new JsonPrimitive(description));
	}

	/**
	 * Gets the HTTP status code.
	 *
	 * @return the HTTP status code
	 */
	public int getStatusCode() {
		return json.get("status_code").getAsInt();
	}
	
	/**
	 * Gets the error.
	 *
	 * @return the error string
	 */
	public String getError() {
		return json.get("error").getAsString();
	}
	
	/**
	 * Gets the error description.
	 *
	 * @return the error string
	 */
	public String getErrorDescription() {
		return json.get("error_description").getAsString();
	}
	
	public boolean isTokenExpiredError() {
		return getError().equals("invalid_grant");
	}
}

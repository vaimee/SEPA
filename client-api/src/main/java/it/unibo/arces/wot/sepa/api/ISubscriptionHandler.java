/* This interface includes all the methods that need to be implemented by a SEPA listener
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

package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;

/**
 * Handles SPARQL 1.1 subscription language events.
 *
 * @see SPARQL11SEProtocol#SPARQL11SEProtocol(SPARQL11SEProperties, ISubscriptionHandler)
 * @see <a href="http://wot.arces.unibo.it/TR/sparql11-subscribe.html">SPARQL 1.1 Subscribe Language</a>
 */
public interface ISubscriptionHandler {
	/**
	 * An event about changes in the graph.
	 * @param notify notification about added and removed triples
	 * 
	 * @see Notification
	 */
	void onSemanticEvent(Notification notify);

	/**
	 * This method is called after the connection with SEPA has been lost.
	 * 

<pre>For Websockets the error code is one of the following.
 
 RFC 6455                 The WebSocket Protocol            December 2011

7.4.1.  Defined Status Codes

   Endpoints MAY use the following pre-defined status codes when sending
   a Close frame.

   1000 indicates a normal closure, meaning that the purpose for
      which the connection was established has been fulfilled.

   1001 indicates that an endpoint is "going away", such as a server
      going down or a browser having navigated away from a page.

   1002 indicates that an endpoint is terminating the connection due
      to a protocol error.

   1003 indicates that an endpoint is terminating the connection
      because it has received a type of data it cannot accept (e.g., an
      endpoint that understands only text data MAY send this if it
      receives a binary message).

   1004 Reserved.  The specific meaning might be defined in the future.

   1005 is a reserved value and MUST NOT be set as a status code in a
      Close control frame by an endpoint.  It is designated for use in
      applications expecting a status code to indicate that no status
      code was actually present.

   1006 is a reserved value and MUST NOT be set as a status code in a
      Close control frame by an endpoint.  It is designated for use in
      applications expecting a status code to indicate that the
      connection was closed abnormally, e.g., without sending or
      receiving a Close control frame.

   1007 indicates that an endpoint is terminating the connection
      because it has received data within a message that was not
      consistent with the type of the message (e.g., non-UTF-8 [RFC3629]
      data within a text message).

   1008 indicates that an endpoint is terminating the connection
      because it has received a message that violates its policy.  This
      is a generic status code that can be returned when there is no
      other more suitable status code (e.g., 1003 or 1009) or if there
      is a need to hide specific details about the policy.

   1009 indicates that an endpoint is terminating the connection
      because it has received a message that is too big for it to
      process.

   1010 indicates that an endpoint (client) is terminating the
      connection because it has expected the server to negotiate one or
      more extension, but the server didn't return them in the response
      message of the WebSocket handshake.  The list of extensions that
      are needed SHOULD appear in the /reason/ part of the Close frame.
      Note that this status code is not used by the server, because it
      can fail the WebSocket handshake instead.

   1011 indicates that a server is terminating the connection because
      it encountered an unexpected condition that prevented it from
      fulfilling the request.

   1015 is a reserved value and MUST NOT be set as a status code in a
      Close control frame by an endpoint.  It is designated for use in
      applications expecting a status code to indicate that the
      connection was closed due to a failure to perform a TLS handshake
      (e.g., the server certificate can't be verified).
</pre>

	 * @param errorResponse the error
	 * 
	 * @see ErrorResponse
	 */
	void onBrokenConnection(ErrorResponse errorResponse);
	
	/**
	 * This method is called if an error occurred
	 * 
	 * @param errorResponse the error
	 * 
	 * @see ErrorResponse
	 */
	void onError(ErrorResponse errorResponse);
	
	/**
	 * This method is called when the first notification has been received
	 */
	void onSubscribe(String spuid,String alias);
	
	/**
	 * This method is called as response to an unsubscribe request
	 */
	void onUnsubscribe(String spuid);
}

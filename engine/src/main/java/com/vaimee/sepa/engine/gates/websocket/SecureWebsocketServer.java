/* Websocket protocol implementation over SSL
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

package com.vaimee.sepa.engine.gates.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import com.vaimee.sepa.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.engine.dependability.Dependability;
import com.vaimee.sepa.engine.gates.SecureWebsocketGate;
import com.vaimee.sepa.engine.scheduling.Scheduler;

public class SecureWebsocketServer extends WebsocketServer {

	@Override
	protected String getWelcomeMessage() {
		return "SPARQL 1.1 Subscribe | wss://%s:%d%s";
	}

	public SecureWebsocketServer(int port, String path, Scheduler scheduler)
			throws SEPAProtocolException, SEPASecurityException {
		super(port, path, scheduler);

		setWebSocketFactory(new DefaultSSLWebSocketServerFactory(Dependability.getSSLContext()));
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		addGate(new SecureWebsocketGate(conn, scheduler),conn);
	}
	
//	@Override
//	public void onOpen(WebSocket conn, ClientHandshake handshake) {
//		logger.trace("@onOpen: " + conn + " Resource descriptor: " + conn.getResourceDescriptor());
//
//		if (!conn.getResourceDescriptor().equals(path)) {
//			logger.warn("@onOpen Bad resource descriptor: " + conn.getResourceDescriptor() + " Use: " + path);
//			ErrorResponse response = new ErrorResponse(HttpStatus.SC_NOT_FOUND, "wrong_path",
//					"Bad resource descriptor: " + conn.getResourceDescriptor() + " Use: " + path);
//			conn.send(response.toString());
//			return;
//		}
//
//		// Add new gate
//		synchronized (gates) {
//			SecureWebsocketGate handler = new SecureWebsocketGate(conn, scheduler);
//			gates.put(conn, handler);
//
//			fragmentedMessages.put(conn, null);
//
//			logger.debug("@onOpen socket: " + conn.hashCode() + " UUID: " + handler.getGID() + " Total sockets: "
//					+ gates.size());
//		}
//	}
}

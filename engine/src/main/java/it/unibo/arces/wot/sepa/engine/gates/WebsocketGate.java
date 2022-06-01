/* A SEPA gate based on Websockets
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

package it.unibo.arces.wot.sepa.engine.gates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class WebsocketGate extends Gate {
	protected static final Logger logger = LogManager.getLogger();
	
	protected final WebSocket socket;
	
	protected boolean isConnected = true;
	
	public WebsocketGate(WebSocket s,Scheduler scheduler){
		super(scheduler);
		this.socket = s;
	}
	
	public void send(Response ret) throws SEPAProtocolException {
		try{
			socket.send(ret.toString());
			logger.trace("Sent: "+ret);
		}
		catch(WebsocketNotConnectedException e){
			logger.error("WebsocketNotConnectedException "+e.getMessage());
			isConnected = false;
			if (ret.isNotification()) {
				Notification notify = (Notification) ret;
				logger.error("WebsocketNotConnectedException failed to send notification SPUID: "+notify.getSpuid()+" Sequence: "+notify.getSequence());
				throw new SEPAProtocolException("WebsocketNotConnectedException failed to send notification SPUID: "+notify.getSpuid()+" Sequence: "+notify.getSequence());
			}
			else {
				logger.error("WebsocketNotConnectedException failed to send error response "+ret);
				throw new SEPAProtocolException("WebsocketNotConnectedException failed to send error response "+ret);
			}
		}	
	}

	@Override
	public boolean ping() {
		return isConnected && socket.isOpen();
	}
}

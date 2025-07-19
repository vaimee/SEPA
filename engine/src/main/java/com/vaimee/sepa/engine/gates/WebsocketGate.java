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

package com.vaimee.sepa.engine.gates;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.response.Notification;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.engine.scheduling.Scheduler;
import com.vaimee.sepa.logging.Logging;

public class WebsocketGate extends Gate {
	protected final WebSocket socket;
	
	protected boolean isConnected = true;
	
	public WebsocketGate(WebSocket s,Scheduler scheduler){
		super(scheduler);
		this.socket = s;
	}
	
	public void send(Response ret) throws SEPAProtocolException {
		try{
			socket.send(ret.toString());
			Logging.getLogger().trace("Sent: "+ret);
		}
		catch(WebsocketNotConnectedException e){
			Logging.getLogger().error("WebsocketNotConnectedException "+e.getMessage());
			isConnected = false;
			if (ret.isNotification()) {
				Notification notify = (Notification) ret;
				Logging.getLogger().error("WebsocketNotConnectedException failed to send notification SPUID: "+notify.getSpuid()+" Sequence: "+notify.getSequence());
				throw new SEPAProtocolException("WebsocketNotConnectedException failed to send notification SPUID: "+notify.getSpuid()+" Sequence: "+notify.getSequence());
			}
			else {
				Logging.getLogger().error("WebsocketNotConnectedException failed to send error response "+ret);
				throw new SEPAProtocolException("WebsocketNotConnectedException failed to send error response "+ret);
			}
		}	
	}

	@Override
	public boolean ping() {
		return isConnected && socket.isOpen();
	}
}

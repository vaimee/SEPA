/* A subscriber is related to an SPU and delivers events to a specific handler. This allows to share SPUs among equivalent subscriptions.
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

package com.vaimee.sepa.engine.processing.subscriptions;

import java.util.UUID;

import com.vaimee.sepa.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.commons.response.Notification;
import com.vaimee.sepa.engine.scheduling.InternalSubscribeRequest;

public class Subscriber {
	private final SPU spu;
	private final InternalSubscribeRequest sub;
	
	// Subscriber Identifier
	private final String sid;
	
	private int sequence = 0;
	
	public Subscriber(SPU spu,InternalSubscribeRequest sub) {
		this.spu = spu;
		this.sub = sub;
		
		sid = "sepa://subscription/" + UUID.randomUUID().toString();
	}
	
	public int nextSequence() {
		sequence++;
		return sequence;
	}
	
	public String getSID() {
		return sid;
	}
	
	public SPU getSPU() {
		return spu;
	}

	public void notifyEvent(Notification event) throws SEPAProtocolException {
		sub.notifyEvent(event);
	}

	public String getGID() {
		return sub.getGID();
	}

	public boolean ping() {
		return sub.ping();
	}
	
}

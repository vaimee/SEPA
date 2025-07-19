/* Subscription manager. The class keeps track of the active gates and subscriptions. It disposes gates which do not reply to ping requests.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.util.ConcurrentHashSet;

import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.engine.core.ResponseHandler;
import com.vaimee.sepa.engine.gates.Gate;
import com.vaimee.sepa.engine.scheduling.InternalUnsubscribeRequest;
import com.vaimee.sepa.engine.scheduling.Scheduler;
import com.vaimee.sepa.logging.Logging;

class GatesMonitor implements ResponseHandler{
	
	public GatesMonitor() {
		Thread thread = new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						return;
					}
					pingGates();
				}
			}
		};
		thread.setName("SEPA-Gates-Ping");
		thread.start();	
	}

	// Active subscriptions and gates
	private final ConcurrentHashMap<String, ArrayList<String>> SUBSCRIPTIONS_HASH_MAP = new ConcurrentHashMap<String, ArrayList<String>>();
	private final Set<Gate> gates = new ConcurrentHashSet<Gate>();

	private Scheduler scheduler = null;

	public void setScheduler(Scheduler p) {
		scheduler = p;
	}

	private void pingGates() {
		Set<Gate> brokenGates = new HashSet<Gate>();

		Logging.getLogger().log(Logging.getLevel("ping"),"pingGates");
		
		for (Gate g : gates) {
			Logging.getLogger().log(Logging.getLevel("ping"),"ping "+g.getGID());
			if (g.ping())
				continue;
			Logging.getLogger().log(Logging.getLevel("ping"),"broken gate "+g.getGID());
			brokenGates.add(g);
		}

		for (Gate g : brokenGates) {
			try {
				Logging.getLogger().log(Logging.getLevel("ping"),"Close internal "+g.getGID());
				onCloseInternal(g.getGID());
			} catch (InterruptedException e) {
				Logging.getLogger().warn("Exception on closing gate: " + g.getGID() + " exception: " + e.getMessage());
			}
			gates.remove(g);
		}
	}

	public synchronized void addGate(Gate g) {
		Logging.getLogger().log(Logging.getLevel("ping"),"Add gate "+g.getGID());
		gates.add(g);
	}

	public synchronized void removeGate(Gate g) {
		Logging.getLogger().log(Logging.getLevel("ping"),"Remove gate "+g.getGID());
		gates.remove(g);
	}

	public synchronized void onSubscribe(String gid, String sid) {
		Logging.getLogger().log(Logging.getLevel("ping"),"onSubscribe "+gid+" "+sid);
		
		if (gid == null) {
			Logging.getLogger().error("@onSubscribe GID is null");
			return;
		}
		if (sid == null) {
			Logging.getLogger().error("@onSubscribe SID is null");
			return;
		}

		// Gate exists?
		if (!SUBSCRIPTIONS_HASH_MAP.containsKey(gid)) {
			// Create a new gate entry
			Logging.getLogger().log(Logging.getLevel("ping"),"@onSubscribe create new gate: " + gid);
			SUBSCRIPTIONS_HASH_MAP.put(gid, new ArrayList<String>());
		}

		// Add subscription
		SUBSCRIPTIONS_HASH_MAP.get(gid).add(sid);

		Logging.getLogger().log(Logging.getLevel("spu"),"ADDED " + gid + " " + sid + " " + SUBSCRIPTIONS_HASH_MAP.size() + " "
				+ SUBSCRIPTIONS_HASH_MAP.get(gid).size());
	}

	public synchronized void onUnsubscribe(String gid, String sid) {
		Logging.getLogger().log(Logging.getLevel("ping"),"onUnsubscribe "+gid+" "+sid);
		
		if (gid == null) {
			Logging.getLogger().error("@onUnsubscribe GID is null");
			return;
		}
		if (sid == null) {
			Logging.getLogger().error("@onUnsubscribe SID is null");
			return;
		}

		if (SUBSCRIPTIONS_HASH_MAP.containsKey(gid)) {
			Logging.getLogger().log(Logging.getLevel("spu"),"REMOVE " + gid + " " + sid + " --- " + SUBSCRIPTIONS_HASH_MAP.size() + " "
					+ SUBSCRIPTIONS_HASH_MAP.get(gid).size());

			// Remove subscription
			SUBSCRIPTIONS_HASH_MAP.get(gid).remove(sid);
		}
		else {
			Logging.getLogger().warn("@onUnsubscribe "+gid+" NOT FOUND. Broken subscription?");
		}
	}

	public void onClose(String gid) throws InterruptedException {
		Logging.getLogger().log(Logging.getLevel("ping"),"onClose "+gid);
		onCloseInternal(gid);
	}

	private synchronized void onCloseInternal(String gid) throws InterruptedException {
		Logging.getLogger().log(Logging.getLevel("ping"),"onCloseInternal "+gid);
		
		if (gid == null) {
			Logging.getLogger().error("@onClose GID is null");
			return;
		}
		if (scheduler == null) {
			Logging.getLogger().error("@onClose scheduler is null");
			return;
		}

		// Gate exists?
		if (!SUBSCRIPTIONS_HASH_MAP.containsKey(gid)) {
			Logging.getLogger().warn("NOT_FOUND " + gid + " " + "---" + " " + SUBSCRIPTIONS_HASH_MAP.size() + " " + "-1");
			return;
		}

		Logging.getLogger().log(Logging.getLevel("spu"),"CLOSE " + gid + " --- " + SUBSCRIPTIONS_HASH_MAP.size() + " "
				+ SUBSCRIPTIONS_HASH_MAP.get(gid).size());

		// Kill all active subscriptions
		for (String sid : SUBSCRIPTIONS_HASH_MAP.get(gid)) {
			Logging.getLogger().log(Logging.getLevel("ping"),"kill "+sid+" "+gid);
			scheduler.schedule(new InternalUnsubscribeRequest(gid, sid, null), this);
			//processor.killSubscription(sid, gid);
			//processor.processUnsubscribe(sid, gid);
		}

		// Remove gate
		Logging.getLogger().log(Logging.getLevel("ping"),"remove gate "+gid);
		SUBSCRIPTIONS_HASH_MAP.remove(gid);
	}

	public synchronized void onError(String gid, Exception e) {
		Logging.getLogger().log(Logging.getLevel("ping"),"onError "+gid+ " "+e.getMessage());
		
		if (gid == null) {
			Logging.getLogger().error("@onError GID is null");
			return;
		}

		Logging.getLogger().error("@onError GID:" + gid + " Exception:" + e);
	}

	public synchronized long getNumberOfGates() {
		return SUBSCRIPTIONS_HASH_MAP.size();

	}

	@Override
	public void sendResponse(Response response) throws SEPAProtocolException {
		// TODO Auto-generated method stub
		
	}
}

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

package it.unibo.arces.wot.sepa.engine.dependability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.util.ConcurrentHashSet;

import it.unibo.arces.wot.sepa.engine.gates.Gate;
import it.unibo.arces.wot.sepa.engine.processing.Processor;

class GatesMonitor {
	static {
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

	private static final Logger logger = LogManager.getLogger();

	// Active subscriptions and gates
	private static final ConcurrentHashMap<String, ArrayList<String>> SUBSCRIPTIONS_HASH_MAP = new ConcurrentHashMap<String, ArrayList<String>>();
	private static final Set<Gate> gates = new ConcurrentHashSet<Gate>();

	private static Processor processor = null;

	public static void setProcessor(Processor p) {
		processor = p;
	}

	private static void pingGates() {
		Set<Gate> brokenGates = new HashSet<Gate>();

		logger.log(Level.getLevel("ping"),"pingGates");
		
		for (Gate g : gates) {
			logger.log(Level.getLevel("ping"),"ping "+g.getGID());
			if (g.ping())
				continue;
			logger.log(Level.getLevel("ping"),"broken gate "+g.getGID());
			brokenGates.add(g);
		}

		for (Gate g : brokenGates) {
			try {
				logger.log(Level.getLevel("ping"),"Close internal "+g.getGID());
				onCloseInternal(g.getGID());
			} catch (InterruptedException e) {
				logger.warn("Exception on closing gate: " + g.getGID() + " exception: " + e.getMessage());
			}
			gates.remove(g);
		}
	}

	public synchronized static void addGate(Gate g) {
		logger.log(Level.getLevel("ping"),"Add gate "+g.getGID());
		gates.add(g);
	}

	public synchronized static void removeGate(Gate g) {
		logger.log(Level.getLevel("ping"),"Remove gate "+g.getGID());
		gates.remove(g);
	}

	public synchronized static void onSubscribe(String gid, String sid) {
		logger.log(Level.getLevel("ping"),"onSubscribe "+gid+" "+sid);
		
		if (gid == null) {
			logger.error("@onSubscribe GID is null");
			return;
		}
		if (sid == null) {
			logger.error("@onSubscribe SID is null");
			return;
		}

		// Gate exists?
		if (!SUBSCRIPTIONS_HASH_MAP.containsKey(gid)) {
			// Create a new gate entry
			logger.log(Level.getLevel("ping"),"@onSubscribe create new gate: " + gid);
			SUBSCRIPTIONS_HASH_MAP.put(gid, new ArrayList<String>());
		}

		// Add subscription
		SUBSCRIPTIONS_HASH_MAP.get(gid).add(sid);

		logger.log(Level.getLevel("spu"),"ADDED " + gid + " " + sid + " " + SUBSCRIPTIONS_HASH_MAP.size() + " "
				+ SUBSCRIPTIONS_HASH_MAP.get(gid).size());
	}

	public synchronized static void onUnsubscribe(String gid, String sid) {
		logger.log(Level.getLevel("ping"),"onUnsubscribe "+gid+" "+sid);
		
		if (gid == null) {
			logger.error("@onUnsubscribe GID is null");
			return;
		}
		if (sid == null) {
			logger.error("@onUnsubscribe SID is null");
			return;
		}

		if (SUBSCRIPTIONS_HASH_MAP.containsKey(gid)) {
			logger.log(Level.getLevel("spu"),"REMOVE " + gid + " " + sid + " --- " + SUBSCRIPTIONS_HASH_MAP.size() + " "
					+ SUBSCRIPTIONS_HASH_MAP.get(gid).size());

			// Remove subscription
			SUBSCRIPTIONS_HASH_MAP.get(gid).remove(sid);
		}
		else {
			logger.warn("@onUnsubscribe "+gid+" NOT FOUND");
		}
	}

	public synchronized static void onClose(String gid) throws InterruptedException {
		logger.log(Level.getLevel("ping"),"onClose "+gid);
		onCloseInternal(gid);
	}

	private static void onCloseInternal(String gid) throws InterruptedException {
		logger.log(Level.getLevel("ping"),"onCloseInternal "+gid);
		
		if (gid == null) {
			logger.error("@onClose GID is null");
			return;
		}
		if (processor == null) {
			logger.error("@onClose processor is null");
			return;
		}

		// Gate exists?
		if (!SUBSCRIPTIONS_HASH_MAP.containsKey(gid)) {
			logger.warn("NOT_FOUND " + gid + " " + "---" + " " + SUBSCRIPTIONS_HASH_MAP.size() + " " + "-1");
			return;
		}

		logger.log(Level.getLevel("spu"),"CLOSE " + gid + " --- " + SUBSCRIPTIONS_HASH_MAP.size() + " "
				+ SUBSCRIPTIONS_HASH_MAP.get(gid).size());

		// Kill all active subscriptions
		for (String sid : SUBSCRIPTIONS_HASH_MAP.get(gid)) {
			logger.log(Level.getLevel("ping"),"kill "+sid+" "+gid);
			processor.killSubscription(sid, gid);
		}

		// Remove gate
		logger.log(Level.getLevel("ping"),"remove gate "+gid);
		SUBSCRIPTIONS_HASH_MAP.remove(gid);
	}

	public synchronized static void onError(String gid, Exception e) {
		logger.log(Level.getLevel("ping"),"onError "+gid+ " "+e.getMessage());
		
		if (gid == null) {
			logger.error("@onError GID is null");
			return;
		}

		logger.error("@onError GID:" + gid + " Exception:" + e);
	}

	public synchronized static long getNumberOfGates() {
		return SUBSCRIPTIONS_HASH_MAP.size();

	}
}
